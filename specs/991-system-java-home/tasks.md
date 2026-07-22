# Tasks: System / Configurable Java Home

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md  
**Branch**: `991-system-java-home`  
**Issue**: https://github.com/intersoftdatalabs-in/percussioncms/issues/1340  

**GitHub story issues** (checklists live on the issues; T0xx remain here as the source of truth):

| Phase / story | GitHub |
|---------------|--------|
| Setup + Foundational | #1377 |
| US1 CMS Jetty | #1378 |
| US2 DTS | #1379 |
| US3 Interactive install | #1380 |
| US4 Unattended install | #1381 |
| US5 Re-point | #1382 |
| US6 Legacy + install.xml | #1383 |
| Polish | #1384 |

## Phase 1: Setup

- [x] T001 Identify owning module path(s) and read AGENTS hierarchy: root `AGENTS.md`, `modules/perc-jetty/AGENTS.md`, `modules/perc-distribution-tree/AGENTS.md` (if present), delivery-tier-distribution README
- [x] T002 Confirm branch JDK 21 and verify baseline tests: `./mvn-env.sh -pl modules/perc-jetty -am test` (structural harness already present for service scripts)

## Phase 2: Foundational (Blocking Prerequisites)

**Goal**: Shared resolution contract implemented as pure Java helpers + dual platform resolve scripts that all stories consume. No CMS/DTS start script cutover yet (that is US1/US2).

- [x] T003 Map all hard-coded `JRE` / `JRE64` / `JAVA_HOME` consumers: `modules/perc-jetty/src/main/jetty/StartJetty.sh`, `StartJetty.bat`, `StopJetty.bat`, `service/install-jetty-service.sh`, `service/install-jetty-service.bat`; DTS `deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/src/main/rootFiles/TomcatStartup.*`, `TomcatShutdown.*`, `DTSProductionService.*`, `DTSStagingService.*`; preinstall `modules/perc-distribution-tree/.../Main.java`; `system/release/installer/**` — update inventory note in `specs/991-system-java-home/research.md` if gaps found
- [x] T004 Assess security/ops surface: only filesystem paths in `java.properties`; no secrets; validate path existence before exec; document service re-install after re-point in `specs/991-system-java-home/quickstart.md` notes
- [x] T005 [P] Implement Java pure helpers for version parse, home validation, properties load/merge/write, and precedence order per `specs/991-system-java-home/contracts/java-home-resolution.md` and `java-properties-contract.md` under `modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/java/` (e.g. `JavaHomeResolver.java`, `JavaPropertiesSupport.java`) using portable `java.nio.file.Path`
- [x] T006 [P] Add JUnit 5 tests for helpers in `modules/perc-distribution-tree/src/test/java/com/percussion/preinstall/java/JavaHomeResolverTest.java` and `JavaPropertiesSupportTest.java` (temp dirs via NIO; major 21 accept/reject; precedence order; merge preserves unknown keys)
- [x] T007 [P] Add Unix resolver script `modules/perc-jetty/src/main/jetty/resolve-java-home.sh` implementing the same precedence (config → env → JRE → JRE64 → PATH → fail with “21”)
- [x] T008 [P] Add Windows resolver script `modules/perc-jetty/src/main/jetty/resolve-java-home.bat` with identical precedence and absolute path outputs for `JAVA_HOME` / `JAVA`
- [x] T009 Add structural/content tests asserting resolvers exist and encode contract markers (precedence labels, major 21 error text) in `modules/perc-jetty/src/test/java/com/percussion/jetty/java/ResolveJavaHomeScriptTest.java`
- [x] T010 Document canonical helper entry points in `modules/perc-jetty/README.md` (section: Java home resolution) and cross-link contracts under `specs/991-system-java-home/contracts/`

---

## Phase 3: User Story 1 — Run CMS without manual InstallDir/JRE (Priority: P1)

**Goal**: CMS Jetty console start/stop uses resolver + `java.properties`; no required `<InstallDir>/JRE` copy/symlink when config or env Java 21 is present.  
**Independent Test**: Quickstart Smoke A + C on CMS; `StartJetty` with only `java.properties` and no JRE folder; unit/structural tests green.

### Tests (Required)

