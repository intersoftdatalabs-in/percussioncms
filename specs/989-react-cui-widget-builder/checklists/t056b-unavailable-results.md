# T056b record — unavailable-message UAT, moved/unavailable URLs (issue #6084)

Date (UTC): 2026-11-14
Executed by: night-issue-prs work agent (OpenCode 2.3.0, muse-spark-1.3-contributor)
Spec: `specs/989-react-cui-widget-builder` (code merged via PR #1337;
FR-013 moved/unavailable message, SC-007 unmapped paths)
Playwright spec:
`modules/perc-qa-automation/frontend/tests/home-unavailable-message-t056b.spec.js`
(new, this PR)
Result: **PASS — 6/6 T056b samples + 2/2 golden smoke, zero console/page errors**

## Environment

- Fresh CMS-only H2 QA cell from `python docker/scripts/perc-devctl.py qa-up`
  (branch tip = `origin/main`; no product code changed by this PR, so no
  hot-deploy — cell runs the merged #1337 shells + dispatcher).
- `qa-health`: `RESULT:OK … HEALTH:healthy` on `/Rhythmyx/rest/mimetypes`.
- `TEST_CMS_URL=http://127.0.0.1:9993` (freeport contract).

## Steps and results

| # | T056b sample | Evidence | Result |
|---|--------------|----------|--------|
| 1 | Unmapped `?view=retiredview123` → on-page Unavailable message | Dispatcher 302 → `spa.jsp?entry=unavailable` → SPA client-routes to `/cm/app/unavailable`; `route-unavailable` visible, title `Unavailable`, body `SPA route is active…`; no classic chrome (`#perc_site_map` / `.perc-mcol` absent) | PASS |
| 2 | Unmapped WB-flavored `?view=retiredwbview456` → same message | Same dispatch chain and assertions as sample 1 | PASS |
| 3 | Direct `spa.jsp?entry=unavailable` (no dispatcher hop) → same message | Same on-page assertions; live-proof screenshot captured at this assertion point (`docs/ai-generated/proofs/issue-6084-unavailable.png`) | PASS |
| 4 | Deleted `home.jsp` (hard-cut, no stub) → branded 404, never blank | HTTP 404; heading `This page cannot be found.` (nbsp-normalized); Main Page / Percussion Help / Support exits present; non-blank body | PASS |
| 5 | Deleted `widgetBuilder.jsp` → same branded 404 | Same assertions as sample 4 | PASS |
| 6 | Regression guard `?view=home` → Home shell still mounts | `home-shell` + `perc-spa-topnav` visible — unmapped-view mapping did not swallow known views | PASS |

Companion run (same cell, same session):

- `tests/golden-unattended-smoke.spec.js` — 2 passed (QA env resolve + Admin login + Content Explorer shell).

Console: zero unexpected JS console/page errors on samples 1–3 and 6
(same favicon/404/net::ERR / React-DevTools filter as the T056 seat).
Samples 4–5 additionally filter the error page's own missing stylesheets
(`Refused to apply style … bootstrap 4.0.0 … MIME type text/html`) — the
CMS-only cell install does not ship `rx_resources/css/bootstrap/4.0.0`
(repo distribution tree ships 3.3.4), so the branded 404 renders unstyled
in the cell; page content and exits are unaffected. Filed as observation in
PR notes; out of scope for the T056b slice.

URL-contract note: the final URL after the dispatcher hop is the SPA client
path `/cm/app/unavailable`, not the transient `spa.jsp?entry=unavailable`
query (the SPA history-replaces on mount; polling the query form alone is
racy). The spec accepts either form.

## server.log note

Zero ERROR/FATAL across the whole cell run (`grep -c` on
`jetty/base/logs/server.log` → 0). This spec never opens the gadgets
section, so the environmental DTS-less burst seen in the T056/T056a records
(#6294) does not occur here.

## T056b checkbox

- [x] Unmapped moved/unavailable URLs show the on-page message; deleted entry
  JSPs serve the branded 404; `?view=home` regression guard green (this note +
  PR evidence) — FR-013 / SC-007 unmapped-path gate satisfied for the 989 shells.
