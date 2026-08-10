# Default log rotation samples (CMS / Jetty + DTS / Tomcat)

**Issue:** [#2348](https://github.com/intersoftdatalabs-in/percussioncms/issues/2348)  
**Ship path (CMS install tree):** `<install-root>/rxconfig/Installer/logrotate/`

These files are **operator samples**. They are **not** auto-enabled on Linux or Windows. Copy or schedule them only with operator consent after path substitution and a dry-run.

## Contents

|           File            |                                                   Role                                                   |
|---------------------------|----------------------------------------------------------------------------------------------------------|
| `percussion-cms`          | Linux `logrotate` fragment for CMS Jetty log roots                                                       |
| `percussion-dts`          | Linux `logrotate` fragment for DTS Tomcat `Deployment/Server/logs` (includes `catalina.out` via `*.out`) |
| `schedule-clean-logs.ps1` | Windows sample: scheduled `perc-doctor clean-logs` (14-day default)                                      |
| `README.md`               | This guide                                                                                               |

Standalone DTS packages also ship `logrotate/percussion-dts` (+ this guide) at the DTS install root for split-host layouts.

## Log roots covered

Aligned with `perc-doctor` `InstallRootGuard.LOG_DIR_RELATIVE` and install layout:

|   Product    |             Relative root              |                             Typical files                              |
|--------------|----------------------------------------|------------------------------------------------------------------------|
| CMS / Jetty  | `jetty/base/logs`                      | Jetty / request / layout logs, `audit/`                                |
| CMS / Jetty  | `jetty/base/modules/perc-logging/logs` | Log4j2 app logs (`server.log`, rotations)                              |
| DTS / Tomcat | `Deployment/Server/logs`               | `catalina.out`, `catalina.*.log`, access / host-manager / manager logs |

Globs are limited to `*.log` and `*.out` (same spirit as `InstallRootGuard.isLogFileName`).

## Linux: enable logrotate (manual)

**Default retention:** daily, keep 14 compressed archives, `copytruncate`, `missingok`, `notifempty`, `delaycompress`.

1. Substitute the install root (`/opt/Percussion` is the documented placeholder).

   ```bash
   INSTALL_ROOT=/opt/Percussion   # or $PERCUSSION_HOME / $DTS_HOME
   sed "s|/opt/Percussion|${INSTALL_ROOT}|g" \
     "${INSTALL_ROOT}/rxconfig/Installer/logrotate/percussion-cms" \
     > /tmp/percussion-cms
   ```
2. Install into `logrotate.d` **only when you intend to enable OS rotation**:

   ```bash
   sudo install -m 0644 /tmp/percussion-cms /etc/logrotate.d/percussion-cms
   # Optional second host / co-located DTS:
   sed "s|/opt/Percussion|${INSTALL_ROOT}|g" \
     "${INSTALL_ROOT}/rxconfig/Installer/logrotate/percussion-dts" \
     | sudo tee /etc/logrotate.d/percussion-dts >/dev/null
   sudo chmod 0644 /etc/logrotate.d/percussion-dts
   ```
3. Dry-run (no changes):

   ```bash
   sudo logrotate -d /etc/logrotate.d/percussion-cms
   sudo logrotate -d /etc/logrotate.d/percussion-dts
   ```
4. Optional one-shot force after dry-run looks correct:

   ```bash
   sudo logrotate -f /etc/logrotate.d/percussion-cms
   ```

### Why `copytruncate`

Jetty, Log4j2, and Tomcat often keep log file handles open. `copytruncate` rotates without requiring a process restart or signal. Optional `postrotate` `systemctl reload` stanzas are commented in the samples for sites that standardize on dual-ship systemd units (GH-962).

### Split CMS vs DTS hosts

- CMS-only host: install `percussion-cms` only.
- DTS-only host: install `percussion-dts` with paths under that host’s DTS install root (`…/Deployment/Server/logs`). Use the copy shipped under CMS `rxconfig/Installer/logrotate/` or the standalone DTS package `logrotate/` directory.
- Co-located: install both fragments; both may share the same `/opt/Percussion` (or site-specific) root.

## Coexistence: Log4j2, logrotate, perc-doctor

|                              Layer                              |                                                       Owns                                                        |                                   Default behaviour                                    |
|-----------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------|
| **Log4j2 RollingFile** (CMS perc-logging / DTS service configs) | Active application log streams                                                                                    | Size-based rollover (CMS pattern: ~10 MB × keep 10)                                    |
| **OS logrotate** (these samples)                                | Continuous `*.log` / `*.out` under known roots, especially `catalina.out` and anything not fully handled by Log4j | Daily, 14 rotations, compress, **copytruncate**                                        |
| **perc-doctor `clean-logs`**                                    | Allowlisted log **files** under the same roots                                                                    | Age / keep-current purge — **manual or scheduled CLI**, not a replacement for rotation |

Recommended production posture:

1. Leave Log4j2 defaults as shipped (size caps for app logs).
2. Enable OS logrotate for Linux hosts that need a hard ceiling on `catalina.out` / unbounded `*.out` and long-lived archives.
3. Schedule `perc-doctor clean-logs --older-than 14d` (or site policy) to reclaim aged rotated/compressed files when disk policy requires it.

Double-rotation (Log4j + logrotate on the same basename) is an acceptable safety net: Log4j bounds active size; logrotate ages leftover handles; `clean-logs` deletes old allowlisted files. Prefer **not** excluding Log4j paths from logrotate unless support has a site-specific reason.

## Windows: scheduled clean-logs

Windows does not use classic `logrotate`. Documented equivalent:

1. Prefer dry-run:

   ```bat
   cd /d C:\Percussion
   bin\perc-doctor.bat --dry-run -v clean-logs --older-than 14d
   ```
2. Or use the sample script (defaults to dry-run):

   ```powershell
   cd C:\Percussion\rxconfig\Installer\logrotate
   .\schedule-clean-logs.ps1 -InstallRoot 'C:\Percussion' -DryRun $true
   ```
3. After review, schedule apply with Task Scheduler (example action):

   ```text
   Program: powershell.exe
   Arguments:
     -NoProfile -ExecutionPolicy Bypass -File "C:\Percussion\rxconfig\Installer\logrotate\schedule-clean-logs.ps1" -InstallRoot "C:\Percussion" -OlderThan "14d" -DryRun:$false
   Trigger: Daily (off-hours recommended)
   ```
4. Alternate Task Scheduler action without the script:

   ```text
   Program: C:\Percussion\bin\perc-doctor.bat
   Arguments: --install-root C:\Percussion -v clean-logs --older-than 14d
   ```

Always validate with `--dry-run` / `-DryRun $true` before enabling unattended apply.

## Support baseline

|          Item          |                             Default                              |
|------------------------|------------------------------------------------------------------|
| Canonical sample path  | `<install-root>/rxconfig/Installer/logrotate/`                   |
| Linux retention        | 14 daily compressed archives, `copytruncate`                     |
| Windows retention      | `perc-doctor clean-logs --older-than 14d` (keep-current default) |
| Auto-enable on install | **No** — operator consent required                               |

## Related

- `modules/perc-doctor` — `clean-logs`, operator install guide
- `modules/perc-jetty` — Log4j2 / Jetty logging layout
- DTS dual-ship systemd notes — `README-systemd.md` (optional postrotate)
- Parent packaging: `modules/perc-distribution-tree`, `delivery-tier-distribution`

