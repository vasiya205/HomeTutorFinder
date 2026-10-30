# Runs the backend unit tests.
. "$PSScriptRoot\common.ps1"
Assert-Java
$mvn = Get-Maven
& $mvn -f (Join-Path $ProjectRoot 'backend\pom.xml') -B -ntp test
exit $LASTEXITCODE
