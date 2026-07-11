# Data Model: Systemd Linux Service Scripts

**Date**: 2026-07-11
**Spec**: [spec.md](./spec.md)

This feature has no application data model — it is a supervision-layer change. The "entities" below describe the *artifacts* on disk and in the installer that must be tracked and validated.

## Entity: Unit File Template (`*.service.template`)

A Mustache-style or `${VAR}`-substituted template shipped in `modules/perc-distribution-tree/src/main/resources/systemd/`. At install time the installer substitutes paths and writes the rendered file to `/etc/systemd/system/<unit-name>.service`.

| Attribute | Type | Required | Description |
|-----------|------|----------|-------------|
| `Unit.Description` | string | yes | Human-readable description, shown by `systemctl status`. |
| `Unit.Documentation` | URL | yes | Link to operator docs (Maven site page). |
| `Unit.After` | list | yes | `network-online.target` (CMS depends on network). |
| `Service.Type` | enum | yes | `simple` (per Decision 1). |
| `Service.EnvironmentFile` | path | yes | `/etc/percussion/cms-%i.env`. |
| `Service.ExecStart` | command | yes | `${PERC_ROOT}/Jetty/base/StartJetty.sh`. |
| `Service.ExecStop` | command | yes | `${PERC_ROOT}/Jetty/base/StopJetty.sh`. |
| `Service.PIDFile` | path | yes | `${JETTY_RUN}/rxjetty.pid`. |
| `Service.Restart` | enum | yes | `on-failure`. |
| `Service.RestartSec` | duration | yes | `30s`. |
| `Service.TimeoutStartSec` | duration | yes | `300s` (initial; tune in `/speckit.tasks`). |
| `Service.TimeoutStopSec` | duration | yes | `120s` (initial; tune in `/speckit.tasks`). |
| `Service.User` | string | yes | Runtime user (extracted from `rx_user.id`). |
| `Service.Group` | string | yes | Runtime group. |
| `Service.KillMode` | enum | yes | `mixed`. |
| `Service.NoNewPrivileges` | bool | yes | `true`. |
| `Service.ProtectSystem` | enum | yes | `strict`. |
| `Service.ProtectHome` | bool | yes | `true`. |
| `Service.PrivateTmp` | bool | yes | `true`. |
| `Service.ReadWritePaths` | path list | yes | `PERC_ROOT`, `JETTY_RUN`. |
| `Service.StandardOutput` | enum | yes | `journal`. |
| `Service.StandardError` | enum | yes | `journal`. |
| `Install.WantedBy` | list | yes | `multi-user.target`. |

**Relationships**: A template renders to N concrete unit files when N instances exist (`percussioncms@default.service`, `percussioncms@instance2.service`, …).

**Validation rules**:
- `User` and `Group` MUST exist on the target host.
- `PERC_ROOT` and `JETTY_RUN` from `EnvironmentFile=` MUST exist on disk.
- The rendered unit MUST pass `systemd-analyze verify`.

---

## Entity: Environment File (`/etc/percussion/cms-<instance>.env`)

| Attribute | Type | Required | Description |
|-----------|------|----------|-------------|
| `PERC_ROOT` | absolute path | yes | Install root (e.g. `/opt/percussion/perc-cms`). |
| `JETTY_HOME` | absolute path | yes | Jetty home (e.g. `${PERC_ROOT}/Jetty/upstream`). |
| `JETTY_BASE` | absolute path | yes | Jetty base (e.g. `${PERC_ROOT}/Jetty/base`). |
| `JETTY_DEFAULTS` | absolute path | yes | Jetty defaults (e.g. `${PERC_ROOT}/Jetty/defaults`). |
| `JETTY_RUN` | absolute path | yes | Runtime dir (e.g. `/var/run/rxjetty/percussioncms`). |
| `JETTY_CONF` | absolute path | yes | `jetty.conf` path. |
| `JETTY_START_LOG` | absolute path | yes | Start log path. |
| `JETTY_PID` | absolute path | yes | PID file path (`${JETTY_RUN}/rxjetty.pid`). |
| `JAVA_HOME` | absolute path | yes | JDK install. |
| `JAVA` | absolute path | yes | `java` binary. |
| `JAVA_OPTIONS` | string | yes | JVM options (existing `DisableAttachMechanism` etc. preserved). |
| `JETTY_ARGS` | string | yes | Jetty module include args. |
| `JETTY_USER` | string | yes | Runtime user (mirrors `Service.User`). |
| `INSTANCE_NAME` | string | yes | The instance identifier (`default`, `instance2`, …). |

