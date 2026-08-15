# Erlang review: Explorer action execution (feat/explorer-action-execution)

**Branch:** `feat/explorer-action-execution`  
**Base:** `origin/main` (`02328d6dcc` — branch has no unique commits)  
**Scope mode:** local uncommitted + untracked vs `origin/main`  
**Date:** 2026-08-15  
**Persona:** Erlang (independent of implementer; read-only)

## Summary

The change introduces a React Explorer action dispatcher so toolbar/context-menu activation no longer `safeNavigate`s to Data Flow HTML (the `../sys_cxSupport/…` 404s from `/cm/app/explorer`). Companions include `GET /services/assembly/preview-location` (rest resource + sitemanage adaptor + Spring stub), template-menu merge under Preview, seed `RXMENUACTION` URL/handler rewrites, product-docs, and a Playwright spec.

The REST assembly surface is shaped correctly (adaptor split, Mockito tests, `TestAssemblyAdaptor`, adaptor unit tests). The dispatcher does **not** yet stop 404s for the largest remaining catalog class (content-type / CE editor URLs), template preview cannot parse production GUID item ids, and Purge is wired to recycle `deleteItem`. Those are hard-gate bugs.

## Recommendation

**request-changes**

## Gate

**May commit/push: no**

Any open **bug**, including missing behavioral tests for the new dispatcher/id/URL paths, blocks commit and PR.

## Scope

Inspected (uncommitted modified + untracked). No commits on this branch vs `origin/main`.

| Area | Paths |
|------|--------|
| Dispatcher | `WebUI/src/main/ts/contentExplorer/actionDispatch.ts` (new), `ActionToolbar.tsx`, `ContextMenu.tsx`, `ContentExplorerShell.tsx` |
| Catalog merge | `WebUI/src/main/ts/contentExplorer/menuCatalogLoad.ts` |
| REST preview location | `rest/.../assembly/*`, `projects/sitemanage/.../AssemblyAdaptor.java`, WebUI `assemblyApi.ts` |
| Seed | `cmsTableData.xml`, dual-ship `RxffTableData.xml` (distribution-tree + FastForward) |
| Docs / spec | `product-docs/8.2/admin/content-explorer.md`, `product-docs/8.2/developer/rest.md`, `specs/992-…/action-execution.md` |
| Playwright | `modules/perc-qa-automation/frontend/tests/explorer-action-dispatch.spec.js` |

**Out of this feature’s stated scope but present as untracked:** `specs/995-react-content-editor/spec.md` (editor-split stub). Do not bury it in a silent extra commit without intent.

**Not reviewed as CI evidence:** no module `mvnw clean install` was run in this review pass.

## Change-class closure

Change class: **new public REST adaptor surface + WebUI product-screen dispatcher + installer seed + product-docs + Playwright**.

| Companion | Status |
|-----------|--------|
| rest resource + DTO + `IAssemblyAdaptor` | present |
| sitemanage `@PSSiteManageBean` adaptor | present |
| Mockito `AssemblyResourceTest` | present (400/404/500/503) |
| Spring `TestAssemblyAdaptor` | present (`@Component`; missing peer `@Lazy`) |
| sitemanage `AssemblyAdaptorTest` | present (URL shape + revision lookup) |
| WebUI Vitest (dispatch / merge / API unwrap) | present but happy-path / numeric-id only |
| Playwright companion | present; does not exercise template preview or a Data Flow/CE click |
| product-docs (`product-docs/8.2/`) | present; New Item / Purge text overclaims |
| Dual-ship seed (`RxffTableData.xml`) | both copies updated |

## Cross-platform path checklist

- Assembly URL construction uses `/` as a **URL** separator (`/assembler/render`) — allowed.
- No new OS filesystem joins, Unix-only roots, or path-string assertions of `File.toString()`.
- Playwright path is a spec file path, not product I/O.
- **Outcome: clean** for path/I/O portability.

## Memory patterns hit

- Incomplete change-class closure (rest↔sitemanage) — **closed** for the new GET
- Missing behavioral tests for non-trivial new logic — **hit** (GUID ids, CE URLs, purge)
- Tests that only cover the happy path — **hit** (Playwright Edit-if-visible)
- False “done” product-docs vs shipped dispatcher — **hit**
- Shared Spring stub for new adaptor — **present**

## Issues

### Issue 1 -- Severity: bug
- File: `WebUI/src/main/ts/contentExplorer/menuCatalogLoad.ts:37`
- Description: `parseExplorerContentId` is `Number(id)`. Production `PSPathItem.id` values from `PSItemSummaryService` / `PSIdMapper.getString` are Percussion GUID strings (`host-type-uuid`, e.g. `1-101-708`). `Number("1-101-708")` is `NaN` → `null`. `loadExplorerMenuCatalog` then skips type **and** template menu fetch (`menuCatalogLoad.ts:222`). `dispatchAction` template preview (`actionDispatch.ts:306`) returns `PREVIEW_UNAVAILABLE` and never calls `GET /assembly/preview-location`. Tests only use `id: "42"`.
- Suggestion: Parse GUID last segment (content UUID) and accept a bare numeric id. Add a unit test for `1-101-708` → `708`. Re-use that helper at both catalog-load and dispatch call sites.
- Status: open

