@echo off
REM Timer entry point for the opencode night-issue-prs workflow (Windows).
REM One bounded cycle per invocation (default --max-issues 5). Run via Task
REM Scheduler with "Do not start a new instance" so overlapping triggers are
REM skipped. A mkdir-based lock guards timer-vs-manual overlap.
REM Usage: scripts\night-timer-run.cmd [--max-issues N] [launcher args...]
setlocal
set "SCRIPT_DIR=%~dp0"
set "REPO_ROOT=%SCRIPT_DIR%.."
set "LOCK_DIR=%REPO_ROOT%\tmp\night-issue-prs.lock"
if "%MAX_ISSUES%"=="" set "MAX_ISSUES=5"

if not exist "%REPO_ROOT%\tmp" mkdir "%REPO_ROOT%\tmp" 2>nul
mkdir "%LOCK_DIR%" 2>nul
if %ERRORLEVEL% neq 0 (
  echo night-issue-prs cycle already running, skipping.
  exit /b 0
)

if "%NIGHT_WORKTREE_PATH%"=="" set "NIGHT_WORKTREE_PATH=%USERPROFILE%\.opencode\worktrees\intersoft-workspace-percussioncms\night-issue-prs"
for /f %%t in ('powershell -NoProfile -Command "Get-Date -AsUTC -Format yyyyMMddTHHmmssZ"') do set "STAMP=%%t"
set "NIGHT_REPORT_PATH=%NIGHT_WORKTREE_PATH%\scratch\night-report-%STAMP%.md"
pushd "%REPO_ROOT%" || (rmdir "%LOCK_DIR%" & exit /b 1)
python scripts\opencode-night-issue-prs.py --max-issues %MAX_ISSUES% %*
set "RC=%ERRORLEVEL%"
popd
rmdir "%LOCK_DIR%"
exit /b %RC%
