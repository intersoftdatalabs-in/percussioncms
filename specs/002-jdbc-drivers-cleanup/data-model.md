# Data Model: JDBC Drivers Packaging Cleanup

**Phase**: 1 — Design & Contracts
**Date**: 2026-07-11
**Spec**: [spec.md](./spec.md)
**Branch**: `002-jdbc-drivers-cleanup`

## Purpose

This feature is build-time / install-script only — it has no runtime data model and no persistent entities. This document enumerates the *build-time* and *install-time* entities (filenames, directories, and configuration values) whose contracts are affected, and pins down the exact values that the implementation must use. It is the source of truth for the unit test fixtures and for the static shell assertion.

## Entities

### E1. Curated JDBC Driver Set

The single source of truth is the `provided`-scope driver dependencies declared in `modules/perc-distribution-tree/pom.xml` (lines 147–181). These are pulled by the `stage-jdbc-drivers` execution (`pom.xml:496-509`) and copied into `jetty/base/lib/jdbc/` by `installDistributionFiles.xml:707-717`.

| # | groupId | artifactId | version (parent POM) | resolved filename | ships today? |
|---|---------|------------|-----------------------|-------------------|--------------|
| 1 | `org.mariadb.jdbc` | `mariadb-java-client` | `3.5.7` (`pom.xml:211`) | `mariadb-java-client-3.5.7.jar` | yes |
| 2 | `org.apache.derby` | `derby` | `10.17.1.0` (`pom.xml:115`) | `derby-10.17.1.0.jar` | yes |
| 3 | `org.apache.derby` | `derbyclient` | `10.17.1.0` | `derbyclient-10.17.1.0.jar` | yes |
| 4 | `org.apache.derby` | `derbynet` | `10.17.1.0` | `derbynet-10.17.1.0.jar` | yes |
| 5 | `org.apache.derby` | `derbyshared` | `10.17.1.0` | `derbyshared-10.17.1.0.jar` | yes |
| 6 | `org.apache.derby` | `derbytools` | `10.17.1.0` | `derbytools-10.17.1.0.jar` | yes |
| 7 | `com.microsoft.sqlserver` | `mssql-jdbc` | `13.3.1.jre11-preview` (`pom.xml:210`) | `mssql-jdbc-13.3.1.jre11-preview.jar` | yes |
| 8 | `net.sourceforge.jtds` | `jtds` | `1.3.1` (`pom.xml:168`) | `jtds-1.3.1.jar` | yes |
| 9 | `com.oracle.database.jdbc` | `ojdbc17` | `23.26.0.0.0` (`pom.xml:212`) | `ojdbc17-23.26.0.0.0.jar` | yes |

Validation rules:
- All 9 entries MUST be present in the assembled `perc-distribution-tree.jar` under `jetty/base/lib/jdbc/`.
- No other JAR (no `provided`-scope non-JDBC dep, no transitive dep) may appear under `jetty/base/lib/jdbc/`.
- The 6 `derby` JARs share the same version, but they are distinct artifacts and have distinct filenames.
- `derbyshared` and `derbytools` are required at runtime by the embedded `derby` engine since
  Derby 10.15 split the engine across multiple JARs — omitting them surfaces as
  `ClassNotFoundException: org.apache.derby.shared.common.error.StandardException`. The
  glob pattern `derby-*.jar` does NOT match them (the artifactId has no hyphen after
  `derby`), so each must appear as a separate `<include>` entry in
  `installDistributionFiles.xml:712-726` and in the staging fixture.
- See `BundledJdbcDrivers.STAGING_GLOBS` and the `GLOB_TO_ARTIFACT_ID` table in
  `modules/perc-distribution-tree/src/test/java/.../BundledJdbcDrivers.java` for the
  programmatic mirror of this table.

### E2. Install Script Delete Set (after change)

The exact-filename list pinned directly in `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/install.xml` (replacing the current glob-based `<delete>` at lines 174-188).

| # | filename | reason |
|---|----------|--------|
| 1 | `mariadb-java-client-3.5.7.jar` | bundled curated driver |
| 2 | `derby-10.17.1.0.jar` | bundled curated driver |
| 3 | `derbyclient-10.17.1.0.jar` | bundled curated driver |
| 4 | `derbynet-10.17.1.0.jar` | bundled curated driver |
| 5 | `mssql-jdbc-13.3.1.jre11-preview.jar` | bundled curated driver |
| 6 | `jtds-1.3.1.jar` | bundled curated driver |
| 7 | `ojdbc17-23.26.0.0.0.jar` | bundled curated driver |

Validation rules:
- The delete set MUST contain exactly these 7 filenames.
- The delete set MUST contain no glob/wildcard characters (`*`, `?`).
- The delete set MUST NOT contain `mysql-connector-java-*.jar`, `mysql-connector.jar`, `derbyshared-*.jar`, `derbytools-*.jar` (removed — they purge integrator drivers and do not match any shipped file).
- Any JAR in `jetty/base/lib/jdbc/` whose filename is NOT in this set MUST be preserved (this is the integrator-supplied driver preservation guarantee).

