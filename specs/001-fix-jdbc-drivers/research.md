# Research: Fix Missing JDBC Drivers in Percussion Distribution Install

**Branch**: `001-fix-jdbc-drivers` | **Date**: 2026-07-10 | **Spec**: [spec.md](spec.md)

## Problem Statement

When `modules/perc-distribution-tree` is built for production (no `DEVELOPMENT=true` env override), the assembled install artifact contains an empty `jetty/base/lib/jdbc/` directory. The CMS cannot bootstrap its repository connection because no JDBC driver JARs are present. The legacy `DEVELOPMENT=true` codepath references a source path (`${basedir}../../../system/Tools/mysql/mysql-connector-java-8.0.18.jar`) that no longer exists in the current tree.

## Investigation Findings (Evidence-Based)

### 1. Where the gap is — `modules/perc-distribution-tree/src/main/resources/installDistributionFiles.xml`

Lines 695–704 wrap the MySQL driver copy in an `<if><equals arg1="${DEVELOPMENT}" arg2="true"/>` guard. Result: only `DEVELOPMENT=true` builds populate `jetty/base/lib/jdbc/`. The source path `${basedir}../../../system/Tools/mysql/mysql-connector-java-8.0.18.jar` does not exist (`system/Tools/mysql/` is absent in the current tree; only `deliverytiersuite/.../p13n-ds/resource/jdbc/mysql-connector-java-5.1.12-bin.jar` exists, and that's an unrelated 12-year-old artifact).

### 2. What other modules already do for JDBC drivers

- **`modules/perc-jetty-jars/pom.xml`** (the existing JDBC-driver packaging module) already declares: `derby`, `derbyclient`, `derbynet`, `jtds` (SQL Server legacy), `mssql-jdbc` (SQL Server modern), `ojdbc17` (Oracle), and `HikariCP`. However:
  - Most are `scope=provided`, so only `ojdbc17` (no scope → compile) is actually packaged into the `jar-with-dependencies` assembly. The other drivers are not included.
  - **MariaDB / MySQL is missing entirely** from this module's dependency list, despite being the default repository.
  - The artifact is currently consumed only by `modules/perc-jetty` (`scope=provided`) and `WebUI` (`scope=runtime`) — `perc-distribution-tree` does not depend on it at all.
- **`deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/pom.xml`** (the DTS analog) uses `org.mariadb.jdbc:mariadb-java-client:${mariadb.version}` (`3.5.7`) bundled alongside Oracle, MSSQL, and jtds into the Tomcat lib dir. This is the proven pattern in this repo for "ship a JDBC driver with the server."
- **`pom.xml` (root)** already manages `ojdbc17`, `jtds`, `mssql-jdbc`, and `sqlite-jdbc` in `<dependencyManagement>` (lines 1475–1489 and 1245). MariaDB is not in the root management but is managed at `deliverytiersuite/delivery-tier-suite/pom.xml:309-311`.

### 3. Constitution constraints that govern the decision

- **I. Module-First Boundaries**: Driver packaging belongs in `modules/perc-jetty-jars` (its stated purpose) plus the assembly step in `modules/perc-distribution-tree`. Not new shared modules.
- **II. Evidence Over Invention**: Use only drivers already coordinated in this repo (mariadb-java-client, ojdbc17, mssql-jdbc, jtds, derby, derbyclient, derbynet). No invented sources.
- **III. Test Discipline**: Must add tests for assembly logic and verification.
- **V. Safe Modernization**: Don't touch `DEVELOPMENT=true` behavior; layer on top.
- **VI. Security by Default**: JDBC drivers are not a security-sensitive surface here; no threat-model changes needed beyond ensuring versions are tracked in dependency management.
- **VII. Build Hygiene**: Use existing parent-POM management; Spotless where applicable.

## Decisions

### Decision 1: Source of bundled JDBC drivers — reuse curated Maven coordinates, not filesystem paths

**Decision**: Replace the broken `system/Tools/mysql/...` filesystem copy in `installDistributionFiles.xml` with `maven-dependency-plugin:copy` (or `unpack-dependencies` if `jar-with-dependencies` semantics are wanted) inside `modules/perc-distribution-tree`, sourcing drivers from the parent-POM-managed coordinates.

**Rationale**:
- Aligns with how `deliverytiersuite/.../delivery-tier-distribution/pom.xml` already does this for DTS Tomcat.
- Tracks versions via the parent POM `<dependencyManagement>` rather than checking JARs into the repo.
- Works on every platform without `..`-relative filesystem path hacks.

**Alternatives considered**:
- *Fix the legacy `system/Tools/mysql/` filesystem path.* Rejected: the path was outside `modules/perc-distribution-tree` (in `system/Tools/`), so the change would cross module boundaries for what is fundamentally a packaging concern. Filesystem copies also bypass version management.
- *Create a new module `modules/perc-jdbc-drivers`.* Rejected per Constitution I — `modules/perc-jetty-jars` already exists for this purpose; no need to add another.
- *Hard-check JARs into `modules/perc-distribution-tree/src/main/resources/`.* Rejected per Constitution II — invents a new repo-side artifact location that diverges from the established DTS pattern.

### Decision 2: Set of drivers to ship in production builds

**Decision**: Ship the canonical JDBC driver set in `jetty/base/lib/jdbc/` for every production build of `modules/perc-distribution-tree`:

| Driver | Coordinate | Source of truth |
|--------|-----------|-----------------|
| MariaDB / MySQL (default repository) | `org.mariadb.jdbc:mariadb-java-client` | `deliverytiersuite/delivery-tier-suite/pom.xml` `${mariadb.version}` (`3.5.7`) |
| Derby (embedded/dev) | `org.apache.derby:derby`, `derbyclient`, `derbynet` | `modules/perc-jetty-jars/pom.xml` (move to root management in this PR) |
| MS SQL Server (modern) | `com.microsoft.sqlserver:mssql-jdbc` | root `pom.xml` `${mssql.version}` |
| MS SQL Server (legacy jTDS) | `net.sourceforge.jtds:jtds` | root `pom.xml` `${jtds.version}` |
| Oracle | `com.oracle.database.jdbc:ojdbc17` | root `pom.xml` (`23.26.0.0.0`) |

**Rationale**: This matches the set `deliverytiersuite/delivery-tier-distribution` already ships for DTS and the set declared (but never properly packaged) in `modules/perc-jetty-jars`. MariaDB is added because it is the CMS default repository. The set covers every database backend the CMS installer scripts (`install.xml`, `installServer.xml`, `installRepository.xml`) accept.

**Alternatives considered**:
- *Ship only MariaDB.* Rejected: integrators using MSSQL/Oracle would still need to copy drivers manually, regressing the contract that `jetty/base/lib/jdbc/` is the documented driver drop-point for all supported DBs.
- *Ship all five as `jar-with-dependencies` from `modules/perc-jetty-jars`.* Rejected (separate from Decision 1): fixing `perc-jetty-jars` scope mix is a wider refactor; the immediate fix is to have `perc-distribution-tree` copy the curated drivers directly, leaving `perc-jetty-jars` cleanup as a follow-up if needed.

### Decision 3: How to surface the failure when a driver coordinate can't be resolved

**Decision**: In `installDistributionFiles.xml`, replace the existing "best-effort copy" with a `<copy>` from `${assembly-directory}/_jdbc-stage/` populated by `maven-dependency-plugin:copy-dependencies` with `failOnAnyMissingDependency=true`. The ANT copy will fail with a clear path/filename if any staged JAR is missing.

**Rationale**: Meets FR-003 ("fail loudly with an actionable error") without requiring a new verification tool. Existing ANT `<copy>` fails by default when the source file is absent; combined with `failOnAnyMissingDependency=true` in the Maven plugin, we get the dual failure mode the spec requires.

**Alternatives considered**:
- *Use `<available>` checks before each copy and emit a custom error message.* Rejected: extra logic; the existing failure semantics are already actionable enough.
- *Write a JUnit test that scans the staged dir after the build.* Deferred to FR-007 / automated verification step.

### Decision 4: Preserve the `DEVELOPMENT=true` legacy path

**Decision**: Keep the existing `DEVELOPMENT=true` block in `installDistributionFiles.xml` but rename it to clearly indicate it is a *legacy override that adds an extra development driver* (not the production driver set). The production driver set runs unconditionally for every build.

**Rationale**: Meets FR-004 (backward compatibility). No semantic break for anyone relying on `DEVELOPMENT=true`.

**Alternatives considered**:
- *Remove the `DEVELOPMENT=true` block.* Rejected: spec FR-004 explicitly requires preserving it.

### Decision 5: Automated verification — script + JUnit

**Decision**: Add a small shell script `modules/perc-distribution-tree/scripts/verify-jdbc-drivers.sh` that:
1. Unpacks the just-built distribution archive from `${basedir}/target/perc-distribution-tree.jar` (or the relevant artifact).
2. Lists `jetty/base/lib/jdbc/` and asserts non-empty, all files > 0 bytes, all files valid `jar` archives (via `unzip -t`).
3. Exits non-zero with a clear message on any failure.

Per module AGENTS convention, scripts live under `scripts/` with a README. The script is also wired into the module's build via a new test execution that runs after `package`.

**Rationale**: This is what FR-007 and SC-005 require. A shell script is the cheapest reliable mechanism that runs on every CI platform the project supports, and it can be invoked manually as well.

**Alternatives considered**:
- *JUnit-only verification.* Rejected: cannot reliably depend on the distribution artifact existing from inside the same module's tests without circular reasoning, and a shell script gives integrators and CI operators a clear entry point.
- *Custom Maven plugin.* Rejected per Constitution V (YAGNI — no new framework).

## Resolved Items (NEEDS CLARIFICATION → Decision)

| # | Original Need | Resolution |
|---|---------------|------------|
| 1 | Where does the bundled driver physically come from in production builds? | Maven coordinates managed in parent POM / DTS POM, copied via `maven-dependency-plugin:copy` into a staging dir, then ANT `<copy>` into `jetty/base/lib/jdbc/`. |
| 2 | Which drivers must ship by default? | MariaDB (default), Derby, MSSQL (modern + jTDS), Oracle. |
| 3 | How do we ensure FR-003 (loud failure)? | `failOnAnyMissingDependency=true` on the copy step + ANT `<copy>` failing on missing source. |
| 4 | What about `DEVELOPMENT=true`? | Preserved as-is; legacy path adds extra dev driver on top of the unconditional production set. |

## Open / Deferred Items (none blocking)

None. All originally-ambiguous items resolved.

### Decision 6 (resolved): Driver filename stability — rename to stable names

**Decision**: Rename staged JARs from their Maven-resolved filenames (e.g. `mariadb-java-client-3.5.7.jar`) to stable, integrator-friendly names (`mariadb-connector.jar`, `derby.jar`, `mssql-connector.jar`, `jtds.jar`, `ojdbc17.jar`) before placing them in `jetty/base/lib/jdbc/`. The rename happens in the same ANT copy step that moves files from `_jdbc-stage/` into `jetty/base/lib/jdbc/` (one `<copy>` per coordinate with explicit `tofile=`, or a small `<move>`/`<rename>` step between staging and install).

**Rationale**:
- Matches the legacy `mysql-connector.jar` naming convention already used by the `DEVELOPMENT=true` codepath, so any existing integrator scripts / docs that reference driver filenames by stable name keep working.
- Removes the implicit version coupling from the install path: future driver upgrades don't change the filename integrators see (the version is tracked in `<dependencyManagement>` instead).
- Keeps `contracts/README.md` Contract 3 and `tasks.md` T024 in agreement.

**Alternatives considered**:
- *Keep Maven filenames.* Rejected: every driver upgrade would silently change the filename in the install tree, which is a hidden behavioral change for integrators.
- *Use only the versionless coordinate name without a friendlier handle.* Rejected: `mariadb-java-client.jar` is technically correct but less obvious than `mariadb-connector.jar` for an integrator scanning the directory.

## References

- `modules/perc-distribution-tree/src/main/resources/installDistributionFiles.xml:695-704` — current JDBC copy block
- `modules/perc-distribution-tree/pom.xml` — module build configuration
- `modules/perc-jetty-jars/pom.xml` — existing JDBC driver packaging module
- `modules/perc-jetty-jars/src/assembly/jar-with-dependencies.xml` — its assembly descriptor
- `deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/pom.xml:355-371, 568-572` — DTS pattern reference
- `deliverytiersuite/delivery-tier-suite/pom.xml:45, 309-311` — `${mariadb.version}` and MariaDB management
- `pom.xml:1475-1489, 1245-1247` — root driver management (Oracle, jtds, mssql, sqlite)
- `.specify/memory/constitution.md` — governing principles
- `modules/perc-distribution-tree/AGENTS.md` — module-specific agent guidelines