@echo off
REM check-no-glob-deletes.bat — Windows counterpart for scripts/check-no-glob-deletes.sh.
REM
REM Delegates to the cross-platform Java main
REM (com.percussion.distribution.install.CheckNoGlobDeletes). The build-time
REM invocation is performed by the exec-maven-plugin java goal in pom.xml;
REM this shim is for operators and CI on Windows.
REM
REM Exit codes match the Java main exactly:
REM   0  no glob-based <delete> patterns found
REM   1  invocation error / file missing
REM   7  one or more <include> entries are glob patterns — the failure this
REM      script exists to catch
REM
REM Usage:
REM   check-no-glob-deletes.bat [same flags as check-no-glob-deletes.sh]

setlocal

set "SCRIPT_DIR=%~dp0"
set "MODULE_DIR=%SCRIPT_DIR%.."
set "JAR=%MODULE_DIR%\target\perc-distribution-tree.jar"

set "JAVA_BIN=java"
if not "%JAVA_HOME%"=="" (
    if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_BIN=%JAVA_HOME%\bin\java.exe"
) else if not "%JAVA_HOME_21%"=="" (
    if exist "%JAVA_HOME_21%\bin\java.exe" set "JAVA_BIN=%JAVA_HOME_21%\bin\java.exe"
)

"%JAVA_BIN%" -cp "%JAR%" com.percussion.distribution.install.CheckNoGlobDeletes %*
exit /b %ERRORLEVEL%
