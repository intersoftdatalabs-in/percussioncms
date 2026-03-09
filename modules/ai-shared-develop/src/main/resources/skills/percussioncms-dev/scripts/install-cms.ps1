<#
.SYNOPSIS
Install Percussion CMS on Windows using a local build artifact or downloaded JAR.
#>

param(
    [string]$InstallDir,
    [string]$Jar
)

Set-StrictMode -Version Latest

if (-not $InstallDir) {
    $InstallDir = Join-Path ([Environment]::GetFolderPath('UserProfile')) 'percussioncms-install'
}

function Get-ProjectRoot {
    if ($env:PROJECT_ROOT) {
        return $env:PROJECT_ROOT
    }

    try {
        $root = (& git rev-parse --show-toplevel 2>$null).Trim()
        if ($root) {
            return $root
        }
    } catch {
        # Ignore
    }

    return (Get-Location).Path
}

$ProjectRoot = Get-ProjectRoot
$DefaultJarPath = Join-Path $ProjectRoot 'modules\perc-distribution-tree\target\perc-distribution-tree.jar'
$JarPath = if ($Jar) { $Jar } else { $DefaultJarPath }

$JavaHome = $env:JAVA_HOME
if (-not $JavaHome) {
    $JavaHome = $env:JAVA_HOME_21
}

if (-not $JavaHome) {
    Write-Error "JAVA_HOME (or JAVA_HOME_21) is not set. Install JDK 21 and set the environment variable."
    exit 1
}

$JavaExe = Join-Path $JavaHome 'bin\java.exe'
if (-not (Test-Path $JavaExe)) {
    $JavaExe = 'java'
}

if (-not (Test-Path $JarPath)) {
    Write-Error "Distribution JAR not found at $JarPath"
    Write-Error "Run './mvn-env.sh clean install' to build the project or pass --jar with a release artifact."
    exit 1
}

Write-Host "Installing Percussion CMS..."
Write-Host "  JAR:         $JarPath"
Write-Host "  Install Dir: $InstallDir"
Write-Host "  JAVA_HOME:   $JavaHome"

& $JavaExe "-jar" "$JarPath" "$InstallDir"

$jrePath = Join-Path $InstallDir 'JRE'
if (Test-Path $jrePath) {
    Remove-Item $jrePath -Force -Recurse -ErrorAction SilentlyContinue
}

try {
    New-Item -ItemType SymbolicLink -Path $jrePath -Target $JavaHome -Force | Out-Null
    Write-Host "Created JRE symbolic link -> $JavaHome"
} catch {
    Write-Warning "Unable to create JRE symbolic link. Please ensure $jrePath points to your JDK home."
}

if (Test-Path (Join-Path $InstallDir 'jetty\StartJetty.bat')) {
    Write-Host "Installation successful!"
    Write-Host "Start the CMS with: cd $InstallDir\jetty && StartJetty.bat"
} else {
    Write-Warning "StartJetty.bat not found. Inspect $InstallDir for installation output."
}
