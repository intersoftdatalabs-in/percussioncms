# Erlang review — issue #2140 locales live 2xx REST smoke

**Date:** 2026-08-06  
**Branch:** `fix/issue-2140-locales-live-2xx-smoke`  
**Reviewer persona:** Erlang (pre-commit gate)  
**Verdict:** **approve**

## Scope

Residual **#2140** of unit slice **#2126** / open unit PR **#2141** (parent **#2117** / epic **#1694**).

Live probe on H2 `perc-devctl qa-up` proved `GET /Rhythmyx/services/locales` → **HTTP 200** with Jackson `{ LocaleSummary: [...] }` (stock system locales). No product 5xx stack.

Change is **QA acceptance only**:

|                                    Path                                     |                         Change                         |
|-----------------------------------------------------------------------------|--------------------------------------------------------|
| `modules/perc-qa-automation/frontend/tests/developer-catalog-smoke.spec.js` | REST smoke for locales 2xx + `LocaleSummary` structure |
| This report                                                                 | Erlang durable record                                  |

**Out of scope (do not duplicate):** unit harden / `requireAdaptor` 503 mapping in open PR **#2141**.

## Checklist

|           Gate            |                                     Result                                      |
|---------------------------|---------------------------------------------------------------------------------|
| Bugs in new logic         | None — peer copy of slots/keywords REST probe pattern                           |
| Behavioral tests          | Playwright REST assertion is the behavioral gate for live residual              |
| Portable paths / file I/O | None touched                                                                    |
| Change-class companions   | REST residual class: smoke only when live already 2xx (peers #2121/#2124/#2146) |
| Secrets                   | No hardcoded password/port; uses `adminBasicAuthHeaders()` + `BASE_URL`         |
| Unit re-implementation    | Avoided — no rest/sitemanage edits                                              |

## Live evidence (session)

- `TEST_CMS_URL=http://127.0.0.1:9993` (`perc-matrix-cms-h2`)
- Basic + `RX_USEBASICAUTH: true`
- `GET /Rhythmyx/services/locales` → **200** `LocaleSummary` array (e.g. Arabic, …)
- Peer controls: slots/keywords/extensions also 200 on same cell

## Notes for merge

- Concurrent open PRs (#2161 keywords, #2162 searches matrix, #2163 extensions) also touch `developer-catalog-smoke.spec.js` — expect merge conflict resolution to keep **all** REST cases (slots, keywords, locales, extensions, searches…).
- `restLocalesResource` already registered on `rest-jax-rs` serviceBeans (unlike searches pre-#2162).

