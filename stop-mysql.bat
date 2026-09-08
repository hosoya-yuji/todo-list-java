@echo off
setlocal
title Taskboard - Stop MySQL App
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-mysql.ps1" -Stop
pause
endlocal
