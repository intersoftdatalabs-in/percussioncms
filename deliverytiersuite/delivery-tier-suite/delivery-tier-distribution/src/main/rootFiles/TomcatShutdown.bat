@echo off
setlocal

SET SCRIPT_DIR=%~dp0
SET SCRIPT_DIR=%SCRIPT_DIR:~0,-1%
SET SERVER_DIR=%SCRIPT_DIR%\Deployment\Server
SET SERVER_URL_PATH=file:///%SERVER_DIR:\=/%

REM Resolve Java via shared precedence contract — required operator step before
REM service stop. See specs/991-system-java-home/contracts/java-home-resolution.md
REM and US2 DTS parity requirement.
call "%SCRIPT_DIR%\..\resolve-java-home.bat" "%SCRIPT_DIR%\.."
if errorlevel 1 (
    echo TomcatShutdown: Java home resolution failed 1>&2
    exit /b 1
)

set JRE_HOME=%JAVA_HOME%

set JAVA_OPTS=%JAVA_OPTS% -Dhttps.protocols=TLSv1.2 -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv4Addresses=true -Dfile.encoding=UTF-8 -Xmx1024m -XX:+DisableExplicitGC -XX:+UseConcMarkSweepGC -XX:NewSize=256m -XX:SurvivorRatio=16 -Djava.endorsed.dirs=%SERVER_DIR%\endorsed -Dcatalina.base=%SERVER_DIR% -Dcatalina.home=%SERVER_DIR% -Djava.io.tmpdir=%SERVER_DIR%\temp -Dderby.system.home=%SERVER_DIR%\derbydata
set CATALINA_HOME=%SERVER_DIR%
"%SERVER_DIR%\bin\catalina.bat" stop
