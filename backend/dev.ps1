# Backend entry point. Arguments are validated by Invoke-Backend.ps1.
& (Join-Path $PSScriptRoot 'scripts\Invoke-Backend.ps1') @args
