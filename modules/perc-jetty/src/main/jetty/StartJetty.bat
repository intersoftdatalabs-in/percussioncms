@echo off
setlocal

SET mypath=%~dp0
SET JETTY_HOME=%mypath%upstream
SET rxDir=%mypath%..
SET JETTY_BASE=%mypath%base
SET JETTY_DEFAULTS=%mypath%defaults
set STOPPORT=50011

REM GH-991: install-time selection wrote java.properties; resolve it (not a
REM mandatory <InstallRoot>\JRE). Hard-fail on resolve. See java-home-resolution.md.
call "%~dp0resolve-java-home.bat" "%rxDir%"
if errorlevel 1 (
    echo StartJetty: Java home resolution failed. Check java.properties or JAVA_HOME. 1>&2
    exit /b 1
)

SET PATH=%JAVA_HOME%\bin;%PATH%

cd %JETTY_BASE%
"%JAVA%" --add-opens java.base/java.lang=ALL-UNNAMED -XX:+DisableAttachMechanism -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv4Addresses=true -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -jar %JETTY_HOME%\start.jar -DSTOP.PORT=%STOPPORT% -DSTOP.KEY="SHUTDOWN" -Drxdeploydir="%rxDir%" -DTIKA_CONFIG="%rxDir%\rxconfig\tika-config.xml" -Djetty.base="%JETTY_BASE%" -Djetty_perc_defaults="%JETTY_DEFAULTS%" --include-jetty-dir="%JETTY_DEFAULTS%" %*

endlocal
