---
id: developer-rest
title: REST API
description: Public REST API orientation for Percussion CMS 8.2
version: "8.2"
order: 51
tags: [developer, rest]
---

# REST API

Percussion CMS exposes a public **REST** surface for product integrations and modern UI clients.

## Module layout

| Module | Responsibility |
|--------|----------------|
| **`rest`** | JAX-RS resources, wire DTOs, `IXxxAdaptor` interfaces, OpenAPI generation inputs |
| **`projects/sitemanage`** | Thin `com.percussion.apibridge` implementations of those interfaces |
| **`system`** | Core services, objectstore, assembly, design backends |

Do **not** reverse the dependency: `rest` never depends on `sitemanage`.

## OpenAPI / exploration

- OpenAPI artifacts are generated from JAX-RS annotations (see `modules/perc-openapi-generator-plugin`).
- A Swagger UI webapp module packages interactive exploration for supported deployments.

Prefer the generated contract as the integration source of truth rather than reverse-engineering
UI traffic alone.

## Auth and clients

- REST calls require the authentication mode configured for the server (session/cookie or token
  patterns depending on surface and deployment).
- Treat credentials and tokens as secrets; never commit them to Git or product-docs examples.

## Workbench-replacement APIs

Developer-module (Workbench replacement) endpoints should map design operations through clean REST
+ adaptors to the same design/system capabilities classic tools used — not ad-hoc sitemanage
endpoints chosen only because they “look REST.” See repository
`docs/developer-module/workbench-rest-and-qa-modes.md` for the engineering contract.

## Keywords (design catalog)

