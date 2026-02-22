<#
.SYNOPSIS
Download the latest Percussion CMS release assets from GitHub.
#>

param(
    [switch]$DownloadDts,
    [string]$OutputDir = ".\downloads"
)

$githubRepo = $env:GITHUB_REPO
if (-not $githubRepo) {
    $githubRepo = "intersoftdatalabs-in/percussioncms"
}

$headers = @{
    "User-Agent" = "percskill-agent"
}
if ($env:GITHUB_TOKEN) {
    $headers["Authorization"] = "token $($env:GITHUB_TOKEN)"
}

Write-Host "Fetching latest release info from $githubRepo..."
$release = Invoke-RestMethod -Uri "https://api.github.com/repos/$githubRepo/releases/latest" -Headers $headers

New-Item -Path $OutputDir -ItemType Directory -Force | Out-Null

$assets = $release.assets
$cmsAsset = $assets | Where-Object { $_.name -match 'perc-distribution-tree' -and $_.name -like '*.jar' } | Select-Object -First 1

if ($cmsAsset) {
    Write-Host "Downloading CMS distribution JAR..."
    $cmsPath = Join-Path $OutputDir "perc-distribution-tree.jar"
    Invoke-WebRequest -Uri $cmsAsset.browser_download_url -Headers $headers -OutFile $cmsPath
    Write-Host "  -> $cmsPath"
} else {
    Write-Warning "CMS distribution JAR not found in release assets."
    Write-Warning "Run the Maven build (./mvn-env.sh clean install) to produce the target artifact."
}

if ($DownloadDts) {
    $dtsAsset = $assets | Where-Object { $_.name -match 'delivery-tier-distribution' -and $_.name -like '*.jar' } | Select-Object -First 1
    if ($dtsAsset) {
        Write-Host "Downloading DTS distribution JAR..."
        $dtsPath = Join-Path $OutputDir "delivery-tier-distribution.jar"
        Invoke-WebRequest -Uri $dtsAsset.browser_download_url -Headers $headers -OutFile $dtsPath
        Write-Host "  -> $dtsPath"
    } else {
        Write-Warning "DTS distribution JAR not found in release assets."
        Write-Warning "Run './mvn-env.sh -P with-dts clean install' to build DTS locally."
    }
}

Write-Host ""
Write-Host "Download complete. Release: $($release.tag_name)"
Write-Host "Files in $OutputDir:"
$jarFiles = Get-ChildItem -Path $OutputDir -Filter *.jar -ErrorAction SilentlyContinue
if ($jarFiles) {
    $jarFiles | ForEach-Object { Write-Host "  $_" }
} else {
    Write-Host "  (no JAR files found)"
}
