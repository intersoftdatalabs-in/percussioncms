# Plan: DCE ↔ Explorer parity

**Parent:** #2400  
**Spec:** [spec.md](./spec.md)  
**Gap matrix:** [contracts/gap-matrix.md](./contracts/gap-matrix.md)

## Phase 0 — Research package (baseline)

| Deliverable | Status |
|-------------|--------|
| `spec.md` / `plan.md` / `gap-matrix.md` | **Done** (package on `main`; matrix refreshed 2026-08-09) |
| Child GH issues for first backlog | **Done** (#2407–#2411 + #2409→#2504–#2507 + #2411→#2428–#2430) |
| Link package + slices on #2400 | **Done** (maintain `## Agent progress` on issue body) |

## Phase 1 — Product shell composition (UI)

Wire existing Explorer panels into `ContentExplorerShell` so `/cm/app/explorer` matches DCE’s primary layout:

| Order | Surface | Existing pieces | Status / notes |
|------:|---------|-----------------|----------------|
| 1.1 | Server action toolbar + context menu | `ActionToolbar`, `ContextMenu`, `actionMenuApi` → `rest/actions` | **Done** — #2407 · [PR #2412](https://github.com/intersoftdatalabs-in/percussioncms/pull/2412) |
| 1.2 | Search drawer/panel | `SearchPanel`, `searchApi` (sitemanage extended search) | **Done** — #2407 · PR #2412 |
| 1.3 | Display format selector + columns | `rest/displayformats`, `DetailList` FR-027 hooks, `pathApi.paginatedFolder(displayFormatId)` | **Done** — #2407 · PR #2412 |
| 1.4 | Folder security side panel | `FolderSecurityPanel`, path folderProperties | **Partial in shell** (toggle landed #2412); polish + properties → **#2410** |
| 1.5 | Clipboard + multi-select | `ClipboardPanel`, selection model | **Done** — #2408 · [PR #2522](https://github.com/intersoftdatalabs-in/percussioncms/pull/2522) |
| 1.6 | Advanced tools | DependencyViewer, RelationshipsView, site/subfolder wizards | **Open** — secondary chrome; no child filed yet (phase 4 priority) |

**Phase 1 exit:** Operator can navigate, act via server menus, search, and change list columns without DCE.  
**Exit status (2026-08-09):** Met for primary chrome (1.1–1.3, 1.5). 1.4 polish and 1.6 remain. Human QA for shell: #2588.

## Phase 2 — REST / path enrichment for list columns

| Gap | Approach | Status |
|-----|----------|--------|
| Folder-valid display format list | `GET /rest/displayformats?validForFolder=true` | **Done** (PR #2412) |
| Column cell data | Use `PSPathItem.displayProperties` / `columnData` when `displayFormatId` set on paginatedFolder | **Done** for shell path (PR #2412) |
| Workflow / modified columns empty | If still empty after format id, extend path list DTO in sitemanage (not invent on client) | Open if QA finds empty columns |
| Saved search **execute** | **Façade** `POST /rest/searches/{idOrName}/execute` (disposition #2504 / [research note](./research/saved-search-execute-disposition.md)); implement #2505 + Explorer #2506 + Playwright #2507 | Disposition **Done** (#2504 / [PR #2579](https://github.com/intersoftdatalabs-in/percussioncms/pull/2579)); implement B–D **open** |
| Translation workflow | Spike existing i18n/item endpoints; new façade only if needed | Inventory **Done** (#2428 / [PR #2431](https://github.com/intersoftdatalabs-in/percussioncms/pull/2431)); REST #2429 + UI #2430 **open** |

## Phase 3 — Action / workflow depth

| Gap | Approach |
|-----|----------|
| Allowed workflow transitions | Complete `rest/actions` allowed-transitions path used by menus |
| Properties dialogs | Folder/item properties parity beyond ACL (#2410) |
| New content menus | Content-type / template menus already partially on `rest/actions` |

## Phase 4 — Advanced / power-user

| Gap | Approach |
|-----|----------|
| Dependency / IA deep tools | Expand beyond summary counts if DCE still ahead; wire into shell |
| Site/subfolder copy | Ensure wizards reachable from shell menus |
| Display format design write | Later; read catalog sufficient for Explorer list |
| DCE menu bar (Content / View / Help) | Optional SPA chrome; toolbar/context covers most actions today |

## Active implementation order (do not re-audit from scratch)

1. **#2410** — folder security polish + properties parity  
2. **#2409** — saved searches: **#2505** REST execute → **#2506** SearchPanel UI → **#2507** Playwright  
3. **#2411** — translation: **#2429** REST façade (if needed) → **#2430** Explorer UI (#2428 inventory already merged)  
4. Phase 4 advanced chrome / menu bar — file children only when prioritized

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
| Large shell PR | Phase 1 ordered slices; 1.1–1.3 and 1.5 landed as separate PRs |
| REST vs sitemanage path APIs | Prefer public `rest` for new contracts; pathmanagement remains for folder CRUD |
| Stale matrix after merges | Refresh `gap-matrix.md` when shell/clipboard/search slices merge (this doc) |
