Write-Host "=======================================================================" -ForegroundColor Cyan
Write-Host "  Starting GhostQueue Cloud Web Server on http://localhost:8080/" -ForegroundColor Green
Write-Host "=======================================================================" -ForegroundColor Cyan
mvn compile exec:java@web
