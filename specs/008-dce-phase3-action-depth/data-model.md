# Data Model: DCE Phase 3 Action and Workflow Depth

**Branch**: `dce-phase3-action-depth` | **Date**: 2026-11-11 | **Spec**: [spec.md](spec.md)

This feature introduces **no new persisted data**. The model below is the UI-domain model the Explorer operates on: offer sets, dialog contents, menu entries, and bulk outcomes. All fields are observable behavior, not storage schema.

## 1. Transition offer set

Per item, per user, per moment — decides what action menus show (FR-001/002).

| Field | Type | Rules |
|-------|------|-------|
| `item` | reference | The content item the menu was opened on |
| `actor` | reference | The current user (permission evaluation is per actor) |
| `offers` | list | Exactly the permitted transitions — no more, no fewer |
| `itemState` | enum | The workflow state the offers were computed for; offers refresh when state changes |

**Validation**: empty offer list is valid (FR edge case) and MUST NOT render a dead option. Offers recomputed after every executed transition.

## 2. Properties dialog content

Per folder or item (FR-003/004/005).

| Field | Type | Rules |
|-------|------|-------|
| `subject` | reference | Folder or item under review |
| `groups` | map | Property groups beyond access control, keyed as DCE shows them |
| `editability` | per-group flag | Editable only where the actor has permission; otherwise read-only |
| `saveOutcome` | enum | `SAVED` \| `VALIDATION_FAILED` \| `DENIED` — persisted edits visible on reopen |

**Validation**: group set and values equal DCE for the parity sample (SC-002). Denied edits produce FR-009 messaging, never silent omission.

## 3. New-content menu

Per folder (FR-006/007/008).

| Field | Type | Rules |
|-------|------|-------|
| `folder` | reference | The folder the menu was opened in |
| `entries` | list | Exactly the creatable types for that folder |
| `templates` | per-entry list | Applicable templates when a type has more than one |
| `createdItem` | reference | On confirm, the new item opened in the editor create flow in the same folder |

**Validation**: entry set equals the folder's allowed types (SC-003 matrix).

## 4. Bulk outcome record

Per bulk invocation (FR-011).

| Field | Type | Rules |
|-------|------|-------|
| `selection` | list | The items the action was invoked on |
| `perItem` | map | One outcome per item: `SUCCESS` \| `DENIED` \| `FAILED(reason)` |
| `aggregate` | derived | Partial success reported as such — never a single pass/fail hiding per-item truth |

**State transitions**:

```text
INVOKED → EVALUATING (per-item permissions)
  → APPLYING (permitted items only)
    → REPORTED (per-item outcomes; successes stand, failures named)
```

**Invariants**:

- No transition to APPLYING for a denied item.
- REPORTED always carries the full per-item map, even when all items share one outcome.
- One item's failure never cancels or corrupts another item's success.
