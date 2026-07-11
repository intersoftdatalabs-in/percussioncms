# Contract: /etc/percussion/cms-<instance>.env

**Date**: 2026-07-11

This contract specifies the per-instance environment file read by the systemd unit via `EnvironmentFile=`.

## File location

`/etc/percussion/cms-<instance>.env`

Where `<instance>` matches the systemd template instance name (`default` for a single-instance install; `instance2`, `instance3`, … for multi-instance).

## File mode / ownership

- Mode: `0640`
- Owner: `root:<JETTY_USER-group>` (group matches the runtime user)
- The unit's `User=` and `Group=` (set from this file) MUST have read access.

## Format

Plain text, one `key=value` per line, `LF` line endings. No `export` prefix. No command substitution. systemd reads the file as `key=value` without shell expansion.

## Required keys

| Key | Example | Description |
|-----|---------|-------------|
| `PERC_ROOT` | `/opt/percussion/perc-cms` | Install root |
| `JETTY_HOME` | `/opt/percussion/perc-cms/Jetty/upstream` | Jetty home |
| `JETTY_BASE` | `/opt/percussion/perc-cms/Jetty/base` | Jetty base |
| `JETTY_DEFAULTS` | `/opt/percussion/perc-cms/Jetty/defaults` | Jetty defaults |
| `JETTY_RUN` | `/var/run/rxjetty/perc-cms` | Runtime dir |
| `JETTY_CONF` | `/opt/percussion/perc-cms/Jetty/base/etc/jetty.conf` | Jetty config |
| `JETTY_START_LOG` | `/opt/percussion/perc-cms/Jetty/base/logs/start.log` | Start log |
| `JETTY_PID` | `/var/run/rxjetty/perc-cms/rxjetty.pid` | PID file |
| `JAVA_HOME` | `/opt/percussion/perc-cms/JRE64` | JDK install |
| `JAVA` | `${JAVA_HOME}/bin/java` | java binary |
| `JAVA_OPTIONS` | `-XX:+DisableAttachMechanism -Drxdeploydir=...` | JVM options (single line, no embedded newlines) |
| `JETTY_ARGS` | `--include-jetty-dir=...` | Jetty args |
| `JETTY_USER` | `percussion` | Runtime user |
| `INSTANCE_NAME` | `default` | The instance identifier |

## Validation

A file is "valid" when:

1. Mode is exactly `0640` (verified by `stat -c '%a'`).
2. All required keys are present.
3. All path values resolve to existing directories or files on disk.
4. No line contains `$(`, backticks, or `export ` (migration script defensively rejects these).
5. `systemctl show percussioncms@<instance>.service -p Environment` shows all expected `KEY=VALUE` pairs (substituting actual env values from the file).

## Security notes (SC-006)

- This file MUST NOT contain DB passwords, JWT signing keys, or other secrets. Secrets belong in CMS application config under `${PERC_ROOT}/rxconfig/Server/`, which is protected by OS file permissions on the install tree (not by this file).
- Migration from legacy `/etc/default/<name>` files: the migration script copies non-secret keys and WARNs (does not silently drop) on any key matching a known-secret pattern (`PASSWORD`, `SECRET`, `KEY`, `TOKEN` in the value).
- The directory `/etc/percussion/` MUST be created with mode `0755 root:root`; per-file mode is `0640` as above.

## Example (illustrative, not for direct copy)

```ini
PERC_ROOT=/opt/percussion/perc-cms
JETTY_HOME=/opt/percussion/perc-cms/Jetty/upstream
JETTY_BASE=/opt/percussion/perc-cms/Jetty/base
JETTY_DEFAULTS=/opt/percussion/perc-cms/Jetty/defaults
JETTY_RUN=/var/run/rxjetty/perc-cms
JETTY_CONF=/opt/percussion/perc-cms/Jetty/base/etc/jetty.conf
JETTY_START_LOG=/opt/percussion/perc-cms/Jetty/base/logs/start.log
JETTY_PID=/var/run/rxjetty/perc-cms/rxjetty.pid
JAVA_HOME=/opt/percussion/perc-cms/JRE64
JAVA=/opt/percussion/perc-cms/JRE64/bin/java
JAVA_OPTIONS=-XX:+DisableAttachMechanism -Drxdeploydir=/opt/percussion/perc-cms -Djetty_perc_defaults=/opt/percussion/perc-cms/Jetty/defaults
JETTY_ARGS=--include-jetty-dir=/opt/percussion/perc-cms/Jetty/defaults
JETTY_USER=percussion
INSTANCE_NAME=default
```
