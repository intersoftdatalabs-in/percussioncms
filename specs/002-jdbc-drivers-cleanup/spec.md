# Feature Specification: JDBC Drivers Packaging Cleanup

**Feature Branch**: `002-jdbc-drivers-cleanup`
**Created**: 2026-07-11
**Status**: Draft
**Input**: User description: "We received some late feedback after 001 was implemented that needs to be addressed. Can you review the review feedback on https://github.com/intersoftdatalabs-in/percussioncms/pull/1184 and start a follow up feature."

## Module Scope *(mandatory for this mono-repo)*

- **Primary module(s)**: `modules/perc-distribution-tree/`
- **Secondary / integration modules**: none
- **AGENTS files to apply**: `./AGENTS.md`, `./modules/perc-distribution-tree/AGENTS.md` (if present)
- **User roles affected**: integrator, ops/site admin, release engineer
- **Install / upgrade impact**: distribution tree (the `_jdbc-stage` leak affects the shipped installer payload); install script behavior (glob delete policy)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Exclude staging directory from shipped distribution (Priority: P1)

A release engineer packaging `perc-distribution-tree.jar` must not have unrelated provided-scope JARs (non-JDBC dependencies that happened to land in the same staging folder) leak into the shipped installer payload. The staging folder used during the build must be excluded or removed before the JAR-with-dependencies is assembled so that only the curated JDBC drivers end up under `jetty/base/lib/jdbc/`.

**Why this priority**: This is a correctness issue for the shipped distribution. Every released build today silently carries ~15 unintended JARs in the installer, which inflates payload size and creates a real risk of classpath conflicts and unintended driver exposure on customer sites. It must be fixed before any further release.

**Independent Test**: After building `perc-distribution-tree`, inspect the contents of `target/perc-distribution-tree-*.jar` (or the relevant installer payload) and confirm that no path matching `**/_jdbc-stage/**` exists, and that no JAR under `jetty/base/lib/jdbc/` is a non-curated dependency.

**Acceptance Scenarios**:

1. **Given** a clean build of `perc-distribution-tree`, **When** the assemble lifecycle runs the `stage-jdbc-drivers` execution and the assembly plugin packages the JAR-with-dependencies, **Then** no `**/_jdbc-stage/**` path is present inside the produced JAR, and `jetty/base/lib/jdbc/` contains only the curated drivers defined for this release.
2. **Given** the staged source folder contains the curated JDBC drivers plus other provided-scope JARs (non-JDBC), **When** the build completes, **Then** the only JARs that appear under the distribution's `jetty/base/lib/jdbc/` are the curated set; the other provided JARs are not present anywhere in the JAR-with-dependencies payload.

---

### User Story 2 - Stop install script from purging integrator-supplied drivers (Priority: P1)

An integrator who has placed a vendor-supplied JDBC driver into `jetty/base/lib/jdbc` (for example a newer `ojdbc17-23.5.0.0.0.jar` or a `mysql-connector-java-*.jar` matching the bundled name pattern) must not have that driver silently removed by the install/upgrade script. The behavior must match the documented promise in `modules/perc-distribution-tree/README.md` that the install script does not purge this folder.

**Why this priority**: Silent data-loss for a customer's chosen driver on upgrade is a high-impact correctness bug and a documentation/behavior contradiction. Integrator trust and upgrade safety depend on this.

**Independent Test**: Drop a vendor JAR (e.g. `ojdbc17-99.99.99.99.jar` or `mysql-connector-java-9.0.0.jar`) into a fresh `jetty/base/lib/jdbc`, run the install/upgrade ANT script, and assert that the integrator's JAR is still present after the script completes.

**Acceptance Scenarios**:

1. **Given** `jetty/base/lib/jdbc` contains an integrator-supplied JAR whose filename matches a bundled-driver glob pattern (e.g. `mysql-connector-java-*.jar`), **When** the install/upgrade script runs, **Then** that integrator JAR is still present after the script completes.
2. **Given** the bundled set of curated drivers is the expected set for the current release, **When** the install/upgrade script runs on a clean `jetty/base/lib/jdbc`, **Then** exactly the curated set is present after the script completes.
3. **Given** `modules/perc-distribution-tree/README.md`, **When** a reader reviews the documentation about the `jetty/base/lib/jdbc` folder, **Then** the documented behavior matches the actual behavior of the install script (no silent purge of matching-name integrator drivers).

