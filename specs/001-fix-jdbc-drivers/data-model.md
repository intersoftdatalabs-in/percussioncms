# Data Model: Fix Missing JDBC Drivers in Percussion Distribution Install

**Branch**: `001-fix-jdbc-drivers` | **Date**: 2026-07-10 | **Spec**: [spec.md](spec.md)

This feature is build/packaging work. There is no runtime data model beyond file artifacts on disk. The "entities" below describe the file-level shape of the install artifact.

## Entities

### Entity: Distribution Artifact

- **What it represents**: The packaged archive produced by `mvn package` of `modules/perc-distribution-tree`. Concretely `modules/perc-distribution-tree/target/perc-distribution-tree.jar` and any final distribution tarball/zip that wraps it.
- **Key attributes**:
  - `internal_layout`: fixed tree, root contains `jetty/`, `rxconfig/`, `Installer/`, `wars/`, `Extensions/`, etc.
  - `jdbc_dir_path`: `jetty/base/lib/jdbc/` — must exist and contain ≥ 1 non-empty JAR.
  - `mode`: production (default) vs `DEVELOPMENT=true` legacy override.
- **Relationships**:
  - Depends on `JDBC Driver JAR`(s) as inputs to assembly.
  - Consumed by the installer (`modules/perc-ant`, `modules/perc-rxapps`) and ultimately by the running Jetty instance.
- **Validation rules (from spec)**:
  - `jetty/base/lib/jdbc/` MUST exist in production builds (FR-001).
  - Every file in that directory MUST have size > 0 and be a valid Java archive (FR-002 / SC-002).
  - At least one JAR MUST be sufficient to bootstrap the default (MariaDB/MySQL) repository (FR-002 / SC-003).
- **State transitions**: N/A (build-time artifact; immutable once produced).

### Entity: JDBC Driver JAR

- **What it represents**: A Java archive containing a JDBC driver implementation, shipped to integrators as part of the install.
- **Key attributes**:
  - `coordinate`: Maven groupId/artifactId, e.g. `org.mariadb.jdbc:mariadb-java-client`.
  - `version`: resolved from parent-POM management.
  - `dbms`: human-readable name (`mariadb`, `derby`, `mssql`, `jtds`, `oracle`).
  - `file_name`: actual filename on disk, e.g. `mariadb-java-client-3.5.7.jar` or renamed to a stable `mariadb-connector.jar` (TBD in tasks; rename recommended for integrator clarity, see open question).
  - `size_bytes`: > 0.
  - `is_valid_jar`: verifiable via `unzip -t` or `jar tf`.
- **Relationships**:
  - Belongs to the `Distribution Artifact`'s `jdbc_dir_path`.
  - Sourced from a curated Maven dependency (parent-POM or module-POM managed).
- **Validation rules (from spec)**:
  - Must be non-empty and a valid JAR (SC-002).
  - Source coordinate must resolve at build time; failure must be loud and actionable (FR-003 / SC-004).
- **State transitions**: N/A (immutable artifact once placed).

### Entity: Installer / Build Mode

- **What it represents**: The mode in which `modules/perc-distribution-tree` was assembled. Currently the only mode flag is the `DEVELOPMENT` environment variable read in `installDistributionFiles.xml:8-10`.
- **Key attributes**:
  - `name`: `production` (default) | `development` (legacy, set via `DEVELOPMENT=true`).
  - `behaviour`:
    - `production`: copies curated JDBC driver set from Maven coordinates into `jetty/base/lib/jdbc/` (NEW behavior).
    - `development`: in addition to production behavior, copies the legacy development MySQL connector from the historical path if it still exists (PRESERVED behavior).
- **Relationships**:
  - Selects which `JDBC Driver JAR`(s) are placed and from which source.
- **Validation rules (from spec)**:
  - Production mode must include drivers (FR-001).
  - `DEVELOPMENT=true` legacy behavior must be preserved (FR-004).
- **State transitions**: N/A (chosen at build invocation).

## Storage / Lifecycle Notes

- No CMS database tables or schemas are touched by this feature.
- No runtime XML applications, REST endpoints, or SOAP contracts change.
- The change is entirely in the build pipeline: `modules/perc-distribution-tree/pom.xml`, `modules/perc-distribution-tree/src/main/resources/installDistributionFiles.xml`, and a new `modules/perc-distribution-tree/scripts/verify-jdbc-drivers.sh`.
- Lifecycle of the produced driver JARs on a deployed install is unchanged: they sit in `jetty/base/lib/jdbc/` and are picked up by Jetty's classloader at startup; the existing installer (`install.xml` / `installServer.xml`) never overwrites them post-install.

## Schema Impact

None. Constitution IV (Contract & Integration Integrity) is satisfied without a schema migration because no public contract, no package `.ppkg` shape, and no DB schema changes.

## Open Question (deferred to tasks.md)

Resolved — see `research.md` §"Decision 6 (resolved): Driver filename stability — rename to stable names." Final answer: rename staged JARs to stable integrator-friendly names (`mariadb-connector.jar`, `derby.jar`, `derby-client.jar`, `derbynet.jar`, `mssql-connector.jar`, `jtds.jar`, `ojdbc17.jar`) when copying from `_jdbc-stage/` into `jetty/base/lib/jdbc/`. This is reflected in `contracts/README.md` Contract 3 and `tasks.md` T024.
