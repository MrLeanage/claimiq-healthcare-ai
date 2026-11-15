[CmdletBinding()]
param(
    [Parameter(Position=0)][ValidateSet('help','start','stop','restart','status','logs','tokens')][string] $Task='help',
    [ValidateSet('all','backend','frontend')][string] $Service='all',
    [ValidateRange(1,65535)][int] $BackendPort,
    [ValidateRange(1,65535)][int] $FrontendPort,
    [string] $JavaHome,
    [string] $AuthLibraryPath,
    [ValidateSet('SqlServer','Memory')][string] $Database
)
$ErrorActionPreference='Stop'
if($Task -eq 'help') {
    'ClaimIQ servers: .\dev.ps1 start | stop | restart | status | logs | tokens'
    'Default: both servers, SQL Server backend on 8081, frontend on 5173.'
    'Use -Service backend or -Service frontend to manage one server.'
    'Options: -BackendPort -FrontendPort -JavaHome -AuthLibraryPath -Database SqlServer|Memory'
    'Read scripts/README.md for logs, tokens, port ownership, and shutdown behavior.'
    return
}
$manager=Join-Path $PSScriptRoot 'scripts\Manage-Server.ps1'
function Invoke-Component([string]$Name,[string]$Action) {
    $options=@{Component=$Name;Action=$Action}
    if($Name -eq 'backend') {
        foreach($key in @('JavaHome','AuthLibraryPath','Database')) {
            if($script:PSBoundParameters.ContainsKey($key)){$options[$key]=$script:PSBoundParameters[$key]}
        }
        if($BackendPort){$options.Port=$BackendPort}
    } else {
        if($FrontendPort){$options.Port=$FrontendPort}
        if($Action -in @('start','restart')) {
            $backendOptions=@{Component='backend';Action='status'}
            if($BackendPort){$backendOptions.Port=$BackendPort}
            $backend=& $manager @backendOptions
            $options.BackendUrl="http://127.0.0.1:$($backend.Port)"
        }
    }
    & $manager @options
}
if($Task -eq 'tokens'){Invoke-Component 'backend' 'tokens';return}
if($Service -ne 'all'){Invoke-Component $Service $Task;return}
if($Task -in @('stop','restart')) {
    Invoke-Component 'frontend' 'stop'
    Invoke-Component 'backend' 'stop'
}
if($Task -in @('start','restart')) {
    Invoke-Component 'backend' 'start'
    Invoke-Component 'frontend' 'start'
} elseif($Task -ne 'stop') {
    Invoke-Component 'backend' $Task
    Invoke-Component 'frontend' $Task
}
