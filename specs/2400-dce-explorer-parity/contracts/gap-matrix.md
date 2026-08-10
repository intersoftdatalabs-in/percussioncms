# Gap matrix: Desktop Content Explorer → SPA Explorer

**Parent:** #2400  
**Evidence date:** 2026-08-09 (refreshed after merged #2412 / #2522 / #2579)  
**Rule:** Status reflects **product route** (`ExplorerRoute` → `ContentExplorerShell`), not isolated components in the registry.

Legend: **Present** | **Partial** | **Missing** | **OUT**

## Navigation & chrome

| Capability | DCE source | Explorer / REST today | Status | Slice |
|------------|------------|------------------------|--------|-------|
| Sites/folders tree | `PSNavigationTree` | `ExplorerTree` + path APIs | Present | — |
| Detail list of children | Main list | `DetailList` + `paginatedFolder` | Present | — |
| Full product shell composition | Frame + panels | Shell composes search, menus, DF, security on product route | **Present** | #2407 · [PR #2412](https://github.com/intersoftdatalabs-in/percussioncms/pull/2412) (merged) · QA [#2588](https://github.com/intersoftdatalabs-in/percussioncms/issues/2588) |
| Menu bar (Content / View / Help) | `ContentExplorerMenu.xml` | Not in SPA shell (toolbar/context only) | **Missing** | Deferred shell chrome; no separate child yet (not blocking #2410 / #2409 / #2411) |
| Server-driven toolbar/context menus | Action manager + server menus | `ActionToolbar` + `ContextMenu` + `actionMenuApi` wired in shell | **Present** | #2407 · [PR #2412](https://github.com/intersoftdatalabs-in/percussioncms/pull/2412) |
| Reduced create/rename/move/copy/delete | Folder actions | `ReducedActions` in shell | Present | — |
| Open / preview item | Open handlers | `openInEditor` + product `openPreviewItem` (page render / asset view URL) from selection | **Present** | #2733 |
| Multi-select list | Selection model | `multiSelectedIds` / `DetailList` multi-select in shell | **Present** | #2408 · [PR #2522](https://github.com/intersoftdatalabs-in/percussioncms/pull/2522) (merged) |
| Display formats for columns | Display format catalog | `GET /rest/displayformats?validForFolder=true` + shell selector + `displayProperties` | **Present** | #2407 · [PR #2412](https://github.com/intersoftdatalabs-in/percussioncms/pull/2412) |
| View options / refresh | View menu | Refresh list control + view tool toggles in shell; full DCE View menu still OUT | **Partial** (refresh **Present**) | #2733 (refresh); full View menu with #2731 menu bar |

## Search

| Capability | DCE source | Explorer / REST today | Status | Slice |
|------------|------------|------------------------|--------|-------|
| Simple / extended search | `PSSearchDialog` | `SearchPanel` toggle in product shell | **Present** | #2407 · [PR #2412](https://github.com/intersoftdatalabs-in/percussioncms/pull/2412) |
| Open / reveal from results | Search results | Callbacks on panel | Present | #2407 |
| Saved searches catalog + run | CE saved search | Catalog `GET /rest/searches` Present; **execute UX Missing**. Disposition **façade** complete (#2504 / [PR #2579](https://github.com/intersoftdatalabs-in/percussioncms/pull/2579)) — see [saved-search-execute-disposition.md](../research/saved-search-execute-disposition.md). Implement #2505–#2507 | **Partial** | #2409 · A done #2504 · B–D open #2505–#2507 |
| Search in ContentBrowser hosts | Dialogs | Host integration pending | **Partial** | host follow-up (outside primary Explorer route) |

## Security & properties

| Capability | DCE source | Explorer / REST today | Status | Slice |
|------------|------------|------------------------|--------|-------|
| Folder ACL view/edit | `PSFolderSecurityPanel` | Panel toggle in shell (#2407); identities / polish remaining | **Partial** | #2410 |
| Object ACL editor (full) | CE ACL dialogs | Partial other epics (#2274 family) | **Partial** | link existing ACL epics |
| Folder properties (community, locale, DF, workflow) | `PSFolder*Panel` | path `folderProperties` partial UI | **Partial** | #2410 |

## Advanced / power user

| Capability | DCE source | Explorer / REST today | Status | Slice |
|------------|------------|------------------------|--------|-------|
| Clipboard cut/copy/paste multi | `PSClipBoard` | `ClipboardPanel` + multi-select wired in product shell | **Present** | #2408 · [PR #2522](https://github.com/intersoftdatalabs-in/percussioncms/pull/2522) (merged) |
| Site copy wizard | CE wizards | `SiteCopyWizard` component exists; **not** primary shell chrome | **Partial** | advanced chrome (phase 4; no open child — open only when prioritized) |
| Subfolder copy wizard | CE wizards | `SubfolderCopyWizard`; not in shell | **Partial** | advanced chrome (phase 4) |
| Dependency viewer | `PSDependencyViewer` | Components + `rest` relationship summary; not in shell | **Partial** | advanced chrome (phase 4) |
| IA / relationships view | Managers | `RelationshipsView`; not in shell | **Partial** | advanced chrome (phase 4) |
| Translation workflow | CE translation | **REST Present:** `GET|POST /rest/content-explorer/translations` (#2429 / [PR #2601](https://github.com/intersoftdatalabs-in/percussioncms/pull/2601)). **Explorer UI Present (slice C #2430):** `TranslationsPanel` in `ContentExplorerShell` (locale list + create-variant; Vitest + Playwright `explorer-translations.spec.js`). In-flight queue + session content-locale **OUT** pending product sign-off (not exposed by REST). See [p-trans-api-inventory.md](../research/p-trans-api-inventory.md). | **Partial** (item locales + create **Present**; in-flight/session **OUT/Missing**) | #2411 → #2428 inventory, #2429 REST, #2430 UI |
| Workflow transitions in menus | CE workflow | Actions path partial | **Partial** | workflow actions (under menus / future residual) |

## Explicit OUT (for now)

| Capability | Reason |
|------------|--------|
| JavaFX desktop window / native file chooser parity | Platform desktop; web redesign for file ops |
| DCE help JavaHelp topics 1:1 | Product help site / in-app help later |
| DCE-only packaging residual | Decommission program after parity |

## Implementation notes

### 2026-08-09 refresh (P-Trans UI #2430)

- **#2429 / PR #2601 merged:** public REST create-variant + item-locale list.
- **#2430 Explorer UI:** `TranslationsPanel` + shell toggle consumes REST; Vitest + surface Playwright. Matrix row **Partial** (Present for locales/create; in-flight + session content-locale still OUT).
- **#2407 / PR #2412 merged:** product shell composition, server action toolbar/context menu, search panel toggle, folder-valid display formats + column path, folder security toggle → **Present** (human QA still open on #2588).
- **#2408 / PR #2522 merged:** multi-select list + clipboard panel in shell → **Present**.
- **#2504 / PR #2579 merged:** saved-search execute disposition = **façade**; matrix row stays **Partial** until #2505–#2507 land execute + UI + Playwright.
- **Residual children not filed this run** for menu bar / advanced chrome: components mostly exist; next implement slices stay on open children **#2410**, **#2409→#2505–#2507**. File PR-sized children only when those land or product prioritizes phase 4.

### 2026-08-08 baseline

- Highest leverage was **compose shell** using existing components + `rest/actions` + `rest/displayformats` (landed).
- REST enhancement: `GET /rest/displayformats?validForFolder=true` (and optional `validForViewsAndSearches`).
- Re-audit after each slice merges; flip Partial → Present only when product route demonstrates the capability.
- **P-Trans (#2411):** inventory under `research/p-trans-api-inventory.md` (PR #2431). Public REST has locale **catalog** only; create locale variant remains SOAP/CX/DCE. Children: inventory #2428, REST façade #2429, Explorer UI #2430.
