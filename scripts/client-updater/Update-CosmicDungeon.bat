@echo off
setlocal
title Cosmic Dungeon TEST Client Updater
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0Update-CosmicDungeon.ps1"
set "CD_UPDATE_EXIT=%ERRORLEVEL%"
echo.
if not "%CD_UPDATE_EXIT%"=="0" echo Update did not complete. Read the message above before starting Minecraft.
pause
exit /b %CD_UPDATE_EXIT%
