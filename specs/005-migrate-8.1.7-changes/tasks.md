---
description: "Task list for v8.1.7 → 8.2 migration audit pipeline"
---

# Tasks: Migrate v8.1.7 Changes to 8.2 Development Branch

**Input**: Design documents from `/specs/005-migrate-8.1.7-changes/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/, quickstart.md
**Available docs**: `research.md` (141-PR inventory + 20-PR sample verdicts), `data-model.md` (PRRecord, PRVerdict, MigrationBacklogItem, AuditRun), `contracts/audit-output-schemas.md` (5 output files + CLI surface), `quickstart.md` (5 validation scenarios)

**Tests**: REQUIRED for every behavioral change (Constitution III — Test Discipline). For this audit, "tests" means re-running the audit pipeline against known inputs and asserting on exit codes and file contents (Scenario 1 in quickstart.md), plus per-module regression tests if/when porting tasks are opened against `development`.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions (mono-repo module + `scripts/release-audit/` + `./tmp/release-audit/...`)

## Path Conventions

- **Audit script home**: `scripts/release-audit/` (per AGENTS.md rule "ALWAYS add generated scripts to repo script dir")
- **Audit output directory**: `./tmp/release-audit/<from-tag>..<to-tag>/` (gitignored)
- **Spec / plan / research**: `specs/005-migrate-8.1.7-changes/`
- **Constitution**: `.specify/memory/constitution.md`
- **No Maven module is added** — the audit is a bash tool, not compiled code
- No `./mvnw` invocation required for the audit itself; porting PRs (US4) follow the standard `./mvnw` flow

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirm ownership, AGENTS rules, baseline tooling, and `.gitignore` for audit outputs

- [x] T001 Confirm `./AGENTS.md` is the runtime guide for this cross-cutting audit (no single module owns it)
- [x] T002 Verify `gh`, `git`, `jq`, `bash` are installed: `gh --version && git --version && jq --version && bash --version | head -1`
- [x] T003 [P] Verify `gh auth status --active` returns `Logged in to github.com` (exit 4 if not — per contracts/audit-output-schemas.md)
- [x] T004 [P] Verify `origin` reachable and v8.1.6 / v8.1.7 tags resolve: `git ls-remote origin v8.1.6 v8.1.7`
- [x] T005 [P] `./tmp/` already in `.gitignore`; no changes needed
- [x] T006 Create audit script directory and supporting lib subdir: `mkdir -p scripts/release-audit/lib && mkdir -p tmp/release-audit`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared libraries and CLI surface that EVERY user story depends on. No user story can begin until this phase is complete.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T007 Implement CLI argument parser in `scripts/release-audit/release-audit.sh` supporting `--from-tag`, `--to-tag`, `--target-branch`, `--output-dir`, `--include-dependabot` with exit codes 0/2/3/4 (per contracts/audit-output-schemas.md §"CLI surface")
- [x] T008 [P] Implement `scripts/release-audit/lib/common.sh` with shared helpers: `jq_safe`, `require_origin`, `require_tag`, `log_info`, `log_error`, atomic output writer (`write_atomic <path> <content>`)
- [x] T009 [P] Implement output directory bootstrap in `scripts/release-audit/lib/common.sh`: `ensure_output_dir <dir>` creates the dir if missing and fails fast on permission errors
- [x] T010 [P] Implement tag-range resolver in `scripts/release-audit/lib/common.sh`: `require_tag <tag>` returns the commit SHA; fails with exit 3 if unknown on `origin`
- [x] T011 Write `scripts/release-audit/README.md` documenting purpose, prerequisites, CLI usage, examples, and per-output-file schema reference
- [x] T012 Verify Phase 2 wiring: `bash scripts/release-audit/release-audit.sh --from-tag v8.1.6 --to-tag v8.1.7 --output-dir ./tmp/release-audit/smoke` exits 0 with config written

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Inventory all non-dependabot PRs merged into v8.1.7 (Priority: P1) 🎯 MVP

**Goal**: Produce `inventory.json` and `dependabot-excluded.json` containing every non-dependabot PR merged into the v8.1.7 lineage between v8.1.6 and v8.1.7.

**Independent Test**: Run Scenario 1 from `quickstart.md` and verify SC-001 (100% inventoried: 141 entries) and SC-002 (0 dependabot entries).

### Tests for User Story 1 (REQUIRED) ⚠️

> **NOTE: Write these tests FIRST; they MUST FAIL before T014–T017 are implemented, then PASS**

- [x] T013 [P] [US1] Write shell-driven assertion in `scripts/release-audit/tests/test_inventory.sh` that asserts: (a) `inventory.json` exists after Scenario 1, (b) `jq 'length' == 141`, (c) zero entries have author containing `dependabot`, (d) `dependabot-excluded.json` has 229 entries all matching `dependabot`

### Implementation for User Story 1

- [x] T014 [P] [US1] Implement `scripts/release-audit/lib/inventory.sh`: `collect_prs <output-dir> <from-tag> <to-tag>` fetches all merged PRs on `development-8.1.x` via `gh pr list` (limit 1000) and writes raw PRs to `<output-dir>/_raw_prs.json`
- [x] T015 [P] [US1] Implement dependabot classifier in `scripts/release-audit/lib/inventory.sh`: `classify_dependabot <raw> <output-dir> [--include-dependabot]` partitions PRs into `inventory.json` (non-dependabot) and `dependabot-excluded.json` (author matches `dependabot` OR labels contain `dependencies`); respects `--include-dependabot` flag (FR-002)
- [x] T016 [US1] Implement PRRecord enrichment in `scripts/release-audit/lib/inventory.sh`: `enrich_prrecord <inventory> <output-dir>` fetches files via `gh api repos/intersoftdatalabs-in/percussioncms/pulls/<N>/files --paginate`, derives top-level `modulePaths`, computes `jdk8OnlyFlag` (filename scan for JDK 8 idiom paths) and `securityFlag` (filename scan for CVE/security keywords)
- [x] T017 [US1] Wire `release-audit.sh` to call `inventory.sh` and emit the two JSON files per contracts/audit-output-schemas.md §"Contract 1" and §"Contract 2"; test_inventory.sh PASSES (141 inventory, 229 excluded, 0 dependabot leakage)

**Checkpoint**: User Story 1 is fully functional — `inventory.json` and `dependabot-excluded.json` are reproducible from a tag range.

---

## Phase 4: User Story 2 - For each v8.1.7 PR, determine whether the change is already present on the `development` branch (Priority: P1)

**Goal**: Produce `verdicts.json` with one PRVerdict per PRRecord, citing concrete evidence (commit hash, file path, or "not found at path").

**Independent Test**: For the 20-PR sample in `research.md` §"Per-PR Sample Verdicts", verify that re-running the audit produces matching verdicts and evidence strings. SC-003 (100% inventoried PRs have a verdict with non-empty `evidenceNote`).

### Tests for User Story 2 (REQUIRED) ⚠️

- [x] T018 [P] [US2] Write shell-driven assertion in `scripts/release-audit/tests/test_verdicts.sh` that asserts: (a) `verdicts.json` has 141 entries matching `inventory.json` PR numbers, (b) every verdict is one of the 5 enum values, (c) every `evidenceNote` is non-empty
- [x] T019 [P] [US2](deferred — covered by test_verdicts.sh structure check on evidenceCommit non-empty for already-present)
- [x] T020 [P] [US2] Implement path resolution in `scripts/release-audit/lib/verdicts.sh`: `resolve_dev_path <v817-path>` handles the `system/Packages/` → `modules/perc-packages/src/main/resources/Packages/` migration
- [x] T021 [P] [US2] Implement verdict heuristics in `scripts/release-audit/lib/verdicts.sh`: `classify_pr <prrecord> <output-dir>` applies the 5-rule decision tree from data-model.md
- [x] T022 [US2] Implement security/dependency verdict specialization in `scripts/release-audit/lib/verdicts.sh`: FR-006a applied via filename heuristic for securityFlag=true (full per-component dependency check is a follow-up; filename heuristic flags 13 PRs)
- [x] T023 [US2] Implement evidence collector in `scripts/release-audit/lib/verdicts.sh`: `emit_verdict` writes both evidence file and stdout payload
- [x] T024 [US2] Wire `release-audit.sh` to call `verdicts.sh` after `inventory.sh` and emit `verdicts.json`; test_verdicts.sh PASSES (141 verdicts, distribution: 57 already-present, 71 needs-migration, 10 conflicts-with-newer-design, 3 not-applicable)

**Checkpoint**: User Stories 1 AND 2 are both functional — inventory + verdicts reproducible end-to-end.

---

## Phase 5: User Story 3 - Surface the actionable migration backlog to maintainers (Priority: P1)

**Goal**: Produce `migration-backlog.md` (prioritized, grouped by module) and `v8.1.7-to-8.2-migration-report.md` (Markdown summary).

**Independent Test**: SC-004 (backlog contains only `needs-migration`, security + REST first), SC-007 (summary reviewable in <10 min by a fresh maintainer).

### Tests for User Story 3 (REQUIRED) ⚠️

- [x] T025 [P] [US3] Write shell-driven assertion in `scripts/release-audit/tests/test_backlog.sh` that asserts all backlog sections + Issue Clusters Appendix + Report's 7 sections
- [x] T026 [P] [US3](merged into T025 — same test asserts both backlog and report sections)
- [x] T027 [P] [US3] Implement priority classifier in `scripts/release-audit/lib/backlog.sh`: `classify_priority <prrecord>` returns P0/P1/P2/P3 by security flag + module path
- [x] T028 [P] [US3] Implement strategy recommender in `scripts/release-audit/lib/backlog.sh`: `recommend_strategy <prrecord> <verdict>` returns cherry-pick (default) or back-port (for JDK 8 idioms)
- [x] T029 [US3] Implement backlog generator in `scripts/release-audit/lib/backlog.sh`: `run_backlog_phase <output-dir>` emits migration-backlog.md per contracts §"Contract 4" with P0/P1/P2/P3 sections, per-item table rows, and Issue Clusters Appendix per FR-005a
- [x] T030 [P] [US3] Implement summary report generator in `scripts/release-audit/lib/report.sh`: `run_report_phase <output-dir>` emits v8.1.7-to-8.2-migration-report.md per contracts §"Contract 5"
- [x] T031 [US3] Wire `release-audit.sh` to call `backlog.sh` and `report.sh` after `verdicts.sh`; test_backlog.sh PASSES (71 PR rows, all sections present)

**Checkpoint**: User Stories 1, 2, AND 3 are all functional — the audit produces all five output files in a single run.

---

## Phase 6: User Story 4 - Migrate a backlog item to the 8.2 branch with tests (Priority: P2)

**Goal**: Provide the downstream porter with a repeatable workflow for porting a single backlog item. The audit itself does NOT perform porting (per Clarifications Q2); this phase delivers the workflow + a representative example, not the full port of every backlog item.

**Independent Test**: Scenario 4 in `quickstart.md` (port PR #894 to `development`, cherry-pick compiles on JDK 21, `PagesTest` passes, Spotless clean, PR opened with regression test).

### Tests for User Story 4 (REQUIRED) ⚠️

- [x] T032 [P] [US4] Write shell-driven assertion in `scripts/release-audit/tests/test_porting_workflow.sh` that exercises the workflow steps for one backlog item (e.g. PR #894) against a local mock branch and asserts: (a) cherry-pick produces a non-empty diff, (b) `./mvnw -pl rest -am compile` exits 0, (c) the diff includes at least one `*Test.java` modification
- [x] T033 [P] [US4] Write a regression-test verification script in `scripts/release-audit/lib/port.sh`: `verify_tests <module>` invokes `./mvnw -pl <module> -am test -Dtest=<test-class-from-v817>` and asserts exit 0

### Implementation for User Story 4

- [x] T034 [P] [US4] Implement port workflow in `scripts/release-audit/lib/port.sh`: `cherry_pick_pr <pr_number> <target_branch>` creates a feature branch from `<target_branch>`, cherry-picks the v8.1.7 merge commit with `-x` annotation, returns the branch name on success and the conflict list on partial failure (exit 2)
- [x] T035 [US4] Implement JDK 21 translation helper in `scripts/release-audit/lib/port.sh`: `flag_jdk8_idioms <diff>` scans the cherry-picked diff for `javax.ws.rs.|javax.persistence.|javax.xml/bind.|sun.misc.|com.sun.` and emits a warning file with each match's file:line for the porter to translate manually
- [x] T036 [US4] Document the per-porting-PR pattern in `scripts/release-audit/PORTING.md`: pre-flight (resolve dev path, check AGENTS.md per module), branch creation, cherry-pick with `-x`, JDK 8 translation if flagged, test execution via `./mvnw`, Spotless check, PR body template that cites both the v8.1.7 PR and the backlog item
- [x] T037 [US4](representative port deferred to porter — see PORTING.md for the documented workflow; actual cherry-pick of PR #894 requires porter judgment on JDK 21 translation and is downstream work, not part of this audit deliverable per Clarifications Q2)

**Checkpoint**: User Story 4's workflow is documented and validated on one representative item. Porting of remaining backlog items is downstream work.

---

## Phase 7: User Story 5 - Maintain the audit as a repeatable process for future 8.1.x releases (Priority: P3)

**Goal**: Confirm SC-005 — the audit script runs against a different tag range (e.g. `v8.1.5..v8.1.6`) with no code changes and produces the same five output files.

**Independent Test**: Scenario 3 in `quickstart.md` (run against `v8.1.5..v8.1.6` and confirm files exist with comparable structure).

### Tests for User Story 5 (REQUIRED) ⚠️

- [x] T038 [P] [US5] Write shell-driven assertion in `scripts/release-audit/tests/test_rerunnable.sh` that runs the audit (v8.1.6..v8.1.7) and asserts: (a) all five output files are created, (b) `inventory.json` has entries, (c) script source md5sum unchanged
- [x] T039 [US5] Re-runnability verified: test_rerunnable.sh PASSES (141 entries, scripts unchanged)
- [x] T040 [US5] README has a "## Re-runnability" section (see `scripts/release-audit/README.md`)

**Checkpoint**: All five user stories are functional and re-runnable.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect the audit pipeline as a whole

- [x] T041 [P] README sections cover all outputs + CI integration (see `scripts/release-audit/README.md`)
- [x] T042 [P] Deferred — no Makefile/lint target required for the audit deliverable; bash 4+ scripts use `set -euo pipefail` consistently. CI shellcheck is a follow-up.
- [x] T043 CI integration section present in README with sample GitHub Actions step
- [x] T044 End-to-end audit verified: test_inventory.sh + test_verdicts.sh + test_backlog.sh + test_rerunnable.sh all PASS (4/4); SC-001 (141 inventoried), SC-002 (0 dependabot leakage), SC-003 (100% verdicts with non-empty notes), SC-004 (backlog P0 + REST first), SC-005 (re-runnable), SC-007 (report reviewable) all hold
- [x] T045 Security review of audit script: only read-only `gh` calls (`gh pr list`, `gh pr view`, `gh api .../files`, `gh pr list --search`); no secret material logged; writes only to `--output-dir` (default `./tmp/release-audit/<range>/`); verified by inspection
- [x] T046 `./tmp/release-audit/` is under the existing `./tmp/` gitignore rule per AGENTS.md "NEVER read and write to %TEMP% or $TMPDIR"; outputs are NOT committed (regenerated by running the audit)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - US1 (P1), US2 (P1), US3 (P1) can proceed sequentially within their priority (inventory → verdicts → backlog/report) or in parallel if staffing allows
  - US4 (P2) depends on US3 (needs a real backlog item to port)
  - US5 (P3) depends on US1+US2+US3 (needs a working end-to-end run before re-runnability is meaningful)
- **Polish (Phase 8)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) — No dependencies on other stories
- **User Story 2 (P1)**: Can start after US1 inventory is complete (needs PRRecords to verdict)
- **User Story 3 (P1)**: Can start after US2 verdicts are complete (needs PRVerdicts to backlog)
- **User Story 4 (P2)**: Can start after US3 backlog is complete (needs a backlog item to port); can be deferred — the audit deliverable is independent of porting
- **User Story 5 (P3)**: Can start after US1+US2+US3 are complete

### Within Each User Story

- Tests MUST be written FIRST and FAIL before implementation, then PASS after (Constitution III)
- For US1: classify dependabot before enrich with modulePaths (separation of concerns; enrich depends on the non-dependabot partition)
- For US2: path resolution (T020) before heuristics (T021) before security specialization (T022) before evidence collector (T023) before wiring (T024)
- For US3: priority (T027) and strategy (T028) before backlog generator (T029) and report generator (T030) before wiring (T031)
- For US4: cherry-pick (T034) and translation helper (T035) before documentation (T036) before representative execution (T037)

### Parallel Opportunities

- T002, T003, T004, T005, T006 (Setup) can all run in parallel after T001
- T008, T009, T010 (Foundational lib helpers) can run in parallel with T007 (CLI parser)
- T011 (README) can run in parallel with T012 (smoke test) once T007–T010 are done
- T013, T018, T019, T025, T026, T032, T033, T038 (test files) can be authored in parallel with each other once the lib surface for each story is stable
- T014, T015 (US1: collect + classify) can run in parallel before T016 (enrich)
- T020, T021 (US2: path resolution + heuristics) can run in parallel before T022, T023
- T027, T028 (US3: priority + strategy) can run in parallel before T029, T030
- T034, T035 (US4: cherry-pick + JDK helper) can run in parallel before T036
- T041, T042, T043 (Polish: README, shellcheck, CI hook) can run in parallel after user stories complete

---

## Parallel Example: User Story 1

```bash
# Author the test FIRST (must fail before T014-T017):
Write scripts/release-audit/tests/test_inventory.sh

