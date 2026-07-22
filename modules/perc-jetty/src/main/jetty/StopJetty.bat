@echo off
setlocal

SET mypath=%~dp0
SET JETTY_HOME=%mypath%upstream
SET rxDir=%mypath%..
SET JETTY_BASE=%mypath%base
SET JETTY_DEFAULTS=%mypath%defaults
set STOPPORT=50011

REM Resolve Java via the shared precedence contract — StopJetty must use the same
REM Java home the matching StartJetty would, even when <installRoot>\JRE is absent.
REM See specs/991-system-java-home/contracts/java-home-resolution.md.
call "%~dp0resolve-java-home.bat" "%rxDir%"
if errorlevel 1 (
    echo StopJetty: Java home resolution failed 1>&2
    exit /b 1
)

SET PATH=%JAVA_HOME%\bin;%PATH%

cd %JETTY_BASE%
"%JAVA%" -jar %JETTY_HOME%\start.jar -DSTOP.PORT=%STOPPORT% -DSTOP.KEY="SHUTDOWN" -Drxdeploydir="%rxDir%" -Djetty.base="%JETTY_BASE%" -Djetty_perc_defaults="%JETTY_DEFAULTS%" --include-jetty-dir="%JETTY_DEFAULTS%" --stop

endlocal
