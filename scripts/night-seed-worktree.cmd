@echo off
REM Windows launcher for night-seed-worktree.py
SETLOCAL
cd /d "%~dp0\.."
python scripts\night-seed-worktree.py %*
IF ERRORLEVEL 1 (
  python3 scripts\night-seed-worktree.py %*
)
ENDLOCAL
