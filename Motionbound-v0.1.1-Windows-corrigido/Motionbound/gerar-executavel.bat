@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\rodar.ps1" -BuildOnly
if errorlevel 1 pause
endlocal
