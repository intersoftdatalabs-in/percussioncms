# QA record — Explorer tree expand-all / collapse-all (issue #5799)

Date (UTC): 2026-11-14
Executed by: night-issue-prs work agent (OpenCode, muse-spark-1.3-contributor)
Parent: #5760 (program #4530, slice 137 — Explorer tree expand-all / collapse-all)
Merged PR under test: #5781 (`fix(webui): Explorer tree expand-all / collapse-all (#5760)`)
Playwright spec:
`modules/perc-qa-automation/frontend/tests/explorer-tree-expand-collapse.spec.js`
Result: **PASS — agent first-pass green (2 passed, 0 skipped)**

## Environment

- Fresh CMS-only H2 QA cell from `python docker/scripts/perc-devctl.py qa-up`
  (branch tip = `origin/main`; no product code changed by this QA ticket, so no
  hot-deploy — cell runs the merged product route).
- `qa-health`: `RESULT:OK … HEALTH:healthy` on `/Rhythmyx/rest/mimetypes`.
- `TEST_CMS_URL=http://127.0.0.1:57513` (freeport contract).

## Test-plan mapping (issue #5799)

| # | QA step | Automated equivalent | Result |
|---|---------|----------------------|--------|
| 1 | Smoke Content Explorer tree toolbar: open Explorer, confirm **Expand all** and **Collapse all** named buttons are present and keyboard-focusable | `toolbar buttons exist, are named, and expand-all reveals loaded descendants`: accessible names `/expand all/i`, `/collapse all/i` asserted; buttons are native `<button type="button">` (tab-focusable by HTML construction, `ExplorerTree.tsx`); axe a11y gate green | PASS |
| 2 | Expand all: with nested folders loaded, click Expand all → loaded descendants reveal; unopened subtrees lazy-load one level on demand (no whole-hierarchy prefetch storm) | Sites → Foo → Bar loaded; collapse-all then expand-all reveals Bar with no refetch of the loaded parent (`fooLoads() === 1` — no prefetch storm). One-level-on-demand shape is inherent to the route design (each folder fetch returns only its direct children; `TREE_ROUTES` mock mirrors the production one-level folder API) — no separate unopened-subtree fetch-count test in this slice | PASS |
| 3 | Collapse all: click Collapse all → tree folds back to roots; selection survives a full collapse/expand cycle | `expand-all and collapse-all preserve the selected folder`: Bar selected → expand keeps selection → collapse hides Bar → expand restores Bar selected | PASS |
| 4 | Accessibility: toolbar buttons expose accessible names; no console/page errors during the flows | `expectNoSeriousA11yViolations` on the explorer shell; `pageErrors` + `consoleErrors` asserted empty in both tests | PASS |

## Spec output (same cell)

- `npm run test:surface -- --path tests/explorer-tree-expand-collapse.spec.js` →
  **2 passed (5.2s)**, 0 skipped, exit 0; zero `pageerror` / console `error`
  events in-spec.
- Baseline `tests/golden-unattended-smoke.spec.js` → **2 passed (1.9s)**
  (env resolve + Admin login + Explorer shell) on the same cell.

## server.log note

All `ERROR`-level lines in the container log pre-date server startup
(installer schema-bootstrap on the empty H2 database, 20:04:03–20:04:07 UTC,
before Jetty `Started … @31261ms` at 20:04:55). Zero `ERROR` / zero `FATAL`
lines during the test window (remaining `error` hits are unzip filename
noise for `error.html` / `DefaultError.xsl` / `globalErrorMessages.js`).

## Live proof

`docs/ai-generated/proofs/issue-5799-explorer-expand-collapse.png` — Content
Explorer shell with the **Expand all** / **Collapse all** toolbar buttons and
the expanded mock tree (Sites › Foo › Bar, Leaf, Assets).

## Pass / fail verdict (per issue criteria)

- **PASS**: expand reveals loaded descendants, collapse folds to roots, folder
  + selection preserved, no JS console errors.

## Out of scope (per issue)

- Slice 136 detail-list sorting (#5759/#5762).
- Anything outside the Explorer folder-tree toolbar.
- Full perc-qa-automation suite; multi-RDBMS matrix.
- Product code changes (none made; none needed — file an implement residual if
  a future run fails instead of fixing here).

## Note on sign-off

This is an **agent-executed first pass**, not a human-QA pass. This PR
supplies the evidence; it does not close #5799 by itself — human sign-off
still required per the issue's QA contract.