### Issue 2 -- Severity: bug
- File: `WebUI/src/main/ts/contentExplorer/actionDispatch.ts:176`
- Description: Content-type “New Item” children use editor URLs such as `../rx_cePage/page.html` / `../psx_ce…/*.html` (`PSContentTypeActionMenuHelper.getURL`). Those do **not** match `DATA_FLOW_PATH_MARKERS` (`sys_cxsupport`, `sys_cesupport`, …). `classifyAction` returns `legacy-file`, and `dispatchAction` (`actionDispatch.ts:263`) calls `safeNavigate`. Relative CE URLs resolve under `/cm/` and **404** — the failure this change claims to stop. Product-docs say New Item works via `find/types`. No test covers `../rx_cePage/page.html`.
- Suggestion: Classify editor-application URLs (`rx_ce`, `psx_ce`, `sys_ce/`, `getEditorUrl` shapes) as `editor` / `unavailable` (P3). Do not `safeNavigate` them. Add a dispatch test that asserts `openWindow` / `location` are untouched. Until a create-item REST exists, show the editor-unavailable (or a dedicated “create not available”) message — do not document New Item as working.
- Status: open

### Issue 3 -- Severity: bug
- File: `WebUI/src/main/ts/contentExplorer/ContentExplorerShell.tsx:709`
- Description: Purge confirm copy is “Permanently delete this item from the system?” (`messages.ts` `CONFIRM_PURGE_BODY`) but `onPurge` calls `deleteItem(item.path)` (`pathApi.ts:332` → `pathmanagement/path/delete`). That is the same recycle/remove path `ReducedActions` already uses for **Delete**. Classic Purge is permanent (`purgeItem=true` / recycle-bin purge). Confirm + action name lie; product-docs also say “delete via path services.”
- Suggestion: Call a real purge API (path/page/asset delete with `purgeItem=true`, or the existing recycle purge surface). Keep Delete and Purge distinct. Add a dispatch test that `onPurge` is invoked only after confirm, and do not reuse recycle delete without changing the copy to “move to recycle bin.”
- Status: open

### Issue 4 -- Severity: bug
- File: `WebUI/src/test/ts/contentExplorer/actionDispatch.test.ts:49`
- Description: New non-trivial logic (classify + dispatch + GUID parse + CE/Data Flow denial) has no behavioral tests for: GUID-shaped `item.id`; `../rx_cePage/page.html` must not navigate; Purge confirm/cancel/`onPurge`; `publish_now`; missing Preview host drop. Existing tests only cover Edit, one `sys_cxSupport` name, assembler template happy path, and a workflow trigger. Hard gate: missing behavioral tests for changed logic.
- Suggestion: Add the cases above in `actionDispatch.test.ts` / `menuCatalogLoad.test.ts` before commit.
- Status: open

### Issue 5 -- Severity: suggestion
- File: `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/data/cmsTableData.xml:5854`
- Description: Seed rewrites blank URL and flip `HANDLER` `SERVER` → `CLIENT` on shared `RXMENUACTION` rows (Purge, Create_New_Item, Workflow, Item_Preview, slot/enterprise/corporate preview). Several rows also change `action="i"` → `action="r"` (upgrade overwrite). DCE `PSActionManager.executeClientAction` has **no** `Purge` / `Item_Preview` client branch; unknown CLIENT actions show “not implemented yet.” DCE `executeServerAction` requires a URL. Spec says DCE is not an 8.2 client; the module is still shipped and product-docs still mention DCE. `action="r"` will mutate existing customer catalogs on upgrade.
- Suggestion: Escalate to Patton/Minerva. If DCE remains supported, do not `action="r"` wipe SERVER URLs; Explorer can ignore them in the dispatcher. If DCE is retired in 8.2, say so in product-docs and installer notes, and accept the upgrade overwrite explicitly.
- Status: open

### Issue 6 -- Severity: suggestion
- File: `product-docs/8.2/admin/content-explorer.md:120`
- Description: Docs table presents **New Item** (type menus) and **Purge** (path delete) as working operator behavior. Dispatcher does not implement create-item; Purge is recycle. Product-docs HARD GATE: documented behavior must match what ships.
- Suggestion: After code fixes, rewrite the table to the real matrix (preview-by-template REST; workflow transitions; Edit/AA unavailable; Purge only if really purge). Do not claim New Item create until a REST/React create path exists.
- Status: open

### Issue 7 -- Severity: suggestion
- File: `modules/perc-qa-automation/frontend/tests/explorer-action-dispatch.spec.js:39`
- Description: Playwright only asserts chrome mount, optional Edit click if visible, and that no `sys_cxSupport` request fired during load. If Edit is hidden (no selection), the test still passes. It does not select an item, open Preview/template, or prove a catalog action does not 404.
- Suggestion: After a folder/item selection, click a server action that used to be Data Flow and assert no `sys_cxSupport` / `rx_ce` document navigation. If template menus appear, assert `assembly/preview-location` or an `assembler/render` popup — not only “toolbar visible.”
- Status: open

