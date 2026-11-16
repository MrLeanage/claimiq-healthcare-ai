[CmdletBinding()]
param(
    [Parameter(Position=0)][ValidateSet('help','start','stop','restart','status','logs','tokens')][string] $Task='help',
    [ValidateSet('all','ai','backend','frontend')][string] $Service='all',
    [ValidateRange(1,65535)][int] $BackendPort,
    [ValidateRange(1,65535)][int] $FrontendPort,
    [ValidateRange(1,65535)][int] $AiPort,
    [string] $JavaHome,
    [string] $PythonExe,
    [string] $AuthLibraryPath,
    [ValidateRange(5,600)][int] $StartupTimeoutSeconds=300,
    [ValidateSet('SqlServer','Memory')][string] $Database
)
$ErrorActionPreference='Stop'
if($Task -eq 'help') {
    'ClaimIQ: .\dev.cmd start | stop | restart | status | logs | tokens'
    'Starts AI, backend, then frontend; prints status and current demo tokens.'
    'Defaults: AI 8090, backend 8081 (SQL Server), frontend 5173.'
    'Use -Service ai|backend|frontend to manage one component.'
    'Options: -AiPort -BackendPort -FrontendPort -PythonExe -JavaHome -AuthLibraryPath -StartupTimeoutSeconds -Database SqlServer|Memory'
    'The .cmd wrapper uses a process-only PowerShell execution-policy override.'
    'Read scripts/README.md for setup, logs, port ownership and shutdown behavior.'
    return
}
$manager=Join-Path $PSScriptRoot 'scripts\Manage-Server.ps1'
$order=@('ai','backend','frontend')
function Invoke-Component([string]$Name,[string]$Action) {
    $options=@{Component=$Name;Action=$Action;TimeoutSeconds=$StartupTimeoutSeconds}
    switch($Name) {
        'ai' {
            if($AiPort){$options.Port=$AiPort}
            if($PythonExe){$options.PythonExe=$PythonExe}
        }
        'backend' {
            foreach($key in @('JavaHome','AuthLibraryPath','Database')) {
                if($script:PSBoundParameters.ContainsKey($key)){$options[$key]=$script:PSBoundParameters[$key]}
            }
            if($BackendPort){$options.Port=$BackendPort}
            if($Action -in @('start','restart')) {
                $ai=Invoke-Component 'ai' 'status'
                $options.AiUrl="http://127.0.0.1:$($ai.Port)"
            }
        }
        'frontend' {
            if($FrontendPort){$options.Port=$FrontendPort}
            if($Action -in @('start','restart')) {
                $backend=Invoke-Component 'backend' 'status'
                $options.BackendUrl="http://127.0.0.1:$($backend.Port)"
            }
        }
    }
    & $manager @options
}
function Show-Status {
    $rows=foreach($name in $order){Invoke-Component $name 'status'}
    $rows | Format-Table Component,Status,Port,PID,Managed,URL -AutoSize | Out-Host
}
function Show-Tokens {
    try { Invoke-Component 'backend' 'tokens' }
    catch { Write-Warning "Tokens unavailable: $($_.Exception.Message)" }
}
function Stop-All {
    $failures=@()
    foreach($name in @('frontend','backend','ai')) {
        Write-Host "Stopping $name..."
        try { Invoke-Component $name 'stop' | Out-Null }
        catch { $failures += "$name`: $($_.Exception.Message)" }
    }
    Show-Status
    if($failures.Count){throw ($failures -join [Environment]::NewLine)}
}
function Start-All {
    $before=@{}
    foreach($name in $order){$before[$name]=Invoke-Component $name 'status'}
    $ports=@($before.Values | ForEach-Object {$_.Port})
    if(@($ports | Select-Object -Unique).Count -ne 3){throw 'Each service must use a different port.'}
    $started=@()
    try {
        for($step=0;$step -lt $order.Count;$step++) {
            $name=$order[$step]
            Write-Host "[$($step+1)/3] Starting $name and waiting for health..."
            Invoke-Component $name 'start' | Out-Null
            if($before[$name].Status -ne 'running'){$started += $name}
        }
    } catch {
        $failure=$_
        Write-Warning 'Startup failed. Stopping only services started by this command.'
        [array]::Reverse($started)
        foreach($name in $started) {
            try {Invoke-Component $name 'stop' | Out-Null}
            catch {Write-Warning $_.Exception.Message}
        }
        Show-Status
        throw $failure
    }
    Show-Status
    Show-Tokens
}

# Serialize whole-stack commands as well as the manager's per-service operations.
$hash=[Security.Cryptography.SHA256]::Create()
try {$key=([BitConverter]::ToString($hash.ComputeHash([Text.Encoding]::UTF8.GetBytes($PSScriptRoot.ToLowerInvariant())))).Replace('-','').Substring(0,16)}
finally {$hash.Dispose()}
$mutex=New-Object Threading.Mutex($false,"Local\ClaimIQ_${key}_stack")
$locked=$false
try {
    try {$locked=$mutex.WaitOne(1000)} catch [Threading.AbandonedMutexException] {$locked=$true}
    if(!$locked){throw 'Another root command is managing the stack. Retry when it finishes.'}
    if($Task -eq 'tokens'){Invoke-Component 'backend' 'tokens';return}
    if($Service -ne 'all') {
        Invoke-Component $Service $Task
        if($Service -eq 'backend' -and $Task -in @('start','restart')){Show-Tokens}
        return
    }
    switch($Task) {
        'start' {Start-All}
        'stop' {Stop-All}
        'restart' {Stop-All;Start-All}
        'status' {Show-Status}
        'logs' {foreach($name in $order){Write-Host "--- $name ---";Invoke-Component $name 'logs'}}
    }
} finally {
    if($locked){$mutex.ReleaseMutex()}
    $mutex.Dispose()
}
