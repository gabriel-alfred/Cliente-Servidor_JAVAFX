@echo off
echo ========================================
echo   Ejecutando Cliente JavaFX
echo ========================================
echo.
cd /d "%~dp0"
call mvn javafx:run
pause
