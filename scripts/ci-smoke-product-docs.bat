@echo off
REM CI / local smoke for product-docs Virtual Site build (issue #2704).
REM Runs scripts\build-cms-docs.bat then fails if default versioned index HTML is missing.
REM Usage: scripts\ci-smoke-product-docs.bat [siteRoot] [outputRoot]
REM Unix parity: scripts/ci-smoke-product-docs.sh
REM
REM Failure artifacts: output root is recreated empty (not left deleted) and a
REM .ci-smoke-meta.txt marker is always written so CI upload-artifact on
REM failure has a path + file even when the docs build emits nothing.
setlocal EnableExtensions
set "REPO_ROOT=%~dp0.."
set "SITE_ROOT=%~1"
if "%SITE_ROOT%"=="" set "SITE_ROOT=%REPO_ROOT%\product-docs"
set "OUT_ROOT=%~2"
if "%OUT_ROOT%"=="" set "OUT_ROOT=%REPO_ROOT%\tmp\product-docs-site"

REM Fresh output so stale HTML cannot mask a failed emit.
REM Recreate dir immediately so failure artifacts always have a path.
if exist "%OUT_ROOT%" rmdir /s /q "%OUT_ROOT%"
mkdir "%OUT_ROOT%" 2>nul
(
  echo status=running
  echo siteRoot=%SITE_ROOT%
  echo outRoot=%OUT_ROOT%
) > "%OUT_ROOT%\.ci-smoke-meta.txt"

echo ci-smoke-product-docs: building siteRoot=%SITE_ROOT% -^> outRoot=%OUT_ROOT%
call "%~dp0build-cms-docs.bat" "%SITE_ROOT%" "%OUT_ROOT%"
if errorlevel 1 (
  (
    echo status=build_failed
    echo buildExit=non-zero
  ) >> "%OUT_ROOT%\.ci-smoke-meta.txt"
  echo ci-smoke-product-docs: FAIL — build-cms-docs.bat exited non-zero 1>&2
  exit /b 1
)

set "DEFAULT_INDEX=%OUT_ROOT%\8.2\index.html"
if exist "%DEFAULT_INDEX%" (
  echo status=ok>> "%OUT_ROOT%\.ci-smoke-meta.txt"
  echo ci-smoke-product-docs: OK — found %DEFAULT_INDEX%
  exit /b 0
)

REM Fallback: any index.html under the output tree (dir /s is recursive).
set "FOUND="
for /f "delims=" %%F in ('dir /s /b "%OUT_ROOT%\index.html" 2^>nul') do (
  set "FOUND=%%F"
  goto :found
)
:found
if defined FOUND (
  echo status=ok>> "%OUT_ROOT%\.ci-smoke-meta.txt"
  echo ci-smoke-product-docs: OK — found index HTML at %FOUND%
  exit /b 0
)

(
  echo status=missing_index
  echo expectedDefault=%DEFAULT_INDEX%
) >> "%OUT_ROOT%\.ci-smoke-meta.txt"
echo ci-smoke-product-docs: FAIL — no index.html under %OUT_ROOT% 1>&2
echo   Expected at least %DEFAULT_INDEX% (or any **\index.html^). 1>&2
echo   Broken Markdown/frontmatter/_config.yaml or Virtual Site build bugs 1>&2
echo   cause this failure; fix content or system virtualsite package and re-run. 1>&2
exit /b 1
