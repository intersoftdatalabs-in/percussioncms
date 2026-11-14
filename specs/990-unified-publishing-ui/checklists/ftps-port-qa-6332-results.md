# QA record — FTPS delivery-server port panel (issue #6332)

Date (UTC): 2026-11-14
Executed by: night-issue-prs work agent (OpenCode 2.3.0, muse-spark-1.3-contributor)
Spec: `specs/990-unified-publishing-ui` (slice 142, issue #6312; code merged via PR #6316)
Playwright spec:
`modules/perc-qa-automation/frontend/tests/publishing/sitesDeliveryServerFtpsPort.spec.js`
Result: **PASS — FTPS port panel green (2 passed + golden 2 passed)**

## Environment

- Fresh CMS-only H2 QA cell from `python docker/scripts/perc-devctl.py qa-up`
  (branch tip = `origin/main`; no product code changed by this QA ticket, so no
  hot-deploy — cell runs the merged #6316 shells).
- `qa-health`: `RESULT:OK … HEALTH:healthy` on `/Rhythmyx/rest/mimetypes`.
- `TEST_CMS_URL=http://127.0.0.1:9993` (freeport contract).

## Test-plan mapping (issue #6332)

| # | QA step | Automated equivalent | Result |
|---|---------|----------------------|--------|
| 1 | Set an FTPS port — save succeeds, value persists on reload | `sets an FTPS port only and does not write an FTP port`: creates FTP + FTPS servers, saves port `2121`, asserts current shows `2121`, host/user/folder unchanged, PUT body port-only | PASS |
| 2 | Blank, non-numeric, conflicting inputs — validation messages, no save | blank → `FTPS port is required`; `21a` → `only digits`; HTTP 400/403/409 keep previous port (`HTTP 400, 403, and 409 keep the previous FTPS port`) | PASS (2/2 in file) |
| 3 | Cancel out of the panel — no change persisted | cancel after edit asserts current still `990`, zero PUTs | PASS |

Console: asserted zero unexpected JS console/page errors in-spec
(`trackJsErrors`; 400/403/409 + favicon/404 filters only).

## server.log note

Zero `ERROR`/`FATAL`-level lines in `/opt/Percussion/jetty/base/logs/server.log`
for the whole cell lifetime (only case-insensitive `error` hits are INFO lines
about the error manager / `DefaultError.xsl` i18n processing). No DTS cell, so
no #6294-style environmental burst this run.

## Baseline

`tests/golden-unattended-smoke.spec.js` → **2 passed** (env resolve + Admin
login + Explorer shell) on the same cell before teardown.

## Out of scope (per issue)

- Actual FTPS delivery/publishing to a live server.
- Non-FTPS delivery-server fields.
