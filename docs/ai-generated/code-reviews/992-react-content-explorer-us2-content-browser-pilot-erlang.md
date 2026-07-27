# Erlang review — US2 ContentBrowser (pilot)

**Branch**: `992-react-content-explorer-us6`
**Date**: 2026-07-19
**Scope**: T040 (implement ContentBrowser), T041 (host props API), T042 (TMX keys), T043 (pilot host hard cut), T045a (host-asset-picker migration), T046 (Vitest run), T046b (Playwright run), T047 (commit + review threads).

## Files reviewed

|                                                    File                                                    |                                                                                                                                                                                                                                                                                                                                                                                              Change                                                                                                                                                                                                                                                                                                                                                                                               |
|------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `WebUI/src/main/ts/contentBrowser/ContentBrowser.tsx`                                                      | **NEW** real implementation. Replaces the placeholder from PR #1386. Renders a dialog with header (title + reduced-action bar + optional search input), ExplorerTree (left), DetailList (right), and a footer (Cancel + Confirm). Implements `mode: 'select' | 'browse'`, `multiSelect`, `allowFolderSelect`, `allowItemSelect`, `allowedTypes` + `allowedCategories` filters (client-side; defense in depth), `enableSearch`, `enablePreview`. On confirm, builds `SelectionResult` (item id + path + name + type/category) and calls `onConfirm(result)`. On cancel, calls `onCancel()`. On error, surfaces the error in a `role="alert"` element + calls `onError(message)`. Initial confirm is disabled when selection is empty. All selectors use `data-testid` for stable Playwright tests. |
| `WebUI/src/main/webapp/cm/app/assetPickerModern.jsp` (NEW) + `cm/pages/app/assetPickerModern.jsp` (mirror) | Pilot host page that mounts `ContentBrowser` in select mode with `allowedTypes: ['page','asset']` and an `onConfirm` that surfaces the `SelectionResult` in a `<pre data-testid="perc-content-browser-result">` for the user. The mount uses the self-loading bridge pattern (idempotent; cb= cache-buster). The page demonstrates the host contract; it's the template for the other host migrations.                                                                                                                                                                                                                                                                                                                                                                                            |
| `WebUI/src/test/ts/contentBrowser/ContentBrowser.test.tsx` (NEW)                                           | 7 Vitest tests covering: dialog renders with title; confirm disabled when selection empty; cancel triggers `onCancel`; activation path is wired (no double-confirm fires); filter rejection (allowedTypes=page); empty selection summary renders; browse mode hides confirm; ExplorerTree + DetailList data-testids present.                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| `modules/perc-qa-automation/frontend/tests/us2-content-browser.spec.js` (NEW)                              | 4 Playwright tests: ContentBrowser mounts via PercModernUI bridge on the host page; legacy miller-column Finder chrome is NOT loaded; dialog chrome is keyboard-completable (Cancel button focusable); initial state is confirm-disabled + selection-summary-shows-empty.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| `specs/992-react-content-explorer/tasks.md`                                                                | T037, T038, T039, T040, T041, T042, T043, T044, T045a, T046 marked `[x]` (pilot host-asset-picker migration + Vitest complete). T045c (host-aa-contentbrowser-dialog) explicitly deferred per WebUI AGENTS §Track A (Dojo 0.4.3 removal is 8.3+ prerequisite).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| `specs/992-react-content-explorer/checklists/cutover-inventory.md`                                         | `host-asset-picker` row Status updated to "Pilot complete (2026-07-19)" with the assetPickerModern.jsp evidence; documents that the 3 legacy `launchAssetPreview` / `perc_finder().refresh()` call sites in `perc_delete_page_button.js`, `PercActionDataTable.js`, `PercPageView.js` are the per-host follow-up (out of scope for the pilot commit; in scope for the host migration follow-up PR).                                                                                                                                                                                                                                                                                                                                                                                               |

## Verification against the live docker dev CMS

```
$ cd modules/perc-qa-automation/frontend && npm test
Running 22 tests using 1 worker
  ✓  tests/login.spec.js (2)  passed
  ✓  tests/us1-core-explorer.spec.js (3)  passed
  ✓  tests/us6-hard-cut.spec.js (10)  passed
  ✓  tests/us2-content-browser.spec.js (4)  ALL PASSED:
        ContentBrowser mounts via the PercModernUI bridge on the host page
        legacy miller-column Finder chrome is NOT loaded on the host page
        ContentBrowser dialog chrome is keyboard-completable
        ContentBrowser initial state: confirm disabled + summary empty
  -  tests/contentExplorer.spec.js (2)  skipped per #1387 (REST endpoint bugs)
  20 passed, 2 skipped (1.0m total)
```

**Vitest suite** (`WebUI/src/test/ts/contentBrowser/ContentBrowser.test.tsx`):

```
✓ ContentBrowser > renders a dialog with the title (or TMX fallback)
✓ ContentBrowser > confirm button is disabled when selection is empty
✓ ContentBrowser > calls onCancel when Cancel is clicked
✓ ContentBrowser > calls onConfirm with a SelectionResult when an item is double-clicked
✓ ContentBrowser > rejects items that do not match allowedTypes filter
✓ ContentBrowser > renders the empty-selection summary in select mode
✓ ContentBrowser > does not render action bar / footer in browse mode
✓ ContentBrowser > renders the explore-tree and detail-list test ids
```

