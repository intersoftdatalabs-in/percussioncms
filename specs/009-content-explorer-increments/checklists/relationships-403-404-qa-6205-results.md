# QA results — relationships 403/404 for unresolvable ids (#6205)

QA verification slice for merged PR #6187 (fix(relationships): unresolvable
ids return 403/404 instead of 500, parent #6130). Executes the three numbered
test-plan steps from issue #6205 against the H2 QA cell and records the
results here.

**Result: PASS** — `/summary` for the unresolvable id returns the documented
403 (no 500, no server ERROR burst); `/edges` degrades gracefully to a 200
empty list (no 500); DependencyViewer renders the auth state for the live
missing-id answer; zero JS console/page errors; zero `server.log`
ERROR/FATAL.

## Cell

- `python docker/scripts/perc-devctl.py qa-up` → container `perc-matrix-cms-h2`
  (reused the healthy cell already up from the sibling QA seat),
  `TEST_CMS_URL=http://127.0.0.1:43677` (H2)
- `python docker/scripts/perc-devctl.py qa-health` → `RESULT:OK`,
  `HTTP:200 HEALTH:healthy`

## Test plan

Probe: `tests/zz-qa-6205-relationships-708-tmp.spec.js` (temporary, removed
after the run — same precedent as #6204).

### 1. `GET .../relationships/708/summary` → 403, not 500 — PASS

- Live response: **403** with body
  `{"Error":{"errorCode":99,"errorType":"WebApplicationException","message":"Cannot summarise node 708"}}`.
- Not 500; no `PSRuntimeExceptionMapper` ERROR burst (see Quality signals).

### 2. `GET .../relationships/708/edges` → graceful, not 500 — PASS

- Live response: **200** with body `{"PSExplorerRelationshipList":""}` —
  an empty edge list, i.e. the "graceful unavailable degradation" the test
  plan calls for (the missing item owns no relationships, so there is nothing
  to list; no exception, no 500).
- Note: PR #6187's body predicted 404 on `/edges` via `listOwned` NOT_FOUND,
  but on H2 the relationship query for the gone content id returns zero rows
  instead of throwing, so the adaptor returns LISTED-empty → 200. This is
  within the endpoint's documented `200 OK` shape and satisfies the issue's
  fail criterion (fail only on 500/ERROR). No residual filed: no 500, no
  ERROR, no contract violation.

### 3. DependencyViewer for a missing item renders auth/missing — PASS

- The probe captured the server's genuine 708 summary answer (403 + body)
  first, then opened the modern Content Explorer, selected a real
  page/asset row, and fulfilled the viewer's summary call with that exact
  live status + body — so the rendered state is driven by the real server
  contract, not a stub.
- Viewer reached `data-testid-state="auth"` (403 → auth mapping) with zero
  `dependency-empty` nodes and zero console/page errors.
- Live proof: `docs/ai-generated/proofs/issue-6205-relationships-auth.png`
  (viewer auth-state crop) and
  `docs/ai-generated/proofs/issue-6205-relationships-auth-page.png`
  (full explorer context showing the permission-denied viewer region).

## Quality signals

- `npm run test:surface -- --path tests/zz-qa-6205-relationships-708-tmp.spec.js`
  (temporary probe spec, removed after the run) → **2 passed**,
  `assertConsoleClean`-equivalent in-spec gate (pageerror + console error,
  known transient network noise allowlisted per
  `explorer-console-clean.js`)
- `docker exec perc-matrix-cms-h2 grep ' ERROR '` server.log → **0 lines**
  (before and after the run); `grep ' FATAL '` → **0 lines**; no
  `relationships/708` or `Failed to get locator for item id=708` lines
- `cd modules/perc-qa-automation && ../../mvnw clean install` → BUILD SUCCESS

## Out of scope (per #6205)

- Relationship graph semantics for resolvable items.
- Typed dimension endpoints (`/outgoing`, `/incoming`, …) beyond the
  summary/edges pair in the test plan.
- Non-H2 RDBMS matrix.

## Related

- Originating bug: #6130 (closed by #6187)
- Merged fix: PR #6187
- Tracking: [night-issue-prs] run 2026-11-14
