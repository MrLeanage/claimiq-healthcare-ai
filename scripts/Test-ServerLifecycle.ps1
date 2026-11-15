# Integration test for a clean Windows checkout. Uses disposable H2 and reserved test ports.
[CmdletBinding()]
param([string]$ProjectRoot, [string]$JavaHome=$env:JAVA_HOME)
$ErrorActionPreference='Stop'
if(!$ProjectRoot){$ProjectRoot=Split-Path (Split-Path $MyInvocation.MyCommand.Path -Parent) -Parent}
$entry=Join-Path $ProjectRoot 'dev.ps1'
$manager=Join-Path $ProjectRoot 'scripts\Manage-Server.ps1'
$common=@{AiPort=18090;BackendPort=18081;FrontendPort=15173;Database='Memory'}
if($JavaHome){$common.JavaHome=$JavaHome}
function Assert([bool]$Value,[string]$Message){if(!$Value){throw $Message}}
function Status([string]$Name) {
    $port=switch($Name){'ai'{18090};'backend'{18081};'frontend'{15173}}
    & $manager -Component $Name -Action status -Port $port
}
function Writer([string]$Output) {
    $match=[regex]::Match($Output,'Writer:\r?\n([^\r\n]+)')
    Assert $match.Success 'Writer token was not printed.'
    Assert ($Output -match 'Reviewer:') 'Reviewer token was not printed.'
    $match.Groups[1].Value.Trim()
}
foreach($name in 'ai','backend','frontend') {
    Assert ((Status $name).Status -eq 'stopped') 'Use a clean checkout with the reserved test ports available.'
}
$collision=$null
try {
    $output=& $entry start @common 6>&1 | Out-String
    $firstToken=Writer $output
    $pids=@{}
    foreach($name in 'ai','backend','frontend') {
        $state=Status $name
        Assert ($state.Status -eq 'running' -and $state.Managed) "$name did not become healthy and managed."
        $pids[$name]=$state.PID
    }
    $null=& $entry start @common 6>&1 | Out-String
    foreach($name in 'ai','backend','frontend') {
        Assert ((Status $name).PID -eq $pids[$name]) 'Repeated start created a duplicate process.'
    }
    $headers=@{Authorization="Bearer $firstToken"}
    $body=@{
        patientReference='PAT-LAUNCHER';providerReference='PROV-LAUNCHER';amount=125;currency='USD'
        serviceDate=(Get-Date).AddDays(-1).ToString('yyyy-MM-dd')
        context=@{
            insurance=@{payerName='Demo payer';planReference='PLAN-DEMO';memberReference='MEM-DEMO';coverageStatus='INACTIVE';authorizationStatus='PENDING'}
            diagnosisCodes=@('Z00.00');clinicalSummary='Synthetic launcher test'
            serviceLines=@(@{codeSystem='LOCAL';procedureCode='DEMO-001';description='Synthetic service';units=1;unitPrice=125})
        }
    } | ConvertTo-Json -Depth 8
    $created=Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:15173/api/v1/claims' -Method Post -Headers $headers -ContentType 'application/json' -Body $body
    $claim=$created.Content | ConvertFrom-Json
    $headers['If-Match']=$created.Headers['ETag']
    $assessment=Invoke-RestMethod -Uri "http://127.0.0.1:15173/api/v1/claims/$($claim.id)/assessment" -Method Post -Headers $headers
    Assert ($assessment.assessment.riskLevel -eq 'HIGH') 'The custom frontend/backend/AI port chain failed.'

    $output=& $entry restart @common 6>&1 | Out-String
    $newToken=Writer $output
    Assert ($firstToken -ne $newToken) 'Backend restart did not rotate tokens.'
    $headers=@{Authorization="Bearer $newToken"}
    $null=Invoke-RestMethod -Uri 'http://127.0.0.1:15173/api/v1/claims' -Headers $headers
    $oldRejected=$false
    try {$null=Invoke-RestMethod -Uri 'http://127.0.0.1:15173/api/v1/claims' -Headers @{Authorization="Bearer $firstToken"}}
    catch {$oldRejected=$_.Exception.Response.StatusCode.value__ -eq 401}
    Assert $oldRejected 'The old token remained valid after restart.'
    $null=& $entry stop @common 6>&1 | Out-String
    $null=& $entry stop @common 6>&1 | Out-String
    foreach($name in 'ai','backend','frontend'){Assert ((Status $name).Status -eq 'stopped') "$name survived stop."}

    # A backend port collision must leave that listener intact and roll back newly started AI.
    $collision=New-Object Net.Sockets.TcpListener([Net.IPAddress]::Loopback,18081)
    $collision.Start()
    $failed=$false
    try {$null=& $entry start @common 6>&1 | Out-String} catch {$failed=$true}
    Assert $failed 'An occupied backend port was accepted.'
    Assert ((Status 'ai').Status -eq 'stopped') 'Failed startup left a new AI process running.'
    Assert ((Status 'backend').PID -eq $PID) 'The unrelated port owner was stopped.'
    Write-Output 'PASS: ordered startup, token output, repeated start, real assessment, restart/token rotation, repeated stop and failure rollback.'
} finally {
    if($collision){$collision.Stop()}
    $null=& $entry stop @common 6>&1 | Out-String
}
