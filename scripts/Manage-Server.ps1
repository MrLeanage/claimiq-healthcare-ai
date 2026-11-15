[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)][ValidateSet('backend','frontend','ai')][string] $Component,
    [Parameter(Mandatory=$true)][ValidateSet('start','stop','restart','status','logs','tokens')][string] $Action,
    [ValidateRange(0,65535)][int] $Port = 0,
    [string] $JavaHome,
    [string] $AuthLibraryPath,
    [ValidateSet('SqlServer','Memory')][string] $Database,
    [string] $BackendUrl,
    [string] $AiUrl,
    [string] $PythonExe,
    [ValidateRange(5,180)][int] $TimeoutSeconds = 90
)
$ErrorActionPreference='Stop'
$projectRoot=[IO.Path]::GetFullPath((Split-Path $PSScriptRoot -Parent))
$sha=[Security.Cryptography.SHA256]::Create()
try { $key=([BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($projectRoot.ToLowerInvariant())))).Replace('-','').Substring(0,16) } finally { $sha.Dispose() }
$runtime=Join-Path $env:LOCALAPPDATA "ClaimIQ\servers\$key\$Component"
New-Item -ItemType Directory -Force -Path $runtime | Out-Null
$stateFile=Join-Path $runtime 'state.json'
$runner=Join-Path $PSScriptRoot 'Run-Server.ps1'
$state=$null
$mutex=New-Object Threading.Mutex($false,"Local\ClaimIQ_${key}_$Component")
$locked=$false
function Contains-Path([string]$Text,[string]$Path) {
    return $Text -and $Text.Replace('/','\').Replace('\\','\').IndexOf($Path.Replace('/','\').Replace('\\','\'),[StringComparison]::OrdinalIgnoreCase) -ge 0
}
function Get-Listener {
    $ids=@(Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique)
    if($ids.Count -gt 1){throw "Port $Port has multiple owners; refusing to manage it."}
    if($ids.Count){return Get-CimInstance Win32_Process -Filter "ProcessId=$($ids[0])"}
    return $null
}
function Test-Server($Process) {
    if(!$Process){return $false}
    if($Component -eq 'ai') {
        $entry=Join-Path $projectRoot 'ai-service\serve.py'
        return $Process.Name -match '^python(?:w|[0-9.]*)?\.exe$' -and
            $Process.CommandLine -match ('(?:^|\s)"?' + [regex]::Escape($entry) + '"?(?:\s|$)')
    }
    if($Component -eq 'frontend') {
        return $Process.Name -eq 'node.exe' -and (Contains-Path $Process.CommandLine (Join-Path $projectRoot 'frontend')) -and $Process.CommandLine -match '[\\/]vite[\\/]bin[\\/]vite\.js'
    }
    if($Process.Name -ne 'java.exe' -or $Process.CommandLine -notlike '*com.claimiq.BackendApplication*'){return $false}
    if($Process.CommandLine -notmatch '-cp\s+@("[^"]+"|\S+)'){return $false}
    $argumentFile=$Matches[1].Trim('"')
    return (Test-Path -LiteralPath $argumentFile) -and (Contains-Path (Get-Content -LiteralPath $argumentFile -Raw) (Join-Path $projectRoot 'backend'))
}
function Get-Worker {
    if(!$state -or !$state.workerPid){return $null}
    $worker=Get-CimInstance Win32_Process -Filter "ProcessId=$($state.workerPid)" -ErrorAction SilentlyContinue
    if($worker -and $worker.CreationDate.ToUniversalTime() -eq ([DateTime]$state.workerCreated).ToUniversalTime() -and
        (Contains-Path $worker.CommandLine $runner) -and (Contains-Path $worker.CommandLine $state.configFile)){return $worker}
    return $null
}
function Test-Ready {
    $url=if($Component -eq 'backend'){"http://127.0.0.1:$Port/actuator/health"}elseif($Component -eq 'ai'){"http://127.0.0.1:$Port/health"}else{"http://127.0.0.1:$Port/"}
    try {
        $request=[Net.HttpWebRequest]::Create($url);$request.Timeout=2000;$request.Proxy=$null
        $response=$request.GetResponse()
        try {return [int]$response.StatusCode -eq 200} finally {$response.Close()}
    } catch {return $false}
}
function Get-Status {
    $listener=Get-Listener
    $owned=Test-Server $listener
    $worker=Get-Worker
    $status=if($listener -and !$owned){'port occupied by another application'}elseif($owned -and (Test-Ready)){'running'}elseif($owned -or $worker){'starting / unhealthy'}else{'stopped'}
    [pscustomobject]@{Component=$Component;Status=$status;Port=$Port;PID=if($listener){$listener.ProcessId}else{$null};Managed=[bool]$worker;URL="http://localhost:$Port";Logs=$runtime}
}
function Stop-Tree($Root) {
    # Verify each process creation time again before stopping it; PID reuse is unsafe.
    $processes=@(Get-CimInstance Win32_Process)
    $tree=New-Object 'System.Collections.Generic.List[object]'
    function Add-Children($Parent,[int]$Depth) {
        foreach($child in $processes | Where-Object {$_.ParentProcessId -eq $Parent.ProcessId -and $_.CreationDate -ge $Parent.CreationDate}) {
            Add-Children $child ($Depth+1)
        }
        $tree.Add($Parent)
    }
    Add-Children $Root 0
    foreach($process in $tree) {
        $current=Get-CimInstance Win32_Process -Filter "ProcessId=$($process.ProcessId)" -ErrorAction SilentlyContinue
        if($current -and $current.CreationDate -eq $process.CreationDate){Stop-Process -Id $current.ProcessId -ErrorAction SilentlyContinue}
    }
}
function Stop-Server {
    $listener=Get-Listener
    if($listener -and !(Test-Server $listener)){throw "Port $Port belongs to another application (PID $($listener.ProcessId)); nothing was stopped."}
    $worker=Get-Worker
    if($worker){Stop-Tree $worker}elseif($listener){Stop-Tree $listener}
    if($state -and $state.sessionFile -and (Test-Path -LiteralPath $state.sessionFile)){Remove-Item -LiteralPath $state.sessionFile}
    $deadline=[DateTime]::UtcNow.AddSeconds(10)
    while((Get-Listener) -and [DateTime]::UtcNow -lt $deadline){Start-Sleep -Milliseconds 250}
    if(Get-Listener){throw "Server has not released port $Port."}
    Write-Host "$Component stopped."
}
function Start-Server {
    $listener=Get-Listener
    if($listener){
        if(!(Test-Server $listener)){throw "Port $Port belongs to an unmanaged or different application (PID $($listener.ProcessId)). Stop its original terminal process, or choose another port. Nothing was stopped."}
        if(!(Test-Ready)){throw "$Component owns port $Port but is unhealthy. Inspect logs or restart it."}
        Write-Host "$Component is already running; no duplicate instance started."
        return
    }
    if(Get-Worker){throw "$Component already has a managed instance. Use status or restart to change its settings."}
    $runId=[Guid]::NewGuid().ToString('N')
    $configFile=Join-Path $runtime "$runId.json"
    $config=@{projectRoot=$projectRoot;component=$Component;port=$Port;sessionFile=(Join-Path $runtime "$runId.tokens.json")
        javaHome=$JavaHome;authLibraryPath=$AuthLibraryPath;database=$Database;backendUrl=$BackendUrl;aiUrl=$AiUrl;pythonExe=$PythonExe}
    if(!$config.javaHome -and $state){$config.javaHome=$state.javaHome}
    if(!$config.authLibraryPath -and $state){$config.authLibraryPath=$state.authLibraryPath}
    if(!$config.database){$config.database=if($state){$state.database}else{'SqlServer'}}
    if(!$config.backendUrl){$config.backendUrl=if($state){$state.backendUrl}else{'http://127.0.0.1:8081'}}
    if(!$config.aiUrl){$config.aiUrl=if($state -and $state.aiUrl){$state.aiUrl}else{'http://127.0.0.1:8090'}}
    if($Component -eq 'ai') {
        if(!$config.pythonExe){$config.pythonExe=if($state -and $state.pythonExe -is [string] -and $state.pythonExe){$state.pythonExe}else{'python'}}
        $config.pythonExe=(Get-Command $config.pythonExe -CommandType Application -ErrorAction Stop | Select-Object -First 1).Source
    }
    $config | ConvertTo-Json | Set-Content -LiteralPath $configFile
    $hostExe=Join-Path $PSHOME 'pwsh.exe'
    if(!(Test-Path -LiteralPath $hostExe)){$hostExe=Join-Path $PSHOME 'powershell.exe'}
    $stdout=Join-Path $runtime "$runId.out.log";$stderr=Join-Path $runtime "$runId.err.log"
    $arguments="-NoProfile -NonInteractive -ExecutionPolicy Bypass -File `"$runner`" -ConfigFile `"$configFile`""
    $worker=Start-Process -FilePath $hostExe -ArgumentList $arguments -WindowStyle Hidden -RedirectStandardOutput $stdout -RedirectStandardError $stderr -PassThru
    $identity=Get-CimInstance Win32_Process -Filter "ProcessId=$($worker.Id)"
    $config.workerPid=$worker.Id;$config.workerCreated=if($identity){$identity.CreationDate.ToUniversalTime().ToString('o')}else{$null}
    $config.configFile=$configFile;$config.stdout=$stdout;$config.stderr=$stderr
    $script:state=[pscustomobject]$config
    $state | ConvertTo-Json | Set-Content -LiteralPath $stateFile
    if(!$identity){throw "$Component worker exited immediately. Run logs to inspect $runtime"}
    Write-Host "Starting $Component on port $Port..."
    $deadline=[DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    while([DateTime]::UtcNow -lt $deadline){
        $listener=Get-Listener
        if((Test-Server $listener) -and (Test-Ready)){
            Write-Host "$Component ready: http://localhost:$Port"
            if($Component -eq 'backend') {
                Write-Host 'Get the current writer/reviewer tokens: .\dev.ps1 tokens (from backend or the solution root).'
                Write-Host 'After restarting the backend, disconnect and reconnect the frontend with a fresh token.'
            }
            return
        }
        if(!(Get-Worker)){Stop-Server;throw "$Component exited during startup. Run logs to inspect $runtime"}
        Start-Sleep -Milliseconds 500
    }
    # A failed start must not leave an unexpected server behind.
    Stop-Server
    throw "$Component did not become ready in $TimeoutSeconds seconds. Inspect logs in $runtime"
}
try {
    try {$locked=$mutex.WaitOne(1000)}catch [Threading.AbandonedMutexException]{$locked=$true}
    if(!$locked){throw 'Another command is managing this server. Retry when it finishes.'}
    $state=if(Test-Path -LiteralPath $stateFile){Get-Content -LiteralPath $stateFile -Raw | ConvertFrom-Json}else{$null}
    if(!$Port){$Port=if($state){[int]$state.port}elseif($Component -eq 'backend'){8081}elseif($Component -eq 'ai'){8090}else{5173}}
    if($state -and [int]$state.port -ne $Port -and $Action -in @('stop','restart','start') -and (Get-Worker)) {
        throw "A managed $Component instance uses port $($state.port). Stop it without a port override before changing ports."
    }
    switch($Action){
        'start' {Start-Server;Get-Status}
        'stop' {Stop-Server;Get-Status}
        'restart' {Stop-Server;Start-Server;Get-Status}
        'status' {Get-Status}
        'logs' {
            if(!$state){Write-Host 'No managed server logs yet.';return}
            Write-Host "Output: $($state.stdout)"
            if(Test-Path -LiteralPath $state.stdout){Get-Content -LiteralPath $state.stdout -Tail 40}
            Write-Host "Errors: $($state.stderr)"
            if(Test-Path -LiteralPath $state.stderr){Get-Content -LiteralPath $state.stderr -Tail 20}
        }
        'tokens' {
            if($Component -ne 'backend'){throw 'Demo tokens belong to the backend.'}
            if(!(Get-Worker) -or !(Test-Path -LiteralPath $state.sessionFile)){throw 'No managed demo session. Run backend restart to create one.'}
            $session=Get-Content -LiteralPath $state.sessionFile -Raw | ConvertFrom-Json
            if([DateTimeOffset]::UtcNow.ToUnixTimeSeconds() -ge $session.expiresAt){throw 'Demo tokens expired. Restart the backend for new tokens.'}
            Write-Host 'Paste one complete token into the frontend Access token field, without the Bearer prefix.'
            Write-Output "Writer:`n$($session.writer)`n`nReviewer:`n$($session.reviewer)"
        }
    }
} finally {if($locked){$mutex.ReleaseMutex()};$mutex.Dispose()}
