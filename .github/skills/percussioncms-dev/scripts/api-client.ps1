<#
.SYNOPSIS
PowerShell helper functions for interacting with the Percussion CMS REST API.
#>

Set-StrictMode -Version Latest

$global:API_BASE = $env:API_BASE
if (-not $global:API_BASE) {
    $global:API_BASE = 'http://localhost:9992/Rhythmyx/rest'
}

$global:PercSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$global:PercAuthHeader = @{}
$global:DefaultHeaders = @{
    'Accept' = 'application/json'
    'Content-Type' = 'application/json'
}

function ConvertFrom-SecureStringToPlainText {
    param([System.Security.SecureString]$SecureString)
    $ptr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureString)
    try {
        [System.Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    } finally {
        [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    }
}

function perc-login {
    [CmdletBinding()]
    param(
        [string]$Username = $env:CMS_USER ?? 'Admin',
        [Parameter(Mandatory=$false)]
        [System.Security.SecureString]$Password
    )

    if (-not $Password) {
        $Password = Read-Host -Prompt "Password for $Username" -AsSecureString
    }
    $plainPassword = ConvertFrom-SecureStringToPlainText -SecureString $Password

    $basicToken = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("$Username`:$plainPassword"))
    $basicHeader = @{ Authorization = "Basic $basicToken" }

    try {
        $response = Invoke-WebRequest -Uri "$API_BASE/folders/by-path/Assets" -Headers $basicHeader -WebSession $PercSession -Method GET -ErrorAction Stop
        if ($response.StatusCode -eq 200) {
            Write-Host "Login successful (Basic Auth)."
            $global:PercAuthHeader = $basicHeader
            return
        }
    } catch {
        # fallback to form auth
    }

    $formUri = "$($API_BASE.TrimEnd('/rest'))/j_security_check"
    try {
        $formResponse = Invoke-WebRequest -Uri $formUri -WebSession $PercSession -Method POST -Body @{ j_username = $Username; j_password = $plainPassword } -ErrorAction Stop -UseBasicParsing
        if ($formResponse.StatusCode -in 200,302,303) {
            Write-Host "Login successful (Form Auth)."
            $global:PercAuthHeader = @{}
            return
        }
    } catch {
        Write-Error "Login failed: $($_.Exception.Message)"
        return
    }
}

function Get-ApiHeaders {
    $headers = @{}
    $headers += $DefaultHeaders
    if ($PercAuthHeader) {
        $headers += $PercAuthHeader
    }
    return $headers
}

function perc-api {
    param(
        [Parameter(Mandatory)]
        [ValidateSet('GET','POST','PUT','PATCH','DELETE')]
        [string]$Method,
        [Parameter(Mandatory)]
        [string]$Path,
        [string]$Body
    )

    if (-not $Path.StartsWith('/')) {
        $Path = "/$Path"
    }
    $uri = "$API_BASE$Path"
    $headers = Get-ApiHeaders

    $invokeParams = @{
        Method = $Method
        Uri = $uri
        Headers = $headers
        WebSession = $PercSession
        ErrorAction = 'Stop'
    }
    if ($Body) {
        $invokeParams.Body = $Body
    }

    try {
        $response = Invoke-RestMethod @invokeParams
        $response | ConvertTo-Json -Depth 5
    } catch {
        Write-Error "API call failed: $($_.Exception.Message)"
    }
}

function perc-api-pretty {
    param(
        [Parameter(Mandatory)]
        [string]$Method,
        [Parameter(Mandatory)]
        [string]$Path,
        [string]$Body
    )

    $json = perc-api -Method $Method -Path $Path -Body $Body
    if ($json) {
        $json | ConvertFrom-Json | ConvertTo-Json -Depth 5
    }
}

function perc-list-sites {
    Write-Host "Listing sites..."
    perc-api-pretty -Method GET -Path '/folders/by-path/Sites'
}

function perc-list-folders {
    param(
        [Parameter(Mandatory)]
        [string]$Site,
        [string]$Path
    )

    $segment = if ($Path) { "$Site/$Path" } else { $Site }
    Write-Host "Listing folders in $segment..."
    perc-api-pretty -Method GET -Path "/folders/by-path/$segment"
}

function perc-list-assets {
    param(
        [string]$Path
    )

    $segment = if ($Path) { "Assets/$Path" } else { 'Assets' }
    Write-Host "Listing assets in $segment..."
    perc-api-pretty -Method GET -Path "/folders/by-path/$segment"
}

function perc-list-pages {
    param(
        [Parameter(Mandatory)]
        [string]$Site,
        [string]$Path
    )

    $segment = if ($Path) { "$Site/$Path" } else { $Site }
    Write-Host "Listing pages in $segment..."
    perc-api-pretty -Method GET -Path "/folders/by-path/$segment"
    Write-Host "TIP: Pages are returned in the 'pages' array of the folders response."
}

function perc-check {
    Write-Host "Checking CMS connectivity at $API_BASE..."
    try {
        $response = Invoke-WebRequest -Uri "$API_BASE/folders/by-path/Assets" -Headers (Get-ApiHeaders) -WebSession $PercSession -Method GET -ErrorAction Stop
        if ($response.StatusCode -eq 200) {
            Write-Host "CMS is reachable and authenticated."
            return
        }
        Write-Warning "CMS returned HTTP $($response.StatusCode)."
    } catch {
        Write-Error "Connectivity check failed: $($_.Exception.Message)"
    }
}

Write-Host "Percussion CMS API client loaded."
Write-Host "  API_BASE:   $API_BASE"
Write-Host "Functions available: perc-login, perc-api, perc-api-pretty, perc-list-sites, perc-list-folders, perc-list-assets, perc-list-pages, perc-check"