Keyword definitions (Workbench **Keywords** / content design) are exposed under `/services/keywords`.
The REST layer is a thin contract over the content **design** web service (`IPSContentDesignWs`) —
create, update, and delete use the same design locks and session identity classic tools use.

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/services/keywords?includeChoices=false\|true` | List keyword definitions; optionally embed choices |
| `GET` | `/services/keywords/{idOrValue}` | Load one keyword by uuid (or numeric id) **or** by value/label (choices included) |
| `POST` | `/services/keywords` | Create a keyword (`label` required, must be unique); optional description, sequence, choices |
| `PUT` | `/services/keywords/{id}` | Update label / description / sequence / choices by uuid |
| `DELETE` | `/services/keywords/{id}` | Delete keyword and its choices by uuid (`204` on success) |

### Request / response shape

JSON objects use the `Keyword` wire type (fields include `guid`, `label`, `value`, `description`,
`sequence`, and `choices[]` with `label` / `value` / `description` / `sequence`). Prefer the
generated OpenAPI schema as the integration source of truth.

Example create body:

```json
{
  "label": "Priority",
  "description": "Item priority",
  "sequence": 1,
  "choices": [
    { "label": "High", "value": "high", "sequence": 1 },
    { "label": "Low", "value": "low", "sequence": 2 }
  ]
}
```

### Status codes and authorization

| Status | Typical meaning |
|--------|-----------------|
| `200` | List / get / create / update success |
| `204` | Delete success |
| `400` | Invalid input (missing label, duplicate label, invalid id) |
| `404` | Keyword not found |
| `500` | Design service or server failure |
| `503` | Keywords adaptor not configured (deployment miswire) |

- Callers must be authenticated; write operations require a request session and user identity for
  the design web service (same pattern as other design catalog writes).
- Design ACL / design-session rules of the underlying content design service still apply — REST does
  not introduce a separate admin-only bypass.

### Integrator notes

- After create/update the server reloads the keyword so the response includes the assigned `guid`
  and normalized choices.
- Prefer uuid (or the `guid` string form) for update/delete; value/label lookup on `GET` is for
  convenience in tooling.
- The Developer SPA Keyword editor uses these endpoints; integrators can call the same surface
  without the UI.

## Sites (catalog)

| Operation | Path | Notes |
|-----------|------|--------|
| List | `GET /services/sites` | All CMS Sites (traditional, page-based, Virtual). JSON is a Jackson root wrap `{ "SiteList": [ { "name", "description", "baseUrl", … } ] }` or a bare array. Each entry includes a plain string `name` when the Site has one. |
| Detail | `GET /services/sites/{nameOrId}` | Site detail including `virtual.*` when configured |
| Virtual properties | `GET` / `PUT /services/sites/{nameOrId}/virtual` | Virtual Site source bag |
| Virtual build | `POST /services/sites/{nameOrId}/virtual/build` | Admin-only; Git-filesystem Virtual Sites |

An HTTP 200 list with Site entries must bind in **Developer → Sites**. Empty list JSON is the
only empty-catalog case. See [Sites & content structure](id:admin-sites).

## Content types (design catalog)

| Operation | Path | Notes |
|-----------|------|--------|
| List | `GET /services/contenttypes` | Name, label, description, guid |
| Detail | `GET /services/contenttypes/{idOrName}` | Field catalog, associations, `designGaps` |

### Field rule expressions (read-only)

Content type **detail** field rows include boolean rule **flags** and, when rules exist,
human-readable **expression summaries**:

| Field | Meaning |
|-------|---------|
| `hasValidation` / `validationExpression` | Validation rules present / summary of conditionals or extension calls |
| `hasVisibilityRules` / `visibilityExpression` | Visibility rules present / summary |
| `hasInputTranslation` / `inputTranslationExpression` | Input transform present / extension call summary |
| `hasOutputTranslation` / `outputTranslationExpression` | Output transform present / extension call summary |
| `control` | Display control name |
| `controlPropertyNames` | Control parameter **names** only (values and full choice catalogs not exposed) |

These expression fields are **null/omitted when empty** (`NON_NULL` JSON). They are **not**
writable via `PUT` — rule write/save and full control property editors remain Workbench /
future design APIs. `designGaps` on detail still calls out write and catalog gaps.

## Views (design catalog)

Content Explorer **view** definitions (Workbench / Developer **Views**, UI-07) are exposed under
`/services/views` (public servlet path `/rest/views`). This is a **separate catalog** from saved
**searches** (`/services/searches`). Do not execute a view through the search execute endpoint.

Operators open Inbox from Explorer **Views → My Content → Inbox** (see
[Content Explorer](id:admin-content-explorer)). Integrators run the same assignment list
with the execute call below.

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/services/views` | List view definitions (name, category, standard vs custom URL) |
| `GET` | `/services/views/{idOrName}` | Load one view by name, numeric id, or GUID string |
| `POST` | `/services/views/{idOrName}/execute` | Execute a **standard** (field-criteria) view or an Inbox-family **custom URL** view |

### Execute request / response

Optional JSON body (all fields optional):

| Field | Meaning |
|-------|---------|
| `folderPath` | Scope results to a folder path; recurse defaults on when set |
| `startIndex` | 1-based page start (must be ≥ 1) |
| `maxResults` | Page size (must be ≥ 1); omitted uses the view design max or a product default |
| `sortColumn` | `sys_title` / `title` / `name`, `type`, or `folderPath` |
| `sortOrder` | `asc` or `desc` |

Successful response is a paged envelope: `children[]` (Explorer-ready rows with `id`, `name`,
`title`, `folderPath`, `type`), `totalCount`, `startIndex`, `viewName`, `displayFormatId`.

### Status codes

| Status | Typical meaning |
|--------|-----------------|
| `200` | List / get / execute success |
| `400` | Invalid execute body, or an **unsupported** custom URL view |
| `404` | View not found or unsafe key (blank, path separators, `..`) |
| `500` | Design or execute engine failure (standard views) |
| `503` | Views adaptor not configured, or custom-view backend unavailable |

### Custom URL views (Inbox family)

Views flagged as **custom** (`customView`) store a classic application URL instead of field
criteria. `POST /services/views/{idOrName}/execute` **runs** the documented Inbox family by
invoking the classic resource and mapping `Item` rows to Explorer items (`id`, `name`, `title`,
`folderPath`, `type`):

