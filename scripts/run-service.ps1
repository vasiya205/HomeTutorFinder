# Runs one backend service from the project root, so it reads the .env file there.
param([Parameter(Mandatory = $true)][string]$Service)
. "$PSScriptRoot\common.ps1"
Assert-Java
$jar = Join-Path $ProjectRoot "backend\$Service\target\$Service-1.0.0.jar"
if (-not (Test-Path $jar)) { throw "$jar not found. Run the 'Build backend' task first." }
if (-not (Test-Path (Join-Path $ProjectRoot '.env'))) { Copy-Item (Join-Path $ProjectRoot '.env.example') (Join-Path $ProjectRoot '.env') }
Set-Location $ProjectRoot
$Host.UI.RawUI.WindowTitle = $Service
& java -Xms64m -Xmx384m -jar $jar
