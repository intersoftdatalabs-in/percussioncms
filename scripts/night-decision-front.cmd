@echo off
REM Windows launcher for night-decision-front.py
SETLOCAL
cd /d "%~dp0\.."
python scripts\night-decision-front.py %*
IF ERRORLEVEL 1 (
  python3 scripts\night-decision-front.py %*
)
ENDLOCAL
