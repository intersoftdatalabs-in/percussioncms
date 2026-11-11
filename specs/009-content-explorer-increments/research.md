# Research: Content Explorer Increments Acceptance and Closure

**Branch**: `4530-content-explorer-increments` | **Date**: 2026-11-11 | **Spec**: [spec.md](spec.md)

All Phase 0 unknowns resolved from repository and tracker evidence. No NEEDS CLARIFICATION remains.

## R1 — Baseline freeze method

### Decision

WP0 freezes four inventories before any other work: (a) goal-trio state on main (already verified done — #5990/#5991/#5992 closed as implemented, re-verified 2026-11-11); (b) the QA ticket list under the strict-plus-named rule (#4553, #5799, QA spawned by #4530-slice merges, plus maintainer-named tickets); (c) every `pr_opened` body-table row with its merge state; (d) #6119 repro confirmation on the H2 cell.

### Rationale

The spec's scope is acceptance plus leftovers; without frozen inventories, runs and reconciliation have no defined finish line (SC-002/SC-004 are counts).

### Alternatives considered

Starting runs without the freeze — rejected: SC-002/SC-004 cannot be evaluated against a moving ticket/row set.

## R2 — Goal-trio acceptance route

### Decision

Acceptance runs execute from the product Explorer route (`spa.jsp?entry=explorer`) on the H2 QA cell, per the quickstart matrix: takedown, stage/unstage, schedule set/clear — single + multi-select, success + named-failure paths. Product-docs `admin/content-explorer.md` Take Down/Stage/Unstage/Schedule/Clear rows are the expected-behavior reference; any deviation found during runs is a bug, not a docs edit, unless behavior intentionally changed.

### Rationale

The epic goal names the product route explicitly; component suites already passed, so route-level proof is the missing layer.

## R3 — #6119 write path and fix location

### Decision

The defect lives in the UI-design search service: `system/webservices/src/com/percussion/webservices/ui/impl/PSUiDesignWs.java` owns `saveSearches` / `findSearches` / `invalidateSearchCatalog` (verified in-tree). The PUT repro (`PUT /Rhythmyx/services/searches/{name}` → 500 `IllegalStateException: Search was saved but is not visible to findSearches` → permanent 404) points at the save → catalog-invalidation → repopulation sequence, possibly an H2-only write-visibility interaction. The fix stays in that service (repair, not redesign), ships with a regression test on the PUT path, and the same endpoint serves Developer `SearchDetailPanel` save — so Developer search save is covered by the same regression test. #6120 (rename/delete UI, merged) is then verified row-loss-free on H2.

### Rationale

#6119's own repro already isolated the sequence; the class ownership is confirmed by grep, not assumed. Repair-not-redesign follows constitution V.

### Alternatives considered

Redesigning the search catalog caching — rejected: out of proportion to a write-visibility defect; revisit only if the repair proves insufficient.

## R4 — QA execution discipline

### Decision

Pass-or-file: every in-scope QA ticket is executed; a pass is recorded with environment, a failure is filed as a tracked bug linked to the ticket. Tickets are never closed as passed without a run. Cross-epic QA stays with owning epics per the clarify decision and does not block this closeout.

### Rationale

SC-002 counts tickets, so the terminal states must be airtight; otherwise "zero open QA" is a bookkeeping fiction.

## R5 — Merge-tail reconciliation mechanics

### Decision

Walk the #4530 body table row by row: merged rows need no action; a `pr_opened` row whose PR will merge stays tracked to that PR; a row whose PR will never merge as-is becomes a **new slice issue** with owner, scope, and acceptance criteria (clarify Option A). The closeout note (FR-008) records the final disposition and links this spec's evidence, making #4530 closable.

### Rationale

Table edits alone don't create ownership for unfinished work; slice issues do — and they feed the existing night-issue-prs pipeline automatically.

## Open items deferred to tasks (not blocking design)

- Exact H2 cell reservation and run dates for the WP1/WP2 acceptance matrix.
- Maintainer-named QA additions under the strict-plus-named rule (named during WP0, not pre-listed here).
- Exact regression-test class/package for the #6119 fix (chosen at implementation time inside the search service tests).
