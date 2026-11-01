$ErrorActionPreference = 'Stop'
$root = (Resolve-Path "$PSScriptRoot\..").Path
$destination = Join-Path $root 'assets\kenney\tilemap_packed.png'
$downloadUrl = 'https://kenney.nl/media/pages/assets/tiny-dungeon/f8422efb44-1674742415/kenney_tiny-dungeon.zip'
$temp = Join-Path $env:TEMP 'motionbound-kenney-tiny-dungeon'
$archive = "$temp.zip"
try {
    Write-Host '[Motionbound] Baixando pack oficial Kenney Tiny Dungeon (CC0)...' -ForegroundColor Cyan
    if (Test-Path $temp) { Remove-Item $temp -Recurse -Force }
    if (Test-Path $archive) { Remove-Item $archive -Force }
    Invoke-WebRequest -Uri $downloadUrl -OutFile $archive -UseBasicParsing
    Expand-Archive -Path $archive -DestinationPath $temp -Force
    $atlas = Get-ChildItem -Path $temp -Filter 'tilemap_packed.png' -File -Recurse | Select-Object -First 1
    if (-not $atlas) { throw 'O download nao contem Tilemap/tilemap_packed.png. Use o link oficial e instale manualmente.' }
    New-Item -ItemType Directory -Force -Path (Split-Path $destination) | Out-Null
    Copy-Item -LiteralPath $atlas.FullName -Destination $destination -Force
    $license = Get-ChildItem -Path $temp -Filter 'License.txt' -File -Recurse | Select-Object -First 1
    if ($license) { Copy-Item -LiteralPath $license.FullName -Destination (Join-Path $root 'assets\kenney\License.txt') -Force }
    Write-Host "[OK] Kenney instalado em: $destination" -ForegroundColor Green
    Write-Host 'Reinicie o jogo. Para o JAR distribuivel, execute gerar-executavel.bat novamente.'
} catch {
    Write-Host "[AVISO] Download nao realizado: $_" -ForegroundColor Yellow
    Write-Host 'Baixe pelo navegador: https://kenney.nl/assets/tiny-dungeon'
    Write-Host 'Extraia o ZIP e copie Tilemap\tilemap_packed.png para assets\kenney\tilemap_packed.png'
    exit 1
} finally {
    if (Test-Path $temp) { Remove-Item $temp -Recurse -Force }
    if (Test-Path $archive) { Remove-Item $archive -Force }
}
