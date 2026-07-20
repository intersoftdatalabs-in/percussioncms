# Erlang Review — 992-react-content-explorer US5 PR #1398 secondary review-thread mitigation (JSDoc broken-link fixes)

**Branch**: `992-react-content-explorer-us5` (off `origin/development` HEAD `07de79f0ac`)
**Base**: `development`
**Reviewer**: Erlang (independent)
**Date**: 2026-07-20
**Scope**: Re-review of fixes for the 2 NEW review threads on PR #1398 (`992-react-content-explorer-us5`) raised against the previous retry-button mitigation commit `aa2d605123`:
- `PRRT_kwDOKZBp3M6STcLE` (dbId 3615715921, `SearchPanel.tsx`) — Broken JSDoc `{@link SearchPanelProps.onRetrySearch}`
- `PRRT_kwDOKZBp3M6STcLJ` (dbId 3615715925, `SearchPanel.tsx`) — Same broken `{@link SearchPanelProps.onRetrySearch}`

Plus re-confirmation that the original `PRRT_kwDOKZBp3M6SQFSD` (the retry button missing) was correctly addressed in the prior mitigation commit (`aa2d605123`); the thread is now `isOutdated: true` because that prior commit moved the `error` JSDoc around.

## Summary

Two broken JSDoc links identified by kilo-code-bot after the retry-button fix. The JSDoc referenced `{@link SearchPanelProps.onRetrySearch}` which is not a real prop — the actual prop is `onRetry?: () => void` on `SearchStatusView` (the parent `SearchPanel` wires its own closure via `onRetry` directly). Both links replaced:
1. The JSDoc on `SearchPanel`'s state-machine description (lines 36–37) now points at `{@link SearchStatusView.onRetry}`.
2. The JSDoc on `SearchStatusView.onRetry`'s parameter (lines 170–171) now points at `{@link SearchPanel}` (the parent component owning the transport) instead of the non-existent `SearchPanelProps.onRetrySearch`.

All 9 SearchPanel Vitest tests still green. `npx tsc --noEmit` clean.

## Scope

- Base: `origin/development` HEAD `07de79f0ac`
- Head: `992-react-content-explorer-us5` (working tree, uncommitted)
- Files: 1 changed
  - `WebUI/src/main/ts/contentExplorer/SearchPanel.tsx` (modified, +5 / -3)
- Prior reports (continuity):
  - `docs/ai-generated/code-reviews/992-react-content-explorer-us5-erlang.md` (initial US5 approval)
  - `docs/ai-generated/code-reviews/992-react-content-explorer-us5-review-thread-mitigation-erlang.md` (retry-button fix; commit `aa2d605123`)
  - kilo-code-bot review threads `PRRT_kwDOKZBp3M6SQFSD`, `PRRT_kwDOKZBp3M6STcLE`, `PRRT_kwDOKZBp3M6STcLJ`

## Recommendation

`approve`

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

(None.)

## Change-by-change verdict

### `WebUI/src/main/ts/contentExplorer/SearchPanel.tsx` (modified, +5 / -3)

- Bullet 4 of the state-machine JSDoc (lines 36–37) replaced
  `{@link SearchPanelProps.onRetrySearch}` with the correct
  `{@link SearchStatusView.onRetry}` (and kept
  `{@link SearchPanel.runSearch}` for the inline default fallback).
- The `onRetry` parameter JSDoc on `SearchStatusView` now points
  at `{@link SearchPanel}` (the parent component) instead of the
  non-existent `SearchPanelProps.onRetrySearch`. The comment
  explains the contract: the parent owns the transport closure.
- No code change — purely JSDoc / TypeDoc cleanup. No runtime
  behavior change.

## Cross-platform path review

Not applicable — JSDoc cleanup only.

## PR thread protocol

After commit + push, all 3 review threads get inline replies with
the mitigation commit hash, followed by `gh api graphql resolveReviewThread`
per thread (where the thread is still active). The previously-resolved
retry-button thread (`PRRT_kwDOKZBp3M6SQFSD`) gets a re-confirmation note
and the GraphQL `resolveReviewThread` mutation is idempotent.

## Handoff

- Recommendation: `approve`. May commit/push: yes.
- Suggested commit message:
  `docs(992/us5): fix broken {@link SearchPanelProps.onRetrySearch} JSDoc refs after retry-button mitigation`

---

## Follow-up #2 (2026-07-20, post-rebase): ST4mT + sync merge

After the mitigation commit `67c0a165b8`, the Kilo Code bot re-ran a
review pass on the rebased PR #1398 (post-merge of spec 993
WorkflowAdminShell into origin/development) and raised one new
unresolved thread:

- `PRRT_kwDOKZBp3M6ST4mT` (dbId 3615878944):
  *Claim:* The replacement `{@link SearchStatusView.onRetry}` is not a
  valid reference because `onRetry` is a destructured prop parameter,
  not a property of the function component.

**Mitigation commit** `c2794aeede566f82807579cf3297ac1d37954d82`
(docs-only; 9 insertions, 4 deletions in
`WebUI/src/main/ts/contentExplorer/SearchPanel.tsx`):

- Replaced `{@link SearchPanel.runSearch}` (target also did not exist)
  with plain-text `` `runSearch` `` in the `onRetry` JSDoc.
- Replaced `{@link SearchStatusView.onRetry}` in the state-machine
  description with an explanatory prose pointer at the `onRetry` prop
  description inside `SearchStatusView` itself.
- Added a parenthetical note in the prop JSDoc documenting *why*
  `SearchStatusView.onRetry` is not a valid `{@link ...}` target.
- Verifying grep: remaining `{@link X}` references in the file all
  resolve to exported declarations —
  `SearchPanelProps.onOpen / .onReveal`,
  `searchExtended`, `SearchPanel`, `SearchStatusView`. None reference
  properties of function components.

**Sync merge** `bdb41210afdb074fd38ca7f28ddfe07ef534a582` (merge-only):
origin/development was brought in to clear the develop-vs-US5 drift
(carrying spec 993 WorkflowAdminShell + the upcoming
`obsolete-cleanup` reorg). Three rebase conflicts resolved:

1. `WebUI/src/main/ts/registry.ts` — concatenated imports and
   registrations for `SearchPanel`, `FolderSecurityPanel`,
   `ContextMenu`, `ActionToolbar`, and `WorkflowAdminShell`. No
   duplicate imports or `componentRegistry.set` calls remain.
2. `WebUI/src/main/ts/contentExplorer/messages.ts` — concatenated
   `SEARCH_*` (9 keys) + `SECURITY_*` (18 keys). No key overlap.
3. `WebUI/src/main/ts/api/contentExplorer/types.ts` — kept US5 search
   DTOs (`PSSearchCriteria`, `PSItemProperties`,
   `PSPagedItemPropertiesList`, `PSPagedItemPropertiesListEnvelope`,
   `PSSearchResults`) preceding the US3 action-menu DTOs (9
   interfaces).

**Verification (post-merge + post-JSDoc-cleanup)**:

```
$ npx vitest run SearchPanel.test.tsx                  -> 9/9 passing
$ npx vitest run SearchPanel + safeNavigate tests      -> 25/25 passing
$ npx vitest run US3/4/5 surface tests                 -> 53/53 passing
```

Final thread state on PR #1398: 4 threads, **0 unresolved**.

Recommendation unchanged: **approve**. May commit/push: **yes**.
No blocking bugs. No portable-path changes. No behavioral change.
