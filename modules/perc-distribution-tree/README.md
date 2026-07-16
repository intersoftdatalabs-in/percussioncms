# perc-distribution-tree

This module contains all Percussion CMS distribution assembly, installation, and upgrade related classes, configurations, and scripts. It is responsible for packaging Jetty, web applications, and configuration files into a complete, deployable Percussion CMS distribution.

## Overview

The `perc-distribution-tree` module is a critical component in the build pipeline that:

- **Assembles the Percussion Distribution**: Packages Jetty application server, WAR files, and configuration files
- **Configures Jetty**: Unpacks Jetty, configures modules, and sets up the base directory
- **Deploys Web Applications**: Stages Rhythmyx (main application), REST API, and SiteManage web applications
- **Manages Configuration**: Copies and initializes Percussion configuration files and database setup scripts
- **Enables Logging Integration**: Automatically configures Jetty's `logging-log4j2` module to ensure all System streams (stdout/stderr) are captured and routed through Apache Log4j2

## Architecture

### Build Process

The module uses Maven's `maven-antrun-plugin` to execute an ANT build script (`src/main/resources/installDistributionFiles.xml`) that orchestrates the distribution assembly. This ANT script:

1. **Unpacks Jetty**: Extracts the Jetty application server from the Maven artifact
2. **Sets Up Directory Structure**: Creates necessary directories (`webapps/`, `lib/`, `logs/`, etc.)
3. **Deploys Web Applications**: Copies and unpacks WAR files for each application
4. **Configures Databases**: Sets up development/production database connectors
5. **Enables Jetty Modules**: Configures the `logging-log4j2` module for log aggregation
6. **Fixes File Permissions**: Adjusts line endings and permissions for shell scripts

### Jetty Logging Integration

**Key Feature**: The distribution automatically enables Jetty's `logging-log4j2` module during assembly. This ensures:

- All console output (System.out/System.err) is captured by Log4j2
- Logging is centralized to a single `server.log` file
- Startup logs, application logs, and system output are unified
- Log rotation, formatting, and filtering follow the centralized Log4j2 configuration

The module enablement is performed by executing:

```bash
java -jar ../start.jar --add-modules=logging-log4j2
```

This happens automatically during the distribution build process. Refer to `src/main/resources/installDistributionFiles.xml` (lines ~705-710) for implementation details.

## Building

```bash
Use ./mvn-env.sh clean install (or mvn-env.bat clean install on Windows) so Maven runs with JDK 21.
```

To build only this module:

```bash
cd modules/perc-distribution-tree
../../mvn-env.sh clean install
```

## Key Files

- **`src/main/resources/installDistributionFiles.xml`**: ANT build script that assembles the distribution
- **`src/main/assembly/perc-assembly.xml`**: Maven Assembly plugin descriptor for distribution packaging
- **`pom.xml`**: Maven configuration with `maven-antrun-plugin` and `maven-assembly-plugin`

## Bundled JDBC Drivers

Every production build of this module ships a curated JDBC driver set into `jetty/base/lib/jdbc/` of the assembled distribution. The drivers are sourced from parent-POM-managed Maven coordinates and staged into `target/classes/distribution/_jdbc-stage/` by the `stage-jdbc-drivers` execution of `maven-dependency-plugin`, then copied into `jetty/base/lib/jdbc/` by the ANT script.

| Database | Driver coordinate | Source of truth |
|----------|------------------|-----------------|
| MariaDB / MySQL (default repository) | `org.mariadb.jdbc:mariadb-java-client` | root `pom.xml` `${mariadb.version}` |
| Derby (embedded/dev) | `org.apache.derby:derby`, `derbyclient`, `derbynet` | root `pom.xml` `${derby.version}` |
| MS SQL Server (modern) | `com.microsoft.sqlserver:mssql-jdbc` | root `pom.xml` `${mssql.version}` |
| MS SQL Server (legacy jTDS) | `net.sourceforge.jtds:jtds` | root `pom.xml` `${jtds.version}` |
| Oracle | `com.oracle.database.jdbc:ojdbc17` | root `pom.xml` `${ojdbc17.version}` |

JARs are placed in the install with their Maven-resolved filenames (e.g. `mariadb-java-client-3.5.7.jar`) so the bundled version is visible to integrators and any version drift is explicit.

### Extending the driver set

Integrators who need a driver that is not bundled (for example an enterprise Oracle driver) can simply drop a JDBC driver JAR into `jetty/base/lib/jdbc/` of the unpacked distribution. The install scripts (`rxconfig/Installer/install.xml`, `installServer.xml`, `installRepository.xml`) do not purge this folder.

## CLI installer: database targets for new installs

By default a **new** command-line install uses the embedded **Derby** repository. To target MySQL/MariaDB, SQL Server, or Oracle on a **new install only**, supply a repository properties file in the same format as `rxconfig/Installer/rxrepository.properties`:

```bash
java -Ddbprops=/path/to/rxrepository.mysql.properties -jar PercussionCMS.jar /path/to/install/root
# equivalent:
java -jar PercussionCMS.jar /path/to/install/root --dbprops=/path/to/rxrepository.mysql.properties
```

### Supported backends

