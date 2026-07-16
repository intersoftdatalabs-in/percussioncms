@echo off
REM Windows entry: harvest Kilo/GitHub review comments into Erlang pattern memory.
SETLOCAL
SET "ROOT=%~dp0.."
cd /d "%ROOT%"

where python >nul 2>&1
IF %ERRORLEVEL%==0 (
  python "%ROOT%\scripts\erlang-harvest-review-patterns.py" %*
  EXIT /B %ERRORLEVEL%
)

where python3 >nul 2>&1
IF %ERRORLEVEL%==0 (
  python3 "%ROOT%\scripts\erlang-harvest-review-patterns.py" %*
  EXIT /B %ERRORLEVEL%
)

echo erlang-harvest: python/python3 not found on PATH 1>&2
EXIT /B 1
