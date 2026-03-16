@echo off
REM Environment setup script for Windows
REM Sets JAVA_HOME to JAVA_HOME_8 for JDK 8 compatibility
REM Run as: mvn-env.bat <maven-args>

REM Check if JAVA_HOME_8 is set
if "%JAVA_HOME_8%"=="" (
    echo Error: JAVA_HOME_8 environment variable is not set.
    echo Please set JAVA_HOME_8 to the path of your JDK 8 installation.
    echo Example: set JAVA_HOME_8=C:\Program Files\Java\jdk-8
    exit /b 1
)

REM Verify the JDK 8 path exists
if not exist "%JAVA_HOME_8%" (
    echo Error: JDK 8 not found at %JAVA_HOME_8%
    echo Please ensure JAVA_HOME_8 points to a valid JDK 8 installation.
    exit /b 1
)

REM Set JAVA_HOME
set JAVA_HOME=%JAVA_HOME_8%

echo Using JDK 8 at %JAVA_HOME%

REM Run Maven wrapper with all arguments
call mvnw.cmd %*
