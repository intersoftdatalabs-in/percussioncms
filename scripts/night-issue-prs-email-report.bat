@echo off
REM Email night-issue-prs scratch report to a Teams channel address.
REM To: -To or env NIGHT_ISSUE_PRS_TEAMS_EMAIL (skip with exit 0 if unset).
REM Unix parity: pwsh scripts/night-issue-prs-email-report.ps1 ...
REM Usage:
REM   scripts\night-issue-prs-email-report.bat -ReportPath tmp\night-issue-prs-report.md
REM   scripts\night-issue-prs-email-report.bat -SelfTest
setlocal EnableExtensions
set "SCRIPT=%~dp0night-issue-prs-email-report.ps1"
where pwsh >nul 2>&1
if not errorlevel 1 (
  pwsh -NoProfile -File "%SCRIPT%" %*
  exit /b %ERRORLEVEL%
)
powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT%" %*
exit /b %ERRORLEVEL%
