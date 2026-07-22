@echo off
REM scripts/run-python-tests.cmd
REM
REM Cross-platform Python-script test runner (Windows).
REM
REM Installs pytest from scripts\requirements-dev.txt and runs pytest over every
REM in-scope script directory per spec 994-python-build-scripts. Used by
REM .github\workflows\python-build-scripts.yml on windows-latest and by Windows
REM developers locally.
REM
REM Usage:
REM   scripts\run-python-tests.cmd [--skip-install] [--pytest-args "ARGS"]
REM
REM Flags:
REM   --skip-install        Skip the pip install step
REM   --pytest-args "ARGS"  Extra args forwarded to `python -m pytest`
REM   -h | --help           Show this help
REM
REM Exit codes:
REM   0   all in-scope pytest cases pass
REM   2   pip install failed
REM   >0  pytest exit code propagated

setlocal EnableExtensions EnableDelayedExpansion

set "PROJECT_ROOT=%~dp0.."
pushd "%PROJECT_ROOT%" >nul || (
  echo ERROR: could not cd to %PROJECT_ROOT% 1>&2
  exit /b 2
)

set "REQUIREMENTS_FILE=%PROJECT_ROOT%\scripts\requirements-dev.txt"
set "SKIP_INSTALL=false"
set "PYTEST_EXTRA_ARGS="

:parse_args
if "%~1"=="" goto after_args
if /i "%~1"=="--skip-install" (
  set "SKIP_INSTALL=true"
  shift /1
  goto parse_args
)
if /i "%~1"=="--pytest-args" (
  shift /1
  if "%~1"=="" (
    echo ERROR: --pytest-args requires a value 1>&2
    popd
    exit /b 1
  )
  set "PYTEST_EXTRA_ARGS=%~1"
  shift /1
  goto parse_args
)
if /i "%~1"=="-h" goto show_help
if /i "%~1"=="--help" goto show_help
echo ERROR: unknown argument: %~1 1>&2
popd
exit /b 1

:show_help
echo Usage: scripts\run-python-tests.cmd [--skip-install] [--pytest-args "ARGS"]
popd
exit /b 0

:after_args
if not exist "%REQUIREMENTS_FILE%" (
  echo ERROR: %REQUIREMENTS_FILE% not found 1>&2
  popd
  exit /b 2
)

if /i not "%SKIP_INSTALL%"=="true" (
  echo === Installing pytest from %REQUIREMENTS_FILE% ===
  python -m pip install -r "%REQUIREMENTS_FILE%"
  if errorlevel 1 (
    echo ERROR: pip install failed 1>&2
    popd
    exit /b 2
  )
)

echo === Running pytest over in-scope script dirs ^(spec 994^) ===
python -m pytest ^
  scripts\ ^
  docker\scripts\ ^
  docker\entrypoint\ ^
  modules\perc-distribution-tree\scripts\ ^
  modules\ai-shared-develop\scripts\ ^
  modules\ai-shared-develop\src\main\resources\skills\ ^
  %PYTEST_EXTRA_ARGS%
set "PYTEST_EXIT=%errorlevel%"
popd
exit /b %PYTEST_EXIT%
