# Quickstart validation: Content Explorer Increments Acceptance and Closure

**Purpose**: Runnable validation proving the epic is closable per [spec.md](spec.md) success criteria.
**Not**: implementation code (see `/speckit-tasks` later). The only code in scope is the #6119 fix.

## Prerequisites

- Branch: `4530-content-explorer-increments`
- H2 QA cell with a published page and asset available
- QA queue access for #4553, #5799 (plus spawned/named tickets as WP0 lists them)
- #6119 repro steps (from the issue body) for before/after comparison

## Scenario matrix

| # | Scenario | Spec refs | Expected |
|---|----------|-----------|----------|
| Q1 | Takedown single + multi, success + named failure | US1, SC-001, FR-001 | Matches product-docs Take Down row; failures name the item |
| Q2 | Stage/unstage single + multi, success + named failure | US1, SC-001, FR-002 | Matches Stage rows; per-item truthful reporting |
| Q3 | Schedule set/clear, invalid-range refusal | US1, SC-001, FR-003 | Dates persist; invalid range posts nothing |
| Q4 | Execute named QA tickets | US2, SC-002, FR-004 | Pass recorded or failure filed as bug |
| Q5 | #6119 fix + #6120 row-loss-free check | US3, SC-003, FR-005 | Rename/delete persist; regression test green |
| Q6 | Body-table row walk | US4, SC-004, FR-006 | Zero bare `PR_OPENED` rows |
| Q7 | No-regression spot check | FR-007 | 992 matrix + 2400 Present rows still hold |
| Q8 | Closeout note | FR-008 | Note links all evidence; #4530 meets close criteria |

## Module verification (pre-PR gate)

For the #6119 fix module only: standalone `clean install` from the module directory with the regression test green; attach command + BUILD SUCCESS to the fix PR. Verification-only work: no Maven run required; record run evidence instead.

## Suggested automated test packages

| Area | Focus |
|------|-------|
| Search service | PUT row-loss regression (save → invalidate → repopulate; Developer shared endpoint) |
| Playwright (re-executed) | explorer-takedown/multi, explorer-staging/multi, explorer-schedule/multi specs |
| QA execution | Named tickets per strict-plus-named rule |
