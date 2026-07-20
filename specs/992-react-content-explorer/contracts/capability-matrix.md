# Capability matrix: Finder + Desktop CE → modern Content Explorer

**Feature**: `992-react-content-explorer`  
**Normative** for phase gating (FR-022, FR-028, FR-029, SC-011, SC-012)  
**Target product release**: **8.2** — all in-scope rows required for **8.2 GA**; **functional parity blocks 8.2**  
**Status**: Seed — expand during implementation; every advanced CE row must be labeled (no silent omit); **post-8.2 “scheduled” is not allowed** for in-scope rows

**Test framework note (2026-07-19)**: Each matrix row's `Acceptance` column is now enforced by a corresponding **Playwright E2E spec** in `modules/perc-qa-automation/frontend/tests/` running against the live docker dev CMS at `http://localhost:9992`. Component-level logic is covered by Vitest + Testing Library in `WebUI/src/test/ts/`. **Both layers must be green** for a row to flip to `Done`. axe-core a11y gate (T082b) runs on every Playwright spec.

## Phase legend

| Phase | Meaning | Intermediate hard-cut gate? | 8.2 GA required? |
|-------|---------|----------------------------|------------------|
| **P0-Core** | US1 core navigate — tree/list, open, create/rename/move/copy/delete, reduced actions | **Yes** — Finder primary nav + Desktop CE (same bar) | **Yes** |
| **P-Host** | US2 content browser + per-host migration | Per-host hard cut (independent within train) | **Yes** (all in-scope hosts) |
| **P-Menu** | US3 full action-configuration menus | No (after intermediate hard cut OK) | **Yes** |
| **P-ACL** | US4 folder permissions/ACL UI | No | **Yes** |
| **P-Search** | US5 full search/locate | No | **Yes** |
| **P-Adv** | US7 advanced CE tools | No | **Yes** (SC-011 / SC-012) |
| **OUT** | Explicitly not this feature | — | — |

---

## Core navigate (P0-Core) — hard-cut bar

| Capability | Legacy source | Target | Phase | Acceptance | Status (US1) | Test coverage |
|------------|---------------|--------|-------|------------|--------------|--------------|
| Explorer tree of sites/folders | CE tree / Finder columns | React tree | P0-Core | Expand/select loads children | **Implemented** — `WebUI/src/main/ts/contentExplorer/ExplorerTree.tsx` + Vitest (`ExplorerTree.test.tsx`); lazy expand; pending UAT against CMS (T024a) | Vitest `ExplorerTree.test.tsx` (4 tests); Playwright `tests/us1-core-explorer.spec.js` mounts + drives (T024b) |
| Detail list of children | CE list / Finder list view | React list + pagination | P0-Core | Columns: name, type at min; SC-005 | **Implemented** — `WebUI/src/main/ts/contentExplorer/DetailList.tsx` + Vitest (`DetailList.test.tsx`); paginatedFolder; SC-005 perf regression guard (`sc005-perf-regression.test.ts`); SC-005 acceptance evidence pending UAT | Vitest `DetailList.test.tsx` (5 tests); SC-005 perf regression Vitest; Playwright `tests/us1-perf-sc005.spec.js` (T025b) against live CMS |
| Open item edit/preview | Both | Existing editor navigation | P0-Core | Path/id open works | **Implemented** — `WebUI/src/main/ts/contentExplorer/openInEditor.ts`; path-first with id fallback (mirrors `HomeShell` default) | Vitest in `ContentExplorerShell` mount spec |
| Create folder | Both | path addFolder APIs | P0-Core | Appears after refresh | **Implemented** — `ReducedActions.tsx` + `pathApi.addNewFolder`; refreshes via state invalidation in shell | Vitest `reducedActions.test.tsx` (6 tests) |
| Rename | Both | renameFolder | P0-Core | Name updates | **Implemented** — `ReducedActions.tsx` + `pathApi.renameFolder` | Vitest `reducedActions.test.tsx` |
| Move | Both | moveItem | P0-Core | Tree/list refresh | **Implemented** — `ReducedActions.tsx` + `pathApi.moveItem` (copy: false) | Vitest `reducedActions.test.tsx` |
| Copy (single item) | Both | path/item copy as today | P0-Core | Reduced action | **Implemented** — `ReducedActions.tsx` + `pathApi.moveItem` (copy: true) | Vitest `reducedActions.test.tsx` |
| Delete + confirm | Both | delete APIs | P0-Core | Destructive confirm | **Implemented** — `ReducedActions.tsx` + `pathApi.deleteItem`; confirmation via `handlers.confirm` (default `window.confirm`) | Vitest `reducedActions.test.tsx` |
| Permission denied / session errors | Both | Clear messaging | P0-Core | No blank hang | **Implemented** — error state surfaces via `errorStateStyle`; permission gating in `selection.ts` (`canRead`/`canWrite`/`canAdmin`); session expiry TMX key reserved (`SESSION_EXPIRED`) | Vitest coverage in component tests |
| ReducedAction set | Finder buttons subset | Product fixed set (ReducedAction enum) | P0-Core | FR-010a; entries enumerated in `data-model.md` | **Implemented** — 7 actions (open/preview/createFolder/rename/move/copy/delete); Vitest (`reducedActions.test.tsx`) | Vitest `reducedActions.test.tsx`; full action checklist Playwright `tests/us3-menus.spec.js` (T056b) |
| Miller-column primary UX | Finder | **Removed** at hard cut | P0-Core | SC-006 | **Pending** — US6 (T028-T036); US1 PR removes the placeholder registry entry but does not delete Finder from JSPs | Playwright `tests/us6-hard-cut.spec.js` (T028b) |
| Desktop CE required for core admin | CE app | **Not required** at hard cut | P0-Core | SC-007 | **Pending** — US6 (T034 docs/distribution) | Playwright `tests/us6-hard-cut.spec.js` CE-retired check |

