@echo off
REM Windows launcher for typesafe-prescreen.py
REM
REM Deterministic + TypeSafe jev pre-screen for the night-issue-prs workflow.
REM Fails open: without a TYPESAFE_API_KEY (or on any API/network error) the
REM script still writes prescreen.json with the deterministic label rules.
REM
REM Usage:
REM   scripts\typesafe-prescreen.cmd --inventory scratch\issues-raw.json --out scratch\prescreen.json
REM
REM Exit code:
REM   0  wrote --out (normal or fallback_rule_only)
REM   2  usage error (bad args / unreadable inventory / bad answers file)
SETLOCAL
cd /d "%~dp0\.."
python scripts\typesafe-prescreen.py %*
IF ERRORLEVEL 1 (
  python3 scripts\typesafe-prescreen.py %*
)
ENDLOCAL