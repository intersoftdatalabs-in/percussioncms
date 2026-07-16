# Implementation Plan: Fix Missing JDBC Drivers in Percussion Distribution Install

**Branch**: `001-fix-jdbc-drivers` | **Date**: 2026-07-10 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/001-fix-jdbc-drivers/spec.md`

## Summary

`modules/perc-distribution-tree` produces an install artifact whose `jetty/base/lib/jdbc/` directory is empty in production builds, breaking first-start repository connectivity. The root cause is in `installDistributionFiles.xml:695-704`, which wraps the only JDBC driver copy in a `<if>${DEVELOPMENT}=true</if>` guard and references a stale `system/Tools/mysql/` filesystem path that no longer exists. The fix replaces the filesystem copy with a Maven-coordinate-driven copy (`maven-dependency-plugin:copy`) sourced from the existing parent-POM-managed driver set (MariaDB, Derby, MSSQL, jTDS, Oracle — the same set already shipped by the DTS analog `delivery-tier-distribution`). The legacy `DEVELOPMENT=true` codepath is preserved for backward compatibility. A new verification script (`scripts/verify-jdbc-drivers.sh`) plus a Maven `verify`-phase wiring enforce FR-007 / SC-005.

## Technical Context

**Language/Version**: Java 21 on `development` (per root `AGENTS.md`); ANT 1.10.x used by the existing `installDistributionFiles.xml` script (no change).
**Primary Dependencies**: Existing parent-POM-managed JDBC coordinates (`org.mariadb.jdbc:mariadb-java-client`, `org.apache.derby:{derby,derbyclient,derbynet}`, `com.microsoft.sqlserver:mssql-jdbc`, `net.sourceforge.jtds:jtds`, `com.oracle.database.jdbc:ojdbc17`). New dependency: none outside what's already in the parent / DTS POMs. MariaDB version will be promoted from `deliverytiersuite/delivery-tier-suite/pom.xml` to root `<dependencyManagement>`.
**Storage**: N/A — no DB schema change. Distribution artifact is filesystem-only.
**Testing**: JUnit 5 + Mockito (project standard); a new shell verification script in `scripts/` per module AGENTS convention. No new test framework.
**Target Platform**: CMS distribution install (Jetty-based). No DTS changes.
**Project Type**: Multi-module CMS mono-repo; this change is scoped to `modules/perc-distribution-tree` (build/POM + ANT script + new verification script).
**Performance Goals**: N/A — build-time change; no runtime perf impact.
**Constraints**:
- Build with `./mvn-env.sh` on JDK 21 (Constitution VII).
- No Spring Boot, no new frameworks (Constitution V).
- Preserve `DEVELOPMENT=true` legacy path verbatim (FR-004).
- Driver versions MUST come from parent-POM `<dependencyManagement>` — no naked version strings (Constitution VII).
- Honor `modules/perc-distribution-tree/AGENTS.md`: scripts under `scripts/` + matching README; update README when assembly flow changes.

**Scale/Scope**:
- One source module changed (`modules/perc-distribution-tree`).
- One parent POM touch to promote `${mariadb.version}` into root `<dependencyManagement>`.
- No runtime module changes.
- New files: `modules/perc-distribution-tree/scripts/verify-jdbc-drivers.sh`, `modules/perc-distribution-tree/scripts/README.md`.

**Owning module(s)**: `modules/perc-distribution-tree` (primary, owns the build artifact). Secondary: `modules/perc-jetty-jars` (existing JDBC-driver packaging module — not modified by this plan, but noted for follow-up).

**AGENTS hierarchy applied**: root `AGENTS.md`; `modules/perc-distribution-tree/AGENTS.md`; `modules/perc-jetty/AGENTS.md` (referenced via embedded system reminder during research).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*
*Source: `.specify/memory/constitution.md` (v2.0.0)*

- [x] **I. Module-First Boundaries**: Owning module identified (`modules/perc-distribution-tree`); no orphan shared code; `modules/perc-jetty-jars` and `modules/perc-jetty` referenced but not modified.
- [x] **II. Evidence Over Invention**: All cited coordinates exist in the parent POM or `deliverytiersuite/delivery-tier-suite/pom.xml`; ANT file path verified by reading `installDistributionFiles.xml:695-704`; no invented APIs.
- [x] **III. Test Discipline**: Verification script (Scenario 1–5 in `quickstart.md`) is the test surface for the assembly change; existing module tests must continue to pass; the `verify-jdbc-drivers.sh` script is wired into the Maven `verify` phase so CI enforces it.
- [x] **IV. Contract & Integration Integrity**: No REST/SOAP/XML-app/`.ppkg`/DB-schema contract changes. Distribution artifact layout is purely additive (new JARs in existing `jdbc/` dir). Documented in `contracts/README.md`.
- [x] **V. Safe Modernization**: No Spring Boot, no new frameworks, no drive-by refactor of unrelated packages; the `DEVELOPMENT=true` legacy path is preserved (no removal).
- [x] **VI. Security by Default**: No authN/Z, XML/XSLT, upload, or crypto surfaces touched. JDBC driver versions are tracked via dependency management; no new ad-hoc downloads.
- [x] **VII. Build, Platform & Dependency Hygiene**: Driver versions come from parent-POM management (Constitution II + VII). New script lives under `scripts/` per module AGENTS. Spotless is not configured in `modules/perc-distribution-tree/pom.xml` (verified — no `<plugin>` entry for `spotless-maven-plugin`); no Spotless gate required.
- [x] **VIII. Documentation & Operability**: `modules/perc-distribution-tree/scripts/README.md` will document the verification script (per module AGENTS rule); `modules/perc-distribution-tree/README.md` will be updated with the JDBC driver set (FR-006). Failure modes are diagnosable from the loud Maven/ANT error messages (FR-003 / SC-004).
- [x] **Complexity Budget**: No constitution violations — every change is local to one module plus a small root POM promotion of `${mariadb.version}` into `<dependencyManagement>` (a hygiene promotion, not a violation). Complexity Tracking table intentionally empty.

**Re-evaluation after Phase 1 design**: All gates still passing; no new violations introduced by the design artifacts.

## Project Structure

### Documentation (this feature)

```text
specs/001-fix-jdbc-drivers/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── README.md
└── tasks.md             # Phase 2 output (NOT created by /speckit.plan)
```

### Source Code (repository modules)

```text
modules/perc-distribution-tree/
├── pom.xml                                                 # add MariaDB dep + copy-dependencies exec
├── src/main/resources/installDistributionFiles.xml         # rewrite JDBC copy block (always-run production copy + preserved DEVELOPMENT block)
├── scripts/
│   ├── verify-jdbc-drivers.sh                              # NEW: verification script (Contracts #2)
│   └── README.md                                           # NEW: script documentation (per module AGENTS)
├── README.md                                               # UPDATE: document bundled JDBC driver set (FR-006)
└── AGENTS.md                                               # UPDATE: reference new script + driver source location

pom.xml (root)                                              # promote ${mariadb.version} into <dependencyManagement>
```

**Structure Decision**: All file changes are local to `modules/perc-distribution-tree` plus a single property promotion in the root POM. This honors Constitution I (module-first) and minimizes blast radius.

## Complexity Tracking

> No constitution violations. Table intentionally empty.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| _none_    | _none_     | _none_                              |