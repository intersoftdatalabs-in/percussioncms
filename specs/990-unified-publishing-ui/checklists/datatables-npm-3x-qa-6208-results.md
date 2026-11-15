# QA record — DataTables npm 3.x migration parity (issue #6208)

Date (UTC): 2026-11-14
Executed by: night-issue-prs work agent (OpenCode, muse-spark-1.3-contributor)
Parent: #6002 (program #5663 jQuery plugin cluster — DataTables core migration slice)
Merged PR under test: #6192
  (`fix(WebUI): migrate DataTables 1.12.1 core to npm 3.x + rewrite legacy fn* call sites`)
Playwright specs:
`modules/perc-qa-automation/frontend/tests/classic-datatables-core.spec.js`
`modules/perc-qa-automation/frontend/tests/classic-datatables-prune.spec.js`
Result: **PASS — agent first-pass green (4 passed, 0 skipped)**

## Environment

- Fresh CMS H2 QA cell from `python docker/scripts/perc-devctl.py qa-up`
  (branch tip = `origin/main` @ `5e16b06b79`; no product code changed by this
  QA ticket, so no hot-deploy — cell runs the merged product route).
- `qa-health`: `RESULT:OK … HTTP:200 HEALTH:healthy` on
  `/Rhythmyx/rest/mimetypes` (container `perc-matrix-cms-h2`).
- `TEST_CMS_URL=http://127.0.0.1:56207` (freeport contract).

## Test-plan mapping (issue #6208)

| # | QA step | Automated equivalent | Result |
|---|---------|----------------------|--------|
| 1 | Dashboard/reporting grids (DataTables surfaces): sort a custom-order column, page, and check footer/render output matches legacy | `classic-datatables-core` test 2 — live grid mirroring the classic shapes (progress-bar `render` column, custom order plug-in column, initial `order`, `footerCallback`): asserts sort order (`alpha` first), footer text (`3 rows`), render markup (`e2e-bar`) | PASS (2.1s-class run; `alpha` first, `3 rows`, `e2e-bar` all asserted green) |
| 2 | Inline-edit datatable: add/edit/delete a row; confirm draw/CRUD/destroy parity with no JS errors | `classic-datatables-core` test 2 — `row.add` + `draw(false)` → `TR` node; `cell().data()` edit; `row().remove().draw()` → 3 remaining; `destroy()` → `isDataTable` false; `legacyGone:true` (no `fnGetData`); zero datatables-attributed `pageerror` | PASS (`addedNode:TR`, `editedFirst:alpha2`, `remaining:3`, `destroyed:true`, `pageErrors:[]`) |
| 3a | Confirm served bytes are DT 3.1.5 (`DataTables 3.1` banner) at the stable jslib URLs | `classic-datatables-core` test 1 — both stable URLs return 200; in-page probe `$.fn.DataTable.version` matches `^3\.`; `dataTableExt.afnSortData` alias intact; zero datatables-attributed plugin errors. Banner assertion via `curl` (recorded below) | PASS — `curl` served JS contains `DataTables 3.1.5`, `grep -c fnGetData` → 0; stable JS + CSS both HTTP 200 |
| 3b | Pruned styling URLs 404 as intended | `classic-datatables-prune` test 2 — 6 kept assets return 200 (core JS+CSS, layout ×2, dropdown JS+CSS); all 21 pruned styling/image URLs return 404. Test 1: dashboard loads with `$.fn.DataTable` + `jqDropdown` present and zero failed jslib requests / plugin JS errors | PASS (spot `curl`: pruned `dataTables.bootstrap5.css` → 404, `images/sort_both.png` → 404) |

Pass / fail criteria (issue #6208): sort/footer/render/CRUD/destroy parity
with zero datatables-attributed console errors. Pre-existing dashboard
`set_label` / `I18N.meesage` noise is out of scope (excluded by design in
both specs — they gate on datatables-attributed errors only).

## Spec output (same cell)

- `npm run test:surface -- --path tests/classic-datatables-core.spec.js` →
  **2 passed (3.6s)**, exit 0; zero datatables-attributed `pageerror` /
  console `error` / `requestfailed` events in-spec.
- `npm run test:surface -- --path tests/classic-datatables-prune.spec.js` →
  **2 passed (2.1s)**, exit 0; zero failed jslib requests, zero plugin JS
  errors in-spec.
- `curl` banner probes (same cell): stable core JS → HTTP 200 with
  `DataTables 3.1.5` marker and 0 `fnGetData` occurrences; stable core CSS
  → HTTP 200; pruned `dataTables.bootstrap5.css` → 404;
  pruned `images/sort_both.png` → 404.

## server.log note

`docker exec perc-matrix-cms-h2 grep -c -E "ERROR|FATAL"
…/logs/server.log` → **0**. No datatables-attributed lines present.
