@echo off
setlocal

REM Layout (installDts copies rootFiles\* to install root):
REM   <InstallRoot>\TomcatStartup.bat
REM   <InstallRoot>\resolve-java-home.bat
REM   <InstallRoot>\Deployment\Server\...
SET SCRIPT_DIR=%~dp0
SET SCRIPT_DIR=%SCRIPT_DIR:~0,-1%
SET SERVER_DIR=%SCRIPT_DIR%\Deployment\Server
SET SERVER_URL_PATH=file:///%SERVER_DIR:\=/%

REM GH-991: install-time selection wrote java.properties; resolve it (not a
REM mandatory <InstallRoot>\JRE). See java-home-resolution.md.
call "%SCRIPT_DIR%\resolve-java-home.bat" "%SCRIPT_DIR%"
if errorlevel 1 (
    echo TomcatStartup: Java home resolution failed 1>&2
    exit /b 1
)

set JRE_HOME=%JAVA_HOME%

if not exist "%SERVER_DIR%\bin\catalina.bat" (
    echo TomcatStartup: missing %SERVER_DIR%\bin\catalina.bat 1>&2
    exit /b 1
)

REM Note: java.endorsed.dirs is not supported on Java 9+ (fatal on 21); do not add it back.
REM Note: CMS GC (UseConcMarkSweepGC) was removed in Java 14 (fatal on 21). G1 is the JDK default.
REM Align Windows JAVA_OPTS with TomcatStartup.sh (no CMS-era GC flags).
set JAVA_OPTS=%JAVA_OPTS% -Dhttps.protocols=TLSv1.2 -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv4Addresses=true -Dfile.encoding=UTF-8 -Xmx1024m -XX:+DisableExplicitGC -Dcatalina.base=%SERVER_DIR% -Dcatalina.home=%SERVER_DIR% -Djava.io.tmpdir=%SERVER_DIR%\temp -Dperc.h2.data.home=%SERVER_DIR%\h2data
set CATALINA_HOME=%SERVER_DIR%
"%SERVER_DIR%\bin\catalina.bat" run
