$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
$git = "C:\Program Files\Git\cmd\git.exe"
$env:JAVA_HOME = "C:\Program Files\Zulu\zulu-25"

$branches = @("mc26.2", "mc26.3", "mc26.1.2")
$original = (& $git branch --show-current).Trim()

$dirty = & $git status --porcelain
if ($dirty -and $dirty.Trim().Length -gt 0) {
    Write-Host "Working tree is dirty. Commit or stash first."
    exit 1
}

New-Item -ItemType Directory -Force -Path "dist" | Out-Null

foreach ($b in $branches) {
    Write-Host "==== Build branch: $b ===="
    & $git checkout $b | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "checkout $b failed" }

    & cmd.exe /c "gradlew.bat clean build --no-daemon --console=plain"
    if ($LASTEXITCODE -ne 0) { throw "build failed on branch $b" }

    Get-ChildItem "build\libs\*.jar" | ForEach-Object {
        Copy-Item $_.FullName "dist\$($_.Name)" -Force
        Write-Host "  -> dist\$($_.Name)"
    }
}

& $git checkout $original | Out-Null
Write-Host "Done. Artifacts in dist/:"
Get-ChildItem "dist\sable-fly*.jar" | ForEach-Object { Write-Host "  $($_.Name)" }