| `DB_BACKEND` | Structured `db.type` | Notes |
|--------------|----------------------|-------|
| `DERBY` | `derby` | Default when no override is given |
| `MYSQL` | `mysql` | MySQL or MariaDB-compatible; sample uses MariaDB driver class |
| `MSSQL` | `sqlserver` | Microsoft SQL Server |
| `ORACLE` | `oracle` | Oracle thin |

### Sample files

Shipped under the distribution installer tree:

- `rxconfig/Installer/samples/rxrepository.mysql.properties`
- `rxconfig/Installer/samples/rxrepository.sqlserver.properties`
- `rxconfig/Installer/samples/rxrepository.oracle.properties`

Copy a sample, replace host/credentials, pre-create the empty database/schema, then pass the file with `-Ddbprops` / `--dbprops`.

### Input precedence (new install)

1. `dbprops` file (`-Ddbprops` / `--dbprops`)
2. Structured CLI `--db.*` (and env-style aliases)
3. Env file (`--db.config.env.file` / `DB_CONFIG_ENV_FILE`)
4. Process environment
5. Defaults (Derby)

### New install vs upgrade

- **New install**: database target input applies; installer writes effective `rxconfig/Installer/rxrepository.properties` and validates connectivity before schema setup.
- **Upgrade**: existing repository configuration is preserved. You do **not** need `-Ddbprops` to keep a non-Derby backend.

Feature design artifacts: `specs/006-installer-db-targets/` (contracts under `contracts/`).

## CLI installer: clean obsolete directories on upgrade

Long-lived installs may retain multi-GB **obsolete** directories (especially `PreInstall` from the old installer). On **upgrade only**, the preinstall step can remove a curated set **early** (before ANT upgrade work):

| Relative path | Notes |
|---------------|--------|
| `PreInstall` | Legacy preinstall/backup tree unused by 8.x |
| `_Percussion_Installation` (or `_Percussion_installation`) | Legacy install-metadata folder |
| `JBossServerXML_BAK` | Offered only when safe for the detected version (not when a 5.3-era migration still needs it without `AppServer`) |

### Flag (automation)

```bash
java -jar PercussionCMS.jar /path/to/existing/install --clean-install-dir
# or
java -jar PercussionCMS.jar /path/to/existing/install --clean-install-dir=true
```

- **Default: false** — non-interactive upgrades never delete these folders unless the flag is set.
- When true, candidates are deleted without a second confirmation (even if a TTY is present).

### Interactive upgrade

If a TTY is available, the flag is not set, and candidate folders exist, the installer prints the list and approximate freeable space and asks `[y/N]` (default **N**).

### Failure policy

If a folder cannot be deleted (permissions, locks), the installer **warns and continues** the upgrade.

Design artifacts: `specs/007-clean-install-dir/`.

The install/upgrade script's `<delete>` block in `install.xml` is pinned to the exact bundled-driver filenames shipped in this release (sourced from the parent POM's version properties: `${mariadb.version}`, `${derby.version}`, `${mssql.version}`, `${jtds.version}`, `${ojdbc17.version}`). When a driver version is bumped, THREE places MUST be updated in lockstep in the same commit:

1. **Parent POM** — bump the relevant version property (`${mariadb.version}`, `${derby.version}`, `${mssql.version}`, `${jtds.version}`, or `${ojdbc17.version}`).
2. **`BundledJdbcDrivers` (test source of truth)** — `modules/perc-distribution-tree/src/test/java/com/percussion/distribution/jdbc/BundledJdbcDrivers.java` exposes two sets:
   - `EXACT_FILENAMES` — the CURRENT bundled filenames for this release.
   - `PRIOR_FILENAMES` — the bundled filenames from the immediately preceding release.
   Move the old filename from `EXACT_FILENAMES` to `PRIOR_FILENAMES` (or add a new `PRIOR_FILENAMES` entry if it isn't already there).
3. **`install.xml`** — `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/install.xml`. The `<delete>` block in the `install_jdbc_drivers` target has a CURRENT `<include>` list and a PRIOR `<include>` list. Update both to mirror `BundledJdbcDrivers` exactly.

The PRIOR set exists so an upgrade from N-1 to N does not leave the prior-version JAR on the Jetty classpath (which can cause `LinkageError` / wrong-version-loaded issues). Old PRIOR entries can be removed by the release manager once they are confident no field upgrade from that era is still in flight. The pin list is enforced as exact filenames — no globs — by `scripts/check-no-glob-deletes.sh` (wired into the Maven `verify` phase), and `InstallXmlDeleteSetTest.deleteSetContainsAllBundledFilenames` asserts that `install.xml`'s delete set equals the union of the two Java constants, so any drift between the three places fails the build with a clear JUnit error rather than silently corrupting the installer.

The bundled driver set is verified by `scripts/verify-jdbc-drivers.sh`, which is wired into the Maven `verify` phase. CI fails the build if any expected driver is missing or is a stub.

## Related Documentation

For information about logging configuration and how the centralized logging works, refer to:

- `modules/perc-jetty/src/main/jetty/defaults/modules/perc-logging/resources/log4j2.xml` - Log4j2 configuration
- `modules/perc-jetty-logging/README.md` - Jetty logging module artifacts
- Main project documentation on logging and Jetty configuration
- `scripts/README.md` — verification utilities used by this module

