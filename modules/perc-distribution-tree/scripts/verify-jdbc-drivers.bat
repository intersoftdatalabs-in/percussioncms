@echo off
REM verify-jdbc-drivers.bat — Windows counterpart for scripts/verify-jdbc-drivers.sh.
REM
REM Delegates to the cross-platform Java main
REM (com.percussion.distribution.install.VerifyJdbcDrivers) so CI on Windows,
REM Linux, and macOS produce identical exit codes (see scripts/README.md).
REM
REM Use this script for manual verification runs on Windows; build-time
REM invocation is performed by the exec-maven-plugin java goal in pom.xml,
REM so this shim is for operators, not for the build itself.
REM
REM Exit codes match the Java main exactly:
REM   0  all checks passed
REM   1  invocation error / missing tool
REM   2  jdbc/ missing or empty
REM   3  one or more JARs are zero-byte
REM   4  one or more JARs are not valid Java archives
REM   5  artifact could not be unpacked
REM   6  expected-driver-set / expected-driver-glob mismatch
REM
REM Usage:
REM   verify-jdbc-drivers.bat [same flags as verify-jdbc-drivers.sh]

setlocal

set "SCRIPT_DIR=%~dp0"
set "MODULE_DIR=%SCRIPT_DIR%.."
set "JAR=%MODULE_DIR%\target\perc-distribution-tree.jar"

REM Find a Java runtime: JAVA_HOME first, else PATH, else JAVA_HOME_21 (project convention).
set "JAVA_BIN=java"
if not "%JAVA_HOME%"=="" (
    if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_BIN=%JAVA_HOME%\bin\java.exe"
) else if not "%JAVA_HOME_21%"=="" (
    if exist "%JAVA_HOME_21%\bin\java.exe" set "JAVA_BIN=%JAVA_HOME_21%\bin\java.exe"
)

REM Surface args after class name; forward everything to the Java main unchanged.
"%JAVA_BIN%" -cp "%JAR%" com.percussion.distribution.install.VerifyJdbcDrivers %*
exit /b %ERRORLEVEL%
