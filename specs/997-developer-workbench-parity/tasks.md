# Tasks: Developer Workbench Parity (997)

**Prerequisites**: `spec.md`, `plan.md`, `contracts/gap-matrix.md`, `docs/developer-module/workbench-functional-inventory.md`, #1690 slice table
**Branch**: `spec-997-developer-workbench-parity` (acceptance layer; execution stays on #1690 slice PRs)
**Tests**: No product code in this package. Verification = matrix/QA evidence, not new test files.

## Phase 1: Setup

- [ ] T001 Read AGENTS hierarchy (root + `WebUI/`, `rest/`, `projects/sitemanage/`, `modules/perc-qa-automation/`) and the inventory §§1–5 (rules, personas, IA, catalog, cross-cutting)
- [ ] T002 Confirm branch is based on current `main`; record #1690 slice-table snapshot (105 rows as of 2026-11-11) as the reconciliation baseline

## Phase 2: WP0 reconciliation (blocking)

- [ ] T003 Map every inventory §6–11 module capability to a gap-matrix row with slice links; add explicit **Missing** rows for unmapped capabilities (SC-001)
- [ ] T004 Map §§12–14 (navigators, wizards, journeys) the same way; classify the "Other" bucket; disposition thin areas (UI Elements, Navigators) as slice-growth issues or scoped-out notes with rationale
- [ ] T005 Cross-check: every #1690 slice row points at a matrix row (no orphan slices); record deltas

## Phase 3: WP1 QA evidence expectations

- [ ] T006 For each Partial row with merged slices, record what human-QA proof flips it to Present (trial shape, screens, pass criteria)
- [ ] T007 For each PR-open slice row, record the merge + QA proof needed; link open PRs (no duplicate issues)

## Phase 4: WP2 decisions

- [ ] T008 File slice-growth issues for thin areas OR record scoped-out rationale (with #1690 link); resolve the 1 blocked slice (implement-or-defer in #1690)
- [ ] T009 Define the journey-level trial set (§14 scenarios) and file the QA tracking issue

## Phase 5: WP3 closeout

- [x] T010 Erlang pre-commit review: approve, 0 findings, 0 blocking
- [ ] T011 Record closeout note when SC-001–SC-004 all hold (matrix 100% Present + table 100% done/verified + QA linked); tick boxes
- [x] T012 Open PR for this acceptance-layer package (shipped as part of this commit push)

## Dependencies

Setup → WP0 (blocks everything) → WP1 → WP2 → WP3. T012 (package PR) is independent of WP0–WP2 execution and may merge first.
