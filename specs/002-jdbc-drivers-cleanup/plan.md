# Implementation Plan: JDBC Drivers Packaging Cleanup

**Branch**: `002-jdbc-drivers-cleanup` | **Date**: 2026-07-11 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/002-jdbc-drivers-cleanup/spec.md`

## Summary

This feature is a follow-up to feature 001 (`fix-jdbc-drivers`, merged in PR #1184) that addresses four late code-review findings: a `_jdbc-stage` directory leaks ~15 unintended JARs into the shipped `perc-distribution-tree.jar`; the install/upgrade ANT script's glob delete silently purges integrator-supplied drivers that happen to match bundled-name patterns, contradicting the README's "do not purge this folder" promise; the surrounding ANT comment mis-attributes the loud-failure guarantee to the ANT `<copy>`; and `scripts/README.md` documents a `verify-jdbc-drivers.sh` example that always exits 6 against a real build.

The technical approach, locked in by the spec's clarification session, is: (1) add a `<delete dir="${assembly-directory}/_jdbc-stage"/>` step in `installDistributionFiles.xml` immediately after the staged copy, before assembly runs; (2) replace the install script's glob-based `<delete>` with an exact-filename list pinned in `install.xml` and regenerated each release from the curated `pom.xml` driver set; (3) correct the misleading ANT comment to attribute loud-failure to the Maven `failOnAnyMissingDependency` / `verify-jdbc-drivers` chain; (4) replace the `scripts/README.md` example with an `--expected-driver-glob` invocation using the same globs wired into the Maven `verify` phase; (5) add JUnit 5 + shell-based test coverage satisfying Constitution III.

## Technical Context

**Language/Version**: Java 21 on `development`; ANT 1.10.x for the build script; POSIX shell for verification scripts
**Primary Dependencies**: Maven multi-module; `maven-dependency-plugin` (driver staging); `maven-antrun-plugin` (assembly orchestration); `maven-assembly-plugin` (jar-with-dependencies packaging); `exec-maven-plugin` (verify-phase JDBC driver check)
**Storage**: N/A — no schema or persistent data; this is build-time and install-script behavior
**Testing**: JUnit 5 (per module convention); Mockito where useful; shell-based assertions in POSIX `sh`; module run via `./mvnw` on the target branch JDK
**Target Platform**: CMS distribution on Jetty (install tree) — installer payload is the artifact under change
**Project Type**: Multi-module CMS mono-repo; this change is scoped to one module: `modules/perc-distribution-tree/`
**Performance Goals**: N/A — no runtime path is affected; the installer is a one-shot operation
**Constraints**:
- Branch JDK 21 via `./mvnw` (per root `AGENTS.md` and `modules/perc-distribution-tree/AGENTS.md`)
- No Spring Boot (Constitution V)
- AGENTS hierarchy: root → module; module `AGENTS.md` requires README updates when build/ANT logic changes
- Idempotency for the staging cleanup (FR-007)
**Scale/Scope**:
- Modules touched: `modules/perc-distribution-tree` only
- Files touched (planned): `pom.xml` (test wiring), `src/main/resources/installDistributionFiles.xml` (delete step + comment), `src/main/resources/distribution/rxconfig/Installer/install.xml` (delete list), `scripts/README.md` (example), `scripts/check-no-glob-deletes.sh` (new assertion script), `src/test/java/.../InstallXmlDeleteSetTest.java` (new JUnit 5 test), `README.md` (cross-reference update)
- User roles affected: release engineer (build hygiene), integrator / ops / site admin (driver preservation on upgrade)
- Install/upgrade impact: distribution tree (build artifact); install script behavior (delete policy corrected)
**Owning module(s)**: `modules/perc-distribution-tree/`
**AGENTS hierarchy applied**: `./AGENTS.md`, `./modules/perc-distribution-tree/AGENTS.md`

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*
*Source: `.specify/memory/constitution.md` (v2.0.0)*

- [x] **I. Module-First Boundaries**: Owning module is `modules/perc-distribution-tree/`; Rule Discovery Protocol applied (root AGENTS.md and module AGENTS.md both read). No new shared module introduced. No cross-module coupling.
- [x] **II. Evidence Over Invention**: Every pattern, version, and file path used in this plan was verified against the current `pom.xml`, `installDistributionFiles.xml`, `install.xml`, `scripts/verify-jdbc-drivers.sh`, `scripts/README.md`, `README.md`, and root `pom.xml` driver versions (lines 115, 168, 210, 211, 212, 1443-1495). The actual driver coordinate list (mariadb-java-client, derby, derbyclient, derbynet, mssql-jdbc, jtds, ojdbc17) matches the curated set in `pom.xml:147-181` and the `stage-jdbc-drivers` execution at `pom.xml:496-509`. No invented APIs.
- [x] **III. Test Discipline**: FR-008 mandates a JUnit 5 unit test + a shell-based static assertion wired into the Maven `verify` phase. Tests will be added in the same change. Fail-then-pass strategy noted in tasks.
- [x] **IV. Contract & Integration Integrity**: The installer payload is a public surface for CMS customers on upgrade. The change preserves backward compatibility for integrator-supplied drivers (the install script stops purging them). No REST/SOAP/schema changes. No `.ppkg` change. The change is a behavior correction on the install script that aligns the implementation with the long-standing documentation promise in `README.md:80`.
- [x] **V. Safe Modernization**: No new framework. No Spring Boot. The change is a surgical correction to the install script, build XML, and docs. No drive-by refactor of unrelated code.
- [x] **VI. Security by Default**: No auth/authZ/XML/upload/crypto/redirect/logging surface is touched. No security-relevant code paths are added. The install script is the only privileged path affected, and the change makes it *less* destructive (fewer silent deletes).
- [x] **VII. Build, Platform & Dependency Hygiene**: Build on `development` JDK 21 via `./mvnw`. No new Maven or npm dependencies introduced. Driver coordinates remain as declared in the parent POM (single source of truth). Spotless is not configured for this module (no `spotless` plugin in `pom.xml`); no formatting change required.
- [x] **VIII. Documentation & Operability**: `README.md`, `scripts/README.md`, and the ANT comment are updated. Failures during the new shell assertion produce a clear single-line error message (per the existing pattern in `verify-jdbc-drivers.sh`). No i18n impact (installer is server-side only).
- [x] **Complexity Budget**: No principle violations. No new top-level modules, no new frameworks, no breaking contracts, no parallel implementations.
- [x] **Governance**: Plan will re-check this list after Phase 1 design. AGENTS.md remains the runtime guide.

## Project Structure

### Documentation (this feature)

```text
specs/002-jdbc-drivers-cleanup/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (empty for this build-script-only feature; see Phase 1)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository modules)