---

## Content browser hosts (P-Host)

| Capability | Legacy source | Target | Phase | Acceptance |
|------------|---------------|--------|-------|------------|
| Reusable browser component | ContentBrowserDialog / Finder pickers | `ContentBrowser` | P-Host | Host contract |
| Single/multi select + filters | Dialogs | props | P-Host | US2 scenarios |
| Pilot host hard cut | — | inventory row | P-Host | Per host |
| AA / editor dialogs | Dojo/JSP browser | React host phases | P-Host | Independent hard cuts |
| Home Library browse | Finder library / 989 | Optional consumer | P-Host | Non-blocking for P0 |

**In-scope hosts for 8.2** (each row produces one task ID and one SC-002 evidence entry in `tasks.md` US2; see `checklists/cutover-inventory.md` §C for current call-site rows):

| Host id | Legacy surface | Hard-cut phase | Acceptance evidence |
|---------|----------------|----------------|---------------------|
| `host-asset-picker` | Finder asset picker widgets | P-Host-1 | SC-002 checklist + Vitest + Playwright (`tests/host-asset-picker.spec.js`) |
| `host-page-picker` | Finder page picker | P-Host-1 | SC-002 checklist + Vitest + Playwright (`tests/host-page-picker.spec.js`) |
| `host-aa-contentbrowser-dialog` | AA Dojo/JSP dialog | P-Host-2 | SC-002 checklist + Vitest + Playwright (`tests/host-aa-contentbrowser-dialog.spec.js`) |
| `host-folder-picker` | Folder picker dialog | P-Host-2 | SC-002 checklist + Vitest + Playwright (`tests/host-folder-picker.spec.js`) |
| `host-home-library` | Home Library browse (989 consumer) | P-Host-3 (optional, non-blocking) | SC-002 if adopted |

Each host row is **not complete** until its individual SC-002 evidence is recorded.

---

## Menus (P-Menu)

| Capability | Legacy source | Target | Phase | Acceptance |
|------------|---------------|--------|-------|------------|
| Context menu by selection | CE action menus | REST `/actions` + UI | P-Menu | SC-003 ≥10 actions (enumerated below) |
| Toolbar/menu bar actions | CE | Same | P-Menu | FR-010 |
| Workflow transitions | CE | Allowed transitions API | P-Menu | Authorized only |
| Keyboard menu access | CE | a11y | P-Menu | FR-013 |