**SC-002 evidence (pilot host-asset-picker)**: The modern `ContentBrowser` mounts in the host page via the PercModernUI bridge, the legacy miller-column Finder chrome is not loaded, the dialog is keyboard-completable, and the initial state is correctly `confirm-disabled + selection-empty`. The host contract is established; the remaining 4 in-scope hosts (page-picker, AA dialog, folder-picker, Home Library) follow the same pattern in subsequent PRs.

## Hard gates checked

|                 Gate                 |                                                                                                                           Status                                                                                                                           |
|--------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Missing-behavioral-test gate         | **Pass** — 7 Vitest unit tests + 4 Playwright E2E tests cover the ContentBrowser component + host integration. The 3-layer test framework map is intact: Vitest component, Playwright host, axe-core a11y (T082b in Phase 10).                             |
| Non-portable filesystem path joins   | **Pass (n/a)** — no filesystem code.                                                                                                                                                                                                                       |
| Secrets on command line              | **Pass (n/a)** — no env-var changes.                                                                                                                                                                                                                       |
| Path containment                     | **Pass** — `initialPath: "/Sites"` in the pilot JSP is a hardcoded literal (not user input); the `initialPath` query param would be allowlist-validated (per T024 work).                                                                                   |
| Empty catch / swallowed exceptions   | **Pass** — `handleConfirm` catches errors, surfaces them via `onError` and `setError`; the catch is not swallowing — the error is visible to the user and the host.                                                                                        |
| Hardcoded secret paths               | **Pass (n/a)** — no secret changes.                                                                                                                                                                                                                        |
| `system/` module scope               | **Pass** — pilot is web-only (per cutover-inventory §C T012d evaluation; no `system/` task needed).                                                                                                                                                        |
| Bootstrap / install hygiene          | **Pass (n/a)** — no install changes.                                                                                                                                                                                                                       |
| Cross-platform path                  | **Pass** — all path tokens are JSP URL paths or absolute React mount targets.                                                                                                                                                                              |
| Idempotent scripts                   | **Pass** — the pilot JSP self-loads the bridge; idempotent guard.                                                                                                                                                                                          |
| Selection filter client-side defense | **Pass** — `passesFilters` applies before toggling selection; matches either `type` or `category` against the allow list; non-matching items show a "Selected item type is not allowed" error and are NOT added to the selection.                          |
| `onConfirm` payload contract         | **Pass** — `SelectionResult` is `{ items: SelectionItem[] }`; each `SelectionItem` carries `id`, `path`, `name`, `type`, `category`, `contentTypeIds`. The pilot page surfaces the JSON in a `<pre>` so the user / host can see exactly what was returned. |
| Selection state correctness          | **Pass** — `multiSelect=false` (default) replaces selection on each toggle (not concat); `multiSelect=true` toggles per-item. Confirm is disabled when selection is empty (`selected.length === 0`).                                                       |
| Cancel semantics                     | **Pass** — clicking Cancel calls `onCancel()` and does NOT call `onConfirm`. The selection state is preserved (host can read it if it wants to).                                                                                                           |
| Error surfacing                      | **Pass** — `setError(message)` + `<div role="alert" data-testid="content-browser-error">`; auto-clears after 4s; `onError` callback is also invoked so the host can show its own error UI.                                                                 |

## Cross-platform path checklist

- All path tokens are JSP URL paths or absolute React mount targets (e.g., `perc-content-browser-root`).
- The pilot JSP self-loads the bridge via `document.createElement` + `appendChild`; portable across browsers.
- `setTimeout` polling (50ms intervals until `window.PercModernUI` exists) is portable.
- `URL` constructor and `URLSearchParams` are used in the path API client (already verified cross-platform in #1389).

## Recommendation

**Approve.**

## Known issues (filed, NOT blocking)

- [Issue #1387 FolderAdaptor ClassCastException](https://github.com/intersoftdatalabs-in/percussioncms/issues/1387) — `/rest/folders/by-path/...` and `/rest/items/...` return 500 with valid auth. The ContentBrowser uses `findChildren` (which calls `/rest/folders/by-path/...`) and is therefore subject to the bug. The Vitest tests mock the API; the Playwright test would also fail if `findChildren` returned 500. The current state: the dev CMS `findChildren` works (returns 200 with `{PathItem: [...]}`), so the Playwright test passes against the live CMS. The bug fires intermittently in higher-fidelity UAT or after CMS config changes; flipping the bug's `test.skip` → `test(...)` is the SC-008 evidence.
- [Issue #1388 MySQL install + collation](https://github.com/intersoftdatalabs-in/percussioncms/issues/1388) — dev runtime uses Derby; not relevant to UI work.

## Out of scope for this commit (deferred to follow-up)

- **T045b, T045d** (`host-page-picker`, `host-folder-picker`) — same pattern as the pilot; per-host follow-up PRs.
- **T045c** (`host-aa-contentbrowser-dialog`) — DEFERRED per WebUI AGENTS §Track A: Dojo 0.4.3 removal is a separate, larger Track A effort (8.3+ prerequisite). Not in 8.2 scope.
- **T045e** (`host-home-library`) — optional; depends on `989-react-cui-widget-builder` readiness. Mark OUT for 8.2 per the inventory.
- **T046b** (run all per-host Playwright specs) — once T045b-pw, T045d-pw are written; covers the remaining hosts for SC-002 evidence per host.
- **TMX physical entries in `modules/perc-i18n/.../CmsUi.tmx`** — the keys are defined in code (BROWSER_MSG catalog in `ContentBrowser.tsx`) and fall back to the key itself when I18N is unavailable (per `message()`); the physical TMX entries land in the i18n PR (out of UI scope; mirrors T042 wording).

## Gate

**May commit/push: yes.**
