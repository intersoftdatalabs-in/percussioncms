<#
.SYNOPSIS
Start the local Percussion DTS installation on Windows.
#>

param(
    [string]$InstallDir
)

Set-StrictMode -Version Latest

if (-not $InstallDir) {
    $InstallDir = $env:DTS_INSTALL_DIR
}
if (-not $InstallDir) {
    $InstallDir = Join-Path ([Environment]::GetFolderPath('UserProfile')) 'percussiondts-install'
}

if (-not (Test-Path (Join-Path $InstallDir 'Deployment\Server'))) {
    Write-Error "DTS installation not found at $InstallDir. Run install-dts first."
    exit 1
}

$startupCandidates = @('TomcatStartup.bat', 'startup.bat')
$startupScript = $null
foreach ($candidate in $startupCandidates) {
    $path = Join-Path $InstallDir $candidate
    if (Test-Path $path) {
        $startupScript = $path
        break
    }
}

if (-not $startupScript) {
    Write-Error "No startup script (TomcatStartup.bat or startup.bat) found in $InstallDir."
    exit 1
}

Write-Host "Starting Percussion DTS..."
Write-Host "  Install dir: $InstallDir"
Write-Host "  Startup script: $startupScript"
Write-Host "Press Ctrl+C to stop."

Push-Location $InstallDir
& $startupScript
Pop-Location
