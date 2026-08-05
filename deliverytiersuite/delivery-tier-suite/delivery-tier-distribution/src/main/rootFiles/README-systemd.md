# Percussion DTS Linux services (systemd)

Production and Staging DTS installers prefer a **native systemd unit** when
systemd is available (same approach as CMS Jetty / GH-962; docs parity for slice
[#1977](https://github.com/intersoftdatalabs-in/percussioncms/issues/1977)).

|         Installer         |   Default service name    |
|---------------------------|---------------------------|
| `DTSProductionService.sh` | `PercussionProductionDTS` |
| `DTSStagingService.sh`    | `PercussionStagingDTS`    |

### Dual-ship policy (keep init.d until soak)

**Both** systemd and init.d paths ship together. Keep init.d until a live Linux
install soak signs off and ops review completes (GH-1978 residual; do not remove
init.d under GH-1976 without human approval). Packaging guards:
`DtsLinuxServiceDualShipPackagingTest`.

Shared unit template: `dts-tomcat.service.in` (must sit next to the role
installer under `Deployment/Server/` after install/upgrade — `installDts.xml`
co-locates it with `DTS*Service.sh`; GH-1984). Ops README also remains at the
DTS install root and is co-located under `Deployment/Server/` on Linux.

SysV/init.d remains available as:

1. **Start helper** — `ExecStart` / `ExecStop` invoke `/etc/init.d/<ServiceName>`
   even when a native unit is registered (no dual chkconfig / update-rc.d on the
   systemd path).
2. **Fallback** — when systemd is absent, or when forced with `--initd`.

**init.d is not removed** by this feature. Do not delete SysV scripts or the
`--initd` path when operating dual-ship hosts. Windows `.bat` installers are
unchanged (Tomcat Windows service).

## Install flags

|    Flag     |                                       Behavior                                       |
|-------------|--------------------------------------------------------------------------------------|
| (default)   | Prefer native systemd when `/run/systemd/system` exists and `systemctl` is available |
| `--systemd` | **Require** systemd; fail install if not available                                   |
| `--initd`   | Force classic SysV/init.d registration only; **no** native unit file                 |
| both flags  | Rejected (`Cannot combine --systemd and --initd`)                                    |

## Install

```bash
# Must run as root (or via sudo). There is no --dry-run flag.
# From Deployment/Server/ (where installDts places the role script + unit template)
cd /path/to/DTS/Deployment/Server
sudo ./DTSProductionService.sh [ServiceName] install
sudo ./DTSStagingService.sh [ServiceName] install

# Flags (optional):
#   --systemd   require systemd
#   --initd     force classic SysV/init.d registration only
```

On systemd hosts the installer writes:

|   Artifact   |                                              Path                                               |
|--------------|-------------------------------------------------------------------------------------------------|
| Environment  | `/etc/default/<ServiceName>`                                                                    |
| Start helper | `/etc/init.d/<ServiceName>` (used by ExecStart; **not** enabled via chkconfig on systemd hosts) |
| Unit         | `/etc/systemd/system/<ServiceName>.service`                                                     |

Then:

```bash
sudo systemctl start PercussionProductionDTS
sudo systemctl status PercussionProductionDTS
journalctl -u PercussionProductionDTS -n 100 --no-pager
```

`TimeoutStartSec=1800` (30 minutes). Override with `systemctl edit <name>`.

Application logs: `$CATALINA_BASE/logs/` (and Log4j2 configs under `conf/perc/`).

### Privilege model

- The **unit does not set `User=`**; systemd starts the unit as root.
- Install scripts chown the Tomcat deployment tree to the install-directory owner
  and install the catalina-based init helper used by `ExecStart` (same as the
  historic init.d-only path).
- See comments in `dts-tomcat.service.in` (contract: `User=` present or documented).

## Root requirement and non-root / dry-run limitations

`DTSProductionService.sh` and `DTSStagingService.sh` **require root** (`id -u` must
be `0`). They write under `/etc/systemd/system`, `/etc/init.d`, `/etc/default`, and
run directories, then run `systemctl daemon-reload` / `enable`.

|               What you need               |                                  Non-root / dry-run                                  |
|-------------------------------------------|--------------------------------------------------------------------------------------|
| Live install, enable, start, uninstall    | **Not supported** without root                                                       |
| Inspect unit template keys before install | **Yes** — read `dts-tomcat.service.in` next to this README                           |
| Verify flags and selection logic          | **Yes** — read `DTSProductionService.sh` / `DTSStagingService.sh` (no host writes)   |
| Structural contract tests (CI / dev)      | **Yes** — Maven tests; no root, no live systemd                                      |
| Preview generated unit without installing | **Manual** — copy template, substitute placeholders offline (no product `--dry-run`) |

There is **no** `--dry-run` installer flag. Offline review of the template + scripts is
the supported dry-run path (see checklist below).

## Dry-run install checklist (no live root)

1. **Confirm artifacts ship** at the DTS product surface / `Deployment/Server/` layout
   after packaging: `dts-tomcat.service.in`, `DTSProductionService.sh`,
   `DTSStagingService.sh`, this `README-systemd.md`.
2. **Contract keys** in `dts-tomcat.service.in` (same table as CMS; see
   `specs/988-linux-systemd-services/contracts/systemd-unit-contract.md`):
   - `Type=forking`, `PIDFile=`, `EnvironmentFile=`, `ExecStart`/`ExecStop`
   - `TimeoutStartSec` ≥ 900 (default **1800**), journal stdout/stderr
   - `WantedBy=multi-user.target`, `After=network.target`
   - Privilege model documented
3. **Flags**: both installers document `--systemd` and `--initd`; both together fail.
4. **Init.d retained**: systemd path keeps the init.d helper for ExecStart and skips
   SysV boot registration; `--initd` still registers classic boot.
5. **Run structural tests** (no root):

   ```bash
   cd deliverytiersuite/delivery-tier-suite/delivery-tier-distribution
   ../../../mvnw test -Dtest=DtsSystemdUnitTemplateTest,DtsServiceInstallScriptTest
   ```
6. **Migration rehearsal (document only)**: uninstall → install order below.

## Uninstall

```bash
sudo ./DTSProductionService.sh [ServiceName] uninstall
sudo ./DTSStagingService.sh [ServiceName] uninstall
```

Disables and removes the systemd unit (when present), removes init.d/default files,
and clears the run directory for that service name.

## Migration from init.d-only

Always **uninstall then install** (do not layer a second registration on the same name):

1. `sudo ./DTSProductionService.sh PercussionProductionDTS uninstall`
2. `sudo ./DTSProductionService.sh PercussionProductionDTS install` (picks systemd when available)
3. `sudo systemctl start PercussionProductionDTS`
4. `sudo systemctl status PercussionProductionDTS` and
   `journalctl -u PercussionProductionDTS -n 50 --no-pager`

Same pattern for Staging (`DTSStagingService.sh` / `PercussionStagingDTS`).

## Force init.d (no systemd unit)

```bash
sudo ./DTSProductionService.sh PercussionProductionDTS install --initd
sudo ./DTSStagingService.sh PercussionStagingDTS install --initd
```

Use when the host has no systemd, or when operators intentionally keep SysV-only
boot registration. The init.d script remains the start helper on the systemd path
as well — forcing `--initd` only changes **boot registration**.

## Windows

`.bat` installers are unchanged (Tomcat Windows service).
