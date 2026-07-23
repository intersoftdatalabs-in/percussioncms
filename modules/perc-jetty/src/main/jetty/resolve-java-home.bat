@echo off
REM Shared runtime Java home resolver for Percussion CMS / DTS start, stop, and
REM service install paths on Windows. See specs/991-system-java-home/contracts/
REM java-home-resolution.md for the algorithm and contracts/java-properties-contract.md
REM for the input format.
REM
REM Usage (call):  call resolve-java-home.bat [<install_root>]
REM                After the call, JAVA_HOME and JAVA are set in the caller's
REM                environment. On failure the script exits non-zero, prints
REM                an actionable error that mentions minimum major version 21
REM                (21 or later) and lists the sources tried.
REM
REM This file MUST be sourced with `call` (not run directly) so the variables
REM propagate to the caller. The exit code is preserved via exit /b.

setlocal EnableDelayedExpansion

REM Minimum supported major version (21 or later is accepted).
set REQUIRED_MAJOR=21
set RESOLVE_SOURCE=
set RESOLVE_ERRORS=

REM Determine install root. The first argument overrides; otherwise we assume
REM this script lives under <install_root>\jetty and the parent of jetty is
REM the install root.
if not "%~1"=="" (
    set "INSTALL_ROOT=%~1"
) else (
    set "INSTALL_ROOT=%~dp0.."
)
REM Canonicalize via the for %%~fI expansion. This is portable across cmd.exe
REM versions and does not rely on pushd/popd + %CD% which has subtle failures
REM under some service-account contexts.
for %%I in ("%INSTALL_ROOT%") do set "INSTALL_ROOT=%%~fI"

set LAUNCHER=java.exe

REM ----- helper: capture launcher -version output into a temp file -----
REM   Args: %1 = candidate home.  Sets VERSION_LINE.  Returns 0 valid, 1 invalid.
goto :main

:validate_java_home
    set "CAND=%~1"
    set "EXE=%CAND%\bin\%LAUNCHER%"
    if not exist "%EXE%" (
        echo launcher missing: %EXE% 1>&2
        exit /b 1
    )
    REM java -version writes to stderr; redirect both to a temp file so the
    REM parser below does not have to depend on `for /F usebackq` command-form
    REM quirks across Windows versions.
    set "TEMP_JV=%TEMP%\perc-jv-%RANDOM%.tmp"
    "%EXE%" -version 1>"%TEMP_JV%" 2>&1
    if errorlevel 1 (
        echo could not execute %EXE% for -version 1>&2
        del "%TEMP_JV%" >nul 2>&1
        exit /b 1
    )
    for /f "usebackq tokens=3 delims= " %%v in ("%TEMP_JV%") do (
        set "VERSION_LINE=%%v"
        del "%TEMP_JV%" >nul 2>&1
        goto :parse_version_line
    )
    del "%TEMP_JV%" >nul 2>&1
    echo could not parse version output for %EXE% 1>&2
    exit /b 1

:parse_version_line
    REM Strip surrounding double quotes then split on . + -
    set "RAW=%VERSION_LINE%"
    set "RAW=%RAW:"=%"
    for /f "tokens=1 delims=.+-" %%a in ("%RAW%") do (
        set "MAJOR=%%a"
        goto :check_major
    )
    echo could not parse major from version line "%VERSION_LINE%" for %EXE% 1>&2
    exit /b 1

:check_major
    if "%MAJOR%"=="1" (
        REM Legacy "1.8.0_xxx" -- bump past leading 1.
        for /f "tokens=2 delims=." %%b in ("%RAW%") do (
            set "MAJOR=%%b"
        )
    )
    REM Accept major >= REQUIRED_MAJOR (21+). Integer compare via GEQ.
    if %MAJOR% GEQ %REQUIRED_MAJOR% (
        exit /b 0
    )
    echo Java major version %MAJOR% is below minimum %REQUIRED_MAJOR% (21 or later required) 1>&2
    exit /b 1