| View (typical name) | Classic resource |
|---------------------|------------------|
| Inbox | `sys_cxViews/inbox` (`../sys_cxViews/inbox.xml`) |
| Outbox | `sys_cxViews/outbox` |
| Recent | `sys_cxViews/recent` |
| Session | `sys_cxViews/session` |
| Checked out by me | `sys_cxViews/checkedoutbyme` |
| Duplicate folder paths | `sys_cxViews/duplicatefolderpaths` |

An empty Inbox is a **`200`** with `children: []` — not an error. Custom URLs **outside** that
allow-list (blank URL, another application, unknown page, path traversal) return **`400`** with a
clear message. Missing or unsafe view keys remain **`404`**. A missing request context or
unavailable `sys_cxViews` resource returns **`503`**, not **`500`**.

Standard field-criteria views (`standardView`) still execute with the same design operators,
display format, max results, and case sensitivity stored on the view design.

### Integrator notes

- Keys may be the view **name**, numeric **id**, or GUID string (including untyped GUID).
- Create / update / delete of view designs is not supported on this API (`designGaps` on detail).
- The Developer SPA lists views via `GET`. Operator Inbox run-from-tree is Explorer
  **Views → My Content → Inbox**, not a free-floating Inbox root.

## Design capability gaps (`designGaps`)

Some Developer detail payloads include a **`designGaps`** array so clients know what the REST
surface does **not** yet match full Workbench / design-WS capability.

### Structured shape (Content Type, Template, Slot detail)

**Breaking change (REST-GAPS-01):** on **content type**, **template**, and **slot** detail responses,
`designGaps` is no longer a free-text string array. Each entry is a structured object
(`{ "code", "message" }`). Integrators that treated entries as bare strings must update.
Other Developer catalog detail resources may still return string arrays until migrated.
There is no dual-shape / dual-version wire for these three paths in this release.

On those three detail responses, each gap is a structured object:

```json
{
  "designGaps": [
    {
      "code": "CT_ITEM_EXITS",
      "message": "Item-level pre/post exits not exposed"
    }
  ]
}
```

| Field | Role |
|-------|------|
| **`code`** | Stable machine-readable id for SPA grouping, docs links, and future i18n keys |
| **`message`** | English human-readable text for operators (this release) |

Do **not** treat these entries as free-text strings on those three detail paths. Other Developer
catalog detail resources may still return string arrays until migrated.

Clients should render **`message`** when present and fall back to **`code`** (or a legacy string)
when needed.

### List vs detail (payload dedup)

Catalog-level gaps are **shared** across every object of a type (they are not per-item data). To
avoid repeating the same large array on every list row:

| Response | `designGaps` |
|----------|--------------|
| **List** (`GET ./searches`, `./views`, `./cecontrols`, `./serverconfigs`, `./relationshiptypes`, .) | Typically **omitted** (null / empty  not serialized) |
| **Detail** (`GET ./{idOrName}`) | **Present** with the full catalog-level list |

SPA detail panels already fall back to local constants when the server omits gaps. Integrators
should treat missing `designGaps` on list rows as "use the detail resource (or known catalog
constants), not as `no gaps'."

Content-type detail may still include **extra** per-item gaps (for example control-resolution
failures); those remain on the detail payload only. Structured `{ code, message }` entries apply
on the Content Type / Template / Slot detail paths described above.

## Searches catalog and execute

CX design **searches** (and optionally **views**) are exposed under `/services/searches`.

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/services/searches` | List search definitions (Developer catalog; views omitted) |
| `GET` | `/services/searches?includeViews=true` | List searches **and** views (Explorer saved-search picker) |
| `GET` | `/services/searches/{idOrName}` | Load one search or view by name, label, GUID, or numeric id |
| `POST` | `/services/searches/{idOrName}/execute` | Execute a standard/user search or view (not a custom URL) |

`includeViews=true` is required for the Explorer Search panel so the default **All** view
(`View_All`) is in the picker. Developer **Searches** remains searches-only; views stay on
`/services/views`.

If the search catalog fails independently of views (or the reverse), `includeViews=true`
still returns whichever side loaded so the picker is not a silent empty list or 500 for
the default All view.

