# Feature Specification: Fix Missing JDBC Drivers in Percussion Distribution Install

**Feature Branch**: `[001-fix-jdbc-drivers]`
**Created**: 2026-07-10
**Status**: Draft
**Input**: User description: "When we run the perc-distribution installation, that target jetty/base/lib/jdbc folder is empty. none of the database drivers are being included or deployed by the installer. This is either a packaging problem in perc-distribution-tree or an error in the install scripts."

## Module Scope *(mandatory for this mono-repo)*

- **Primary module(s)**: `modules/perc-distribution-tree`
- **Secondary / integration modules**: `modules/perc-jetty`, `modules/perc-jetty-jars`, `modules/perc-jetty-logging`, install scripts under `modules/perc-ant` and `modules/perc-rxapps`
- **AGENTS files to apply**: root `AGENTS.md`, `modules/perc-distribution-tree/AGENTS.md`
- **User roles affected**: integrator (admin performing install/upgrade), operator (running the deployed CMS)
- **Install / upgrade impact**: distribution tree (the assembled install artifact must contain database drivers so the CMS can connect to its repository on first start)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Integrator performs a clean install (Priority: P1)

An integrator downloads the Percussion CMS distribution and runs the installer on a fresh target host. After the installer finishes, the integrator (or the CMS itself on first start) needs to be able to connect to the chosen database. Today, the `jetty/base/lib/jdbc/` directory in the assembled distribution is empty, so Jetty cannot load any JDBC driver and the CMS fails to start its database connection.

**Why this priority**: Without JDBC drivers in the assembled distribution, the product is uninstallable in any non-trivial database scenario. This is the primary value path for every customer install.

**Independent Test**: Run a clean build of `modules/perc-distribution-tree`, unpack the resulting distribution archive, and inspect `jetty/base/lib/jdbc/`. The directory must contain the database driver JAR(s) required to bootstrap a connection to the default repository, and the CMS must be able to start against the default repository without manual driver placement by the integrator.

**Acceptance Scenarios**:

1. **Given** a clean checkout and a fresh `mvn clean install` of `modules/perc-distribution-tree` performed without any environment overrides, **When** the assembled distribution archive is unpacked, **Then** `jetty/base/lib/jdbc/` exists and contains at least one JDBC driver JAR sufficient to bootstrap the default repository.
2. **Given** the unpacked distribution, **When** the CMS is started with the default repository configuration, **Then** the server starts successfully and the repository connects without the integrator needing to copy additional JDBC driver JARs into `jetty/base/lib/jdbc/`.
3. **Given** the unpacked distribution, **When** the integrator inspects `jetty/base/lib/jdbc/`, **Then** every JAR present is a real JDBC driver JAR (no zero-byte placeholders, no unrelated libraries).

---

### User Story 2 - Integrator swaps to a different supported database (Priority: P2)

An integrator has the distribution unpacked and wants to point the CMS at a different supported database (for example, switching from the default bundled driver to a vendor-supplied or different-version driver for their enterprise database). They expect the `jetty/base/lib/jdbc/` folder to be the documented, supported place to add or replace driver JARs, and they expect any driver that ships with the distribution to be functionally usable.

**Why this priority**: Even if every install is on the default database, the driver folder is the documented extension point for switching databases. Empty or non-functional files break that contract.

**Independent Test**: After a clean install, place a vendor JDBC driver JAR into `jetty/base/lib/jdbc/`, restart the CMS, and verify the server picks up the driver. Separately, verify that any JDBC JAR that ships in the distribution is a real, non-truncated JDBC driver (not an empty file or a stub).

**Acceptance Scenarios**:

1. **Given** the unpacked distribution with `jetty/base/lib/jdbc/` populated, **When** the integrator adds a new JDBC driver JAR and restarts, **Then** the new driver is recognized on startup.
2. **Given** any JDBC driver JAR that ships in the distribution, **When** the integrator inspects it (file size, contents), **Then** it is a valid, non-empty JDBC driver implementation (not a placeholder).

---

### User Story 3 - Build / CI verifies driver inclusion (Priority: P3)

A build engineer or CI pipeline builds `modules/perc-distribution-tree` and needs an automated way to assert that the assembled distribution actually includes the expected JDBC driver(s). Without this, the regression that produced the empty `jdbc/` folder can silently return.

**Why this priority**: This is a guardrail against the regression re-occurring, not a user-facing capability on its own. It depends on User Story 1 being delivered first.

**Independent Test**: After a build, run an automated check that confirms `jetty/base/lib/jdbc/` in the assembled artifact contains the expected driver(s) with non-zero size and that the count of drivers matches what the build was configured to ship.

**Acceptance Scenarios**:

1. **Given** a built distribution artifact, **When** the automated check runs, **Then** it passes only when `jetty/base/lib/jdbc/` contains the configured set of non-empty driver JARs.
2. **Given** a build that accidentally drops driver copies (for example, by reintroducing a guard that only copies drivers under a non-default flag), **When** the automated check runs, **Then** it fails with a clear message identifying the missing or empty driver JAR(s).

---

