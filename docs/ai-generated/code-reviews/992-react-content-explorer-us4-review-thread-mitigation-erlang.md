# Erlang Review — 992-react-content-explorer US4 (P-ACL) review-thread mitigation

**Branch**: `992-react-content-explorer-us4` (off `origin/development` HEAD `07de79f0ac`)
**Base**: `development`
**Reviewer**: Erlang (independent)
**Date**: 2026-07-20
**Scope**: Re-review of fixes for the 4 open review threads on PR #1397
(`992-react-content-explorer-us4`):
- `PRRT_kwDOKZBp3M6SP3Wc` (line 1, FolderSecurityPanel.tsx — CRITICAL: lockout detection broken)
- `PRRT_kwDOKZBp3M6SP3Wi` (line 1, FolderSecurityPanel.tsx — WARNING: confirmLockout unhandled)
- `PRRT_kwDOKZBp3M6SP3Wo` (line 1, us4-acl.spec.js — SUGGESTION: ACL_URL cache-buster at module load)
- `PRRT_kwDOKZBp3M6SP3Ws` (line 1, FolderSecurityPanel.tsx — SUGGESTION: DEFAULT_PROPS dead code)

## Summary

All 4 review findings are addressed in the fix pack:

1. **CRITICAL lockout detection fix** — `originalPermissionRef` (a
   `useRef<PSFolderPermission|undefined>`) is set when the panel loads
   its own data via the `initial`-less code path. `attemptSave` reads
   `originalPermissionRef.current` for the `before` snapshot, so the
   `detectSelfLockout` call no longer collapses to `before === after`.
2. **WARNING confirmLockout try/catch** — the host-supplied
   `confirmLockout` call is wrapped in try/catch; a rejection is
   treated as "user cancelled" (no save proceeds) with a console.warn
   for ops visibility.
3. **SUGGESTION ACL_URL cache-buster** — the constant is replaced
   with a per-call `aclUrl(folderId)` helper so each `test()` gets a
   fresh `Date.now()`. (Same pattern used by the sibling specs.)
4. **SUGGESTION DEFAULT_PROPS dead code** — the unused constant is
   removed; `originalPermissionRef.current ?? current.permission`
   is the new fallback chain.
5. **Bonus rules-of-hooks fix** discovered during the new
   `self-lockout warning fires WITHOUT initial props` test — the
   `drafts` `useMemo` was previously defined AFTER the
   early-return branches (loading / error / no-access). When the
   panel rendered in the loading state on mount and then resolved to
   the ready state, React threw "Rendered more hooks than during the
   previous render". The `useMemo` is now defined BEFORE the early
   returns so the hook order is stable across renders.

All 13 FolderSecurityPanel Vitest tests green (+2 new fix-pack
tests). All 5 US4 Playwright tests green.

## Scope

- Base: `origin/development` HEAD `07de79f0ac`
- Head: `992-react-content-explorer-us4` (working tree, uncommitted)
- Files: 3 changed
  - `WebUI/src/main/ts/contentExplorer/FolderSecurityPanel.tsx` (modified)
  - `WebUI/src/test/ts/contentExplorer/FolderSecurityPanel.test.tsx` (modified, +61)
  - `modules/perc-qa-automation/frontend/tests/us4-acl.spec.js` (modified)
