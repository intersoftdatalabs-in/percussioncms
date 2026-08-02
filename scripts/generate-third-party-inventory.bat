@echo off
REM Windows launcher for generate-third-party-inventory.py (issue #1689)
SETLOCAL
cd /d "%~dp0\.."
python scripts\generate-third-party-inventory.py %*
IF ERRORLEVEL 1 (
  python3 scripts\generate-third-party-inventory.py %*
)
ENDLOCAL