Update procedure (manual, on each driver version bump):
1. Bump the relevant `${...version}` in the parent `pom.xml`.
2. Update the 7 lines in `install.xml` to the new `<artifactId>-<newVersion>.jar` filenames.
3. The same globs used by the Maven `verify` phase (`pom.xml:737`) are version-resilient, so they typically need no change.

### E3. Staging Folder (`_jdbc-stage`)

Transient build-only directory at `${assembly-directory}/_jdbc-stage/` (= `target/classes/distribution/_jdbc-stage/`).

| Attribute | Value |
|-----------|-------|
| Created by | `stage-jdbc-drivers` execution (`pom.xml:496-509`) |
| Populated by | `maven-dependency-plugin:copy-dependencies` with `includeScope=provided`, `excludeTransitive=true` |
| Consumed by | ANT `<copy>` at `installDistributionFiles.xml:707-717` |
| Lifecycle | MUST be physically deleted from `${assembly-directory}` before assembly packages the JAR (`maven-assembly-plugin` execution at `pom.xml:672-694`) |

Validation rules:
- The directory MUST NOT exist inside the produced `perc-distribution-tree.jar`.
- The deletion MUST be idempotent (succeed whether the directory exists or not) — implemented by ANT `<delete>` semantics, which is idempotent by default.
- The deletion step MUST run after the staged copy (which consumes the directory) and before the `maven-assembly-plugin` `package` phase.

### E4. `verify-jdbc-drivers.sh` Example Invocation (after change)

`modules/perc-distribution-tree/scripts/README.md` line 17 must show an example that exits 0 against a freshly built artifact. Per clarification Q4, the example is switched to `--expected-driver-glob` using the same globs wired into the Maven `verify` execution:

```sh
./scripts/verify-jdbc-drivers.sh --artifact path/to/perc-distribution-tree.jar \
    --expected-driver-glob 'mariadb-java-client-*.jar,derby-*.jar,derbyclient-*.jar,derbynet-*.jar,mssql-jdbc-*.jar,jtds-*.jar,ojdbc17-*.jar'
```

Validation rules:
- Quoting style is single-quotes (POSIX-safe; the glob contains `*`).
- The glob list is identical to the one wired into `pom.xml:737`.
- Running this example verbatim against `target/perc-distribution-tree.jar` MUST exit 0.

## Relationships

```
parent pom.xml         (versions, single source of truth)
   │  ${mariadb.version}, ${derby.version}, ${mssql.version}, ${jtds.version}, ${ojdbc17.version}
   ▼
perc-distribution-tree/pom.xml
   │  <provided> driver deps (E1)        stage-jdbc-drivers execution
   ▼
target/classes/distribution/_jdbc-stage/  (E3, transient)
   │  <copy> in installDistributionFiles.xml:707-717
   ▼
target/classes/distribution/jetty/base/lib/jdbc/  (E2, curated set in installer payload)
   │  maven-assembly-plugin packages into
   ▼
target/perc-distribution-tree.jar  (SC-001: no _jdbc-stage/** inside; SC-002: jdbc/ has exactly E1)

At install/upgrade time, on the customer's machine:
perc-distribution-tree.jar  ── unpacked by installer ──▶  $install.dir/jetty/base/lib/jdbc/
   │  install.xml:install_jdbc_drivers target:
   │    1. <delete> the E2 exact-filename list from $install.dir/jetty/base/lib/jdbc/   (preserves integrator drivers)
   │    2. <copy> from $install.src/jetty/base/lib/jdbc/ into $install.dir/jetty/base/lib/jdbc/
   ▼
Final customer state: bundled E1 + any integrator-supplied drivers that survived
```

## State transitions

None — this feature has no persistent state. All state is build-time or install-time and is replaced on each run.

## Test fixtures (for Phase 1 quickstart and tasks)

- **JUnit 5 test fixture**: the unit test will parse `install.xml` (as a classpath resource) and the test will assert (a) every filename in the E2 list is in the delete set; (b) no entry in the delete set contains `*` or `?`; (c) `mysql-connector-java-9.0.0.jar` and `ojdbc17-99.99.99.99.jar` are not in the delete set. The expected delete set is a constant in the test that mirrors E2.
- **Shell assertion fixture**: `scripts/check-no-glob-deletes.sh` extracts the `<delete>` block under the `install_jdbc_drivers` target from `install.xml` and greps for `*` and `?` characters within `<include name="...">` lines. Exits 0 if none found, exits 7 (new exit code) if any glob remains.

## Out-of-scope entities

- No database tables, no REST resources, no `.ppkg` packages, no i18n keys. (Constitution IV — Contract & Integration Integrity: no public or semi-public surface is changed.)