[CmdletBinding()]
param(
    [Parameter(Position=0)]
    [ValidateSet('help','install','dev','verify','build','test','e2e','browsers','preview','format','start','stop','restart','status','logs')]
    [string] $Task = 'help',
    [ValidateRange(1,65535)][int] $Port = 5173,
    [string] $BackendUrl = 'http://127.0.0.1:8081',
    [string] $JavaHome
)
$ErrorActionPreference = 'Stop'
if ($Task -eq 'help') {
    'Server commands: start, stop, restart, status, logs'
    'Tasks: install, dev, verify, build, test, e2e, browsers, preview, format'
    'Examples (inside frontend): .\dev.ps1 install; .\dev.ps1 dev; .\dev.ps1 verify'
    'Options: -Port 5173 -BackendUrl http://127.0.0.1:8081 -JavaHome <JDK>'
    'Start backend separately: backend/dev.ps1 demo. E2E requires a built backend JAR.'
    return
}
if ($Task -in @('start','stop','restart','status','logs')) {
    $options=@{Component='frontend';Action=$Task}
    foreach($name in @('Port','BackendUrl')) {
        if($PSBoundParameters.ContainsKey($name)){$options[$name]=$PSBoundParameters[$name]}
    }
    & (Join-Path (Split-Path $PSScriptRoot -Parent) 'scripts\Manage-Server.ps1') @options
    return
}
$node = (Get-Command node -ErrorAction Stop).Source
$npmCommand = Get-Command npm.cmd -ErrorAction SilentlyContinue
$portable = Join-Path $env:LOCALAPPDATA 'ClaimIQ\tools\npm-12.2.2'
$npmCli = Join-Path $portable 'package\bin\npm-cli.js'
if (!$npmCommand -and !(Test-Path -LiteralPath $npmCli)) {
    if ($Task -ne 'install') { throw 'Run .\dev.ps1 install first, or install Node.js with npm.' }
    # Official npm package, cached outside the repository; no global PATH changes.
    New-Item -ItemType Directory -Path $portable -Force | Out-Null
    $archive = Join-Path $portable 'npm.tgz'
    Invoke-WebRequest -Uri 'https://registry.npmjs.org/npm/-/npm-12.2.2.tgz' -OutFile $archive
    & tar.exe -xzf $archive -C $portable
    if ($LASTEXITCODE -ne 0 -or !(Test-Path -LiteralPath $npmCli)) { throw 'Could not unpack npm.' }
}
$oldBackend = $env:BACKEND_URL
$oldJava = $env:JAVA_HOME
Push-Location $PSScriptRoot
try {
    $env:BACKEND_URL = $BackendUrl
    if ($JavaHome) { $env:JAVA_HOME = (Resolve-Path -LiteralPath $JavaHome).Path }
    [string[]] $arguments = switch ($Task) {
        'install' { @('ci') }
        'dev' { @('run','dev','--','--port',"$Port") }
        'preview' { @('run','preview','--','--host','127.0.0.1','--port',"$Port") }
        'browsers' { @('exec','--','playwright','install','chromium') }
        'e2e' { @('run','test:e2e') }
        default { @('run',$Task) }
    }
    if ($npmCommand) { & $npmCommand.Source @arguments } else { & $node $npmCli @arguments }
    if ($LASTEXITCODE -ne 0) { throw "Frontend task '$Task' failed (exit $LASTEXITCODE)." }
} finally {
    Pop-Location
    $env:BACKEND_URL = $oldBackend
    $env:JAVA_HOME = $oldJava
}
