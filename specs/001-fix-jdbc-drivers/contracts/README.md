# Contracts: Fix Missing JDBC Drivers in Percussion Distribution Install

**Branch**: `001-fix-jdbc-drivers` | **Date**: 2026-07-10 | **Spec**: [spec.md](spec.md)

This feature has no public REST, SOAP, XML-application, or `.ppkg` contract surface (Constitution IV satisfied trivially — no breaking change). The "contracts" below describe the build-time surface that downstream modules, CI, and integrators rely on.

---

## Contract 1: Distribution Artifact Layout — `jetty/base/lib/jdbc/`

### Producer

`modules/perc-distribution-tree` (Maven build, phase `package`, via `maven-assembly-plugin` + `maven-antrun-plugin` running `installDistributionFiles.xml`).

### Consumer

- The Jetty base classloader at CMS startup (`jetty/base/lib/` is on the classpath per Jetty 12 conventions; `jdbc/` is a documented subdirectory used by the CMS installer scripts as the integrator's driver drop-point).
- The installer scripts `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/install.xml`, `installServer.xml`, `installRepository.xml` (consume but do not modify).
- Integrators (operators adding vendor JDBC drivers for non-default databases).

### Contract

```
<distribution-root>/jetty/base/lib/jdbc/
├── <driver-jar-1>.jar      # non-empty, valid Java archive
├── <driver-jar-2>.jar
└── ...
```

Requirements:

1. The directory MUST exist after a successful production build (FR-001).
2. It MUST contain at least one JDBC driver JAR in production builds.
3. Every JAR in the directory MUST have `size > 0` bytes.
4. Every JAR MUST be a valid Java archive (openable by `jar tf` / `unzip -t`).
5. The set MUST include a MariaDB/MySQL driver (the default CMS repository backend).

### Breaking-change posture

None. The directory is the same path; integrators who already drop their own drivers into this directory continue to work. Adding drivers is purely additive.

---

## Contract 2: Verification Script — `verify-jdbc-drivers.sh`

### Producer

`modules/perc-distribution-tree/scripts/verify-jdbc-drivers.sh` (new; documented in `modules/perc-distribution-tree/scripts/README.md` per module AGENTS rules).

### Consumer

- CI pipelines running module verification.
- Developers running manual pre-merge checks.
- The module's build (invoked via a Maven `exec-maven-plugin` or `maven-antrun-plugin` execution, TBD in tasks.md).

### Contract

**Invocation:**

```sh
./modules/perc-distribution-tree/scripts/verify-jdbc-drivers.sh \
    [--artifact <path-to-perc-distribution-tree.jar>] \
    [--workdir <scratch-dir>]
```

Defaults:
- `--artifact`: `modules/perc-distribution-tree/target/perc-distribution-tree.jar`
- `--workdir`: `${TMPDIR:-/tmp}/verify-jdbc-drivers-$$` (scratch space, cleaned on exit)

**Exit codes:**

| Code |                                                                 Meaning                                                                 |
|------|-----------------------------------------------------------------------------------------------------------------------------------------|
| 0    | All checks passed: artifact unpacked, `jetty/base/lib/jdbc/` exists, ≥ 1 JAR present, every JAR > 0 bytes, every JAR passes `unzip -t`. |
| 1    | Invocation error (bad args, artifact not found, etc.).                                                                                  |
| 2    | `jetty/base/lib/jdbc/` missing or empty.                                                                                                |
| 3    | One or more JARs are zero-byte.                                                                                                         |
| 4    | One or more JARs are not valid Java archives.                                                                                           |
| 5    | Artifact could not be unpacked.                                                                                                         |

**Output:**

- Human-readable summary on stdout (table of JARs with size + valid-jar status).
- On failure, a single-line error to stderr indicating the exit-code reason.

**Inputs:**

- Read-only access to the distribution artifact (and to `unzip`, `stat`, standard POSIX utilities — no privileged access).

**Breaking-change posture:**

N/A — new script.

---

## Contract 3: Maven Coordinate Set Bundled in Production

### Producer

`modules/perc-distribution-tree/pom.xml` (new `<dependency>` and `<execution>` entries in `maven-dependency-plugin`).

### Consumer

The `installDistributionFiles.xml` ANT script, which reads staged JARs from `${assembly-directory}/_jdbc-stage/`.

### Contract

|               Coordinate               |                                    Version source                                    |                                        Staged filename                                         |
|----------------------------------------|--------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------|
| `org.mariadb.jdbc:mariadb-java-client` | `${mariadb.version}` (`3.5.7`) — promote to root `<dependencyManagement>` in this PR | `mariadb-connector.jar` (renamed for clarity, matches legacy `mysql-connector.jar` convention) |
| `org.apache.derby:derby`               | root `<dependencyManagement>`                                                        | `derby.jar`                                                                                    |
| `org.apache.derby:derbyclient`         | root `<dependencyManagement>`                                                        | `derby-client.jar`                                                                             |
| `org.apache.derby:derbynet`            | root `<dependencyManagement>`                                                        | `derbynet.jar`                                                                                 |
| `com.microsoft.sqlserver:mssql-jdbc`   | `${mssql.version}`                                                                   | `mssql-connector.jar`                                                                          |
| `net.sourceforge.jtds:jtds`            | `${jtds.version}`                                                                    | `jtds.jar`                                                                                     |
| `com.oracle.database.jdbc:ojdbc17`     | `23.26.0.0.0` (already in root mgmt)                                                 | `ojdbc17.jar`                                                                                  |

Versions MUST be tracked in `<dependencyManagement>` (no naked version strings in this POM).

**Breaking-change posture:**

Adds new `<dependency>` entries. No existing coordinates change. No transitive downgrade risk because all versions match existing management.