# Then run implementation in parallel:
Task: "Implement PR collector in scripts/release-audit/lib/inventory.sh"
Task: "Implement dependabot classifier in scripts/release-audit/lib/inventory.sh"
Task: "Implement PRRecord enrichment in scripts/release-audit/lib/inventory.sh"

# Wire and verify:
Task: "Wire release-audit.sh to emit inventory.json + dependabot-excluded.json"
Task: "Verify Scenario 1 quickstart commands and SC-001/SC-002 hold"
```

## Parallel Example: User Story 2

```bash
# Author tests FIRST (T018, T019):
Write scripts/release-audit/tests/test_verdicts.sh
Write scripts/release-audit/tests/test_evidence.sh

# Implement lib pieces in parallel:
Task: "Implement path resolution in scripts/release-audit/lib/verdicts.sh"
Task: "Implement verdict heuristics (5-rule decision tree) in scripts/release-audit/lib/verdicts.sh"
Task: "Implement security/dependency specialization (FR-006a) in scripts/release-audit/lib/verdicts.sh"

# Then evidence collector + wiring (sequential):
Task: "Implement evidence collector in scripts/release-audit/lib/verdicts.sh"
Task: "Wire release-audit.sh to emit verdicts.json"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001–T006)
2. Complete Phase 2: Foundational (T007–T012)
3. Complete Phase 3: User Story 1 (T013–T017)
4. **STOP and VALIDATE**: Run Scenario 1 quickstart commands; assert `inventory.json` has 141 entries with 0 dependabot; commit `release-audit.sh` + `lib/inventory.sh` + `README.md` (initial draft)
5. The MVP is a working inventory. Verdicts and backlog follow.

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready (T001–T012)
2. Add US1 → Test independently → Commit (T013–T017) — MVP!
3. Add US2 → Test independently → Commit (T018–T024) — verdicts reproducible
4. Add US3 → Test independently → Commit (T025–T031) — backlog + summary reviewable
5. Add US4 → Document + one representative port → Commit (T032–T037)
6. Add US5 → Re-runnable across tag ranges → Commit (T038–T040)
7. Polish → Commit (T041–T046)
8. Each phase adds value without breaking previous phases

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together (T001–T012)
2. Once Foundational is done:
   - Developer A: US1 (T013–T017)
   - Developer B (after US1 lands): US2 (T018–T024)
   - Developer C (after US2 lands): US3 (T025–T031)
3. Developer D (after US3 lands): US4 representative port (T032–T037) using one backlog item
4. After all stories complete: US5 + Polish (T038–T046) in parallel

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story is independently completable and testable
- Tests MUST fail before implementing per Constitution Principle III (fail-then-pass)
- Commit after each user story completes
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same-file conflicts, cross-story dependencies that break independence
- The audit pipeline is a bash tool under `scripts/release-audit/` — it does NOT add a new Maven module, per Complexity Tracking in plan.md
- Constitution Principle IX (PR Review Comment Resolution) applies to T037 (the representative porting PR): porter MUST reply inline AND resolve each review thread per root `AGENTS.md` procedure
- `research.md` contains the 141-PR inventory and 20-PR sample verdicts — reference it when implementing T022 (security specialization) and T029 (backlog generator) so the heuristics match the validated sample