**File mode**: `0640`, owner `root:<JETTY_USER-group>`. Group MUST match `Service.Group`.

**Validation rules**:
- File MUST be syntactically valid `key=value` lines; the migration script defensively strips `$(...)`, backticks, and `export` prefixes.
- All path values MUST exist after install; missing paths fail the install.
- No secret material (DB passwords, JWT signing keys) MUST be in this file — those belong in CMS application config under `rxconfig/Server/`, not in the unit's env file.

---

## Entity: Legacy Init.d Script (deleted in this release)

Files deleted:

- `system/release/installer/Linux/percussion-service.sh`
- `system/release/installer/Linux/InstallPublisherDaemon.sh`
- `system/release/installer/Linux/InstallDaemon.sh`
- `system/release/installer/Linux/InstallFTSDaemon.sh`
- `system/release/installer/unix/InstallPublisherDaemon.sh`
- `system/release/installer/unix/InstallDaemon.sh`
- `system/release/installer/unix/InstallFTSDaemon.sh`
- `deliverytiersuite/.../DTSStagingService.sh`
- `deliverytiersuite/.../DTSProductionService.sh`

**Lifecycle**:
- **Pre-install**: script may exist on the target host (legacy install).
- **Migration**: script is stopped, symlinks under `/etc/rc?.d/` are removed, the script itself is removed from `/etc/init.d/`, `chkconfig` / `update-rc.d` deregistration is run.
- **Post-install**: script MUST NOT exist on the target host (FR-007 / SC-004).

**Migration detection inputs** (read-only, used by the migration script):

| Signal | Path / Pattern |
|--------|----------------|
| Init.d script | `/etc/init.d/{percussion,rxjetty,PercussionCMS,PercussionD,DTSStagingService,DTSProductionService}` |
| Run-level symlinks | `/etc/rc?.d/[SK]??<name>`, `/etc/rc.d/rc?.d/[SK]??<name>` |
| chkconfig | `chkconfig --list | grep <name>` |
| Default config | `/etc/default/<name>` |
| Service wrapper jar | `${PERC_ROOT}/perc-service-wrapper.jar` |
| Install marker | `${PERC_ROOT}/rxconfig/Server/objectstore.properties` |

---

## Entity: Instance Record (in-memory only)

The migration script builds an in-memory list of instances during detection. Not persisted.

| Field | Type | Description |
|-------|------|-------------|
| `instance_name` | string | `default`, `instance2`, … |
| `perc_root` | path | Detected `PERC_ROOT` |
| `jetty_home` | path | Detected `JETTY_HOME` |
| `jetty_base` | path | Detected `JETTY_BASE` |
| `jetty_run` | path | Detected `JETTY_RUN` |
| `jetty_user` | string | Detected runtime user |
| `java_options` | string | Detected `JAVA_OPTIONS` |
| `legacy_initd_path` | path | The legacy script path that produced this instance |
| `state` | enum | `detected | migrating | migrated | failed` |

**State transitions**:
- `detected → migrating` when the migration script begins processing
- `migrating → migrated` when `systemctl is-active` returns `active`
- `migrating → failed` when `systemctl start` fails (FR-011 recovery path)
- `failed → migrating` on retry

---

## Entity: Installer Manifest Reference (deleted)

Files in `modules/perc-ant` and `modules/perc-distribution-tree` (Maven `xml` / `properties`) that reference the legacy init.d scripts MUST be updated to reference the new systemd unit files instead. The tasks phase enumerates these via grep before deletion.
