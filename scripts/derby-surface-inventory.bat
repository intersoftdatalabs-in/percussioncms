@echo off
REM Windows launcher for Derby surface inventory (#548 / T004)
SETLOCAL
cd /d "%~dp0\.."
python scripts\derby-surface-inventory.py %*
IF ERRORLEVEL 1 (
  python3 scripts\derby-surface-inventory.py %*
)
ENDLOCAL
