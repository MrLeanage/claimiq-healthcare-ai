[CmdletBinding()]
param(
    [Parameter(Position=0)]
    [ValidateSet('help','info','test','verify','package','dependencies','clean','demo','local','sql-test','start','stop','restart','status','logs','tokens')]
    [string] $Task = 'help',
    [ValidateRange(1,65535)][int] $Port,
    [string] $JavaHome,
    [string] $AuthLibraryPath,
    [ValidateSet('SqlServer','Memory')][string] $Database = 'SqlServer',
    [switch] $Docs
)
$ErrorActionPreference = 'Stop'
$backend = Split-Path $PSScriptRoot -Parent
if ($Task -eq 'help') {
    Write-Output 'Server commands: start, stop, restart, status, logs, tokens'
    Write-Output 'Tasks: info, test, verify, package, dependencies, clean, demo, local, sql-test'
    Write-Output 'Options: -Port <number> -JavaHome <JDK directory> -AuthLibraryPath <DLL directory> -Database SqlServer|Memory -Docs (local only)'
    Write-Output 'Examples: .\dev.ps1 demo | .\dev.ps1 verify | .\dev.ps1 local -Docs'
    Write-Output 'SQL tests require CLAIMIQ_IT_ADMIN_URL for a development/test instance.'
    Write-Output 'Full instructions: scripts/README.md (inside backend)'
    return
}
if ($Task -in @('start','stop','restart','status','logs','tokens')) {
    $options=@{Component='backend';Action=$Task}
    foreach($name in @('Port','JavaHome','AuthLibraryPath','Database')) {
        if($PSBoundParameters.ContainsKey($name)){$options[$name]=$PSBoundParameters[$name]}
    }
    & (Join-Path (Split-Path $backend -Parent) 'scripts\Manage-Server.ps1') @options
    return
}
$previousJavaHome = $env:JAVA_HOME
$previousJavaToolOptions = $env:JAVA_TOOL_OPTIONS
try {
    if ($JavaHome) {
        $resolvedJava = (Resolve-Path -LiteralPath $JavaHome).Path
        if (!(Test-Path -LiteralPath (Join-Path $resolvedJava 'bin\java.exe'))) {
            throw 'JavaHome must contain bin\java.exe.'
        }
        $env:JAVA_HOME = $resolvedJava
    }
    if ($AuthLibraryPath) {
        $authDirectory = (Resolve-Path -LiteralPath $AuthLibraryPath).Path
        $env:JAVA_TOOL_OPTIONS = ($previousJavaToolOptions + ' -Djava.library.path="' + $authDirectory + '"').Trim()
        if (!(Test-Path -LiteralPath $authDirectory -PathType Container)) {
            throw 'AuthLibraryPath must be a directory containing the JDBC authentication DLL.'
        }
    }
    if ($Task -eq 'demo') {
        if (!$Port) { $Port = 8081 }
        & (Join-Path $PSScriptRoot 'Start-Demo.ps1') -Port $Port -Database $Database -AuthLibraryPath $AuthLibraryPath
        return
    }
    $mavenArguments = @('-B')
    switch ($Task) {
        'info' { $mavenArguments += '--version' }
        'test' { $mavenArguments += 'test' }
        'verify' { $mavenArguments += @('clean','verify') }
        'package' { $mavenArguments += 'package' }
        'dependencies' { $mavenArguments += 'dependency:resolve' }
        'clean' { $mavenArguments += 'clean' }
        'local' {
            if (!$Port) { $Port = 8080 }
            $profiles = if ($Docs) { 'local,docs' } else { 'local' }
            $mavenArguments += @('spring-boot:run', "-Dspring-boot.run.profiles=$profiles",
                "-Dspring-boot.run.arguments=--server.port=$Port")
        }
        'sql-test' {
            if (!$env:CLAIMIQ_IT_ADMIN_URL -or !$env:CLAIMIQ_IT_ADMIN_URL.StartsWith('jdbc:sqlserver://')) {
                throw 'Set CLAIMIQ_IT_ADMIN_URL to a SQL Server development/test instance first. See backend/scripts/README.md.'
            }
            $mavenArguments += @('-Psqlserver-it','clean','verify')
        }
    }
    Push-Location $backend
    try {
        & .\mvnw.cmd @mavenArguments
        if ($LASTEXITCODE -ne 0) { throw "Backend task '$Task' failed (exit code $LASTEXITCODE)." }
    } finally { Pop-Location }
} finally {
    $env:JAVA_HOME = $previousJavaHome
    $env:JAVA_TOOL_OPTIONS = $previousJavaToolOptions
}
