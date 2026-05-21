@echo off
cd /d "%~dp0"
python "popularize.py"
exit /b %ERRORLEVEL%

