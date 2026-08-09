# Gap matrix: Desktop Content Explorer → SPA Explorer

**Parent:** #2400  
**Evidence date:** 2026-08-08  
**Rule:** Status reflects **product route** (`ExplorerRoute` → `ContentExplorerShell`), not isolated components in the registry.

Legend: **Present** | **Partial** | **Missing** | **OUT**

## Navigation & chrome

| Capability | DCE source | Explorer / REST today | Status | Slice |
|------------|------------|------------------------|--------|-------|
| Sites/folders tree | `PSNavigationTree` | `ExplorerTree` + path APIs | Present | — |
| Detail list of children | Main list | `DetailList` + `paginatedFolder` | Present | — |
| Full product shell composition | Frame + panels | Shell composes search, menus, DF, security (#2407) | **Partial→Present (in PR)** | #2407 |
| Menu bar (Content / View / Help) | `ContentExplorerMenu.xml` | Not in SPA shell | **Missing** | #2407 follow-up |
| Server-driven toolbar/context menus | Action manager + server menus | Wired in shell (#2407) | **Present (in PR)** | #2407 |
| Reduced create/rename/move/copy/delete | Folder actions | `ReducedActions` in shell | Present | — |
| Open / preview item | Open handlers | `openInEditor`; preview default no-op | **Partial** | preview slice |
| Multi-select list | Selection model | Single-select only in shell | **Missing** | #2408 |
| Display formats for columns | Display format catalog | REST filter + shell selector + displayProperties (#2407) | **Partial→Present (in PR)** | #2407 |
| View options / refresh | View menu | Partial (list reload on path change) | **Partial** | shell |

## Search

| Capability | DCE source | Explorer / REST today | Status | Slice |
|------------|------------|------------------------|--------|-------|
| Simple / extended search | `PSSearchDialog` | Search panel toggle in product shell (#2407) | **Present (in PR)** | #2407 |
| Open / reveal from results | Search results | Callbacks on panel | Present | #2407 |
| Saved searches catalog + run | CE saved search | `GET /rest/searches` design catalog; **no execute-in-Explorer UX**. Disposition: **façade** (`POST /rest/searches/{idOrName}/execute`) — see [saved-search-execute-disposition.md](../research/saved-search-execute-disposition.md) (#2504); implement #2505–#2507 | **Missing** | #2409 |
| Search in ContentBrowser hosts | Dialogs | Host integration pending | **Partial** | host follow-up |

## Security & properties

| Capability | DCE source | Explorer / REST today | Status | Slice |
|------------|------------|------------------------|--------|-------|
| Folder ACL view/edit | `PSFolderSecurityPanel` | Panel toggle in shell; identities polish remaining | **Partial** | #2410 |
| Object ACL editor (full) | CE ACL dialogs | Partial other epics (#2274 family) | **Partial** | link existing |
| Folder properties (community, locale, DF, workflow) | `PSFolder*Panel` | path `folderProperties` partial UI | **Partial** | properties slice |

## Advanced / power user

| Capability | DCE source | Explorer / REST today | Status | Slice |
|------------|------------|------------------------|--------|-------|
| Clipboard cut/copy/paste multi | `PSClipBoard` | `ClipboardPanel` + APIs; **not in shell** | **Partial** | #2408 |
| Site copy wizard | CE wizards | `SiteCopyWizard`; not in shell | **Partial** | advanced chrome |
| Subfolder copy wizard | CE wizards | `SubfolderCopyWizard`; not in shell | **Partial** | advanced chrome |
| Dependency viewer | `PSDependencyViewer` | Components + `rest` relationship summary; not in shell | **Partial** | advanced chrome |
| IA / relationships view | Managers | `RelationshipsView`; not in shell | **Partial** | advanced chrome |
| Translation workflow | CE translation | Catalog Present (`/rest/locales`); create-variant **Legacy-only** (SOAP/CX); item variants + in-flight **Missing** — see [p-trans-api-inventory.md](../research/p-trans-api-inventory.md) | **Missing** | #2411 → #2428 inventory, #2429 REST, #2430 UI |
| Workflow transitions in menus | CE workflow | Actions path partial | **Partial** | workflow actions |

## Explicit OUT (for now)

| Capability | Reason |
|------------|--------|
| JavaFX desktop window / native file chooser parity | Platform desktop; web redesign for file ops |
| DCE help JavaHelp topics 1:1 | Product help site / in-app help later |
| DCE-only packaging residual | Decommission program after parity |

## Implementation notes (2026-08-08)

- Highest leverage: **compose shell** (#2401) using existing components + `rest/actions` + `rest/displayformats`.
- REST enhancement for #2401: `GET /rest/displayformats?validForFolder=true` (and optional `validForViewsAndSearches`).
- Re-audit after each slice merges; flip Partial → Present only when product route demonstrates the capability.
- **P-Trans (#2411):** overnight inventory under `research/p-trans-api-inventory.md`. Public REST has locale **catalog** only; create locale variant remains SOAP/CX/DCE (`NewTranslations`, `sys_CreateTranslations`, `ACTION_PASTE_NEW_TRNSL`). Children: inventory #2428, REST façade #2429, Explorer UI #2430.
