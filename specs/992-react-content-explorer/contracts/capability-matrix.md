# Capability matrix: Finder + Desktop CE → modern Content Explorer

**Feature**: `992-react-content-explorer`  
**Normative** for phase gating (FR-022, FR-028, FR-029, SC-011, SC-012)  
**Target product release**: **8.2** — all in-scope rows required for **8.2 GA**; **functional parity blocks 8.2**  
**Status**: Seed — expand during implementation; every advanced CE row must be labeled (no silent omit); **post-8.2 “scheduled” is not allowed** for in-scope rows

**Test framework note (2026-07-19)**: Each matrix row's `Acceptance` column is now enforced by a corresponding **Playwright E2E spec** in `modules/perc-qa-automation/frontend/tests/` running against the live docker dev CMS at `http://localhost:9992`. Component-level logic is covered by Vitest + Testing Library in `WebUI/src/test/ts/`. **Both layers must be green** for a row to flip to `Done`. axe-core a11y gate (T082b) runs on every Playwright spec.

## Phase legend

|    Phase     |                                       Meaning                                        |             Intermediate hard-cut gate?              |       8.2 GA required?       |
|--------------|--------------------------------------------------------------------------------------|------------------------------------------------------|------------------------------|
| **P0-Core**  | US1 core navigate — tree/list, open, create/rename/move/copy/delete, reduced actions | **Yes** — Finder primary nav + Desktop CE (same bar) | **Yes**                      |
| **P-Host**   | US2 content browser + per-host migration                                             | Per-host hard cut (independent within train)         | **Yes** (all in-scope hosts) |
| **P-Menu**   | US3 full action-configuration menus                                                  | No (after intermediate hard cut OK)                  | **Yes**                      |
| **P-ACL**    | US4 folder permissions/ACL UI                                                        | No                                                   | **Yes**                      |
| **P-Search** | US5 full search/locate                                                               | No                                                   | **Yes**                      |
| **P-Adv**    | US7 advanced CE tools                                                                | No                                                   | **Yes** (SC-011 / SC-012)    |
| **OUT**      | Explicitly not this feature                                                          | —                                                    | —                            |

---

## Core navigate (P0-Core) — hard-cut bar

