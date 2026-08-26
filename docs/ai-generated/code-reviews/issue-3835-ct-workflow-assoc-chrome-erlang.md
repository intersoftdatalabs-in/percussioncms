# Erlang review: issue #3835 Developer Content Type workflow-assoc chrome (Cycle Verify)

| Field | Value |
|-------|--------|
| **Date** | 2026-08-26 |
| **Branch** | `fix/issue-3835-ct-workflow-assoc-chrome` |
| **Base** | `origin/main` (#3833 cluster already merged) |
| **Recommendation** | approve |
| **Gate** | May commit/push: yes |
| **Memory patterns hit** | Change-class closure (WebUI panel + Vitest + Playwright + product-docs); lock chrome must be findable without scrolling past fields table (peer #3834); unlocked association editors stay disabled |

## Summary

Cycle Verify residual for `tests/developer-content-type-workflows.spec.js` on the #3833 cluster tip. Cluster union left Lock / Save / Unlock after the fields table and gated on GET `detail`, so H2 Playwright timed out on `developer-ct-lock` and saw `developer-ct-wf-add-name` enabled while unlocked.

This change always mounts a **sticky top** lock toolbar (Lock / Save / Unlock / Enabled) before GET detail finishes. Workflow add/remove/default no-op without a held lock. Playwright waits for the toolbar and asserts add-name disabled. Product-docs: toolbar at top; Add/Remove stay disabled until Lock.

## Gate

- No bugs found. Standalone `WebUI` and `modules/perc-qa-automation` `mvnw clean install` succeeded (Vitest 3118 passed).
- Behavioral tests: toolbar present while detail is loading; workflow add-name disabled after load; unlocked add/remove ignored; existing CD-08 lock → PUT path unchanged.
- Companions: Playwright workflows spec (toolbar + lock timeout); lock-save / template specs wait for the same toolbar; product-docs Developer Content Types.
- Cross-platform path checklist: N/A (no filesystem path/file I/O; REST/URL `/` only).
- C2 reverse-deps: none (no Java public type/signature change).

## Issues

None.

## Notes (non-blocking)

- Peer residual #3834 / PR #3840 moved the same toolbar for lock-save Playwright. This PR is stacked on merged #3833, not on #3840 (CONFLICTING vs current main). Landing both will need a small toolbar-union if #3840 is still open.
- Unlocked workflow add still uses native `disabled` plus handler guards so a click on a disabled control cannot dirty the set.
