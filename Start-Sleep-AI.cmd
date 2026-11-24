@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\Start-Sleep-Server.ps1"
if errorlevel 1 pause
