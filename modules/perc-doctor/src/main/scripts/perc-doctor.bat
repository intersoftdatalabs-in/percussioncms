@echo off
REM Copyright (c) 2026 Intersoft Data Labs, Inc.
REM
REM Licensed under the Apache License, Version 2.0 (the "License");
REM you may not use this file except in compliance with the License.
REM You may obtain a copy of the License at
REM
REM     http://www.apache.org/licenses/LICENSE-2.0
REM
REM Unless required by applicable law or agreed to in writing, software
REM distributed under the License is distributed on an "AS IS" BASIS,
REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
REM See the License for the specific language governing permissions and
REM limitations under the License.
REM
REM Operator launcher for perc-doctor on Windows.
REM Layout (CMS install or dist zip):
REM   <install-root>\bin\perc-doctor.bat
REM   <install-root>\bin\perc-doctor.jar
REM
REM Default --install-root is the parent of this script's directory (the install
REM root). Operators may still pass --install-root explicitly to override.
REM Never hardcodes a user home path.

setlocal EnableExtensions EnableDelayedExpansion

set "SCRIPT_DIR=%~dp0"
if "%SCRIPT_DIR:~-1%"=="\" set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"
for %%I in ("%SCRIPT_DIR%\..") do set "INSTALL_ROOT=%%~fI"
set "JAR=%SCRIPT_DIR%\perc-doctor.jar"

if not exist "%JAR%" (
  echo perc-doctor: jar not found at %JAR% 1>&2
  echo Expected layout: ^<install-root^>\bin\perc-doctor.jar next to this script. 1>&2
  exit /b 1
)

set "JAVA_EXE="
if defined JAVA_HOME (
  if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
)
if not defined JAVA_EXE (
  where java >nul 2>&1
  if not errorlevel 1 (
    set "JAVA_EXE=java"
  )
)
if not defined JAVA_EXE (
  echo perc-doctor: java not found. Set JAVA_HOME ^(JDK 21+^) or put java on PATH. 1>&2
  exit /b 1
)

set "HAS_INSTALL_ROOT=0"
for %%A in (%*) do (
  if /I "%%~A"=="--install-root" set "HAS_INSTALL_ROOT=1"
)

if "%HAS_INSTALL_ROOT%"=="0" (
  "%JAVA_EXE%" -jar "%JAR%" --install-root "%INSTALL_ROOT%" %*
) else (
  "%JAVA_EXE%" -jar "%JAR%" %*
)
set "EXIT_CODE=%ERRORLEVEL%"
endlocal & exit /b %EXIT_CODE%
