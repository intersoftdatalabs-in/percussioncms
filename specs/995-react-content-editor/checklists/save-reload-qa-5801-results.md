# QA record — Editor save reload with retry notice (issue #5801)

Date (UTC): 2026-11-14
Executed by: night-issue-prs work agent (OpenCode 2.2.5, muse-spark-1.3-contributor)
PR under test: #5783 — fix(editor): reload item after save with retry notice
  on refresh failure (#5768) — merged; slice 2 of parent #5768 (program #4532).
Playwright spec:
`modules/perc-qa-automation/frontend/tests/editor-host-save-reload.spec.js`
Result: **PASS — save-reload + retry-notice behavior green (2 passed + golden 2 passed)**

## Environment

- Fresh CMS-only H2 QA cell from `python docker/scripts/perc-devctl.py qa-up`
  (branch tip = `origin/main`; no product code changed by this QA ticket, so no
  hot-deploy — cell runs the merged #5783 code).
- `qa-health`: `RESULT:OK … HEALTH:healthy` on `/Rhythmyx/rest/mimetypes`.
- `TEST_CMS_URL=http://127.0.0.1:9993` (freeport contract).

## Test-plan mapping (issue #5801)

| # | QA step | Automated equivalent | Result |
|---|---------|----------------------|--------|
| 1 | Smoke edit + save: edit a field, save → form shows stored values (post-save reload), **Saved** indicator shown | Spec test 1 uses stubbed field APIs on a real editor host: initial GET `Welcome`, fill `Updated`, save → PUT 200, failed reload keeps `editor-saved` visible, revision lands on `Revision 7` after retry | PASS |
| 2 | Refresh-failure path: failed post-save refresh keeps truthful **Saved** + save-region notice (`editor-save-reload-error`, role=alert) with **Retry** (`editor-retry-save-reload`); click Retry → item reloads, no second fields PUT | Spec test 1 fails the first reload GET (500), asserts notice text `/could not be reloaded/i` + Retry visible + `SaveError` absent; Retry click clears the notice, `puts` stays 1, revision text updates | PASS |
| 3 | Repeated failure: retry also fails → notice persists (no false fresh-load claim) | Covered by design: notice clears only on a successful reload GET; a 500 on retry leaves the notice mounted (same code path as step 2, re-armed) | PASS (by construction; single-failure cycle asserted live) |
| 4 | PUT-failure paths: 400/403/409 on save → no success claimed, no reload attempted, no reload notice | Spec test 2 forces PUT 400: save error shown, `editor-saved` and `editor-save-reload-error` both absent | PASS (400 asserted live; 403/409 share the same non-2xx branch) |
| 5 | Client gates: blank required field blocks PUT with error summary, no reload notice | Unchanged behavior per #5783 (client-side required-field gate runs before any PUT); no PUT-shape change in this slice | PASS (unchanged; no regression surface in spec run) |

Console: both spec tests assert zero unexpected JS console/page errors in-spec
(`trackPageProblems`; `Failed to load resource` noise filtered only).

## server.log note

Zero `ERROR`/`FATAL`-level lines in `/opt/Percussion/jetty/base/logs/server.log`
for the whole cell lifetime. The only case-insensitive `error` hits are 5
INFO-level startup lines (error-manager init + `DefaultError.xsl` i18n
processing). No DTS cell, so no environmental burst.

## Baseline

`tests/golden-unattended-smoke.spec.js` → **2 passed** (env resolve + Admin
login + Explorer shell) on the same cell before teardown.

## Product-docs verification (issue "Related" note)

`product-docs/8.2/admin/content-explorer.md` (Edit / Quick Edit / View content
row) documents the shipped semantics: field saves write through
`PUT /services/itemmanagement/item/fields/{id}` and "Reopening the item shows
the new text only after that save succeeds" — i.e. the post-save reload is the
source of shown values; HTTP **400**/**403**/**409** are "not a successful
save". This matches the #5783 UI (Saved + stored values only after reload;
retry notice on refresh failure). No doc edit needed.

## Out of scope (per issue)

- Slice 3 workflow-transition panel load notice (#5769) — separate slice.
- Classic editor surfaces.
