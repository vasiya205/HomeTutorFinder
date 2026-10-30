# Quick health check of the local setup and the running services.
. "$PSScriptRoot\common.ps1"
function Check($label, [scriptblock]$test) {
    try { $result = & $test; Write-Host ("[ OK ] {0} {1}" -f $label, $result) -ForegroundColor Green }
    catch { Write-Host ("[FAIL] {0} - {1}" -f $label, $_.Exception.Message) -ForegroundColor Red }
}
Check 'Java 21+' { Assert-Java; ((Get-JavaVersion) -split "`n")[0] }
Check 'Node.js' { & (Get-Npm) --version | Out-Null; (& node --version) }
Check '.env file' { if (-not (Test-Path (Join-Path $ProjectRoot '.env'))) { throw 'missing - copy .env.example to .env' }; 'found' }
Check 'PostgreSQL on localhost:5432' {
    $client = New-Object Net.Sockets.TcpClient
    try { $client.Connect('localhost', 5432); 'reachable' } finally { $client.Close() }
}
$ports = [ordered]@{ 'eureka-server' = 8761; 'api-gateway' = 8080; 'auth-service' = 8081; 'tutor-service' = 8082; 'booking-service' = 8083; 'notification-service' = 8084 }
foreach ($name in $ports.Keys) {
    Check "$name (:$($ports[$name]))" { (Invoke-RestMethod -TimeoutSec 3 "http://localhost:$($ports[$name])/actuator/health").status }
}
Check 'Frontend (:5173)' { (Invoke-WebRequest -UseBasicParsing -TimeoutSec 3 'http://localhost:5173').StatusCode }
