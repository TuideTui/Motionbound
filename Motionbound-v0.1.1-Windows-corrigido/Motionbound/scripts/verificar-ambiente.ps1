$root = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
Write-Host '==================================================' -ForegroundColor Cyan
Write-Host '  Motionbound | Diagnostico Windows' -ForegroundColor Cyan
Write-Host '==================================================' -ForegroundColor Cyan
Write-Host "Raiz: $root"
Write-Host "build.gradle presente: $(Test-Path (Join-Path $root 'build.gradle'))"
Write-Host "settings.gradle presente: $(Test-Path (Join-Path $root 'settings.gradle'))"
Write-Host ''
foreach ($name in @('java','javac','git','code')) {
    $cmd = Get-Command $name -ErrorAction SilentlyContinue
    if ($cmd) {
        Write-Host "[OK] $name = $($cmd.Source)" -ForegroundColor Green
        if ($name -eq 'java') { & $cmd.Source -version 2>&1 | Select-Object -First 1 | ForEach-Object { Write-Host "  $_" } }
        if ($name -eq 'javac') { & $cmd.Source -version 2>&1 | ForEach-Object { Write-Host "  $_" } }
    } else {
        Write-Host "[AVISO] $name nao encontrado no PATH" -ForegroundColor Yellow
    }
}
$compiler = Get-Command javac.exe -ErrorAction SilentlyContinue
if ($compiler) {
    $jdkHome = Split-Path -Parent (Split-Path -Parent $compiler.Source)
    Write-Host "JDK usado pelo iniciar-jogo.bat: $jdkHome" -ForegroundColor Green
}
Write-Host "JAVA_HOME global: $env:JAVA_HOME"
Write-Host "Sprites Kenney: $(Test-Path (Join-Path $root 'assets\kenney\tilemap_packed.png'))"
Write-Host "Git local inicializado: $(Test-Path (Join-Path $root '.git'))"
Write-Host 'Gradle 9.1 sera instalado automaticamente em .tools na primeira execucao.'
Write-Host 'O jogo usa o JDK selecionado por javac; Java 8 no PATH global nao impede a execucao.'