- [x] T011 [P] [US1] Extend `modules/perc-jetty/src/test/java/com/percussion/jetty/java/ResolveJavaHomeScriptTest.java` (or new `StartJettyJavaHomeTest.java`) to assert `StartJetty.sh`, `StartJetty.bat`, `StopJetty.bat` source/call `resolve-java-home` and do **not** unconditionally assign only `%rxDir%\JRE` / `${rxDir}/JRE`
- [x] T012 [P] [US1] Test `install-jetty-service.sh` / `.bat` snippets populate service Java from resolver/`java.properties` (not hard-coded install-dir JRE only) in `modules/perc-jetty/src/test/java/com/percussion/jetty/service/InstallJettyServiceJavaHomeTest.java`

### Implementation

- [x] T013 [US1] Wire `StartJetty.sh` to source `resolve-java-home.sh` with install root = parent of jetty dir; fail fast if resolve fails — `modules/perc-jetty/src/main/jetty/StartJetty.sh`
- [x] T014 [P] [US1] Wire `StartJetty.bat` and `StopJetty.bat` to call `resolve-java-home.bat` — `modules/perc-jetty/src/main/jetty/StartJetty.bat`, `StopJetty.bat`
- [x] T015 [US1] Update `modules/perc-jetty/src/main/jetty/service/install-jetty-service.sh` to resolve Java via the shared order before writing `/etc/default/${SERVICE_NAME}` (`JAVA_HOME` / `JAVA` lines)
- [x] T016 [US1] Update `modules/perc-jetty/src/main/jetty/service/install-jetty-service.bat` to set Procrun `--JavaHome` / `PR_JVM` from resolved home (absolute), not only `..\JRE`
- [x] T017 [US1] Add operator note for post-install re-point (edit `java.properties`, restart; re-run service install if service cached home) in `modules/perc-jetty/README.md` and `specs/991-system-java-home/quickstart.md` (supports US5 early)
- [ ] T018 [US1] Commit US1 + foundational (T005–T017 as needed), open PR against `development`, pause for review/merge before US2
- [ ] T019 [US1] Monitor CI/Kilo checks; address feedback; resolve review threads per AGENTS.md before next story

---

## Phase 4: User Story 2 — Run DTS under same resolution rules (Priority: P1)

**Goal**: DTS Production/Staging console and service scripts share the same precedence and version 21 rules as CMS.  
**Independent Test**: Quickstart Smoke B; DTS start/stop without InstallDir/JRE when config/env set; structural tests.

### Tests (Required)

- [x] T020 [P] [US2] Structural tests for DTS rootFiles resolvers and startup scripts in `deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/src/test/java/.../DtsJavaHomeScriptTest.java` (assert source order / no sole hard-coded JRE success path)
- [x] T021 [P] [US2] Structural tests for `DTSProductionService.sh` / `DTSStagingService.sh` / `.bat` Java home selection in same test package

### Implementation

- [x] T022 [US2] Add or package DTS copies of resolve helpers under `deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/src/main/rootFiles/resolve-java-home.sh` and `resolve-java-home.bat` (keep behavior identical to Jetty contract; avoid silent drift)
- [x] T023 [US2] Update `TomcatStartup.sh` and `TomcatStartup.bat` to use resolver instead of only `cd JRE` / `SCRIPT_DIR\JRE` — `.../rootFiles/TomcatStartup.sh`, `TomcatStartup.bat`
- [x] T024 [P] [US2] Update `TomcatShutdown.sh` and `TomcatShutdown.bat` the same way — `.../rootFiles/TomcatShutdown.sh`, `TomcatShutdown.bat`
- [x] T025 [US2] Update `DTSProductionService.sh`, `DTSStagingService.sh`, `DTSProductionService.bat`, `DTSStagingService.bat` to resolve Java via shared order when writing service env / Procrun JavaHome
- [x] T026 [US2] Document DTS Java resolution in `deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/README.md` (or module README)
- [ ] T027 [US2] Commit US2, open PR, pause for review/merge before US3

---

## Phase 5: User Story 3 — Interactive multi-candidate install selection (Priority: P1)

**Goal**: Interactive install discovers eligible Java 21 candidates; prompts when multiple; auto-selects when one; fails when zero; writes `java.properties`.  
**Independent Test**: Quickstart Smoke E; unit tests for discovery + prompt path with fixtures; no silent “success” requiring manual JRE copy.

### Tests (Required)

- [ ] T028 [P] [US3] Unit tests for candidate discovery/filter (major 21, executable launcher) in `modules/perc-distribution-tree/src/test/java/com/percussion/preinstall/java/JavaCandidateDiscoveryTest.java`
- [ ] T029 [P] [US3] Unit tests for selection outcomes (0/1/N candidates) and `java.properties` write content in `.../JavaInstallSelectionTest.java`

