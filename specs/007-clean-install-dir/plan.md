# Implementation Plan: Clean Obsolete Install Directories on Upgrade

**Branch**: `985-clean-install-dir` | **Date**: 2026-07-16 | **Spec**: [spec.md](spec.md)  
**Input**: Feature specification from `specs/007-clean-install-dir/spec.md` ([#1157](https://github.com/intersoftdatalabs-in/percussioncms/issues/1157))  
**Feature directory**: `specs/007-clean-install-dir`

## Summary

Long-lived CMS installs retain obsolete directories (especially multi-GB `PreInstall` trees from the old installer). On **upgrade**, after detecting an existing product install and **before** ANT upgrade work, the preinstall process optionally removes a curated MVP set of paths (`PreInstall`, `_Percussion_Installation` / casing variant, and conditionally `JBossServerXML_BAK`). Deletion runs only when the operator confirms interactively or passes `--clean-install-dir` (default **false**). Failures warn and continue. Implementation is a testable Java helper wired from `Main`, reusing existing `parseArgs` style.

## Technical Context

- **Language/Version**: Java 21 on `development`
- **Owning Module(s)**: `modules/perc-distribution-tree` (`com.percussion.preinstall`)
- **AGENTS Hierarchy**: root `AGENTS.md`, `modules/perc-distribution-tree/AGENTS.md`
- **Dependencies & Storage**: JDK NIO filesystem only; no new third-party libs; no DB schema
- **Testing**: JUnit 5 under `modules/perc-distribution-tree/src/test/java/com/percussion/preinstall/`
- **Scale/Impact**: Upgrade-time disk ops; operators/automation; can free tens of GB; warn-and-continue on errors

## Constitution Check

*Source: `.specify/memory/constitution.md`*

- [x] **I. Module-First Boundaries**: Single owning module; no new top-level package sprawl
- [x] **II. Evidence Over Invention**: Paths and upgrade detection grounded in `install.xml`, `remove_PercussionInstallation.xml`, `Main` Version.properties / JBoss bak usage
- [x] **III. Test Discipline**: Unit tests for list/size/decision/delete/eligibility required
- [x] **IV. Contract & Integration Integrity**: CLI additive; no REST/schema breaks; upgrade still runs if cleanup fails
- [x] **V. Safe Modernization**: No Spring Boot; localized to preinstall
- [x] **VI. Security by Default**: Path confinement under install root; no symlink escape deletes
- [x] **VII. Build & Dependency Hygiene**: JDK 21 / `./mvn-env.sh`; no new deps
- [x] **VIII. Documentation & Operability**: README + installer output
- [x] **IX. PR Review Comment Resolution**: Process when PR opens
- [x] **Complexity Budget**: No violations

**Post-design re-check**: Gates still pass. JBoss bak eligibility is the main safety constraint (research D3).

## Project Structure

### Documentation (this feature)

```text
specs/007-clean-install-dir/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── README.md
│   ├── cleanup-cli.md
│   └── obsolete-paths.md
├── checklists/requirements.md
├── spec.md
└── tasks.md                 # via /speckit-tasks
```

### Source Code (affected paths)

```text
modules/perc-distribution-tree/
├── src/main/java/com/percussion/preinstall/
│   ├── Main.java                      # Wire early cleanup on upgrade
│   └── ObsoleteInstallDirCleaner.java # NEW: list, size, decide, delete
├── src/test/java/com/percussion/preinstall/
│   └── ObsoleteInstallDirCleanerTest.java  # NEW
├── README.md                          # Document flag + path list
└── src/main/resources/distribution/rxconfig/Installer/
    └── remove_PercussionInstallation.xml  # Optional: note superseded by Java cleaner or call no-op
```

**Structure Decision**: Prefer pure Java in preinstall for interactive UX and unit tests; keep ANT stub from conflicting (document or no-op).

## Implementation approach

1. **`ObsoleteInstallDirCleaner`**: candidates, sizes, eligibility, confined delete, result DTO.
2. **CLI**: read `clean-install-dir` from `DbInstallConfigResolver.parseArgs` options map (and optional system property).
3. **`Main.main`**: after `loadVersionProperties` / upgrade known, if upgrade → run cleaner before extract/ANT; log results; never fail install solely on cleanup errors.
4. **Prompt**: `System.console().readLine` when required; default N.
5. **Tests**: scenarios in [quickstart.md](quickstart.md).
6. **Docs**: README + contracts path list.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| _none_ | | |

## Phase outputs

| Phase | Artifact | Status |
|-------|----------|--------|
| 0 | [research.md](research.md) | Complete |
| 1 | [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md) | Complete |
| 2 | `tasks.md` | Use `/speckit-tasks` |

## Next command

```text
/speckit-tasks
```
