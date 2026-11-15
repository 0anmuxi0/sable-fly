@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"
set "GIT=C:\Program Files\Git\cmd\git.exe"
set "JAVA_HOME=C:\Program Files\Zulu\zulu-25"

for /f "delims=" %%b in ('"%GIT%" branch --show-current') do set "ORIGINAL=%%b"
for /f "delims=" %%s in ('"%GIT%" status --porcelain') do (
    echo Working tree is dirty. Commit or stash first.
    exit /b 1
)

if not exist dist mkdir dist

for %%b in (mc26.2 mc26.3 mc26.1.2) do (
    echo ==== Build branch: %%b ====
    "%GIT%" checkout %%b
    if errorlevel 1 exit /b 1
    call gradlew.bat clean build --no-daemon --console=plain
    if errorlevel 1 exit /b 1
    for %%f in (build\libs\*.jar) do (
        copy /y "%%f" "dist\" >nul
        echo   -^> dist\%%~nxf
    )
)

"%GIT%" checkout !ORIGINAL!
echo Done. Artifacts in dist/:
dir /b dist\sable-fly*.jar
endlocal
