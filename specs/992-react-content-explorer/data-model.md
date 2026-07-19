# Data Model: Unified React Content Explorer

**Feature**: `992-react-content-explorer`  
**Date**: 2026-07-19  
**Scope**: Conceptual entities for modern explorer/browser clients and existing server DTOs. No new persistence schema for core navigate / ACL phases.

## Entity overview

```text
PathNode (folder or item summary)
    ├── accessLevel
    ├── children[] (lazy)
    └── displayProperties{}

FolderProperties
    ├── permission (accessLevel + principal lists)
    └── acl (object ACL if present)

Selection (client)
    └── items[] → SelectionResult (host)

ActionDefinition (post-cutover)
    └── parameters[]

SearchQuery / SearchResult
ClipboardEntry (post-cutover advanced)
```

---

## PathNode (server: `PSPathItem` / list helpers)

| Field | Source / notes | Validation |
|-------|----------------|------------|
| id | Content/folder guid string | Required for open/ops when known |
| name / title | Display name | Non-empty for create/rename |
| path | Logical CMS path | Path encoding per existing `perc_utils` / server rules |
| type / category | Item type / folder vs page/asset | Filters in browser host |
| leaf | Tree leaf flag | — |
| hasFolderChildren / hasItemChildren / hasSectionChildren | Expandability hints | May be false on paginated children (server doc) |
| accessLevel | `PSFolderPermission.Access` | ADMIN, WRITE, READ, VIEW |
| displayProperties / columnData | Map for list columns | Optional; display-format phase |
| folderPath / folderPaths | Parent context | — |

**Relationships**: Hierarchical via path; children loaded on demand (`folder` / `paginatedFolder`).

**Client state**: Expanded path set, selected node id(s), loaded page cursors (`startIndex`, `maxResults`, sort).

---

## FolderProperties (server: `PSFolderProperties`)

| Field | Notes |
|-------|--------|
| id, name | Folder identity |
| permission | `PSFolderPermission` |
| acl | `PSObjectAcl` when present |
| communityId / communityName | Community context |
| locale | Folder locale |
| displayFormatName | List formatting |
| workflowId | Default workflow |
| allowedSites | Asset publish constraint |

**Mutations**: `POST saveFolderProperties` — server validates; UI warns if current user would lose access (FR-015).

---

## PSFolderPermission

| Field | Notes |
|-------|--------|
| accessLevel | Default for unspecified principals |
| adminPrincipals / writePrincipals / readPrincipals / viewPrincipals | Lists of `{ type: USER\|ROLE, name }` |

**Rules**: Only users with folder admin rights may edit (FR-016); server enforces.

---

## Selection / SelectionResult (client + host contract)

| Field | Required | Notes |
|-------|----------|--------|
| id | Yes (when item known) | Stable content/folder id |
| path | Preferred | For hosts that open by path |
| type / category | When filtering | Reject disallowed types (FR-006) |
| name | Optional | Display |
| multi | Host flag | Array of items when multi-select |

**Validation**: Host filters (folder-only, type allow-list); confirm disabled when invalid (US2).

---

## ReducedAction (hard-cut client enum)

Product-fixed set for FR-010a (not full action config). Enumerated entries:

| Action | Phase availability | Typical server call |
|--------|--------------------|---------------------|
| open / preview | P0-Core (intermediate hard cut) | Navigation to editor/preview by path/id |
| createFolder | P0-Core | addNewFolder / addFolder |
| rename | P0-Core | renameFolder |
| move | P0-Core | moveItem |
| copy | P0-Core | moveItem copy semantics or item copy service (inventory) |
| delete | P0-Core | delete folder/item endpoints with confirm |
| edit | P-Menu (FR-010 expansion) | existing itemmanagement update endpoints |
| properties | P-Menu | folderProperties / item properties |
| workflow transitions | P-Menu (FR-010 expansion) | Allowed transitions API |
| workflow check-in/out | P-Menu (FR-010 expansion) | Force check-in / check-out endpoints |
| workflow history | P-Menu (FR-010 expansion) | transitions audit endpoint |

P0-Core entries are the **intermediate subset** of the full set above and are required at the Finder/Desktop CE intermediate hard cut. The P-Menu entries ship with the full configuration-driven menus (FR-010) — they expand, not redefine, the ReducedAction set.

## ActionDefinition — SC-003 enumeration mapping

The ≥10 high-value actions enumerated in `contracts/capability-matrix.md` P-Menu (open, edit, preview, create folder, rename, move, copy, delete, properties, force check-in, transition, history) map to existing server actions on `rest` `ActionMenuResource` and sitemanage itemmanagement. The ReducedAction set above is the **intermediate subset** of those same actions available at the intermediate hard cut. When full action menus ship (FR-010), the full enumeration expands the ReducedAction set rather than redefining the actions.

---

## ActionDefinition (post-cutover US3)

Aligned with `rest` ActionMenu models:

| Field | Notes |
|-------|--------|
| name / label | Display (prefer TMX when product keys exist) |
| url / handler key | Execution target |
| visibility context | Selection type, workflow, UI context |
| parameters | ActionMenuParameter list |
| enabled | From server allow-list |

**State**: Built per selection change; hide/disable unauthorized (FR-011).

---

## SearchQuery / SearchResult

| Field | Notes |
|-------|--------|
| criteria | Simple/extended fields per searchmanagement API |
| results[] | PathNode-like summaries |
| totalCount | If provided by API |

**Transitions**: idle → searching → results | empty | error; open/reveal maps result → tree path.

---

## ClipboardEntry (US7 advanced)

| Field | Notes |
|-------|--------|
| itemIds / paths | Multi-select payload |
| operation | copy \| cut |
| sourceFolder | Origin path |
| pasted | Cleared or retained per CE-like rules after paste |

---

## Lifecycle notes

| Event | Behavior |
|-------|----------|
| Expand folder | Fetch children; cache by path; invalidate on mutate |
| Mutate (rename/move/delete) | Optimistic optional; always reconcile from server refresh |
| ACL save | Full replace of editable permission fields; error keeps prior UI state until success |
| Session expiry | Clear sensitive client state; re-login messaging (US1) |
| Host dialog open/close | Independent selection from main explorer (edge case) |

## Identity & uniqueness

- Prefer **content/folder id** as stable key; **path** as navigation key (paths can change on rename/move).
- After rename/move, refresh parent and update selection to new path/id from server response.

## Out of model (this feature)

- Full content item field schema (editors own that).
- Desktop CE SOAP DTOs as runtime model (reference only).
- New tables for menus/ACL (existing CMS config/storage).
