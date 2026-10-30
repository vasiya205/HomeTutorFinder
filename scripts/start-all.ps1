# Alternative to the VS Code task: builds, then opens one PowerShell window per service + the frontend.
. "$PSScriptRoot\common.ps1"
& "$PSScriptRoot\build-backend.ps1"
$services = 'eureka-server', 'auth-service', 'tutor-service', 'booking-service', 'notification-service', 'api-gateway'
foreach ($service in $services) {
    Start-Process powershell -ArgumentList '-NoExit', '-ExecutionPolicy', 'Bypass', '-File', "$PSScriptRoot\run-service.ps1", '-Service', $service
    Start-Sleep -Seconds 2
}
Start-Process powershell -ArgumentList '-NoExit', '-ExecutionPolicy', 'Bypass', '-File', "$PSScriptRoot\run-frontend.ps1"
Write-Host "Starting... open http://localhost:5173 in about 60 seconds." -ForegroundColor Green
