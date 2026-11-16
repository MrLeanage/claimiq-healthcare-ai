# Starts a loopback-only demo with one-hour JWTs. SQL Server storage is the default.
param(
    [ValidateRange(1, 65535)][int] $Port = 8081,
    [ValidateSet('SqlServer','Memory')][string] $Database = 'SqlServer',
    [string] $AuthLibraryPath
)
$ErrorActionPreference = 'Stop'
$profiles = 'demo'
$previousJavaToolOptions = $env:JAVA_TOOL_OPTIONS
$demoJavaToolOptions = $previousJavaToolOptions
if ($Database -eq 'SqlServer') {
    $profiles = 'demo,demo-sqlserver'
    if (!$AuthLibraryPath) { $AuthLibraryPath = $env:CLAIMIQ_SQL_AUTH_DIR }
    $settingsFile = Join-Path $env:LOCALAPPDATA 'ClaimIQ\backend.json'
    if (!$AuthLibraryPath -and (Test-Path -LiteralPath $settingsFile)) {
        $AuthLibraryPath = (Get-Content -LiteralPath $settingsFile -Raw | ConvertFrom-Json).authLibraryPath
    }
    if (!$AuthLibraryPath) { throw 'Set up the JDBC DLL folder: scripts\Configure-SqlServer.ps1 -AuthLibraryPath <actual x64 DLL folder>. Or use -Database Memory.' }
    $authDirectory = (Resolve-Path -LiteralPath $AuthLibraryPath).Path
    if (!(Get-ChildItem -LiteralPath $authDirectory -Filter 'mssql-jdbc_auth-*.x64.dll' -File)) {
        throw 'The auth folder must contain the matching Microsoft JDBC x64 authentication DLL.'
    }
    # Environment parsing preserves spaces through Maven's Windows command wrapper.
    $demoJavaToolOptions = ($previousJavaToolOptions + ' -Djava.library.path="' + $authDirectory + '"').Trim()
}
$key = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($key)
$rng.Dispose()
$previousSecret = $env:CLAIMIQ_DEMO_JWT_SECRET
function ConvertTo-Base64Url([byte[]] $Bytes) {
    [Convert]::ToBase64String($Bytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
}
function New-DemoToken([string] $Subject, [string] $Scopes) {
    $now = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    $header = ConvertTo-Base64Url ([Text.Encoding]::UTF8.GetBytes('{"alg":"HS256","typ":"JWT"}'))
    # JWT timestamps are Unix seconds. 3600 gives one hour; restarting rotates the demo key.
    $payload = @{ sub=$Subject; scope=$Scopes; iss='claimiq-demo'; aud='claimiq-api'; iat=$now; exp=($now+3600) } | ConvertTo-Json -Compress
    $body = "$header.$(ConvertTo-Base64Url ([Text.Encoding]::UTF8.GetBytes($payload)))"
    $hmac = New-Object System.Security.Cryptography.HMACSHA256
    $hmac.Key = $key
    try { "$body.$(ConvertTo-Base64Url ($hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($body))))" }
    finally { $hmac.Dispose() }
}
try {
    $env:JAVA_TOOL_OPTIONS = $demoJavaToolOptions
    $env:CLAIMIQ_DEMO_JWT_SECRET = [Convert]::ToBase64String($key)
    Write-Host "Storage: $Database"
    Write-Host "Swagger: http://localhost:$Port/swagger-ui/index.html"
    Write-Host 'In Authorize, paste a token without the Bearer prefix. Tokens expire after one hour.'
    $writerToken=New-DemoToken 'demo-writer' 'claims:read claims:write'
    $reviewerToken=New-DemoToken 'demo-reviewer' 'claims:read claims:review'
    if($env:CLAIMIQ_DEMO_SESSION_FILE) {
        @{writer=$writerToken;reviewer=$reviewerToken;expiresAt=([DateTimeOffset]::UtcNow.ToUnixTimeSeconds()+3600)} |
            ConvertTo-Json | Set-Content -LiteralPath $env:CLAIMIQ_DEMO_SESSION_FILE
    }
    Write-Host "Writer (create/read/submit):`n$writerToken"
    Write-Host "Reviewer (read/review):`n$reviewerToken"
    Push-Location (Split-Path $PSScriptRoot -Parent)
    try {
        & .\mvnw.cmd '-B' 'spring-boot:run' "-Dspring-boot.run.profiles=$profiles" "-Dspring-boot.run.arguments=--server.port=$Port"
        if ($LASTEXITCODE -ne 0) { throw "Demo exited with code $LASTEXITCODE" }
    } finally { Pop-Location }
} finally {
    if($env:CLAIMIQ_DEMO_SESSION_FILE -and (Test-Path -LiteralPath $env:CLAIMIQ_DEMO_SESSION_FILE)) { Remove-Item -LiteralPath $env:CLAIMIQ_DEMO_SESSION_FILE }
    $env:CLAIMIQ_DEMO_JWT_SECRET = $previousSecret
    $env:JAVA_TOOL_OPTIONS = $previousJavaToolOptions
}
