@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"
set "GIT=C:\Program Files\Git\cmd\git.exe"
set "JAVA_HOME=C:\Program Files\Zulu\zulu-25"

title Sable-Fly Launcher

call :banner
echo.
call :menu
echo.
choice /c 123 /n /m "  Select branch [1-3]: "

if errorlevel 3 (
    set "BRANCH=mc26.1.2"
    set "VER=26.1.2"
) else if errorlevel 2 (
    set "BRANCH=mc26.3"
    set "VER=26.3"
) else (
    set "BRANCH=mc26.2"
    set "VER=26.2"
)

echo.
call :info "!BRANCH!" "Minecraft !VER!"
echo.

REM ---- discard local run-game.bat changes so checkout never blocks ----
call :step "Preparing" "discarding local launcher changes"
"%GIT%" checkout -- run-game.bat >nul 2>&1

call :step "Switching" "git checkout !BRANCH!"
"%GIT%" checkout !BRANCH! >nul 2>&1
if errorlevel 1 (
    call :err "Checkout failed for !BRANCH!"
    echo.
    "%GIT%" status --short
    echo.
    call :warn "Commit or stash the changes above, then retry."
    pause
    exit /b 1
)
call :ok "Now on !BRANCH!"

echo.
call :step "Building" "gradlew build"
call gradlew.bat build --no-daemon --console=plain
if errorlevel 1 (
    call :err "Build failed."
    pause
    exit /b 1
)
call :ok "Build succeeded"

echo.
call :step "Launching" "Minecraft !VER!"
call :deco
call gradlew.bat runClient --no-daemon --console=plain
if errorlevel 1 (
    echo.
    call :err "Game exited with an error."
    pause
    exit /b 1
)
endlocal
exit /b 0


REM ================= helpers =================

:banner
powershell -NoProfile -Command "$e=[char]27; $r=$e+'[0m'; Write-Host ''; Write-Host ($e+'[96m  ____         _     _          _____ _       '+$r); Write-Host ($e+'[96m / ___|  __ _ | |__ | | ___    |  ___| |_   _ '+$r); Write-Host ($e+'[95m  \___ \ / _` | |_ \| |/ _ \   | |_  | | | | |'+$r); Write-Host ($e+'[96m   ___) | (_| || |_) | |  __/   |  _| | | |_| |'+$r); Write-Host ($e+'[92m  |____/ \__,_||_.__/|_|\___|   |_|   |_|\__, |'+$r); Write-Host ($e+'[92m                                        |___/  '+$r); Write-Host ''"
exit /b 0

:menu
powershell -NoProfile -Command "$e=[char]27; $r=$e+'[0m'; $d=$e+'[90m'; $w=$e+'[97m'; Write-Host ($d+'  ------------------------------------------------------------'+$r); Write-Host ('   '+$e+'[92m[1]'+$r+'  Minecraft '+$w+'26.2'+$r+'   '+$d+'- Create 6.0.9-2'+$r); Write-Host ('   '+$e+'[96m[2]'+$r+'  Minecraft '+$w+'26.3'+$r+'   '+$d+'- Create 6.0.9-2'+$r); Write-Host ('   '+$e+'[93m[3]'+$r+'  Minecraft '+$w+'26.1.2'+$r+' '+$d+'- Create 6.0.9-4'+$r); Write-Host ($d+'  ------------------------------------------------------------'+$r)"
exit /b 0

:step
powershell -NoProfile -Command "$e=[char]27; Write-Host ('  '+$e+'[94m>'+$e+'[0m '+$e+'[97m%~1'+$e+'[0m '+$e+'[90m%~2'+$e+'[0m')"
exit /b 0

:ok
powershell -NoProfile -Command "$e=[char]27; Write-Host ('  '+$e+'[92m[OK]'+$e+'[0m '+$e+'[92m%~1'+$e+'[0m')"
exit /b 0

:err
powershell -NoProfile -Command "$e=[char]27; Write-Host ('  '+$e+'[91m[ERROR]'+$e+'[0m '+$e+'[91m%~1'+$e+'[0m')"
exit /b 0

:warn
powershell -NoProfile -Command "$e=[char]27; Write-Host ('  '+$e+'[93m[WARN]'+$e+'[0m '+$e+'[93m%~1'+$e+'[0m')"
exit /b 0

:info
powershell -NoProfile -Command "$e=[char]27; Write-Host ('  '+$e+'[95m[BRANCH]'+$e+'[0m '+$e+'[97m%~1'+$e+'[0m '+$e+'[90m->'+$e+'[0m '+$e+'[92m%~2'+$e+'[0m')"
exit /b 0

:deco
powershell -NoProfile -Command "$e=[char]27; Write-Host ('  '+$e+'[95m'+('=' * 62)+$e+'[0m')"
exit /b 0