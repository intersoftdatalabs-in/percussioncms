# Implementation Plan: CLI Installer Database Targets for New Installs

**Branch**: `984-installer-db-targets` | **Date**: 2026-07-15 | **Spec**: [spec.md](spec.md)  
**Input**: Feature specification from `specs/006-installer-db-targets/spec.md` (GitHub [#949](https://github.com/intersoftdatalabs-in/percussioncms/issues/949))  
**Feature directory**: `specs/006-installer-db-targets` (see `.specify/feature.json`; branch number differs from sequential spec id)

## Summary

New CLI installs of Percussion CMS still default to embedded Derby and lack a complete, documented way to target MySQL/MariaDB, SQL Server, or Oracle using a repository properties file. Partial support already exists: `com.percussion.preinstall.Main.resolveDbConfig` maps structured `--db.*` / env inputs to `perc.db.*` system properties for **mysql** and **sqlserver**, and `installRepository.xml` `repository_properties` writes those into install-root `rxrepository.properties` when `do.install=true`.

This plan **completes** that path for issue #949 by: (1) adding `-Ddbprops` / `--dbprops` input in `rxrepository.properties` format; (2) adding **Oracle**; (3) aligning default MySQL-compatible driver class with shipped MariaDB JDBC; (4) fail-fast field validation and connectivity validation on new install; (5) samples + README; (6) unit tests with extracted resolver. Upgrade mode remains untouched for backend identity.

## Technical Context

- **Language/Version**: Java 21 on `development`; ANT 1.10.x install scripts (existing)
- **Owning Module(s)**: `modules/perc-distribution-tree` (primary — preinstall `Main`, distribution installer XML, samples, README); `modules/perc-ant` (secondary — optional connect-validation action if not kept inside distribution-only ANT/Java)
- **AGENTS Hierarchy**: root `AGENTS.md`; `modules/perc-distribution-tree/AGENTS.md`
- **Dependencies & Storage**: Existing JDBC drivers in distribution (`jetty/base/lib/jdbc` — see `001-fix-jdbc-drivers`); no new third-party libraries; no TableFactory schema format change; uses existing `rxrepository.properties` keys and `PSJdbcUtils` backend labels
- **Testing**: JUnit 5 (+ Mockito as needed) under `modules/perc-distribution-tree` (and `perc-ant` if new action lives there); `./mvn-env.sh`; no new test framework
- **Scale/Impact**: Install-time only; integrators/automation; new-install config write + validation; upgrades must not regress; DTS full write-through out of scope

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*  
*Source: `.specify/memory/constitution.md` (v2.3.0)*

- [x] **I. Module-First Boundaries**: Owning modules identified; shared JDBC constants reused from `modules/utils` (`PSJdbcUtils`); no new org-only module.
- [x] **II. Evidence Over Invention**: Plan cites existing `Main.resolveDbConfig`, `installRepository.xml` `repository_properties`, `rxrepository.properties`, `PSJdbcUtils`, table-factory `-dbprops` precedent; no invented APIs.
- [x] **III. Test Discipline**: Unit tests planned for resolver, validation, backend mapping, precedence; connect-validation testable with mock/invalid endpoint; behavioral changes covered (see [quickstart.md](quickstart.md)).
- [x] **IV. Contract & Integration Integrity**: No REST/SOAP/XML-app/`.ppkg` breaks; repository property **key names** preserved; upgrade backend identity preserved; additive CLI contract only.
- [x] **V. Safe Modernization**: No Spring Boot; no drive-by rewrite of extract/upgrade helpers; extract only DB resolution for testability.
- [x] **VI. Security by Default**: No password logging; path validation remains for zip extract (existing); dbprops path handled as file read with clear errors; no secret leakage in messages.
- [x] **VII. Build & Dependency Hygiene**: JDK 21 via `./mvn-env.sh`; no naked new dependency versions; scripts/docs under module conventions.
- [x] **VIII. Documentation & Operability**: README + shipped samples; fail-fast messages actionable; note new vs upgrade.
- [x] **IX. PR Review Comment Resolution**: Apply when PRs land (process obligation).
- [x] **Complexity Budget**: No constitution violations. Optional residual: password via `-D` special-character risk mitigated per research D6/D10 notes (temp props or tests).

**Re-evaluation after Phase 1 design**: Gates still pass. Design keeps CMS-only scope, reuses ANT write path, and isolates Oracle + dbprops as additive branches. Complexity Tracking empty.

## Project Structure

### Documentation (this feature)

```text
specs/006-installer-db-targets/
├── plan.md                 # This file
├── research.md             # Phase 0
├── data-model.md           # Phase 1
├── quickstart.md           # Phase 1
├── contracts/
│   ├── README.md
│   ├── installer-db-input.md
│   └── rxrepository-properties.md
├── checklists/
│   └── requirements.md
└── tasks.md                # Phase 2 — created by /speckit-tasks (not this command)
```

### Source Code (affected paths)

```text
modules/perc-distribution-tree/
├── src/main/java/com/percussion/preinstall/
│   ├── Main.java                    # Wire resolver; fail fast on resolve errors; non-zero exit
│   └── DbInstallConfigResolver.java # NEW (name flexible): parse/load/map/validate pure logic
├── src/test/java/com/percussion/preinstall/
│   └── DbInstallConfigResolverTest.java  # NEW
├── src/main/resources/distribution/rxconfig/Installer/
│   ├── installRepository.xml        # Oracle branch; optional validateRepositoryConnection target hook
│   ├── rxrepository.properties      # defaults unchanged (Derby)
│   └── samples/                     # NEW: mysql, sqlserver, oracle sample property files
├── README.md                        # Document -Ddbprops / --dbprops / backends / upgrade note
└── (optional) scripts/              # Only if a standalone verify helper is warranted

modules/perc-ant/                    # OPTIONAL if connect validation is a reusable Ant action
├── src/main/java/com/percussion/ant/install/
│   └── PSValidateRepositoryConnection.java  # NEW (or equivalent)
└── src/test/java/...                # Unit tests for the action

modules/utils/                       # READ-ONLY reference: PSJdbcUtils backend constants
```

**Structure Decision**: Prefer keeping resolution in `perc-distribution-tree` preinstall package (owns CLI entry). Prefer connectivity validation as `perc-ant` action invoked from `installRepository.xml` so JDBC drivers on the install classpath are available. Avoid changing DTS modules.

## Implementation approach (design summary)

1. **Extract** `resolveDbConfig` / helpers from `Main` into a testable class; support:
   - `-Ddbprops` and `--dbprops` → load `Properties`, map per [research.md](research.md) D3
   - Existing structured CLI/env path (mysql/sqlserver + **oracle**)
   - Precedence D2
2. **Oracle**: Java mapping + ANT `propertyfile` branch for `perc.db.type=oracle`
3. **MariaDB driver default** for composed mysql path (research D5); dbprops may override class/name
4. **Validation**: static required fields at resolve time; **connect** after properties written (research D6)
5. **main error handling**: on `IllegalArgumentException` / validation failure, print message and `System.exit` non-zero (today many failures may be swallowed by broad catch)
6. **Samples + README** (FR-010)
7. **Tests** per quickstart scenarios 1–5 minimum

## Complexity Tracking

*(No constitution exceptions.)*

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|--------------------------------------|
| _none_    | _none_     | _none_                               |

## Phase outputs

| Phase |                                         Artifact                                         |                          Status                           |
|-------|------------------------------------------------------------------------------------------|-----------------------------------------------------------|
| 0     | [research.md](research.md)                                                               | Complete                                                  |
| 1     | [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md) | Complete                                                  |
| 2     | `tasks.md`                                                                               | **Not** created by `/speckit-plan` — use `/speckit-tasks` |

## Next command

```text
/speckit-tasks
```

