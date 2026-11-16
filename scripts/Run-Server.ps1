# Background worker; invoked only by Manage-Server.ps1 with a per-run configuration.
param([Parameter(Mandatory=$true)][string] $ConfigFile)
$ErrorActionPreference = 'Stop'
$config = Get-Content -LiteralPath $ConfigFile -Raw | ConvertFrom-Json
if ($config.component -notin @('backend','frontend','ai')) { throw 'Unknown component.' }
if ($config.component -eq 'ai') {
    & $config.pythonExe -u (Join-Path $config.projectRoot 'ai-service\serve.py') --port $config.port
    exit $LASTEXITCODE
}
$entry = Join-Path $config.projectRoot ($config.component + '\dev.ps1')
if ($config.component -eq 'backend') {
    $env:CLAIMIQ_DEMO_SESSION_FILE = $config.sessionFile
    if($config.aiUrl){$env:CLAIMIQ_AI_BASE_URL=$config.aiUrl}
    $options = @{ Task='demo'; Port=[int]$config.port; Database=$config.database }
    if ($config.javaHome) { $options.JavaHome=$config.javaHome }
    if ($config.authLibraryPath) { $options.AuthLibraryPath=$config.authLibraryPath }
} else {
    $options = @{ Task='dev'; Port=[int]$config.port; BackendUrl=$config.backendUrl }
}
& $entry @options
