#!/usr/bin/env sh
# verify-no-finder-jsp-references.bat (T029b, feature 992-react-content-explorer).
#
# Windows counterpart to scripts/verify-no-finder-jsp-references.sh.
# Per the project's cross-platform rule (root AGENTS.md), every
# required POSIX script has a .bat counterpart or a cross-platform
# runner so Windows CI agents can run the gate.
#
# Run from the repo root:
#   scripts\verify-no-finder-jsp-references.bat
@echo off
setlocal

set "REPO_ROOT=%~dp0.."
pushd "%REPO_ROOT%" >nul

set "FAIL=0"
set "TARGET_JSP=WebUI\src\main\webapp\cm\app\webmgt.jsp"

echo ==^> checking %TARGET_JSP% for finder.jsp navigation entries

REM Use findstr (Windows port of grep) with regex via /R. The pattern
REM matches the literal navigation-entry forms (see .sh for the
REM rationale and carve-outs). findstr does NOT have a portable way
REM to strip JSP comments inline, so we use a small PowerShell one-liner
REM to strip <%-- ... --%> blocks before the findstr invocation.
for %%F in ("%TARGET_JSP%") do (
    if not exist "%%~fF" (
        echo   FAIL: target JSP does not exist: %%~fF 1>&2
        set "FAIL=1"
    ) else (
        for /f "usebackq tokens=*" %%M in (
            `powershell -NoProfile -Command "Get-Content -LiteralPath '%%~fF' | ForEach-Object { $_ -replace '<%--.*?--%>','' } | Select-String -Pattern '<jsp:include[ \t]+page=\"includes/finder\.jsp|<%%@include[ \t]+file=\"includes/finder\.jsp'"`
        ) do (
            echo   FAIL: %%~fF contains finder.jsp navigation entry: 1>&2
            echo     %%M 1>&2
            set "FAIL=1"
        )
    )
)

if "%FAIL%"=="1" (
    echo verify-no-finder-jsp-references: FAIL 1>&2
    popd >nul
    exit /b 1
)
echo verify-no-finder-jsp-references: PASS
popd >nul
exit /b 0