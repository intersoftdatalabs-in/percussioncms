---
description: "Task list for fixing missing JDBC drivers in the Percussion distribution install"
---

# Tasks: Fix Missing JDBC Drivers in Percussion Distribution Install

**Input**: Design documents from `/specs/001-fix-jdbc-drivers/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: REQUIRED for every behavioral code change (Constitution III — Test Discipline).
Each user story MUST include test tasks. Prefer fail-then-pass (write/adjust tests first).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact mono-repo file paths in descriptions (module + `src/main` / `src/test`)

## Path Conventions

- **Mono-repo modules** (Percussion CMS): use real module roots
  - Primary owning module: `modules/perc-distribution-tree/`
  - Secondary (POM hygiene promotion): `pom.xml` (root)
  - No runtime modules touched (no `system/`, `rest/`, `projects/sitemanage/`, `WebUI/`, DTS)
- Paths in tasks below are real paths to be edited
- Run builds with `./mvn-env.sh` (or `mvn-env.bat`) against the branch JDK (21 on `development`)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirm ownership, AGENTS rules, and build baseline for touched modules

- [x] T001 Identify owning module path(s) and read AGENTS hierarchy in `AGENTS.md`, `modules/perc-distribution-tree/AGENTS.md`, and `modules/perc-jetty/AGENTS.md`
- [x] T002 Confirm branch JDK 21 and verify `./mvn-env.sh -pl modules/perc-distribution-tree -am test-compile` baseline succeeds on `001-fix-jdbc-drivers`
- [x] T003 [P] Note that `modules/perc-distribution-tree/pom.xml` has no Spotless plugin (verified during planning); no Spotless gate required for this change
- [x] T004 [P] Re-read `modules/perc-distribution-tree/src/main/resources/installDistributionFiles.xml:695-704` to confirm current JDBC copy block content before editing

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Promote MariaDB version into root POM management and stage curated JDBC drivers so every later user story phase can rely on them being present

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T005 [P] Promote `${mariadb.version}` (`3.5.7`) from `deliverytiersuite/delivery-tier-suite/pom.xml:45, 309-311` into a new `<dependency>` entry under root `pom.xml` `<dependencyManagement>` (alongside `ojdbc17`, `jtds`, `mssql-jdbc`, `sqlite-jdbc`) — **BLOCKING for T007**
- [x] T006 [P] Verify the existing driver coordinates in root `pom.xml` (`com.oracle.database.jdbc:ojdbc17:23.26.0.0.0`, `net.sourceforge.jtds:jtds`, `com.microsoft.sqlserver:mssql-jdbc`, `org.xerial:sqlite-jdbc`) and confirm `org.apache.derby:*` versions are managed (if not, add them with the same versions used in `modules/perc-jetty-jars/pom.xml`); any additions here are also **BLOCKING for T007**
- [x] T007 [P] Add new `<dependency>` entries to `modules/perc-distribution-tree/pom.xml` for `org.mariadb.jdbc:mariadb-java-client`, `org.apache.derby:derby`, `org.apache.derby:derbyclient`, `org.apache.derby:derbynet`, `com.microsoft.sqlserver:mssql-jdbc`, `net.sourceforge.jtds:jtds`, `com.oracle.database.jdbc:ojdbc17` (all `scope=provided` since they are build-time-only copies, not imported by preinstall Java code) — **depends on T005 + T006 (version management) being complete first**
- [x] T008 Add a new `maven-dependency-plugin` `<execution>` (id `stage-jdbc-drivers`, phase `generate-resources`) to `modules/perc-distribution-tree/pom.xml` that uses `<goal>copy-dependencies</goal>` with `<includeScope>provided</includeScope>`, `<outputDirectory>${assembly-directory}/_jdbc-stage</outputDirectory>`, `<failOnAnyMissingDependency>true</failOnAnyMissingDependency>` — satisfies FR-003 / SC-004
- [x] T009 Create the new verification script directory and its README at `modules/perc-distribution-tree/scripts/README.md` documenting the `verify-jdbc-drivers.sh` script (per module AGENTS rule: scripts go under `scripts/` with a README)

**Checkpoint**: Foundation ready — `${assembly-directory}/_jdbc-stage/` will be populated by `mvn generate-resources` and user story implementation can now begin.

---

## Phase 3: User Story 1 - Integrator performs a clean install (Priority: P1) 🎯 MVP

**Goal**: A production build of `modules/perc-distribution-tree` ships a non-empty, valid `jetty/base/lib/jdbc/` directory containing the MariaDB/MySQL driver so the CMS can bootstrap its default repository connection on first start.

**Independent Test**: Run `./mvn-env.sh -pl modules/perc-distribution-tree -am clean install` with no `DEVELOPMENT` override; unpack `modules/perc-distribution-tree/target/perc-distribution-tree.jar`; assert `jetty/base/lib/jdbc/` exists and contains a non-empty `mariadb-connector.jar` (or `mariadb-java-client-*.jar`).

### Tests for User Story 1 (REQUIRED) ⚠️

> **NOTE: Write or update these tests FIRST; ensure they FAIL before implementation, then PASS**

- [x] T010 [P] [US1] Add a new shell test `modules/perc-distribution-tree/scripts/verify-jdbc-drivers.sh` that: takes `--artifact <path>` (default `target/perc-distribution-tree.jar`), unpacks into a scratch dir, asserts `jetty/base/lib/jdbc/` exists (exit code 2 if missing), asserts ≥ 1 `*.jar` present (exit 2 if empty), asserts every JAR `size > 0` (exit 3 if any zero-byte), asserts every JAR passes `unzip -t` (exit 4 if any invalid). Exit 0 only when all checks pass. — this is the US1 acceptance test
- [x] T011 [P] [US1] Manual validation per `specs/001-fix-jdbc-drivers/quickstart.md` Scenarios 1–3 (default-driver ships, directory exists and non-empty, no zero-byte stubs); record pass output in PR description

### Implementation for User Story 1

- [x] T012 [US1] Edit `modules/perc-distribution-tree/src/main/resources/installDistributionFiles.xml` to replace the lines 695–704 `DEVELOPMENT=true`-gated `<copy>` with an *unconditional* production copy block: always run `<mkdir dir="${assembly-directory}/jetty/base/lib/jdbc/"/>` and `<copy todir="${assembly-directory}/jetty/base/lib/jdbc/">` of every file in `${assembly-directory}/_jdbc-stage/` (use `<fileset>`); ANT `<copy>` will fail by default if a source file is missing, which combined with `failOnAnyMissingDependency=true` (T008) gives loud failure (FR-001, FR-002, FR-003)
- [x] T013 [US1] Edit `modules/perc-distribution-tree/src/main/resources/installDistributionFiles.xml` to preserve the legacy `DEVELOPMENT=true` block (now retitled in a comment as a legacy override that adds an extra development driver on top of the production set); ensure it does NOT remove the production copy added in T012 (FR-004)
- [x] T014 [US1] Wire the verification script into Maven `verify` phase in `modules/perc-distribution-tree/pom.xml` via `exec-maven-plugin` (id `verify-jdbc-drivers`, phase `verify`, executable `./scripts/verify-jdbc-drivers.sh`, argument `--artifact ${project.build.directory}/perc-distribution-tree.jar`) — satisfies FR-007 / SC-005
- [x] T015 [US1] Run `./mvn-env.sh -pl modules/perc-distribution-tree -am clean verify` end-to-end and confirm US1 acceptance criteria pass (SC-001, SC-002, SC-005)
- [x] T016 [US1] Update `modules/perc-distribution-tree/README.md` to document the JDBC driver set that ships in `jetty/base/lib/jdbc/` (MariaDB, Derby, MSSQL, jTDS, Oracle) and explain the integrator extension point — satisfies FR-006
- [x] T017 [US1] Update `modules/perc-distribution-tree/AGENTS.md` to reference the new verification script and the new driver source location (`_jdbc-stage/` populated by `maven-dependency-plugin:copy-dependencies`)

**Checkpoint**: At this point, US1 is fully functional — a clean production build ships a non-empty `jdbc/` folder with a working MariaDB driver.

---

## Phase 4: User Story 2 - Integrator swaps to a different supported database (Priority: P2)

**Goal**: The `jetty/base/lib/jdbc/` folder is the documented extension point for swapping drivers; integrators can add a vendor JDBC driver on top of the bundled set and have it recognized; bundled drivers are valid (not stubs).

**Independent Test**: After a US1 build, manually place an additional JDBC driver JAR into the unpacked distribution's `jetty/base/lib/jdbc/` and verify (a) it is preserved through any re-assembly steps that touch the install tree, and (b) the existing bundled drivers are non-zero-byte and openable with `unzip -t`.

### Tests for User Story 2 (REQUIRED) ⚠️

- [x] T018 [P] [US2] Extend `modules/perc-distribution-tree/scripts/verify-jdbc-drivers.sh` (built in T010) with a `--strict-jar-validity` mode that runs `unzip -t` against every shipped JAR and reports any non-archive; reuse the same exit-code 4 path on failure (covers US2 AC2: shipped JARs are valid, not stubs)
- [x] T019 [P] [US2] Document the "drop an extra driver into `jetty/base/lib/jdbc/`" workflow in `modules/perc-distribution-tree/README.md` so integrators know the folder is additive and the install scripts (`install.xml`, `installServer.xml`, `installRepository.xml`) do not purge it

### Implementation for User Story 2

- [x] T020 [US2] Confirm (read-only, no edit expected) that `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/install.xml`, `installServer.xml`, and `installRepository.xml` do not delete files under `jetty/base/lib/jdbc/` during install; if they do, document the behavior in the new README section and call it out in the PR description
- [x] T021 [US2] Run `specs/001-fix-jdbc-drivers/quickstart.md` Scenario 3 (no zero-byte stubs) and Scenario 4 (loud failure when a coordinate is broken) end-to-end against the US1 build to demonstrate the bundled drivers are real and the failure path works
- [x] T022 [US2] Run `./mvn-env.sh -pl modules/perc-distribution-tree -am verify` and confirm US2 acceptance criteria pass (SC-002)

**Checkpoint**: US1 and US2 are independently functional — bundled drivers are valid and the `jdbc/` folder is a documented extension point.

---

## Phase 5: User Story 3 - Build / CI verifies driver inclusion (Priority: P3)

**Goal**: An automated check in the build fails the build when the assembled distribution does not ship the expected non-empty JDBC driver set, preventing silent regression.

**Independent Test**: Temporarily break a driver coordinate (rename in pom); run `./mvn-env.sh -pl modules/perc-distribution-tree -am verify`; confirm the build exits non-zero with a clear message naming the missing driver; revert and confirm green.

### Tests for User Story 3 (REQUIRED) ⚠️

- [x] T023 [P] [US3] Extend `modules/perc-distribution-tree/scripts/verify-jdbc-drivers.sh` to also accept an `--expected-driver-set <comma-separated-names>` option that asserts every named driver is present; exit code 6 if any expected driver is missing — covers US3 AC1 (configured set matches what's shipped)

### Implementation for User Story 3

- [x] T024 [US3] In `modules/perc-distribution-tree/pom.xml`, hard-code the expected driver set in the `exec-maven-plugin` invocation from T014 by passing `--expected-driver-set mariadb-connector.jar,derby.jar,derby-client.jar,derbynet.jar,mssql-connector.jar,jtds.jar,ojdbc17.jar` (matches `contracts/README.md` Contract 3 staged filenames)
- [x] T025 [US3] Run `specs/001-fix-jdbc-drivers/quickstart.md` Scenario 4 end-to-end (intentional coordinate break → expected non-zero exit with actionable message) and Scenario 6 (CI integration); record outcomes in PR description
- [x] T026 [US3] Confirm `./mvn-env.sh -pl modules/perc-distribution-tree -am clean verify` is green on the un-modified source tree (SC-005)

**Checkpoint**: All three user stories are independently functional; CI fails loudly on regression.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final hygiene and cross-cutting verification

- [x] T027 [P] Update `modules/perc-distribution-tree/scripts/README.md` to also list the expected driver set and the exit-code table from `contracts/README.md` Contract 2 (script consumer documentation)
- [x] T028 [P] Add a single-line `scripts/README.md` pointer from `modules/perc-distribution-tree/README.md` "Verifying the build" section (linkability)
- [x] T029 Re-run `./mvn-env.sh -pl modules/perc-distribution-tree -am clean verify` from a clean tree to confirm the full pipeline (Phases 1–5) passes end-to-end and all SC-001 through SC-005 success criteria are observable in the output
- [x] T030 Run `specs/001-fix-jdbc-drivers/quickstart.md` Scenarios 1–6 in order and capture results for the PR description
- [x] T031 Run `git diff` review of touched files (`modules/perc-distribution-tree/pom.xml`, `modules/perc-distribution-tree/src/main/resources/installDistributionFiles.xml`, `pom.xml`, `modules/perc-distribution-tree/README.md`, `modules/perc-distribution-tree/AGENTS.md`, `modules/perc-distribution-tree/scripts/verify-jdbc-drivers.sh`, `modules/perc-distribution-tree/scripts/README.md`) and confirm no drive-by changes outside scope (Constitution V)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately.
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories.
- **User Stories (Phase 3+)**: All depend on Foundational phase completion.
  - User stories are sequential in priority (P1 → P2 → P3) but each is independently testable once its preceding phase is complete.
- **Polish (Final Phase)**: Depends on all user stories being complete.

### User Story Dependencies

- **User Story 1 (P1)**: Depends on Phase 2 only. Independent of US2/US3.
- **User Story 2 (P2)**: Depends on US1 (because the `verify-jdbc-drivers.sh` script was introduced in US1 and US2 extends it). Stays independently testable on its own artifacts.
- **User Story 3 (P3)**: Depends on US1 + US2 (extends the script with `--expected-driver-set`, hard-codes the driver names in the Maven wiring). Stays independently testable.

### Within Each User Story

- Tests are written/updated and FAIL before implementation, then PASS after (Constitution III).
- POM edits (T007, T008) precede ANT script edits (T012, T013) — the ANT copy depends on the Maven-staged files existing.
- `verify-jdbc-drivers.sh` is created in US1, extended in US2 and US3, so the order US1 → US2 → US3 must be preserved.

### Parallel Opportunities

- Phase 1 tasks T003 and T004 are `[P]` (different files / different concerns, no overlap).
- Phase 2 tasks T005 and T006 are `[P]` (different files / different concerns: root `pom.xml` promotion vs root `pom.xml` verification — coordination is by file so they can be split if reviewed carefully). T007 is `[P]` in the sense that it touches a different file (`modules/perc-distribution-tree/pom.xml`) but it has a hard dependency on T005 + T006 completing first (versions must be in root `<dependencyManagement>` before module deps can reference them without naked version strings).
- Phase 2 T008 and T009 can run in parallel with T005/T006/T007 (different files again).
- US1 tests T010 and T011 are `[P]`.
- US2 tests T018 and T019 are `[P]`.
- US3 test T023 is `[P]` (different file: extends the script in a new mode, not the existing test paths).
- Polish tasks T027 and T028 are `[P]`.

---

## Parallel Example: User Story 1

```bash
# Launch US1 tests + docs research together:
Task: "Add verification script at modules/perc-distribution-tree/scripts/verify-jdbc-drivers.sh"
Task: "Re-read installer scripts install.xml/installServer.xml/installRepository.xml for purge behavior"
Task: "Confirm mariadb-java-client coordinates are accessible from the local Maven repo / parent POM"
```

After tests are red:

```bash
Task: "Edit modules/perc-distribution-tree/pom.xml to declare + stage JDBC driver dependencies"
Task: "Edit modules/perc-distribution-tree/src/main/resources/installDistributionFiles.xml to copy _jdbc-stage into jetty/base/lib/jdbc/"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Run `./mvn-env.sh -pl modules/perc-distribution-tree -am clean verify`, unpack the jar, confirm `jetty/base/lib/jdbc/` contains the MariaDB driver
5. Ship the PR if US1 is green

### Incremental Delivery

1. Setup + Foundational → foundation ready (`_jdbc-stage/` populated; `verify-jdbc-drivers.sh` exists)
2. Add User Story 1 → production build ships MariaDB driver → MVP deployable
3. Add User Story 2 → bundled-driver validity verified + drop-point documented
4. Add User Story 3 → CI fails loudly on regression
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers (single-module change so not heavily parallel):

1. One developer owns US1 end-to-end (POM + ANT + script + README) — single coherent change.
2. A second developer can draft US2 (script extension + README updates) once US1's script skeleton is in place; final integration happens after US1 merges.
3. US3 is owned by the same developer as US1/US2 to keep the script and Maven wiring coherent.

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story is independently completable and testable
- Verify tests fail before implementing (Constitution III)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same-file conflicts, cross-story dependencies that break independence
- Constitution gates: I (module-first), II (evidence over invention — no invented drivers), III (tests required), V (no new frameworks / no Spring Boot), VII (no naked version strings; use parent-POM management)