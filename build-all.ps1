# 自动构建 Sable Fly 三个 MC 版本（26.2 / 26.3 / 26.1.2）
# 用法：powershell -ExecutionPolicy Bypass -File build-all.ps1
# 说明：在单一工作区内依次切换 git 分支构建，产物收集到 dist/
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

$branches = @("mc26.2", "mc26.3", "mc26.1.2")   # mc26.2 = 26.2
$original = (git branch --show-current).Trim()

# 检查工作区干净（避免切换分支丢失改动）
$dirty = git status --porcelain
if ($dirty) {
    Write-Host "工作区有未提交改动，请先提交或 stash：" -ForegroundColor Red
    Write-Host $dirty
    exit 1
}

New-Item -ItemType Directory -Force -Path "dist" | Out-Null

foreach ($b in $branches) {
    Write-Host "==== 构建分支 $b ====" -ForegroundColor Cyan
    git checkout $b
    if ($LASTEXITCODE -ne 0) { throw "checkout $b failed" }

    .\gradlew.bat clean build --no-daemon --console=plain
    if ($LASTEXITCODE -ne 0) { throw "build failed on branch $b" }

    Get-ChildItem "build\libs\*.jar" | ForEach-Object {
        Copy-Item $_.FullName "dist\$($_.Name)" -Force
        Write-Host "  -> dist\$($_.Name)" -ForegroundColor Green
    }
}

git checkout $original
Write-Host "全部完成。产物在 dist/：" -ForegroundColor Cyan
Get-ChildItem "dist\sable-fly*.jar" | ForEach-Object { Write-Host "  $($_.Name)" }