### Edge Cases

- What happens when the source location for a bundled JDBC driver is missing from the build (for example, the legacy `system/Tools/mysql/` path referenced by the current ANT script no longer exists)? The build must fail loudly, not silently produce an empty `jdbc/` directory.
- What happens when an integrator's target filesystem is case-sensitive (Linux) vs. case-insensitive (Windows)? The install path and any subsequent runtime lookups must be case-correct on both.
- What happens if the build is invoked with the legacy `DEVELOPMENT=true` environment override? The behavior must remain backward-compatible (the development driver must still be copied in that mode) while production builds without the override must also include the drivers.
- What happens when the integrator picks a database whose driver is not bundled (for example, an enterprise Oracle or DB2 driver)? The install must not pretend to provide one; the `jdbc/` folder must still exist and be empty-or-documented so the integrator knows where to drop their own driver.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The assembled distribution archive produced by `modules/perc-distribution-tree` MUST contain a `jetty/base/lib/jdbc/` directory in the production (non-`DEVELOPMENT`) build.
- **FR-002**: The `jetty/base/lib/jdbc/` directory in the assembled distribution MUST contain at least one JDBC driver JAR sufficient to bootstrap the default repository of the CMS, and every JAR present MUST be a non-empty, valid JDBC driver implementation.
- **FR-003**: The build for `modules/perc-distribution-tree` MUST fail with a clear, actionable error message when a required source JDBC driver JAR cannot be located, instead of silently producing an empty driver directory.
- **FR-004**: The behavior under the legacy `DEVELOPMENT=true` environment override MUST be preserved: when that override is set, the development driver copy path must continue to function as it does today (backward compatibility).
- **FR-005**: The source location(s) used to populate `jetty/base/lib/jdbc/` during assembly MUST point to locations that exist in the current repository layout; if a previously used path no longer exists, it MUST be updated to a current, valid path (for example, the correct location under `modules/perc-jetty-jars/`, `modules/perc-jetty/`, or a curated dependency in the parent POM) rather than left dangling.
- **FR-006**: The set of JDBC driver(s) shipped in the distribution MUST be documented in `modules/perc-distribution-tree/README.md` (and/or `AGENTS.md`) so integrators know what is bundled and how to extend it for additional databases.
- **FR-007**: An automated verification step MUST be available (for example, a script or test) that can confirm after a build that `jetty/base/lib/jdbc/` in the assembled artifact contains the expected non-empty driver JAR(s); this verification must be runnable from a clean checkout without privileged access.

### Key Entities *(include if feature involves data)*

- **Distribution Artifact**: The packaged archive produced by `modules/perc-distribution-tree`. Has a fixed internal layout including `jetty/base/lib/jdbc/`.
- **JDBC Driver JAR**: A Java archive containing a JDBC driver implementation, identified by name and version. Sourced either from the build's curated dependencies or from a well-known path in the repo.
- **Installer / Build Mode**: The mode in which the distribution was assembled (production vs. `DEVELOPMENT=true`), which historically has gated the copy of drivers.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: After a clean `mvn clean install` of `modules/perc-distribution-tree` with no environment overrides, the assembled distribution's `jetty/base/lib/jdbc/` directory contains one or more JDBC driver JARs and the total size of that directory is greater than zero (verifiable by a simple directory listing + size check on the unpacked artifact).
- **SC-002**: 100% of JDBC driver JARs shipped in the assembled distribution are non-empty files (file size > 0 bytes) and are recognized as valid Java archives (for example, openable with `jar tf`).
- **SC-003**: The CMS, when started from the unpacked distribution with the default repository configuration, can establish its initial repository connection without any manual JDBC driver placement by the integrator (verifiable by a clean install + first-start run).
- **SC-004**: Builds that cannot locate a required source JDBC driver JAR fail the build with an actionable error message (containing the missing path or coordinate) rather than silently producing an empty `jdbc/` directory.
- **SC-005**: The automated verification step (per FR-007) is runnable as part of the module's build verification and exits non-zero when the shipped drivers do not match the expected, non-empty set.

## Assumptions

- The default repository for a fresh install is the bundled database (MySQL/MariaDB family), and shipping at least one matching JDBC driver is sufficient to satisfy the "bootstrap the default repository" requirement. Drivers for additional enterprise databases (Oracle, MS SQL Server, DB2, etc.) are expected to be supplied by the integrator if needed and do not have to ship with the default distribution.
- The `modules/perc-distribution-tree/AGENTS.md` guidance (build via `../../mvn-env.sh clean install`) applies; no changes are required to the wrapper itself.
- The legacy `DEVELOPMENT=true` override and the path it currently references are kept for backward compatibility; the fix layers production-mode driver inclusion on top of the existing logic rather than removing the override.
- The `installDistributionFiles.xml` ANT script remains the assembly orchestrator; the fix updates or augments that script (and its referenced source locations) rather than introducing a parallel packaging system.
- A reusable JDBC driver JAR is available somewhere in the repository or as a curated Maven dependency in the parent POM, so the assembly can reference a known-good path without inventing a new third-party download.