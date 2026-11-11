# Feature Specification: Content Explorer Increments Acceptance and Closure

**Feature Branch**: `4530-content-explorer-increments`

**Created**: 2026-11-11

**Status**: Draft

**Input**: "Run speckit.specify on the #4530 backlog, taking into account what has already been built" — epic [#4530](https://github.com/intersoftdatalabs-in/percussioncms/issues/4530) (Content Explorer next product increments, open) state as of 2026-11-11.

**Tracked as**: [GitHub Epic #4530](https://github.com/intersoftdatalabs-in/percussioncms/issues/4530) (successor: closeout issues filed from the UI gap sweep)

## Module Scope

- **Primary module(s)**: Verification surfaces only — `WebUI/` (Explorer route `spa.jsp?entry=explorer`), `modules/perc-qa-automation/` (H2 QA proof), `product-docs/8.2/` (operator rows if behavior changed)
- **Secondary / integration modules**: `rest` / `projects/sitemanage` only via the #6119 backend defect fix; otherwise untouched
- **User roles affected**: CMS operators (takedown/stage/schedule flows), QA engineers (verification tail), epic owners (slice bookkeeping)
- **Install / upgrade impact**: none

## Built-state baseline (2026-11-11 — spec builds on this, not over it)

- The epic's three goal flows are **done on main, no second PR**: takedown (#5990), stage/remove-from-staging (#5991), schedule set/clear (#5992) — each with Vitest, Playwright specs, and product-docs rows.
- Functional slices 43–152 in the #4530 body table are **done/merged or `pr_opened` with an owning PR** (multi-select purge/restore, copy URL/GUID, workflow comment, checkout owner, folder path, custom views, paging, assembler preview, where-used, folder publish, clear dates, relationships, create asset/copy/promotable, flush cache, translation variant, folder rename, incremental queue, set workflow/community/locale/format/sites, checkin comment, page template, relationship move/open/retarget, revision restore, audit trail, display title, NUL guards, yes-no/number/date/keyword fields, column sort/keys/visibility/density, tree type-ahead/inline-folder, 400-naming trio #5942–44, saved-search rename/delete #6075).
- **Not built / not owned — this spec's scope**: end-to-end acceptance of the goal trio from the product route, the QA verification tail, backend defect #6119, and merge-tail reconciliation. No new item-action slices are specified here; any genuinely new action discovered later gets its own slice issue.

## Clarifications

### Session 2026-11-11

- Q: Which QA tickets count toward closing US2 and SC-002's zero-open-QA bar? → A: Strict plus named additions — #4553, #5799, and QA spawned by #4530-slice merges, plus any QA ticket explicitly named into scope by maintainer decision; all other cross-epic QA stays with its owning epic (Option C).
- Q: For a `pr_opened` row whose PR will never merge as-is, what form must its re-tracking take to satisfy FR-006? → A: New slice issue with owner, scope, and acceptance criteria (Option A).

## User Scenarios & Testing

Each story is independently testable.

### User Story 1 - Goal-trio acceptance from the product route (Priority: P1)

As an operator, I can take down, stage/unstage, and schedule a page or asset from the product Explorer route and each flow completes with correct success/failure reporting, so the epic's stated goal is proven — not just code-complete.

**Why this priority**: The epic goal ("operators can takedown, stage, and schedule from the product Explorer route") is the acceptance bar for the whole program; merged code without route-level proof is not done.

**Independent Test**: Can be fully tested on H2 by driving takedown, stage/unstage, and schedule set/clear from `spa.jsp?entry=explorer` (single + multi-select, success + named-failure paths) and recording pass/fail; delivers a proven goal trio.

**Acceptance Scenarios**:

1. **Given** a published page on H2, **When** the operator takes it down from the Explorer route, **Then** the takedown completes, the state is visible, and failures name the item and reason.
2. **Given** a page or asset on H2, **When** the operator stages and then removes it from staging, **Then** both transitions complete with per-item truthful reporting.
3. **Given** a page or asset on H2, **When** the operator sets and clears publish dates, **Then** the dates persist correctly and invalid ranges are refused before any server call.

---

### User Story 2 - QA verification tail closed (Priority: P1)

As a QA engineer, every open verification ticket in the Explorer increments scope passes or its failure is filed as a tracked bug, so no silent unverified surface ships in 8.2.

**Why this priority**: Known-open QA (stage/remove-from-staging, tree expand/collapse) plus QA spawned by in-flight slice merges is the outstanding human-proof tail.

**Independent Test**: Can be fully tested by executing each open QA ticket in scope and recording pass (or filing the failure as a bug linked to the ticket); delivers a zero-open-QA scope.

**Acceptance Scenarios**:

1. **Given** open QA ticket #4553 (stage/remove-from-staging verification), **When** it is executed, **Then** it passes or its failure is filed as a tracked bug.
2. **Given** open QA ticket #5799 (tree expand-all/collapse-all), **When** it is executed, **Then** it passes or its failure is filed as a tracked bug.
3. **Given** any QA ticket spawned by an in-flight slice merge, or any QA ticket explicitly named into scope by maintainer decision, **When** it is executed, **Then** it passes or its failure is filed as a tracked bug — no in-scope QA ticket stays unowned. (All other cross-epic QA stays with its owning epic and does not block this closeout.)

---

### User Story 3 - Backend defect #6119 fixed, #6075 unblocked (Priority: P2)

As an operator, I can rename and delete saved searches from the Search panel without row loss, because the underlying H2 PUT defect is fixed with a regression test.

**Why this priority**: #6119 is a preexisting backend defect (PUT row-loss) that blocks an otherwise-complete slice (#6075, PR #6120 open); it is the only known code defect in the remaining scope.

**Independent Test**: Can be fully tested by renaming and deleting a saved search on H2 and confirming no row loss, with the regression test passing; delivers the unblocked slice.

**Acceptance Scenarios**:

1. **Given** the #6119 fix on H2, **When** a saved search is renamed, **Then** the rename persists with no row loss.
2. **Given** the #6119 fix on H2, **When** a saved search is deleted, **Then** the row is removed and the selection clears without affecting other rows.

---

### User Story 4 - Merge tail reconciled and epic closable (Priority: P3)

As an epic owner, every `pr_opened` row in the #4530 body table is either merged or explicitly re-tracked with an owner and date, so the epic can close with no orphan slices.

**Why this priority**: Dozens of rows sit at `pr_opened`; without reconciliation the epic cannot close even when the code is done.

**Independent Test**: Can be fully tested by walking the body table and confirming each row is merged-closed or re-tracked; delivers a closable epic.

**Acceptance Scenarios**:

1. **Given** the #4530 body table, **When** each `pr_opened` row is checked, **Then** every row is merged or re-tracked as a new slice issue carrying owner, scope, and acceptance criteria.
2. **Given** all rows dispositioned, **When** the closeout note is recorded, **Then** #4530 meets its close criteria (goal trio proven per US1, QA tail per US2, no orphans).

---

### Edge Cases

- H2 vs golden-environment differences in acceptance runs: record environment with each result; environment-specific failures are filed as bugs, not waived silently.
- Partial failures in bulk flows during US1 runs: per-item reporting must hold; a partial failure is a finding, not a pass.
- QA failure on a ticket: file as a tracked bug linked to the ticket; the ticket itself is not closed as passed.
- #6119 fix causes regressions elsewhere: the fix ships with a regression test covering the PUT path; any new failure blocks the fix PR.
- In-flight PR abandoned by its owner: a new slice issue is filed with owner, scope, and acceptance criteria — never left at `pr_opened` indefinitely and never closed by table edit alone.

## Requirements

### Functional Requirements

- **FR-001**: Takedown from the product Explorer route MUST be verified working on H2 (single + multi-select, success + named-failure paths) — evidence recorded, not assumed from merge.
- **FR-002**: Stage and remove-from-staging from the product Explorer route MUST be verified working on H2 with per-item truthful reporting.
- **FR-003**: Schedule set/clear from the product Explorer route MUST be verified working on H2, including invalid-range refusal before any server call.
- **FR-004**: Every in-scope QA ticket (#4553, #5799, QA spawned by in-flight slice merges, plus any QA ticket explicitly named into scope by maintainer decision) MUST be executed; passes recorded, failures filed as tracked bugs. All other cross-epic QA stays with its owning epic.
- **FR-005**: Backend defect #6119 (H2 PUT row-loss) MUST be fixed with a regression test, and saved-search rename/delete (#6075) MUST work without row loss on H2.
- **FR-006**: Every `pr_opened` row in the #4530 body table MUST be dispositioned as merged or re-tracked as a new slice issue carrying owner, scope, and acceptance criteria; zero orphan slices.
- **FR-007**: No 992-parity or 2400-Present behavior regressed by this work: the parity matrices MUST still hold after the remaining changes (regression guard).
- **FR-008**: A closeout note MUST be recorded linking this spec's evidence back to #4530 disposition (and to #6110 where Phase 3 overlaps), making the epic closable.

### Key Entities

- **Goal-trio flow**: Takedown, stage/unstage, or schedule executed from the product Explorer route on H2 (the epic acceptance unit).
- **QA ticket**: An open verification ticket in scope; terminal states are passed or failure-filed-as-bug.
- **Slice tracker row**: One #4530 body-table row; terminal states are merged or explicitly re-tracked.
- **Backend defect #6119**: H2 PUT row-loss blocking #6075; terminal state is fixed-with-regression-test.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Takedown, stage/unstage, and schedule each verified end-to-end from `spa.jsp?entry=explorer` on H2 (single + multi-select, success + named-failure paths) — 3/3 flows with recorded evidence.
- **SC-002**: Zero open QA tickets in the Explorer increments scope, or every remaining one has a dated owner — counted from the QA queue, not assumed.
- **SC-003**: #6119 closed with a passing regression test; #6075 unblocked (merged or merge-ready) with rename/delete verified row-loss-free on H2.
- **SC-004**: Zero orphan `pr_opened` rows in the #4530 body table (every row merged or re-tracked with owner/date).

## Assumptions

- Functional item-action slices are owned (done/merged or `pr_opened` with an owning PR) — this spec specifies **no new item actions**; genuinely new actions get their own slice issues.
- In-flight PR ownership stays with current owners; US4 only reconciles bookkeeping, never reassigns code authorship silently (re-tracking names the new owner explicitly).
- H2 QA environment is available for acceptance runs (same surface convention as prior H2 surface proofs).
- #6110 (Phase 3 disposition) overlaps on workflow/menu items; evidence is shared, not duplicated — cross-link, don't re-verify.
- Performance expectations follow standard product defaults (no invented targets); H2 surface proofs record environment with results.

## Dependencies

- Epic #4530 body table (slice inventory + PR links) as the bookkeeping source of truth.
- Backend defect #6119 and slice PR #6120 (#6075).
- QA queue for #4553, #5799, and QA spawned by in-flight merges.
- H2 QA environment for US1–US3 runs.
- 992 capability matrix + 2400 gap matrix as the no-regression baseline (FR-007).