Execute looks up the same combined catalog (searches first, then views). Keys accepted:
internal name (`View_All`), display label (`All`), GUID string, or numeric id. Custom URL
searches and custom views return **400**.

## Content Explorer folders (Rhythmyx path façade)

Public REST for **Rhythmyx** folder operations used by Content Explorer integrators and (later)
Explorer mutation clients. Base path: **`/Rhythmyx/rest/content-explorer/folders`**.

This surface is a thin façade over classic **`IPSContentWs`** folder methods (same domain path as
SOAP content folder ops). It is **not** the CM1 site/asset `/rest/folders` resource and **not**
pathmanagement browse/pagination (`/services/pathmanagement/path/*`).

### Path forms

Accept either repository form or a documented single-slash form; the server normalizes:

| Client form | Normalized for WS |
|-------------|-------------------|
| `//Folders/...`, `//Sites/...` | kept |
| `/Folders/...`, `/Sites/...` | promoted to `//…` |
| `Folders/...`, `Sites/...` | promoted to `//…` |
| `/` | root listing (`Folders` + `Sites`) |

### Endpoints (v1)

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/content-explorer/folders/by-path/{path}` | Load folder by RX path |
| `GET` | `/content-explorer/folders/by-id/{id}` | Load folder by guid / content id |
| `GET` | `…/by-id/{id}/children` | Direct children (items + folders) |
| `GET` | `…/by-path/{path}/children` | Direct children by path |
| `GET` | `…/by-id/{id}/child-folders` | Folder children only |
| `GET` | `…/by-path/{path}/child-folders` | Folder children only by path |
| `POST` | `/content-explorer/folders` | Add folder (`name` + `parentPath`) |
| `POST` | `/content-explorer/folders/tree` | Add missing path segments (`path`) |
| `PUT` | `/content-explorer/folders/by-id/{id}` | Save name / description / community / locale / properties |
| `POST` | `/content-explorer/folders/move-children` | Multi-child move |
| `POST` | `/content-explorer/folders/add-children` | Multi-child attach |
| `POST` | `/content-explorer/folders/remove-children` | Multi-child detach (`purgeItems` optional) |
| `DELETE` | `/content-explorer/folders/by-id/{id}?purge=false` | Recursive delete; optional purge |

Prefer the generated **OpenAPI** schema for wire field names. Auth and folder ACLs of the underlying
content web service still apply.

### Dual-run mutation flag (Explorer clients)

Content Explorer can optionally route **folder mutations** (create / rename / move / delete)
under **Folders** and **Sites** to this façade while **browse / list / pagination** stay on
pathmanagement.

| Item | Value |
|------|--------|
| Operator property name | `perc.explorer.rxFolderMutations` |
| Default | **off** (pathmanagement only — zero behavior change) |
| Client enable (QA / dual-run) | URL `?rxFolderMutations=1`, or `sessionStorage` / `localStorage` key `perc.explorer.rxFolderMutations=true`, or `window.__PERC_RX_FOLDER_MUTATIONS__ = true` |
| When on | Mutations under `/Folders` and `/Sites` (and `//Folders` / `//Sites`) use this REST surface |
| Still on pathmanagement | List/paginate, ACL folder-properties save, copy, non-RX roots (`/Assets`, `/Design`, `/Recycling`, …) |

See also [Content Explorer](id:admin-content-explorer) dual-run notes.

### Migration notes

- Content Explorer **browse / pagination** remains on pathmanagement until a deliberate client
  switch; mutation dual-run is **default off** (`perc.explorer.rxFolderMutations`).
- Do not confuse with foldermanagement workflow assignment (`/services/folders`) or CM1
  `FoldersResource` section APIs.

## Testing tips

- Unit-test resources with Mockito and provide Spring test stubs for new adaptor interfaces on the
  rest test classpath.
- Exercise adaptor implementations in sitemanage tests.
- Run **standalone** `mvnw clean install` in each changed module before PR (see root `AGENTS.md`).

## Related

- [Extensions & packages](id:developer-extensions)
- [Build from source](id:developer-build-source)
- [Content Explorer](id:admin-content-explorer)
