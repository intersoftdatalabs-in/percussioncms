# Capability matrix: Finder + Desktop CE → modern Content Explorer

**Feature**: `992-react-content-explorer`  
**Normative** for phase gating (FR-022, FR-028, FR-029, SC-011, SC-012)  
**Target product release**: **8.2** — all in-scope rows required for **8.2 GA**; **functional parity blocks 8.2**  
**Status**: Seed — expand during implementation; every advanced CE row must be labeled (no silent omit); **post-8.2 “scheduled” is not allowed** for in-scope rows

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

| Capability | Legacy source | Target | Phase | Acceptance |
|------------|---------------|--------|-------|------------|
| Explorer tree of sites/folders | CE tree / Finder columns | React tree | P0-Core | Expand/select loads children |
| Detail list of children | CE list / Finder list view | React list + pagination | P0-Core | Columns: name, type at min; SC-005 |
| Open item edit/preview | Both | Existing editor navigation | P0-Core | Path/id open works |
| Create folder | Both | path addFolder APIs | P0-Core | Appears after refresh |
| Rename | Both | renameFolder | P0-Core | Name updates |
| Move | Both | moveItem | P0-Core | Tree/list refresh |
| Copy (single item) | Both | path/item copy as today | P0-Core | Reduced action |
| Delete + confirm | Both | delete APIs | P0-Core | Destructive confirm |
| Permission denied / session errors | Both | Clear messaging | P0-Core | No blank hang |
| ReducedAction set | Finder buttons subset | Product fixed set (ReducedAction enum) | P0-Core | FR-010a; entries enumerated in `data-model.md` |
| Miller-column primary UX | Finder | **Removed** at hard cut | P0-Core | SC-006 |
| Desktop CE required for core admin | CE app | **Not required** at hard cut | P0-Core | SC-007 |

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
| `host-asset-picker` | Finder asset picker widgets | P-Host-1 | SC-002 checklist + Vitest |
| `host-page-picker` | Finder page picker | P-Host-1 | SC-002 checklist + Vitest |
| `host-aa-contentbrowser-dialog` | AA Dojo/JSP dialog | P-Host-2 | SC-002 checklist + Vitest |
| `host-folder-picker` | Folder picker dialog | P-Host-2 | SC-002 checklist + Vitest |
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

| Capability | Legacy source | Target | Phase | Acceptance |
|------------|---------------|--------|-------|------------|
| View folder permission levels | CE `PSFolderSecurityPanel` | folderProperties UI | P-ACL | SC-004 |
| Edit ACL principals | CE ACL editor | saveFolderProperties | P-ACL | SC-004 |
| Lockout self warning | CE | Client warn + server | P-ACL | FR-015 |
| Read-only without rights | CE | Hide/disable | P-ACL | FR-016 |

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