REM ----- source 1: install-root java.properties -----
:try_config
    set "PROPS=%INSTALL_ROOT%\java.properties"
    if not exist "%PROPS%" (
        call :record_error PRODUCT_CONFIG "%PROPS%" "not found"
        exit /b 1
    )
    set "CFG_HOME="
    set "CFG_LAUNCHER="
    for /f "usebackq tokens=1,2 delims==" %%a in ("%PROPS%") do (
        set "KEY=%%a"
        set "VAL=%%b"
        if /i "!KEY!"=="JAVA_HOME" set "CFG_HOME=!VAL!"
        if /i "!KEY!"=="JAVA" set "CFG_LAUNCHER=!VAL!"
    )
    if defined CFG_HOME (
        call :validate_java_home "!CFG_HOME!"
        if not errorlevel 1 (
            set "JAVA_HOME=!CFG_HOME!"
            if defined CFG_LAUNCHER (set "JAVA=!CFG_LAUNCHER!") else (set "JAVA=!CFG_HOME!\bin\%LAUNCHER%")
            set "RESOLVE_SOURCE=java.properties (PRODUCT_CONFIG)"
            exit /b 0
        )
        call :record_error PRODUCT_CONFIG "!CFG_HOME!" "not a valid Java home (minimum major %REQUIRED_MAJOR%)"
    ) else (
        call :record_error PRODUCT_CONFIG "%PROPS%" "JAVA_HOME missing"
    )
    exit /b 1

REM ----- source 2: process env JAVA_HOME -----
:try_env
    if not defined JAVA_HOME (
        call :record_error PROCESS_ENV "JAVA_HOME" "unset"
        exit /b 1
    )
    call :validate_java_home "%JAVA_HOME%"
    if not errorlevel 1 (
        set "JAVA=%JAVA_HOME%\bin\%LAUNCHER%"
        set "RESOLVE_SOURCE=env JAVA_HOME (PROCESS_ENV)"
        exit /b 0
    )
    call :record_error PROCESS_ENV "%JAVA_HOME%" "not a valid Java home (minimum major %REQUIRED_MAJOR%)"
    exit /b 1

REM ----- source 3: legacy install-dir JRE / JRE64 -----
:try_legacy
    call :try_legacy_one JRE INSTALL_DIR_JRE
    if not errorlevel 1 exit /b 0
    call :try_legacy_one JRE64 INSTALL_DIR_JRE64
    if not errorlevel 1 exit /b 0
    exit /b 1

:try_legacy_one
    set "DIRN=%~1"
    set "LABEL=%~2"
    set "CAND=%INSTALL_ROOT%\%DIRN%"
    if not exist "!CAND!\" (
        exit /b 1
    )
    call :validate_java_home "!CAND!"
    if not errorlevel 1 (
        set "JAVA_HOME=!CAND!"
        set "JAVA=!CAND!\bin\%LAUNCHER%"
        set "RESOLVE_SOURCE=legacy install-dir !DIRN! (!LABEL!)"
        exit /b 0
    )
    call :record_error !LABEL! "!CAND!" "not a valid Java home (minimum major %REQUIRED_MAJOR%)"
    exit /b 1

REM ----- source 4: PATH discovery (best-effort; quiet fail) -----
:try_path
    set "PATH_OK="
    for %%D in ("%PATH:;=";"%") do (
        set "DIR=%%~D"
        if exist "!DIR!\%LAUNCHER%" (
            REM Canonicalize the parent of bin via for %%~fI to compute the home.
            set "BIN=!DIR!"
            goto :path_check
        )
    )
    call :record_error PATH "%LAUNCHER%" "no launcher found"
    exit /b 1
:path_check
    for %%I in ("!BIN!\..") do set "HOME_DIR=%%~fI"
    call :validate_java_home "!HOME_DIR!"
    if not errorlevel 1 (
        set "JAVA_HOME=!HOME_DIR!"
        set "JAVA=!BIN!\%LAUNCHER%"
        set "RESOLVE_SOURCE=PATH"
        exit /b 0
    )
    call :record_error PATH "!BIN!" "launcher below minimum major %REQUIRED_MAJOR%"
    exit /b 1

:record_error
    set "RESOLVE_ERRORS=!RESOLVE_ERRORS!  - %~1 %~2 (%~3)"
    exit /b 0

:main
    call :try_config
    if not errorlevel 1 goto :end_success
    call :try_env
    if not errorlevel 1 goto :end_success
    call :try_legacy
    if not errorlevel 1 goto :end_success
    call :try_path
    if not errorlevel 1 goto :end_success
    echo resolve-java-home: no compatible Java home found. 1>&2
    echo Required Java major version: %REQUIRED_MAJOR% or later 1>&2
    echo Install root: %INSTALL_ROOT% 1>&2
    echo Sources tried: 1>&2
    if defined RESOLVE_ERRORS echo %RESOLVE_ERRORS% 1>&2
    endlocal & exit /b 1

:end_success
    endlocal & set "JAVA_HOME=%JAVA_HOME%" & set "JAVA=%JAVA%" & set "RESOLVE_SOURCE=%RESOLVE_SOURCE%"
    exit /b 0