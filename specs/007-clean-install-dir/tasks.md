# Tasks: Clean Obsolete Install Directories on Upgrade

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)  
**Branch**: `985-clean-install-dir`  
**Feature directory**: `specs/007-clean-install-dir`  
**Issue**: [#1157](https://github.com/intersoftdatalabs-in/percussioncms/issues/1157)

**Tests**: Required by project constitution (III) and plan — unit tests for list/size/decision/delete/eligibility and Main wiring decisions.

## Phase 1: Setup

- [x] T001 Identify owning module and read AGENTS hierarchy: root `AGENTS.md`, `modules/perc-distribution-tree/AGENTS.md`
- [x] T002 Confirm branch `985-clean-install-dir` (or current) is JDK 21 and note baseline for `./mvnw -pl modules/perc-distribution-tree test` (use `-Dai.integrity.skip=true` if integrity hashes block locally)
- [x] T003 [P] Re-read research D1–D8 and contracts in `specs/007-clean-install-dir/contracts/` (CLI + obsolete path list + JBoss bak eligibility)

## Phase 2: Foundational (Blocking Prerequisites)

**Goal**: Create `ObsoleteInstallDirCleaner` skeleton, DTOs, path confinement, and upgrade-detection helpers usable by all stories.  
**Blocks**: All user stories.

- [x] T004 Map upgrade detection and Main lifecycle in `modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/Main.java` (`loadVersionProperties`, extract, `execJar`) and confirm early insertion point **after** version load / install path known and **before** extract/ANT
- [x] T005 Assess security surface: recursive delete under install root only; symlink escape; no logging of unrelated secrets; large-tree IO cost
- [x] T006 Create `modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/ObsoleteInstallDirCleaner.java` with package-visible/public API: candidate record(s), `listEligibleCandidates(installRoot, major, minor)`, `estimateSizeBytes(path)`, `isUnderInstallRoot(root, path)`, empty-result safe defaults
- [x] T007 [P] Add foundational unit tests in `modules/perc-distribution-tree/src/test/java/com/percussion/preinstall/ObsoleteInstallDirCleanerTest.java` for: path confinement rejects escape; empty root → no candidates; size of known fixture files sums correctly
- [x] T008 Run `./mvnw -pl modules/perc-distribution-tree test -Dtest=ObsoleteInstallDirCleanerTest -Dai.integrity.skip=true` and fix foundational regressions

## Phase 3: User Story 1 — Interactive upgrade offers cleanup (Priority: P1)

**Goal**: On upgrade with TTY, when candidates exist and flag is not true, show list + space and delete only on yes (default N). Early in Main.  
**Independent Test**: Unit-test decision + prompt parsing; integration of list/size/delete for accept/decline without full product upgrade.

### Tests

- [x] T009 [P] [US1] Unit tests in `ObsoleteInstallDirCleanerTest.java` for prompt decision: default/empty/no → no delete; yes/YES → delete; candidates empty → no prompt required
- [x] T010 [P] [US1] Unit tests that interactive + candidates + flag false requires prompt path; after simulated yes, `PreInstall` fixture directory is removed

### Implementation

- [x] T011 [US1] Implement human-readable size formatting and prompt text builder in `ObsoleteInstallDirCleaner.java` (folder names + per-path size + total; warn that 8.x does not need these dirs)
- [x] T012 [US1] Implement console prompt via injectable `Console` / `LineReader` abstraction (or package-visible method taking `Supplier<String>` / `Function`) so tests do not need a real TTY
- [x] T013 [US1] Implement best-effort recursive delete with per-path failure capture (warn-and-continue) in `ObsoleteInstallDirCleaner.java`
- [x] T014 [US1] Wire upgrade early cleanup orchestration in `modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/Main.java`: detect upgrade, list candidates, if prompt required then prompt, else skip when no candidates; log outcomes; never abort Main solely on cleanup failure
- [x] T015 [US1] Run cleaner unit tests + `MainExtractExecutableTest` (if still applicable) via `./mvnw -pl modules/perc-distribution-tree test -Dtest=ObsoleteInstallDirCleanerTest,MainExtractExecutableTest -Dai.integrity.skip=true`
- [ ] T016 [US1] Commit US1 changes and open PR; pause for review
- [ ] T017 [US1] Monitor CI/Kilo; address feedback; reply+resolve review threads per `AGENTS.md`
- [ ] T018 [US1] Verify merge before US2 (or continue on same PR stack if team prefers single PR — still complete review gates)

## Phase 4: User Story 2 — Automated flag `--clean-install-dir` (Priority: P1)

**Goal**: Default false retains folders; true deletes without prompt; flag overrides interactive prompt.  
**Independent Test**: Decision matrix unit tests; parseArgs options include `clean-install-dir`.

### Tests

- [x] T019 [P] [US2] Unit tests for flag parsing: absent/false → no delete when non-interactive; true / bare flag → proceed without prompt even if interactive flag set
- [x] T020 [P] [US2] Unit test: non-interactive + flag false + candidates present → retain all paths

### Implementation

- [x] T021 [US2] Parse `--clean-install-dir` from `DbInstallConfigResolver.parseArgs` options in Main (and optional `-Dclean.install.dir` system property if documented); default false
- [x] T022 [US2] Implement `shouldProceedWithoutPrompt` / decision matrix in `ObsoleteInstallDirCleaner.java` per contracts/cleanup-cli.md (flag wins over TTY)
- [x] T023 [US2] Ensure new-install path (no upgrade markers) is no-op even if flag true
- [ ] T024 [US2] Run unit tests; commit US2; PR/review/merge gate as for US1

## Phase 5: User Story 3 — Full MVP path list + JBoss eligibility (Priority: P1)

**Goal**: Candidates include `PreInstall`, `_Percussion_Installation` / `_Percussion_installation`, and conditional `JBossServerXML_BAK`; never live product dirs.  
**Independent Test**: Fixture with all three + `jetty`/`rxconfig`; only eligible MVP paths deleted.

### Tests

- [x] T025 [P] [US3] Unit tests: all three MVP dirs present (eligible) → all three listed; only those deleted on proceed
- [x] T026 [P] [US3] Unit tests: `JBossServerXML_BAK` **not** eligible when major=5, minor=3, and `AppServer` missing; eligible when major≥6 (or 5.4+) if present
- [x] T027 [P] [US3] Unit tests: casing variants for `_Percussion_Installation` / `_Percussion_installation` resolve the existing path once

### Implementation

- [x] T028 [US3] Encode MVP relative names and JBoss eligibility rules from `specs/007-clean-install-dir/contracts/obsolete-paths.md` into `ObsoleteInstallDirCleaner.java`
- [x] T029 [US3] Hard-exclude known live roots (never list `jetty`, `rxconfig`, `ObjectStore`, `Repository`, etc.) in candidate discovery
- [x] T030 [US3] Update or annotate `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/remove_PercussionInstallation.xml` so it does not double-delete or conflict (no-op note or remove TODO that implies incomplete product behavior)
- [ ] T031 [US3] Run full cleaner tests; commit US3; PR/review gate

## Phase 6: User Story 4 — Operator-visible cleanup reporting (Priority: P2)

**Goal**: Installer output clearly shows deleted vs retained candidates and approximate space.  
**Independent Test**: Capture formatted report strings for delete / retain / fail / nothing-to-do paths.

### Tests

- [x] T032 [P] [US4] Unit tests for report formatting: deleted list, retained list, failed list with messages, total size line; no silent empty report when candidates existed

### Implementation

- [x] T033 [US4] Implement `formatCleanupReport(CleanupResult)` (or equivalent) and print via `System.out` / log from Main after cleanup decision
- [x] T034 [US4] When candidates found but flag false and non-interactive, emit advisory that dirs were left and how to enable `--clean-install-dir`
- [ ] T035 [US4] Run tests; commit US4; PR/review gate

## Phase 7: Polish & Cross-Cutting Concerns

- [x] T036 [P] Document flag, prompt, MVP paths, and JBoss eligibility in `modules/perc-distribution-tree/README.md`
- [x] T037 [P] Security pass: symlink escape, path confinement, refuse delete outside install root (tests already cover; re-read cleaner)
- [x] T038 Walk [quickstart.md](quickstart.md) scenarios 1–5 against implementation; fix gaps
- [x] T039 Full module test: `./mvnw -pl modules/perc-distribution-tree test -Dtest=ObsoleteInstallDirCleanerTest,MainExtractExecutableTest,MainInstallExitCodeTest -Dai.integrity.skip=true`
- [ ] T040 Final PR polish commits if needed; resolve all review threads before merge

---

## Dependencies & Execution Order

```text
Phase 1 Setup
    ↓
Phase 2 Foundational  (cleaner skeleton + confinement + size)
    ↓
Phase 3 US1 (P1)  Interactive prompt + early Main wire  ← MVP core UX
    ↓
Phase 4 US2 (P1)  --clean-install-dir flag matrix
    ↓
Phase 5 US3 (P1)  Full path list + JBoss eligibility
    ↓
Phase 6 US4 (P2)  Reporting / operator visibility
    ↓
Phase 7 Polish
```

| Story |         Depends on          |                  Notes                   |
|-------|-----------------------------|------------------------------------------|
| US1   | Foundational                | Prompt + delete + Main early hook        |
| US2   | US1 decision API            | Flag matrix builds on same decision path |
| US3   | Foundational (+ US1 delete) | Eligibility expands candidate list       |
| US4   | US1–US3 results             | Formats CleanupResult                    |

**Note**: US3 can start in parallel with US2 after Foundational if API is stable; PR policy may still sequence merges.

## Parallel Execution Examples

### Foundational

```text
T006 implement skeleton || T007 write confinement/size tests (coordinate on method signatures)
```

### US3

```text
T025 list tests || T026 JBoss eligibility tests || T027 casing tests
```

### Polish

```text
T036 README || T037 security re-read
```

## Implementation Strategy

### MVP (minimum shippable for #1157)

1. Phase 1–2
2. **US1** (interactive PreInstall cleanup early on upgrade)
3. **US2** (flag for automation) — strongly recommended in same release as US1

US3 eligibility + multi-path and US4 reporting complete the clarified MVP set.

### Incremental delivery

| Increment |   Stories    |                      Outcome                      |
|-----------|--------------|---------------------------------------------------|
| Core      | US1 + US2    | Interactive + flag cleanup of detected candidates |
| List      | US3          | Full MVP paths + safe JBoss rule                  |
| Ops       | US4 + Polish | Clear logs/docs                                   |

### Task format validation

All tasks use: `- [ ]`, sequential `T00N`, optional `[P]`, story labels `[US1]`–`[US4]` on story phases only, and explicit file paths.

## Summary Counts

|    Phase     | Task IDs  | Count  |
|--------------|-----------|--------|
| Setup        | T001–T003 | 3      |
| Foundational | T004–T008 | 5      |
| US1          | T009–T018 | 10     |
| US2          | T019–T024 | 6      |
| US3          | T025–T031 | 7      |
| US4          | T032–T035 | 4      |
| Polish       | T036–T040 | 5      |
| **Total**    | T001–T040 | **40** |

| User story | Task count |
|------------|------------|
| US1        | 10         |
| US2        | 6          |
| US3        | 7          |
| US4        | 4          |

**Suggested MVP scope**: Phase 1 + 2 + **US1 + US2** (T001–T024).

**Parallel opportunities**: `[P]` on independent tests and docs; story PRs sequential per constitution when shipping multi-PR.
