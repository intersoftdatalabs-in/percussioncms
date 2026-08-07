# delivery-tier-distribution

This module contains all the configuration files for DTS. For e.g.
* Log4j configurations
* script files for installing DTS as service
* DB configurations
* Spring Security Configurations
* Email Configurations
* DTS tomcat configurations
etc...

## Last-install user defaults

After a **successful** DTS install, non-secret settings are merged into the same file used by the CMS
installer:

```text
~/.intsof/percussion/last-install.properties
```

- Production installs use the `dts.prod.*` prefix
- Staging installs use the `dts.stage.*` prefix
- Includes `*.version` (version installed) and `*.install.directory`
- Passwords are never written

On subsequent runs, missing CLI values (including install path) are filled from the matching prefix.
See `InstallerUserSettings` and `com.intsof.common:utilities`.

## Runtime platform (Jakarta EE 11 / Tomcat 11)

|            Item             |                                         Value                                         |
|-----------------------------|---------------------------------------------------------------------------------------|
| Tomcat version property     | `${tomcat.version}` (currently **11.0.x**) in `delivery-tier-suite/pom.xml`           |
| Cargo container             | **`tomcat11x`** via `cargo-maven3-plugin`                                             |
| Conf overlay source         | `src/main/tomcat11/`                                                                  |
| Windows Procrun             | `rootFiles/tomcat11.exe` + `tomcat11w.exe` (installed by `installDts.xml`)            |
| Windows service scripts     | `DTSProductionService.bat` / `DTSStagingService.bat` → **`tomcat11.exe`**             |
| Embedded DB (new install)   | **H2** under `Deployment/Server/h2data/` (`-Dperc.h2.data.home`)                      |
| Shared JDBC on `common/lib` | H2 + external drivers (jTDS, MariaDB, MSSQL, Oracle, HikariCP) — **not** Apache Derby |

### Windows service logging (issue #938)

When DTS runs as a **Windows service**, Procrun redirects JVM stdout/stderr to:

```text
Deployment/Server/logs/catalina.log
```

This matches Linux `CATALINA_OUT=${CATALINA_HOME}/logs/catalina.log` in
`DTSProductionService.sh` / `DTSStagingService.sh`. Installers also set the Log4j
JUL bridge and `log4j.configurationFile` to `log4j2/conf/log4j2-tomcat.xml`
(service mode does not run `setenv.bat`). Operator details:
`src/main/rootFiles/README-windows-service.md`.

**HTTPS (Tomcat 11):** `conf/server.xml` uses nested `<SSLHostConfig>` / `<Certificate>` (legacy Connector `keystoreFile` attributes no longer create a default SSL host config). Keystore defaults: `conf/.keystore`, type **PKCS12**, password from `conf/perc/perc-catalina.properties`.

**Derby:** Not packaged into the shipping Tomcat `common/lib`. Upgrade-time Derby→H2 migration still runs offline from `installDts.xml` (`PSMigrateDtsEmbeddedRepository` / `PSUpgradeDerby` via perc-ant). Do not re-add `org.apache.derby` cargo container dependencies without an explicit migration-window reason.

