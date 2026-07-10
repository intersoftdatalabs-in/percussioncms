@echo off
REM fast-build.bat - Windows counterpart to fast-build.sh.
REM Run a Maven build with slow, non-essential plugins skipped for quick dev iteration.
REM
REM This reactor binds several plugins to the default lifecycle that are expensive across a
REM 29+ module build (javadoc jar generation, a custom repo-wide SHA-256 hash integrity check
REM that verifies 15,000+ hashes per module, dependency bytecode analysis, enforcer rules,
REM and unit tests). None of these are needed for a quick "does it compile/package" iteration
REM loop, so this script skips them via their documented Maven properties.
REM
REM Usage:
REM   scripts\fast-build.bat [--with-tests] [--online] <maven args...>
REM
REM Examples:
REM   scripts\fast-build.bat -pl deliverytiersuite\delivery-tier-suite\delivery-tier-distribution -am package
REM   scripts\fast-build.bat -pl modules\perc-security-utils install
REM   scripts\fast-build.bat --with-tests -pl system install
REM   scripts\fast-build.bat --online -pl rest install
REM
REM Skipped by default (see scripts\README.md for rationale):
REM   -Dai.integrity.skip=true       (custom ai-build-integrity hash generate/verify mojos)
REM   -Dmaven.javadoc.skip=true      (attach-javadocs jar generation)
REM   -Denforcer.skip=true           (maven-enforcer-plugin rule checks)
REM   -Dmdep.analyze.skip=true       (maven-dependency-plugin analyze-only bytecode scan)
REM   -DskipTests=true               (surefire unit tests; use --with-tests to run them)
REM   -Dcheckstyle.skip=true         (defensive; not bound to the default build today)

setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%.."

set WITH_TESTS=0
set OFFLINE=1
set "MAVEN_ARGS="

:parse_args
if "%~1"=="" goto args_done
if /I "%~1"=="--with-tests" (
    set WITH_TESTS=1
    shift
    goto parse_args
)
if /I "%~1"=="--online" (
    set OFFLINE=0
    shift
    goto parse_args
)
if /I "%~1"=="--help" goto show_help
if /I "%~1"=="-h" goto show_help
set "MAVEN_ARGS=!MAVEN_ARGS! %~1"
shift
goto parse_args

:args_done
if "!MAVEN_ARGS!"=="" (
    echo ERROR: no Maven goal(s) provided ^(e.g. package, install^). 1>&2
    goto show_help
)

set "FAST_FLAGS=-Dai.integrity.skip=true -Dmaven.javadoc.skip=true -Denforcer.skip=true -Dmdep.analyze.skip=true -Dcheckstyle.skip=true"
if "%WITH_TESTS%"=="0" (
    set "FAST_FLAGS=!FAST_FLAGS! -DskipTests=true"
)

set "OFFLINE_FLAG="
if "%OFFLINE%"=="1" set "OFFLINE_FLAG=-o"

echo Project root : %PROJECT_ROOT%
echo Offline      : %OFFLINE%
echo With tests   : %WITH_TESTS%
echo Command      : mvn-env.bat %OFFLINE_FLAG% %FAST_FLAGS% %MAVEN_ARGS%
echo.

pushd "%PROJECT_ROOT%"
call "%PROJECT_ROOT%\mvn-env.bat" %OFFLINE_FLAG% %FAST_FLAGS% %MAVEN_ARGS%
set "EXIT_CODE=%ERRORLEVEL%"
popd
exit /b %EXIT_CODE%

:show_help
echo Usage:
echo   scripts\fast-build.bat [--with-tests] [--online] ^<maven args...^>
echo.
echo Examples:
echo   scripts\fast-build.bat -pl deliverytiersuite\delivery-tier-suite\delivery-tier-distribution -am package
echo   scripts\fast-build.bat -pl modules\perc-security-utils install
echo   scripts\fast-build.bat --with-tests -pl system install
exit /b 0