**SC-003 high-value action enumeration (≥10; gate for full-menu P-Menu phase, not for intermediate hard cut)**:

| # | Action category | Example | Selection |
|---|------------------|---------|-----------|
| 1 | Open | Open in editor | Item / Folder |
| 2 | Edit | Edit item | Item |
| 3 | Preview | Preview | Item / Folder |
| 4 | Folder ops — create | Create folder | Folder |
| 5 | Folder ops — rename | Rename | Folder / Item |
| 6 | Folder ops — move | Move | Folder / Item |
| 7 | Folder ops — copy | Copy | Folder / Item |
| 8 | Folder ops — delete | Delete with confirm | Folder / Item |
| 9 | Properties | Open folder properties | Folder / Item |
| 10 | Workflow — check in/out | Force check-in | Item (checked out) |
| 11 | Workflow — transition | Transition (allowed) | Item (workflow state) |
| 12 | Workflow — history | View transitions | Item |

Two or more of #10–#12 satisfy the SC-003 "workflow or properties" clause.

## Advanced CE (P-Adv) — relationship / IA entity dimensions

The dependency viewer and IA/relationship views surface the following entities/dimensions (defined in `data-model.md`):

| Dimension | Source entity | Notes |
|-----------|---------------|-------|
| Outgoing relationships | `PSRelationship` (parent→child, AA link, etc.) | Filter by relationship type |
| Incoming relationships | `PSRelationship` reverse | Symmetric view |
| Active Assembly links | AA cross-reference | Type filter `aa` |
| Site/taxonomy edges | `PSNode` taxonomy | IA traversal |
| Local dependency | Item-local references (template, image) | Per-item graph |
| Reverse dependency | Inbound local references | Per-item graph |

**Acceptance per row**: at least the dimension above renders for the selected node, with permission filtering matching `PSFolderPermission`.

---

## Folder security (P-ACL)

| Capability | Legacy source | Target | Phase | Acceptance | Status | Test coverage |
|------------|---------------|--------|-------|------------|------------|---------------|
| View folder permission levels | CE `PSFolderSecurityPanel` | `FolderSecurityPanel` + `folderProperties` | P-ACL | SC-004 | **Implemented** — `WebUI/src/main/ts/contentExplorer/FolderSecurityPanel.tsx`; pure helpers in `aclLockout.ts` (`canViewSecurityPanel`, `canEditSecurityPanel`); uses sitemanage `PSFolderProperties` + `PSFolderPermission` (mirrored to live `projects/sitemanage/src/main/java/com/percussion/pathmanagement/data/` 1:1, no invented fields) | Vitest `WebUI/src/test/ts/contentExplorer/FolderSecurityPanel.test.tsx` (11 tests); Playwright `modules/perc-qa-automation/frontend/tests/us4-acl.spec.js` (5 tests) |
| Edit ACL principals | CE ACL editor | `saveFolderProperties` | P-ACL | SC-004 | **Implemented** — `FolderSecurityPanel` add / remove controls per level; saves via `pathApi.saveFolderProperties`. SC-004 second-user effect is gated on a system-installed CMS (the dev Derby image has no folder ACL data); Vitest + Playwright cover the structural surface | Vitest + Playwright as above |
| Lockout self warning | CE | Client warn (`window.confirm` or host-provided `confirmLockout`) + server | P-ACL | FR-015 | **Implemented** — pure `detectSelfLockout` / `wouldSelfLockout` in `aclLockout.ts`; the panel surfaces the warning via `EXPLORER_MSG.SECURITY_LOCKOUT_WARNING_BODY`; Vitest tests assert both `confirm=true` (save proceeds) and `confirm=false` (save aborted) paths | Vitest `aclLockout.test.ts` (20 tests covering single + multi-level removals, USER + ROLE names, identity-set semantics, empty/null defensive cases); `FolderSecurityPanel.test.tsx` self-lockout allow + cancel paths |
| Read-only without rights | CE | `canViewSecurityPanel` gate on the panel; edit controls disabled via `canEditSecurityPanel` | P-ACL | FR-016 | **Implemented** — `FolderSecurityPanel` renders the `SECURITY_READ_ONLY` banner + disables the Save button when `accessLevel !== 'ADMIN'`; renders the `PERMISSION_DENIED` placeholder when `accessLevel === 'VIEW'` | Vitest `aclLockout.test.ts` `canViewSecurityPanel` + `canEditSecurityPanel` tests; `FolderSecurityPanel.test.tsx` READ / VIEW paths |

