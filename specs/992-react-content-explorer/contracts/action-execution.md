# Contract: Explorer action execution

**Feature**: `992-react-content-explorer` (US3 execution)  
**Companion spec**: Content Editor forms/controls are **out of scope** — see `specs/995-react-content-editor/spec.md`.

## Goal

Server-driven Explorer menus (`RXMENUACTION`) must **execute in React** via REST / existing services. They must **not** navigate to Data Flow (legacy XML application) `.html` / `.xml` URLs. Those URLs 404 from the SPA (`../sys_cxSupport/…` resolves under `/cm/`).

Desktop Content Explorer is **not** a 8.2 client. Do not dual-ship HTML for DCE.

## Language

- User-facing and new APIs: **template** / `templateId` (not variant).
- The former XML Application server is the **Data Flow Server** (platform I/O). It is not a UI toolkit.

## Dispatcher

`WebUI/src/main/ts/contentExplorer/actionDispatch.ts`

| Kind | Behavior |
|------|----------|
| `client` | Existing Explorer handlers (open, folder ops, workflow-transition, default preview) |
| `rest` | Public REST (preview-location, types, transitions, purge, publish) |
| `editor` | P3 — do **not** open CM1 `?view=editor`; TMX “editor not available” |
| `unavailable` | Slot arrange / New Item (P3 editor). Active Assembly parents are `rest` (996 preview host) |
| `legacy-file` | Non-app file (e.g. `.xls`) via same-origin `safeNavigate` |

Presentation (`ActionToolbar` / `ContextMenu`) **always** calls `onInvoke`. It never `safeNavigate`s.

## P1 (this train, after P0)

| Action | Behavior |
|--------|----------|
| Translate | Open Explorer Translations panel (existing REST) |
| Impact Analysis | Open Explorer Dependencies panel |
| Copy URL to Clipboard | Copy site-path preview URL (or CMS path) |
| Revisions | Open Revisions panel; restore when restorable |
| Audit Trail | Same panel, audit tab |
| New Copy | Confirm, then copy in current folder |
| Promotable Version | Confirm, then promotable version in current folder |
| Flush Cache (Refresh Item) | Confirm, then flush **all** assembler pages (not only the selected item) |
| Nav Reset | Same as classic `PSNavReset`; on 8.2 typically a no-op once nav is loaded (FastForward variants unused) |
| Publish Now / slot arrange | Still unavailable (P2 leftover / slot context) |
| Active Assembly | New window: assembled page/snippet preview + light overlay (`specs/996-react-active-assembly`) |

## P1 REST

| Method | Path | Notes |
|--------|------|-------|
| `POST` | `/services/assembly/flush-cache` | Empty keys = all assembler pages. `{ ok, message }` |
| `POST` | `/services/assembly/nav-reset` | Same goal as `PSNavReset`. 8.2 FastForward typically no-op. `{ ok, message }` |
| `GET` | `/services/itemmanagement/item/revisions/{id}` | Existing revisions + comments |
| `GET` | `/services/itemmanagement/item/restoreRevision/{id}` | Existing restore |
| `POST` | `/services/itemmanagement/item/newCopy/{id}` | New copy in first folder path |
| `POST` | `/services/itemmanagement/item/promotableVersion/{id}` | Promotable version in first folder path |

## P0 REST

`GET /services/assembly/preview-location?contentId=&templateId=&revision=`

Returns `{ previewUrl, contentId, templateId, revision }`.  
`previewUrl` is the assembly preview path (`/assembler/render?sys_contentid&sys_template&sys_revision&sys_context=0&sys_itemfilter=preview`).

Template **menus** come from existing `GET /actions/find/templates/{id}` and are merged under Preview parents (same pattern as New Item + `/actions/find/types`).

## Inventory

Normative action table and priorities live in the implementation plan (session plan) and should be copied into this file as the spec is expanded. Priority: **P0** (stop 404 + preview/new/workflow/purge/publish) → **P1** Explorer-only REST → **P2** slot/AA → **P3** Content Editor spec → **P4** delete unused Data Flow UI requestors.

## Hard bans

- Fetching Data Flow `.json` as a UI API
- Opening `/cm/app/?view=editor` from Explorer Edit
- Mass-keeping `.html` URLs “for DCE”
