# GhostQueue Application Launcher
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host " Launching GhostQueue - Healthcare Appointment Intelligence" -ForegroundColor White
Write-Host "========================================================" -ForegroundColor Cyan

$mavenPath = "$env:USERPROFILE\scoop\apps\maven\current\bin\mvn.cmd"

if (Test-Path $mavenPath) {
    & $mavenPath javafx:run
} else {
    mvn javafx:run
}
