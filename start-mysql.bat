@echo off
setlocal
title Taskboard - MySQL
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-mysql.ps1"
if errorlevel 1 pause
endlocal