---

### User Story 3 - Correct the misleading ANT copy comment and the verify-script example (Priority: P2)

A maintainer reading the build XML around `stage-jdbc-drivers` must not be misled into believing that ANT's `<copy>` will fail the build when a staged source is missing. The verify-script example in `scripts/README.md` must use driver filenames that actually match what the build ships, so that running the example does not falsely report missing drivers.

**Why this priority**: Misleading comments and broken examples erode confidence in the build and verification tooling, and they hide the real failure guarantees. They are correctness-of-documentation issues, not security issues, and do not block a release once P1 items are addressed.

**Independent Test**: Read the relevant comment block in `installDistributionFiles.xml` near the stage-jdbc-drivers execution and confirm it accurately describes ANT behavior (empty globbed `<fileset>` is a silent no-op, not a build failure) and correctly attributes loud-failure to the Maven copy/verify phase. Then run the documented verify-script example from `scripts/README.md` on a freshly built artifact and confirm it exits 0.

**Acceptance Scenarios**:

1. **Given** the ANT `<copy>` uses a globbed `<fileset>` for staged JDBC drivers, **When** a maintainer reads the surrounding comment, **Then** the comment does not claim the copy itself fails the build when no files match; it correctly states the loud-failure guarantee comes from the Maven copy/verify phase.
2. **Given** `scripts/README.md` documents example invocations of `verify-jdbc-drivers.sh`, **When** a user copies the documented example verbatim against a freshly built artifact, **Then** the example exits 0 and does not falsely report missing drivers.

---

### Edge Cases

