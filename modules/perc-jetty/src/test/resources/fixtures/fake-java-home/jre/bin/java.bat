@echo off
REM T058 fixture: fake `java` Windows cmd script for FR-013 layer-3 behavioral tests.
REM
REM Invocation: ...\fake-java-home\jre\bin\java.bat [-version]
REM
REM Emits a synthetic `openjdk version "X.Y.Z"` line on stderr so the
REM resolve-java-home.bat parser path runs end-to-end without a real JDK. The major
REM version is selected via the script's parent directory name suffix:
REM
REM   ...\fake-java-home-21\jre\bin\java.bat   -> emits "21.0.0" (success)
REM   ...\fake-java-home-8\jre\bin\java.bat    -> emits "8.0.0" (rejected)
REM   ...\fake-java-home-17\jre\bin\java.bat   -> emits "17.0.0" (rejected)
REM   ...\fake-java-home-22\jre\bin\java.bat   -> emits "22.0.0" (rejected)
REM
REM Exit code is always 0 so the parser path is exercised. The resolver runs
REM `java -version` and captures stderr; this script writes only to stderr
REM to mimic real-Java behavior.

setlocal EnableDelayedExpansion

REM Resolve the major version from the parent directory's basename: e.g.
REM C:\Temp\foo-21\jre\bin\java.bat -> 21.
set "PARENT_DIR=%~dp0"
for %%I in ("%PARENT_DIR%") do set "PARENT_DIR=%%~dpI"
for %%I in ("%PARENT_DIR:~0,-5%") do set "GRANDPARENT_DIR=%%~fI"

set "MAJOR=21"
echo %GRANDPARENT_DIR% | findstr /R /C:"-8$" >nul
if %ERRORLEVEL% EQU 0 set "MAJOR=8"
echo %GRANDPARENT_DIR% | findstr /R /C:"-17$" >nul
if %ERRORLEVEL% EQU 0 set "MAJOR=17"
echo %GRANDPARENT_DIR% | findstr /R /C:"-22$" >nul
if %ERRORLEVEL% EQU 0 set "MAJOR=22"

if "%MAJOR%"=="8"  echo openjdk version "8.0.0" 2026-01-01  1>&2
if "%MAJOR%"=="17" echo openjdk version "17.0.0" 2026-01-01 1>&2
if "%MAJOR%"=="21" echo openjdk version "21.0.0" 2026-01-01 1>&2
if "%MAJOR%"=="22" echo openjdk version "22.0.0" 2026-01-01 1>&2

exit /b 0