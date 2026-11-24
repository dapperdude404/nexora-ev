@echo off
cd /d "%~dp0.."
gradle assembleDebug
exit /b %errorlevel%