- What happens when the curated driver list changes between releases and an integrator's vendor driver happens to match a now-renamed bundled glob? (Stated P1 outcome: integrator driver is preserved.)
- What happens when no curated drivers are present in the build (e.g. driver dependencies were not declared in this release)? The build must still produce a valid JAR-with-dependencies (no build failure from empty stage), and `verify-jdbc-drivers` must report the missing drivers via the Maven verify phase, not via a silent ANT no-op.
- What happens if `_jdbc-stage` already exists in a developer's local `target/classes/distribution/` from a previous build? The cleanup must be idempotent and not fail on a missing staging directory.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The `perc-distribution-tree` Maven build MUST NOT include any `_jdbc-stage/**` path inside the produced `perc-distribution-tree.jar` (or JAR-with-dependencies equivalent). The mechanism MUST be an explicit `<delete dir="${assembly-directory}/_jdbc-stage"/>` step in `modules/perc-distribution-tree/src/main/resources/installDistributionFiles.xml` immediately after the staged copy, before assembly runs, so the staging directory is physically removed (not merely excluded) prior to packaging.
- **FR-002**: The `perc-distribution-tree` Maven build MUST ensure that only the curated set of JDBC drivers is present under `jetty/base/lib/jdbc/` in the assembled distribution; non-JDBC provided-scope dependencies MUST NOT be included in the JDBC folder or anywhere else in the JDBC area of the assembled JAR.
- **FR-003**: The install/upgrade ANT script MUST NOT delete an integrator-supplied JAR from `jetty/base/lib/jdbc/` solely because its filename matches a bundled-driver glob pattern. Driver removal in this script MUST be limited to the bundled drivers actually shipped by this release, identified by the exact bundled filename (e.g. `<artifactId>-<version>.jar`) for each curated driver. The delete set MUST be a small, explicit list of exact `<artifactId>-<version>.jar` filenames pinned directly in `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/install.xml`, regenerated each release from the curated `pom.xml` driver set (single source of truth = the pom).
- **FR-004**: The behavior of the install/upgrade script regarding `jetty/base/lib/jdbc/` MUST match the documentation in `modules/perc-distribution-tree/README.md`. The README's promise that the install script "does not purge this folder" (per `modules/perc-distribution-tree/README.md:80`) MUST be made true by this change — integrator-supplied drivers are preserved on install/upgrade.
- **FR-005**: The comment block in `installDistributionFiles.xml` describing the `<copy>` for staged JDBC drivers MUST accurately describe ANT behavior. The loud-failure guarantee for missing drivers MUST be attributed to the correct mechanism (Maven `failOnAnyMissingDependency` on the copy and/or the `verify-jdbc-drivers` exec in the verify phase), not to the ANT `<copy>` itself.
- **FR-006**: The example invocations of `verify-jdbc-drivers.sh` in `modules/perc-distribution-tree/scripts/README.md` MUST use the script option actually wired into the Maven `verify` execution (`--expected-driver-glob`, per `modules/perc-distribution-tree/pom.xml:736`), with globs matching the real Maven-resolved artifact names (e.g. `mariadb-java-client-*.jar`, `derby-*.jar`, `ojdbc17-*.jar`, `mssql-jdbc-*.jar`, `jtds-*.jar`). The example MUST exit 0 when run verbatim against a freshly built artifact.
- **FR-007**: The cleanup of the `_jdbc-stage` directory MUST be idempotent: it MUST succeed (no failure) whether the directory exists or does not exist at cleanup time.
- **FR-008**: The change MUST ship with automated tests satisfying the Constitution III (Test Discipline) non-negotiable for a change that touches the package install/upgrade path. Minimum coverage:
  - A JUnit 5 unit test (using the module's existing test patterns and the project Maven wrapper `./mvn-env.sh`) that loads the install/upgrade ANT script's delete set and asserts: (a) every bundled driver declared in `pom.xml` (provided-scope driver dependencies) is in the delete set under its exact `<artifactId>-<version>.jar` filename; (b) no filename containing a glob/wildcard character (`*`, `?`) appears in the delete set; (c) a representative integrator-supplied filename (e.g. `mysql-connector-java-9.0.0.jar`, `ojdbc17-99.99.99.99.jar`) is NOT in the delete set.
  - A shell-based assertion (extend `verify-jdbc-drivers.sh` or add a sibling script under `modules/perc-distribution-tree/scripts/`) that is wired into the Maven `verify` phase and that statically inspects `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/install.xml` to assert no glob-based `<delete>` patterns remain in the `jetty/base/lib/jdbc` cleanup logic.
  - Tests MUST be runnable via the project Maven wrapper on the target branch JDK and MUST pass before this change is considered complete.

### Key Entities *(include if feature involves data)*

- **Curated JDBC Driver Set**: The specific set of JDBC driver artifacts (groupId/artifactId/version) declared for inclusion in `jetty/base/lib/jdbc/` for a given release. Authoritative source for what must ship and what must not.
- **Staging Folder (`_jdbc-stage`)**: A transient build-only directory used during the `stage-jdbc-drivers` Maven execution. Must never appear in the assembled JAR.
- **Integrator-Supplied Driver**: A JDBC JAR placed into `jetty/base/lib/jdbc/` by the integrator (customer / ops) outside of the bundled set. The install/upgrade script must not silently remove it solely based on filename-pattern matching.
- **`verify-jdbc-drivers.sh` Example Invocation**: A documented command-line example in `scripts/README.md`; the filenames and options in the example must be a faithful representation of what the build actually produces.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: After building `perc-distribution-tree`, inspecting the produced `perc-distribution-tree-*.jar` (e.g. via `unzip -l` or equivalent) shows zero entries matching `**/_jdbc-stage/**`.
- **SC-002**: After building `perc-distribution-tree`, the list of JARs under `jetty/base/lib/jdbc/` inside the produced JAR is byte-identical to the curated set declared for the release (no extra non-JDBC JARs, no missing curated drivers).
- **SC-003**: Running the install/upgrade ANT script against a `jetty/base/lib/jdbc` folder containing both the curated bundled drivers and an integrator-supplied driver whose name matches a bundled glob results in the integrator-supplied driver still being present after the script completes.
- **SC-004**: Running the documented `verify-jdbc-drivers.sh` example from `modules/perc-distribution-tree/scripts/README.md` verbatim against a freshly built artifact exits 0 and reports all expected drivers present. The example MUST use `--expected-driver-glob` (the option actually wired into the Maven `verify` execution) with globs matching the real Maven-resolved artifact names.
- **SC-005**: The behavior of the install/upgrade script with respect to `jetty/base/lib/jdbc` is consistent with `modules/perc-distribution-tree/README.md` — the README's claim that the install script "does not purge this folder" (per `modules/perc-distribution-tree/README.md:80`) is true after this change: only exact-bundled-filename JARs are deleted; integrator-supplied JARs are preserved.
- **SC-006**: The automated test suite added per FR-008 runs green via the project Maven wrapper `./mvn-env.sh` against the target branch JDK. The JUnit 5 unit test asserts the exact-filename delete set, the absence of glob patterns in the delete set, and the preservation of integrator-supplied filenames. The shell-based static assertion wired into the Maven `verify` phase reports zero glob-based `<delete>` patterns in `install.xml`'s `jetty/base/lib/jdbc` cleanup logic.

## Clarifications

### Session 2026-07-11

- Q: Policy for the install/upgrade ANT script's `jdbc/` folder cleanup (preserves integrator drivers vs. documents the purge vs. quarantine-then-purge)? → A: Preserve integrator drivers (Option A). The script's deletes are narrowed to the exact bundled-driver filenames; integrator-supplied JARs are left untouched. The README's "do not purge this folder" promise (per `modules/perc-distribution-tree/README.md:80`) is made true by this change.
- Q: Where does the install/upgrade ANT script get its authoritative bundled-driver filename list (single source of truth for the targeted delete)? → A: Pin the list directly in `install.xml` (Option B). A small, explicit list of exact `<artifactId>-<version>.jar` filenames is maintained in `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/install.xml`, regenerated each release from the curated `pom.xml` driver set.
- Q: Mechanism to keep `_jdbc-stage` out of the assembled JAR (assembly descriptor exclude vs. `<delete>` after copy vs. restructure staging location)? → A: `<delete dir="${assembly-directory}/_jdbc-stage"/>` after the staged copy (Option B), in `modules/perc-distribution-tree/src/main/resources/installDistributionFiles.xml`, run immediately after the staged copy and before assembly runs.
- Q: `scripts/README.md` example — fix filenames or switch to `--expected-driver-glob`? → A: Switch the example to `--expected-driver-glob` (Option A) with globs matching the real Maven-resolved artifact names (e.g. `mariadb-java-client-*.jar`, `derby-*.jar`, `ojdbc17-*.jar`, `mssql-jdbc-*.jar`, `jtds-*.jar`). Matches the option actually wired into the Maven `verify` execution and survives version bumps.
- Q: Test coverage scope (Constitution III non-negotiable for a change touching install/upgrade paths)? → A: JUnit 5 unit test on delete logic + shell-based static assertion wired into Maven `verify` (Option C). Unit test asserts the exact-filename delete set, no glob patterns in the delete set, and preservation of integrator-supplied filenames. Shell assertion statically inspects `install.xml` to assert no glob-based `<delete>` patterns remain.

## Assumptions

- The curated set of JDBC drivers for the current release is defined in `modules/perc-distribution-tree/pom.xml` (provided-scope driver dependencies) and is the single source of truth for what ships to `jetty/base/lib/jdbc/`.
- The `_jdbc-stage` directory is purely a transient build artifact and is not referenced by any runtime/install behavior.
- The install/upgrade ANT script (`modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/install.xml`) is the script executed during CMS install/upgrade when this distribution tree is unpacked.
- `verify-jdbc-drivers.sh` is invoked by the Maven `verify` phase; whatever options/filenames are wired into that invocation are the ones that must succeed in the documented example.
- The project is on the `development` branch (JDK 21) and the changes will be implemented using `./mvn-env.sh` per `AGENTS.md`.
- No DB schema, package (`.ppkg`), or runtime config changes are required; this is a build-time and install-script cleanup only.

