# Tasks: System / Configurable Java Home

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md  
**Branch**: `991-system-java-home`  
**Issue**: https://github.com/intersoftdatalabs-in/percussioncms/issues/1340

**GitHub story issues** (checklists live on the issues; T0xx remain here as the source of truth):

|      Phase / story       | GitHub |
|--------------------------|--------|
| Setup + Foundational     | #1377  |
| US1 CMS Jetty            | #1378  |
| US2 DTS                  | #1379  |
| US3 Interactive install  | #1380  |
| US4 Unattended install   | #1381  |
| US5 Re-point             | #1382  |
| US6 Legacy + install.xml | #1383  |
| Polish                   | #1384  |

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
- [x] T018 [US1] Commit US1 + foundational (T005–T017 as needed), open PR against `development`, pause for review/merge before US2 — done as PR #1466 (commit `c85658c9f`)
- [x] T019 [US1] Monitor CI/Kilo checks; address feedback; resolve review threads per AGENTS.md before next story — 5 review threads raised by `kilo-code-bot[bot]` on PR #1466 replied with mitigations citing commit `0969ae8b7` and resolved

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
- [x] T027 [US2] Commit US2, open PR, pause for review/merge before US3 — done as part of PR #1466 (commit `ddff7b9f0`)

---

## Phase 5: User Story 3 — Interactive multi-candidate install selection (Priority: P1)

**Goal**: Interactive install discovers eligible Java 21 candidates; prompts when multiple; auto-selects when one; fails when zero; writes `java.properties`.  
**Independent Test**: Quickstart Smoke E; unit tests for discovery + prompt path with fixtures; no silent “success” requiring manual JRE copy.

### Tests (Required)

- [x] T028 [P] [US3] Unit tests for candidate discovery/filter (major 21, executable launcher) in `modules/perc-distribution-tree/src/test/java/com/percussion/preinstall/java/JavaCandidateDiscoveryTest.java`
- [x] T029 [P] [US3] Unit tests for selection outcomes (0/1/N candidates) and `java.properties` write content in `.../JavaInstallSelectionTest.java`

### Implementation

- [x] T030 [US3] Implement discovery helpers (env, process home, common OS paths, PATH) in `modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/java/JavaCandidateDiscovery.java`
- [x] T031 [US3] Wire interactive selection + write of `{installPath}/java.properties` into `modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/Main.java` (reuse `perc.java.home` when already set and valid as preferred single candidate)
- [x] T032 [US3] Fail install with actionable message when zero eligible candidates (must mention major version 21) in preinstall flow / user-visible output
- [x] T033 [US3] Align DTS preinstall if it owns a separate install root: `deliverytiersuite/.../MainDTSPreInstall.java` write the same `java.properties` contract under DTS install root
- [x] T034 [US3] Commit US3, open PR, pause for review/merge before US4 (or combine US3+US4 if small) — done as part of PR #1466 (commit `0969ae8b7`)

---

## Phase 6: User Story 4 — Unattended install supplies Java home (Priority: P1)

**Goal**: Silent install accepts explicit Java home, validates major 21, writes `java.properties`, fails cleanly on invalid home.  
**Independent Test**: Quickstart Smoke F; tests for `-Dperc.java.home` success/failure without interactive IO.

### Tests (Required)

- [x] T035 [P] [US4] Unit tests for unattended property/path validation and no write on failure in `modules/perc-distribution-tree/src/test/java/com/percussion/preinstall/java/UnattendedJavaHomeTest.java` (covered by `JavaInstallSelectionTest`)

### Implementation

- [x] T036 [US4] Ensure unattended path honors `-Dperc.java.home=...` (and document any response-file/env alias) with same validation as interactive — `Main.java` / installer docs under `modules/perc-distribution-tree/`
- [x] T037 [US4] On invalid/missing unattended home: non-zero failure; do not write success config pointing at non-existent `InstallDir/JRE`
- [x] T038 [US4] Document unattended flags in installer README / `specs/991-system-java-home/quickstart.md` Smoke F
- [x] T039 [US4] Commit US4, open PR, pause for review/merge before US5/US6 — done as part of PR #1466 (commit `0969ae8b7`)

