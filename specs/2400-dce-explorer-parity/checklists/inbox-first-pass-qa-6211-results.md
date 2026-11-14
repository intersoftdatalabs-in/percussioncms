# QA record — Explorer Inbox leaf + execute first pass (issue #6211)

Date (UTC): 2026-11-14
Executed by: night-issue-prs work agent (OpenCode, muse-spark-1.3-contributor)
Parent: #6107 (gap-matrix re-audit; row: Inbox) — CLOSED; grandparent #2400 (closed) / successor epic #4530 (open)
Playwright specs:
`modules/perc-qa-automation/frontend/tests/explorer-inbox.spec.js`
Result: **PASS — agent first-pass green (2 passed, 0 skipped)**

## Environment

- Fresh CMS-only H2 QA cell from `python docker/scripts/perc-devctl.py qa-up`
  (branch tip = `origin/main`; no product code changed by this QA ticket, so no
  hot-deploy — cell runs the merged product route).
- `qa-health`: `RESULT:OK … HEALTH:healthy` on `/Rhythmyx/rest/mimetypes`.
- `TEST_CMS_URL=http://127.0.0.1:9993` (freeport contract).

## Test-plan mapping (issue #6211)

| # | QA step | Automated equivalent | Result |
|---|---------|----------------------|--------|
| 1 | Healthy H2 QA stack (`qa-up` → `qa-health` RESULT:OK); note TEST_CMS_URL / host port | `qa-up` RESULT:OK, `qa-health` RESULT:OK HEALTH:healthy; `TEST_CMS_URL=http://127.0.0.1:9993` | PASS |
| 2 | Login as Admin → `/Rhythmyx/cm/app/spa.jsp?entry=explorer` → Views → My Content → Inbox leaf visible | `Explorer shell a11y; Inbox leaf under Views My Content`: shell + views tree + Inbox leaf visible (no skip — catalog has Inbox) | PASS |
| 3 | Run Inbox → results region shows assignments or the empty state (empty HTTP 200 is success) | `run Inbox lists assignments or honest empty`: leaf click → `POST /services/views/{id}/execute` → 200 → `No items in this view` empty state | PASS |
| 4 | `npm run test:surface -- --path tests/explorer-inbox.spec.js` exits 0 with no skip when the leaf exists | `2 passed (5.1s)`, 0 skipped, exit 0 | PASS |

## REST evidence (same cell, authenticated session)

- `GET /Rhythmyx/services/views` → **200**, 7 view defs; Inbox present
  (`{"id":1,"name":"Inbox","label":"Inbox","customView":true,…}`).
- `POST /Rhythmyx/services/views/1/execute`
  (`{ViewExecuteRequest:{}, startIndex:0, maxResults:25}`) → **200**
  `{"ViewExecuteResult":{"displayFormatId":1,"startIndex":1,"totalCount":0,"viewName":"Inbox"}}`
  — empty assignment list is HTTP 200 success, not a failure.

Console: asserted zero unexpected JS console/page errors in-spec
(`attachPageErrors`; 404/400 resource filters only) — both tests green.

## server.log note

Zero `ERROR`-level and zero `FATAL`-level lines in
`/opt/Percussion/jetty/base/logs/server.log` for the whole cell lifetime
(only case-insensitive `error` hits are INFO lines about the error manager /
`DefaultError.xsl` i18n processing).

## Live proof

`docs/ai-generated/proofs/issue-6211-explorer-inbox.png` — Views → My Content →
Inbox leaf selected, results region `No items in this view`.

## Baseline

`tests/golden-unattended-smoke.spec.js` → **2 passed** (env resolve + Admin
login + Explorer shell) on the same cell.

## Pass / fail verdict (per issue criteria)

- **PASS**: leaf visible on the product route; execute returns 200 with the
  empty state; no new ERROR/FATAL in `server.log`.

## Out of scope (per issue)

- Full perc-qa-automation suite.
- Multi-RDBMS matrix.
- Product code changes (none made; none needed — file an implement residual if
  a future run fails instead of fixing here).
- Non-Inbox custom URL views (#4928).

## Note on the gap-matrix row

This is an **agent-executed first pass**, not a human-QA pass. Per the
gap-matrix contract (`specs/2400-dce-explorer-parity/contracts/gap-matrix.md`:
"**Never Present** from agent merge or docs/Playwright alone"; "flip …
Inbox before #6211 pass" forbidden), the Inbox row stays **Partial** until a
human QA pass is recorded. This PR supplies the evidence; it does not flip
the row and does not close #6211 by itself — human sign-off still required.
