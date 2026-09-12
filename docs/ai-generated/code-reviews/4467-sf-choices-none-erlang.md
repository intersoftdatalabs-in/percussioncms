# Erlang review: #4467 SharedFieldGroupDetailPanel choices-none Vitest

## Summary

Cycle Verify #4467 failed `SharedFieldGroupDetailPanel > sends choices type none when the catalog is cleared` on PR #4466 tip **and** independently on `origin/main` (same test/panel files). The PUT contract (`choices: { type: "none" }`) is already correct; the spec raced GET `controlProperties`. `developer-ct-ch-type` mounts disabled while `controlPropsLoading` is true, so `setType` no-ops and `handleSaveControlProperties` returns before `replaceSharedFieldControlProperties`.

Fix: wait for enabled type=`local` plus a local entry, then wait for Save enabled after selecting `none`. No production UI change.

## Scope

- Branch: `fix/issue-4467-sf-choices-none` vs `origin/main`
- Files: `WebUI/src/test/ts/developer/SharedFieldGroupDetailPanel.test.tsx`
- Memory patterns: Tests that proceed before async load; false green / ignored enablement
- Cross-platform path review: no filesystem path I/O in the diff
- Playwright: N/A (test-only; product chrome unchanged)

## Recommendation

approve

## Gate

May commit/push: yes

## Issues

None.
