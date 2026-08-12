# Plan: DCE ↔ Explorer parity

**Parent:** #2400  
**Spec:** [spec.md](./spec.md)  
**Gap matrix:** [contracts/gap-matrix.md](./contracts/gap-matrix.md)

## Phase 0 — Research package (baseline)

| Deliverable | Status |
|-------------|--------|
| `spec.md` / `plan.md` / `gap-matrix.md` | **Done** (package on `main`; matrix refreshed 2026-08-11: Views + Inbox **Missing** #3108 / #3102; #3109 false-Present→Partial vs open Failed QA; prior #2829 P-Trans OUT) |
| Child GH issues for first backlog | **Done** (#2407–#2411 + #2409→#2504–#2507 + #2411→#2428–#2430) |
| Link package + slices on #2400 | **Done** (maintain `## Agent progress` on issue body) |

## Phase 1 — Product shell composition (UI)

Wire existing Explorer panels into `ContentExplorerShell` so `/cm/app/explorer` matches DCE’s primary layout:

| Order | Surface | Existing pieces | Status / notes |
|------:|---------|-----------------|----------------|
| 1.1 | Server action toolbar + context menu | `ActionToolbar`, `ContextMenu`, `actionMenuApi` → `rest/actions` | **Done** — #2407 · [PR #2412](https://github.com/intersoftdatalabs-in/percussioncms/pull/2412) |
| 1.2 | Search drawer/panel | `SearchPanel`, `searchApi` (sitemanage extended search) | **Done** — #2407 · PR #2412 |
| 1.3 | Display format selector + columns | `rest/displayformats`, `DetailList` FR-027 hooks, `pathApi.paginatedFolder(displayFormatId)` | **Done** — #2407 · PR #2412 |
| 1.4 | Folder security side panel | `FolderSecurityPanel`, path folderProperties | **Done** — #2410 · [PR #2599](https://github.com/intersoftdatalabs-in/percussioncms/pull/2599); human QA #2600 |
| 1.5 | Clipboard + multi-select | `ClipboardPanel`, selection model | **Done** — #2408 · [PR #2522](https://github.com/intersoftdatalabs-in/percussioncms/pull/2522) |
| 1.6 | Advanced tools | DependencyViewer, RelationshipsView, site/subfolder wizards | **Done** — #2768 / #2769 / #2767 / #2792 shell chrome Present |

**Phase 1 exit:** Operator can navigate, act via server menus, search, and change list columns without DCE.  
**Exit status (2026-08-10):** Met for primary + advanced chrome slices above. Remaining open work is mostly **human QA** (see #2400 Agent progress).

## Phase 2 — REST / path enrichment for list columns

| Gap | Approach | Status |
|-----|----------|--------|
| Folder-valid display format list | `GET /rest/displayformats?validForFolder=true` | **Done** (PR #2412) |
| Column cell data | Use `PSPathItem.displayProperties` / `columnData` when `displayFormatId` set on paginatedFolder | **Done** for shell path (PR #2412) |
| Workflow / modified columns empty | If still empty after format id, extend path list DTO in sitemanage (not invent on client) | Open if QA finds empty columns |
| Saved search **execute** | **Façade** `POST /rest/searches/{idOrName}/execute` (disposition #2504 / [research note](./research/saved-search-execute-disposition.md)); implement #2505 + Explorer #2506 + Playwright #2507 | **Done** A–D (#2504–#2507); matrix **Partial** until human QA passes (#3109) — QA #2607 Failed / #2645 open / #2729 Failed |
| Translation workflow | Spike existing i18n/item endpoints; new façade only if needed | Inventory **Done** (#2428); REST **Done** (#2429 / [PR #2601](https://github.com/intersoftdatalabs-in/percussioncms/pull/2601)); Explorer UI **Done** (#2430 / [PR #2648](https://github.com/intersoftdatalabs-in/percussioncms/pull/2648)); in-flight + session **OUT** signed (#2829 / [p-trans-out-disposition.md](./research/p-trans-out-disposition.md)); human QA #2649 |

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

**Agent implement first-wave under #2400 is largely complete** (as of 2026-08-10). Prefer **human QA** and **p1 hierarchy bugs** over inventing new chrome slices — **except** product-IN **Missing** rows with an IA map (product IN/OUT first; do not invent OUT).

1. **#3102 / #3109** — gap matrix no longer claims **Present** for shell/DF/search, menu, toolbar, saved search, Sites tree while QA is Failed/open; see [false-present-qa-reconcile.md](./research/false-present-qa-reconcile.md)  
2. **#2989** (and related folder QA) — Sites/hierarchy p1 before new chrome features  
3. **#2411 / #2829** — translation: locales+create **Present**; in-flight/session **OUT** (docs #2829); **no queue implement** without product re-open; human QA **#2649**  
4. **Human QA handoffs** on #2400 Agent progress (shell, menus, wizards, search, ACL peers) — not agent re-implement  
5. **Object ACL** remains **Partial** on gap-matrix → existing ACL epics (#2274 family), not new #2400 chrome spam  
6. **Views + Inbox (operator reality-check #3102)** — matrix **Missing** until product IN/OUT/REDESIGN (#3108; [views-inbox-missing-disposition.md](./research/views-inbox-missing-disposition.md)). **Do not implement SPA Views/Inbox without product IN.** Research IA/API map + child backlog: [research/views-inbox-ia-api-map.md](./research/views-inbox-ia-api-map.md) (#3110). If product **IN**: V1 [#3115](https://github.com/intersoftdatalabs-in/percussioncms/issues/3115) execute façade → V2 [#3116](https://github.com/intersoftdatalabs-in/percussioncms/issues/3116) Explorer tree/run → V3 [#3117](https://github.com/intersoftdatalabs-in/percussioncms/issues/3117) Playwright; Inbox [#3118](https://github.com/intersoftdatalabs-in/percussioncms/issues/3118) custom-URL or signed OUT.  
7. Phase 4 / redesign only when product prioritizes (including any reopened P-Trans OUT row)

### Views / Inbox planning (2026-08-11)

| Doc | Role |
|-----|------|
| [views-inbox-missing-disposition.md](./research/views-inbox-missing-disposition.md) | Why matrix **Missing** (#3108) |
| [views-inbox-ia-api-map.md](./research/views-inbox-ia-api-map.md) | DCE tree, REST inventory, SPA IA, execute disposition, child issues (#3110) |
| Gap matrix | Rows stay **Missing** until product IN / OUT / REDESIGN — no Present from Developer catalog or View menu chrome |

### Epic #2400 remaining-open criteria (research program)

Documented in [p-trans-out-disposition.md](./research/p-trans-out-disposition.md) § “Epic #2400 remaining-open criteria” (updated 2026-08-11 for Views/Inbox):

- No **silent** omissions; every known DCE capability is Present / Partial / Missing / Explicit OUT (or cross-epic pointer)  
- **Missing** rows (Views, Inbox as of #3108) require product IN/OUT/REDESIGN before implement; not agent invent  
- No required open agent implement children for Present rows  
- Remaining work = human QA, product OUT/redesign decisions, and any product-IN implement children  
- Do not keep epic open solely to track signed OUT rows after QA disposition  

**Current state:** epic stays **open** for the open **human QA** set, ACL cross-epic Partial pointer, and **Missing** Views/Inbox until product disposition (#3102 family).

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