Residual checklist: `docs/ai-generated/tasks/667-jakarta-ee11-residual-checklist/README.md` (issue #667).

## Java baseline

✅ Builds and runs on **JDK 21** (product baseline for 8.2). Historical notes below refer to earlier refactors.

### Refactored Classes:

- `MainDTSPreInstall.java` - Java 17 features including var, lambda expressions, improved string handling
- `AntJobFailedException.java` - Updated formatting and copyright

### Key Improvements:

- Applied modern Java 17 var keyword for better type inference
- Used lambda expressions for cleaner, more readable code
- Improved string comparisons and null checks
- Enhanced code formatting and consistency
- Removed unused dependencies (Apache Axis StringUtils)

## Build

```bash
./mvnw -pl deliverytiersuite/delivery-tier-suite/delivery-tier-distribution -am clean install
# Windows: mvnw.cmd -pl deliverytiersuite/delivery-tier-suite/delivery-tier-distribution -am clean install

## Interactive installer mode (issue #1513)

When a **console/TTY is available** and you do **not** pass `--silent` / `--no-tty`, the DTS preinstall walks through:

1. Installation directory (prompted if the path argument is omitted)
2. System Java 21+ home (discovery / multi-candidate menu; or `-Dperc.java.home=...`)
3. Server type: Production vs Staging
4. Database backend (menu: H2, SQL Server/Express, MySQL/MariaDB, PostgreSQL, or env-style config file)
5. Optional connection test for external backends (best-effort)
6. Summary (no secrets) and confirm

Silent/automation installs are unchanged:

```bash
# Interactive (console)
java -jar PercussionDTS.jar

# Silent / CI
java -jar PercussionDTS.jar /path/to/install/root --silent --db.type=h2
```

## Java home resolution (GH-991 / issue #1340)

Operators **do not** need to place a JRE under `<InstallDir>/JRE`. At DTS
install time the preinstall selects a system JDK/JRE (or honors
`-Dperc.java.home=...`) and writes `<InstallDir>/java.properties`. Console
and service scripts **must** resolve Java through that contract (see
`specs/991-system-java-home/contracts/`):

1. `<InstallDir>/java.properties` (`JAVA_HOME`, optionally `JAVA`) — **primary**
2. Process `JAVA_HOME` environment variable
3. Optional legacy `<InstallDir>/JRE` then `<InstallDir>/JRE64` (if an operator still has one)
4. `java` discoverable on `PATH`
5. **Hard fail** with major **21+** and sources tried — never soft-fail into an unvalidated JRE path

Resolve helpers (`resolve-java-home.sh` / `.bat`) ship in
`src/main/rootFiles/` next to `TomcatStartup.*` / `TomcatShutdown.*`.
`installDts.xml` places those files at the **product surface root** (and under
`Staging/` for staging). Service installers live under
`Deployment/Server/` and locate the surface root **two levels up**. Console
scripts use the **same directory** as the script (`SCRIPT_DIR`) as the surface
root. Both consoles and both service installers **hard-fail** if resolve fails
before writing Procrun `--JavaHome` or `/etc/default/<service>`. See
`specs/991-system-java-home/quickstart.md` for re-point steps (edit
`java.properties`, restart — no JRE folder required).

## Log rotation samples (GH-2348)

Standalone DTS ships **opt-in** Linux `logrotate` samples under `logrotate/` at the install root (`percussion-dts` + README). They are **not** copied into `/etc/logrotate.d` automatically.

- Covers `Deployment/Server/logs` (`*.log`, `*.out` including `catalina.out`)
- Defaults: daily, rotate 14, compress, **copytruncate**
- Dry-run: `logrotate -d /etc/logrotate.d/percussion-dts` after path substitution
- Co-located CMS installs also keep the full CMS+DTS samples under `rxconfig/Installer/logrotate/`
- Windows: schedule `perc-doctor clean-logs --older-than 14d` (see CMS logrotate README)

## Linux services (systemd) — GH-962 / dual-ship (GH-1978)

Production and Staging installers under `src/main/rootFiles/` prefer **native systemd**
when available:

|          Script           |       Default unit        |
|---------------------------|---------------------------|
| `DTSProductionService.sh` | `PercussionProductionDTS` |
| `DTSStagingService.sh`    | `PercussionStagingDTS`    |

Shared unit template: `dts-tomcat.service.in` (`Type=forking`, `TimeoutStartSec=1800`, journal).  
`installDts.xml` co-locates the template and `README-systemd.md` under `Deployment/Server/`
next to `DTSProductionService.sh` / `DTSStagingService.sh` (scripts resolve
`dirname $0`/dts-tomcat.service.in; GH-1984). Ops notes: `README-systemd.md` (dry-run /
non-root limitations, migration uninstall→install, init.d retained as start helper +
fallback). Flags: `--systemd`, `--initd`. Install requires root (no product `--dry-run`).
Windows `.bat` unchanged.

**Dual-ship policy:** keep init.d until a live Linux soak signs off (do not remove
init.d / #1976 without ops review). `installDts.xml` places the role-specific
installer **and** `dts-tomcat.service.in` under `Deployment/Server/` (scripts are
intentionally excluded from the install-root bulk `*` copy so only Production or
Staging is selected). Packaging guard: `DtsLinuxServiceDualShipPackagingTest`.

```bash
cd Deployment/Server
sudo ./DTSProductionService.sh install
# flags: --systemd (require) | --initd (force SysV)
sudo systemctl start PercussionProductionDTS
journalctl -u PercussionProductionDTS -n 50 --no-pager
```

## Installer jar (`java -jar`)

The package artifact `target/delivery-tier-distribution.jar` is launched with:

```bash
java -jar delivery-tier-distribution.jar <install-or-upgrade-folder>
```

`MainDTSPreInstall` validates Zip entry paths with
`com.percussion.security.validation.PathValidation` (CWE-22 / ZipSlip). That class
lives in `perc-security-utils` and is **not** on a thin jar classpath when using
`java -jar`. Likewise, `InstallerUserSettings` needs
`com.intsof.common.utilities.UserConfiguration` from `com.intsof.common:utilities`
(compile dependency only unless staged into the jar).

**GH-1180 / GH-1825:** `maven-dependency-plugin` unpacks required classes into
`${project.build.outputDirectory}` so `maven-jar-plugin` ships them in the fat
installer jar:

|               Artifact               |           What is included            |             Execution id             |
|--------------------------------------|---------------------------------------|--------------------------------------|
| `com.percussion:perc-security-utils` | `PathValidation` + nested types only  | `unpack-pathvalidation`              |
| `org.apache.logging.log4j:log4j-api` | Required by `PathValidation`'s logger | `unpack-pathvalidation`              |
| `com.intsof.common:utilities`        | Full jar (~27 KB; no 3rd-party deps)  | `unpack-userconfiguration` (GH-1825) |

This is intentionally **not** a full `jar-with-dependencies` (unlike
`perc-distribution-tree`), so wars/Tomcat/Spring stay out of the installer jar.

Verify phase fails the build if `PathValidation.class`, `LogManager.class`,
`UserConfiguration.class`, or `AppConfigurationFolder.class` is missing from the
packaged jar (`verify-pathvalidation-shaded` antrun). Unit tests under
`DtsInstallerJarContains*` assert the same jar listing invariants.

