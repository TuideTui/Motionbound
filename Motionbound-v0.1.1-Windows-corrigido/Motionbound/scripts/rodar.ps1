param([switch]$BuildOnly)
$ErrorActionPreference = 'Stop'
$root = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
Set-Location -LiteralPath $root

Write-Host ''
Write-Host '========================================================' -ForegroundColor Cyan
Write-Host ' MOTIONBOUND | Inicio do projeto Windows' -ForegroundColor Cyan
Write-Host '========================================================' -ForegroundColor Cyan
Write-Host "Pasta do projeto: $root"

foreach ($file in @('build.gradle', 'settings.gradle', 'src\main\java\br\edu\motionbound\DesktopLauncher.java')) {
    if (-not (Test-Path -LiteralPath (Join-Path $root $file))) {
        Write-Host "[ERRO] Arquivo obrigatorio ausente: $file" -ForegroundColor Red
        Write-Host 'Extraia a pasta Motionbound inteira do novo ZIP. Nao abra um diretorio acima ou abaixo.'
        exit 1
    }
}

# The PC has old Oracle Java 8 earlier in PATH, but javac 25 is installed.
# Select java.exe from the very same JDK folder as javac.exe, only for this process.
$compiler = Get-Command javac.exe -ErrorAction SilentlyContinue
if (-not $compiler) {
    Write-Host '[ERRO] O compilador javac nao foi encontrado.' -ForegroundColor Red
    Write-Host 'Instale JDK 17+ e execute verificar-ambiente.bat.'
    exit 1
}
$jdkBin = Split-Path -Parent $compiler.Source
$jdkHome = Split-Path -Parent $jdkBin
$javaExe = Join-Path $jdkBin 'java.exe'
if (-not (Test-Path -LiteralPath $javaExe)) {
    Write-Host "[ERRO] java.exe nao encontrado em $jdkBin" -ForegroundColor Red
    exit 1
}
$javacOutput = (& $compiler.Source -version 2>&1 | Out-String).Trim()
if ($javacOutput -match 'javac\s+(\d+)') {
    $javaMajor = [int]$Matches[1]
} else {
    Write-Host "[ERRO] Versao Java desconhecida: $javacOutput" -ForegroundColor Red
    exit 1
}
if ($javaMajor -lt 17) {
    Write-Host "[ERRO] Foi encontrado JDK $javaMajor; o Motionbound exige JDK 17+." -ForegroundColor Red
    exit 1
}
$env:JAVA_HOME = $jdkHome
$env:PATH = "$jdkBin;$env:PATH"
Write-Host "[OK] Usando o $javacOutput (temporariamente, apenas neste script)." -ForegroundColor Green
Write-Host "[OK] JAVA_HOME = $jdkHome"

# Gradle 9.1.0 supports running on Java 25. Downloaded locally, no global install.
$gradleVersion = '9.1.0'
$toolDir = Join-Path $root '.tools'
$gradleBat = Join-Path $toolDir "gradle-$gradleVersion\bin\gradle.bat"
if (-not (Test-Path -LiteralPath $gradleBat)) {
    New-Item -ItemType Directory -Force -Path $toolDir | Out-Null
    $archive = Join-Path $toolDir "gradle-$gradleVersion-bin.zip"
    Write-Host "[Motionbound] Baixando Gradle $gradleVersion (somente na primeira execucao)..." -ForegroundColor Cyan
    try {
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip" -OutFile $archive -UseBasicParsing
        Expand-Archive -Path $archive -DestinationPath $toolDir -Force
        Remove-Item -LiteralPath $archive -Force
    } catch {
        Write-Host "[ERRO] Falha ao baixar Gradle: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host 'Confira sua internet e tente novamente.'
        exit 1
    }
}

# Explicit -p fixes the exact error in the user's screenshot:
# even if the VS Code terminal is opened in the parent directory, Gradle uses the correct build.gradle.
$task = if ($BuildOnly) { 'fatJar' } else { 'run' }
Write-Host "[Motionbound] Gradle --project-dir `"$root`" $task" -ForegroundColor Cyan
& $gradleBat --project-dir "$root" $task --console=plain
$code = $LASTEXITCODE
if ($code -ne 0) {
    Write-Host "[ERRO] Falha na tarefa '$task' (codigo $code)." -ForegroundColor Red
    Write-Host 'Copie a mensagem de erro completa deste terminal para diagnostico.'
    exit $code
}
if ($BuildOnly) {
    Write-Host "[OK] Executavel: $root\build\libs\Motionbound-0.1.1-all.jar" -ForegroundColor Green
} else {
    Write-Host '[OK] O jogo foi encerrado normalmente.' -ForegroundColor Green
}
