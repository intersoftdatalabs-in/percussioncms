# Contract: Path Management API (explorer core)

**Consumers**: React Content Explorer, Content Browser, (legacy Finder until hard cut)  
**Provider**: `projects/sitemanage` — `PSPathService` (`@Path("/path")` under pathmanagement service root)  
**Base (typical)**: `{SERVICES_ROOT}/pathmanagement/path` where `SERVICES_ROOT` is `/services` or `/Rhythmyx/services` per `WebUI/src/main/ts/api/paths.ts`

## AuthN/Z

- Same-origin session cookie as CM UI.
- Mutating calls MUST include product CSRF token (modern client pattern).
- Server enforces folder permissions; 403/validation errors returned as service faults—UI MUST surface messages.

## Core operations (hard-cut / US1)

| Operation | Method | Path / body | Response | Notes |
|-----------|--------|-------------|----------|--------|
| Find children | GET | `/folder/{path:.*}` | `PSPathItem[]` | Full list when small |
| Paginated children | GET | `/paginatedFolder/{path:.*}?startIndex&maxResults&sortColumn&sortOrder&child&displayFormatId&category&type` | `PSPagedItemList` | **Required** for SC-005 large folders |
| Find item by path | GET | `/item/{path:.*}` | `PSPathItem` | Open / resolve |
| Find item by id | GET | `/item/id/{id}` | `PSPathItem` | Open by id |
| Add folder | GET | `/addNewFolder/{path:.*}` or `/addFolder/{path:.*}` | `PSPathItem` | Match existing Finder usage |
| Rename folder | POST | `/renameFolder` body `PSRenameFolderItem` | path item / status | |
| Move item | POST | `/moveItem` body `PSMoveFolderItem` | status | Move/copy semantics as today |
| Delete | (existing delete folder/item endpoints used by `PercPathService.js`) | | | Confirm in UI first |
| Validate path | GET | `/validate/{path:.*}`, `/lastExisting/{path:.*}` | string/path | Deep link / goto |

## Folder properties / ACL (US4)

| Operation | Method | Path | Body / response |
|-----------|--------|------|-----------------|
| Get properties | GET | `/folderProperties/{id}` | `PSFolderProperties` |
| Save properties | POST | `/saveFolderProperties` | `PSFolderProperties` → `PSNoContent` |

**Rules**:
- `id` must be valid guid string (server validates).
- Save may reject during site-copy locks (`PSSiteCopyUtils`)—surface server message.
- Client SHOULD warn before save if current user loses access (FR-015); server remains authoritative.

## Error model

- Path not found → service exception / 4xx with message.
- Validation → `PSValidationException` style messages.
- Client treats empty body / network failure as recoverable error state (not blank tree).

## Compatibility

- **No breaking change** to existing JSON field names for core ops without versioned dual support.
- New query params only if backward compatible defaults.
- Modern TS clients type against current DTO shapes (`PathItem`, `FolderProperties`, paged list).

## Out of contract

- Desktop CE SOAP `FindFolderChildren`.
- Action menu listing (see [action-menu-api.md](./action-menu-api.md)).
- Full text search body (searchmanagement separate service).
