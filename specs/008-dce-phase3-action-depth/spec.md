# Feature Specification: DCE Phase 3 Action and Workflow Depth

**Feature Branch**: `dce-phase3-action-depth`

**Created**: 2026-11-11

**Status**: Draft

**Input**: "Run speckit.specify on DCE phase 3" — Phase 3 (Action / workflow depth) of `specs/2400-dce-explorer-parity/plan.md`, tracked for disposition in GitHub issue #6110.

**Tracked as**: [GitHub Issue #6110](https://github.com/intersoftdatalabs-in/percussioncms/issues/6110) (Phase 3 disposition; parent program #2400 closed, continuation epic #4530)

## Module Scope

- **Primary module(s)**: React Content Explorer surface (`WebUI/`) and its backing content services (`projects/sitemanage/`, `rest/`) where menu/property data originates
- **Secondary / integration modules**: workflow and security services behind transition and permission data
- **User roles affected**: CMS editors and operators (daily content actions), administrators (properties review)
- **Install / upgrade impact**: none — no schema, config, or distribution changes expected

## Clarifications

### Session 2026-11-11

- Q: Should this specification cover only the remaining parity delta not yet shipped, or should it also include re-verification and rework of the surfaces already delivered under #4530? → A: Full rework allowed — planning may replace #4530-shipped surfaces where parity demands it (Option C).
- Q: Should item-properties editing be in scope for the first delivery of this feature, or should the first delivery be read-only with editing to follow? → A: Editable in v1 — FR-005 stands; dialogs support permitted edits from the start (Option A).
- Q: Should bulk or multi-select actions be in scope for this feature? → A: In scope — bulk/multi-select actions included with per-item permission and partial-failure handling (Option A).

## User Scenarios & Testing

Each story is independently testable.

### User Story 1 - Only valid workflow transitions are offered in menus (Priority: P1)

As an editor working in the React Content Explorer, I see exactly the workflow transitions I am allowed to perform on the selected item — the same set the Desktop Content Explorer would offer — so I never attempt an action that will be rejected.

**Why this priority**: Executing a disallowed transition (or missing an allowed one) is a correctness defect in daily editorial work; menu-depth parity is the core of Phase 3.

**Independent Test**: Can be fully tested by opening action menus on items in several workflow states and comparing the offered transitions against DCE behavior; delivers trustworthy menus.

**Acceptance Scenarios**:

1. **Given** an item in a workflow state with two permitted transitions, **When** the operator opens its action menu in Explorer, **Then** both transitions are offered and no others are.
2. **Given** an item for which the operator has no permitted transition, **When** the operator opens its action menu, **Then** no transition is offered (rather than an option that fails on use).
3. **Given** a transition is executed from the menu, **When** it completes, **Then** the item reflects the new state and the menu offer set updates accordingly.

---

### User Story 2 - Folder and item properties dialogs have full parity beyond ACL (Priority: P1)

As an operator, I can open folder and item properties dialogs from the Explorer and see and edit the same properties the Desktop Content Explorer shows — beyond the already-covered access-control entries — so I do not need to switch tools for routine review.

**Why this priority**: Properties review is a daily operator task; ACL-only parity leaves routine work stranded in the legacy tool.

**Independent Test**: Can be fully tested by opening properties dialogs on a sample of folders and items and comparing visible fields and edit behavior against DCE; delivers tool independence for routine review.

**Acceptance Scenarios**:

1. **Given** a folder, **When** the operator opens its properties dialog in Explorer, **Then** all DCE-shown property groups beyond access control are present with the same values.
2. **Given** a content item, **When** the operator opens its properties dialog in Explorer, **Then** all DCE-shown property groups are present with the same values.
3. **Given** an editable property the operator is permitted to change, **When** the operator edits and saves it, **Then** the change persists and is visible on reopen.

---

### User Story 3 - New-content menus list creatable types and templates (Priority: P2)

As an editor, I can create new content from type and template menus in the Explorer for the current folder, so creation does not require navigating elsewhere.

**Why this priority**: Creation is a primary flow but partially covered by existing menus; this story closes the remaining menu gap rather than opening a new surface.

**Independent Test**: Can be fully tested by opening the new-content menu in folders with different allowed-type sets and completing creation for each listed entry; delivers in-place creation.

**Acceptance Scenarios**:

1. **Given** a folder that allows three content types, **When** the operator opens the new-content menu there, **Then** exactly those three types are listed.
2. **Given** a listed type with multiple templates, **When** the operator chooses it, **Then** the applicable templates are offered for selection.
3. **Given** a chosen type and template, **When** the operator confirms, **Then** the editor create flow opens for the new item in the current folder.

---

### Edge Cases

- No permitted transitions exist for the item: menus offer none and explain nothing further is required (no dead option).
- A transition fails partway: the operator gets a clear failure message and the item state is unchanged or clearly reported.
- Properties dialog opened on an item with no editable properties: all values visible read-only; save is not offered or is a safe no-op.
- Operator lacks permission for a property or action: a clear permission message appears; no silent omission that looks like missing data.
- Large type/template lists: menus remain usable (scrollable/organized) rather than truncated without indication.
- Bulk selection with mixed permissions: permitted items proceed, denied items are named in the result, and partial success is reported per item rather than as a single pass/fail.

## Requirements

### Functional Requirements

- **FR-001**: Action menus MUST offer only the workflow transitions the current user is permitted to perform on the selected item, matching DCE behavior for the same item and state.
- **FR-002**: Action menus MUST NOT offer transitions the user cannot perform.
- **FR-003**: Folder properties dialogs MUST present the same property groups and values as DCE beyond access-control entries.
- **FR-004**: Item properties dialogs MUST present the same property groups and values as DCE.
- **FR-005**: Permitted property edits made in Explorer dialogs MUST persist and be visible on reopen.
- **FR-006**: New-content menus MUST list exactly the creatable content types for the current folder.
- **FR-007**: Choosing a type with multiple templates MUST offer the applicable templates before creating.
- **FR-008**: Confirming a type/template choice MUST open the editor create flow for the new item in the current folder.
- **FR-009**: Permission-denied outcomes MUST surface a clear message; the product MUST NOT silently omit data in a way that resembles missing content.
- **FR-010**: Menu, dialog, and offer behavior MUST be consistent across the Explorer tree, list, and related host surfaces where the same action applies.
- **FR-011**: Bulk (multi-select) actions MUST evaluate permissions per item and report results per item: successes stand, failures name the item and reason, and one item's failure MUST NOT silently cancel or corrupt the others.

### Key Entities

- **Workflow transition offer**: The set of permitted state changes for one item and one user at one moment (decides what menus show).
- **Properties dialog**: The read/edit view of a folder's or item's property groups beyond access control.
- **New-content menu entry**: One creatable type (plus its applicable templates) for the current folder.

## Success Criteria

### Measurable Outcomes

- **SC-001**: For a sample of at least 20 items across at least 5 workflow states, 100% of Explorer menu transition offers match DCE behavior (no missing, extra, or failing offers).
- **SC-002**: For a sample of at least 10 folders and 10 items, 100% of DCE-shown property groups beyond access control appear in Explorer dialogs with equal values.
- **SC-003**: New-content creation from Explorer menus succeeds for every listed type/template combination in a test matrix of at least 3 folders with differing allowed-type sets.
- **SC-004**: In a trial with at least 3 operators, 100% complete the transition, properties-review, and create flows without support escalation.

## Assumptions

- The Desktop Content Explorer is the behavior reference for offer sets, dialog fields, and menu contents.
- Some Phase 3 groundwork already ships (workflow menus landed under earlier work, the folder properties editor exists, item-level dialogs and creation slices partly landed under epic #4530) — this spec covers the **remaining parity delta**, and planning will map each FR against that existing coverage before implementation.
- Item-level properties and creation flows already delivered under #4530 MAY be rebuilt or replaced where DCE parity demands it (Option C decision, 2026-11-11); unchanged shipped surfaces are referenced, not rebuilt, by default.
- Views/Inbox, translation in-flight/session handling, and installer/packaging changes are out of scope.
- No schema, repository, or distribution changes are expected; if planning finds otherwise, the spec is amended first.

## Dependencies

- DCE parity gap matrix (`specs/2400-dce-explorer-parity/contracts/gap-matrix.md`) as the behavior reference.
- Disposition tracking in GitHub issue #6110; continuation epic #4530 for overlap resolution.
- Constitution constraints: UI work follows the WebUI + Playwright companion rule (component tests plus Playwright specs for changed screens).
