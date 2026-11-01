@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"
echo.
echo [Motionbound] Iniciador na pasta: %CD%
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\rodar.ps1"
if errorlevel 1 (
    echo.
    echo [ERRO] Motionbound nao iniciou. Envie uma imagem desta janela.
    pause
)
endlocal
