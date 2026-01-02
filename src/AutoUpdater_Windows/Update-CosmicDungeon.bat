@echo off
REM Run the updater silently on login

powershell -WindowStyle Hidden -ExecutionPolicy Bypass ^
  -File "%~dp0Update-CosmicDungeon.ps1"
