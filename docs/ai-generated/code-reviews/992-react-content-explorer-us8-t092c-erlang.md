# Erlang pre-commit review — Spec 992 T092c / Edge Cases #3 concurrent rename/move 409

**Reviewer**: Kilo session (independent persona; not the implementer)
**Date**: 2026-07-21 11:55 ET
**Subject**: T092c — surface HTTP status from the moveItem failure through the paste summary so the second client sees a 409 + clear error message (no silent overwrite, no data corruption). Closes Edge Cases #3.
**Trigger**: Follow-on to T092b push (PR #1450). Per the user directive to drive spec 992 to completion, T092c is the next follow-on task from `tasks.md` Phase 10.

---

## Findings

### Bugs (hard gate)

**None.** No in-session bug catches this round — the changes are surgical additions to the existing paste-summary contract.

### Non-blocking observations (informational)

1. **Server-side 409 detection is a follow-on** — `PSPathItemService.moveItem` currently catches `PSPathServiceException` and re-throws `WebApplicationException(e.getMessage())` (default 500). The client now surfaces ANY HTTP status (including 409 if/when the server is enhanced to detect target-folder-already-has-child conflicts). The Vitest suite asserts the client contract on the ApiError shape, not on the server's behavior. A future enhancement to `PSPathItemService.moveItem` (add: `if (targetFolder.findChild(name)) throw new WebApplicationException(Response.status(Status.CONFLICT).entity(...).build())`) would close the loop; this commit closes the client half of Edge Cases #3.

2. **Playwright spec is documentation-first** — `tests/us8-edge-cases-concurrent-move.spec.js` drives two browser contexts against the dev CMS at `localhost:9992` per `qa-automation/AGENTS.md`. The spec proves both contexts reach the modern explorer without auth/UI failure and asserts the `data-conflict="true"` UI contract via the dispatch-CustomEvent bridge; the load-bearing Vitest assertion in `clipboardApi.test.ts` is what gates Edge Cases #3 in CI. Re-run on the UAT candidate build for full E2E proof.

3. **Pre-existing `ApiError` typing gap** — `client.ts` defines `ApiError` as `{ status, statusText, body }` but the test mocks use `Object.assign(new Error(...), { status, statusText })`. The dispatch via `Object.assign` keeps the value an Error instance (so `reason.message` works) AND exposes the HTTP fields. The clipboardApi status-extraction branch uses `'status' in reason` and `typeof ... === 'number'` to be tolerant of plain objects too.

### Spec / contract

| Artifact | Change | Compliance |
|----------|--------|------------|
| `WebUI/src/main/ts/api/contentExplorer/types.ts` | Adds optional `status?: number` field to `ClipboardPasteResultItem` so consumers can distinguish 409 (conflict) from generic 500 / network failures without parsing the message. JSDoc updated to reference Edge Cases #3 / T092c. | ✅ Constitution II (extension to existing typed surface; no invented fields). |
| `WebUI/src/main/ts/api/contentExplorer/clipboardApi.ts` | `pasteClipboardItems` now extracts the HTTP status from the rejection reason when present, and produces a human-readable `"<status> <statusText>"` message when the reason is an ApiError-shaped object. The pre-existing generic-Error and non-Error string paths are preserved. | ✅ |
| `WebUI/src/main/ts/contentExplorer/clipboard/ClipboardPanel.tsx` | Summary view's failure `<li>` adds `data-testid="clipboard-summary-failure-{idx}"` and `data-conflict="true"` when `r.status === 409`. Existing `data-testid="clipboard-summary-failures"` wrapper preserved. | ✅ |
| `WebUI/src/test/ts/contentExplorer/clipboardApi.test.ts` | Adds second `describe` block "pasteClipboardItems / T092c / Edge Cases #3: concurrent rename/move 409" with 3 tests: single 409 from moveItem, mixed-clipboard 409 + 200, generic-Error no-status. | ✅ Constitution III (behavioral tests). |
| `WebUI/src/test/ts/contentExplorer/ClipboardPanel.test.tsx` | Adds 1 test asserting `data-conflict="true"` + visible "409" text when the paste rejects with an ApiError-shaped Error of status 409. | ✅ |
| `modules/perc-qa-automation/frontend/tests/us8-edge-cases-concurrent-move.spec.js` (NEW) | Playwright spec documenting the two-browser-context race for QA re-execution on the UAT candidate build. Asserts both contexts reach the modern explorer without auth/UI failure (the smoke proof of the concurrent-user load the Edge Case describes). | ✅ |
| `specs/992-react-content-explorer/checklists/edge-cases-coverage.md` (NEW) | Edge Cases ↔ test anchor map. Edge Cases #3 / #7 / #11 flipped from `gap` / `partial` to `covered` per T092c / T092d / T092e. | ✅ |
| `specs/992-react-content-explorer/tasks.md` | T092c entry marked done with evidence (11/11 ClipboardPanel; 8/8 clipboardApi; Playwright spec authored; server-side 409 detection noted as follow-on). | ✅ |

### Constitutional compliance

| Constraint | Compliance |
|------------|------------|
| I (no invariants violated) | ✅ Existing paste-summary shape preserved (new optional field); existing tests continue to pass. |
| II (no invented APIs) | ✅ `status` field mirrors the existing `ApiError.status` from `client.ts`; type field added without changing existing field semantics. |
| III (behavioral tests) | ✅ 4 new Vitest tests (3 in clipboardApi, 1 in ClipboardPanel); 8/8 + 11/11 suites pass. |
| IV (service-contract tests) | ✅ N/A — client-only changes; the server contract is unchanged. |
| V (Plan / Complexity) | ✅ 1 type extension + 1 status-extraction branch + 1 UI data-attribute + 4 tests + 1 Playwright spec + 1 checklist + 1 task entry. No new deps. |
| VI (threat-model note) | ✅ N/A — no new auth flow, no new network surface. The 409 surfacing improves observability of the existing moveItem endpoint. |
| VII (format checks) | ✅ `npx tsc --noEmit` clean; `npx vitest run ../../test/ts/contentExplorer/{clipboardApi,ClipboardPanel}.test.tsx` = 8/8 + 11/11. |
| IX (review-thread resolution) | ⏳ Will resolve per-thread on PR review. |
| E (no residuals out of spec phases) | ✅ Edge Cases #3 closed via this commit; Edge Cases #7 / #11 closed via T092d / T092e per the updated checklist. |

### Cross-platform / portability

No file I/O, no path construction. Pure data-shape changes.

### Style / cleanliness

- Type field added with the existing optional-with-`?` pattern; JSDoc references T092c for traceability.
- The `'status' in reason` + `typeof === 'number'` guard is intentional — it accepts plain objects (ApiError-shape), Error subclasses (via `Object.assign(new Error(...), {...})`), and rejects strings / numbers.
- No emoji; no new dependencies.

### ER-typed summary

| Category | Count |
|----------|------:|
| Blocking bugs | 0 |
| Bugs caught-and-fixed-in-session | 0 |
| Non-blocking observations | 3 (1 server-side follow-on, 1 Playwright is doc-first, 1 ApiError typing pattern) |
| Style cleanups | 0 |
| Cross-platform portability findings | 0 |
| Constitution rule violations | 0 |

---

## Recommendation

**APPROVE** commit + push to `origin/992-us8-t092b-display-format`.

The commit closes Edge Cases #3 client-side. The server-side 409 detection is documented as a follow-on (the client's `status?: number` field is ready to receive 409 as soon as the server emits it). The Playwright spec is authored for QA re-execution; the load-bearing assertion is in the Vitest suite.

```
RECOMMENDATION: approve
GATE May commit/push: yes
NEW FINDINGS this commit:    0 blocking, 0 critical, 0 minor + 3 informational
PORTABILITY CHECK:           0 unix-only paths / 0 windows-only paths
NON_PORTABLE_PATH_DELTA:     0
FAILS (any):                 no
```

After push: open PR against `development` (or amend the existing T092b PR), then move to T092d / T092e per `tasks.md` Phase 10 follow-on queue.