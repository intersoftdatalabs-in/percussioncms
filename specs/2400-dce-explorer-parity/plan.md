# Plan: DCE ↔ Explorer parity

**Parent:** #2400  
**Spec:** [spec.md](./spec.md)  
**Gap matrix:** [contracts/gap-matrix.md](./contracts/gap-matrix.md)

## Phase 0 — Research package (this PR baseline)

| Deliverable | Status |
|-------------|--------|
| `spec.md` / `plan.md` / `gap-matrix.md` | In progress |
| Child GH issues for first backlog | In progress |
| Link package + slices on #2400 | In progress |

## Phase 1 — Product shell composition (UI)

Wire existing Explorer panels into `ContentExplorerShell` so `/cm/app/explorer` matches DCE’s primary layout:

| Order | Surface | Existing pieces | Notes |
|------:|---------|-----------------|-------|
| 1.1 | Server action toolbar + context menu | `ActionToolbar`, `ContextMenu`, `actionMenuApi` → `rest/actions` | Load on selection change |
| 1.2 | Search drawer/panel | `SearchPanel`, `searchApi` (sitemanage extended search) | Open/reveal → shell navigation |
| 1.3 | Display format selector + columns | `rest/displayformats`, `DetailList` FR-027 hooks, `pathApi.paginatedFolder(displayFormatId)` | Filter folder-valid formats |
| 1.4 | Folder security side panel | `FolderSecurityPanel`, path folderProperties | ADMIN/WRITE gates |
| 1.5 | Clipboard + multi-select | `ClipboardPanel`, selection model | Multi-select list rows |
| 1.6 | Advanced tools | DependencyViewer, RelationshipsView, site/subfolder wizards | Secondary chrome |

**Phase 1 exit:** Operator can navigate, act via server menus, search, and change list columns without DCE.

## Phase 2 — REST / path enrichment for list columns

| Gap | Approach |
|-----|----------|
| Folder-valid display format list | `GET /rest/displayformats?validForFolder=true` (filter on existing catalog) |
| Column cell data | Use `PSPathItem.displayProperties` / `columnData` when `displayFormatId` set on paginatedFolder; map sources in UI |
| Workflow / modified columns empty | If still empty after format id, extend path list DTO in sitemanage (not invent on client) |
| Saved search **execute** | New or extended `rest` search runtime API mapping design `SearchDef` → criteria (slice) |
| Translation workflow | Spike existing i18n/item endpoints; new façade only if needed |

## Phase 3 — Action / workflow depth

| Gap | Approach |
|-----|----------|
| Allowed workflow transitions | Complete `rest/actions` allowed-transitions path used by menus |
| Properties dialogs | Folder/item properties parity beyond ACL |
| New content menus | Content-type / template menus already partially on `rest/actions` |

## Phase 4 — Advanced / power-user

| Gap | Approach |
|-----|----------|
| Dependency / IA deep tools | Expand beyond summary counts if DCE still ahead |
| Site/subfolder copy | Ensure wizards reachable from shell menus |
| Display format design write | Later; read catalog sufficient for Explorer list |

## Test strategy

| Layer | When |
|-------|------|
| Vitest pure helpers + shell composition | Every UI PR |
| Vitest `renderA11yGate` (T082a) + EXPLORER_MSG key shape | Every Explorer UI PR |
| `rest` unit tests (Mockito resource) | Every REST PR |
| Module `mvnw clean install` | Pre-PR hard gate (`rest`, `WebUI`, `sitemanage` as touched) |
| Playwright surface + `expectNoSeriousA11yViolations` (T082b) | Product-visible Explorer changes |

**i18n / 508:** [checklists/i18n-a11y-hard-gate.md](./checklists/i18n-a11y-hard-gate.md) — non-optional for UI work.

## Risk

| Risk | Mitigation |
|------|------------|
| 992 matrix overstates Done | Gap matrix uses **product shell** evidence, not component existence alone |
| Large shell PR | Phase 1 ordered slices; land 1.1–1.3 first |
| REST vs sitemanage path APIs | Prefer public `rest` for new contracts; pathmanagement remains for folder CRUD |
