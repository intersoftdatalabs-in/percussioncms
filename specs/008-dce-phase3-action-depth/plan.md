# Implementation Plan: DCE Phase 3 Action and Workflow Depth

**Branch**: `dce-phase3-action-depth` | **Date**: 2026-11-11 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `specs/008-dce-phase3-action-depth/spec.md` (clarified 2026-11-11: full rework allowed, edit-in-v1, bulk in scope)
**Tracked as**: [GitHub Issue #6110](https://github.com/intersoftdatalabs-in/percussioncms/issues/6110) (parent program #2400 closed; continuation epic #4530)

## Summary

Complete Desktop Content Explorer parity for action/workflow depth inside the React Content Explorer: menus offer exactly the permitted workflow transitions, folder/item properties dialogs cover everything beyond access control, new-content menus list creatable types/templates, and bulk (multi-select) actions apply per-item permissions with per-item results. Approach is extension of proven Explorer and action-menu wiring — no new backend modules, no schema changes.

## Technical Context

**Language/Version**: TypeScript/React (WebUI SPA, existing shell) + Java 21 (rest JAX-RS resources, sitemanage adaptors)

**Primary Dependencies**: Existing Explorer wiring — `WebUI/src/main/ts/contentExplorer/` (`ContentExplorerShell`, `ContextMenu`, `actionDispatch`, `actionEnablement`, `ContentTypePickerDialog`, `FolderSecurityPanel`), `rest/.../actions/ActionMenuResource` (`getAllowedTransitions`), workflow checkout/checkin REST, `workflowActions/` dialogs

**Storage**: N/A — no repository schema or data-model changes (spec assumption; amended only if planning proves otherwise)

**Testing**: Vitest (WebUI unit/component) + Playwright (`modules/perc-qa-automation/`) per the WebUI companion rule

**Target Platform**: Supported browsers per product matrix; CMS server on Windows/Linux/macOS

**Project Type**: Web application (existing SPA frontend + Java backend)

**Performance Goals**: Standard product expectations (menu offers must not regress perceived responsiveness); deferred per clarify — no invented targets

**Constraints**: No new top-level modules; reuse the rest `IXxxAdaptor` pattern; user-visible strings via `perc-i18n` TMX; WCAG 2.1 AA for new/changed chrome (repo hard gate)

**Scale/Scope**: Single-item flows + multi-select bulk flows (FR-011); sample sizes per SC-001–SC-004 (20 items/5 states, 10+10 dialogs, 3 folders, 3 operators)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **I. Module-First Boundaries**: Owning modules listed (WebUI explorer, rest actions, sitemanage adaptors); shared code stays in existing utils; no new modules.
- [x] **II. Evidence Over Invention**: Plan cites existing paths (`contentExplorer/`, `ActionMenuResource.getAllowedTransitions`, `FolderSecurityPanel`, `ContentTypePickerDialog`); no invented APIs.
- [x] **III. Test Discipline**: Vitest + Playwright companions for changed screens; verification scenarios in quickstart map to SC-001–SC-004.
- [x] **IV. Contract & Integration Integrity**: Rest adaptor pattern preserved; no `.ppkg`/REST contract breaks; no ad-hoc persistence (no schema work at all).
- [x] **V. Safe Modernization**: Extension of existing shells and dialogs; no Spring Boot, no new frameworks.
- [x] **VI. Security by Default**: No secret logging; permission-denied handled as explicit user messaging (FR-009), not data exposure.
- [x] **VII. Build & Dependency Hygiene**: Parent POM dependency management; JDK 21 via `./mvnw`; Spotless per project standard.
- [x] **VIII. Documentation & Operability**: Diagnosable failures via existing logs/toasts; i18n via `perc-i18n` TMX for new strings.
- [x] **IX. PR Review Comment Resolution**: Story checkpoints (commit → PR → resolve threads → merge) apply at implementation time.

**Re-evaluation after Phase 1 design**: Gates still pass. Design uses existing action/dispatch/dialog plumbing; contracts are UI-behavioral (offer sets, per-item bulk results); bulk matrix (FR-011) is the sizing driver, not a new framework.

## Project Structure

### Documentation (this feature)

```text
specs/008-dce-phase3-action-depth/
├── plan.md                 # This file
├── research.md             # Phase 0 output
├── data-model.md           # Phase 1 output
├── quickstart.md           # Phase 1 output
├── contracts/
│   ├── transition-offer.md
│   ├── properties-parity.md
│   └── bulk-result.md
├── checklists/
│   └── requirements.md     # Spec quality (speckit-specify)
├── spec.md
└── tasks.md                # Phase 2 — /speckit-tasks (not this command)
```

### Source Code (affected paths — expected)

```text
WebUI/src/main/ts/contentExplorer/    # menus, dialogs, bulk dispatch (ContextMenu, actionDispatch, actionEnablement, pickers)
WebUI/src/main/ts/workflowActions/    # transition dialogs/panels as needed
WebUI/src/main/ts/i18n/               # TMX key additions for new strings
projects/sitemanage/...               # adaptor coverage only if a gap is proven (prefer existing adaptors)
rest/.../actions/...                  # adaptor coverage only if a gap is proven (no new resources expected)
modules/perc-qa-automation/...        # Playwright specs for changed screens
```

**Structure Decision**: Web application extension inside existing modules. Backend changes only if research proves an adaptor gap (R2); default is frontend-only against existing REST.

## Implementation approach

### Work packages

| WP | Name | Outcome | Covers |
|----|------|---------|--------|
| WP0 | Coverage mapping | FR-by-FR map: shipped (#2732, folder props, #4530 slices) vs delta vs rework (Option C) | All FRs, R1 |
| WP1 | Transition offers | Menus show exactly permitted transitions; state refresh after execute | US1, FR-001/002/010 |
| WP2 | Properties dialogs | Folder/item parity beyond ACL; permitted edits persist | US2, FR-003/004/005 |
| WP3 | New-content menus | Type/template listing; create-flow entry | US3, FR-006/007/008 |
| WP4 | Bulk handling | Per-item permissions, per-item results, partial-failure reporting | FR-011, FR-009 |
| WP5 | Verification | SC-001–SC-004 evidence (samples, trial); i18n/a11y gates | All SCs |

### Sequencing

WP0 first (it sizes everything else under the Option C rework rule). WP1–WP3 in parallel after WP0; WP4 after WP1 (bulk builds on single-item offer semantics); WP5 last. Each WP ends in commit → PR → review per constitution workflow.

### Story → WP mapping

| Spec story | WPs |
|------------|-----|
| US1 transition menus | WP0, WP1, (WP4 for bulk), WP5 |
| US2 properties dialogs | WP0, WP2, WP5 |
| US3 new-content menus | WP0, WP3, WP5 |

## Complexity Tracking

*(No constitution principle violations requiring exception.)*

| Item | Note |
|------|------|
| Bulk acceptance matrix | Inherent to FR-011; bounded by WP4 + per-item result contract |
| Option C rework breadth | Bounded by WP0 coverage map; rework only where parity demands it |

## Phase outputs

| Phase | Artifact | Status |
|-------|----------|--------|
| 0 | [research.md](research.md) | Complete |
| 1 | [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md) | Complete |
| 2 | [tasks.md](tasks.md) | Next — `/speckit-tasks` (not this command) |

## Risks and mitigations

| Risk | Mitigation |
|------|------------|
| Overlap duplication with #4530-shipped surfaces | WP0 map + Option C rule: reference unchanged surfaces, rebuild only for parity |
| Permission-model drift (menu offers wrong) | WP1 against existing enablement paths; SC-001 sample matrix |
| Bulk partial-failure confusion | WP4 + bulk-result contract; SC-004 operator trial |
| i18n/a11y gate miss on new strings | WP5 gate checks; TMX + axe coverage with Playwright companions |
