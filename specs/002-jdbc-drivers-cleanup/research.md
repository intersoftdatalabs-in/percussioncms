# Research: JDBC Drivers Packaging Cleanup

**Phase**: 0 — Outline & Research
**Date**: 2026-07-11
**Spec**: [spec.md](./spec.md)
**Branch**: `002-jdbc-drivers-cleanup`

## Purpose

This document records the evidence used to plan the implementation. Every fact below was verified directly against the current `development` branch in this workspace. No NEEDS CLARIFICATION items remain in the spec after the 2026-07-11 clarification session; the unknowns that existed in the original PR review have been resolved by the spec's locked-in decisions and are simply restated here with the evidence that supports them.

## Verified facts (evidence base)

### F1. `_jdbc-stage` leakage is real and measurable

- `modules/perc-distribution-tree/pom.xml:496-509` defines execution `stage-jdbc-drivers` with `outputDirectory = ${assembly-directory}/_jdbc-stage`. It uses `includeScope=provided`, `excludeTransitive=true`. Provided-scope dependencies in this module (`pom.xml:147-181`):
  - `org.mariadb.jdbc:mariadb-java-client`
  - `org.apache.derby:derby`, `derbyclient`, `derbynet` (and `derbyshared`, `derbytools` are declared in the parent POM at `pom.xml:1443-1470` but are not declared as `provided` dependencies of this module — they are only present transitively, so they will not be copied by `copy-dependencies` with `excludeTransitive=true`. Verified by reading the dependency block: the only `provided` JDBC driver deps are the 7 listed above.)
- The `<copy>` at `installDistributionFiles.xml:707-717` then copies from `${assembly-directory}/_jdbc-stage` into `${assembly-directory}/jetty/base/lib/jdbc/`. The staging directory itself is never deleted.
- The `maven-assembly-plugin` execution `create-my-bundle` at `pom.xml:672-694` uses `jar-with-dependencies` with no exclude pattern for `_jdbc-stage/**`, so the entire `${assembly-directory}` tree is packaged.
- **Decision**: Per FR-001 and the spec's clarification Q3 (Option B), the fix is `<delete dir="${assembly-directory}/_jdbc-stage"/>` immediately after the staged copy, before assembly runs.

### F2. Install script glob delete can hit integrator drivers

- `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/install.xml:174-188` deletes from `jetty/base/lib/jdbc` using glob patterns:
  - `mariadb-java-client-*.jar`, `derby-*.jar`, `derbyclient-*.jar`, `derbynet-*.jar`, `derbyshared-*.jar`, `derbytools-*.jar`, `mssql-jdbc-*.jar`, `jtds-*.jar`, `ojdbc17-*.jar`, `mysql-connector-java-*.jar`, `mysql-connector.jar`
- A vendor driver like `mysql-connector-java-9.0.0.jar` or `ojdbc17-23.5.0.0.0.jar` would be deleted.
- `modules/perc-distribution-tree/README.md:80` states: "The install scripts (`rxconfig/Installer/install.xml`, `installServer.xml`, `installRepository.xml`) do not purge this folder." Contradiction.
- **Decision**: Per spec clarification Q1 (Option A — preserve) and Q2 (Option B — pin in `install.xml`), the glob delete is replaced with an exact-filename list, regenerated each release from the curated `pom.xml` driver set.

### F3. Curated driver set and resolved filenames (the exact pin list)

- Maven coordinates and parent-POM-managed versions:
  - `org.mariadb.jdbc:mariadb-java-client` → `pom.xml:211` (`${mariadb.version} = 3.5.7`) → resolves to `mariadb-java-client-3.5.7.jar`
  - `org.apache.derby:derby` → `pom.xml:115` (`${derby.version} = 10.17.1.0`) → `derby-10.17.1.0.jar`
  - `org.apache.derby:derbyclient` → same → `derbyclient-10.17.1.0.jar`
  - `org.apache.derby:derbynet` → same → `derbynet-10.17.1.0.jar`
  - `com.microsoft.sqlserver:mssql-jdbc` → `pom.xml:210` (`${mssql.version} = 13.3.1.jre11-preview`) → `mssql-jdbc-13.3.1.jre11-preview.jar`
  - `net.sourceforge.jtds:jtds` → `pom.xml:168` (`${jtds.version} = 1.3.1`) → `jtds-1.3.1.jar`
  - `com.oracle.database.jdbc:ojdbc17` → `pom.xml:212` (`${ojdbc17.version} = 23.26.0.0.0`) → `ojdbc17-23.26.0.0.0.jar`
