@echo off
REM Environment setup script for Windows
REM Sets JAVA_HOME to JAVA_HOME_21 for JDK 21 compatibility
REM Run as: mvn-env.bat <maven-args>

REM Check if JAVA_HOME_21 is set
if "%JAVA_HOME_21%"=="" (
    echo Error: JAVA_HOME_21 environment variable is not set.
    echo Please set JAVA_HOME_21 to the path of your JDK 21 installation.
    echo Example: set JAVA_HOME_21=C:\Program Files\Java\jdk-21
    exit /b 1
)

REM Verify the JDK 21 path exists
if not exist "%JAVA_HOME_21%" (
    echo Error: JDK 21 not found at %JAVA_HOME_21%
    echo Please ensure JAVA_HOME_21 points to a valid JDK 21 installation.
    exit /b 1
)

REM Set JAVA_HOME
set JAVA_HOME=%JAVA_HOME_21%

echo Using JDK 21 at %JAVA_HOME%

REM Get the absolute path of the script directory
set SCRIPT_DIR=%~dp0
set TMP_DIR=%SCRIPT_DIR%tmp

mkdir "%TMP_DIR%" 2>nul

REM Run Maven wrapper with all arguments
call "%SCRIPT_DIR%mvnw.cmd" -Djava.io.tmpdir="%TMP_DIR%" %*
