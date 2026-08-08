# Copyright (c) 2026 Intersoft Data Labs, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#
# See the License for the specific language governing permissions and
# limitations under the License.

<#
.SYNOPSIS
  Sample Windows scheduled retention for Percussion CMS / DTS logs via perc-doctor.

.DESCRIPTION
  Classic Linux logrotate is not available on Windows. Operators should schedule
  perc-doctor clean-logs (age-based purge of allowlisted files under known log
  roots) as the documented Windows counterpart to the Linux logrotate samples
  in this directory (issue #2348).

  This script is a SAMPLE. It is not registered with Task Scheduler automatically.
  Prefer dry-run first. Do not enable unattended apply without operator review.

.PARAMETER InstallRoot
  CMS (or co-located CMS+DTS) install root. Defaults to parent of this script's
  install tree when placed under rxconfig/Installer/logrotate (three levels up),
  otherwise requires an explicit path. Generic examples: C:\Percussion

.PARAMETER OlderThan
  Duration passed to clean-logs (e.g. 14d). Default 14d matches logrotate rotate 14.

.PARAMETER DryRun
  When set, pass --dry-run (report only). Default: $true for safety.

.EXAMPLE
  # Preview only (safe)
  .\schedule-clean-logs.ps1 -InstallRoot 'C:\Percussion' -DryRun

.EXAMPLE
  # Apply after operator review (Task Scheduler action)
  .\schedule-clean-logs.ps1 -InstallRoot 'C:\Percussion' -OlderThan '14d' -DryRun:$false
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory = $false)]
    [string] $InstallRoot,

    [Parameter(Mandatory = $false)]
    [string] $OlderThan = '14d',

    [Parameter(Mandatory = $false)]
    [bool] $DryRun = $true
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Resolve-DefaultInstallRoot {
    # This sample lives at <install-root>/rxconfig/Installer/logrotate/
    # Climb three levels: logrotate -> Installer -> rxconfig -> install-root
    $here = $PSScriptRoot
    if (-not $here) {
        $here = Split-Path -Parent $MyInvocation.MyCommand.Path
    }
    $candidate = (Resolve-Path (Join-Path $here '..\..\..')).Path
    $doctorBat = Join-Path $candidate 'bin\perc-doctor.bat'
    if (Test-Path -LiteralPath $doctorBat) {
        return $candidate
    }
    return $null
}

if (-not $InstallRoot) {
    $InstallRoot = Resolve-DefaultInstallRoot
}

if (-not $InstallRoot) {
    Write-Error "InstallRoot is required (e.g. -InstallRoot 'C:\Percussion'). Could not infer from script location."
}

$InstallRoot = (Resolve-Path -LiteralPath $InstallRoot).Path
$doctorBat = Join-Path $InstallRoot 'bin\perc-doctor.bat'
$doctorJar = Join-Path $InstallRoot 'bin\perc-doctor.jar'

if (-not (Test-Path -LiteralPath $doctorBat) -and -not (Test-Path -LiteralPath $doctorJar)) {
    Write-Error "perc-doctor not found under install root: $InstallRoot (expected bin\perc-doctor.bat or bin\perc-doctor.jar)"
}

$argsList = @(
    '--install-root', $InstallRoot
)
if ($DryRun) {
    $argsList += '--dry-run'
}
$argsList += @('-v', 'clean-logs', '--older-than', $OlderThan)

Write-Host "perc-doctor clean-logs (DryRun=$DryRun OlderThan=$OlderThan)"
Write-Host "InstallRoot=$InstallRoot"

if (Test-Path -LiteralPath $doctorBat) {
    & $doctorBat @argsList
    exit $LASTEXITCODE
}

# Fallback: java -jar when .bat is missing but jar is present
$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\java.exe' } else { 'java' }
& $java -jar $doctorJar @argsList
exit $LASTEXITCODE