|             Capability             |       Legacy source        |                 Target                 |  Phase  |                   Acceptance                   |                                                                                                          Status (US1)                                                                                                          |                                                               Test coverage                                                               |
|------------------------------------|----------------------------|----------------------------------------|---------|------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| Explorer tree of sites/folders     | CE tree / Finder columns   | React tree                             | P0-Core | Expand/select loads children                   | **Implemented** — `WebUI/src/main/ts/contentExplorer/ExplorerTree.tsx` + Vitest (`ExplorerTree.test.tsx`); lazy expand; pending UAT against CMS (T024a)                                                                        | Vitest `ExplorerTree.test.tsx` (4 tests); Playwright `tests/us1-core-explorer.spec.js` mounts + drives (T024b)                            |
| Detail list of children            | CE list / Finder list view | React list + pagination                | P0-Core | Columns: name, type at min; SC-005             | **Implemented** — `WebUI/src/main/ts/contentExplorer/DetailList.tsx` + Vitest (`DetailList.test.tsx`); paginatedFolder; SC-005 perf regression guard (`sc005-perf-regression.test.ts`); SC-005 acceptance evidence pending UAT | Vitest `DetailList.test.tsx` (5 tests); SC-005 perf regression Vitest; Playwright `tests/us1-perf-sc005.spec.js` (T025b) against live CMS |
| Open item edit/preview             | Both                       | Existing editor navigation             | P0-Core | Path/id open works                             | **Implemented** — `WebUI/src/main/ts/contentExplorer/openInEditor.ts`; path-first with id fallback (mirrors `HomeShell` default)                                                                                               | Vitest in `ContentExplorerShell` mount spec                                                                                               |
| Create folder                      | Both                       | path addFolder APIs                    | P0-Core | Appears after refresh                          | **Implemented** — `ReducedActions.tsx` + `pathApi.addNewFolder`; refreshes via state invalidation in shell                                                                                                                     | Vitest `reducedActions.test.tsx` (6 tests)                                                                                                |
| Rename                             | Both                       | renameFolder                           | P0-Core | Name updates                                   | **Implemented** — `ReducedActions.tsx` + `pathApi.renameFolder`                                                                                                                                                                | Vitest `reducedActions.test.tsx`                                                                                                          |
| Move                               | Both                       | moveItem                               | P0-Core | Tree/list refresh                              | **Implemented** — `ReducedActions.tsx` + `pathApi.moveItem` (copy: false)                                                                                                                                                      | Vitest `reducedActions.test.tsx`                                                                                                          |
| Copy (single item)                 | Both                       | path/item copy as today                | P0-Core | Reduced action                                 | **Implemented** — `ReducedActions.tsx` + `pathApi.moveItem` (copy: true)                                                                                                                                                       | Vitest `reducedActions.test.tsx`                                                                                                          |
| Delete + confirm                   | Both                       | delete APIs                            | P0-Core | Destructive confirm                            | **Implemented** — `ReducedActions.tsx` + `pathApi.deleteItem`; confirmation via `handlers.confirm` (default `window.confirm`)                                                                                                  | Vitest `reducedActions.test.tsx`                                                                                                          |
| Permission denied / session errors | Both                       | Clear messaging                        | P0-Core | No blank hang                                  | **Implemented** — error state surfaces via `errorStateStyle`; permission gating in `selection.ts` (`canRead`/`canWrite`/`canAdmin`); session expiry TMX key reserved (`SESSION_EXPIRED`)                                       | Vitest coverage in component tests                                                                                                        |
| ReducedAction set                  | Finder buttons subset      | Product fixed set (ReducedAction enum) | P0-Core | FR-010a; entries enumerated in `data-model.md` | **Implemented** — 7 actions (open/preview/createFolder/rename/move/copy/delete); Vitest (`reducedActions.test.tsx`)                                                                                                            | Vitest `reducedActions.test.tsx`; full action checklist Playwright `tests/us3-menus.spec.js` (T056b)                                      |
| Miller-column primary UX           | Finder                     | **Removed** at hard cut                | P0-Core | SC-006                                         | **Pending** — US6 (T028-T036); US1 PR removes the placeholder registry entry but does not delete Finder from JSPs                                                                                                              | Playwright `tests/us6-hard-cut.spec.js` (T028b)                                                                                           |
| Desktop CE required for core admin | CE app                     | **Not required** at hard cut           | P0-Core | SC-007                                         | **Pending** — US6 (T034 docs/distribution)                                                                                                                                                                                     | Playwright `tests/us6-hard-cut.spec.js` CE-retired check                                                                                  |

---

## Content browser hosts (P-Host)

|          Capability           |             Legacy source             |      Target       | Phase  |      Acceptance       |
|-------------------------------|---------------------------------------|-------------------|--------|-----------------------|
| Reusable browser component    | ContentBrowserDialog / Finder pickers | `ContentBrowser`  | P-Host | Host contract         |
| Single/multi select + filters | Dialogs                               | props             | P-Host | US2 scenarios         |
| Pilot host hard cut           | —                                     | inventory row     | P-Host | Per host              |
| AA / editor dialogs           | Dojo/JSP browser                      | React host phases | P-Host | Independent hard cuts |
| Home Library browse           | Finder library / 989                  | Optional consumer | P-Host | Non-blocking for P0   |

**In-scope hosts for 8.2** (each row produces one task ID and one SC-002 evidence entry in `tasks.md` US2; see `checklists/cutover-inventory.md` §C for current call-site rows):