### Implementation

- [ ] T030 [US3] Implement discovery helpers (env, process home, common OS paths, PATH) in `modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/java/JavaCandidateDiscovery.java`
- [ ] T031 [US3] Wire interactive selection + write of `{installPath}/java.properties` into `modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/Main.java` (reuse `perc.java.home` when already set and valid as preferred single candidate)
- [ ] T032 [US3] Fail install with actionable message when zero eligible candidates (must mention major version 21) in preinstall flow / user-visible output
- [ ] T033 [US3] Align DTS preinstall if it owns a separate install root: `deliverytiersuite/.../MainDTSPreInstall.java` write the same `java.properties` contract under DTS install root
- [ ] T034 [US3] Commit US3, open PR, pause for review/merge before US4 (or combine US3+US4 if small)

---

## Phase 6: User Story 4 — Unattended install supplies Java home (Priority: P1)

**Goal**: Silent install accepts explicit Java home, validates major 21, writes `java.properties`, fails cleanly on invalid home.  
**Independent Test**: Quickstart Smoke F; tests for `-Dperc.java.home` success/failure without interactive IO.

### Tests (Required)

- [ ] T035 [P] [US4] Unit tests for unattended property/path validation and no write on failure in `modules/perc-distribution-tree/src/test/java/com/percussion/preinstall/java/UnattendedJavaHomeTest.java`

### Implementation

- [ ] T036 [US4] Ensure unattended path honors `-Dperc.java.home=...` (and document any response-file/env alias) with same validation as interactive — `Main.java` / installer docs under `modules/perc-distribution-tree/`
- [ ] T037 [US4] On invalid/missing unattended home: non-zero failure; do not write success config pointing at non-existent `InstallDir/JRE`
- [ ] T038 [US4] Document unattended flags in installer README / `specs/991-system-java-home/quickstart.md` Smoke F
- [ ] T039 [US4] Commit US4, open PR, pause for review/merge before US5/US6

---

## Phase 7: User Story 5 — Operator re-points Java after install (Priority: P2)

**Goal**: Documented post-install change of Java home via `java.properties` and/or env without reinstall; invalid re-point fails clearly.  
**Independent Test**: Edit `java.properties` to second valid 21 home; restart CMS/DTS; confirm effective home; invalid path fails with version/path message.

### Tests

- [ ] T040 [P] [US5] Unit test that updated `java.properties` is preferred over env and install-dir JRE (precedence) in `JavaHomeResolverTest.java` (extend T006)

### Implementation

- [ ] T041 [US5] Finalize re-point docs: edit `java.properties`, restart console; for services re-run install-jetty-service / DTS service install or update `/etc/default` / Procrun — `modules/perc-jetty/README.md`, DTS README, `specs/991-system-java-home/quickstart.md`
- [ ] T042 [US5] Ensure resolve failure messages list attempted sources when config path is invalid after re-point — `resolve-java-home.sh` / `.bat` and Java helper error formatting
- [ ] T043 [US5] Commit US5 (may combine with US6 polish PR)

---

## Phase 8: User Story 6 — Compatibility with existing manual InstallDir/JRE (Priority: P2)

**Goal**: Operator-provided copy/symlink at `InstallDir/JRE` still works as fallback; config/env win; install.xml does not hard-fail when JRE dir absent.  
**Independent Test**: Quickstart Smoke D + G; install upgrade path without JRE folder when `java.properties` present.

### Tests

- [ ] T044 [P] [US6] Unit/structural tests: install-dir JRE used only when higher sources absent; JRE64 after JRE — `JavaHomeResolverTest.java` / script tests
- [ ] T045 [P] [US6] Test or structural assert that `install.xml` JRE backup/`lib/ext` tasks are soft-gated (skip when JRE missing) — e.g. document + optional XPath/string test under `modules/perc-distribution-tree/src/test/java/.../InstallXmlJreSoftGateTest.java`

### Implementation

- [ ] T046 [US6] Soft-gate `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/install.xml` tasks that assume `${install.dir}/JRE` exists (backup, lib/ext copy): skip with log when absent and config Java is in use
- [ ] T047 [US6] Update legacy helpers under `system/release/installer/unix/`, `Linux/`, `windows/` to use resolver or `java.properties` instead of only `./JRE`; replace “Must be version 1.8” messaging with **21**
- [ ] T048 [US6] Migration notes: “from manual copy/symlink under InstallDir/JRE to system/config Java” in `modules/perc-jetty/README.md` and product install docs path used by distribution
- [ ] T049 [US6] Commit US6 + any remaining US5, open PR

