---
description: "Task list for JDBC Drivers Packaging Cleanup"
---

# Tasks: JDBC Drivers Packaging Cleanup

**Input**: Design documents from `/specs/002-jdbc-drivers-cleanup/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/, quickstart.md

**Tests**: REQUIRED for every behavioral code change (Constitution III — Test Discipline). Each user story MUST include test tasks. Prefer fail-then-pass (write/adjust tests first).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact mono-repo file paths in descriptions (module + `src/main` / `src/test`)

## Path Conventions

- **Module**: `modules/perc-distribution-tree/`
- **Build XML**: `modules/perc-distribution-tree/src/main/resources/installDistributionFiles.xml`
- **Install script**: `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/install.xml`
- **Module README**: `modules/perc-distribution-tree/README.md`
- **Module scripts**: `modules/perc-distribution-tree/scripts/{verify-jdbc-drivers.sh,check-no-glob-deletes.sh,README.md}`
- **Tests**: `modules/perc-distribution-tree/src/test/java/com/percussion/distribution/jdbc/`
- **Module POM**: `modules/perc-distribution-tree/pom.xml`
- Build/test with `./mvn-env.sh` (JDK 21 on `development`)

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirm ownership, AGENTS rules, and build baseline for the single touched module.

- [x] T001 Verify module ownership: confirm `modules/perc-distribution-tree/` is the only owning module; re-read root `AGENTS.md` and `modules/perc-distribution-tree/AGENTS.md`
- [x] T002 Confirm branch baseline: run `git status` clean on `002-jdbc-drivers-cleanup` based on `development`; confirm JDK 21 via `./mvn-env.sh -v`
- [x] T003 [P] Confirm `mvn-env.sh` baseline build works: `cd modules/perc-distribution-tree && ../../mvn-env.sh -o -q clean verify` succeeds before any edits (record baseline `mvn verify` output excerpt for the PR description)
- [x] T004 [P] Note: this module has no `spotless` plugin in `pom.xml`; no formatting task required

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Establish the exact pin lists, the test source tree, and the test dependencies that all three user stories depend on. No user-story work can begin until this phase is complete.

- [x] T005 [P] Create test source root `modules/perc-distribution-tree/src/test/java/com/percussion/distribution/jdbc/` (this module has no existing `src/test` tree)
- [x] T006 Add JUnit 5 test dependencies to `modules/perc-distribution-tree/pom.xml` (the module's `<dependencies>` block, NOT `dependencyManagement`): add `<dependency><groupId>org.junit.jupiter</groupId><artifactId>junit-jupiter</artifactId><scope>test</scope></dependency>` (version is inherited from parent `dependencyManagement` at root `pom.xml:448-453`; do not pin a version)
- [x] T007 [P] Add a `surefire-plugin` declaration to `modules/perc-distribution-tree/pom.xml` `<build><plugins>` if not already present, using parent-POM `pluginManagement` defaults (root `pom.xml:1947-1964`) — this is a first test in the module, so plugin activation must be verified
- [x] T008 [P] Define the exact pin list as a constant: in `modules/perc-distribution-tree/src/test/java/com/percussion/distribution/jdbc/BundledJdbcDrivers.java`, create a package-private enum or constant set listing the 7 bundled filenames from data-model.md (E2): `mariadb-java-client-3.5.7.jar`, `derby-10.17.1.0.jar`, `derbyclient-10.17.1.0.jar`, `derbynet-10.17.1.0.jar`, `mssql-jdbc-13.3.1.jre11-preview.jar`, `jtds-1.3.1.jar`, `ojdbc17-23.26.0.0.0.jar`
- [x] T009 Verify foundational phase: run `cd modules/perc-distribution-tree && ../../mvn-env.sh -o test -Dtest=NoSuchTest` (or `test-compile`) and confirm the test source tree compiles against the new junit-jupiter dependency; resolve any version-mismatch or surefire errors before proceeding

**Checkpoint**: Foundation ready — test source root exists, JUnit 5 is on the module classpath, the bundled-driver constant is defined, and `mvn test` runs cleanly with zero tests. User story implementation can now begin.

---

## Phase 3: User Story 1 — Exclude staging directory from shipped distribution (Priority: P1) 🎯 MVP

**Goal**: A release engineer's `mvn package` no longer includes `_jdbc-stage/**` inside the produced `perc-distribution-tree.jar`; the staging directory is physically removed by an explicit `<delete>` step in the ANT script before assembly runs.

**Independent Test**: After `cd modules/perc-distribution-tree && ../../mvn-env.sh clean package`, `unzip -l target/perc-distribution-tree.jar | grep '_jdbc-stage'` returns zero matches; `unzip -l target/perc-distribution-tree.jar | awk '/jetty\/base\/lib\/jdbc/ && /\.jar$/'` lists exactly the 7 curated drivers from data-model.md (E1).

### Tests for User Story 1 (REQUIRED) ⚠️

> **NOTE: Write these tests FIRST; ensure they FAIL before the implementation, then PASS after.**

- [x] T010 [P] [US1] Create the staging-cleanup regression test scaffold: add `modules/perc-distribution-tree/src/test/java/com/percussion/distribution/jdbc/StagingCleanupAntScriptTest.java` with one `@Test` method that reads `src/main/resources/installDistributionFiles.xml` as a classpath resource, parses the ANT XML (use `javax.xml.parsers.DocumentBuilderFactory` — already in JDK), locates the staging-copy `<copy>` block, and asserts that a sibling `<delete dir="..._jdbc-stage..."/>` element appears AFTER it and BEFORE the next major section (the `fixcrlf` task). This test MUST fail on the unedited build XML.
- [x] T011 [P] [US1] Add the curated-set regression test: in the same file, add a second `@Test` method that reads `installDistributionFiles.xml`, locates the `<copy todir="${assembly-directory}/jetty/base/lib/jdbc/">` block, and asserts (a) the `<fileset>` `<include>` patterns are exactly: `mariadb-java-client-*.jar`, `derby-*.jar`, `derbyclient-*.jar`, `derbynet-*.jar`, `mssql-jdbc-*.jar`, `jtds-*.jar`, `ojdbc17-*.jar` (matches the `verify-jdbc-drivers.sh` globs wired into `pom.xml:737`); AND (b) the module's `pom.xml` declares no non-driver `provided`-scope dependency that is copied by the staging execution — i.e. the union of the staging `<include>` globs covers exactly the curated set and no other `provided` dep. The test reads the module's `pom.xml` as a classpath resource, parses the `<dependencies>` block, and asserts that for every `<dependency>` with `<scope>provided</scope>`, either the artifactId matches one of the seven bundled globs, or the dependency is excluded by name from the staging copy. This addresses FR-002's "anywhere else" clause (non-JDBC provided-scope deps MUST NOT appear in the JDBC area).

### Implementation for User Story 1

- [x] T012 [US1] Edit `modules/perc-distribution-tree/src/main/resources/installDistributionFiles.xml` immediately after the `<copy todir="${assembly-directory}/jetty/base/lib/jdbc/">` block (currently lines 707-717) and before the `LEGACY: DEVELOPMENT=true` `<if>` block: add `<delete dir="${assembly-directory}/_jdbc-stage" failonerror="false" verbose="true"/>` so the staging folder is physically removed (ANT `<delete>` is idempotent on missing dirs when `failonerror="false"`; satisfies FR-007 idempotency). After editing, T010's `<delete>` assertion must pass and the new test must be green.
- [x] T013 [US1] Rephrase the misleading ANT comment at `modules/perc-distribution-tree/src/main/resources/installDistributionFiles.xml:702-704`: replace the claim "ANT `<copy>` fails by default when a staged source file is missing" with the truthful description that an empty globbed `<fileset>` is a silent no-op, and explicitly attribute the loud-failure guarantee to (a) `failOnAnyMissingDependency=true` on the Maven `copy-dependencies` execution (`pom.xml:506`) and (b) the `verify-jdbc-drivers` exec in the `verify` phase (`pom.xml:725-740`). This implements FR-005 and is read by reviewers and future maintainers.
- [x] T014 [US1] Run the US1 tests: `cd modules/perc-distribution-tree && ../../mvn-env.sh -o test -Dtest='StagingCleanupAntScriptTest'`. Both `@Test` methods must pass.
- [x] T015 [US1] Run the full module build end-to-end: `cd modules/perc-distribution-tree && ../../mvn-env.sh -o clean verify`. Confirm: (a) BUILD SUCCESS; (b) `verify-jdbc-drivers.sh` reports `OK: 7 JDBC driver JAR(s) verified`; (c) `target/perc-distribution-tree.jar` exists and has no `_jdbc-stage` entries (manual `unzip -l` check from quickstart Scenario 1).
- [x] T016 [US1] Commit the US1 changes as a single logical commit (e.g. `fix(distribution-tree): remove _jdbc-stage staging dir before assembly`); include the test output excerpt in the commit body

**Checkpoint**: At this point, User Story 1 is complete. The shipped JAR no longer leaks `_jdbc-stage/**`, and a regression test guards against reintroduction. This is the MVP — the artifact now ships the right payload.

---

## Phase 4: User Story 2 — Stop install script from purging integrator-supplied drivers (Priority: P1)

**Goal**: The install/upgrade ANT script's `<delete>` block in `install.xml` no longer uses glob patterns; it uses an exact-filename list of the 7 bundled drivers. Integrator-supplied drivers in `jetty/base/lib/jdbc/` (including ones whose names match the old bundled-name globs, e.g. `mysql-connector-java-9.0.0.jar`) are preserved on install/upgrade.

**Independent Test**: Static check — read `install.xml` and assert the `<delete>` block contains exactly the 7 bundled filenames (E2) and no `*` or `?` characters. The JUnit test and the shell assertion together prove the property; a full end-to-end installer run is documented in quickstart Scenario 7 for manual verification only.

### Tests for User Story 2 (REQUIRED) ⚠️

> **NOTE: Write these tests FIRST; they MUST fail on the current (glob-based) `install.xml`; then pass after the pin-list replacement.**

- [x] T017 [P] [US2] Create `modules/perc-distribution-tree/src/test/java/com/percussion/distribution/jdbc/InstallXmlDeleteSetTest.java` with four `@Test` methods:
  - `deleteSetContainsAllBundledFilenames()` — read `src/main/resources/distribution/rxconfig/Installer/install.xml` as a classpath resource, parse with `DocumentBuilderFactory`, locate the `<target name="install_jdbc_drivers">` element, find its `<delete>` child, extract the union of `<include name="...">` filenames, and assert the set equals the E2 list from `BundledJdbcDrivers`. This MUST fail on the current (glob-based) `install.xml`.
  - `deleteSetContainsNoGlobPatterns()` — same setup, assert no `<include name="...">` value contains `*` or `?`. This MUST fail on the current file (the globs `mariadb-java-client-*.jar`, etc., contain `*`).
  - `deleteSetPreservesIntegratorFilenames()` — assert that `mysql-connector-java-9.0.0.jar` and `ojdbc17-99.99.99.99.jar` are NOT in the delete set. (The current file lists `mysql-connector-java-*.jar` which would match the first, so this MUST fail on the current file.)
  - `deleteSetOmitsLegacyAndNonShippedGlobs()` — assert that the delete set does NOT contain `derbyshared-*.jar`, `derbytools-*.jar`, `mysql-connector-java-*.jar`, or `mysql-connector.jar` (these are removed as part of the cleanup since they purge integrator drivers and match no shipped file).
- [x] T018 [P] [US2] Create the new shell assertion script `modules/perc-distribution-tree/scripts/check-no-glob-deletes.sh` as a POSIX-`sh` script (modeled on `verify-jdbc-drivers.sh` style: `set -u`, `usage()`, clear single-line error message, distinct exit code 7 for "glob found"). It accepts `--install-xml <path>` (default: `src/main/resources/distribution/rxconfig/Installer/install.xml` relative to the module), extracts the `<delete>` block under the `install_jdbc_drivers` target, and greps for `*` or `?` within `<include name="...">` lines. Exits 0 if none; exits 7 with a clear error if any glob is found.
- [x] T019 [P] [US2] Wire `check-no-glob-deletes.sh` into the Maven `verify` phase: edit `modules/perc-distribution-tree/pom.xml` to add a new `exec-maven-plugin` execution (mirror the existing `verify-jdbc-drivers` execution at `pom.xml:725-740`) named e.g. `check-no-glob-deletes`, with `<phase>verify</phase>`, `<executable>${basedir}/scripts/check-no-glob-deletes.sh</executable>`, and no extra arguments. This MUST make `mvn verify` fail when the assertion script exits non-zero.

### Implementation for User Story 2

- [x] T020 [US2] Edit `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/install.xml`: replace the entire `<delete failonerror="false" verbose="true">` block at lines 174-188 (the one with the 11 glob `<include>` lines) with a new `<delete failonerror="false" verbose="true">` block that lists exactly 7 `<include name="..."/>` entries (no globs) for the E2 filenames:
  ```
  <delete failonerror="false" verbose="true">
      <fileset dir="${install.dir}/jetty/base/lib/jdbc">
          <include name="mariadb-java-client-3.5.7.jar"/>
          <include name="derby-10.17.1.0.jar"/>
          <include name="derbyclient-10.17.1.0.jar"/>
          <include name="derbynet-10.17.1.0.jar"/>
          <include name="mssql-jdbc-13.3.1.jre11-preview.jar"/>
          <include name="jtds-1.3.1.jar"/>
          <include name="ojdbc17-23.26.0.0.0.jar"/>
      </fileset>
  </delete>
  ```
  The `mysql-connector-java-*.jar`, `mysql-connector.jar`, `derbyshared-*.jar`, `derbytools-*.jar` globs are removed entirely. After editing, all 4 `@Test` methods in T017 must pass.
- [x] T021 [US2] Update the surrounding comment at `install.xml:168-173` to honestly describe the new behavior: the delete set is the bundled-driver exact filenames for this release; integrator-supplied drivers are preserved; when driver versions bump, this list is updated in lockstep with the parent POM's version properties (and the README's "Extending the driver set" section in `README.md:80` is now truthful).
- [x] T022 [US2] Run the US2 JUnit tests: `cd modules/perc-distribution-tree && ../../mvn-env.sh -o test -Dtest='InstallXmlDeleteSetTest'`. All 4 methods must pass.
- [x] T023 [US2] Run the full module build: `cd modules/perc-distribution-tree && ../../mvn-env.sh -o clean verify`. Both `verify-jdbc-drivers` and the new `check-no-glob-deletes` executions must pass.
- [x] T024 [US2] Commit the US2 changes as a single logical commit (e.g. `fix(distribution-tree): pin install.xml jdbc delete set to exact bundled filenames`)

**Checkpoint**: At this point, User Stories 1 AND 2 are both complete and independently testable. The shipped JAR is correct AND the install/upgrade behavior is correct.

---

## Phase 5: User Story 3 — Correct the misleading ANT copy comment and the verify-script example (Priority: P2)

**Goal**: A maintainer reading the build XML around `stage-jdbc-drivers` is not misled about ANT behavior. The `scripts/README.md` example for `verify-jdbc-drivers.sh` uses the option actually wired into the Maven `verify` execution (`--expected-driver-glob`) and exits 0 when run verbatim against a freshly built artifact.

**Independent Test**: Read the relevant comment in `installDistributionFiles.xml` and assert it does not claim `<copy>` fails on missing sources. Run the documented `scripts/README.md` example verbatim against `target/perc-distribution-tree.jar` and assert exit 0 with the expected "OK: 7 JDBC driver JAR(s) verified" output.

### Tests for User Story 3 (REQUIRED) ⚠️

> **NOTE: These are static / behavioral tests; write first, confirm they fail on the unfixed docs, then confirm pass after the fix.**

- [x] T025 [P] [US3] Add a new `@Test` method to `modules/perc-distribution-tree/src/test/java/com/percussion/distribution/jdbc/StagingCleanupAntScriptTest.java` named `antCopyCommentDoesNotMisattributeFailure()`: read `installDistributionFiles.xml`, extract the comment block immediately preceding the staging-copy `<copy>` (lines 695-704 in the unedited file), and assert the comment does NOT contain the substring "ANT `<copy>` fails by default when a staged source file is missing" (or equivalent misleading phrasing). It SHOULD contain references to `failOnAnyMissingDependency` and/or `verify-jdbc-drivers`. This MUST fail on the unedited file (note: T013 already rephrases the comment; this test guards against regression of that rephrasing).
- [x] T026 [P] [US3] *(removed by `/speckit.analyze` remediation A2 — over-spec; T028 already proves SC-004 by running the documented example verbatim against a fresh build)*

### Implementation for User Story 3

- [x] T027 [US3] Edit `modules/perc-distribution-tree/scripts/README.md`: replace the example at lines 13-18 with an `--expected-driver-glob` example using the same globs wired into `pom.xml:737`. Use single-quote POSIX-safe quoting around the glob string (it contains `*`):
  ```
  ./scripts/verify-jdbc-drivers.sh --artifact path/to/perc-distribution-tree.jar \
      --expected-driver-glob 'mariadb-java-client-*.jar,derby-*.jar,derbyclient-*.jar,derbynet-*.jar,mssql-jdbc-*.jar,jtds-*.jar,ojdbc17-*.jar'
  ```
  After editing, T028's exit-0 assertion is the proof of SC-004.
- [x] T028 [US3] Sanity-check the documented example by running it verbatim against the freshly built artifact: `cd modules/perc-distribution-tree && ../../mvn-env.sh -o clean package && ./scripts/verify-jdbc-drivers.sh --artifact target/perc-distribution-tree.jar --expected-driver-glob 'mariadb-java-client-*.jar,derby-*.jar,derbyclient-*.jar,derbynet-*.jar,mssql-jdbc-*.jar,jtds-*.jar,ojdbc17-*.jar'`. Expect exit 0 and `OK: 7 JDBC driver JAR(s) verified under jetty/base/lib/jdbc/`. This implements FR-006 and SC-004.
- [x] T029 [US3] Update the example's exit-code table comment in `scripts/README.md` if needed to reflect that the documented example is `--expected-driver-glob` (not `--expected-driver-set`); minor wording consistency.
- [x] T030 [US3] Run the full module build: `cd modules/perc-distribution-tree && ../../mvn-env.sh -o clean verify`. Both `verify` executions must pass: `verify-jdbc-drivers` and `check-no-glob-deletes`, plus the JUnit suite.
- [x] T031 [US3] Commit the US3 changes as a single logical commit (e.g. `docs(distribution-tree): correct verify-jdbc-drivers.sh README example`)

**Checkpoint**: All three user stories are now complete and independently testable. The shipped JAR is correct, the install/upgrade behavior is correct, and the documentation matches reality.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final integration check, cross-references, and any small consistency fixes found during the implementation. No drive-by refactors.

- [x] T032 [P] Update `modules/perc-distribution-tree/README.md` "Extending the driver set" section (lines 78-80): the "do not purge this folder" claim is now actually true. Add a one-sentence note that the bundled delete set in `install.xml` is pinned to the exact filenames shipped in this release, and a pointer to `scripts/verify-jdbc-drivers.sh` for verification. This update is required (not optional): `README.md:80` is the public, customer-facing contract for FR-003/FR-004, and the plan claimed this file would be updated. Required by F1 of the `/speckit.analyze` report.
- [x] T033 [P] Update `modules/perc-distribution-tree/scripts/README.md` "Adding a script here" section to mention the new `check-no-glob-deletes.sh` script per the established pattern (per `AGENTS.md` requirement to document new scripts).
- [x] T034 [P] Run the full module test+verify suite one last time: `cd modules/perc-distribution-tree && ../../mvn-env.sh -o clean verify`. Capture the final `verify` output excerpt for the PR description.
- [x] T035 [P] Validate quickstart.md Scenarios 1-6 from `specs/002-jdbc-drivers-cleanup/quickstart.md` end-to-end on a clean checkout; capture command output for the PR description. Scenario 7 (manual installer simulation) is documented but not run in CI.
- [x] T036 [P] Open the PR against `development` with title `fix(distribution-tree): address 002-jdbc-drivers-cleanup review findings` and a body that links to the 4 review comments on PR #1184 and explains how each is now addressed. The PR description MUST include: (a) a link to `specs/002-jdbc-drivers-cleanup/spec.md` and a one-line Summary; (b) a per-FR/SC mapping table showing which task/commit satisfies which requirement; (c) the final `mvn clean verify` excerpt; (d) the `unzip -l` excerpt proving no `_jdbc-stage` entries. Do NOT merge until CI is green and a reviewer approves.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational — independently testable
- **User Story 2 (Phase 4)**: Depends on Foundational — independently testable
- **User Story 3 (Phase 5)**: Depends on Foundational — independently testable (no functional overlap with US1/US2; touches only docs)
- **Polish (Phase 6)**: Depends on all three user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2). No dependencies on US2 or US3. Can be implemented and merged as the MVP. The only cross-cutting concern: T013 (ANT comment rephrasing) and T014 (test run) and T015 (full build) are sequenced within US1.
- **User Story 2 (P1)**: Can start after Foundational. Functionally independent of US1 (different files: `install.xml` vs. `installDistributionFiles.xml`). However, US2's T019 wires a new `exec-maven-plugin` execution into `pom.xml`; this requires the foundational JUnit/Maven wiring from Phase 2.
- **User Story 3 (P2)**: Can start after Foundational. Independent of US1 and US2. Touches `installDistributionFiles.xml` (T013's comment edit, also touched by US1 T012/T013) and `scripts/README.md` (only US3). The comment-edit overlap is minor: T013 (US1) rephrases the comment to satisfy FR-005; T025 (US3) writes a regression test that the comment stays rephrased. These can land in either order, but the recommended order is US1 first (so the comment fix is part of the US1 commit) and US3 second (so the US3 test guards the US1 fix).

### Within Each User Story

- Tests MUST be written/updated and FAIL before implementation, then PASS after
- Implementation edits are surgical and confined to the listed files
- Full `mvn clean verify` is the final gate for each user story
- Commit after each user story (US1, US2, US3) and at the end (Polish)

### Parallel Opportunities

- Setup tasks T001/T002/T003/T004 are all independent and can be done in any order (T003 is the longest; T001/T002/T004 are quick reads)
- Within Foundational: T005 (mkdir) and T006 (POM junit dep) and T007 (surefire plugin) and T008 (constant file) are independent and can be done in parallel
- US1 tests T010 and T011 are independent and can be done in parallel
- US2 tests T017 (Java) and T018 (shell) and T019 (POM wiring) are independent
- US3 tests T025 and T026 are independent
- US1, US2, US3 can be worked on in parallel by different developers once Foundational is complete (US1 and US2 touch different files; US3 touches `installDistributionFiles.xml` (comment only) and `scripts/README.md`)
- US1 must commit its `_jdbc-stage` fix before the PR is opened (T036), since both US1 and US3 modify the same XML file's comment area

---

## Parallel Example: User Story 2

```bash
# After Phase 2 (Foundational) is complete, US2's three test tasks can be launched in parallel
# by different developers or agents because they touch different files:

Task T017 (Java): "Write InstallXmlDeleteSetTest.java with 4 @Test methods"
Task T018 (Shell): "Write scripts/check-no-glob-deletes.sh"
Task T019 (POM):   "Wire check-no-glob-deletes.sh into Maven verify phase"

# Then US2 implementation:
Task T020: "Replace glob-based <delete> in install.xml with exact-filename list"
Task T021: "Rephrase surrounding comment in install.xml"
# (T020 and T021 are in the same file — same developer, sequential)

# Final gate:
Task T022-T024: "Run tests, run full mvn clean verify, commit"
```

---

## Implementation Strategy

### MVP First (User Stories 1 + 2 — the P1s)

The MVP for this feature is **US1 + US2 together** because both are P1 and both must land for the bug to be considered fixed from the customer's perspective (US1 stops the JAR leak; US2 stops the install-time purge). US3 is documentation-only and is a P2.

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: User Story 1 (MVP increment #1 — correct artifact payload)
4. Complete Phase 4: User Story 2 (MVP increment #2 — correct install behavior)
5. **STOP and VALIDATE**: Test both stories independently via `mvn clean verify`; manually inspect the JAR; manually run the `scripts/README.md` example
6. Open PR (T036) for review

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Commit (MVP increment #1)
3. Add User Story 2 → Test independently → Commit (MVP increment #2)
4. Add User Story 3 → Test independently → Commit (docs fix)
5. Open PR with all three commits → review → merge

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 (touches `installDistributionFiles.xml`)
   - Developer B: User Story 2 (touches `install.xml` + `pom.xml` + new test files)
   - Developer C: User Story 3 (touches `installDistributionFiles.xml` comment + `scripts/README.md`)
3. Stories complete and integrate independently; US1 and US3 share a file but only the comment region, so they should land in commit order (US1 first, then US3)

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story is independently completable and testable
- Verify tests fail before implementing (fail-then-pass per Constitution III)
- Commit after each user story or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same-file conflicts that break [P] parallelism, cross-story dependencies that break independence
- This module has no `src/test` tree and no JUnit 5 dependencies today; Phase 2 creates them — all user-story tests depend on Phase 2 being complete
- Build/test invocations always use `../../mvn-env.sh` (or `mvn-env.bat` on Windows) per the module `AGENTS.md`
- Do NOT use Spring Boot (Constitution V); do NOT add new top-level Maven modules (Constitution Complexity Budget)
