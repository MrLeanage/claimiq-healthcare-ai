param([ValidateSet('serve','train','test','start','stop','restart','status','logs')][string]$Task='serve', [int]$Port=8090, [string]$PythonExe='python')
$ErrorActionPreference='Stop'
if($Task -in @('start','stop','restart','status','logs')) {
    $options=@{Component='ai';Action=$Task}
    if($PSBoundParameters.ContainsKey('Port')){$options.Port=$Port}
    if($PSBoundParameters.ContainsKey('PythonExe')){$options.PythonExe=$PythonExe}
    & (Join-Path (Split-Path $PSScriptRoot -Parent) 'scripts\Manage-Server.ps1') @options
    return
}
Push-Location $PSScriptRoot
try {
    switch ($Task) {
        'serve' { & $PythonExe -u (Join-Path $PSScriptRoot 'serve.py') --port $Port }
        'train' { & $PythonExe train.py }
        'test' { & $PythonExe -m unittest discover -s tests -v }
    }
    if ($LASTEXITCODE -ne 0) { throw "AI service task failed: $Task" }
} finally { Pop-Location }
