# QA record — SFTP delivery-server passphrase panel (issue #5800)

Date (UTC): 2026-11-14
Executed by: night-issue-prs work agent (OpenCode, muse-spark-1.3-contributor)
Parent: #5764 (program #4531, slice 132 — SFTP delivery-server passphrase in PublishingShell)
Merged PR under test: #5782 (`fix(publishing): set the SFTP delivery-server passphrase in PublishingShell (#5764)`)
Playwright spec:
`modules/perc-qa-automation/frontend/tests/publishing/sitesDeliveryServerSftpPassphrase.spec.js`
Result: **PASS — agent first-pass green (2 passed + golden 2 passed)**

## Environment

- Fresh CMS-only H2 QA cell from `python docker/scripts/perc-devctl.py qa-up`
  (branch tip = `origin/main`; no product code changed by this QA ticket, so no
  hot-deploy — cell runs the merged #5782 shells).
- `qa-health`: `RESULT:OK … HEALTH:healthy` on `/Rhythmyx/rest/mimetypes`.
- `TEST_CMS_URL=http://127.0.0.1:9993` (freeport contract).

## Test-plan mapping (issue #5800)

| # | QA step | Automated equivalent | Result |
|---|---------|----------------------|--------|
| 1 | Smoke Sites → Publishing → SFTP delivery server: secret never rendered (input opens empty, clears after save) | `sets an SFTP passphrase only and never renders the secret`: Local-server overlay opens `sftp-passphrase-not-sftp` for non-SFTP; SFTP overlay shows host/port/user/folder/name/driver with empty input and `never shown` status; workspace never contains private key or passphrase text | PASS |
| 2 | Set passphrase → save → success; input clears; stored value updated (never read back in UI) | valid save issues passphrase-only PUT (base64 body, no plaintext secret, no host/user/folder/privateKey leakage); panel confirms only after 200 and input unmounts | PASS |
| 3 | Blank / cancel paths keep the stored value | cancel after edit → zero PUTs, form unmounts; blank → `SFTP passphrase is required`, over-long → `255 characters or fewer`, zero PUTs | PASS |
| 4 | Error paths: 400/403/409 → mapped messages, stored value kept | `HTTP 400, 403, and 409 keep the previous SFTP passphrase`: each status maps to its message, form stays open, host/port/user/folder unchanged | PASS |
| 5 | Regression: FTP password panel and sibling panels unchanged | PUT body assertions are passphrase-property-only; sibling specs (SFTP folder/host/port/username/private-key) untouched by this run | PASS (scope: unchanged files, no regressions observed) |

Console: asserted zero unexpected JS console/page errors in-spec
(`trackJsErrors`; 400/403/409 + resource-load filters only).

## Spec output (same cell)

- `npm run test:surface -- --path tests/publishing/sitesDeliveryServerSftpPassphrase.spec.js` →
  **2 passed (3.6s)**, exit 0; zero `pageerror` / console `error` events in-spec.
- Baseline `tests/golden-unattended-smoke.spec.js` → **2 passed (2.5s)**
  (env resolve + Admin login + Explorer shell) on the same cell before teardown.

## server.log note

Zero `ERROR` / zero `FATAL` lines in
`/opt/Percussion/jetty/base/logs/server.log` for the whole cell lifetime
(`grep -c` both return 0). No DTS cell this run.

## Live proof

`docs/ai-generated/proofs/issue-5800-sftp-passphrase.png` — PublishingShell
Sites workspace with the delivery-server list (screenshot captured post-run
on the same cell before teardown).

## Pass / fail verdict (per issue criteria)

- **PASS**: passphrase sets and persists via passphrase-only PUT, secret never
  rendered, blank/cancel/error keep stored value, zero JS console errors.

## Out of scope (per issue)

- Sibling slice 131 SFTP folder (#5763/#5766) — already merged, covered separately.
- New REST endpoints (none added — existing PUT reused).
- Full perc-qa-automation suite; multi-RDBMS matrix.
- Product code changes (none made; none needed — file an implement residual if
  a future run fails instead of fixing here).

## Note on sign-off

This is an **agent-executed first pass**, not a human-QA pass. This PR
supplies the evidence; it does not close #5800 by itself — human sign-off
still required per the issue's QA contract.
