@echo off
REM Windows launcher for night-rtk-metrics.py
SETLOCAL
cd /d "%~dp0\.."
python scripts\night-rtk-metrics.py %*
IF ERRORLEVEL 1 (
  python3 scripts\night-rtk-metrics.py %*
)
ENDLOCAL
