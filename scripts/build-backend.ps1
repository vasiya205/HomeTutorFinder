# Builds all Spring Boot services into backend/<service>/target/<service>-1.0.0.jar
. "$PSScriptRoot\common.ps1"
Assert-Java
$mvn = Get-Maven
Write-Step "Building backend (first build downloads dependencies, takes a few minutes)"
& $mvn -f (Join-Path $ProjectRoot 'backend\pom.xml') -B -ntp -DskipTests package
if ($LASTEXITCODE -ne 0) { throw "Backend build failed (see the errors above)." }
Write-Host "Backend built successfully." -ForegroundColor Green
