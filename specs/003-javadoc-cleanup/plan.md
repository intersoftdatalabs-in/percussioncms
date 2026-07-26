# Implementation Plan: Javadoc Cleanup for Content Explorer Module

**Branch**: `003-javadoc-cleanup` | **Date**: 2026-07-11 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/003-javadoc-cleanup/spec.md`

## Summary

Eliminate the javadoc errors and warnings that the `maven-javadoc-plugin` (3.12.0 from
parent POM, JDK 21) currently emits for the `perc-content-explorer` module
(`modules/DesktopContentExplorer/`). Baseline captured on 2026-07-11 shows **44
errors / 100 warnings summary / 198 raw warning lines**, all from this module's own
sources. Fix root causes by repairing Javadoc comments and HTML/Javadoc tags in the
module's Java files, with last-resort `@SuppressWarnings("javadoc")` only where the
module already follows that pattern. No signature, visibility, dependency, or behavior
changes. Targets: zero errors, ≥ 80% warning reduction versus the captured baseline.

## Technical Context

**Language/Version**: Java 21 on `development` (matches root `AGENTS.md`)
**Primary Dependencies**: Existing module stack only — no new dependencies. Module
already depends on `com.percussion:perc-system`, `com.percussion:utils`,
`com.percussion:webservices`, `com.percussion:perc-security-utils`,
`com.percussion:perc-i18n`, `com.percussion:perc-xml-security`, and
`org.openjfx:javafx-{controls,graphics,base,web,swing}` 17 (see
`modules/DesktopContentExplorer/pom.xml`).
**Storage**: N/A — no persistence touched.
**Testing**: JUnit 5 / Mockito already configured at the parent POM level. Per FR-009,
**no new tests** are added for this docs-only feature; existing module tests must
continue to pass. Verification is performed via the javadoc build itself plus
`./mvn-env.sh -pl modules/DesktopContentExplorer test`.
**Target Platform**: CMS desktop client (Swing + JavaFX). Built via `./mvn-env.sh`
(jdk21 env). No runtime platform change.
**Project Type**: Multi-module CMS mono-repo; this work is scoped to a single legacy
desktop client module (`perc-content-explorer`).
**Performance Goals**: N/A — this is a documentation/quality change. The "build slowing
down" wording in the user request refers to javadoc-induced build noise, not to wall
time.
**Constraints**:
- Branch is `development`; JDK 21 via `./mvn-env.sh`. Never run plain `mvn`.
- No Spring Boot (Constitution V). No new modules / new plugins (Constitution VII).
- No public-type-signature or visibility changes anywhere in the module (FR-004).
- No new dependencies, no parent POM edits (FR-005).
- Javadoc plugin configuration stays as inherited from `pom.xml:2636-2653`
(`maven-javadoc-plugin` 3.12.0, `doclint=all`, `failOnError=false`,
`failOnWarnings=false`).
**Scale/Scope**: Single module — `modules/DesktopContentExplorer/` only. ~70 Java
files implicated by the baseline; ~242 issue lines total.
**Owning module(s)**: `modules/DesktopContentExplorer/` (`com.percussion:perc-content-explorer`).
**AGENTS hierarchy applied**:
- Root `AGENTS.md` (always-on governance: mono-repo, JDK branch matrix, rule discovery
protocol, Module List, JUnit 5 + Mockito + Spotless).
- Module `AGENTS.md` / `AGENTS.local.md`: **none present** at
`modules/DesktopContentExplorer/` (Rule Discovery Protocol confirms by `ls`); root
rules apply directly.

## Constitution Check

*Source: `.specify/memory/constitution.md` v2.0.0*

- [x] **I. Module-First Boundaries**: owning module is `modules/DesktopContentExplorer/`
  (confirmed via spec's Module Scope section). Rule Discovery Protocol run; no local
  override files exist, so root `AGENTS.md` is the operative governance. No new shared
  modules created.
- [x] **II. Evidence Over Invention**: every technique used (`doclint=all`,
  `maven-javadoc-plugin` 3.12.0, baseline command) is documented in the parent POM at
  `pom.xml:2636-2653` and was reproduced empirically today. No third-party APIs
  invented; no new libraries.
- [x] **III. Test Discipline**: this feature is documentation-only. Per FR-009 and
  Constitution III, no new behavioral tests are warranted for doc-comment changes;
  existing module tests must continue to pass (verified via the quickstart `test` step).
- [x] **IV. Contract & Integration Integrity**: no REST/SOAP/package/schema surfaces
  touched. The javadoc build contract is inherited from the parent POM unchanged;
  any future `failOnWarnings=true` decision is explicitly out of scope. The module's
  public Java API is unchanged (FR-004 + SC-004).
- [x] **V. Safe Modernization**: no drive-by refactors; no new frameworks; no Spring
  Boot. Scope strictly limited to comment/HTML repairs in the one module. Cross-module
  `{@link}` references are corrected within the existing module's comments only.
- [x] **VI. Security by Default**: no authn/authz, XML parsing, file upload,
  cryptography, redirect, or sensitive logging surfaces touched.
- [x] **VII. Build, Platform & Dependency Hygiene**: JDK 21 via `./mvn-env.sh` on
  `development`; no new Maven or npm dependencies; no plugin overrides; Spotless is not
  configured at the parent level for this module, so no Spotless step is required.
- [x] **VIII. Documentation & Operability**: this is itself a documentation-quality
  improvement (Constitution VIII in action). README/scripts unchanged; no new doc
  files created (YAGNI). The spec's checklist + baseline + post-cleanup artifacts are
  the durable record.
- [x] **Complexity Budget**: no principle violations; Complexity Tracking table is
  therefore empty below.
- [x] **Governance**: re-check after Phase 1 (see "Re-check after Phase 1" below).

### Re-check after Phase 1

- [x] Same eight items re-evaluated against the Phase 1 design artifacts
  (`data-model.md`, `contracts/README.md`, `quickstart.md`). No item changed status.
- The "Build, Platform & Dependency Hygiene" item was reconfirmed against the parent
  POM rather than against the spec's prose to catch the plugin-version drift risk.

## Project Structure

### Documentation (this feature)

```text
specs/003-javadoc-cleanup/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   └── README.md
├── checklists/
│   └── requirements.md  # from /speckit.specify
├── baseline-raw.txt     # captured javadoc output, pre-cleanup (verification artifact)
├── post-cleanup.txt     # captured javadoc output, post-cleanup (verification artifact; produced by implementation phase)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository modules)

```text
modules/DesktopContentExplorer/
├── pom.xml                                             # UNCHANGED
├── README.md                                           # UNCHANGED unless content-explorer currently documents javadoc conventions (verify during implementation)
├── refactored-java11-packages.txt                      # UNCHANGED
└── src/
    ├── main/
    │   ├── java/com/percussion/                        # CHANGED ONLY (comment/HTML repairs in .java files)
    │   │   ├── cx/                                     # ~70 files implicated by baseline
    │   │   ├── ...
    │   │   ├── ServerConnection.java
    │   │   └── ...
    │   └── resources/                                  # UNCHANGED
    └── test/                                           # UNCHANGED (FR-009: no test code changes)
```

**Structure Decision**: All edits live inside `modules/DesktopContentExplorer/src/main/java/`.
The pom, README, resources, test sources, and any other module under `modules/` are
out of scope. No new directories, no new artifacts outside the spec folder.

## Complexity Tracking

> No Constitution Check violations. Table intentionally empty.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|--------------------------------------|
| _none_    | _n/a_      | _n/a_                                |

