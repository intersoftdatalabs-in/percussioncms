# Data Model: Content Explorer Increments Acceptance and Closure

**Branch**: `4530-content-explorer-increments` | **Date**: 2026-11-11 | **Spec**: [spec.md](spec.md)

This feature introduces **no new persisted data**. The model below is the verification-domain model: the acceptance run, the QA ticket, the slice row, and the defect — each with terminal states that make SC-001–SC-004 countable.

## 1. Acceptance run

One execution of a goal-trio flow from the product route on H2 (FR-001/002/003).

| Field | Type | Rules |
|-------|------|-------|
| `flow` | enum | `TAKEDOWN` \| `STAGE_UNSTAGE` \| `SCHEDULE` |
| `selection` | enum | `SINGLE` \| `MULTI` |
| `path` | enum | `SUCCESS` \| `NAMED_FAILURE` |
| `environment` | string | H2 cell identity, recorded with the result — never omitted |
| `outcome` | enum | `PASS` \| `FAIL(filed bug)` |

**Validation**: a run without a recorded environment is not evidence. A fail without a linked bug is not dispositioned.

## 2. QA ticket (in scope)

| Field | Type | Rules |
|-------|------|-------|
| `ticket` | reference | #4553, #5799, spawned-by-merge QA, or maintainer-named ticket |
| `state` | enum | `OPEN` \| `PASS(recorded)` \| `FAIL(filed bug)` |
| `scopeRule` | enum | `NAMED` \| `SPAWNED` \| `MAINTAINER_NAMED` — how it entered scope |

**Validation**: terminal states are pass-recorded or failure-filed. Cross-epic QA never enters this model (stays with owning epics).

## 3. Slice tracker row

One #4530 body-table row (FR-006).

| Field | Type | Rules |
|-------|------|-------|
| `row` | reference | Slice number + issue link |
| `state` | enum | `MERGED` \| `PR_OPENED(tracked to PR)` \| `RETRACKED(new slice issue)` |
| `owner` | reference | Required for `RETRACKED`; the new slice issue carries owner, scope, acceptance criteria |

**State transitions**:

```text
PR_OPENED → MERGED (PR lands)
PR_OPENED → RETRACKED (PR will never merge as-is; new slice issue filed)
```

**Invariant**: no row rests at bare `PR_OPENED` at closeout — each is merged or re-tracked.

## 4. Backend defect #6119

| Field | Type | Rules |
|-------|------|-------|
| `defect` | reference | H2 PUT row-loss on saved-search save |
| `state` | enum | `OPEN` \| `FIXED(regression test passing)` |
| `unblocked` | reference | #6075/#6120 rename/delete verified row-loss-free on H2 after the fix |

**Validation**: the regression test exercises the PUT path through save → catalog invalidation → repopulation, including the Developer `SearchDetailPanel` shared endpoint.