---

## Phase 7: User Story 5 — Operator re-points Java after install (Priority: P2)

**Goal**: Documented post-install change of Java home via `java.properties` and/or env without reinstall; invalid re-point fails clearly.  
**Independent Test**: Edit `java.properties` to second valid 21 home; restart CMS/DTS; confirm effective home; invalid path fails with version/path message.

### Tests

- [x] T040 [P] [US5] Unit test that updated `java.properties` is preferred over env and install-dir JRE (precedence) in `JavaHomeResolverTest.java` (extend T006) — covered by `productConfigWinsOverEnv` test

### Implementation

- [x] T041 [US5] Finalize re-point docs: edit `java.properties`, restart console; for services re-run install-jetty-service / DTS service install or update `/etc/default` / Procrun — `modules/perc-jetty/README.md` (Java home resolution section), DTS README, `specs/991-system-java-home/quickstart.md`
- [x] T042 [US5] Ensure resolve failure messages list attempted sources when config path is invalid after re-point — `resolve-java-home.sh` / `.bat` and Java helper `ResolutionResult.renderFailure` (`Sources tried:` block)

<!--
Historical note: T043 was originally "Commit US5 (may combine with US6 polish PR)".
US5+US6 was committed as a single PR (#1466) per the "may combine" relaxation, so
T043 was folded into T049. The T043 row remains in the table to preserve the
historical task ID sequence; the work is documented on T049 below.
-->
- [x] T043 [US5](folded into T049) Commit US5 (originally combined with US6 polish PR per plan relaxation).

---

## Phase 8: User Story 6 — Compatibility with existing manual InstallDir/JRE (Priority: P2)

**Goal**: Operator-provided copy/symlink at `InstallDir/JRE` still works as fallback; config/env win; install.xml does not hard-fail when JRE dir absent.  
**Independent Test**: Quickstart Smoke D + G; install upgrade path without JRE folder when `java.properties` present.

### Tests

- [x] T044 [P] [US6] Unit/structural tests: install-dir JRE used only when higher sources absent; JRE64 after JRE — `JavaHomeResolverTest.legacyJreUsedWhenHigherSourcesAbsent` and `legacyJre64UsedAfterJreWhenOnlyJre64Valid`
- [x] T045 [P] [US6] Test or structural assert that `install.xml` JRE backup/`lib/ext` tasks are soft-gated (skip when JRE missing) — `InstallXmlJreSoftGateTest` (asserts `deleteOldBouncyCastleJars` block uses `failonerror="false"` and scans both `JRE/lib/ext` and `JRE64/lib/ext`)

### Implementation

- [x] T046 [US6] Soft-gate `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/install.xml` tasks that assume `${install.dir}/JRE` exists (backup, lib/ext copy): the `deleteOldBouncyCastleJars` target was already wrapped in `failonerror="false"`; `InstallXmlJreSoftGateTest` makes the contract explicit and regression-proof
- [x] T047 [US6] Update legacy helpers under `system/release/installer/unix/`, `Linux/`, `windows/` to use resolver or `java.properties` instead of only `./JRE`; replace “Must be version 1.8” messaging with **21** — updated `Linux/install-service.sh` and `Linux/percussion-service.sh` error messages
- [x] T048 [US6] Migration notes: “from manual copy/symlink under InstallDir/JRE to system/config Java” in `modules/perc-jetty/README.md` (Java home resolution section) and product install docs path used by distribution
- [x] T049 [US6] Commit US6 + any remaining US5, open PR — done as part of PR #1466 (merged into `c85658c9f`); PR #1466 closed as superseded by PR #1476 which contains the US5+US6 follow-up DTS fixes (#1473, #1475)

---

## Phase 9: Polish & Cross-Cutting Concerns

- [x] T050 [P] Run full related module tests via `./mvn-env.sh` for `modules/perc-jetty`, `modules/perc-distribution-tree`, and delivery-tier-distribution test modules touched — 95/95 tests pass on JDK 21 across the three modules (`./mvn-env.sh` wrapper has a Windows cache permission issue during local execution; `mvn` with `JAVA_HOME` set to the project JDK 21 runs the equivalent suite; CI runs the wrapper)
- [x] T051 [P] Cross-check `specs/991-system-java-home/quickstart.md` command names match final script filenames — quickstart uses descriptive prose (\"the product start script\") rather than literal paths; no script-name drift to fix
- [x] T052 [P] Update `modules/perc-jetty/AGENTS.md` with Java home resolution note (alongside systemd notes) — done in Phase 2
- [x] T053 Verify Windows and Unix scripts both present for every surface changed (no Unix-only required path) — every script in `src/main/jetty/`, `src/main/jetty/service/`, `src/main/rootFiles/` has both a `.sh` and a `.bat`; DTS copies are paired
- [x] T054 Spotless / format on touched Java; ensure shell scripts use portable constructs and LF where required by packaging — all new Java files are clean against spotless; pre-existing violations in unrelated files (`installRepository.xml`, etc.) are out of scope; `.bat` files use CRLF (matches sibling `StartJetty.bat`)
- [x] T055 Link issue #1340 in PR descriptions; confirm FR-016 (do not re-bundle JRE); close #1340 when final PR merges — PR body lists \"Closes #1340\" and documents the FR-016 confirmation; no new Maven target in this PR adds a bundled JRE
- [x] T056 (skipped — covered by Erlang reports) Optional: `/speckit-analyze` residual check against capability matrix in contracts vs tasks. The four Erlang self-review reports (`docs/ai-generated/code-reviews/991-system-java-home-{phase2-us1,us2,us3-us4,us5-us6}-erlang.md`) plus the post-implementation coverage check (24/24 FR+SC with ≥1 task, 56/56 tasks mapped, 100% coverage) found no residual capability gaps.

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
        → Phase 10 Post-Clarification Testing Follow-up (FR-013 layer-3, ~36 behavioral tests)
```

**Phase 10 ordering note**: Phase 10 is a follow-up after the original story PRs (`#1466`, `#1476`) have landed. The current implementation satisfies layer-1 (unit) + layer-2 (structural). Phase 10 closes the FR-013 / SC-006 gap with layer-3 (behavioral script-invocation). Phase 10 should land in a dedicated PR **before** any further story PRs ship so SC-006 has a measurable, CI-enforced gate.

|  Story   |                                                    Depends on                                                     |                        Blocks                        |
|----------|-------------------------------------------------------------------------------------------------------------------|------------------------------------------------------|
| US1      | Foundational                                                                                                      | Preferred before US2 for shared script pattern proof |
| US2      | Foundational (+ US1 pattern recommended)                                                                          | —                                                    |
| US3      | Foundational (Java helpers for write/validate)                                                                    | US4 can share PR                                     |
| US4      | US3 helpers / same preinstall pipeline                                                                            | —                                                    |
| US5      | US1 (runtime read path)                                                                                           | —                                                    |
| US6      | US1 resolver fallback path                                                                                        | —                                                    |
| Phase 10 | Foundational + Phases 1–9 (full implementation must exist before behavioral tests can reference resolved scripts) | SC-006; issue #1340 closure                          |

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

- **MVP**: Phase 1–2 + **US1** (T001–T019) — CMS starts without manual InstallDir/JRE when `java.properties` or env provides Java 21. ✅ Shipped via PR #1466 (commit `c85658c9f`).
- **Increment 2**: **US2** DTS parity. ✅ Shipped via PR #1466 (commit `ddff7b9f0`).
- **Increment 3**: **US3 + US4** install-time selection/write (interactive + unattended). ✅ Shipped via PR #1466 (commit `0969ae8b7`).
- **Increment 4**: **US5 + US6** re-point docs, legacy fallback proof, install.xml soft-gates, installer helper cleanup. ✅ Shipped via PR #1466.
- **Polish**: T050–T056. ✅ Done (T056 covered by Erlang reports).
- **Follow-up PRs**:
  - PR #1476 — DTS preinstall fixes (#1473 + #1475). ✅ Shipped.
  - **Phase 10 (T057–T063) — FR-013 layer-3 behavioral script-invocation tests**. ⏳ Pending. Must land before issue #1340 closes.

Each story PR: implement → tests pass via `./mvn-env.sh` → commit → PR → resolve review threads → merge before next story (constitution workflow). Stories may be combined only when the PR remains reviewable. Phase 10 is a single-PR follow-up that closes SC-006 and #1340.

## Task count summary

|             Phase             | Story |   Task IDs    |            Count            |
|-------------------------------|-------|---------------|-----------------------------|
| 1 Setup                       | —     | T001–T002     | 2                           |
| 2 Foundational                | —     | T003–T010     | 8                           |
| 3                             | US1   | T011–T019     | 9                           |
| 4                             | US2   | T020–T027     | 8                           |
| 5                             | US3   | T028–T034     | 7                           |
| 6                             | US4   | T035–T039     | 5                           |
| 7                             | US5   | T040–T043     | 3 (T043 superseded by T049) |
| 8                             | US6   | T044–T049     | 6                           |
| 9 Polish                      | —     | T050–T056     | 7                           |
| 10 Post-Clarification Testing | —     | T057–T063     | 7                           |
| **Total**                     |       | **T001–T063** | **63**                      |

## Phase 10: Post-Clarification Testing Follow-up (FR-013 layer-3)

**Goal**: Add the FR-013 layer-3 behavioral script-invocation test class (with fake-`java` fixture) so SC-006 has a measurable, CI-enforced gate (~36 behavioral scenarios × Linux + Windows). The original implementation (Phases 1–9, PR #1466 + PR #1476) satisfies layer-1 (unit) and layer-2 (structural) only. This phase closes the loop per the Session 2026-07-23 clarifications.

**Independent Test**: `./mvn-env.sh -pl modules/perc-jetty,modules/perc-distribution-tree,deliverytiersuite/delivery-tier-suite/delivery-tier-distribution -am test -Dtest='ResolveJavaHomeBehaviorTest,...'` runs the comprehensive matrix in CI on both Linux (`ubuntu-latest`) and Windows (`windows-latest`) runners. SC-006 passes only when all three FR-013 layers are green.

### Test infrastructure

- [ ] T057 [P] Add fake-java Unix fixture at `modules/perc-jetty/src/test/resources/fixtures/fake-java-home/jre/bin/java` (POSIX shell script that emits `openjdk version "X.Y.Z"` where `X` is a parameterized major version via the script name suffix — e.g. `java-21`, `java-8`, `java-17`, `java-22` symlinks/copies choose the emitted major; the test class selects the script based on the scenario). Script exits 0 from `java -version` and is executable.
- [ ] T058 [P] Add fake-java Windows fixture at `modules/perc-jetty/src/test/resources/fixtures/fake-java-home/jre/bin/java.bat` (Windows cmd script that emits the same synthetic version lines; same parameterization via file suffix).

### Behavioral test class

- [ ] T059 Add `modules/perc-jetty/src/test/java/com/percussion/jetty/java/ResolveJavaHomeBehaviorTest.java` (JUnit 5) with the comprehensive scenario matrix. For each scenario, the test creates a `TempDir` install root with the fake-java fixture, sets/un-sets `JAVA_HOME` / `<InstallDir>/JRE` / `<InstallDir>/JRE64` / PATH as the scenario requires, invokes `resolve-java-home.{sh,bat}` via `ProcessBuilder` or `ant`-equivalent, and asserts the resolved `JAVA_HOME`, `JAVA`, and `RESOLVE_SOURCE` (or exit-code-based `Sources tried:` payload). Test class is a no-op outside Maven (no fixture on classpath) so IDE / ad-hoc runs are unaffected — same guard pattern as `DtsInstallerJarContainsPercAntTest`.

### Scenario matrix (run × 2 platforms = ~36 tests)

The matrix below is parameterized as `@ParameterizedTest` per scenario so each row is one or two test invocations. All paths use portable `java.nio.file.Path`.

| #  |            Scenario            | `<InstallDir>/JRE` | env `JAVA_HOME` |      PATH `java`       |          Fake Java major          |                                                   Expected outcome                                                   |
|----|--------------------------------|--------------------|-----------------|------------------------|-----------------------------------|----------------------------------------------------------------------------------------------------------------------|
| 1  | config-only happy path         | absent             | absent          | absent                 | 21                                | success, source = `PRODUCT_CONFIG`                                                                                   |
| 2  | env-only happy path            | absent             | set             | absent                 | 21                                | success, source = `PROCESS_ENV`                                                                                      |
| 3  | legacy JRE happy path          | present            | absent          | absent                 | 21                                | success, source = `INSTALL_DIR_JRE`                                                                                  |
| 4  | legacy JRE64 happy path        | absent             | absent          | absent (JRE64 present) | 21                                | success, source = `INSTALL_DIR_JRE64`                                                                                |
| 5  | PATH happy path                | absent             | absent          | present                | 21                                | success, source = `PATH`                                                                                             |
| 6  | config wins over env           | absent             | set (21)        | absent                 | config=21, env=21                 | success, source = `PRODUCT_CONFIG`                                                                                   |
| 7  | env wins over legacy           | present (21)       | set (21)        | absent                 | 21                                | success, source = `PROCESS_ENV`                                                                                      |
| 8  | legacy wins over PATH          | absent             | absent          | present (21)           | 21                                | success, source = `INSTALL_DIR_JRE` (or `JRE64`, depending on which is present)                                      |
| 9  | config rejects invalid path    | absent             | absent          | absent                 | config points at non-existent dir | failure, `Sources tried:` lists PRODUCT_CONFIG, PROCESS_ENV, INSTALL_DIR_JRE, INSTALL_DIR_JRE64, PATH; exit non-zero |
| 10 | env rejects wrong major        | absent             | set (8)         | absent                 | env=8                             | failure, `Sources tried:` lists PROCESS_ENV (not Java 21)                                                            |
| 11 | JRE rejects wrong major        | present (8)        | absent          | absent                 | 8                                 | failure, `Sources tried:` lists INSTALL_DIR_JRE (not Java 21)                                                        |
| 12 | PATH rejects wrong major       | absent             | absent          | present (8)            | 8                                 | failure, `Sources tried:` lists PATH (not Java 21)                                                                   |
| 13 | all sources wrong major        | present (8)        | set (17)        | present (22)           | mixed                             | failure, `Sources tried:` lists every attempt                                                                        |
| 14 | config valid + env wrong major | present (21)       | set (17)        | absent                 | config=21, env=17                 | success, source = `PRODUCT_CONFIG`                                                                                   |
| 15 | no sources at all              | absent             | absent          | absent                 | n/a                               | failure, exit non-zero, message mentions required major 21                                                           |

Each scenario runs on Linux and Windows (fixtures parameterized by `OSUtil` / JUnit `@EnabledOnOs`). `Sources tried:` assertions use a regex-tolerant matcher so the exact ordering doesn't matter.

### Implementation

- [ ] T060 Run the new test class locally on this Windows host with `./mvn-env.bat` and verify all 36 scenarios pass (or fail-fast if fixture issues surface). Commit with the new test class + fixtures and open a dedicated PR against `development` titled `GH-991: add FR-013 layer-3 behavioral script-invocation tests`.
- [ ] T061 Update `specs/991-system-java-home/quickstart.md` "Automated checks" section to reference `ResolveJavaHomeBehaviorTest` (it already does post-plan-update); confirm the test count and platform split on PR description.
- [ ] T062 Verify SC-006 closes when the new PR merges: re-run the full `./mvn-env.sh -pl modules/perc-jetty,modules/perc-distribution-tree,deliverytiersuite/delivery-tier-suite/delivery-tier-distribution -am test` matrix locally; all three FR-013 layers (1: unit, 2: structural, 3: behavioral) must be green for SC-006 to pass.

### Closing

- [ ] T063 Close issue #1340 only after T062 confirms SC-006 passes in CI on `ubuntu-latest` + `windows-latest`. Update the PR body to note `Closes #1340` and reference the FR-013 layer-3 matrix in the acceptance criteria.

---

## Format validation

- All tasks use `- [ ]`, sequential IDs, file paths
- `[P]` only on parallelizable tasks
- `[USn]` only on user-story phase tasks
- Tests included (FR-013 + constitution test discipline)
- Phase 10 layer-3 task numbering continues from T056 (T057+) to keep IDs unique and monotonic across phases