```text
modules/perc-distribution-tree/
├── pom.xml                                                         # wire shell assertion into verify phase
├── README.md                                                       # cross-reference update (if needed)
├── scripts/
│   ├── README.md                                                   # replace --expected-driver-set example with --expected-driver-glob
│   ├── verify-jdbc-drivers.sh                                      # (unchanged) — already supports --expected-driver-glob
│   └── check-no-glob-deletes.sh                                    # NEW — static assertion against install.xml
├── src/main/resources/
│   ├── installDistributionFiles.xml                                # add <delete dir="${assembly-directory}/_jdbc-stage"/>; correct comment
│   └── distribution/rxconfig/Installer/
│       └── install.xml                                             # replace glob delete with exact-filename list
└── src/test/java/
    └── com/percussion/distribution/jdbc/
        └── InstallXmlDeleteSetTest.java                            # NEW — JUnit 5 unit test
```

**Structure Decision**: All edits live in `modules/perc-distribution-tree/`. The change is intentionally narrow — it is a follow-up bug fix to a 3-file change (feature 001, PR #1184). The new JUnit test follows the module's existing test source root (`src/test/java`); the new shell assertion lives under the existing `scripts/` directory per module `AGENTS.md`. No new Maven module, no new shared library, no public REST/SOAP/API surface change.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|--------------------------------------|
| *(none)*  |            |                                      |

No Constitution violations. No complexity budget entries required.