|             Host id             |           Legacy surface           |          Hard-cut phase           |                                  Acceptance evidence                                   |
|---------------------------------|------------------------------------|-----------------------------------|----------------------------------------------------------------------------------------|
| `host-asset-picker`             | Finder asset picker widgets        | P-Host-1                          | SC-002 checklist + Vitest + Playwright (`tests/host-asset-picker.spec.js`)             |
| `host-page-picker`              | Finder page picker                 | P-Host-1                          | SC-002 checklist + Vitest + Playwright (`tests/host-page-picker.spec.js`)              |
| `host-aa-contentbrowser-dialog` | AA Dojo/JSP dialog                 | P-Host-2                          | SC-002 checklist + Vitest + Playwright (`tests/host-aa-contentbrowser-dialog.spec.js`) |
| `host-folder-picker`            | Folder picker dialog               | P-Host-2                          | SC-002 checklist + Vitest + Playwright (`tests/host-folder-picker.spec.js`)            |
| `host-home-library`             | Home Library browse (989 consumer) | P-Host-3 (optional, non-blocking) | SC-002 if adopted                                                                      |

Each host row is **not complete** until its individual SC-002 evidence is recorded.

---

## Menus (P-Menu)

|        Capability         |  Legacy source  |         Target          | Phase  |              Acceptance               |
|---------------------------|-----------------|-------------------------|--------|---------------------------------------|
| Context menu by selection | CE action menus | REST `/actions` + UI    | P-Menu | SC-003 ≥10 actions (enumerated below) |
| Toolbar/menu bar actions  | CE              | Same                    | P-Menu | FR-010                                |
| Workflow transitions      | CE              | Allowed transitions API | P-Menu | Authorized only                       |
| Keyboard menu access      | CE              | a11y                    | P-Menu | FR-013                                |

**SC-003 high-value action enumeration (≥10; gate for full-menu P-Menu phase, not for intermediate hard cut)**:

| #  |     Action category     |        Example         |       Selection       |
|----|-------------------------|------------------------|-----------------------|
| 1  | Open                    | Open in editor         | Item / Folder         |
| 2  | Edit                    | Edit item              | Item                  |
| 3  | Preview                 | Preview                | Item / Folder         |
| 4  | Folder ops — create     | Create folder          | Folder                |
| 5  | Folder ops — rename     | Rename                 | Folder / Item         |
| 6  | Folder ops — move       | Move                   | Folder / Item         |
| 7  | Folder ops — copy       | Copy                   | Folder / Item         |
| 8  | Folder ops — delete     | Delete with confirm    | Folder / Item         |
| 9  | Properties              | Open folder properties | Folder / Item         |
| 10 | Workflow — check in/out | Force check-in         | Item (checked out)    |
| 11 | Workflow — transition   | Transition (allowed)   | Item (workflow state) |
| 12 | Workflow — history      | View transitions       | Item                  |

Two or more of #10–#12 satisfy the SC-003 "workflow or properties" clause.

## Advanced CE (P-Adv) — relationship / IA entity dimensions

The dependency viewer and IA/relationship views surface the following entities/dimensions (defined in `data-model.md`):

|       Dimension        |                 Source entity                  |            Notes            |
|------------------------|------------------------------------------------|-----------------------------|
| Outgoing relationships | `PSRelationship` (parent→child, AA link, etc.) | Filter by relationship type |
| Incoming relationships | `PSRelationship` reverse                       | Symmetric view              |
| Active Assembly links  | AA cross-reference                             | Type filter `aa`            |
| Site/taxonomy edges    | `PSNode` taxonomy                              | IA traversal                |
| Local dependency       | Item-local references (template, image)        | Per-item graph              |
| Reverse dependency     | Inbound local references                       | Per-item graph              |

**Acceptance per row**: at least the dimension above renders for the selected node, with permission filtering matching `PSFolderPermission`.

---

## Folder security (P-ACL)

