# T056 record — Home + Widget Builder E2E smoke SC-001/002/007 (issue #6082)

Date (UTC): 2026-11-14
Executed by: night-issue-prs work agent (OpenCode 2.3.0, muse-spark-1.3-contributor)
Spec: `specs/989-react-cui-widget-builder` (code merged via PR #1337)
Playwright specs:
`modules/perc-qa-automation/frontend/tests/home-deep-links-sc-007.spec.js` (new, this PR),
`tests/home-react-editor.spec.js`, `tests/main-nav-smoke-sc-005.spec.js`
(T056a companion, WB-enabled branch), `tests/golden-unattended-smoke.spec.js`
Result: **PASS — SC-001/002/007 smoke green (12 passed, 1 env-skip)**

## Environment

- Fresh CMS-only H2 QA cell from `python docker/scripts/perc-devctl.py qa-up`
  (branch tip = `origin/main`; no product code changed by this PR, so no
  hot-deploy — cell runs the merged #1337 shells).
- `qa-health`: `RESULT:OK … HEALTH:healthy` on `/Rhythmyx/rest/mimetypes`.
- `TEST_CMS_URL=http://127.0.0.1:9993` (freeport contract).

## Steps and results

| # | SC step | Evidence | Result |
|---|---------|----------|--------|
| 1 | SC-007 known deep links land on correct sections | New `home-deep-links-sc-007.spec.js`: `initialScreen=library→library`, `list→recent`, `search→search`, `newitem→create` (each asserts `home-nav-<section>` `aria-current="page"`); absent and unknown values default to Recent per `deepLinkMap.ts` | PASS (6/6) |
| 2 | SC-002 Widget Builder entry | Same spec: `entry=widget-builder` → `widget-builder-app` mounted (cell had WB active). Disabled-redirect branch also covered by the spec | PASS (enabled branch) |
| 3 | SC-001 Home smoke (Recent/Create open React editor, no leftover `?view=editor`) | `home-react-editor.spec.js`: Create-asset and Create-Page tests pass; recent-item test skips cleanly (fresh H2 cell has no recent items — documented skip in the spec) | PASS (2 passed, 1 env-skip) |
| 4 | SC-001/002 cross-check via main nav | `main-nav-smoke-sc-005.spec.js`: gadgets + Explorer + Home + WB gate, WB-enabled branch | PASS (1/1) |
| 5 | Baseline shell/login | `golden-unattended-smoke.spec.js`: env resolve + Admin login + Explorer shell | PASS (2/2) |

Console: zero unexpected JS console/page errors in all four runs
(same favicon/404/net::ERR/React-DevTools filter as
`top-nav-restructure.spec.js`).

## server.log note

Same environmental ERROR burst as the T056a record (#6083): while the
gadgets section loaded, `PSActivityService` "Cannot find a site for path:
`/Sites/Corporate_Investments`", `PSDeliveryClient` fatal transport, and
forms/activity REST 500s — CMS-only cell has no DTS publish server and no
published demo site. UI unaffected (all assertions passed). Already filed as
follow-up #6294 (backend hardening / empty-data handling); out of scope for
the T056 smoke slice.

## T056 checkbox

- [x] Home + Widget Builder E2E smoke executed and recorded (this note + PR evidence) — SC-001/002/007 gate satisfied for the 989 shells.
- Unmapped/obsolete-path on-page messaging remains T056b (#6084), separate seat — not claimed here.
