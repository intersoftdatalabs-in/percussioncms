# Phase 3 disposition: action / workflow depth (issue #6110)

**Parent:** #2400 · **Plan:** [plan.md](../plan.md) § Phase 3 · **Disposition issue:** [#6110](https://github.com/intersoftdatalabs-in/percussioncms/issues/6110)
**Evidence date:** 2026-11-12 (live GitHub state + `origin/main` tip `f4e696f2f9`)
**Verdict: no-gap** — every Phase 3 prose row already has an owner, shipped
code on `main`, and a closed issue/PR record. No new child issues filed.

## Row 1 — Allowed workflow transitions

| Item | Evidence (checked 2026-11-12) |
|------|-------------------------------|
| Base owner | [#2732](https://github.com/intersoftdatalabs-in/percussioncms/issues/2732) **CLOSED** — workflow transitions in Explorer menus, absorbed by cluster [PR #2748](https://github.com/intersoftdatalabs-in/percussioncms/pull/2748) **MERGED** |
| Human QA | [#2743](https://github.com/intersoftdatalabs-in/percussioncms/issues/2743) **CLOSED**, labelled `QA: Passed` (retest Pass 2026-08-22/24) |
| Depth owner (#4530) | [#4909](https://github.com/intersoftdatalabs-in/percussioncms/issues/4909) **CLOSED** (slice 46: comment-required transition) — [PR #4922](https://github.com/intersoftdatalabs-in/percussioncms/pull/4922) **MERGED** |
| Code on `main` | `itemWorkflowApi.transitionWithComments*` (`WebUI/src/main/ts/api/contentExplorer/itemWorkflowApi.ts`), `workflowMenuActions` merged into toolbar/context menus, `SetWorkflowDialog.tsx` — all present |
| Matrix | `contracts/gap-matrix.md` row "Workflow transitions in menus" = **Present** |

Rationale: the `rest/actions` allowed-transitions path the plan asked to
"complete" is the exact surface #2732 wired (`workflowMenuActions` +
`transitionWithComments`) and #4909 hardened (comment gate). Nothing left to
plan.

## Row 2 — Properties dialogs beyond ACL (#2410)

| Item | Evidence (checked 2026-11-12) |
|------|-------------------------------|
| Owner | [#2410](https://github.com/intersoftdatalabs-in/percussioncms/issues/2410) **CLOSED** — [PR #2599](https://github.com/intersoftdatalabs-in/percussioncms/pull/2599) **MERGED** |
| Human QA | [#2600](https://github.com/intersoftdatalabs-in/percussioncms/issues/2600) **CLOSED** (`COMPLETED`), labelled `QA: Passed`; QA-Failed residual [#3206](https://github.com/intersoftdatalabs-in/percussioncms/issues/3206) **CLOSED**, retest [#2781](https://github.com/intersoftdatalabs-in/percussioncms/issues/2781) **CLOSED** |
| Object-ACL peer (cross-epic, not #2400 backlog) | #2274 family B1–B5 merged; peer Playwright [#2642](https://github.com/intersoftdatalabs-in/percussioncms/issues/2642) merged; QAs [#2640](https://github.com/intersoftdatalabs-in/percussioncms/issues/2640) **CLOSED** labelled `QA: Passed`, [#2643](https://github.com/intersoftdatalabs-in/percussioncms/issues/2643)/[#2672](https://github.com/intersoftdatalabs-in/percussioncms/issues/2672) **CLOSED** |
| Code on `main` | `FolderPropertiesEditor` (community, locale, display format, workflow) mounted inside `FolderSecurityPanel.tsx` on the product shell — present |
| Matrix | "Folder properties" row = **Present**; "Object ACL editor" = **Partial** cross-epic pointer (tracks on #1690/#2274, not #2400) |

Rationale: "beyond ACL" is the `FolderPropertiesEditor` half of #2410, shipped
with the ACL half. Object-ACL depth explicitly lives outside #2400 (matrix
cross-epic pointer). Nothing left to plan under Phase 3.

## Row 3 — New content menus

| Item | Evidence (checked 2026-11-12) |
|------|-------------------------------|
| Create asset in folder | [#4970](https://github.com/intersoftdatalabs-in/percussioncms/issues/4970) **CLOSED** — [PR #4982](https://github.com/intersoftdatalabs-in/percussioncms/pull/4982) **MERGED** |
| New copy of selected item | [#5006](https://github.com/intersoftdatalabs-in/percussioncms/issues/5006) **CLOSED** — [PR #5020](https://github.com/intersoftdatalabs-in/percussioncms/pull/5020) **MERGED** |
| Promotable version | [#5007](https://github.com/intersoftdatalabs-in/percussioncms/issues/5007) **CLOSED** — [PR #5027](https://github.com/intersoftdatalabs-in/percussioncms/pull/5027) **MERGED** |
| Flush assembler cache | [#5008](https://github.com/intersoftdatalabs-in/percussioncms/issues/5008) **CLOSED** — [PR #5035](https://github.com/intersoftdatalabs-in/percussioncms/pull/5035) **MERGED** |
| Code on `main` | `CreateAssetDialog` wired in `ReducedActions.tsx`, `multiSelectNewCopy.ts` — present |
| Matrix | "Reduced create/rename/move/copy/delete" (`ReducedActions` in shell) = **Present** |

Rationale: the "content-type / template menus … partially on `rest/actions`"
the plan noted are now the shipped #4530 create/copy/promotable/flush-cache
slices. Nothing left to plan.

## Decision

- [x] Each Phase 3 row mapped to its #4530 slice (or other owner), or no-gap recorded with rationale — **no-gap, rationale above**.
- No residual filed: the mapping *is* the closure record, and umbrella #4530 remains the residual tracker per repo convention (umbrellas stay open).
- Plan table ([plan.md](../plan.md) § Phase 3) updated to point at this note.
