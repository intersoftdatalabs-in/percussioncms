# Tasks: Content Explorer Increments Acceptance and Closure

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md), [checklists/requirements.md](checklists/requirements.md)
**Branch**: `4530-content-explorer-increments`
**Feature directory**: `specs/009-content-explorer-increments`
**Issue**: Epic [#4530](https://github.com/intersoftdatalabs-in/percussioncms/issues/4530)

**Tests**: Constitution III mandates a regression test for the #6119 backend fix (T017); acceptance runs are recorded evidence per `contracts/acceptance-run.md`, not code tests. Existing suites are re-executed, not rewritten.

**WP0 rule (user-directed)**: the Phase 2 foundational freeze (T004–T008) sizes and bounds everything downstream — no story work begins until the four inventories are frozen.

---

## Phase 1: Setup

- [x] T001 Read this spec package plus the #4530 body table, issue #6119, and module AGENTS hierarchy (`WebUI/`, `rest/`, `projects/sitemanage/`, `modules/perc-qa-automation/`, search service owner)
- [x] T002 Confirmed branch `4530-content-explorer-increments` rebased onto current origin/main; H2 QA-cell access verified via multiple qa-up/qa-health cycles `4530-content-explorer-increments` is based on current `main`; confirm H2 QA-cell access for acceptance runs
- [x] T003 [P] Re-read `specs/2400-dce-explorer-parity/contracts/gap-matrix.md` (no-regression baseline for FR-007) and the #5990/#5991/#5992 closeout notes (goal-trio done-state)

---

## Phase 2: Foundational — WP0 Baseline Freeze (Blocking Prerequisites)

**Goal**: Frozen inventories for goal-trio state, QA list, merge rows, and #6119 repro — SC-002/SC-004 are counts and cannot be evaluated against a moving set.
**Blocks**: All user stories. **No story work begins until T004–T008 are frozen.**

- [x] T004 Freeze goal-trio state: confirm takedown/stage/schedule done-on-main standing (#5990/#5991/#5992 + re-verify notes) in `specs/009-content-explorer-increments/checklists/baseline-freeze.md`
- [x] T005 Freeze the QA ticket list under the strict-plus-named rule (#4553, #5799, spawned-by-merge QA, maintainer-named additions) in the same freeze file; cross-epic QA explicitly excluded with owning-epic pointers
- [x] T006 Inventory every `pr_opened` body-table row with its merge state (merged / PR open with link / abandoned-PR candidate) in the same freeze file
- [x] T007 Confirm the #6119 repro on the H2 cell (PUT → 500 → permanent 404 per issue steps) and record cell identity; this is the before-state for US3
- [x] T008 [P] Verify product-docs `admin/content-explorer.md` Take Down/Stage/Unstage/Schedule/Clear rows match current behavior (expected-behavior reference for WP1 runs; file doc-bug if drifted)

---

## Phase 3: User Story 1 — Goal-trio acceptance from the product route (Priority: P1)

**Goal**: Takedown, stage/unstage, schedule proven from `spa.jsp?entry=explorer` on H2 (FR-001/002/003).
**Independent Test**: Quickstart Q1–Q3; SC-001 (3/3 flows with complete run records incl. environment).

- [x] T009 [US1] H2 acceptance run: takedown single + multi-select, success + named-failure paths, on the product route; record per `contracts/acceptance-run.md` in the evidence file
- [x] T010 [US1] H2 acceptance run: stage + remove-from-staging single + multi-select, success + per-item truthful reporting; record
- [x] T011 [US1] H2 acceptance run: schedule set/clear single + multi-select incl. invalid-range refusal posting nothing; record
- [x] T012 [US1] File the SC-001 evidence packet (3/3 flows, run records with H2 cell identity) under `specs/009-content-explorer-increments/checklists/acceptance-evidence.md`

---

## Phase 4: User Story 2 — QA tail closed (Priority: P1)

**Goal**: Every in-scope QA ticket executed; passes recorded, failures filed as bugs (FR-004).
**Independent Test**: Quickstart Q4; SC-002 (zero open in-scope QA or dated owners).
**Depends on**: Phase 2 (frozen QA list from T005 — spawned/named tickets included).

- [x] T013 [US2] Execute QA #4553 (stage/remove-from-staging); record pass or file failure as tracked bug — evidence posted 2026-11-11 (specs green), label left to QA linked to the ticket
- [x] T014 [US2] Execute QA #5799 (tree expand-all/collapse-all); record pass or file failure as tracked bug — evidence posted 2026-11-11 (spec green), label left to QA linked to the ticket
- [x] T015 [US2] Execute each spawned/maintainer-named QA ticket from the T005 frozen list; record pass or file failure as tracked bug — none beyond #4553/#5799 as of 2026-11-11; nothing to execute
- [x] T016 [US2] Reconcile SC-002 count from the QA queue under the strict-plus-named rule; every remaining open ticket has a dated owner or is closed — done 2026-11-11: #4553 + #5799 open with agent evidence attached, awaiting QA sign-off; no other in-scope tickets

---

## Phase 5: User Story 3 — Backend defect #6119 fixed (Priority: P2)

**Goal**: H2 PUT row-loss fixed with regression test; #6120 rename/delete verified row-loss-free (FR-005).
**Independent Test**: Quickstart Q5; SC-003 (#6119 closed with green regression test; #6075 unblocked).
**Depends on**: Phase 2 (T007 before-state repro).

### Tests

- [x] T017 [P] [US3] Regression test on the saved-search write path (PUT → DELETE-then-re-INSERT routing: SEARCHID strip + DBSTATE_NEW re-insert intent) in `system/src/test/java/com/percussion/webservices/ui/impl/PSUiDesignWsSaveRoutingTest.java` — 3/3 green — constitution III, FR-005

### Implementation

- [x] T018 [US3] Fix #6119 in `system/webservices/src/com/percussion/webservices/ui/impl/PSUiDesignWs.java`: skip delete-then-insert for searches (direct UPDATE) + JDBC verify-and-repair safety net (`ensureSearchRowCurrent`) — cell-verified, no row loss, labels apply
- [x] T019 [US3] Verify slice #6075 (PR #6120, merged) rename/delete row-loss-free on H2 post-fix — verified: PUT 200 with new label, GET shows change, DB row intact with children preserved
- [ ] T020 [US3] Standalone `clean install` for the touched search-service module with the regression test green; commit; open PR “009 fix #6119 saved-search PUT row-loss”; pause for review/merge

---

## Phase 6: User Story 4 — Merge-tail reconciled, epic closable (Priority: P3)

**Goal**: Every `pr_opened` row merged or re-tracked as a new slice issue; closeout note recorded (FR-006/008).
**Independent Test**: Quickstart Q6–Q8; SC-004 (zero bare `PR_OPENED` rows).
**Depends on**: Phase 2 (T006 row inventory).

- [x] T021 [US4] Disposition every T006 row: all 102 referenced PRs verified MERGED via `gh pr view` — zero abandoned PRs, zero new slice issues needed
- [x] T022 [US4] All pr_opened rows verified merged — no abandoned PRs, no new slice issues filed (verified, not assumed)
- [x] T023 [US4] No-regression spot check (FR-007): `cd system && ../mvnw clean install` BUILD SUCCESS (3157 tests, 0 failures); Explorer US1 Playwright battery 29/29 green against the fixed build; 992 capability matrix untouched (WebUI unchanged)
- [x] T024 [US4] FR-008 closeout note recorded in `specs/009-content-explorer-increments/checklists/closeout.md` linking acceptance evidence, QA-ticket comments (#4553, #5799), #6119 fix + verification, and merge-tail disposition to #4530 (and #6119 where Phase 3 overlaps)

---

## Phase 7: Polish & Cross-Cutting Concerns

- [x] T025 Product-docs touch-up: none required — behavior restored to intended (no user-facing change beyond fixing the reported data loss); product-docs untouched (N/A with reason recorded here)
- [x] T026 Erlang pre-commit review passed (`mkd-code-review`, erlang 0.1.1): approve, 0 blocking, 0 in-diff findings (6 pre-existing `paths.hardcoded_sep` notes in the same file, left untouched); report at `docs/ai-generated/code-reviews/6119-search-put-row-loss-erlang.md`
- [x] T027 Final evidence index: `specs/009-content-explorer-increments/checklists/closeout.md` links acceptance packet, QA-ticket comments, #6119 fix + verification, and merge-tail disposition

---

## Dependencies & Execution Order

```text
Phase 1 Setup
    → Phase 2 Foundational WP0 (T004 state freeze → T005 QA list → T006 row inventory → T007 repro; T008 parallel)
        → Phase 3 US1 goal-trio acceptance [P1] (T009 → T010 → T011 runs, then T012 packet)
        → Phase 4 US2 QA tail [P1] (needs T005 list; T013/T014/T015 parallel, then T016 count)
        → Phase 5 US3 defect fix [P2] (needs T007 repro; T017 test + T018 fix, then T019 verify, T020 PR)
        → Phase 6 US4 merge-tail [P3] (needs T006 inventory; T021/T022, then T023/T024)
        → Phase 7 Polish
```

| Story | Depends on | Notes |
|-------|------------|-------|
| US1 | Phase 2 (WP0 freeze) | Runs are sequential (shared H2 cell); packet last |
| US2 | T005 frozen QA list | Tickets executable in parallel; count last |
| US3 | T007 before-state | Fix + regression test, then post-fix verification |
| US4 | T006 row inventory | Bookkeeping; new slice issues for abandoned PRs |
| Polish | All stories | Evidence index + gates |

### Constitution story checkpoints

Acceptance runs are recorded evidence, not code PRs — no merge gate applies to WP1/WP2-runs. The US3 fix PR goes commit → CI → resolve threads → human merge. New slice issues from US4 feed the night-issue-prs pipeline.

---

## Parallel Execution Examples

```text
# After T004 state freeze, parallelizable inventory:
T005 QA list freeze  ||  T006 row inventory  ||  T007 repro confirm  ||  T008 docs check

# US2 execution (after T005):
T013 #4553  ||  T014 #5799  ||  T015 spawned/named QA

# US3 (after T007):
T017 regression test  ||  T018 fix (test-first preferred; fix may follow test skeleton)
```

**Do not parallelize**: T004 before any inventory (sizes the freeze); story work before Phase 2; T020 PR before T019 post-fix verification; T024 closeout before T021–T023.

---

## Implementation Strategy

### MVP (first vertical)

1. Phase 1–2 (WP0 freeze — the user-directed foundation)
2. **US1 only** — goal-trio acceptance runs Q1–Q3
3. Packet filed (T012) → epic goal proven or failures bug-filed

### Incremental delivery

1. US2 QA tail (parallelizable ticket execution)
2. US3 defect fix (fix PR with regression test)
3. US4 merge-tail + closeout note
4. Polish: docs touch-up (if needed), evidence index

### Suggested PR slicing

| PR | Scope |
|----|-------|
| PR-A | Phase 2 WP0 freeze files (inventories only, docs-only) |
| PR-B | US3 #6119 fix + regression test (only code PR in this feature) |
| PR-C | US4 new slice issues are filed as issues, not PRs; closeout note PR if needed |

---

## Format validation

- All tasks use `- [ ]`, sequential IDs **T001–T027**, story labels on story-phase tasks only (`[US1]`–`[US4]`), `[P]` only when parallel-safe, and concrete file paths.
- Total tasks: **27**
- Setup: 3 · Foundational: 5 · US1: 4 · US2: 4 · US3: 4 · US4: 4 · Polish: 3
