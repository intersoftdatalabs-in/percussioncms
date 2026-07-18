# Percussion CMS Linux service (systemd)

## Overview

On modern Linux hosts, `install-jetty-service.sh` installs a **native systemd unit**
for the CMS Jetty process (default name `PercussionCMS`). SysV/init.d remains
available as a fallback when systemd is not present or when forced with `--initd`.

This addresses [GitHub issue #962](https://github.com/intersoftdatalabs-in/percussioncms/issues/962):
short start timeouts and empty journals when systemd wraps LSB init scripts during
long post-upgrade startups.

## Install (systemd — default when available)

```bash
sudo ./install-jetty-service.sh [ServiceName] install
# optional flags:
#   --systemd   require systemd (fail if missing)
#   --initd     force classic init.d / chkconfig path
```

Prompts for the run-as user (same as before), writes:

| Artifact | Path |
|----------|------|
| Environment | `/etc/default/<ServiceName>` |
| Start helper | `/etc/init.d/<ServiceName>` (used by ExecStart; **not** enabled via chkconfig on systemd hosts) |
| Unit | `/etc/systemd/system/<ServiceName>.service` |

Then:

```bash
sudo systemctl daemon-reload
sudo systemctl enable <ServiceName>
sudo systemctl start <ServiceName>
sudo systemctl status <ServiceName>
journalctl -u <ServiceName> -n 100 --no-pager
```

## Long startup / upgrades

The unit sets `TimeoutStartSec=1800` (30 minutes) so package/DB upgrade work during
start does not false-fail under systemd. To raise further, use a drop-in:

```bash
sudo systemctl edit PercussionCMS
# [Service]
# TimeoutStartSec=3600
```

Application logs remain under `JETTY_BASE/logs/` (see Log4j2 `server.log`).

## Uninstall

```bash
sudo ./install-jetty-service.sh [ServiceName] uninstall
```

Disables and removes the systemd unit (when present), removes init.d/default files,
and clears the run directory for that service name.

## Migration from init.d-only installs

1. `sudo ./install-jetty-service.sh PercussionCMS uninstall` (stops service if running)
2. `sudo ./install-jetty-service.sh PercussionCMS install` (picks systemd when available)
3. `sudo systemctl start PercussionCMS`

## Force init.d (no systemd unit)

```bash
sudo ./install-jetty-service.sh PercussionCMS install --initd
```
