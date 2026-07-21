# SC-003 checklist: ≥10 high-value configuration-driven actions (US3 / T056)

**Gate**: SC-003 — full-menu phase (P-Menu) supports a **minimum of 10 high-value
actions** spanning open/edit, folder ops, and at least two workflow or
properties actions (FR-010–FR-013; capability-matrix.md P-Menu enumeration).

**Server**: `com.percussion.rest.actions.ActionMenuResource` at
`/Rhythmyx/rest/actions/*` (see `specs/992-react-content-explorer/contracts/action-menu-api.md`).

**Acceptance**: each row below maps to a `MenuAction` produced by
`findActions()` / `findAllowedContentTypeMenus()` / `findAllowedTemplateMenus()`
that the modern Content Explorer renders via `ContextMenu` / `ActionToolbar`
(US3 T053 / T054). Action execution is host-specific (mapped from server `url`
or `handler`); see per-row Execute path.

## Action enumeration (12 rows; ≥10 covered; ≥2 workflow)

| #  | Action category           | Server `name` / handler (canonical)        | Selection | Execute path                                                  |
|----|---------------------------|--------------------------------------------|-----------|----------------------------------------------------------------|
| 1  | Open                      | `perc.ui.explorer.open` / `client`         | Item / Folder | `ReducedActions.open` → `openInEditor.openInEditor` (US1 T021)        |
| 2  | Edit                      | `perc.ui.explorer.edit` / `client`         | Item       | Navigate to `/cm/app/editAsset.jsp?...` (legacy URL preserved)        |
| 3  | Preview                   | `perc.ui.explorer.preview` / `client`      | Item / Folder | `ReducedActions.preview` → editor preview tab or `/perc-finder.preview` |
| 4  | Folder — create           | `perc.ui.explorer.createFolder` / `client` | Folder     | `ReducedActions.createFolder` → `pathApi.addNewFolder` (US1 T020)      |
| 5  | Folder — rename           | `perc.ui.explorer.rename` / `client`       | Folder / Item | `ReducedActions.rename` → `pathApi.renameFolder` (US1 T020)            |
| 6  | Folder — move             | `perc.ui.explorer.move` / `client`         | Folder / Item | `ReducedActions.move` → `pathApi.moveItem({copy:false})` (US1 T020)   |
| 7  | Folder — copy             | `perc.ui.explorer.copy` / `client`         | Folder / Item | `ReducedActions.copy` → `pathApi.moveItem({copy:true})` (US1 T020)    |
| 8  | Folder — delete (confirm) | `perc.ui.explorer.delete` / `client`       | Folder / Item | `ReducedActions.delete` → `window.confirm` → `pathApi.deleteItem`       |
| 9  | Properties (folder)       | `perc.ui.explorer.folderProperties` / `client` | Folder | Navigate to `/cm/pages/app/folderproperties?path=...` (US4 T060)    |
| 10 | Workflow — check in/out   | `perc.ui.explorer.forceCheckIn` / `client` | Item (checked out) | POST `/services/workflow/checkin` (rest `ItemWorkflowResource`)  |
| 11 | Workflow — transition     | `perc.ui.explorer.transition` / `client`   | Item (workflow state) | Server `getAllowedTransitions` (rest `ActionMenuResource`, currently `// Not implemented yet` — see gap policy below) |
| 12 | Workflow — history        | `perc.ui.explorer.transitionHistory` / `client` | Item | Navigate to `/Rhythmyx/.../workflow/history?itemId=...` (existing JSP) |

> **Two of #10–#12 cover the SC-003 workflow clause.** All three are
> mapped to existing server paths (gap: #11 has no REST list
> endpoint yet — see below).

## Gap policy (per `contracts/action-menu-api.md`)

| Action | Server listing | Execution | US3 status |
|--------|----------------|-----------|------------|
| 1–9 (folder ops + nav) | Partial coverage via `findActions` | Existing sitemanage REST (`pathApi`) + existing JSP routes | **Ready** (reduced-action set covers execute today; full P-Menu maps server-side `url`/`handler` to the existing routes) |
| 10 — force check-in | `findAllowedContentTypeMenus` exposes | Existing `rest/ItemWorkflowResource` | **Ready** (no new façade needed) |
| 11 — allowed transitions | `getAllowedTransitions` is currently `// Not implemented yet` per `ActionMenuResource.java:99-102` | Existing sitemanage service `PSWorkflowService.getAllowedTransitions` | **Gap** — record in `contracts/capability-matrix.md` P-Menu row; tracked separately under `rest` (T052 deferred; not blocking 8.2 per T052 decision 2026-07-20) |
| 12 — workflow history | `findActions({name: 'transitionHistory'})` exposes the navigation URL | Read-only navigation to `/Rhythmyx/...` | **Ready** (no new façade; existing JSP coverage) |

**T052 decision (2026-07-20)**: **No new sitemanage or `rest` façade is
required for US3 P-Menu in 8.2.** The 10 of 12 actions above reuse
existing server paths; #11 is a known gap that does not block P-Menu
listing / rendering — the modern explorer can render #10 and #12
alongside a stub or "not supported in this release" label for #11
until a follow-up `rest` enhancement lands the GET transitions list.

## Test coverage map

| Layer | Spec / file | Covers |
|-------|-------------|--------|
| Vitest (unit / mapper) | `WebUI/src/test/ts/contentExplorer/actionMenuApi.test.ts` (12 tests) | All 12 action shapes wire-encode / decode; mapper stability (no mutation, sortRank, label fallback, child flattening) |
| Vitest (component) | `WebUI/src/test/ts/contentExplorer/ContextMenu.test.tsx` (7 tests) | Render leaves, cascading children, empty state, click/activate, Escape close, aria-label |
| Vitest (component) | `WebUI/src/test/ts/contentExplorer/ActionToolbar.test.tsx` (4 tests) | Render toolbar, click → onInvoke, empty state, aria-label |
| Playwright (E2E live CMS) | `modules/perc-qa-automation/frontend/tests/us3-menus.spec.js` (4 tests) | Toolbar mounts in modern React shell, menu present, keyboard navigation, refresh on selection change |

Both layers must be green for the SC-003 row in
`contracts/capability-matrix.md` P-Menu to flip to **Done**.

## Acceptance evidence log

- 2026-07-20: Vitest layers green (23 tests, 0 failing). Playwright
  spec scaffolded; behavioral verification against the live
  dev CMS is in `T057b`. The dev CMS has no installed action menus
  (`GET /actions/find` returns `{"ActionMenu":[]}`), so SC-003's
  "≥10 actions visible" verification is documented as a coverage
  expectation against a system-installed CMS, not this minimal
  Derby-mode dev image. Production / UAT must run
  `us3-menus.spec.js` against a system-installed CMS (or a CMS with
  the standard finder action set) to flip the matrix row to Done.
