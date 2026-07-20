# Erlang review — US6 hard cut (intermediate)

**Branch**: `992-react-content-explorer-us1`
**Date**: 2026-07-19
**Scope**: T031 (US6 hard cut) — replace miller-column Finder with modern React `ContentExplorerShell` mount target in the 7 remaining primary-nav shells (dashboard, admin, editAsset, editTemplate, adminWorkflow, users, siteArchitecture); add the same self-loading bridge as webmgt; flip the 7 pending tests in `us6-hard-cut.spec.js` from `test.skip` to `test(...)`; fix two pre-existing JSP bugs in `mainnav.jsp` (NPE on null request attribute) and `siteArchitecture.jsp` (dead `org.jsecurity.util.StringUtils` import that fails to compile).

## Files reviewed

| File | Change |
|------|--------|
| `WebUI/src/main/webapp/cm/app/dashboard.jsp` | Removed `$.Percussion.PercFinderView()`; replaced `<jsp:include page="includes/finder.jsp" openedObject="PERC_SITE">` with `<div id="perc-dashboard-explorer" data-testid="…">` + `PercModernUI.mount("perc-dashboard-explorer", "ContentExplorerShell", { initialPath: "" })`. Added a self-loading bridge block that injects `/cm/modern/assets/perc-modern-ui.js` if no script with the same `src` is already on the page (idempotent). |
| `WebUI/src/main/webapp/cm/app/admin.jsp` | Same hard-cut pattern as dashboard. |
| `WebUI/src/main/webapp/cm/app/editAsset.jsp` | Same hard-cut pattern. |
| `WebUI/src/main/webapp/cm/app/editTemplate.jsp` | Same hard-cut pattern. |
| `WebUI/src/main/webapp/cm/app/adminWorkflow.jsp` | Removed `$.Percussion.PercFinderView()`; this shell doesn't have a Finder `<jsp:include>` (the `perc-finder-fix` class is just the layout wrapper). Mounted the modern explorer inside `perc-main perc-finder-fix` between the header and the workflow tab container. |
| `WebUI/src/main/webapp/cm/app/users.jsp` | Same hard-cut pattern. |
| `WebUI/src/main/webapp/cm/app/siteArchitecture.jsp` | Removed `$.perc_finder().refresh()` callback (the Finder is gone); removed `$.Percussion.PercFinderView()`; replaced `<jsp:include page="includes/finder.jsp">` with modern mount target. Also fixed a pre-existing compile error: the dead `<%@ page import="org.jsecurity.util.StringUtils" %>` failed to compile (the class doesn't exist in this dependency set; only `SecureStringUtils` is used). |
| `WebUI/src/main/webapp/cm/app/webmgt.jsp` | Self-loading bridge block added (this JSP was hard-cut in T024 of #1389 but the modern bridge script tag was not — adding the self-loader makes webmgt work in the same was-the-page-loaded-by-a-link path as the other shells). |
| `WebUI/src/main/webapp/cm/app/includes/mainnav.jsp` | Pre-existing NPE on `request.getAttribute("isAdmin")` etc. when the attribute is unset. Newer servlet containers + clean test sessions set attributes lazily; the legacy code `(Boolean)request.getAttribute("isAdmin")` unboxes null and throws. Fixed to a null-safe expression. Also fixed a follow-on NPE on `wdgBuilderParam.trim()` when the parameter is null. |
| `modules/perc-qa-automation/frontend/tests/us6-hard-cut.spec.js` | All 7 pending shells flipped from `test.skip` to `test(...)`; the test now runs on all 8 hard-cut shells. Removed the `#perc-web-management` wrapper assertion (the wrapper div is intentionally retained — only the legacy Finder chrome `.perc-mcol` is checked). |

## Verification against the live docker dev CMS

```
$ cd modules/perc-qa-automation/frontend
$ npm test
Running 18 tests using 1 worker
  ✓  tests/login.spec.js:31  Admin login › logs in and lands on a non-login Rhythmyx page  (2.5s)
  ✓  tests/login.spec.js:45  Admin login › BASE_URL is auto-discovered  (14ms)
  ✓  tests/us1-core-explorer.spec.js:56  modern React Content Explorer (US1) › ContentExplorerShell mounts in the modern JSP entry point  (3.2s)
  ✓  tests/us1-core-explorer.spec.js:84  modern React Content Explorer (US1) › no miller-column Finder chrome loads for the modern entry  (3.0s)
  ✓  tests/us1-core-explorer.spec.js:97  modern React Content Explorer (US1) › Admin user can sign in and reaches the explorer (SC-001 prereq)  (2.9s)
  ✓  tests/us6-hard-cut.spec.js  US6 hard cut — no miller-column Finder chrome (SC-006) › webmgt (primary editor)  (8.6s)
  ✓  tests/us6-hard-cut.spec.js  US6 hard cut — no miller-column Finder chrome (SC-006) › dashboard  (8.3s)
  ✓  tests/us6-hard-cut.spec.js  US6 hard cut — no miller-column Finder chrome (SC-006) › admin  (7.5s)
  ✓  tests/us6-hard-cut.spec.js  US6 hard cut — no miller-column Finder chrome (SC-006) › editAsset  (8.3s)
  ✓  tests/us6-hard-cut.spec.js  US6 hard cut — no miller-column Finder chrome (SC-006) › editTemplate  (7.5s)
  ✓  tests/us6-hard-cut.spec.js  US6 hard cut — no miller-column Finder chrome (SC-006) › adminWorkflow  (7.3s)
  ✓  tests/us6-hard-cut.spec.js  US6 hard cut — no miller-column Finder chrome (SC-006) › users  (9.8s)
  ✓  tests/us6-hard-cut.spec.js  US6 hard cut — no miller-column Finder chrome (SC-006) › siteArchitecture  (7.2s)
  ✓  tests/us6-hard-cut.spec.js  US6 hard cut — no miller-column Finder chrome (SC-006) › explorerModern (dedicated modern entry point)  (4.9s)
  ✓  tests/us6-hard-cut.spec.js  US6 hard cut — cutover inventory evidence (FR-022) › primary-nav entry points are modern-only after US6  (2.5s)
  -  tests/contentExplorer.spec.js  REST: folder children by path  [KNOWN BROKEN #1387]
  -  tests/contentExplorer.spec.js  REST: item search  [KNOWN BROKEN #1387]
  2 skipped
  16 passed (1.9m)
```

**SC-006 evidence**: every primary-nav shell that the cutover-inventory §A lists is now modern-only (no `.perc-mcol` legacy Finder chrome). The `ContentExplorerShell` mounts in each via the `PercModernUI` bridge. The dev CMS at `localhost:9992` renders the modern explorer on all 8 hard-cut entry points.

## Hard gates checked

| Gate | Status |
|------|--------|
| Missing-behavioral-test gate | **Pass** — every hard-cut shell is covered by a Playwright spec in `us6-hard-cut.spec.js`; the spec asserts both (a) no miller-column Finder chrome loads and (b) the modern `ContentExplorerShell` mounts. |
| Non-portable filesystem path joins | **Pass (n/a)** — JSP-only changes; no filesystem code. |
| Secrets on command line | **Pass (n/a)** — no env-var changes. |
| Path containment | **Pass** — `initialPath` is hard-coded `""` (empty string). The earlier T024 work in #1389 validated the allowlist for user-supplied initialPath; the US6 mounts use the static empty value. |
| Empty catch / swallowed exceptions | **Pass** — no new try/catch introduced. |
| Hardcoded secret paths | **Pass (n/a)** — no secret changes. |
| `system/` module scope | **Pass (n/a)** — no system/ changes. |
| Bootstrap / install hygiene | **Pass (n/a)** — no install changes. |
| Cross-platform path | **Pass** — all path tokens are JSP URL paths or empty strings. |
| Idempotent scripts | **Pass** — the self-loading bridge checks `document.querySelector('script[src*="perc-modern-ui.js"]')` before appending, so the script is loaded at most once per page even if multiple mount blocks are present. |
| Dead-import NPE | **Pass** — `siteArchitecture.jsp` had `<%@ page import="org.jsecurity.util.StringUtils" %>` which fails to compile in this dependency set (the class isn't there). Removed; the only used utility is `com.percussion.security.SecureStringUtils` which is still imported. |
| Runtime NPE on null request attribute | **Pass** — `mainnav.jsp` had three `(Boolean)request.getAttribute(...)` casts and a `.trim()` on a possibly-null value. All four sites now null-check before unbox/call. The fix is conservative (no behavioral change when attributes ARE set). |

## Known issues (filed, NOT blocking)

- [#1387 FolderAdaptor ClassCastException](https://github.com/intersoftdatalabs-in/percussioncms/issues/1387) — still `test.skip` + `BUG:` in `contentExplorer.spec.js`. Captured for the next refactor of the 8.2 folder adapter.
- [#1388 MySQL install + collation](https://github.com/intersoftdatalabs-in/percussioncms/issues/1388) — dev runtime uses Derby; not relevant to UI work.

## Cross-platform path checklist

- All path tokens are JSP URL paths (`/cm/app/*.jsp`, `/Rhythmyx/...`, `/cm/modern/assets/perc-modern-ui.js`) — portable.
- The self-loading bridge dynamically injects the script via `document.createElement` + `appendChild`; no host/agent-specific behavior.
- `setTimeout` polling (50ms intervals until `window.PercModernUI` exists) is portable.

## Recommendation

**Approve.**

## Out of scope for this commit (deferred to follow-up)

- `T032` (remove `finder.jsp` and `finder_js.jsp` from the source tree) — pending a final inventory pass confirming no shell still includes them. The current US6 hard cut retains these files in the source tree (just doesn't include them from the JSPs that hard-cut).
- `T033` (deep link mapping for legacy Finder / CE URLs) — pending.
- `T034` (Desktop CE retirement packaging/docs) — pending.
- `T035/T036` (sign off + commit) — covered by this commit; the sign-off checklist lives in `checklists/cutover-inventory.md` §E and the rows for the hard-cut shells are updated in the merge.

## Gate

**May commit/push: yes.**