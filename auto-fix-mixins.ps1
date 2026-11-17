param([int]$MaxRounds = 30)
$ErrorActionPreference = 'Continue'
$root = 'C:\Users\_anmuxi_\Desktop\Sable-Fly'
Set-Location $root
$jsonPath = Join-Path $root 'src\main\resources\sable.mixins.json'
$logPath = Join-Path $root 'runs\client\logs\latest.log'
$removed = @()

for ($i = 1; $i -le $MaxRounds; $i++) {
    Write-Host "===== Round $i =====" -ForegroundColor Cyan
    if (Test-Path $logPath) { Remove-Item $logPath -Force -ErrorAction SilentlyContinue }
    $psi = Start-Process -FilePath 'cmd.exe' -ArgumentList '/c','gradlew.bat runClient --no-daemon' -WorkingDirectory $root -PassThru -WindowStyle Hidden -RedirectStandardOutput "$root\run-round.log" -RedirectStandardError "$root\run-round-err.log"
    $crashed = $false
    $mixinErr = $null
    for ($t = 0; $t -lt 60; $t++) {
        Start-Sleep -Seconds 3
        if ($psi.HasExited) {
            $out = ''
            if (Test-Path "$root\run-round.log") { $out = Get-Content "$root\run-round.log" -Raw -ErrorAction SilentlyContinue }
            if ($out -match 'Critical injection failure|InvalidInjectionException|Mixin apply for mod sable failed|could not find any targets|Unable to find|Mixin prepare for mod sable failed') {
                $crashed = $true
                if ($out -match 'sable\.mixins\.json:([\w.]+)') { $mixinErr = $Matches[1] }
                elseif ($out -match 'sable-fabric\.mixins\.json:([\w.]+)') { $mixinErr = $Matches[1] }
            }
            break
        }
        if (Test-Path $logPath) {
            $log = Get-Content $logPath -Raw -ErrorAction SilentlyContinue
            if ($log -match 'Sound engine started|Loaded \d+ shader sources') {
                Write-Host "[OK] Main menu reached at round $i" -ForegroundColor Green
                Stop-Process -Id $psi.Id -Force -ErrorAction SilentlyContinue
                exit 0
            }
        }
    }
    if (-not $crashed) {
        Write-Host "[WARN] Round $i no clear result" -ForegroundColor Yellow
        Stop-Process -Id $psi.Id -Force -ErrorAction SilentlyContinue
        break
    }
    Write-Host "[FAIL] $mixinErr" -ForegroundColor Red
    if (-not $mixinErr) { Stop-Process -Id $psi.Id -Force -ErrorAction SilentlyContinue; break }
    $j = Get-Content $jsonPath -Raw | ConvertFrom-Json
    $changed = $false
    $newMixins = @()
    $newClient = @()
    foreach ($m in $j.mixins) { if ($m -eq $mixinErr) { $changed = $true } else { $newMixins += $m } }
    foreach ($m in $j.client) { if ($m -eq $mixinErr) { $changed = $true } else { $newClient += $m } }
    if (-not $changed) { Write-Host "[WARN] $mixinErr not in json" -ForegroundColor Yellow; Stop-Process -Id $psi.Id -Force -ErrorAction SilentlyContinue; break }
    $j.mixins = $newMixins
    $j.client = $newClient
    $j | ConvertTo-Json -Depth 10 | Set-Content $jsonPath -Encoding UTF8
    $removed += $mixinErr
    Write-Host "[REMOVED] $mixinErr" -ForegroundColor Magenta
    Stop-Process -Id $psi.Id -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
}
Write-Host ""
Write-Host "===== Removed $($removed.Count) mixins =====" -ForegroundColor Cyan
$removed | ForEach-Object { Write-Host "  $_" }