- Prior reports (continuity):
  - `docs/ai-generated/code-reviews/992-react-content-explorer-us4-erlang.md` (initial US4 approval)
  - kilo-code-bot review threads `PRRT_kwDOKZBp3M6SP3Wc|Wi|Wo|Ws` (this PR's targets)
- Memory patterns hit: React rules-of-hooks (every render must call hooks in the same order); per-test cache-buster helpers; try/catch around host-supplied callbacks; pure-helper tests pinned to the fix scenario.

## Recommendation

`approve`

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

(None.)

## Change-by-change verdict

### `WebUI/src/main/ts/contentExplorer/FolderSecurityPanel.tsx` (modified)

- `originalPermissionRef` added (a `React.useRef<PSFolderPermission
  | undefined>(initial?.permission)`). The `useEffect` for the
  own-data load sets it on resolve; the subsequent save uses it for
  the `before` snapshot. Mitigation for thread `PRRT_kwDOKZBp3M6SP3Wc`.
- The `?? current.permission` fallback chain preserves the original
  `attemptSave`-cannot-snap the user's own initial behaviour: if
  no snapshot is available (no initial + load rejected → status
  = error), the comparison falls through to the current state and
  the `detectSelfLockout` returns empty (the user's edit hasn't
  been committed yet, so no lockout warning is appropriate).
- The `confirmLockout` call inside `attemptSave` is wrapped in
  try/catch; a host-supplied callback that throws is treated as
  cancel so the save does NOT proceed without explicit confirmation.
  A `console.warn` surfaces the reason for ops. Mitigation for
  thread `PRRT_kwDOKZBp3M6SP3Wi`.
- The unused `DEFAULT_PROPS` constant is removed. Mitigation for
  thread `PRRT_kwDOKZBp3M6SP3Ws`.
- The `drafts` `useMemo` is moved BEFORE the early-return branches
  so the hook order is stable when `status.kind` transitions from
  `loading` to `ready` between renders. The memoization body pulls
  `permission` from `status.props.permission` when in the ready
  state, falling back to `undefined` otherwise (the `useMemo` body
  returns `[]` when `permission` is `undefined`). Same data the
  previous arrangement produced — the change is purely about hook
  ordering.

### `WebUI/src/test/ts/contentExplorer/FolderSecurityPanel.test.tsx` (modified, +61)

- 2 new tests pin the fix for kilo-code-bot thread 3614415903
  (`self-lockout warning fires WITHOUT initial props`) and the
  fix for thread 3614415910 (`a host confirmLockout that throws
  is treated as cancel`):
  - The first test renders with NO `initial` prop, lets the
    component's own load resolve, then removes the current user
    from adminPrincipals + clicks Save. The test asserts that
    `confirmLockout` was called (proving the lockout check fired)
    and that `save` was called (proving the user-confirmed
    proceed path worked). This test would have FAILED against the
    pre-fix code because the lockout check collapsed to `before ===
    after` and never fired.
  - The second test renders with `initial`, removes Admin,
    supplies a `confirmLockout` that rejects, and asserts that
    `save` was NOT called + `console.warn` was invoked. This test
    would have FAILED against the pre-fix code because the rejection
    would have surfaced as an unhandled promise rejection.
- The `:vitest-ignore-line` comment distinguishes the
  `console.warn` spy in the throwing-test (the spy is restored at
  the end of the test).

### `modules/perc-qa-automation/frontend/tests/us4-acl.spec.js` (modified)

- The module-load-time `const ACL_URL = \`${BASE_URL}/.../?_=${Date.now()}\`;`
  is replaced with a `function aclUrl(folderId)` helper. The helper
  evaluates `Date.now()` per call (the cache-buster). The five
  callsites updated to use `aclUrl()` / `aclUrl("0")`.
- This change matches the per-test cache-buster pattern already used
  in `us3-menus.spec.js` / `us5-search.spec.js` / `us7-advanced.spec.js`
  (consistency across the QA-automation module). Mitigation for thread
  `PRRT_kwDOKZBp3M6SP3Wo`.

## Cross-platform path review

Not applicable — the changes are TS / test-file logic; URL constants
remain on the established `BASE_URL + path + cache-buster` pattern.

## PR thread protocol

This review closes all 4 open review threads. After commit + push,
each thread gets an inline reply with the mitigation commit hash,
followed by `gh api graphql resolveReviewThread` per thread. The
GraphQL `reviewThreads(first: 50) { nodes { isResolved } }` re-query
afterwards confirms `isResolved: true` for every thread.

## Handoff

- Recommendation: `approve`. May commit/push: yes.
- Suggested commit message:
  `fix(992/us4): address kilo-code-bot PR #1397 review threads (lockout detection + confirmLockout try/catch + aclUrl helper + useMemo order)`
- After PR is updated and threads are resolved, US5's open thread
  can be addressed (see sibling fix-pack), then move to the Polish
  phase (T082–T091) per tasks.md.

