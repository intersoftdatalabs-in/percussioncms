# QA results — Explorer recent-items quick access (#6331, implements #6306 test plan)

Implementation merged via #6306 (`fix(WebUI): Explorer recent-items quick access (#6256)`);
this record executes the #6331 test plan against a fresh H2 QA cell. No product
code changed in this PR.

## Environment

- Cell: `python docker/scripts/perc-devctl.py qa-up` → `RESULT:OK`
  (`TEST_CMS_URL=http://127.0.0.1:9993`, container `perc-matrix-cms-h2`)
- `qa-deploy-webui` → `RESULT:OK` (cell serves branch WebUI with the Recent panel)
- `qa-health` → `RESULT:OK … HEALTH:healthy` before and after the runs
- Specs: `npm run test:surface -- --path tests/explorer-recent-items.spec.js`
  → **2 passed**; `tests/golden-unattended-smoke.spec.js` → **2 passed**
- Manual-plan driver (temporary spec, removed before commit): per-row remove +
  clear-all + persistence → **1 passed**
- `qa-down` → `RESULT:OK` (cell removed)

## Test-plan mapping

| # | QA step (#6331 test plan) | Result |
|---|---------------------------|--------|
| 1 | Browse several folders/items in Explorer — Recent panel records them in order | PASS — spec test 1 browses `/Sites` → `Corporate_Investments` (dblclick), asserts the Recent row appears with `data-recent-kind="folder"`; order covered by `recentItems.test.ts` unit suite in #6306 |
| 2 | Click a Recent row — navigates to the item; per-row remove and clear-all — panel updates and persists (sanitized localStorage) | PASS — spec test 1 re-browses from the Recent row and asserts `data-selected-folder-path`; manual driver removed one of two rows (2→1), clear-all emptied the panel, reload persisted the empty panel with storage `perc.explorer.recent.Admin=[]`; zero JS console/page errors asserted in both |
| 3 | No console errors during the session; panel never triggers 500-log spam on missing items | PASS — both spec tests assert `jsErrors == []` (unexpected console/page errors fail the run); spec test 2 seeds a deleted entry and asserts a "no longer available" status notice with zero `role="alert"` errors; server.log shows zero `ERROR`/`FATAL` lines for the whole cell lifetime (case-insensitive hits are INFO lines about the error manager / `DefaultError.xsl` i18n processing) |

Out of scope (per issue): non-Recent Explorer surfaces (covered by sibling slices).

## Proofs

- `docs/ai-generated/proofs/issue-6256-explorer-recent-items.png` — Recent panel
  live proof from the #6306 cycle (already committed)
- `docs/ai-generated/proofs/issue-6331-recent-remove-clear.png` — empty Recent
  panel after per-row remove + clear-all + reload (this run)
