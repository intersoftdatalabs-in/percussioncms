# Research: DCE Phase 3 Action and Workflow Depth

**Branch**: `dce-phase3-action-depth` | **Date**: 2026-11-11 | **Spec**: [spec.md](spec.md)

All Phase 0 unknowns resolved from repository evidence. No NEEDS CLARIFICATION remains.

## R1 — Coverage map: shipped vs delta vs rework

### Decision

WP0 produces an FR-by-FR map with three dispositions: **reference** (shipped surface reused as-is), **verify** (shipped surface with a proof task), **build/rework** (delta or Option C replacement). Known starting points from prior audits:

- Workflow menus in Explorer: present (earlier #2732 work) — default disposition **verify** against FR-001/002.
- Folder properties editor: present — default **verify** against FR-003.
- Item-level properties dialogs and creation slices: partially delivered under epic #4530 — default **verify**, rebuild only where the map proves a parity miss (Option C rule from clarify 2026-11-11).
- New-content menus: partially on existing action paths — **verify**, then build the remainder (FR-006–008).

### Rationale

The clarify session decided full rework is allowed but unchanged shipped surfaces are referenced, not rebuilt. The map bounds WP1–WP4 so implementation does not duplicate #4530.

### Alternatives considered

Skipping WP0 and building from the spec alone — rejected: proven overlap with #4530 makes duplicate implementation the likely outcome.

## R2 — Allowed-transitions path

### Decision

Reuse the existing action-menu path: `IActionMenuAdaptor.findAllowedTransitions` served by `ActionMenuResource.getAllowedTransitions` (both verified present in `rest/`). WP1 verifies production adaptor coverage for the Explorer menu flows; **no new REST resource is expected**. If WP0/WP1 proves a prod-adaptor gap, the fallback is extending the sitemanage adaptor behind the same contract — never a parallel endpoint.

### Rationale

The interface already exists and the 992 audit confirmed menu enablement builds on it. Evidence over invention (constitution II).

### Alternatives considered

New dedicated transitions endpoint — rejected unless a gap is proven; would fork the contract surface for one consumer.

## R3 — Properties dialog inventory

### Decision

Inventory before building: `FolderSecurityPanel`/`FolderAclDialog` and the folder properties editor cover the folder side (verify against FR-003); item dialogs are the likely delta (FR-004/005). Extend existing dialogs; do not introduce a second dialog framework.

### Rationale

Folder-side presence is verified in-tree; item-side is the documented partial. Matches the extend-don't-rebuild structure decision.

## R4 — Bulk action patterns

### Decision

Build FR-011 on the existing multi-select/dispatch plumbing in `contentExplorer/` (`actionDispatch`, `actionEnablement`, `clipboard/`): per-item permission evaluation reuses the single-item offer semantics (WP1), and per-item results follow the [bulk-result contract](contracts/bulk-result.md). Partial failure is reported per item; successes stand.

### Rationale

Bulk as a composition of single-item semantics keeps one permission model (FR-010 consistency) instead of a parallel bulk model that can drift.

### Alternatives considered

Separate bulk pipeline — rejected: doubles the permission logic and its test matrix.

## R5 — i18n and accessibility process

### Decision

Every new user-visible string gets a `perc-i18n` TMX key in the same change set; changed screens get Playwright companions that include the established axe checks (following the `renderA11yGate`/axe pattern used across the Explorer program). WP5 gates SC evidence plus these two hard gates.

### Rationale

Repo hard gates require TMX-only chrome and WCAG 2.1 AA for UI work; the 2400 audit showed per-slice sign-off is otherwise unverifiable.

## Open items deferred to tasks (not blocking design)

- Exact TMX key names for new strings (chosen at implementation time).
- Sample fixture lists for SC-001–SC-003 matrices (chosen from existing fixtures in tasks phase).
- Operator-trial roster for SC-004 (arranged at verification time).
