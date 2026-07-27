# Erlang Code Review: 993-workflow-admin-react-ui-us2

## Summary

This review covers Phase 4 (User Story 2: Assign Workflows to Sites and Folders) for Feature 993 (Unified Workflow & Admin React UI).
The implementation introduces `WorkflowSiteAssign.tsx` to handle workflow-to-folder assignment via background job execution (`GetAssociatedFoldersJob/start`) and status polling (`workflowassignment/isInProgress`).
Code quality is high: async polling lifecycle is properly cleaned up via `useRef` and `useEffect` unmount handlers, UI states are decoupled from locale text via explicit `JobState` enums, and comprehensive Vitest unit tests back the behavior.

## Scope

- Base: `5898108648` (`development`)
- Head: `993-workflow-site-assignment`
- Files: 6 changed (351 insertions, 6 deletions)
- Prior report: `docs/ai-generated/code-reviews/993-workflow-admin-react-ui-erlang.md`
- Memory patterns hit: `tests.structural-only` (verified tests exercise behavioral async loop), `paths.hardcoded-sep` (verified REST URLs use `/`)

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Cross-Platform Path Review

- REST API path templates and URL routing parameters correctly use `/`.
- No filesystem path joins or OS-dependent separators introduced.
- Cross-platform path review outcome: **Pass (no issues)**.

## Re-review (Async Polling Overlap Fix)

- Updated `WorkflowSiteAssign.tsx` to replace `setInterval` with recursive `setTimeout` (`pollTimerRef`).
- Added `isMountedRef` check inside `pollJobStatus` to ensure state updates (`setJobState`, `setJobStatusMsg`) do not run if the dialog unmounts while a status GET request is in-flight.
- Next poll tick is scheduled only after the prior request resolves, preventing concurrent in-flight polling requests.
- Re-review recommendation: **approve**.

## Issues

No blocking bugs or suggestions identified.

### Verification Matrix

- `npm test`: 6/6 tests passing (including `WorkflowSiteAssign.test.tsx`).
- `i18n`: 100% TMX message coverage (`message(WF_ADMIN_MSG.*)`).
- `React`: Clean effect unmount, `isMountedRef` guards, and recursive `setTimeout` interval cleanup.

