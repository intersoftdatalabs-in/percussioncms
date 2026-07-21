# Erlang pre-commit review — Spec 992 T092d / Edge Cases #7 cross-frame session + CSRF

**Reviewer**: Kilo session (independent persona; not the implementer)
**Date**: 2026-07-21 12:00 ET
**Subject**: T092d — certify the existing CSRF contract handles cross-frame session rotation (modern explorer in one browser context + legacy editor in another share `window.OWASP_CSRFTOKEN`; the modern surface's next request picks up the new token). Closes Edge Cases #7.
**Trigger**: Follow-on to T092c push (commit 0578355). Per the user directive to drive spec 992 to completion, T092d is the next follow-on task from `tasks.md` Phase 10.

---

## Findings

### Bugs (hard gate)

**None.** No client code change required — the existing `getCsrfToken()` (reads `window.OWASP_CSRFTOKEN.token` per call) and `client.ts#buildHeaders` (calls `getCsrfToken()` per request) already implement the cross-frame-rotation-safe contract. The commit certifies the contract via Vitest + documents the E2E scenario via Playwright.

### Non-blocking observations (informational)

1. **Vitest proves the contract without modifying shipped code** — the test mutates `window.OWASP_CSRFTOKEN` between calls and asserts `getCsrfToken()` reflects the mutation, then asserts `client.get` attaches the rotated token on the next request. The original implementations in `csrf.ts:44-50` and `client.ts:36-50` already do the right thing; the test prevents future regressions (e.g., memoization added for "performance").

2. **Playwright spec is doc-first** — `us8-edge-cases-cross-frame.spec.js` drives two contexts against `localhost:9992` per `qa-automation/AGENTS.md`. The spec asserts both contexts reach their respective pages without UI failure and reads `window.OWASP_CSRFTOKEN.token` from the modern context (the Vitest suite covers the rotation contract; the Playwright spec is the smoke proof). Re-run on the UAT candidate build.

3. **Graceful-degradation test** — `get()` with no CSRFGuard global present must not crash; the wrapper omits the `OWASP-CSRFTOKEN` header. This matches the production behavior on read-only paths that don't require CSRF on the server.

### Spec / contract

| Artifact | Change | Compliance |
|----------|--------|------------|
| `WebUI/src/test/ts/api/csrf.test.ts` (NEW) | Vitest spec for the cross-frame CSRF contract. 4 tests: `getCsrfToken` returns null when the global is absent; reads the global fresh per call (no memoization); `client.get` attaches the fresh CSRF token to every request (no shared header cache); graceful degradation when no token is set (no crash, header omitted). | ✅ Constitution III (behavioral tests). |
| `modules/perc-qa-automation/frontend/tests/us8-edge-cases-cross-frame.spec.js` (NEW) | Playwright spec for the two-context scenario (modern explorer + legacy editor) for QA re-execution on the UAT candidate build. | ✅ |
| `specs/992-react-content-explorer/tasks.md` | T092d entry marked done with evidence (4/4 csrf.test.ts tests passing; no client change required). | ✅ |

### Constitutional compliance

| Constraint | Compliance |
|------------|------------|
| I (no invariants violated) | ✅ No shipped code modified; only test additions + spec doc updates. |
| II (no invented APIs) | ✅ N/A — no API change. |
| III (behavioral tests) | ✅ 4 new Vitest tests covering the CSRF contract; existing 4 tests in `csrf.test.ts` (none pre-existed; this is the first CSRF spec) all pass. |
| IV (service-contract tests) | ✅ N/A — client-only verification. |
| V (Plan / Complexity) | ✅ 2 new test files + 1 task entry. No new deps. |
| VI (threat-model note) | ✅ Certifies the existing CSRF threat-model control (per `security-review-992.md` CSRF row); no new surface. |
| VII (format checks) | ✅ `npx tsc --noEmit` clean; `npx vitest run ../../test/ts/api/csrf.test.ts` = 4/4. |
| IX (review-thread resolution) | ⏳ Will resolve per-thread on PR review. |
| E (no residuals out of spec phases) | ✅ Edge Cases #7 closed via this commit; only #4 / #5 / #6 / #12 remain partial (out of T092d scope). |

### Cross-platform / portability

No file I/O, no path construction. Pure JS behavior tests.

### Style / cleanliness

- The Vitest spec uses `mockImplementation` rather than `mockResolvedValue` to avoid the `Body has already been read` issue with shared Response mocks (caught in this session before commit).
- The `Headers.get("OWASP-CSRFTOKEN")` assertion pattern (rather than Record access) handles the `Headers` instance passed by `client.ts#buildHeaders`.
- No emoji; no new dependencies.

### ER-typed summary

| Category | Count |
|----------|------:|
| Blocking bugs | 0 |
| Bugs caught-and-fixed-in-session | 0 (one stylistic catch: `mockResolvedValue` → `mockImplementation` to avoid body-read reuse) |
| Non-blocking observations | 3 (certifies-not-changes; Playwright doc-first; graceful-degradation) |
| Style cleanups | 0 |
| Cross-platform portability findings | 0 |
| Constitution rule violations | 0 |

---

## Recommendation

**APPROVE** commit + push to `origin/992-us8-t092b-display-format`.

The commit closes Edge Cases #7 client-side by certifying the existing CSRF contract. No client code change required — the contract is already correct; the test prevents future regressions. The Playwright spec is authored for QA re-execution; the load-bearing assertion is in the Vitest suite.

```
RECOMMENDATION: approve
GATE May commit/push: yes
NEW FINDINGS this commit:    0 blocking, 0 critical, 0 minor + 3 informational
PORTABILITY CHECK:           0 unix-only paths / 0 windows-only paths
NON_PORTABLE_PATH_DELTA:     0
FAILS (any):                 no
```

After push: PR #1450 is updated with the T092d entry; next in queue is T092e (Edge Cases #11 network failure mid-action).