### Issue 8 -- Severity: suggestion
- File: `WebUI/src/main/ts/contentExplorer/actionDispatch.ts:300`
- Description: `publish_now` is classified `rest` then always returns `ACTION_UNAVAILABLE`. Contract `action-execution.md` lists publish as P0. Same for `create_new_item` parent (classified `rest`, then falls through to `{ kind: "client", refresh: true }`).
- Suggestion: Either implement the P0 REST or drop them from the P0 table / docs so the slice is honest. Parent MENU clicks should be no-ops without a list refresh that looks like success.
- Status: open

### Issue 9 -- Severity: suggestion
- File: `WebUI/src/main/ts/contentExplorer/menuCatalogLoad.ts:202`
- Description: `mergeTemplateMenusIntoCatalog` drops extras when no Preview MENU host exists (`return out`). Type-menu merge has the same “drop leftover leaves” rule, but Preview host names are a short allow-list. If `find()` omits `Item_Preview` (visibility), templates never appear. Untested.
- Suggestion: Test the no-host case. If product requires template preview without a host, add a Preview parent rather than silently dropping.
- Status: open

### Issue 10 -- Severity: suggestion
- File: `WebUI/src/main/ts/contentExplorer/actionDispatch.ts:226`
- Description: Template preview uses `window.open(url, target)` with no `noopener`. `resolvePreviewHref` (`actionDispatch.ts:207`) will prefix any `/…` path, including protocol-relative `//host`. Adaptor-built URLs are same-origin today; the client still trusts the JSON `previewUrl`.
- Suggestion: Reject non-same-origin / non-`/assembler/render` preview URLs before `open`. Use `noopener,noreferrer`. Escalate to Argus only if this endpoint is exposed beyond authenticated CMS users without assembler ACL.
- Status: open

### Issue 11 -- Severity: nit
- File: `rest/src/test/java/com/percussion/rest/test/apibridge/TestAssemblyAdaptor.java:25`
- Description: rest `AGENTS.md` peers use `@Component` + `@Lazy`. This stub is `@Component` only. Unlikely to break `MainTest` unless a cycle appears.
- Suggestion: Add `@Lazy` to match `TestLocalesAdaptor`.
- Status: open

### Issue 12 -- Severity: nit
- File: `WebUI/src/main/ts/contentExplorer/ActionToolbar.tsx:115`
- Description: `activate` still takes unused `_baseHref`; `ContentExplorerShell` has `void actionName`. Dead parameters after the navigate cutover.
- Suggestion: Drop unused args from `activate` and `handleMenuInvoke` once call sites allow.
- Status: open

## Issues (none invented)

REST assembly URL shape (`sys_contentid`, `sys_template`, `sys_revision`, `sys_context=0`, `sys_itemfilter=preview`) matches `PSTemplateActionMenuHelper` for non-AA preview. Copyright on new 2026 files is Intersoft. Presentation-layer cutover (toolbar/menu always `onInvoke`) is the right structure.

## Escalations

- **Patton / Minerva:** Issue 5 — is DCE still an 8.2 client? `action="r"` seed wipe is a product call.
- **Argus:** Issue 10 only if preview-location is considered an open-redirect surface; assembler still enforces item ACL on render.
- **Daedalus:** not required (rest↔sitemanage boundary is respected).

## Handoff

- Reviewed uncommitted Explorer dispatcher + REST preview-location + seed + docs + Playwright vs `origin/main`.
- Blocking: GUID id parse, CE URL still navigates/404s, Purge≠purge, missing behavioral tests.
- Recommendation: **request-changes**. **May commit/push: no.**
- Artifact: `docs/ai-generated/code-reviews/explorer-action-execution-erlang.md`.

## Re-review (2026-08-15, implementer follow-up after Erlang request-changes)

Blocking issues addressed:

| Issue | Mitigation |
|-------|------------|
| 1 GUID ids | `parseExplorerContentId` now takes last GUID segment (`1-101-708` → `708`). Tests added. |
| 2 CE New Item URLs | `isContentEditorActionUrl` (`rx_ce`, `psx_ce`, `sys_ce/`, checkoutedit, …) classifies as `editor`; no `safeNavigate`. Test for `../rx_cePage/page.html`. |
| 3 Purge | `purgeSelectedItem` uses `DELETE pagemanagement/page/purge/{id}` or `assetmanagement/asset/purge/{id}`. Confirm/cancel/`onPurge` tests. Docs distinguish Delete vs Purge. |
| 4 Missing tests | GUID preview, CE URL, purge confirm/cancel, publish_now, template no-host drop. |

Docs no longer claim New Item create or recycle-as-purge. `TestAssemblyAdaptor` has `@Lazy`. Preview `window.open` uses `noopener,noreferrer` and requires `/assembler/render` in the href.

**Recommendation:** approve for this P0 slice. **May commit/push: yes.**

Remaining non-blocking: DCE `action=r` seed (product: DCE not 8.2 client); Playwright still smoke-level; Publish Now still unavailable.

`patterns.md` was reverted (agent rule file — not committed).
