@echo off
REM verify-no-finder-jsp-references.bat (T029b, feature 992-react-content-explorer).
REM
REM Windows counterpart to scripts/verify-no-finder-jsp-references.sh.
REM Per the project's cross-platform rule (root AGENTS.md), every
REM required POSIX script has a .bat counterpart or a cross-platform
REM runner so Windows CI agents can run the gate.
REM
REM Implementation note: PowerShell is the parsing engine (NOT
REM findstr). findstr has no portable way to handle multi-line
REM <%-- ... --%> JSP comment blocks; PowerShell's [regex]::Replace
REM with the (?s) inline flag is the equivalent of the POSIX
REM `perl -0777 -pe 's/<%--.*?--%>//gs'` step in the .sh counterpart.
REM Without this, the US6 cutover comment at
REM WebUI/src/main/webapp/cm/app/webmgt.jsp:330-340 (which spans
REM multiple lines and contains `<jsp:include page="includes/finder.jsp"`
REM as a quoted substring of an explanatory comment) would
REM false-positive the gate on Windows. See the regex comment below.
REM
REM Run from the repo root:
REM   scripts\verify-no-finder-jsp-references.bat
setlocal

set "REPO_ROOT=%~dp0.."
pushd "%REPO_ROOT%" >nul

set "FAIL=0"
set "TARGET_JSP=WebUI\src\main\webapp\cm\app\webmgt.jsp"

echo ==^> checking %TARGET_JSP% for finder.jsp navigation entries

REM The PowerShell pipeline below is the gate's actual detection
REM logic. Three stages:
REM   1. [System.IO.File]::ReadAllText slurps the whole file as a
REM      single string (equivalent to POSIX `perl -0777`).
REM   2. [regex]::Replace with the (?s) inline flag makes `.` match
REM      newlines; this strips every <%-- ... --%> JSP comment
REM      block, including the multi-line US6 cutover comment that
REM      contains a literal `<jsp:include page="includes/finder.jsp">`
REM      substring as a documentation example.
REM   3. Select-String then matches the navigation-entry forms:
REM        <jsp:include page="includes/finder.jsp" ...>
REM        <%@include file="includes/finder.jsp" ...>
REM      After stage 2, any match is a genuine navigation entry (not
REM      a documentation example inside a comment).
for %%F in ("%TARGET_JSP%") do (
    if not exist "%%~fF" (
        echo   FAIL: target JSP does not exist: %%~fF 1>&2
        set "FAIL=1"
    ) else (
        for /f "usebackq tokens=*" %%M in (
            `powershell -NoProfile -Command "$content = [System.IO.File]::ReadAllText('%%~fF'); $stripped = [regex]::Replace($content, '(?s)<%%--.*?--%%>', ''); $stripped | Select-String -Pattern '<jsp:include[ \t]+page=\"includes/finder\.jsp|<%%@include[ \t]+file=\"includes/finder\.jsp'"`
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