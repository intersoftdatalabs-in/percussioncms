# Inventory: Linux service install / start / stop (CMS Jetty + DTS)

**Issue**: #1975 (slice 1 of #962)  
**Validated against**: `main` at inventory capture (branch `fix/issue-1975-linux-service-inventory`)  
**Scope**: filesystem / packaging inventory only — no live root systemd soak, no init.d removal.

This document is the durable source of truth for paths claimed in #1975. Update it when packaging or scripts move.

---

## Summary verdict

|                                       Area                                       |                                    Verdict                                    |
|----------------------------------------------------------------------------------|-------------------------------------------------------------------------------|
| CMS Jetty source scripts under `modules/perc-jetty/src/main/jetty/service/`      | **Present** as documented                                                     |
| CMS dual-ship (systemd prefer + `--initd` / `--systemd`)                         | **Present** — no silent init.d removal                                        |
| CMS packaging of `service/` into Jetty distribution                              | **Present** via perc-jetty assembly → perc-distribution-tree                  |
| CMS init.d helper template `defaults/bin/rxjetty.sh`                             | **MISSING from source** (see [Gap A](#gap-a--cms-rxjettysh-template-missing)) |
| DTS source scripts + unit template under delivery-tier-distribution `rootFiles/` | **Present** as documented                                                     |
| DTS dual-ship                                                                    | **Present**                                                                   |
| DTS installDts placement of service scripts                                      | **Present** → `Deployment/Server/`                                            |
| DTS unit template co-location with service scripts                               | **MISMATCH** (see [Gap B](#gap-b--dts-unit-template-install-path))            |

---

## CMS Jetty (`modules/perc-jetty`)

### Source tree (validated)

|                        Path                        |                                       Role                                       |   Status   |
|----------------------------------------------------|----------------------------------------------------------------------------------|------------|
| `src/main/jetty/service/install-jetty-service.sh`  | Linux service install/uninstall; prefers native systemd; `--initd` / `--systemd` | Present    |
| `src/main/jetty/service/install-jetty-service.bat` | Windows Procrun (Linux slice out of scope)                                       | Present    |
| `src/main/jetty/service/percussion-cms.service.in` | systemd unit template (`Type=forking`, `TimeoutStartSec=1800`, journal)          | Present    |
| `src/main/jetty/service/README-systemd.md`         | Operator install / migrate / journal notes                                       | Present    |
| `src/main/jetty/service/win/`                      | Procrun binaries                                                                 | Present    |
| `src/main/jetty/StartJetty.sh` / `StopJetty.sh`    | Console / helper start-stop (PID under `/var/run/rxjetty/…`)                     | Present    |
| `src/main/jetty/resolve-java-home.sh`              | JAVA_HOME resolution (sourced by service install; GH-991)                        | Present    |
| `src/main/jetty/defaults/bin/rxjetty.sh`           | init.d **start-helper template** (`${rxjetty_service}` substitution)             | **Absent** |

### Packaging chain into a real CMS install

1. **perc-jetty process-resources** (`modules/perc-jetty/pom.xml`):
   - Unpacks Jetty home → `${assembly-directory}/upstream`
   - Copies **full** `src/main/jetty/**` → `${assembly-directory}/` (includes `service/`, `StartJetty.sh`, `resolve-java-home.sh`, `defaults/`, …)
2. **perc-jetty package**: `src/main/assembly/jetty-assembly.xml` packages `${assembly-directory}` as zip/tar.gz (`**` include; does not exclude `service/`).
3. **perc-distribution-tree**:
   - Maven unpack of `perc-jetty` artifact → `${jetty-directory}`
   - `installDistributionFiles.xml` copies that tree to `${assembly-directory}/jetty`
4. **Installed layout (operator-facing)**:  
   `<rxDir>/jetty/service/install-jetty-service.sh`  
   `<rxDir>/jetty/service/percussion-cms.service.in`  
   `<rxDir>/jetty/resolve-java-home.sh`  
   `<rxDir>/jetty/StartJetty.sh` / `StopJetty.sh`

No separate installer step is required to “pick” `service/` — it rides with the Jetty overlay copy. There is **no** additional filter that strips `service/` in distribution-tree.

### Runtime layout (when install script succeeds)

|             Artifact             |                                                Path                                                |
|----------------------------------|----------------------------------------------------------------------------------------------------|
| Default service name             | `PercussionCMS`                                                                                    |
| Environment file                 | `/etc/default/<ServiceName>`                                                                       |
| Start helper (always written)    | `/etc/init.d/<ServiceName>` (from `rxjetty.sh` template)                                           |
| systemd unit (when systemd path) | `/etc/systemd/system/<ServiceName>.service`                                                        |
| PID directory / file             | `/var/run/rxjetty/<ServiceName>/` · `…/rxjetty.pid`                                                |
| SysV boot registration           | **Only** on non-systemd / `--initd` path (`enableSysV`); **skipped** when native unit is installed |

### Dual-ship confirmation (CMS)

- Code comment header: `prefer native systemd unit; keep init.d as fallback`
- Flags: `--systemd`, `--initd` (mutually exclusive)
- systemd branch: installs unit, keeps `/etc/init.d` helper for `ExecStart`/`ExecStop`, message `SysV boot registration skipped`
- Fallback branch: `enableSysV` (chkconfig / update-rc.d / rc.d links)
- Structural tests assert dual-ship contracts (below)

### Structural tests (CMS)

|            Test class             |                               Path                               |
|-----------------------------------|------------------------------------------------------------------|
| `SystemdUnitTemplateTest`         | `modules/perc-jetty/src/test/java/com/percussion/jetty/service/` |
| `InstallJettyServiceScriptTest`   | same                                                             |
| `InstallJettyServiceJavaHomeTest` | same (GH-991 service Java wiring; still valid companion)         |

---

## DTS (`deliverytiersuite/delivery-tier-suite/delivery-tier-distribution`)

### Source tree (validated)

|                                  Path                                   |                           Role                           | Status  |
|-------------------------------------------------------------------------|----------------------------------------------------------|---------|
| `src/main/rootFiles/DTSProductionService.sh`                            | Production install/uninstall; systemd prefer + `--initd` | Present |
| `src/main/rootFiles/DTSStagingService.sh`                               | Staging install/uninstall                                | Present |
| `src/main/rootFiles/dts-tomcat.service.in`                              | Shared Tomcat unit template                              | Present |
| `src/main/rootFiles/README-systemd.md`                                  | Ops notes                                                | Present |
| `src/main/rootFiles/DTSProductionService.bat` / `DTSStagingService.bat` | Windows (out of Linux scope)                             | Present |
| `src/main/rootFiles/resolve-java-home.sh`                               | DTS JAVA_HOME helper                                     | Present |
| `src/main/rootFiles/rxconfig/Installer/installDts.xml`                  | Installer placement of service scripts                   | Present |

### Defaults

|  Variant   |  Default `SERVICE_NAME`   |            PID run parent (script)             |
|------------|---------------------------|------------------------------------------------|
| Production | `PercussionProductionDTS` | `/var/run/PercussionProductionService/<name>/` |
| Staging    | `PercussionStagingDTS`    | `/var/run/PercussionStagingService/<name>/`    |

Environment: `/etc/default/<ServiceName>` with `CATALINA_*` / `JAVA_HOME` (no shell commands in defaults).  
Unit path: `/etc/systemd/system/<ServiceName>.service`.  
init.d helper: `/etc/init.d/<ServiceName>` generated from `catalina.sh` (not a separate product template file).

### Packaging into a real DTS install

1. **delivery-tier-distribution process-resources**: copies `src/main/rootFiles/**` into the assembly / installer payload root.
2. **`installDts.xml` (`install-dts` macro)** — fresh + upgrade:
   - Copies product root `*` (platform-filtered) to `${install.dir}${staging.dir}/`
   - **Excludes** `DTSProductionService.{sh,bat}` and `DTSStagingService.{sh,bat}` from that root copy
   - **Separately** copies the selected Linux/Windows service script to:  
     `${install.dir}${staging.dir}/Deployment/Server/`
   - Does **not** specially copy `dts-tomcat.service.in` or `README-systemd.md` into `Deployment/Server/`

### Operator-facing paths after install

|                    Artifact                    |                               Intended / actual path                               |
|------------------------------------------------|------------------------------------------------------------------------------------|
| Service installer script                       | `<DTSRoot>[/Staging]/Deployment/Server/DTS{Production\|Staging}Service.sh`         |
| `catalina.sh`                                  | same Server tree `bin/catalina.sh`                                                 |
| `dts-tomcat.service.in` (script resolution)    | **Script expects**: same directory as service script (`dirname $0`)                |
| `dts-tomcat.service.in` (installDts placement) | **Lands at**: `<DTSRoot>[/Staging]/` (product surface root) via root `*` copy      |
| `README-systemd.md`                            | Product surface root (same as above)                                               |
| `resolve-java-home.sh`                         | Product surface root; scripts resolve via `INSTALL_ROOT` two levels up from Server |

### Dual-ship confirmation (DTS)

- Both Production and Staging scripts: `--initd` / `--systemd`, `use_systemd_install`, SysV skip message on systemd path
- Structural tests: `DtsServiceInstallScriptTest`, `DtsSystemdUnitTemplateTest`

### Structural tests (DTS)

|          Test class           |                          Path                           |
|-------------------------------|---------------------------------------------------------|
| `DtsSystemdUnitTemplateTest`  | `…/src/test/java/com/percussion/delivery/distribution/` |
| `DtsServiceInstallScriptTest` | same                                                    |

---

## Spec / prior implementation

|                 Item                  |                               Location / link                               |
|---------------------------------------|-----------------------------------------------------------------------------|
| Spec folder                           | `specs/988-linux-systemd-services/`                                         |
| Unit contract                         | `contracts/systemd-unit-contract.md`                                        |
| Merged CMS+DTS systemd implementation | PR [#1334](https://github.com/intersoftdatalabs-in/percussioncms/pull/1334) |
| Parent feature issue                  | [#962](https://github.com/intersoftdatalabs-in/percussioncms/issues/962)    |
| This inventory slice                  | [#1975](https://github.com/intersoftdatalabs-in/percussioncms/issues/1975)  |

### Spec drift notes (corrected here)

- `plan.md` still lists DTS as “Out of scope” in one decision table; **tasks T024–T027 and shipped code include DTS** (in-scope extension). Prefer this inventory + `spec.md` module scope for current truth.
- `plan.md` lists unit template name variants (`PercussionCMS.service.in` vs `percussion-cms.service.in`); **shipped name is** `percussion-cms.service.in`.
- `plan.md` lists `defaults/bin/rxjetty.sh` as a path to maintain; **file is not in the tree** (Gap A).

---

## Gap A — CMS `rxjetty.sh` template missing

**Symptom**: `install-jetty-service.sh` `installInitScriptAndDefaults` requires:

```text
${JETTY_DEFAULTS}/bin/rxjetty.sh
```

i.e. installed path `<rxDir>/jetty/defaults/bin/rxjetty.sh`. That file is **not** under `modules/perc-jetty/src/main/jetty/defaults/` (no `bin/` directory at all).

**History**: Product template lived at `system/Tools/jetty/defaults/bin/rxjetty.sh` and was **deleted** in the Jetty 12 / WebUI restructure (PR #662, commit `a82b983bce` — “Cleanup old system/Tools/jetty files”) without a corresponding restore under `modules/perc-jetty`.

**Impact**: Both systemd and `--initd` install paths call `installInitScriptAndDefaults` **before** branch selection. Missing template → hard fail: `Missing Jetty service template: …/rxjetty.sh`. Dual-ship **logic** is present; **packaged start helper template** is not.

**Not done in #1975**: restore/ship the template (product code change; residual issue).

**Suggested fix direction (residual)**: restore a Jetty-12-aligned `rxjetty.sh` under `modules/perc-jetty/src/main/jetty/defaults/bin/` (historical file is a starting point; re-validate against current `JETTY_*` env and upstream `jetty.sh`), plus a structural test that asserts the template exists and contains `${rxjetty_service}` / PID markers.

---

## Gap B — DTS unit template install path

**Symptom**: `DTSProductionService.sh` / `DTSStagingService.sh` resolve:

```text
$(dirname "$0")/dts-tomcat.service.in
```

Service scripts install to `Deployment/Server/`. `installDts.xml` places `dts-tomcat.service.in` at the **product surface root** (root `*` copy), not next to the service script.

**Impact**: systemd install path fails with `Missing systemd unit template` unless operators manually copy the `.in` file beside the service script (or run from a non-standard layout). init.d-only path does not need the `.in` file.

**Not done in #1975**: change `installDts.xml` and/or script resolution (residual issue).

**Suggested fix direction (residual)**: either (1) copy `dts-tomcat.service.in` (and optionally `README-systemd.md`) into `Deployment/Server/` in `installDts.xml` fresh+upgrade paths, or (2) resolve the template from `INSTALL_ROOT` / product surface. Prefer (1) to match script comments and README “same directory as catalina”.

---

## Explicit non-findings (still dual-shipped)

- **init.d not removed** from CMS or DTS installers.
- **Windows** `.bat` / Procrun / `tomcat11.exe` paths remain; Linux inventory does not delete them.
- Structural dual-ship tests remain and assert `--initd`, systemd detection, and no dual SysV enable on the systemd path.

---

## Residual work (out of this inventory PR)

|                                     Topic                                      |               Owner slice               |
|--------------------------------------------------------------------------------|-----------------------------------------|
| Restore/ship CMS `defaults/bin/rxjetty.sh` + structural test                   | Product fix residual under #962 / #1975 |
| Co-locate DTS `dts-tomcat.service.in` with service scripts (or fix resolution) | Product fix residual under #962 / #1975 |
| Live root systemd soak                                                         | Child C (out of scope for inventory)    |
| Product deprecation of SysV                                                    | Child D — **human ops review required** |

---

## How to re-validate

```text
# CMS source + tests
modules/perc-jetty/src/main/jetty/service/*
modules/perc-jetty/src/test/java/com/percussion/jetty/service/*

# CMS packaging
modules/perc-jetty/pom.xml  (copy src/main/jetty → assembly-directory)
modules/perc-jetty/src/main/assembly/jetty-assembly.xml
modules/perc-distribution-tree (unpack perc-jetty → jetty/)

# DTS source + installer
delivery-tier-distribution/src/main/rootFiles/DTS*Service.sh
delivery-tier-distribution/src/main/rootFiles/dts-tomcat.service.in
delivery-tier-distribution/src/main/rootFiles/rxconfig/Installer/installDts.xml

# Dual-ship greps
--initd / use_systemd_install / SysV boot registration skipped
```

