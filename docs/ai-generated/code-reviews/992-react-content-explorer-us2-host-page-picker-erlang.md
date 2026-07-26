# Erlang review — US2 host-page-picker migration (T045b + T045b-pw)

**Branch**: `992-react-content-explorer-us2`
**Date**: 2026-07-19
**Scope**: T045b (migrate `host-page-picker` to `ContentBrowser`) + T045b-pw (Playwright spec). Follow-up to the US2 pilot (PR #1391) which migrated the asset picker.

## Files reviewed

|                                                   File                                                   |                                                                                                                                                                                       Change                                                                                                                                                                                       |
|----------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `WebUI/src/main/webapp/cm/app/pagePickerModern.jsp` (NEW) + `cm/pages/app/pagePickerModern.jsp` (mirror) | New modern entry point that mounts `ContentBrowser` in **select mode + multiSelect: true + allowedTypes: ['page']** (page-only filter; folders and assets are rejected client-side). Self-loading bridge pattern (idempotent; cb= cache-buster). The page demonstrates (a) the multi-select path of the ContentBrowser and (b) the page-only filter (folders and assets rejected). |
| `modules/perc-qa-automation/frontend/tests/host-page-picker.spec.js` (NEW)                               | 4 Playwright tests covering the host contract: ContentBrowser mounts via PercModernUI bridge; no legacy miller-column Finder chrome; initial state confirm-disabled + multi-select summary empty; keyboard-completable Cancel button.                                                                                                                                              |
| `specs/992-react-content-explorer/checklists/cutover-inventory.md`                                       | `host-page-picker` row Status updated to "Complete (2026-07-19)"; documents that the 4 legacy call sites in `PercSiteImpactView.js` / `PercActionDataTable.js` / `PercPageView.js` are the per-host follow-up (out of scope for the pilot commit).                                                                                                                                 |
| `specs/992-react-content-explorer/tasks.md`                                                              | T045b + T045b-pw marked `[x]`.                                                                                                                                                                                                                                                                                                                                                     |

## Verification against the live docker dev CMS

```
$ cd modules/perc-qa-automation/frontend && npm test -- tests/host-page-picker.spec.js
Running 4 tests using 1 worker
  ✓  ContentBrowser mounts on the page-picker host page (10.0s)
  ✓  legacy miller-column Finder chrome is NOT loaded on the page-picker host (6.6s)
  ✓  page-picker initial state: confirm disabled, multi-select summary empty (7.9s)
  ✓  page-picker dialog chrome is keyboard-completable (Cancel button focusable) (6.7s)
4 passed (1.0m)
```

**SC-002 evidence** (host-page-picker): the modern `ContentBrowser` mounts in the host page; legacy miller-column Finder chrome is not loaded; the dialog is keyboard-completable; initial state is confirm-disabled + multi-select summary empty.

## Hard gates checked

|                 Gate                 |                                                                                                             Status                                                                                                             |
|--------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Missing-behavioral-test gate         | **Pass** — 4 Playwright tests cover the page-picker host contract (mount, no legacy chrome, initial state, keyboard). Combined with the 4 asset-picker tests + 4 ContentBrowser component tests = 12 tests in the US2 surface. |
| Non-portable filesystem path joins   | **Pass (n/a)** — no filesystem code.                                                                                                                                                                                           |
| Secrets on command line              | **Pass (n/a)** — no env-var changes.                                                                                                                                                                                           |
| Path containment                     | **Pass** — the host page does not accept user-supplied `initialPath` (it's hardcoded to `/Sites`); the `ContentBrowser` component's initialPath allowlist validation (per T024 work) covers the broader contract.              |
| Empty catch / swallowed exceptions   | **Pass (n/a)** — no new try/catch.                                                                                                                                                                                             |
| Hardcoded secret paths               | **Pass (n/a)** — no secret changes.                                                                                                                                                                                            |
| `system/` module scope               | **Pass** — web-only (per cutover-inventory §C T012d evaluation; no `system/` task needed).                                                                                                                                     |
| Bootstrap / install hygiene          | **Pass (n/a)** — no install changes.                                                                                                                                                                                           |
| Cross-platform path                  | **Pass** — all path tokens are JSP URL paths or absolute React mount targets.                                                                                                                                                  |
| Idempotent scripts                   | **Pass** — the host page self-loads the bridge; idempotent guard.                                                                                                                                                              |
| Multi-select dedup                   | **Pass** — verified in the upstream ContentBrowser fix (PR #1391 review thread #2): `handleListActivate` skips if `isSelected(sel.id)`.                                                                                        |
| Empty-selection confirm guard        | **Pass** — verified in the upstream ContentBrowser fix (PR #1391 review thread #1): `handleConfirm` is no-op for empty selections in BOTH modes.                                                                               |
| Selection filter for the page picker | **Pass** — `allowedTypes: ['page']` is client-side; the bridge passes it to `passesFilters(item, allowedTypes, allowedCategories)` which short-circuits on non-matching type and surfaces `BROWSER_MSG.TYPE_MISMATCH`.         |

## Cross-platform path checklist

- All path tokens are JSP URL paths or absolute React mount targets (e.g., `perc-page-picker-root`).
- The host page self-loads the bridge via `document.createElement` + `appendChild`; portable across browsers.
- `setTimeout` polling (50ms intervals until `window.PercModernUI` exists) is portable.

## Recommendation

**Approve.**

## Known issues (filed, NOT blocking)

- [Issue #1387 FolderAdaptor ClassCastException](https://github.com/intersoftdatalabs-in/percussioncms/issues/1387) — `/rest/folders/by-path/...` and `/rest/items/...` return 500 with valid auth. The ContentBrowser's `findChildren` and the page picker's findItemByPath are subject to the bug. The dev CMS `findChildren` works (returns 200 with `{PathItem: [...]}`) so the tests pass; flipping the bug's `test.skip` → `test(...)` is the SC-008 evidence when the upstream fix lands.
- [Issue #1388 MySQL install + collation](https://github.com/intersoftdatalabs-in/percussioncms/issues/1388) — dev runtime uses Derby; not relevant to UI work.

## Out of scope for this commit (deferred to follow-up)

- **Per-host migration** of the 4 legacy call sites in `PercSiteImpactView.js` (lines 58, 173, 187), `PercActionDataTable.js` (line 131), `PercPageView.js` (line 1222) — the host page establishes the contract; the per-host follow-up PRs replace the legacy `$.perc_finder().launchPagePreview(...)` calls with `PercModernUI.mount("...", "ContentBrowser", { onConfirm: ... })` invocations.
- **T045c** (`host-aa-contentbrowser-dialog`) — DEFERRED per WebUI AGENTS §Track A: Dojo 0.4.3 removal is 8.3+ prerequisite.
- **T045d** (`host-folder-picker`, folder-only mode) — same pattern as T045a/T045b; follow-up PR.
- **T045e** (`host-home-library`) — optional; depends on 989 readiness; mark OUT for 8.2 if 989 isn't ready.

## Gate

**May commit/push: yes.**