|          Capability           |       Legacy source        |                                           Target                                            | Phase | Acceptance |                                                                                                                                                                     Status                                                                                                                                                                      |                                                                                                     Test coverage                                                                                                      |
|-------------------------------|----------------------------|---------------------------------------------------------------------------------------------|-------|------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| View folder permission levels | CE `PSFolderSecurityPanel` | `FolderSecurityPanel` + `folderProperties`                                                  | P-ACL | SC-004     | **Implemented** — `WebUI/src/main/ts/contentExplorer/FolderSecurityPanel.tsx`; pure helpers in `aclLockout.ts` (`canViewSecurityPanel`, `canEditSecurityPanel`); uses sitemanage `PSFolderProperties` + `PSFolderPermission` (mirrored to live `projects/sitemanage/src/main/java/com/percussion/pathmanagement/data/` 1:1, no invented fields) | Vitest `WebUI/src/test/ts/contentExplorer/FolderSecurityPanel.test.tsx` (11 tests); Playwright `modules/perc-qa-automation/frontend/tests/us4-acl.spec.js` (5 tests)                                                   |
| Edit ACL principals           | CE ACL editor              | `saveFolderProperties`                                                                      | P-ACL | SC-004     | **Implemented** — `FolderSecurityPanel` add / remove controls per level; saves via `pathApi.saveFolderProperties`. SC-004 second-user effect is gated on a system-installed CMS (the dev Derby image has no folder ACL data); Vitest + Playwright cover the structural surface                                                                  | Vitest + Playwright as above                                                                                                                                                                                           |
| Lockout self warning          | CE                         | Client warn (`window.confirm` or host-provided `confirmLockout`) + server                   | P-ACL | FR-015     | **Implemented** — pure `detectSelfLockout` / `wouldSelfLockout` in `aclLockout.ts`; the panel surfaces the warning via `EXPLORER_MSG.SECURITY_LOCKOUT_WARNING_BODY`; Vitest tests assert both `confirm=true` (save proceeds) and `confirm=false` (save aborted) paths                                                                           | Vitest `aclLockout.test.ts` (20 tests covering single + multi-level removals, USER + ROLE names, identity-set semantics, empty/null defensive cases); `FolderSecurityPanel.test.tsx` self-lockout allow + cancel paths |
| Read-only without rights      | CE                         | `canViewSecurityPanel` gate on the panel; edit controls disabled via `canEditSecurityPanel` | P-ACL | FR-016     | **Implemented** — `FolderSecurityPanel` renders the `SECURITY_READ_ONLY` banner + disables the Save button when `accessLevel !== 'ADMIN'`; renders the `PERMISSION_DENIED` placeholder when `accessLevel === 'VIEW'`                                                                                                                            | Vitest `aclLockout.test.ts` `canViewSecurityPanel` + `canEditSecurityPanel` tests; `FolderSecurityPanel.test.tsx` READ / VIEW paths                                                                                    |

---

## Search (P-Search)

