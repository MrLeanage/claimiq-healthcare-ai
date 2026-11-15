param([ValidateSet('serve','train','test')][string]$Task='serve', [int]$Port=8090)
$ErrorActionPreference='Stop'
Push-Location $PSScriptRoot
try {
    switch ($Task) {
        'serve' { python -m app.server --port $Port }
        'train' { python train.py }
        'test' { python -m unittest discover -s tests -v }
    }
    if ($LASTEXITCODE -ne 0) { throw "AI service task failed: $Task" }
} finally { Pop-Location }