- **Note on legacy `mysql-connector` entries**: the current delete list includes `mysql-connector-java-*.jar` and `mysql-connector.jar`. The mariadb-java-client *is* the production MySQL driver (per the parent POM's table at `README.md:70` — "MariaDB / MySQL (default repository)"). These `mysql-connector*` globs match no shipped JAR today, but they will silently delete any integrator-supplied `mysql-connector-java-*.jar` (e.g. a customer adding a stock MySQL Connector/J driver for a non-MariaDB MySQL setup). These two globs MUST be removed from the install script's delete set as part of the FR-003 fix.
- **Note on `derbyshared-*.jar` / `derbytools-*.jar`**: these are declared in the parent POM dependencyManagement (`pom.xml:1461-1470`) but are NOT pulled into `perc-distribution-tree` (the module has no `provided` dep on them, and the `stage-jdbc-drivers` execution has `excludeTransitive=true`). They therefore never ship in the curated set. They are safe to remove from the install script's delete list (they will never match a shipped file), and removing them reduces the risk of accidentally purging an integrator-supplied `derbyshared-*.jar` (which can exist in the wild).

### F4. Maven `verify` phase already uses `--expected-driver-glob`

- `pom.xml:725-740` invokes `verify-jdbc-drivers.sh` with `--expected-driver-glob mariadb-java-client-*.jar,derby-*.jar,derbyclient-*.jar,derbynet-*.jar,mssql-jdbc-*.jar,jtds-*.jar,ojdbc17-*.jar`. This is the canonical, version-resilient invocation.
- `scripts/README.md:17` documents `--expected-driver-set mariadb-connector.jar,derby.jar,mssql-connector.jar,jtds.jar,ojdbc17.jar` — wrong filenames; never matches the shipped artifacts; will always exit 6.
- **Decision**: Per spec clarification Q4 (Option A), replace the README example with `--expected-driver-glob` using the same globs wired into the Maven `verify` execution (so the example is faithful to the wired-in invocation and survives driver version bumps).

### F5. The misleading ANT copy comment

- `installDistributionFiles.xml:702-704` claims: "ANT `<copy>` fails by default when a staged source file is missing; combined with `failOnAnyMissingDependency=true` on the Maven copy, this gives the loud-failure behavior required by FR-003 / SC-004."
- The `<copy>` at lines 707-717 uses a globbed `<fileset>`. When no globs match, ANT's `<copy>` is a silent no-op — it does not fail.
- The actual loud-failure guarantees are: (a) `failOnAnyMissingDependency=true` on the Maven `copy-dependencies` execution in `pom.xml:506` (this catches missing Maven coordinates); (b) the `verify-jdbc-drivers` exec in the `verify` phase at `pom.xml:725-740` (this catches missing/invalid JARs in the assembled artifact).
- **Decision**: Per FR-005, the comment is rephrased to honestly describe ANT behavior and to attribute loud-failure to the correct mechanisms.

### F6. Existing test patterns in this module

- The module has no existing Java test sources (no `src/test/java` tree) — the only verification is the shell-based `verify-jdbc-drivers.sh`. Adding the first JUnit 5 test requires: adding `junit-jupiter` to the module POM (via parent-POM `dependencyManagement`), enabling `maven-surefire-plugin` (already in parent POM `pluginManagement`), and creating a single test class.
- The verify script at `scripts/verify-jdbc-drivers.sh` is a clean POSIX-`sh` reference for the assertion style (clear error messages, distinct exit codes, no over-engineering).

## Resolved decisions (locked by spec clarification)

| #  |                                                          Decision                                                          |       Spec reference        |
|----|----------------------------------------------------------------------------------------------------------------------------|-----------------------------|
| D1 | Install/upgrade script policy = **preserve integrator drivers**                                                            | Clarification Q1 (Option A) |
| D2 | Source of truth for the delete set = **exact-filename list pinned in `install.xml`**                                       | Clarification Q2 (Option B) |
| D3 | Mechanism to keep `_jdbc-stage` out of JAR = **`<delete dir="${assembly-directory}/_jdbc-stage"/>` after the staged copy** | Clarification Q3 (Option B) |
| D4 | `scripts/README.md` example = **switch to `--expected-driver-glob` with real Maven-resolved globs**                        | Clarification Q4 (Option A) |
| D5 | Test scope = **JUnit 5 unit test + shell assertion wired into Maven `verify`**                                             | Clarification Q5 (Option C) |

## Out-of-scope (deliberately not addressed)

- Modernizing the broader install script structure (e.g. refactoring it from ANT to Maven or a Java class). The fix is a surgical pin of the delete set; modernization is a separate effort (Constitution V — Safe Modernization prefers incremental improvement over drive-by refactors).
- Removing the `mysql*.jar` "copy old driver from app server" logic at `install.xml:156-166` (the install_jdbc_drivers target copies mysql drivers from old AppServer paths before the bundled copy). This is the *copy* side, not the *delete* side, and is a different (legacy-migration) concern. Out of scope for this cleanup.
- Changing the `mariadb-java-client` artifact (e.g. switching to `mysql-connector-j`). The MariaDB driver is the production MySQL driver per the parent POM table.
- Adding a `derbyshared` / `derbytools` `provided` dependency to ship those JARs. They are not currently shipped and have not been requested by customers. Out of scope.

## Best-practice references (per Constitution II — Evidence Over Invention)

- ANT `<copy>` semantics with empty globbed fileset: ANT manual "Default exclude behavior" — empty `<fileset>` is a no-op, not a failure.
- `maven-dependency-plugin:copy-dependencies` `failOnAnyMissingDependency` flag: see Apache Maven Dependency Plugin documentation; causes the goal to fail the build when any requested artifact cannot be resolved.
- JUnit 5 + Maven Surefire pattern in this repo: see any of the existing test modules under `modules/perc-*` for the established `junit-jupiter` + `@Test` pattern; the parent POM provides `maven-surefire-plugin` in `pluginManagement` with the project's standard `junit-platform` configuration.
- POSIX-`sh` assertion style: mirrors `verify-jdbc-drivers.sh` (clear error messages, distinct exit codes, `set -u`, `trap cleanup EXIT`).

## Conclusion

All open questions from the original PR review (and from the spec's "WHAT" analysis) are resolved. Phase 1 design can proceed using the resolved decisions and the verified facts above. No NEEDS CLARIFICATION items remain.
