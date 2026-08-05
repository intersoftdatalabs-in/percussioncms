# Percussion CMS Linux service (systemd)

## Overview

On modern Linux hosts, `install-jetty-service.sh` installs a **native systemd unit**
for the CMS Jetty process (default name `PercussionCMS`). SysV/init.d remains
available as:

1. **Start helper** — `ExecStart` / `ExecStop` always invoke `/etc/init.d/<ServiceName>`
   even when a native unit is registered (the unit does **not** dual-register that
   helper via chkconfig / update-rc.d on the systemd path).
2. **Fallback** — when systemd is absent, or when forced with `--initd`.

This addresses [GitHub issue #962](https://github.com/intersoftdatalabs-in/percussioncms/issues/962)
(parent of slice [#1977](https://github.com/intersoftdatalabs-in/percussioncms/issues/1977)):
short start timeouts and empty journals when systemd wraps LSB init scripts during
long post-upgrade startups.

**init.d is not removed** by this feature. Do not delete SysV scripts or the
`--initd` path when operating dual-ship hosts.

## Install flags

|    Flag     |                                        Behavior                                         |
|-------------|-----------------------------------------------------------------------------------------|
| (default)   | Prefer native systemd when `/run/systemd/system` exists and `systemctl` is available    |
| `--systemd` | **Require** systemd; fail install if not available                                      |
| `--initd`   | Force classic init.d / chkconfig (or update-rc.d) registration; **no** native unit file |
| both flags  | Rejected (`Cannot combine --systemd and --initd`)                                       |

## Install (systemd — default when available)

```bash
# Must run as root (or via sudo). There is no --dry-run flag.
sudo ./install-jetty-service.sh [ServiceName] install
# optional flags:
#   --systemd   require systemd (fail if missing)
#   --initd     force classic init.d / chkconfig path
```

Prompts for the run-as user (same as before), writes:

|   Artifact   |                                                                 Path                                                                  |
|--------------|---------------------------------------------------------------------------------------------------------------------------------------|
| Environment  | `/etc/default/<ServiceName>`                                                                                                          |
| Start helper | `/etc/init.d/<ServiceName>` (from `jetty/defaults/bin/rxjetty.sh`; used by ExecStart; **not** enabled via chkconfig on systemd hosts) |
| Unit         | `/etc/systemd/system/<ServiceName>.service`                                                                                           |

The start-helper template ships at `<rxDir>/jetty/defaults/bin/rxjetty.sh` (GH-1983). Install
substitutes `${rxjetty_service}` into `/etc/init.d/<ServiceName>` for **both** systemd and
`--initd` paths. Service install hard-fails if that template is missing.

Then:

```bash
sudo systemctl daemon-reload
sudo systemctl enable <ServiceName>
sudo systemctl start <ServiceName>
sudo systemctl status <ServiceName>
journalctl -u <ServiceName> -n 100 --no-pager
```

### Privilege model

- The **unit does not set `User=`**; systemd starts the unit as root.
- The Jetty init helper drops / applies the process user from `JETTY_USER` in
  `/etc/default/<ServiceName>` (set from the install-time run-as prompt).
- This matches the historic init.d-only path.

## Root requirement and non-root / dry-run limitations

`install-jetty-service.sh` **requires root** (`id -u` must be `0`). It writes under
`/etc/systemd/system`, `/etc/init.d`, `/etc/default`, and run directories, then runs
`systemctl daemon-reload` / `enable`.

|               What you need               |                                  Non-root / dry-run                                  |
|-------------------------------------------|--------------------------------------------------------------------------------------|
| Live install, enable, start, uninstall    | **Not supported** without root                                                       |
| Inspect unit template keys before install | **Yes** — read `percussion-cms.service.in` next to this README                       |
| Verify flags and selection logic          | **Yes** — read `install-jetty-service.sh` (no host writes)                           |
| Structural contract tests (CI / dev)      | **Yes** — Maven tests; no root, no live systemd                                      |
| Preview generated unit without installing | **Manual** — copy template, substitute placeholders offline (no product `--dry-run`) |

There is **no** `--dry-run` installer flag. Offline review of the template + scripts is
the supported dry-run path (see checklist below). Live dual-ship soak is out of scope
for packaging verification on a non-root workstation.

## Dry-run install checklist (no live root)

Use this on a packaging machine, CI agent, or non-root workstation:

1. **Confirm artifacts ship**:
   - under `<rxDir>/jetty/service/`: `percussion-cms.service.in`, `install-jetty-service.sh`,
     this `README-systemd.md`
   - under `<rxDir>/jetty/defaults/bin/`: `rxjetty.sh` (start-helper template; required by install)
2. **Contract keys** in the unit template (see
   `specs/988-linux-systemd-services/contracts/systemd-unit-contract.md`):
   - `Type=forking`, `PIDFile=`, `EnvironmentFile=`, `ExecStart`/`ExecStop`
   - `TimeoutStartSec` ≥ 900 (default **1800**), `StandardOutput`/`StandardError=journal`
   - `WantedBy=multi-user.target`, `After=network.target`
   - Privilege model documented (no bare `User=` without docs)
3. **Flags**: script documents `--systemd` and `--initd`; both together must fail.
4. **Init.d retained**: systemd path keeps `/etc/init.d/<name>` as ExecStart helper and
   skips SysV boot registration; `--initd` still registers classic boot.
5. **Run structural tests** (no root):

   ```bash
   # from repo, module standalone
   cd modules/perc-jetty
   ../../mvnw test -Dtest=SystemdUnitTemplateTest,InstallJettyServiceScriptTest,RxJettyStartHelperTemplateTest
   ```
6. **Migration rehearsal (document only)**: uninstall → install order below; do not
   run against production without change control.

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

Always **uninstall then install** (do not layer a second registration on the same name):

1. `sudo ./install-jetty-service.sh PercussionCMS uninstall` (stops service if running)
2. `sudo ./install-jetty-service.sh PercussionCMS install` (picks systemd when available)
3. `sudo systemctl start PercussionCMS`
4. `sudo systemctl status PercussionCMS` and `journalctl -u PercussionCMS -n 50 --no-pager`

## Force init.d (no systemd unit)

```bash
sudo ./install-jetty-service.sh PercussionCMS install --initd
```

Use when the host has no systemd, or when operators intentionally keep SysV-only
boot registration. The init.d script remains the start helper on the systemd path
as well — forcing `--initd` only changes **boot registration**, not the existence
of the helper script on a later native-unit reinstall.
