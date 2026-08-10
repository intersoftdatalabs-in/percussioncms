@echo off
REM Build product-docs Virtual Site to static HTML via Maven exec:java (system module).
REM Usage: scripts\build-cms-docs.bat [siteRoot] [outputRoot]
setlocal
set "REPO_ROOT=%~dp0.."
set "SITE_ROOT=%~1"
if "%SITE_ROOT%"=="" set "SITE_ROOT=%REPO_ROOT%\product-docs"
set "OUT_ROOT=%~2"
if "%OUT_ROOT%"=="" set "OUT_ROOT=%REPO_ROOT%\tmp\product-docs-site"

pushd "%REPO_ROOT%\system" || exit /b 1
call "..\mvnw.cmd" -q -DskipTests compile org.codehaus.mojo:exec-maven-plugin:3.5.0:java ^
  "-Dexec.classpathScope=compile" ^
  "-Dexec.mainClass=com.percussion.services.virtualsite.PSVirtualSiteBuildMain" ^
  "-Dexec.args=%SITE_ROOT% %OUT_ROOT% product-docs"
set "RC=%ERRORLEVEL%"
popd
exit /b %RC%
