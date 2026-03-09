<#
.SYNOPSIS
Start the local Percussion CMS installation on Windows.
#>

param(
    [string]$InstallDir
)

Set-StrictMode -Version Latest

if (-not $InstallDir) {
    $InstallDir = $env:CMS_INSTALL_DIR
}
if (-not $InstallDir) {
    $InstallDir = Join-Path ([Environment]::GetFolderPath('UserProfile')) 'percussioncms-install'
}

$JettyDir = Join-Path $InstallDir 'jetty'
$StartScript = Join-Path $JettyDir 'StartJetty.bat'

if (-not (Test-Path $StartScript)) {
    Write-Error "StartJetty.bat not found in $JettyDir. Run install-cms first."
    exit 1
}

if (-not (Test-Path (Join-Path $InstallDir 'JRE'))) {
    Write-Warning "JRE directory not found. Ensure JRE -> JAVA_HOME links are created by the installer."
}

Write-Host "Starting Percussion CMS..."
Write-Host "  Install dir: $InstallDir"
Write-Host "Press Ctrl+C to stop."

Push-Location $JettyDir
& $StartScript
Pop-Location