---

## Phase 9: Polish & Cross-Cutting Concerns

- [ ] T050 [P] Run full related module tests via `./mvn-env.sh` for `modules/perc-jetty`, `modules/perc-distribution-tree`, and delivery-tier-distribution test modules touched
- [ ] T051 [P] Cross-check `specs/991-system-java-home/quickstart.md` command names match final script filenames
- [ ] T052 [P] Update `modules/perc-jetty/AGENTS.md` with Java home resolution note (alongside systemd notes)
- [ ] T053 Verify Windows and Unix scripts both present for every surface changed (no Unix-only required path)
- [ ] T054 Spotless / format on touched Java; ensure shell scripts use portable constructs and LF where required by packaging
- [ ] T055 Link issue #1340 in PR descriptions; confirm FR-016 (do not re-bundle JRE); close #1340 when final PR merges
- [ ] T056 Optional: `/speckit-analyze` residual check against capability of matrix in contracts vs tasks

---

## Dependencies & Execution Order

```text
Phase 1 Setup
    → Phase 2 Foundational (Java helpers + resolve scripts + tests)
        → Phase 3 US1 CMS Jetty (P1)     [MVP]
        → Phase 4 US2 DTS (P1)           [after US1 merge preferred]
        → Phase 5 US3 Interactive install (P1)
        → Phase 6 US4 Unattended install (P1)  [may combine with US3]
        → Phase 7 US5 Re-point docs (P2)
        → Phase 8 US6 Legacy + install.xml (P2)
        → Phase 9 Polish
```

| Story | Depends on | Blocks |
|-------|------------|--------|
| US1 | Foundational | Preferred before US2 for shared script pattern proof |
| US2 | Foundational (+ US1 pattern recommended) | — |
| US3 | Foundational (Java helpers for write/validate) | US4 can share PR |
| US4 | US3 helpers / same preinstall pipeline | — |
| US5 | US1 (runtime read path) | — |
| US6 | US1 resolver fallback path | — |

## Parallel Execution Examples

```text
# Foundational (after T003–T004):
T005 Java helpers          ||  T007 resolve-java-home.sh  ||  T008 resolve-java-home.bat
T006 helper unit tests     ||  T009 script structural tests

# US1:
T011 StartJetty structural tests  ||  T012 service install Java tests
T014 bat Start/Stop               ||  (after T013 sh StartJetty)

# US2:
T020 Dts script tests  ||  T021 service script tests
T023 TomcatStartup     ||  T024 TomcatShutdown

# US3:
T028 discovery tests  ||  T029 selection tests
# then T030–T033 sequential on preinstall
```

## Implementation Strategy

- **MVP**: Phase 1–2 + **US1** (T001–T019) — CMS starts without manual InstallDir/JRE when `java.properties` or env provides Java 21.  
- **Increment 2**: **US2** DTS parity.  
- **Increment 3**: **US3 + US4** install-time selection/write (interactive + unattended).  
- **Increment 4**: **US5 + US6** re-point docs, legacy fallback proof, install.xml soft-gates, installer helper cleanup.  
- **Polish**: T050–T056, close #1340.

Each story PR: implement → tests pass via `./mvn-env.sh` → commit → PR → resolve review threads → merge before next story (constitution workflow). Stories may be combined only when the PR remains reviewable.

## Task count summary

| Phase | Story | Task IDs | Count |
|-------|-------|----------|-------|
| 1 Setup | — | T001–T002 | 2 |
| 2 Foundational | — | T003–T010 | 8 |
| 3 | US1 | T011–T019 | 9 |
| 4 | US2 | T020–T027 | 8 |
| 5 | US3 | T028–T034 | 7 |
| 6 | US4 | T035–T039 | 5 |
| 7 | US5 | T040–T043 | 4 |
| 8 | US6 | T044–T049 | 6 |
| 9 Polish | — | T050–T056 | 7 |
| **Total** | | **T001–T056** | **56** |

## Format validation

- All tasks use `- [ ]`, sequential IDs, file paths  
- `[P]` only on parallelizable tasks  
- `[USn]` only on user-story phase tasks  
- Tests included (FR-013 + constitution test discipline)  
