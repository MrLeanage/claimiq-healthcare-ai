[CmdletBinding()]
param([Parameter(Mandatory=$true)][string] $AuthLibraryPath)
$ErrorActionPreference = 'Stop'
$resolved = (Resolve-Path -LiteralPath $AuthLibraryPath).Path
if (!(Get-ChildItem -LiteralPath $resolved -Filter 'mssql-jdbc_auth-*.x64.dll' -File)) {
    throw 'Choose the x64 folder containing the matching Microsoft JDBC authentication DLL.'
}
$directory = Join-Path $env:LOCALAPPDATA 'ClaimIQ'
New-Item -ItemType Directory -Force -Path $directory | Out-Null
@{ authLibraryPath = $resolved } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $directory 'backend.json')
Write-Host 'Saved the JDBC DLL folder for this Windows user. Run .\dev.ps1 demo inside backend.'
