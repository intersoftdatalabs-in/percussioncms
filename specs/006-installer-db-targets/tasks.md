# Tasks: CLI Installer Database Targets for New Installs

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)  
**Branch**: `984-installer-db-targets`  
**Feature directory**: `specs/006-installer-db-targets`  
**Issue**: [#949](https://github.com/intersoftdatalabs-in/percussioncms/issues/949)

**Tests**: Required by project constitution (III) and plan — unit tests for all behavioral resolver/validation changes; connect-validation covered with unit/mocked failure paths.

## Phase 1: Setup

- [x] T001 Identify owning modules and read AGENTS hierarchy: root `AGENTS.md`, `modules/perc-distribution-tree/AGENTS.md`, and skim `modules/perc-ant` for Ant action patterns used by install
- [x] T002 Confirm JDK 21 on branch `984-installer-db-targets` and run baseline `./mvn-env.sh -pl modules/perc-distribution-tree -am test` (note failures unrelated to this feature)
- [x] T003 [P] Re-read research decisions D1–D10 in `specs/006-installer-db-targets/research.md` and contracts in `specs/006-installer-db-targets/contracts/` before coding

## Phase 2: Foundational (Blocking Prerequisites)

**Goal**: Extract testable DB config resolution from `Main`, wire fail-fast exit, preserve existing mysql/sqlserver structured CLI behavior as the base for all stories.  
**Blocks**: All user stories.

- [x] T004 Map existing flow in `modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/Main.java` (`parseArgs`, `resolveDbConfig`, `execJar` `-Dperc.db.*` pass-through) and `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/installRepository.xml` target `repository_properties` (`do.install` guard, mysql/sqlserver/derby branches)
- [x] T005 Assess security surface for this feature: password handling in process args/logs, dbprops file read, no ZipSlip changes; document “do not log PWD” rule for implementers in class-level Javadoc on the new resolver
- [x] T006 Create package-visible/testable resolver class `modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/DbInstallConfigResolver.java` by moving/extracting `resolveDbConfig`, `loadEnvFile`, `getConfigValue`, `parseArgs` DB-related helpers, and `ResolvedDbConfig` / `ParsedArgs` records from `Main.java` without behavior change yet
- [x] T007 Refactor `modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/Main.java` to call `DbInstallConfigResolver`; on `IllegalArgumentException` (and equivalent resolve failures) print a clear message and `System.exit` with non-zero code (do not swallow as soft “install likely failed” success path)
- [x] T008 [P] Add foundational unit tests in `modules/perc-distribution-tree/src/test/java/com/percussion/preinstall/DbInstallConfigResolverTest.java` covering: default Derby when no options; structured `--db.type=mysql` with required fields maps to `perc.db.cms.*`; missing required mysql fields throws; existing sqlserver mapping still works
- [x] T009 Run `./mvn-env.sh -pl modules/perc-distribution-tree -am test -Dtest=DbInstallConfigResolverTest,MainExtractExecutableTest` and fix regressions from the extract

## Phase 3: User Story 1 — Fresh install to enterprise RDBMS via property file (Priority: P1)

**Goal**: Integrator can pass `-Ddbprops` / `--dbprops` (rxrepository.properties format) and get effective CMS repository config for MySQL/MariaDB, SQL Server, and Oracle without manual post-edit (FR-001–FR-004, FR-011).  
**Independent Test**: Resolver unit tests for all three backends + ANT oracle write branch; optional full install per [quickstart.md](quickstart.md) Scenario 7.

### Tests

- [x] T010 [P] [US1] Add unit tests in `modules/perc-distribution-tree/src/test/java/com/percussion/preinstall/DbInstallConfigResolverTest.java` for loading a temp MySQL `dbprops` file → `perc.db.type=mysql` and CMS fields match file keys (`DB_SERVER`, `UID`, etc.)
- [x] T011 [P] [US1] Add unit tests in `modules/perc-distribution-tree/src/test/java/com/percussion/preinstall/DbInstallConfigResolverTest.java` for MSSQL and ORACLE `dbprops` files (backend normalization `MSSQL`→`sqlserver`, `ORACLE`→`oracle`)
- [x] T012 [P] [US1] Add unit test in `modules/perc-distribution-tree/src/test/java/com/percussion/preinstall/DbInstallConfigResolverTest.java` that `dbprops` identity fields take precedence over conflicting `--db.type` CLI values (research D2)

### Implementation

- [x] T013 [US1] Implement `dbprops` load path in `modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/DbInstallConfigResolver.java`: accept `-Ddbprops` system property and CLI `--dbprops` / `dbprops` option; resolve relative path against CWD; map keys per research D3 / `contracts/rxrepository-properties.md`
- [x] T014 [US1] Add Oracle structured + dbprops mapping in `modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/DbInstallConfigResolver.java` (`perc.db.type=oracle`, backend `ORACLE`, default driver name/class aligned with bundled ojdbc / `PSJdbcUtils`)
- [x] T015 [US1] Align composed (non-dbprops) mysql defaults to shipped MariaDB driver class/name in `modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/DbInstallConfigResolver.java` (research D5); keep dbprops free to override class/name
- [x] T016 [US1] Add Oracle branch to `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/installRepository.xml` in target `repository_properties` (mirror mysql/sqlserver `propertyfile` entries for `DB_*` / `UID` / `PWD` / SSL keys when `perc.db.type=oracle`)
- [x] T017 [US1] Verify mysql/sqlserver `propertyfile` blocks in `installRepository.xml` still write when `perc.db.*` is supplied via JVM `-D` (ANT property semantics); fix only if a regression is found
- [x] T018 [US1] Run `./mvn-env.sh -pl modules/perc-distribution-tree -am test -Dtest=DbInstallConfigResolverTest` and ensure US1 tests pass
- [ ] T019 [US1] Commit US1 changes and open a PR scoped to enterprise dbprops + Oracle write-through; pause downstream stories until PR is ready for review
- [ ] T020 [US1] Monitor CI/Kilo checks on the US1 PR; address feedback; reply+resolve review threads per `AGENTS.md` PR Review Comment Resolution
- [ ] T021 [US1] Verify human approval and merge of US1 PR before starting User Story 2

## Phase 4: User Story 2 — Default remains Derby when no alternate target is supplied (Priority: P1)

**Goal**: No override ⇒ Derby default unchanged (FR-005, SC-002).  
**Independent Test**: Resolver returns Derby; default `rxrepository.properties` ship file still DERBY; no forced non-Derby fields.

### Tests

- [x] T022 [P] [US2] Extend `modules/perc-distribution-tree/src/test/java/com/percussion/preinstall/DbInstallConfigResolverTest.java` with explicit cases: empty options → derby; empty/whitespace `db.type` → derby; no `perc.db.cms.backend` forced for derby

### Implementation

- [x] T023 [US2] Confirm `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/rxrepository.properties` remains Derby defaults and derby branch in `installRepository.xml` only stamps SSL keys (no accidental backend overwrite)
- [x] T024 [US2] If any US1 change altered default path, fix `DbInstallConfigResolver` / `Main` so omit-override behavior matches pre-feature Derby default
- [x] T025 [US2] Run targeted tests `./mvn-env.sh -pl modules/perc-distribution-tree -am test -Dtest=DbInstallConfigResolverTest`
- [ ] T026 [US2] Commit US2 changes (or no-op confirmation commit note) and open/update PR; pause for review
- [ ] T027 [US2] Monitor checks, resolve review threads, verify merge before User Story 5

## Phase 5: User Story 5 — Upgrade path unchanged (Priority: P1)

**Goal**: Upgrade must not rewrite repository backend identity; no requirement for `-Ddbprops` (FR-006, SC-005).  
**Independent Test**: Fixture/assert `repository_properties` write logic only when `do.install=true`; document/test that upgrade leaves existing `DB_BACKEND` alone.

### Tests

- [x] T028 [P] [US5] Add a focused unit or resource-level test under `modules/perc-distribution-tree/src/test/java/` that asserts the `repository_properties` fresh-install write is gated by `do.install` (e.g. parse/inspect `installRepository.xml` structure, or a small Ant propertyfile unit if one already exists in perc-ant patterns) — choose the lightest reliable approach that fails if the `do.install` guard is removed
- [x] T029 [P] [US5] Add regression test notes or fixture under `modules/perc-distribution-tree/src/test/resources/com/percussion/preinstall/upgrade-fixture/` with a sample existing `rxrepository.properties` (`DB_BACKEND=MSSQL`) used by T028 or a dedicated assertion helper so non-Derby pre-upgrade config is represented in-repo

### Implementation

- [x] T030 [US5] Audit `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/installRepository.xml` and `install.xml` to ensure no new targets write `DB_BACKEND` outside `${do.install}`; fix if any US1 Oracle/mysql work leaked into upgrade path
- [x] T031 [US5] Ensure preinstall `Main` / resolver does not overwrite install-root repository files before ANT mode detection (resolution only produces JVM props; ANT still owns write under `do.install`)
- [x] T032 [US5] Run `./mvn-env.sh -pl modules/perc-distribution-tree -am test` for affected tests
- [ ] T033 [US5] Commit US5 changes and open PR; pause for review/merge before P2 stories

## Phase 6: User Story 3 — Invalid or incomplete database target fails fast (Priority: P2)

**Goal**: Missing file, incomplete keys, connectivity failure → non-success, clear messages, no password leakage (FR-007–FR-009, FR-012, SC-003–SC-004).  
**Independent Test**: Unit tests for missing/incomplete dbprops; connect-validation action fails on unreachable DB without logging password.

### Tests

- [x] T034 [P] [US3] Add unit tests in `modules/perc-distribution-tree/src/test/java/com/percussion/preinstall/DbInstallConfigResolverTest.java` for missing dbprops path, unreadable path, unknown `DB_BACKEND`, missing required keys; assert messages list key names and do not contain password values
- [x] T035 [P] [US3] Add unit tests in `modules/perc-ant/src/test/java/com/percussion/ant/install/PSValidateRepositoryConnectionTest.java` (or chosen class name) for: missing driver guidance path, connection failure with short timeout, password not present in logged/exception user-facing text

### Implementation

- [x] T036 [US3] Harden static validation messages in `modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/DbInstallConfigResolver.java` (required-field lists per backend; unknown backend allowed-list; path errors)
- [x] T037 [US3] Implement Ant action `modules/perc-ant/src/main/java/com/percussion/ant/install/PSValidateRepositoryConnection.java` that loads install-root `rxconfig/Installer/rxrepository.properties`, attempts JDBC connect with short login timeout using drivers from install `jetty/base/lib/jdbc` (and product conventions), fails the build on error without echoing `PWD`
- [x] T038 [US3] Wire validation into `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/installRepository.xml` for **new install only** after `repository_properties` / password lasagna steps and before heavy schema work that depends on a live connection (pick the earliest safe dependency point consistent with existing target order)
- [x] T039 [US3] Handle missing JDBC driver with actionable fail message (FR-012) in the validate action under `modules/perc-ant/src/main/java/com/percussion/ant/install/PSValidateRepositoryConnection.java`
- [x] T040 [US3] Register/discover the new Ant task the same way sibling `PS*` install tasks are registered (update taskdef/typedef resources under `modules/perc-ant` if required by existing packaging)
- [x] T041 [US3] Run `./mvn-env.sh -pl modules/perc-ant,modules/perc-distribution-tree -am test -Dtest=DbInstallConfigResolverTest,PSValidateRepositoryConnectionTest`
- [ ] T042 [US3] Commit US3 changes and open PR; pause for review/merge

## Phase 7: User Story 4 — Documented property-file contract and samples (Priority: P2)

**Goal**: Integrators can find `-Ddbprops`, supported backends, required keys, and samples for MySQL/MariaDB, SQL Server, Oracle; upgrade vs new install clarified (FR-010, SC-006).  
**Independent Test**: Samples exist under distribution tree; README documents contract; matches [contracts/installer-db-input.md](contracts/installer-db-input.md).

### Tests

- [x] T043 [P] [US4] Add a lightweight test or verify step (JUnit resource existence check under `modules/perc-distribution-tree/src/test/java/com/percussion/preinstall/DbInstallSamplePropertiesTest.java` or assembly assertion) that sample files exist at the packaged paths under `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/samples/`

### Implementation

- [x] T044 [P] [US4] Create `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/samples/rxrepository.mysql.properties` with MYSQL-compatible keys and MariaDB driver class example (placeholder credentials)
- [x] T045 [P] [US4] Create `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/samples/rxrepository.sqlserver.properties` with MSSQL example keys
- [x] T046 [P] [US4] Create `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/samples/rxrepository.oracle.properties` with ORACLE thin example keys
- [x] T047 [US4] Update `modules/perc-distribution-tree/README.md` with: `-Ddbprops` / `--dbprops`, secondary `--db.*`, supported backends, precedence summary, new-install vs upgrade note, pointer to samples and `specs/006-installer-db-targets/contracts/`
- [x] T048 [US4] If installer prints usage/help from `Main.java`, add a short line documenting `--dbprops` in `modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/Main.java` (only if a help path already exists or can be added without scope creep)
- [ ] T049 [US4] Run tests including sample existence check; commit US4 and open PR; pause for review/merge

## Phase 8: Polish & Cross-Cutting Concerns

- [x] T050 [P] Walk [quickstart.md](quickstart.md) scenarios 1–5 against the implementation; fix gaps or update quickstart if commands drifted
- [x] T051 [P] Security pass: grep install/preinstall/validate code paths for password logging (`PWD`, `db.password`, `perc.db.password`) under `modules/perc-distribution-tree` and `modules/perc-ant`; remediate any clear-text leak
- [x] T052 Spotless / format check on touched modules via `./mvn-env.sh -pl modules/perc-distribution-tree,modules/perc-ant -am spotless:check` (or module-equivalent if Spotless not configured — then skip with note)
- [x] T053 Full module test pass: `./mvn-env.sh -pl modules/perc-distribution-tree,modules/perc-ant -am test`
- [x] T054 Update `specs/006-installer-db-targets/plan.md` / this `tasks.md` checkboxes as work completes; ensure issue #949 acceptance criteria are traceable to completed tasks
- [ ] T055 Final PR (or stack restack) for any polish-only commits; resolve all review threads before merge

---

## Dependencies & Execution Order

```text
Phase 1 Setup
    ↓
Phase 2 Foundational  (extract resolver, fail-fast Main, baseline tests)
    ↓
Phase 3 US1 (P1)  dbprops + Oracle + MariaDB default  ← MVP
    ↓
Phase 4 US2 (P1)  Derby default regression
    ↓
Phase 5 US5 (P1)  Upgrade non-regression
    ↓
Phase 6 US3 (P2)  Validation + connectivity fail-fast
    ↓
Phase 7 US4 (P2)  Samples + README
    ↓
Phase 8 Polish
```

| Story | Depends on | Notes |
|-------|------------|-------|
| US1 | Foundational | MVP; delivers issue #949 core value |
| US2 | US1 merged (or same PR only if tiny) | Confirms additive default |
| US5 | US1 (ANT changes exist) | Must stay before calling feature “done” for release |
| US3 | US1 (+ ideally US5 guard intact) | Connect validate needs written props path |
| US4 | US1 (samples match real keys) | Can draft samples in parallel after T013 mapping is stable |

**Story PR policy** (constitution): Implement → commit → PR per story → merge before next story when practical. US2 may be a thin follow-up PR if US1 already preserved Derby.

## Parallel Execution Examples

### Within Foundational

```text
# After T006 class skeleton exists:
T008 unit tests can be drafted in parallel with T007 Main wiring (coordinate on API shape)
```

### Within US1

```text
# Parallel after resolver API supports dbprops:
T010 MySQL dbprops tests
T011 MSSQL/Oracle dbprops tests  
T012 precedence test
# Implementation T013–T015 sequential on same class; T016 ANT Oracle can start once perc.db.type=oracle is defined
```

### Within US4

```text
T044, T045, T046 sample files in parallel
T043 sample existence test in parallel once paths known
T047 README after samples land
```

### Within US3

```text
T034 resolver validation tests || T035 validate-action tests (after action skeleton T037)
```

## Implementation Strategy

### MVP (minimum shippable for #949)

1. Complete Phase 1–2  
2. Complete **User Story 1** (dbprops + MySQL/MSSQL/Oracle effective config write)  
3. Validate with US1 tests + quickstart Scenario 2–3  

### Incremental delivery

| Increment | Stories | Customer-visible outcome |
|-----------|---------|---------------------------|
| MVP | US1 | Non-Derby new install via property file |
| Safety | US2 + US5 | Default + upgrade safe |
| Hardening | US3 | Fail-fast + connect validate |
| Enablement | US4 | Docs/samples |
| Close | Polish | CI green, secrets clean |

### Task format validation

All tasks use: `- [ ]`, sequential `T00N` IDs, optional `[P]`, story labels `[US1]`–`[US5]` on story-phase tasks only, and explicit file paths in descriptions.

## Summary Counts

| Phase | Task IDs | Count |
|-------|----------|-------|
| Setup | T001–T003 | 3 |
| Foundational | T004–T009 | 6 |
| US1 | T010–T021 | 12 |
| US2 | T022–T027 | 6 |
| US5 | T028–T033 | 6 |
| US3 | T034–T042 | 9 |
| US4 | T043–T049 | 7 |
| Polish | T050–T055 | 6 |
| **Total** | T001–T055 | **55** |

| User story | Task count (approx.) |
|------------|----------------------|
| US1 | 12 |
| US2 | 6 |
| US5 | 6 |
| US3 | 9 |
| US4 | 7 |

**Suggested MVP scope**: Phase 1 + Phase 2 + **User Story 1** (T001–T021).

**Parallel opportunities**: Marked with `[P]` on independent test/sample tasks; story PRs remain sequential per constitution checkpoint.
