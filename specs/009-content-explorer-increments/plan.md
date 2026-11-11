# Implementation Plan: Content Explorer Increments Acceptance and Closure

**Branch**: `4530-content-explorer-increments` | **Date**: 2026-11-11 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `specs/009-content-explorer-increments/spec.md` (clarified 2026-11-11: strict-plus-named QA scope; abandoned PRs become new slice issues)
**Tracked as**: [GitHub Epic #4530](https://github.com/intersoftdatalabs-in/percussioncms/issues/4530) (functional slices 43–152 done or `pr_opened` with owning PRs as of 2026-11-11)

## Summary

Close out the Explorer increments program without building new item actions: prove the goal trio (takedown/stage/schedule) from the product route on H2, drain the QA verification tail, fix the one known backend defect (#6119 H2 saved-search PUT row-loss, unblocking already-merged #6120), and reconcile the merge tail so epic #4530 can close with zero orphans. One bounded backend fix; the rest is verification runs, QA execution, and bookkeeping.

Technical approach: H2 QA-cell acceptance runs per quickstart matrix; QA ticket execution with pass-or-file discipline; server-side fix in the saved-search write path (`saveSearches` → catalog invalidation → `findAllSearches` repopulation); body-table reconciliation with new slice issues for abandoned PRs.

**Late-breaking state (verified 2026-11-11 during planning)**: PR #6120 (slice #6075 rename/delete UI) is MERGED — the UI half of US3 is done; what remains is the #6119 backend fix plus row-loss-free verification. Product-docs `admin/content-explorer.md` already documents Take Down/Stage/Unstage/Schedule/Clear rows.

## Technical Context

**Language/Version**: Java 21 (one backend fix) + TypeScript/React verification via existing suites; no new UI code expected

**Primary Dependencies**: Saved-search write path (server `saveSearches` / catalog invalidation / `findAllSearches` repopulation per #6119 repro); existing Explorer suites (`itemPublish`, `actionDispatch`, explorer/schedule/stage/takedown Playwright specs); H2 QA cell for acceptance runs

**Storage**: N/A — no schema changes (possible cache/invalidation behavior change inside the search service only)

**Testing**: Re-run existing Vitest suites (no new unit suites unless the #6119 fix needs one); H2 QA-cell runs per quickstart; Playwright specs re-executed where they cover US1/US3 paths

**Target Platform**: H2 QA cell for acceptance (authoritative sign-off per spec); golden-environment differences recorded, not waived

**Project Type**: Verification + single backend defect fix + epic bookkeeping (not a feature build)

**Performance Goals**: Standard product expectations; no new latency targets (acceptance runs record environment with results)

**Constraints**: No new item-action slices in this change set (spec assumption); abandoned PRs become new slice issues, not silent table edits (clarify Option A); no 992/2400 parity regressions (FR-007)

**Scale/Scope**: 3 goal flows, named QA tickets (#4553, #5799 + spawned), 1 backend defect, ~60 `pr_opened` body-table rows to reconcile

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **I. Module-First Boundaries**: Backend fix stays in the owning search service module; verification touches no new modules; bookkeeping touches only the epic body + this spec package.
- [x] **II. Evidence Over Invention**: Plan cites verified paths (search write path from #6119 repro, existing specs, product-docs rows); no invented APIs.
- [x] **III. Test Discipline**: Regression test required for the #6119 fix; acceptance runs recorded per quickstart; QA pass-or-file discipline per FR-004.
- [x] **IV. Contract & Integration Integrity**: No REST contract changes (PUT semantics repaired, not redesigned); no `.ppkg` impact.
- [x] **V. Safe Modernization**: Single localized backend fix; no new frameworks, no Spring Boot.
- [x] **VI. Security by Default**: No secret handling changes; permission-denied reporting already covered by merged slices; failure outputs name items, never credentials.
- [x] **VII. Build & Dependency Hygiene**: Parent POM; JDK 21 via `./mvnw`; Spotless on touched Java.
- [x] **VIII. Documentation & Operability**: Diagnosable failures already established by slice error regions; closeout note is the documentation deliverable (FR-008).
- [x] **IX. PR Review Comment Resolution**: Applies to the #6119 fix PR and any new slice issues' PRs.

**Re-evaluation after Phase 1 design**: Gates still pass. The single code change is the search-service PUT fix with a regression test; everything else is runs and records. No new modules, no contract changes.

## Project Structure

### Documentation (this feature)

```text
specs/009-content-explorer-increments/
├── plan.md                 # This file
├── research.md             # Phase 0 output
├── data-model.md           # Phase 1 output
├── quickstart.md           # Phase 1 output
├── contracts/
│   ├── acceptance-run.md
│   └── slice-disposition.md
├── checklists/
│   └── requirements.md     # Spec quality (speckit-specify)
├── spec.md
└── tasks.md                # Phase 2 — /speckit-tasks (not this command)
```

### Source Code (affected paths — expected)

```text
system/.../search/...        # saved-search write path: saveSearches → catalog invalidation → findAllSearches (exact classes per R3)
system/src/test/...          # regression test for the PUT row-loss fix
modules/perc-qa-automation/  # re-executed (not rewritten) explorer/schedule/stage/takedown specs
product-docs/8.2/admin/content-explorer.md  # already documents Take Down/Stage/Schedule/Clear; update only if behavior changed
```

**Structure Decision**: Verification-led closeout. The only anticipated code change is the #6119 fix in the owning search service; all suites and docs are re-used as evidence, updated only on behavior change.

## Implementation approach

### Work packages

| WP | Name | Outcome | Covers |
|----|------|---------|--------|
| WP0 | Baseline freeze | Inventory: goal-trio state, QA ticket list (strict-plus-named), `pr_opened` row states, #6119 repro confirmation | All FRs, R1 |
| WP1 | Goal-trio acceptance | H2 route-level runs for takedown/stage/schedule (single + multi, success + named-failure) | US1, FR-001/002/003 |
| WP2 | QA tail + defect | Execute named QA; fix #6119 with regression test; verify #6120 row-loss-free | US2, US3, FR-004/005 |
| WP3 | Merge-tail + closeout | Disposition every `pr_opened` row (merged or new slice issue); closeout note; no-regression check | US4, FR-006/007/008 |

### Sequencing

WP0 first (it freezes the inventories everything else consumes). WP1 and WP2 in parallel after WP0 (acceptance runs independent of the defect fix; #6120 verification waits on the fix). WP3 last (needs merge states settled). Single backend-fix PR inside WP2 goes through the normal review/thread gate.

### Story → WP mapping

| Spec story | WPs |
|------------|-----|
| US1 goal-trio acceptance | WP0, WP1 |
| US2 QA tail | WP0, WP2 |
| US3 #6119 fix | WP0 (repro), WP2 |
| US4 merge-tail + closeout | WP0, WP3 |

## Complexity Tracking

*(No constitution principle violations requiring exception.)*

| Item | Note |
|------|------|
| H2-only vs golden differences | Recorded per run, not waived (edge case); golden rerun only if H2 result is disputed |
| ~60-row reconciliation | Bookkeeping, bounded by WP0 inventory; abandoned PRs become tracked slice issues |

## Phase outputs

| Phase | Artifact | Status |
|-------|----------|--------|
| 0 | [research.md](research.md) | Complete |
| 1 | [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md) | Complete |
| 2 | [tasks.md](tasks.md) | Next — `/speckit-tasks` (not this command) |

## Risks and mitigations

| Risk | Mitigation |
|------|------------|
| #6119 fix causes search regressions | Regression test on the PUT path; re-run search suites before merge |
| QA failure avalanche | Pass-or-file discipline: each failure becomes a tracked bug, scope stays bounded by strict-plus-named rule |
| `pr_opened` rows silently stall | WP0 inventory with merge-state per row; abandoned PRs get new slice issues (clarify Option A) |
| Acceptance disputes (H2 vs golden) | Environment recorded with every result; no silent waivers |