---

## Search (P-Search)

| Capability | Legacy source | Target | Phase | Acceptance |
|------------|---------------|--------|-------|------------|
| Simple/extended search | CE / Finder search | searchmanagement | P-Search | US5 |
| Open/reveal from results | Both | Explorer | P-Search | US5 |
| Search in content browser | Dialogs | enableSearch prop | P-Search | US2/US5 |
| Saved searches catalog | CE | Matrix detail / REST gap | P-Search | Spike if missing |

---

## Translation (P-Trans) — required for 8.2 parity

**Status**: Translation was missing from the initial capability matrix seed and is added per PR review. Content Explorer exposes a translation workflow (locale variants, in-flight translations, source-vs-target status) that Finder did not surface; parity requires it on the modern explorer.

| Capability | Legacy source | Target | Phase | Acceptance |
|------------|---------------|--------|-------|------------|
| Show item locales (current + available) | CE translation panel | Explorer item detail | P-Trans | Each item row shows current locale + available locale list |
| Translate (create new locale variant) | CE translation action | action-menu integration (`/actions` + itemmanagement) | P-Trans | Authorized user can request a new locale for an item |
| In-flight translation status | CE translation queue | Explorer / search filter | P-Trans | List filter `translationState=inFlight`; result shows state |
| Switch source/target locale session context | CE locale toggle | Modern shell locale switcher | P-Trans | Selecting a locale re-issues path API calls under that locale |

**Open question for follow-up** (non-blocking for plan): exact REST surface for translation queue — investigate whether existing `rest` or `sitemanage` endpoints cover in-flight status, or whether a thin façade is required (T052a/T052b pattern).

---

## Advanced CE (P-Adv) — after intermediate hard cut; **required for 8.2** (FR-028, FR-029)

| Capability | Legacy source | Target | Phase | Acceptance |
|------------|---------------|--------|-------|------------|
| Multi-item clipboard copy/paste | CE `PSClipBoard` | Explorer clipboard | P-Adv | US7 |
| Site copy wizard | CE wizards + sitemanage copy | React wizard | P-Adv | Matrix row UAT |
| Subfolder copy wizard | CE wizards | React wizard | P-Adv | Matrix row UAT |
| Dependency viewer | CE `PSDependencyViewer` | React view | P-Adv | US7 |
| IA / relationship views | CE managers | React views | P-Adv | US7 |
| Display format full columns | CE display formats | List columns | P-Adv or P0 partial | FR-027 |
| Relationships manager deep tools | CE | React | P-Adv | Inventory |

**SC-011 / SC-012**: Before **8.2 GA**, every in-scope matrix row (including P-Adv) is **Done** with acceptance met—not unlabeled, not “post-8.2.”

---

## Explicit OUT (this feature)

| Capability | Rationale |
|------------|-----------|
| Content editor field forms / AA canvas | Separate tracks |
| JSF Admin / Publishing screens | Track B other features |
| GWT Package Manager | Separate |
| Eclipse Workbench | Separate |
| Offline desktop CE rewrite | Spec out of scope |
| Permanent dual Finder+Explorer production path | Clarification hard cut |
| Shipping 8.2 without full matrix parity | FR-029 / SC-012 release gate |

---

## Maintenance

- Update phase column when PRs land.
- Cutover inventory cross-links Finder call sites and CE packaging rows.
- New CE capabilities discovered in code review → add row; never silent drop.
