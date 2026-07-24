# delivery-tier-distribution

This module contains all the configuration files for DTS. For e.g.
* Log4j configurations
* script files for installing DTS as service
* DB configurations
* Spring Security Configurations
* Email Configurations
* DTS tomcat configurations
etc...

## Java 17 Refactoring Status

✅ **Fully refactored to Java 17** (August 4, 2025)

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
./mvn-env.sh -pl deliverytiersuite/delivery-tier-suite/delivery-tier-distribution -am clean install
# Windows: mvn-env.bat -pl deliverytiersuite/delivery-tier-suite/delivery-tier-distribution -am clean install

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
## Linux services (systemd) — GH-962

Production and Staging installers under `src/main/rootFiles/` prefer **native systemd**
when available:

| Script | Default unit |
|--------|----------------|
| `DTSProductionService.sh` | `PercussionProductionDTS` |
| `DTSStagingService.sh` | `PercussionStagingDTS` |

Shared unit template: `dts-tomcat.service.in` (`Type=forking`, `TimeoutStartSec=1800`, journal).  
Ops notes: `README-systemd.md`. Flags: `--systemd`, `--initd`. Windows `.bat` unchanged.

```bash
sudo ./DTSProductionService.sh install
sudo systemctl start PercussionProductionDTS
journalctl -u PercussionProductionDTS -n 50 --no-pager
```

## Linux services (systemd) — GH-962

Production and Staging installers under `src/main/rootFiles/` prefer **native systemd**
when available:

| Script | Default unit |
|--------|----------------|
| `DTSProductionService.sh` | `PercussionProductionDTS` |
| `DTSStagingService.sh` | `PercussionStagingDTS` |

Shared unit template: `dts-tomcat.service.in` (`Type=forking`, `TimeoutStartSec=1800`, journal).  
Ops notes: `README-systemd.md`. Flags: `--systemd`, `--initd`. Windows `.bat` unchanged.

```bash
sudo ./DTSProductionService.sh install
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
`java -jar`.

**GH-1180:** package runs a **minimal** `maven-shade-plugin` step that merges only:

| Artifact | What is included |
|----------|------------------|
| `com.percussion:perc-security-utils` | `PathValidation` + nested types only |
| `org.apache.logging.log4j:log4j-api` | Required by `PathValidation`'s logger |

This is intentionally **not** a full `jar-with-dependencies` (unlike
`perc-distribution-tree`), so wars/Tomcat/Spring stay out of the installer jar.

Verify phase fails the build if `PathValidation.class` or `LogManager.class` is
missing from the packaged jar (`verify-pathvalidation-shaded` antrun).

