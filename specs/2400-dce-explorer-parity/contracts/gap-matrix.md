# Gap matrix: Desktop Content Explorer → SPA Explorer

**Parent:** #2400  
**Evidence date:** 2026-08-10 (closeout re-audit #2794 against `origin/main` after #2748 / #2773 / #2775 / #2777 / #2782)  
**Rule:** Status reflects **product route** (`ExplorerRoute` → `ContentExplorerShell`), not isolated components in the registry.

Legend: **Present** | **Partial** | **Missing** | **OUT**

## Navigation & chrome

| Capability | DCE source | Explorer / REST today | Status | Slice |
|------------|------------|------------------------|--------|-------|
| Sites/folders tree | `PSNavigationTree` | `ExplorerTree` + path APIs | Present | — |
| Detail list of children | Main list | `DetailList` + `paginatedFolder` | Present | — |
| Full product shell composition | Frame + panels | Shell composes search, menus, DF, security on product route | **Present** | #2407 · [PR #2412](https://github.com/intersoftdatalabs-in/percussioncms/pull/2412) (merged) · QA [#2588](https://github.com/intersoftdatalabs-in/percussioncms/issues/2588) |
| Menu bar (Content / View / Help) | `ContentExplorerMenu.xml` | `ExplorerMenuBar` on product shell (nested Content/View/Help; view tools under View) | **Present** | #2731 · absorbed [PR #2748](https://github.com/intersoftdatalabs-in/percussioncms/pull/2748) · nested ActionToolbar also #2730 / [PR #2782](https://github.com/intersoftdatalabs-in/percussioncms/pull/2782) |
| Server-driven toolbar/context menus | Action manager + server menus | `ActionToolbar` (nested MENU dropdowns) + `ContextMenu` + `actionMenuApi` wired in shell | **Present** | #2407 · [PR #2412](https://github.com/intersoftdatalabs-in/percussioncms/pull/2412) · nested dropdown #2730 / #2731 |
| Reduced create/rename/move/copy/delete | Folder actions | `ReducedActions` in shell | Present | — |
| Open / preview item | Open handlers | `openInEditor` + product `openPreviewItem` (page render / asset view URL) from selection | **Present** | #2733 · absorbed [PR #2748](https://github.com/intersoftdatalabs-in/percussioncms/pull/2748) |
| Multi-select list | Selection model | `multiSelectedIds` / `DetailList` multi-select in shell | **Present** | #2408 · [PR #2522](https://github.com/intersoftdatalabs-in/percussioncms/pull/2522) (merged) |
| Display formats for columns | Display format catalog | `GET /rest/displayformats?validForFolder=true` + shell selector + `displayProperties` | **Present** | #2407 · [PR #2412](https://github.com/intersoftdatalabs-in/percussioncms/pull/2412) |
| View options / refresh | View menu | `ExplorerMenuBar` View → Refresh + panel toggles + display format; always-visible refresh residual | **Present** | #2731 · #2733 · [PR #2748](https://github.com/intersoftdatalabs-in/percussioncms/pull/2748) |

## Search

| Capability | DCE source | Explorer / REST today | Status | Slice |
|------------|------------|------------------------|--------|-------|
| Simple / extended search | `PSSearchDialog` | `SearchPanel` toggle in product shell | **Present** | #2407 · [PR #2412](https://github.com/intersoftdatalabs-in/percussioncms/pull/2412) |
| Open / reveal from results | Search results | Callbacks on panel | Present | #2407 |
| Saved searches catalog + run | CE saved search | Catalog `GET /rest/searches` + execute façade `POST /services/searches/{idOrName}/execute` + `SearchPanel` picker/run on product shell | **Present** | #2409 · A–D #2504–#2507 · [PR #2579](https://github.com/intersoftdatalabs-in/percussioncms/pull/2579) / [#2592](https://github.com/intersoftdatalabs-in/percussioncms/pull/2592) / [#2606](https://github.com/intersoftdatalabs-in/percussioncms/pull/2606) / [#2644](https://github.com/intersoftdatalabs-in/percussioncms/pull/2644) |
| Search in ContentBrowser hosts | Dialogs | `ContentBrowser` mounts shared `SearchPanel` when `enableSearch` (asset picker host on); catalog + free-text + saved execute reuse Explorer APIs | **Present** | #2793 · [PR #2798](https://github.com/intersoftdatalabs-in/percussioncms/pull/2798) (merged) |

## Security & properties

| Capability | DCE source | Explorer / REST today | Status | Slice |
|------------|------------|------------------------|--------|-------|
| Folder ACL view/edit | `PSFolderSecurityPanel` | `FolderSecurityPanel` toggle on product shell with identities + saveFolderProperties | **Present** | #2410 · [PR #2599](https://github.com/intersoftdatalabs-in/percussioncms/pull/2599) (merged) · QA [#2600](https://github.com/intersoftdatalabs-in/percussioncms/issues/2600) |
| Object ACL editor (full) | CE ACL dialogs | Partial other epics (#2274 family) | **Partial** | link existing ACL epics |
| Folder properties (community, locale, DF, workflow) | `PSFolder*Panel` | `FolderPropertiesEditor` inside `FolderSecurityPanel` on product shell | **Present** | #2410 · [PR #2599](https://github.com/intersoftdatalabs-in/percussioncms/pull/2599) (merged) · QA [#2600](https://github.com/intersoftdatalabs-in/percussioncms/issues/2600) |

## Advanced / power user

| Capability | DCE source | Explorer / REST today | Status | Slice |
|------------|------------|------------------------|--------|-------|
| Clipboard cut/copy/paste multi | `PSClipBoard` | `ClipboardPanel` + multi-select wired in product shell | **Present** | #2408 · [PR #2522](https://github.com/intersoftdatalabs-in/percussioncms/pull/2522) (merged) |
| Site copy wizard | CE wizards | `SiteCopyWizard` on product shell via Content → Site Copy (`content-site-copy`); source prefilled from `/Sites/<name>` | **Present** | #2767 · [PR #2773](https://github.com/intersoftdatalabs-in/percussioncms/pull/2773) (merged) |
| Subfolder copy wizard | CE wizards | `SubfolderCopyWizard` on product shell via Content → Subfolder Copy (`content-subfolder-copy`); source prefilled from active/selected folder path | **Present** | #2792 · [PR #2796](https://github.com/intersoftdatalabs-in/percussioncms/pull/2796) (merged) |
| Dependency viewer | `PSDependencyViewer` | `DependencyViewer` + relationship summary REST mounted in product Explorer shell (View → Dependencies; #2768) | **Present** | #2768 · [PR #2775](https://github.com/intersoftdatalabs-in/percussioncms/pull/2775) (merged) · advanced chrome (phase 4) |
| IA / relationships view | Managers | `RelationshipsView` on product shell (View → IA Relationships; selected item + REST summary) | **Present** | #2769 · [PR #2777](https://github.com/intersoftdatalabs-in/percussioncms/pull/2777) (merged) · advanced chrome (phase 4) |
| Translation workflow | CE translation | **REST Present:** `GET|POST /rest/content-explorer/translations` (#2429 / [PR #2601](https://github.com/intersoftdatalabs-in/percussioncms/pull/2601)). **Explorer UI Present (slice C #2430):** `TranslationsPanel` in `ContentExplorerShell` (locale list + create-variant; Vitest + Playwright `explorer-translations.spec.js`). In-flight queue + session content-locale **OUT** pending product sign-off (not exposed by REST). See [p-trans-api-inventory.md](../research/p-trans-api-inventory.md). | **Partial** (item locales + create **Present**; in-flight/session **OUT/Missing**) | #2411 → #2428 inventory, #2429 REST, #2430 UI |
| Workflow transitions in menus | CE workflow | `itemWorkflowApi` + `workflowMenuActions` merge Workflow group into toolbar/context menus; invoke `transitionWithComments` for `workflow-transition:*` triggers | **Present** | #2732 · absorbed [PR #2748](https://github.com/intersoftdatalabs-in/percussioncms/pull/2748) · QA [#2743](https://github.com/intersoftdatalabs-in/percussioncms/issues/2743) |

## Explicit OUT (for now)

| Capability | Reason |
|------------|--------|
| JavaFX desktop window / native file chooser parity | Platform desktop; web redesign for file ops |
| DCE help JavaHelp topics 1:1 | Product help site / in-app help later |
| DCE-only packaging residual | Decommission program after parity |

## Implementation notes

### 2026-08-10 closeout re-audit (#2794)

Re-audited product route on `main` after thrash merges. Matrix flips / cite updates:

| Capability | Was | Now | Evidence on main |
|------------|-----|-----|------------------|
| Workflow transitions in menus | Partial | **Present** | #2732 absorbed by cluster [PR #2748](https://github.com/intersoftdatalabs-in/percussioncms/pull/2748) (`ContentExplorerShell` + `workflowMenuActions` + `itemWorkflowApi`) |
| Menu bar / preview / view refresh | Present | **Present** (cites) | Same cluster #2748 absorbs #2731 + #2733 |
| Site copy | Present | **Present** (cites) | [PR #2773](https://github.com/intersoftdatalabs-in/percussioncms/pull/2773) merged |
| Relationships | Present | **Present** (cites) | [PR #2777](https://github.com/intersoftdatalabs-in/percussioncms/pull/2777) merged |
| Dependency viewer | Present | **Present** (cites) | [PR #2775](https://github.com/intersoftdatalabs-in/percussioncms/pull/2775) merged (no longer leave Partial) |
| Saved searches catalog + run | Partial | **Present** | #2505–#2507 merged; `SearchPanel` execute on product shell |
| Folder ACL + folder properties | Partial | **Present** | #2410 / [PR #2599](https://github.com/intersoftdatalabs-in/percussioncms/pull/2599) merged |
| Subfolder copy | Partial | **Present** | #2792 / [PR #2796](https://github.com/intersoftdatalabs-in/percussioncms/pull/2796) merged on product shell |
| ContentBrowser host search | Partial | **Present** | #2793 / [PR #2798](https://github.com/intersoftdatalabs-in/percussioncms/pull/2798) merged |
| Translation (in-flight/session) | Partial | **Partial** | Intentional OUT residual under #2411 |

No product code residual filed this run — all Present rows above have product-route evidence on `main`.

### 2026-08-10 refresh (ContentBrowser search host #2793)

- **#2793 Search in ContentBrowser hosts:** `enableSearch` mounts shared `SearchPanel` (not a stub input). Open → selection for confirm; Reveal → folderPath navigation. Transport seams optional for Vitest. Primary residual host `assetPickerModern.jsp` sets `enableSearch: true`. Playwright `host-asset-picker` soft-skips when host fixture lacks the flag. Matrix row **Present**.

### 2026-08-10 refresh (site copy shell #2767)

- **#2767 Site copy wizard in Explorer shell chrome:** Content → Site Copy mounts `SiteCopyWizard` on `ContentExplorerShell` when selection is under `/Sites/<name>` (pure `sitePath` helper). Vitest shell/menu wiring + Playwright `explorer-site-copy.spec.js` (soft-skip multi-site submit on H2). Matrix row **Present**.

### 2026-08-10 refresh (subfolder copy shell #2792)

- **#2792 Subfolder copy wizard in Explorer shell chrome:** Content → Subfolder Copy mounts `SubfolderCopyWizard` on `ContentExplorerShell` when a non-root folder path is in context (pure `folderPath` helper; prefers selected folder row). Submit remains existing `pathApi.moveItem({copy:true})`. Vitest shell/menu wiring + Playwright `explorer-subfolder-copy.spec.js` (soft-skip multi-folder submit on H2). Matrix row **Present**.

### 2026-08-10 refresh (dependency viewer #2768)

- **#2768 Dependency viewer in Explorer shell:** View → Dependencies toggle on `ExplorerMenuBar`; `ContentExplorerShell` mounts `DependencyViewer` for the selected content item (relationship summary REST loaders reused; optional `loadDependencySummary` test seam). Select-item hint when no eligible selection. Unique panel/hint ids (`explorer-dependencies-panel` / `explorer-dependencies-hint`) mirror site-copy aria-controls. Vitest shell wiring + Playwright `explorer-dependencies.spec.js` (soft-skip deep counts without fixtures). Matrix row **Present**.

### 2026-08-10 refresh (menu bar #2731 + relationships #2769)

- **#2731 Explorer DCE top menu bar:** `ExplorerMenuBar` (Content / View / Help) on `ContentExplorerShell`; view tools nested under View (not multi-row flat buttons). `ActionToolbar` renders `children[]` as nested dropdowns (coordinates with #2730). Vitest + Playwright `explorer-menu-bar.spec.js`. Matrix row **Present**.
- **#2769 Relationships view in Explorer shell:** View → IA Relationships toggles `RelationshipsView` for the selected item (REST `/content-explorer/relationships/{id}/summary`); select-item hint when none. Vitest shell + menu bar; Playwright `explorer-relationships.spec.js`. Matrix row **Present**. Sibling DependencyViewer shell mount is **Present** (#2768).

### 2026-08-09 refresh (P-Trans UI #2430)

- **#2429 / PR #2601 merged:** public REST create-variant + item-locale list.
- **#2430 Explorer UI:** `TranslationsPanel` + shell toggle consumes REST; Vitest + surface Playwright. Matrix row **Partial** (Present for locales/create; in-flight + session content-locale still OUT).
- **#2407 / PR #2412 merged:** product shell composition, server action toolbar/context menu, search panel toggle, folder-valid display formats + column path, folder security toggle → **Present** (human QA still open on #2588).
- **#2408 / PR #2522 merged:** multi-select list + clipboard panel in shell → **Present**.
- **#2504 / PR #2579 merged:** saved-search execute disposition = **façade**; later slices #2505–#2507 completed execute + UI + Playwright → matrix **Present** as of #2794 closeout.

### 2026-08-08 baseline

- Highest leverage was **compose shell** using existing components + `rest/actions` + `rest/displayformats` (landed).
- REST enhancement: `GET /rest/displayformats?validForFolder=true` (and optional `validForViewsAndSearches`).
- Re-audit after each slice merges; flip Partial → Present only when product route demonstrates the capability.
- **P-Trans (#2411):** inventory under `research/p-trans-api-inventory.md` (PR #2431). Public REST create-variant + item-locale list landed; Explorer UI locales/create Present; in-flight queue + session content-locale still OUT.
