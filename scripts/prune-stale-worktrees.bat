@echo off
REM Windows launcher for prune-stale-worktrees.py
SETLOCAL
cd /d "%~dp0\.."
python scripts\prune-stale-worktrees.py %*
IF ERRORLEVEL 1 (
  python3 scripts\prune-stale-worktrees.py %*
)
ENDLOCAL
