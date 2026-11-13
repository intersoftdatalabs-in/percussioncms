# T056a record — main-nav smoke SC-005 / FR-020 (issue #6083)

Date (UTC): 2026-11-13
Executed by: night-issue-prs work agent (OpenCode 2.3.0, muse-spark-1.3-contributor)
Spec: `specs/989-react-cui-widget-builder` (code merged via PR #1337)
Playwright spec: `modules/perc-qa-automation/frontend/tests/main-nav-smoke-sc-005.spec.js`
Result: **PASS — 4/4 SC-005 steps, 100%**

## Environment

- Fresh CMS-only H2 QA cell from `python docker/scripts/perc-devctl.py qa-up`
  (installer chain rebuilt from this branch: sitemanage `clean install`,
  WebUI `package`, `perc-distribution-tree` `clean package`).
- `qa-health`: `RESULT:OK … HEALTH:healthy` on `/Rhythmyx/rest/mimetypes`.
- `TEST_CMS_URL=http://127.0.0.1:35985` (freeport; `QA_CMS_HOST_PORT=35985`).

## Steps and results

| # | SC-005 step | Evidence | Result |
|---|-------------|----------|--------|
| 1 | Dashboard opens from main nav | Home shell → `home-nav-gadgets` tab → `home-gadgets-section` visible (Dashboard composes as the Home `gadgets` section; not a top-nav item) | PASS |
| 2 | At least one other non-Home tab opens | `nav-explorer` → `content-explorer-shell` visible, URL `/explorer` | PASS |
| 3 | Home still opens | `nav-home` → `home-shell` + `home-title` visible, URL `/home` | PASS |
| 4 | Widget Builder opens only when enabled | Cell had WB active: `developer-widget-builder-link` present → `widget-builder-app` visible. Disabled path also covered by the spec (direct route must redirect to `/home` with no `widget-builder-app`) | PASS (enabled branch) |

Companion runs (same cell, same session):

- `tests/top-nav-restructure.spec.js` — 2 passed (Home→Explorer adjacency, Admin shell, Developer sub-entries incl. optional Widget Builder).
- `tests/golden-unattended-smoke.spec.js` — 2 passed (Admin login + Content Explorer shell).

Console: zero unexpected JS console/page errors in all three runs
(same favicon/404/net::ERR/React-DevTools filter as `top-nav-restructure.spec.js`).

## server.log note

6 ERROR lines in one burst (2026-11-13 18:37:32 UTC) while the gadgets
section loaded on the fresh cell: `PSPubServerService currentDefaultServer`
null (forms/activity REST), `PSDeliveryClient` fatal transport, and
`PSActivityService` "Cannot find a site for path:
`/Sites/Corporate_Investments`". Assessment: CMS-only cell has no DTS
publish server and no published demo site — environmental, UI unaffected
(all assertions passed). Filed as follow-up #6294 (backend hardening /
empty-data handling); out of scope for the nav-smoke slice.

## T056a checkbox

- [x] Main-nav smoke checklist executed and recorded (this note + PR evidence) — SC-005 gate satisfied for the 989 shells.
