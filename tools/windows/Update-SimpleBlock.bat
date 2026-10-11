@echo off
rem Double-click to install the latest successful Simple Block build into your Minecraft game directory.
rem Settings: Update-SimpleBlock.settings.ini in this folder. Setup guide: docs\PLAYTESTING_SETUP.md
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0Update-SimpleBlock.ps1" %*
set "UPDATE_EXIT_CODE=%ERRORLEVEL%"
echo.
pause
exit /b %UPDATE_EXIT_CODE%