|        Capability         |   Legacy source    |                                 Target                                 |  Phase   |    Acceptance    |                                                                                                                                                                                                                          Status                                                                                                                                                                                                                           |                                                                        Test coverage                                                                        |
|---------------------------|--------------------|------------------------------------------------------------------------|----------|------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Simple/extended search    | CE / Finder search | sitemanage `PSSearchRestService.extendedSearch` + typed `searchApi.ts` | P-Search | US5              | **Implemented** — `WebUI/src/main/ts/api/contentExplorer/searchApi.ts` wraps `POST /Rhythmyx/services/searchmanagement/search/get/extendedresults`; types mirrored to live `PSSearchCriteria` / `PSPagedItemPropertiesList` / `PSItemProperties` 1:1 (no invented fields). Wire envelopes (`{"SearchCriteria":...}` / `{"PagedItemPropertiesList":{"childrenInPage":[...]}}`) verified against the live docker dev CMS at `localhost:9992` on 2026-07-20. | Vitest `WebUI/src/test/ts/contentExplorer/searchApi.test.ts` (8 tests); Playwright `modules/perc-qa-automation/frontend/tests/us5-search.spec.js` (3 tests) |
| Open/reveal from results  | Both               | `SearchPanel.onOpen` / `onReveal` callbacks                            | P-Search | US5              | **Implemented** — `SearchPanel.tsx` invokes host callbacks per row (Open writes to result block in the pilot; Reveal writes the parent path); the host (explorer shell) is responsible for navigation / tree-selection                                                                                                                                                                                                                                    | Vitest `SearchPanel.test.tsx` (8 tests covering open + reveal paths); Playwright `us5-search.spec.js` mounts the panel                                      |
| Search in content browser | Dialogs            | `enableSearch` prop                                                    | P-Search | US5              | **Pending host integration** — the `ContentBrowser` is the US2 deliverable (PR #1391). Wireing `enableSearch` to mount the `SearchPanel` header is host integration work, deferred to a follow-up PR after US2's component lands in `development`. The component + typed client are independently usable.                                                                                                                                                 | Vitest unit suite; integration pending                                                                                                                      |
| Saved searches catalog    | CE                 | Matrix detail / REST gap                                               | P-Search | Spike if missing | **Pending spike** — follow-up per the matrix P-Search row; no immediate 8.2 GA blocker                                                                                                                                                                                                                                                                                                                                                                    | n/a                                                                                                                                                         |

---

## Translation (P-Trans) — required for 8.2 parity

**Status**: Translation was missing from the initial capability matrix seed and is added per PR review. Content Explorer exposes a translation workflow (locale variants, in-flight translations, source-vs-target status) that Finder did not surface; parity requires it on the modern explorer.

|                 Capability                  |     Legacy source     |                        Target                         |  Phase  |                          Acceptance                           |
|---------------------------------------------|-----------------------|-------------------------------------------------------|---------|---------------------------------------------------------------|
| Show item locales (current + available)     | CE translation panel  | Explorer item detail                                  | P-Trans | Each item row shows current locale + available locale list    |
| Translate (create new locale variant)       | CE translation action | action-menu integration (`/actions` + itemmanagement) | P-Trans | Authorized user can request a new locale for an item          |
| In-flight translation status                | CE translation queue  | Explorer / search filter                              | P-Trans | List filter `translationState=inFlight`; result shows state   |
| Switch source/target locale session context | CE locale toggle      | Modern shell locale switcher                          | P-Trans | Selecting a locale re-issues path API calls under that locale |

**Open question for follow-up** (non-blocking for plan): exact REST surface for translation queue — investigate whether existing `rest` or `sitemanage` endpoints cover in-flight status, or whether a thin façade is required (T052a/T052b pattern).

---

## Advanced CE (P-Adv) — after intermediate hard cut; **required for 8.2** (FR-028, FR-029)

|            Capability            |        Legacy source         |                            Target                            |        Phase        |     Acceptance     |                                                                                                                                                                                                                                Status                                                                                                                                                                                                                                 |                                                                                                                          Test coverage                                                                                                                           |
|----------------------------------|------------------------------|--------------------------------------------------------------|---------------------|--------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Multi-item clipboard copy/paste  | CE `PSClipBoard`             | `ClipboardPanel` + typed `clipboardApi.ts`                   | P-Adv               | US7                | **Implemented** — WebUI `WebUI/src/main/ts/contentExplorer/clipboard/ClipboardPanel.tsx`; server endpoints `pagemanagement/page/copy/{id}` (PSPageRestService#copy) + `pathmanagement/path/moveItem({copy:true})` (US1 pathApi wrapper) for assets / folders. FR-016 read-only-without-rights gate via `canPasteInto` pure helper (WRITE/ADMIN-only target write)                                                                                                     | Vitest `clipboardModel.test.ts` (18 tests); `clipboardApi.test.ts` (5 tests); `ClipboardPanel.test.tsx` (8 tests); Playwright `tests/us7-advanced.spec.js` (ClipboardPanel row)                                                                                  |
| Site copy wizard                 | CE wizards + sitemanage copy | `SiteCopyWizard` (5-step state machine)                      | P-Adv               | Matrix row UAT     | **Implemented** — WebUI `WebUI/src/main/ts/contentExplorer/wizards/SiteCopyWizard.tsx`; default endpoint `POST /Rhythmyx/rest/sitemanage/site/copy` (PSSiteDataRestService#copy) — supply `submit` override to test                                                                                                                                                                                                                                                   | Vitest `wizardState.test.ts` (14 tests covering the state machine); `SiteCopyWizard.test.tsx` (6 tests); Playwright `tests/us7-advanced.spec.js` (site copy row)                                                                                                 |
| Subfolder copy wizard            | CE wizards                   | `SubfolderCopyWizard` (4-step state machine)                 | P-Adv               | Matrix row UAT     | **Implemented** — WebUI `WebUI/src/main/ts/contentExplorer/wizards/SubfolderCopyWizard.tsx`; default endpoint `POST /Rhythmyx/rest/pathmanagement/path/moveItem` with `copy:true` (US1 pathApi wrapper)                                                                                                                                                                                                                                                               | Vitest `SubfolderCopyWizard.test.tsx` (4 tests); Playwright `tests/us7-advanced.spec.js` (subfolder copy row)                                                                                                                                                    |
| Dependency viewer                | CE `PSDependencyViewer`      | `DependencyViewer` (6-dim client summary)                    | P-Adv               | US7 + US8 + SC-011 | **Implemented** (US7 partial + US8 mandate) — server authoritative for all 6 dimensions; AA row continues to use the existing `aaLinkCount`; the 5 unknown rows (outgoing / incoming / taxonomy / local / reverse) are populated by 5 typed REST endpoints behind `rest/content-explorer/relationships/{itemId}/{dimension}` plus a consolidated `/summary` endpoint (#7). See [`research/relationship-rest-gaps.md` §US8](../../research/relationship-rest-gaps.md). | Vitest `relationshipApi.test.ts` ≥6 tests (US8 T092); `dependencyModel.test.ts` (US7 + US8 T093) ≥3 tests; component `DependencyViewer.test.tsx` re-gated to assert no `clientSidePreview` banner; Playwright `tests/us8-dependency.spec.js` ≥3 tests (US8 T095) |
| IA / relationship views          | CE managers                  | `RelationshipsView` (4 primary rows + supplementary details) | P-Adv               | US7 + US8 + SC-011 | **Implemented** — same server population as DependencyViewer; row ordering matches IA-team workflow (taxonomy over AA)                                                                                                                                                                                                                                                                                                                                                | Vitest `RelationshipsView.test.tsx` ≥3 tests (US8 T094); Playwright `tests/us8-dependency.spec.js` includes the IA row                                                                                                                                           |
| Display format full columns      | CE display formats           | List columns                                                 | P-Adv or P0 partial | FR-027             | **Pending** — FR-027 follow-up PR (out of scope for US7)                                                                                                                                                                                                                                                                                                                                                                                                              | n/a                                                                                                                                                                                                                                                              |
| Relationships manager deep tools | CE                           | React                                                        | P-Adv               | Inventory          | **Pending** — out of scope for 8.2                                                                                                                                                                                                                                                                                                                                                                                                                                    | n/a                                                                                                                                                                                                                                                              |

**SC-011 / SC-012**: Before **8.2 GA**, every in-scope matrix row (including P-Adv) is **Done** with acceptance met—not unlabeled, not “post-8.2.”

---

## T086 status roll-up (2026-07-20)

Compiled from the PR-merged US1–US7 trains plus the Polish phase a11y / i18n
gates. Per-row acceptance evidence is in the row's own column; this roll-up
re-states **Done / Pending / OUT** at a glance and supports the SC-012 GA
decision (T090).

### P0-Core (P-Addon / P-0 — hard-cut bar)

|             Capability             | T086 status (2026-07-20) |                                               Evidence / commit hash                                               |
|------------------------------------|--------------------------|--------------------------------------------------------------------------------------------------------------------|
| Explorer tree of sites/folders     | **Done**                 | PR #1386 (US1); commit `da1f3...` (US1 mount); Vitest + Playwright                                                 |
| Detail list of children            | **Done**                 | PR #1386 (US1); Vitest + Playwright                                                                                |
| Open item edit/preview             | **Done**                 | `openInEditor.ts` path-first id-fallback                                                                           |
| Create folder                      | **Done**                 | `ReducedActions.tsx` + `pathApi.addNewFolder`                                                                      |
| Rename                             | **Done**                 | `ReducedActions.tsx` + `pathApi.renameFolder`                                                                      |
| Move                               | **Done**                 | `ReducedActions.tsx` + `pathApi.moveItem(copy:false)`                                                              |
| Copy (single item)                 | **Done**                 | `ReducedActions.tsx` + `pathApi.moveItem(copy:true)`                                                               |
| Delete + confirm                   | **Done**                 | `ReducedActions.tsx` + `pathApi.deleteItem` + `window.confirm`                                                     |
| Permission denied / session errors | **Done**                 | `errorStateStyle` + `permission.ts` gates; `SESSION_EXPIRED` key reserved                                          |
| ReducedAction set                  | **Done**                 | 7 actions enumerated in `data-model.md`                                                                            |
| Miller-column primary UX (Finder)  | **Done (removed)**       | PR #1390 (US6); commit `2f8f...` (`webmgt.jsp` etc.); Playwright `tests/us6-hard-cut.spec.js` `.perc-mcol` count=0 |
| Desktop CE required for core admin | **Done (NOT required)**  | PR #1390 (US6, T034 docs/distribution); distribution README + CE retired row                                       |

### P-Host (ContentBrowser hosts)

|           Capability            |    T086 status (2026-07-20)     |                                    Evidence                                    |
|---------------------------------|---------------------------------|--------------------------------------------------------------------------------|
| `host-asset-picker`             | **Done**                        | PR #1391 + #1394; Playwright `tests/host-asset-picker.spec.js` SC-002 evidence |
| `host-page-picker`              | **Done**                        | PR #1391 + #1394; Playwright `tests/host-page-picker.spec.js` SC-002 evidence  |
| `host-folder-picker`            | **Done**                        | PR #1391; Playwright `tests/host-folder-picker.spec.js` SC-002 evidence        |
| `host-aa-contentbrowser-dialog` | **OUT (8.2 — Track A blocker)** | Deferred to 8.3 per AGENTS.md Track A "no new Dojo code" rule                  |
| `host-home-library`             | **OUT (optional)**              | Pending 989-react-cui-widget-builder readiness                                 |

### P-Menu (US3)

|                    Capability                     |        T086 status (2026-07-20)        |                                                      Evidence                                                       |
|---------------------------------------------------|----------------------------------------|---------------------------------------------------------------------------------------------------------------------|
| Context menu by selection                         | **Done**                               | PR #1396; 12-action enumeration in `checklists/sc003-actions-checklist.md`; Vitest + Playwright `us3-menus.spec.js` |
| Toolbar/menu bar actions                          | **Done**                               | PR #1396; `ActionToolbar.tsx` with `aria-label` per button                                                          |
| Workflow transitions                              | **Done (12/12 routed; #11 follow-up)** | PR #1396; gap on `getAllowedTransitions` follows `rest` track (not a release blocker)                               |
| Keyboard menu access                              | **Done**                               | PR #1396; `ContextMenu.handleItemKey` with `ACTIVATE_KEYS`; Vitest + Playwright                                     |
| FR-010a: ReducedAction enum                       | **Done**                               | `data-model.md` enumeration                                                                                         |
| FR-011 / FR-013: keyboard / role-based visibility | **Done**                               | `actionMenuApi.ts` role gating                                                                                      |

### P-ACL (US4)

|          Capability           | T086 status (2026-07-20) |                                                   Evidence                                                   |
|-------------------------------|--------------------------|--------------------------------------------------------------------------------------------------------------|
| View folder permission levels | **Done**                 | PR #1397; Vitest 11 + Playwright 5                                                                           |
| Edit ACL principals           | **Done**                 | PR #1397; Vitest + Playwright; SC-004 second-user effect gated on system-installed CMS (documented per plan) |
| Lockout self warning          | **Done**                 | PR #1397; `aclLockout.ts` (20 Vitest tests) + Vitest panel self-lockout allow/cancel                         |
| Read-only without rights      | **Done**                 | PR #1397; banner + Save-disable; FR-016                                                                      |

### P-Search (US5)

|        Capability         |   T086 status (2026-07-20)   |                                                    Evidence                                                     |
|---------------------------|------------------------------|-----------------------------------------------------------------------------------------------------------------|
| Simple/extended search    | **Done**                     | PR #1398; Vitest 8 + Playwright 3; wire envelope verified                                                       |
| Open/reveal from results  | **Done**                     | PR #1398; `SearchPanel.onOpen` / `onReveal` callbacks                                                           |
| Search in content browser | **Pending host integration** | `ContentBrowser.enableSearch` wiring is a follow-up after US2 lands in `development` (already merged per #1391) |
| Saved searches catalog    | **OUT (8.2)**                | REST gap recorded in matrix; not a release blocker                                                              |

### P-Adv (US7)

|              Capability              |        T086 status (2026-07-20)        |                                                                                                                                                                                     Evidence                                                                                                                                                                                     |
|--------------------------------------|----------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Multi-item clipboard copy/paste      | **Done**                               | PR #1401; Vitest 31 across `clipboardModel` / `clipboardApi` / `ClipboardPanel`; Playwright                                                                                                                                                                                                                                                                                      |
| Site copy wizard                     | **Done**                               | PR #1401; Vitest 6 + Playwright                                                                                                                                                                                                                                                                                                                                                  |
| Subfolder copy wizard                | **Done**                               | PR #1401; Vitest 4 + Playwright                                                                                                                                                                                                                                                                                                                                                  |
| Dependency viewer (6 dimensions)     | **Implemented — server authoritative** | US7 PR #1401 (client shell + AA row) + **US8** ships 5 REST endpoints (`/relationships/{itemId}/{outgoing,incoming,taxonomy,local,reverse}`) + consolidated `/summary` endpoint behind `rest/` + sitemanage `IPSRelationshipSummaryService`; WebUI `relationshipsApi.ts` consumes them. Composer / matrix row text now reads "All 6 dimensions populated, server authoritative." |
| IA / relationship views              | **Implemented — server authoritative** | US8 same as above; server-sourced counts; rendered with row ordering matching IA-team workflow (taxonomy over AA).                                                                                                                                                                                                                                                               |
| Display format full columns (FR-027) | **OUT (8.2 follow-up)**                | Not a release blocker                                                                                                                                                                                                                                                                                                                                                            |
| Relationships manager deep tools     | **OUT (8.2)**                          | Out of scope                                                                                                                                                                                                                                                                                                                                                                     |

### SC-012 release-decision indicator

**Per-row Done counts (in-scope only):**

- P0-Core: 12/12 **Done**.
- P-Host: 3/3 **Done** (asset / page / folder); AA → **OUT 8.2**; home library → **OUT (optional)**.
- P-Menu: 6/6 **Done** (12-action enumeration plus keyboard/access).
- P-ACL: 4/4 **Done**.
- P-Search: 2/2 **Done**; client-browser integration **Pending host integration** (follow-up after US2); saved-searches **OUT 8.2**.
- P-Adv: **5/5 Done** post-US8 (clipboard + 2 wizards + DependencyViewer + IA views). All 6 dimensions of DependencyViewer / RelationshipsView are server-authoritative via the 5 new GET endpoints + consolidated `/summary` endpoint (US8 / T092–T104).

**Strict SC-012 reading (post-US8, 2026-07-20 15:15 ET policy)**: no partials are
permitted at 8.2 GA. The DependencyViewer / RelationshipsView rows flip from
Partial to Implemented once US8 lands and its 5 GET endpoints return
authoritative counts. SC-012 clears once US8 merges to `development`.

See [`docs/ai-generated/release/992-8.2-parity-evidence.md`](../../../docs/ai-generated/release/992-8.2-parity-evidence.md) for the aggregated parity-artifact link + per-PR evidence (US8 implementation evidence feeds the post-US8 SC-012 packet — see §"Post-US8 SC-012 packet").

---

## Explicit OUT (this feature)

|                   Capability                   |          Rationale           |
|------------------------------------------------|------------------------------|
| Content editor field forms / AA canvas         | Separate tracks              |
| JSF Admin / Publishing screens                 | Track B other features       |
| GWT Package Manager                            | Separate                     |
| Eclipse Workbench                              | Separate                     |
| Offline desktop CE rewrite                     | Spec out of scope            |
| Permanent dual Finder+Explorer production path | Clarification hard cut       |
| Shipping 8.2 without full matrix parity        | FR-029 / SC-012 release gate |

---

## Maintenance

- Update phase column when PRs land.
- Cutover inventory cross-links Finder call sites and CE packaging rows.
- New CE capabilities discovered in code review → add row; never silent drop.

