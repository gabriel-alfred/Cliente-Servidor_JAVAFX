@echo off
echo ========================================
echo   Ejecutando Servidor
echo ========================================
echo.
cd /d "%~dp0"
call mvn exec:java -Dexec.mainClass="server.ServerApp"
pause
