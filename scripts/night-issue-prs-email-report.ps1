#Requires -Version 5.1
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
    Email the night-issue-prs report to a Microsoft Teams channel address.

.DESCRIPTION
    Reads the To: address from -To or NIGHT_ISSUE_PRS_TEAMS_EMAIL.
    Sends via SMTP when NIGHT_ISSUE_PRS_SMTP_HOST is set; otherwise Outlook COM
    (logged-in desktop Outlook on Windows).

    Skip (exit 0) when no To: address is configured — overnight must not fail closed.
    Never prints the full destination address (redacted in output).

.PARAMETER ReportPath
    Markdown report file (repo tmp/ or workflow scratch copy). Required unless -SelfTest.

.PARAMETER Subject
    Email subject. Default: "night-issue-prs report".

.PARAMETER To
    Destination (Teams channel email). Overrides NIGHT_ISSUE_PRS_TEAMS_EMAIL.

.PARAMETER From
    SMTP From:. Overrides NIGHT_ISSUE_PRS_MAIL_FROM.

.PARAMETER DryRun
    Resolve To:/transport and exit without sending.

.PARAMETER SelfTest
    Run redact + HTML helpers only (no network, no Outlook).
#>
[CmdletBinding()]
param(
    [Parameter()]
    [string]$ReportPath,

    [Parameter()]
    [string]$Subject = 'night-issue-prs report',

    [Parameter()]
    [string]$To,

    [Parameter()]
    [string]$From,

    [Parameter()]
    [switch]$DryRun,

    [Parameter()]
    [switch]$SelfTest
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Get-RedactedEmail {
    param([string]$Email)
    if ([string]::IsNullOrWhiteSpace($Email)) {
        return ''
    }
    $at = $Email.IndexOf('@')
    if ($at -lt 1) {
        return '(set)'
    }
    $local = $Email.Substring(0, $at)
    $domain = $Email.Substring($at)
    $keep = [Math]::Min(3, $local.Length)
    return ($local.Substring(0, $keep) + '***' + $domain)
}

function ConvertTo-HtmlEncoded {
    param([string]$Text)
    if ($null -eq $Text) {
        return ''
    }
    return ($Text -replace '&', '&amp;' -replace '<', '&lt;' -replace '>', '&gt;' -replace '"', '&quot;')
}

function ConvertTo-ReportHtml {
    param([string]$Markdown)
    $encoded = ConvertTo-HtmlEncoded -Text $Markdown
    $body = @"
<html>
<body>
<p>Percussion CMS <strong>night-issue-prs</strong> run report (markdown attached).</p>
<pre style="font-family: Consolas, Menlo, monospace; font-size: 12px; white-space: pre-wrap;">$encoded</pre>
</body>
</html>
"@
    return $body
}

function Get-EnvTrimmed {
    param([string]$Name)
    $raw = [Environment]::GetEnvironmentVariable($Name)
    if ([string]::IsNullOrWhiteSpace($raw)) {
        return ''
    }
    return $raw.Trim()
}

if ($SelfTest) {
    $sample = 'channel.name@tenant.teams.ms'
    $redacted = Get-RedactedEmail -Email $sample
    if ($redacted -ne 'cha***@tenant.teams.ms') {
        Write-Error "SelfTest redact failed: $redacted"
        exit 1
    }
    $html = ConvertTo-ReportHtml -Markdown '# Hi <b>x</b>'
    if ($html -notmatch '&lt;b&gt;') {
        Write-Error 'SelfTest HTML encode failed'
        exit 1
    }
    Write-Output 'selftest=ok'
    exit 0
}

$resolvedTo = $To
if ([string]::IsNullOrWhiteSpace($resolvedTo)) {
    $resolvedTo = Get-EnvTrimmed -Name 'NIGHT_ISSUE_PRS_TEAMS_EMAIL'
}
if ([string]::IsNullOrWhiteSpace($resolvedTo)) {
    Write-Output 'status=skipped_no_address'
    Write-Output 'summary=NIGHT_ISSUE_PRS_TEAMS_EMAIL unset and -To omitted; not sending'
    exit 0
}

$resolvedFrom = $From
if ([string]::IsNullOrWhiteSpace($resolvedFrom)) {
    $resolvedFrom = Get-EnvTrimmed -Name 'NIGHT_ISSUE_PRS_MAIL_FROM'
}
if ([string]::IsNullOrWhiteSpace($resolvedFrom)) {
    $resolvedFrom = Get-EnvTrimmed -Name 'NIGHT_ISSUE_PRS_SMTP_USER'
}

$smtpHost = Get-EnvTrimmed -Name 'NIGHT_ISSUE_PRS_SMTP_HOST'
$smtpPortRaw = Get-EnvTrimmed -Name 'NIGHT_ISSUE_PRS_SMTP_PORT'
$smtpUser = Get-EnvTrimmed -Name 'NIGHT_ISSUE_PRS_SMTP_USER'
$smtpPass = Get-EnvTrimmed -Name 'NIGHT_ISSUE_PRS_SMTP_PASS'
$smtpPort = 587
if (-not [string]::IsNullOrWhiteSpace($smtpPortRaw)) {
    $smtpPort = [int]$smtpPortRaw
}

$transport = 'outlook'
if (-not [string]::IsNullOrWhiteSpace($smtpHost)) {
    $transport = 'smtp'
}

$redacted = Get-RedactedEmail -Email $resolvedTo

if ($DryRun) {
    Write-Output "status=dry_run"
    Write-Output "transport=$transport"
    Write-Output "to_redacted=$redacted"
    Write-Output "summary=would send via $transport to $redacted"
    exit 0
}

if ([string]::IsNullOrWhiteSpace($ReportPath)) {
    Write-Error 'ReportPath is required unless -SelfTest'
    exit 1
}
if (-not (Test-Path -LiteralPath $ReportPath)) {
    Write-Error "Report file not found: $ReportPath"
    exit 1
}

$reportText = [System.IO.File]::ReadAllText((Resolve-Path -LiteralPath $ReportPath).Path)
$html = ConvertTo-ReportHtml -Markdown $reportText
$fullReportPath = (Resolve-Path -LiteralPath $ReportPath).Path

if ($transport -eq 'smtp') {
    if ([string]::IsNullOrWhiteSpace($resolvedFrom)) {
        Write-Error 'SMTP send requires NIGHT_ISSUE_PRS_MAIL_FROM or NIGHT_ISSUE_PRS_SMTP_USER'
        exit 1
    }
    $message = New-Object System.Net.Mail.MailMessage
    $message.From = New-Object System.Net.Mail.MailAddress($resolvedFrom)
    $message.To.Add($resolvedTo)
    $message.Subject = $Subject
    $message.Body = $html
    $message.IsBodyHtml = $true
    $attachment = New-Object System.Net.Mail.Attachment($fullReportPath)
    $message.Attachments.Add($attachment) | Out-Null
    $client = $null
    try {
        $client = New-Object System.Net.Mail.SmtpClient($smtpHost, $smtpPort)
        $client.EnableSsl = $true
        if (-not [string]::IsNullOrWhiteSpace($smtpUser)) {
            $secure = ConvertTo-SecureString -String $smtpPass -AsPlainText -Force
            $client.Credentials = New-Object System.Net.NetworkCredential($smtpUser, $secure)
        }
        $client.Send($message)
    } finally {
        $attachment.Dispose()
        $message.Dispose()
        if ($null -ne $client) {
            $client.Dispose()
        }
    }
    Write-Output 'status=sent'
    Write-Output "transport=smtp"
    Write-Output "to_redacted=$redacted"
    Write-Output "summary=sent via smtp to $redacted"
    exit 0
}

try {
    $outlook = New-Object -ComObject Outlook.Application
} catch {
    Write-Error "Outlook COM unavailable and NIGHT_ISSUE_PRS_SMTP_HOST unset. $($_.Exception.Message)"
    exit 1
}

$mail = $outlook.CreateItem(0)
$mail.To = $resolvedTo
$mail.Subject = $Subject
$mail.HTMLBody = $html
$mail.Attachments.Add($fullReportPath) | Out-Null
$mail.Send()
Write-Output 'status=sent'
Write-Output 'transport=outlook'
Write-Output "to_redacted=$redacted"
Write-Output "summary=sent via outlook to $redacted"
exit 0
