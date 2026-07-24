@echo off
setlocal

SET mypath=%~dp0
SET JETTY_HOME=%mypath%upstream
SET rxDir=%mypath%..
SET JETTY_BASE=%mypath%base
SET JETTY_DEFAULTS=%mypath%defaults
set STOPPORT=50011

REM Resolve Java via shared precedence: java.properties > env > install-dir JRE|JRE64 > PATH > fail.
REM See specs/991-system-java-home/contracts/java-home-resolution.md.
call "%~dp0resolve-java-home.bat" "%rxDir%"
if errorlevel 1 (
    echo StartJetty: Java home resolution failed 1>&2
    exit /b 1
)

SET PATH=%JAVA_HOME%\bin;%PATH%

cd %JETTY_BASE%
"%JAVA%" --add-opens java.base/java.lang=ALL-UNNAMED -XX:+DisableAttachMechanism -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv4Addresses=true -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -jar %JETTY_HOME%\start.jar -DSTOP.PORT=%STOPPORT% -DSTOP.KEY="SHUTDOWN" -Drxdeploydir="%rxDir%" -DTIKA_CONFIG="%rxDir%\rxconfig\tika-config.xml" -Djetty.base="%JETTY_BASE%" -Djetty_perc_defaults="%JETTY_DEFAULTS%" --include-jetty-dir="%JETTY_DEFAULTS%" %*

endlocal
