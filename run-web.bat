@echo off
echo =======================================================================
echo   Starting GhostQueue Cloud Web Server on http://localhost:8080/
echo =======================================================================
mvn compile exec:java@web
pause
