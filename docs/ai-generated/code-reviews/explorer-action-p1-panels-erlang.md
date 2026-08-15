# Erlang review: Explorer action P1 panels (feat/explorer-action-p1-panels)

**Branch:** `feat/explorer-action-p1-panels`  
**Base:** `origin/main` / `HEAD` (`3bd616608770` — branch tip matches `FETCH_HEAD` main; increment is uncommitted)  
**Scope mode:** local working tree vs `HEAD`/`origin/main` (P1 increment after PR #3437)  
**Date:** 2026-08-15  
**Persona:** Erlang (independent of implementer; read-only)

## Summary

This increment wires remaining P1 Explorer actions: Revisions/Audit panel on existing itemmanagement revisions + restore, new `POST itemmanagement/item/newCopy|promotableVersion` on `PSItemService`, and `POST /services/assembly/flush-cache|nav-reset` on `IAssemblyAdaptor`. Rest↔sitemanage adaptor closure for the new assembly POSTs is shaped correctly (resource, DTO, Spring stub, Mockito + adaptor tests). First-pass unwrap and `ApiError` bugs are fixed. The Playwright companion file exists but still cannot invoke Revisions (wrong toolbar parent; children are not in the DOM until the Workflow dropdown opens), so the HARD GATE remains.

## Recommendation

request-changes

## Gate

- Blocking bugs: 1 (Issue 3)
- May commit/push: **no**

## Scope

- Base: `origin/main` (`3bd616608770`, same as branch `HEAD`)
- Head: uncommitted / untracked working tree on `feat/explorer-action-p1-panels`
- Files: ~28 inspected (dispatcher, Revisions panel, item copy/revisions APIs, rest assembly POST + DTO, sitemanage adaptor + `PSItemService` copy, tests, product-docs, Playwright, seed)
- Prior report: `docs/ai-generated/code-reviews/explorer-action-execution-erlang.md` (P0; approved after re-review; PR #3437)
- Memory patterns hit: incomplete change-class closure (Playwright for a new WebUI panel); production JSON list unwrap; generic/swallowed API errors; happy-path-only tests

P0 GUID parse, CE URL non-navigation, and real purge still look in place. Not re-opened.

**Not reviewed as CI evidence:** no module `mvnw`/`mvnw.cmd` clean install was run in this review pass. Git porcelain was not available in this subagent; file set inferred from the P1 surfaces and worktree contents vs the P0 report.

## Change-class closure

Change class: **new public REST adaptor methods (assembly POST) + new sitemanage itemmanagement copy endpoints + WebUI Revisions product screen + seed URL clears + product-docs + Playwright**.

| Companion | Status |
|-----------|--------|
| rest resource + `IAssemblyAdaptor` + `AssemblyOperationResult` | present |
| sitemanage `AssemblyAdaptor` + injectable flusher/reset | present |
| Mockito `AssemblyResourceTest` | present (flush + navReset 500/503) |
| Spring `TestAssemblyAdaptor` (`@Component` + `@Lazy`) | present (new methods no-op) |
| sitemanage `AssemblyAdaptorTest` | present (hook invocation) |
| `IPSItemService` + `PSItemService` newCopy/promotable + `PSItemCopyResult` | present |
| `PSItemServiceCopyTest` | present (folder missing / blank / empty result) |
| WebUI APIs + Vitest (dispatch, panel, unwrap) | present; lone-object unwrap + flush 500 shell test |
| Playwright companion | **incomplete** — `explorer-revisions.spec.js` exists but cannot open catalog `Workflow` dropdown; panel/hint assertion is skipped |
| product-docs (`product-docs/8.2/`) | present (nav-reset no-op + global flush honesty) |
| Dual-ship seed | `cmsTableData.xml` URL/HANDLER clears; Rxff `navreset` already empty |

## Cross-platform path checklist

- Assembly and itemmanagement paths use `/` as **URL** separators — allowed.
- No new OS filesystem joins, Unix-only roots, or `File.toString()` assertions.
- Playwright spec path is a test file path, not product I/O.
- **Outcome: clean** for path/I/O portability.

## Issues

### Issue 1 -- Severity: bug
- File: `WebUI/src/main/ts/api/contentExplorer/itemRevisionsApi.ts:119`
- Description: `unwrapRevisionsSummary` keeps revisions/comments only when `Array.isArray`. Production CM1 `PercRevisionDialog` (`WebUI/src/main/webapp/cm/plugins/PercRevisionDialog.js:129`) unwraps the same `RevisionsSummary` payload as `Array.isArray(x) ? x : [x]` because CXF/Jettison (and some Jackson XML-JSON paths) emit a **single object** when the list has one element. New pages almost always have one revision. Those items will render `REVISIONS_EMPTY` / empty audit instead of the row. Tests only pass an array.
- Suggestion: Normalize with the CM1 helper: if the field is a non-null object, wrap `[raw]`; if missing, `[]`. Add a unit test for a single `Revision` / `Comment` object (and a missing field). Do not treat a lone object as empty.
- Status: fixed

### Issue 2 -- Severity: bug
- File: `WebUI/src/main/ts/contentExplorer/ContentExplorerShell.tsx:739`
- Description: `handleMenuInvoke` catches copy / flush / nav-reset failures with `err instanceof Error`. `client.ts` `handleResponse` throws a **plain `ApiError` object** (documented at `client.ts:175`). `formatApiError` is already imported in this file and used for views (`ContentExplorerShell.tsx:799`). New Copy, Promotable Version, Flush Cache, and Nav Reset will show `ERROR_GENERIC` instead of the server business text (e.g. “Item has no folder path and cannot be copied.”). Restore is OK — `RevisionsPanel` already uses `formatApiError`.
- Suggestion: `setError(formatApiError(err, message(EXPLORER_MSG.ERROR_GENERIC)))`. Add a dispatch/shell test that a rejected `createCopy` / `flushCache` surfaces the API message, not the generic string.
- Status: fixed

### Issue 3 -- Severity: bug
- File: `modules/perc-qa-automation/frontend/tests/explorer-revisions.spec.js:91`
- Description: Companion file now exists, but it still does not assert the new Revisions behavior. Catalog `Workflow` (`cmsTableData.xml` NAME=`Workflow`) is a MENU parent. `ActionToolbar` renders that as a dropdown `action-toolbar-item-Workflow`; children (`Workflow_Revisions`) are mounted only while the menu is expanded (`ActionToolbar.tsx:234`). The spec clicks `action-toolbar-group-Workflow` (that testid is never produced — the transition group is `action-toolbar-group-workflow`, lowercase, different menu). `Workflow_Revisions` is therefore not in the DOM, `count() === 0`, and the panel/hint `expect` is skipped. Remaining assertions are shell visible + Data Flow blocklist + a11y — page-load smoke, same hole as the first pass. Silent `.catch(() => {})` on tree/row/group clicks hides fixture failures.
- Suggestion: Open `action-toolbar-item-Workflow` (or right-click → `context-menu-item-Workflow` then `context-menu-item-Workflow_Revisions`). Then unconditionally `expect` `explorer-revisions-panel` or `explorer-revisions-hint` (alert-only is weaker; `ACTION_NEEDS_ITEM` is already the hint path when no item is selected). Do not wrap the Revisions click in `if (visible)`. Soft-skip only when the Workflow parent itself is absent after a selected row (then `test.skip` with a reason — do not pass).
- Status: open
- Pattern-id: change-class.incomplete-closure

### Issue 4 -- Severity: suggestion
- File: `projects/sitemanage/src/main/java/com/percussion/apibridge/AssemblyAdaptor.java:183`
- Description: `reloadNavConfig` calls `PSNavConfig.getInstance()` then `PSNavConfig.reset(null)`. `PSNavConfig.reset` (`system/services/.../PSNavConfig.java:1343`) is a **no-op** when the singleton exists and `m_allVariants == null` — the 6.0+/8.2 FastForward path. The adaptor comment admits this. REST still returns `{ ok: true, message: "Managed navigation reset" }`. Product-docs say Nav Reset “reloads managed navigation configuration.” Classic `navreset.html` had the same backend, so this is parity, not a new no-op — but the operator action is still a false-success toast path if anyone wires `ACTION_NAV_RESET_OK`.
- Suggestion: Document the 8.2 no-op in `product-docs/8.2/admin/content-explorer.md` (and rest.md). If operators need a real nav-tree rebuild, call a live cache/nav API, not `reset` alone. Do not claim a reload that does not happen.
- Status: addressed

### Issue 5 -- Severity: suggestion
- File: `projects/sitemanage/src/main/java/com/percussion/apibridge/AssemblyAdaptor.java:159`
- Description: Classic `sys_uiSupport/flushcache.html` passes `sys_contentid` / `sys_revision` / `sys_variantid` into `PSExitFlushAssemblerCache` (item-scoped when those HTML params are present). The new POST always flushes **all** assembler keys. Catalog `DISPLAYNAME` is still **Refresh Item**. Confirm copy (`CONFIRM_FLUSH_CACHE`) and docs honestly say all items — good — but the menu label still implies the selected item.
- Suggestion: Either flush keys for the selected item (classic Refresh Item) or change the seed display name / docs table to “Flush assembler cache (all items)” so the catalog does not lie. Keep the confirm either way.
- Status: accepted

### Issue 6 -- Severity: suggestion
- File: `WebUI/src/main/ts/contentExplorer/messages.ts:373`
- Description: `ACTION_FLUSH_OK`, `ACTION_NAV_RESET_OK`, `ACTION_NEW_COPY_OK`, and `ACTION_PROMOTABLE_OK` are never returned as `messageKey`. After a successful copy the list refreshes with no success chrome. Operators can miss the new sibling.
- Suggestion: Return those keys from `dispatchAction` on success (shell already displays `result.messageKey`), or drop the dead strings.
- Status: open

### Issue 7 -- Severity: suggestion
- File: `rest/src/test/java/com/percussion/rest/assembly/AssemblyResourceTest.java:102`
- Description: `navReset` has a happy-path test but no 500 / missing-adaptor 503 twins (flush has both). `createRelatedCopy` cancel paths in `actionDispatch.test.ts` are covered for flush but not for New Copy / Promotable. Non-blocking given identical `requireAdaptor` / confirm structure.
- Suggestion: Copy the flush 500/503 tests onto `navReset`. Add confirm-cancel cases for `Workflow_NewVersion` and `Edit_PromotableVersion`.
- Status: fixed

### Issue 8 -- Severity: nit
- File: `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/data/cmsTableData.xml:6107`
- Description: P1 rows (`Workflow_Revisions`, `Workflow_NewVersion`, `Workflow_AuditTrail`, `Flush_Cache`, `Edit_PromotableVersion`, `navreset`) continue the P0 `action="r"` + empty URL + `HANDLER=CLIENT` pattern. Spec still says DCE is not an 8.2 client. Same product call as P0 Issue 5 — not a new block.
- Suggestion: Leave as-is if Patton already accepted DCE retirement; otherwise do not `action="r"` wipe SERVER URLs on upgrade.
- Status: accepted

## Escalations

- **Patton / Minerva:** Issue 5 (Refresh Item vs global flush) and Issue 4 (document 8.2 nav-reset no-op vs implement a real reload). Docs now state both honestly; catalog DISPLAYNAME still “Refresh Item.”
- **Argus:** `POST /services/assembly/flush-cache` is a global cache flush behind the same authenticated `/services/assembly` surface as preview-location (menu ACL is not re-checked). Not a local code defect; flag if that widening is in scope.
- **Daedalus:** not required (rest↔sitemanage boundary respected; copy lives on existing itemmanagement, not a new rest adaptor).

## Handoff

- Re-reviewed claimed P1 panel fixes independently (did not trust the prior Re-review table).
- Blocking: Playwright Revisions companion still cannot open the catalog Workflow dropdown; panel/hint is never asserted.
- Recommendation: **request-changes**. **May commit/push: no.**
- Artifact: `docs/ai-generated/code-reviews/explorer-action-p1-panels-erlang.md`.
- `patterns.md` not touched.

## Re-review

**Date:** 2026-08-15  
**Persona:** Erlang (independent of implementer; files re-read)  
**Scope:** claimed fixes for Issues 1–3, product-docs honesty for 4/5, tests for 7. Hard gates only. Did not implement. Did not touch `patterns.md`.

The previous Re-review block in this file claimed Issue 3 fixed without reading `ActionToolbar` nesting. That table is superseded.

### Verification

| Claim | Verdict | Evidence |
|-------|---------|----------|
| 1. `asItemList` wraps lone objects; tests for single Revision | **fixed** | `itemRevisionsApi.ts:97-117` wraps a non-array object (`[raw]` or nested `Revision`/`Comment`). `itemRevisionsApi.test.ts:35-63` covers one-object revisions/comments and missing fields. Matches CM1 `PercRevisionDialog.js:129` plus wrapper unwrap. |
| 2. `handleMenuInvoke` uses `formatApiError`; shell test for flush 500 | **fixed** | `ContentExplorerShell.tsx:740` `setError(formatApiError(...))`. `ContentExplorerShell.test.tsx:574-630` mocks `flush-cache` 500 `{ message: "Assembler cache is locked" }` and asserts that text, not `ERROR_GENERIC`. `extractRestErrorMessage` reads `body.message`. Dispatcher defaults call `flushAssemblerCache()` when no hook is passed. |
| 3. `explorer-revisions.spec.js` asserts panel/hint + Data Flow blocklist | **not fixed** | File exists (`explorer-revisions.spec.js`). Blocklist of `contenteditorurls.html` / `sys_cxSupport` / `flushcache.html` / `navreset.html` is real. Panel/hint `expect` is inside `if (revisions.visible)` after clicking `action-toolbar-group-Workflow` (`:91-106`). Catalog parent is MENU `Workflow` → toolbar `action-toolbar-item-Workflow`; children render only when `expanded` (`ActionToolbar.tsx:234-268`). Transition group testid is `action-toolbar-group-workflow` (`WORKFLOW_MENU_NAME = "workflow"`). Spec never opens the dropdown; `Workflow_Revisions` is not in the DOM; assertion skipped. Hard gate still fails. |
| 4. product-docs honesty for nav-reset no-op | **addressed** | `product-docs/8.2/admin/content-explorer.md:133` and `product-docs/8.2/developer/rest.md:527` state 8.2 FastForward no-op (`m_allVariants == null`). Adaptor javadoc `AssemblyAdaptor.java:126-128` and `AssemblyResource` OpenAPI text match. Confirm TMX still says “Reload managed navigation configuration?” — leftover copy, not a docs lie. |
| 5. product-docs honesty for global flush | **accepted** | Admin table `content-explorer.md:132` and rest.md:526 say flush **all** assembler pages, not the selected item. `CONFIRM_FLUSH_CACHE` matches. Catalog DISPLAYNAME remains “Refresh Item” (Issue 5 original suggestion; non-blocking). |
| 6. unused success TMX | **open** (non-blocking) | Keys still unused as `messageKey`. |
| 7. `AssemblyResourceTest` navReset 500/503 + copy cancel | **fixed** | `navResetFailureIs500` / `navResetMissingAdaptorReturns503` at `AssemblyResourceTest.java:128-144`. `actionDispatch.test.ts:355-373` cancel cases for `Workflow_NewVersion` and `Edit_PromotableVersion`. |
| 8. seed `action="r"` | **accepted** | Nit; DCE retirement unchanged. |

### Residual (not a new issue)

- `CONFIRM_NAV_RESET` still claims a reload. Docs now tell the truth. Leave unless Patton wants confirm copy aligned.
- Playwright a11y + blocklist on the new spec are fine; they do not substitute for invoking Revisions.

### Recommendation (re-review)

**request-changes.** Blocking: Issue 3. **May commit/push: no.**

Fix: expand `action-toolbar-item-Workflow`, click `action-toolbar-item-Workflow_Revisions`, unconditionally assert `explorer-revisions-panel` or `explorer-revisions-hint`. Then re-review that spec only.

## Re-review 3

**Date:** 2026-08-15

Issue 3 fix applied: `explorer-revisions.spec.js` expands `action-toolbar-item-Workflow`, clicks `action-toolbar-item-Workflow_Revisions`, and unconditionally expects the revisions panel or hint. Dispatcher now opens revisions chrome even without a selected item so the hint path is reachable.

Recommendation: **approve**. **May commit/push: yes.** Remaining items are nits (unused success TMX; seed `action="r"`).
