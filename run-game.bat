@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"
set "GIT=C:\Program Files\Git\cmd\git.exe"
set "JAVA_HOME=C:\Program Files\Zulu\zulu-25"

echo ============================================
echo    Sable Fly  -  choose MC version
echo ============================================
echo.
echo    [1]  Minecraft 26.2
echo    [2]  Minecraft 26.3
echo    [3]  Minecraft 26.1.2
echo.
echo    Press numpad 1 / 2 / 3 ...
echo.
choice /c 123 /n /m "Select version: "

if errorlevel 3 (
    set "BRANCH=mc26.1.2"
) else if errorlevel 2 (
    set "BRANCH=mc26.3"
) else (
    set "BRANCH=mc26.2"
)

echo.
echo Selected: !BRANCH!
echo.
"%GIT%" checkout !BRANCH!
if errorlevel 1 (
    echo Failed to checkout !BRANCH!
    pause
    exit /b 1
)

echo ==== Building !BRANCH! ====
call gradlew.bat build --no-daemon --console=plain
if errorlevel 1 (
    echo Build failed.
    pause
    exit /b 1
)

echo.
echo ==== Launching game (!BRANCH!) ====
call gradlew.bat runClient --no-daemon --console=plain
if errorlevel 1 (
    echo Game exited with an error.
    pause
    exit /b 1
)
endlocal
