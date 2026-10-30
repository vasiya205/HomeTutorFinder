# Installs frontend packages (first time) and starts the Vite dev server on http://localhost:5173
. "$PSScriptRoot\common.ps1"
$npm = Get-Npm
$frontend = Join-Path $ProjectRoot 'frontend'
Set-Location $frontend
if (-not (Test-Path (Join-Path $frontend 'node_modules'))) {
    Write-Step "Installing frontend packages (first time only)"
    & $npm install --no-fund --no-audit
    if ($LASTEXITCODE -ne 0) { throw "npm install failed." }
}
& $npm run dev
