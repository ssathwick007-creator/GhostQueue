@echo off
echo ========================================================
echo Launching GhostQueue - Healthcare Appointment Intelligence
echo ========================================================
echo.

set MAVEN_CMD=%USERPROFILE%\scoop\apps\maven\current\bin\mvn.cmd

if exist "%MAVEN_CMD%" (
    call "%MAVEN_CMD%" compile javafx:run
) else (
    mvn compile javafx:run
)

if %ERRORLEVEL% neq 0 (
    echo.
    echo Trying fallback launcher...
    java --module-path "target\lib;target\ghostqueue-app-1.0.0-SNAPSHOT.jar" --module com.ghostqueue/com.ghostqueue.MainApp
)

pause
