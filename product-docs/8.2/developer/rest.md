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

## User preferences

Stored per-user preferences live under `/services/preferences` (same resource under the
`/Rhythmyx/services` context-path prefix when the CMS is reached that way).

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/services/preferences/` | All stored preferences for the current user |
| `GET` | `/services/preferences/{name}` | One named preference (for example `perc_profile_gravatar_email`) |
| `PUT` | `/services/preferences/` | Save one preference (`UserPreference` Jackson root) |

An **unset** catalog or named preference is a successful empty result, not a missing resource:

| Status | Meaning |
|--------|---------|
| `200` | List (possibly empty) or named preference. When the name is not stored, the body is a `UserPreference` with that `name` and an empty `value` |
| `500` | Unexpected server failure |

Do **not** treat empty store as `404`. Product Profile loads the Gravatar override from
`GET /services/preferences/` (the list), not `GET /services/preferences/perc_profile_gravatar_email`.
Explorer chrome does not probe the named preference. A blank list entry (or no entry) means
“use the account default.” Clients that still send `GET /preferences/{name}` should accept
`200` + empty `value` as unset — do not treat that 200 as a stored email.

## Sites (catalog)

| Operation | Path | Notes |
|-----------|------|--------|
| List | `GET /services/sites` | All CMS Sites (traditional, page-based, Virtual). JSON is a Jackson root wrap `{ "SiteList": [ { "name", "description", "baseUrl", … } ] }` or a bare array — **not** `{ "empty": false }`. Each entry includes a plain string `name` when the Site has one. |
| Detail | `GET /services/sites/{nameOrId}` | Site detail including `virtual.*` when configured |
| Virtual properties | `GET` / `PUT /services/sites/{nameOrId}/virtual` | Virtual Site source bag. PUT JSON is `{ "VirtualSiteProperties": { "sourceKind", "rootPath", "remoteUrl", "branch", "configFile", "siteKey" } }` (Jackson/JAXB root wrap). Allow-listed `sourceKind`: `git-filesystem`, `csv-filesystem` (GET round-trips the kind after PUT). Unknown kinds and `csv-filesystem` + `remoteUrl` return **400**. A flat `{ "sourceKind": … }` body returns **400** unexpected element `sourceKind`. Optional `remoteUrl` + `branch` clone/fetch before Git Build; omit `remoteUrl` to keep a stored remote; send `""` to clear. |
| Virtual build | `POST /services/sites/{nameOrId}/virtual/build` | Admin-only; `git-filesystem` or `csv-filesystem`. Git fetches `remoteUrl` when set; CSV reads a local CSV tree (`rootPath`; optional `_config.yaml`). Same action as **Developer → Sites → Build Virtual Site**. Unknown `sourceKind` is **400**. |
| Virtual preview status | `GET /services/sites/{nameOrId}/virtual/preview` | Admin-only; last-build availability for `git-filesystem` **and** `csv-filesystem` (not git-only). Missing build → `available=false` (HTTP 200). Repository / unknown `sourceKind` → 400. CLI assemble is previewable only at the default output root (no last-output pointer). |
| Virtual preview file | `GET /services/sites/{nameOrId}/virtual/preview/{relPath}` | Admin-only; assembled file stream from last output. Traversal (`../`) or file over 20 MB → 400; missing file → 404 (not 500) |
| Virtual publish | `POST /services/sites/{nameOrId}/virtual/publish` | Admin-only; `git-filesystem` or `csv-filesystem`. Builds then copies assembled files to the Site filesystem root (`IPSSite.root`). Same action as **Developer → Sites → Publish Virtual Site**. Repository / unknown kinds are **400**. |

An HTTP 200 list with Site entries must bind in **Developer → Sites**. Empty catalog chrome
appears only when this list **and** the sitemanage `GET /sitemanage/site/` `SiteSummary` fallback
are both empty. See [Sites & content structure](id:admin-sites).

## Templates (design catalog)

Assembly templates used by the [Design SPA](id:admin-design-templates) are exposed under
`/services/templates`.

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/services/templates` | List template summaries (label, name, id, description) |
| `GET` | `/services/templates/{idOrName}` | Design detail (source, bindings, slots, assembler, `designGaps`) |
| `PUT` | `/services/templates/{idOrName}` | Update label, description, templateSource, assembler, bindings, and/or slots |
| `POST` | `/services/templates` | Create a modern assembly template (**when installed**) — no Widget XML |
| `DELETE` | `/services/templates/{idOrName}` | Delete a modern assembly template — no Widget XML |
| `POST` | `/services/templates/summaries-by-filter` | List summaries matching a `TemplateFilter` |

`PUT` omits unchanged fields. Name/id remain unsupported. Delete returns **204** when
the template is removed and **404** when it is not found. Lock remains unsupported
(`designGaps` code `TPL_LOCK`).
Create (`POST /services/templates`) is the Design **Create template** contract when that
slice is on the server; otherwise create stays on residual classic hosts.

## Slots (design catalog)

Assembly **slots** used by **Developer → Slots** are exposed under `/services/slots`.

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/services/slots` | List slot summaries (label, name, description) |
| `GET` | `/services/slots/{idOrName}` | Design detail (finder, associations, `designGaps`) |
| `PUT` | `/services/slots/{idOrName}` | Update label, description, and/or content-type/template associations |

JSON may wrap a single item as `SlotDetail`. `associations` and `designGaps` are arrays
(`SlotAssociationSummary[]` and structured `{code,message}` gaps). Some Jackson/JAXB
envelopes historically serialize a list as a single object, a `{ SlotAssociation: … }` /
`{ DesignGap: … }` wrapper, or an empty-collection bean (`{ "empty": false }`).
Finder arguments may appear as a JAXB map (`{ "entry": [ { "key", "value" }, … ] }`)
instead of a flat `{ name: value }` object.

**Developer → Slots** detail treats those non-array shapes as an empty list or unwraps
the single item, and flattens finder-argument maps to readable `name = value` rows.
The slot form stays on screen (or shows an in-panel error). It does
**not** replace the Developer shell with **Unable to load Developer**. Capability gaps
still render as the human-readable **message** (fallback **code**). Load failures stay
in the slot detail panel — use **Back** to return to the catalog.

## Content types (design catalog)

| Operation | Path | Notes |
|-----------|------|--------|
| List | `GET /services/contenttypes` | Name, label, description, guid |
| Detail | `GET /services/contenttypes/{idOrName}` | Field catalog, associations, `designGaps` |
| Lock | `POST /services/contenttypes/{idOrName}/lock` | **Admin.** Self-only design-session lock (`IPSContentDesignWs.loadContentTypes` with `lock=true`, `overrideLock=false`). Does **not** save. `200` + `ObjectLockSummary` (`session`, `locker`, `remainingTime` minutes). Re-lock by the same session user extends the lock. |
| Unlock | `POST /services/contenttypes/{idOrName}/unlock` | **Admin.** Releases a lock owned by the current session user (Workbench `releaseLocks`). Does **not** save. `204` on success. |

Lock/unlock status codes:

| Status | Typical meaning |
|--------|-----------------|
| `200` | Lock acquired (body is `ObjectLockSummary`) |
| `204` | Unlock success |
| `403` | Caller is not Admin |
| `404` | Content type not found |
| `409` | Locked by another user (self-only; the lock is not stolen) |
| `500` | Design service or server failure |

`PUT /services/contenttypes/{idOrName}` still lock-saves-unlocks in one request. Use these lock/unlock endpoints when a client needs an explicit design session before a later save.

JSON may wrap a single item as `ContentTypeDetail`. Integrators and the Developer
SPA unwrap that envelope and read `guid.stringValue` (or synthesize
`hostId-type-uuid` when `stringValue` is omitted) before calling
`GET /services/acls/object/{guid}` for **Object ACL**. The list `guid` is a
fallback when detail omits Guid parts.

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

## Templates (assembly catalog)

Assembly templates used by Design and Developer are exposed under `/services/templates`.
Create uses the modern package/manifest model — **no Widget definition XML**.

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/services/templates` | List template summaries (design catalog) |
| `POST` | `/services/templates/summaries-by-filter` | Filter summaries (for example by content id) |
| `GET` | `/services/templates/{idOrName}` | Load design detail (source, bindings, slots, assembler) |
| `PUT` | `/services/templates/{idOrName}` | Update label, description, source, assembler, bindings, slots |
| `POST` | `/services/templates` | Create a modern assembly template (`name` required, unique, no spaces) |
| `DELETE` | `/services/templates/{idOrName}` | Delete a modern assembly template (204; no Widget XML) |

Create body is a Jackson-wrapped `TemplateDetail`:

```json
{
  "TemplateDetail": {
    "name": "site.html.snippet",
    "label": "HTML snippet",
    "description": "Modern HTML-first template",
    "assembler": "Java/global/percussion/assembly/htmlAssembler"
  }
}
```

When `assembler` is omitted, the server defaults to HTML-first
(`Java/global/percussion/assembly/htmlAssembler`). `400` is returned for a missing or
invalid name or a duplicate name.

`DELETE /services/templates/{idOrName}` removes the template from the assembly catalog
and returns **204**. It does **not** write Widget definition XML. `404` means the name
or id was not found. Lock remains out of scope (`designGaps` code `TPL_LOCK`).

The Design SPA **Create template** action uses POST; **Delete** uses this DELETE. See
[Design templates](id:admin-design-templates).

## Display formats (design catalog)

Content Explorer **display format** definitions (Developer **Display Formats**) are exposed
under `/services/displayformats`. Responses include a nested `guid` object and a plain
`guidString` (`host-type-uuid`) so clients can load **Object ACL** via
`GET /services/acls/object/{guid}`.

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/services/displayformats` | List formats (optional `validForFolder` / `validForViewsAndSearches`) |
| `GET` | `/services/displayformats/{idOrName}` | Load one format by internal name or GUID string |

JSON wraps the list as `DisplayFormatList` (`{"DisplayFormatList":[…]}`) including the empty
catalog (`{"DisplayFormatList":[]}`, not a bare `[]`) and a single item as `DisplayFormat`.
Integrators should unwrap those envelopes and read `guid.stringValue` or `guidString` (never
assume the GUID is missing when `displayId` is present). See [Users, roles & security](id:admin-users-roles)
for the operator Object ACL steps.

### Object ACL save (display format and peers)

Design-time ACLs use `/services/acls`. Production JSON uses Jackson
`WRAP_ROOT_VALUE` / `UNWRAP_ROOT_VALUE`:

| Method | Path | Envelope |
|--------|------|----------|
| `GET` | `/services/acls/object/{guid}` | Response may wrap as `{"Acl":{…}}` |
| `POST` | `/services/acls/` | Request `{"CreateAclRequest":{"objectGuid":{"stringValue":"…"},"owner":{"name":"Admin","type":"USER"}}}` |
| `PUT` | `/services/acls/bulk` | Request `{"AclList":[{…}]}` — **not** a bare JSON array |

The server binds that `{"AclList":[…]}` envelope (or a bare JSON array) to an
`AclList` instance (not a raw `java.util.ArrayList`). A JAX-RS reader, a Jackson
deserializer, and a string-body bind all accept the SPA save shape so Display Format
Object ACL Save is HTTP 200, not `ClassCastException: Cannot cast java.util.ArrayList
to AclList`. Nested `guid` / `objectGuid` objects may omit `stringValue` when
`type` and `uuid` are present.

Each ACL in the list should include `objectGuid` (and `objectType` / `objectId` when known),
plus the ACL `id` / `guid` when the object already has an ACL. The server **merges** that
payload onto the existing `PSX_ACLS` row (same SYSID / object identity) instead of inserting
a duplicate. After HTTP 200, `GET /services/acls/object/{guid}` returns `aclEntries`
including **Default**, **AnyCommunity**, and any USER you saved (for example Admin).
Entries use `principal.name` only; the principal type lives on `type.type`
(`USER`, `COMMUNITY`, `ROLE`, …). A bare array body is HTTP 400.

## Action menus (design catalog)

Content Explorer **action menu** definitions (Developer **Action Menus**) are exposed
under `/services/actions/catalog`. Each catalog and detail row includes a nested
`guid` (`PSTypeEnum.ACTION` = 107, string form `0-107-{actionId}`) so clients can
load **Object ACL** via `GET /services/acls/object/{guid}`. When the nested Guid is
hard to bind, clients may also synthesize that same string from the numeric `id`.

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/services/actions/catalog` | List action menus (tree roots with children) |
| `GET` | `/services/actions/catalog/{idOrName}` | Load one menu by name, numeric id, or GUID string |

JSON may wrap a single item as `ActionMenu`. Integrators should unwrap that
envelope and read `guid.stringValue` (never assume the GUID is missing when `id`
is present). See [Object ACL & default template](id:admin-object-acl).

## Views (design catalog)

Content Explorer **view** definitions (Workbench / Developer **Views**, UI-07) are exposed under
`/services/views` (public servlet path `/rest/views`). This is a **separate catalog** from saved
**searches** (`/services/searches`). Do not execute a view through the search execute endpoint.
List and detail include a nested `guid` (`PSTypeEnum.VIEW_DEF` = 18, string form
`0-18-{viewId}`) so clients can load **Object ACL**. Unwrap Jackson `ViewDef` envelopes
and read `guid.stringValue` or synthesize from `id` when the Guid is omitted.

Operators open Inbox from Explorer **Views → My Content → Inbox** (see
[Content Explorer](id:admin-content-explorer)). Integrators run the same assignment list
with the execute call below. `GET /services/views` includes the Inbox design view (name
`Inbox`, custom URL `../sys_cxViews/inbox.xml`) even when the design-WS load path
collapses sibling CX views to `View_All`.

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/services/views` | List view definitions (name, category, standard vs custom URL; includes `guid`) |
| `GET` | `/services/views/{idOrName}` | Load one view by name, numeric id, or GUID string (includes `guid` for Object ACL) |
| `POST` | `/services/views/{idOrName}/execute` | Execute a **standard** (field-criteria) view or an Inbox-family **custom URL** view |

### Execute request / response

The preferred execute body uses the Jackson / JAXB root name `ViewExecuteRequest`.
Explorer Inbox and other clients should send that envelope. The server also
accepts a flat `{ "startIndex": 1, "maxResults": 50 }` object (same fields) so
Inbox execute is not rejected as `unexpected element startIndex`. An empty body
is allowed when you need design defaults only. Recommended JSON:

```json
{
  "ViewExecuteRequest": {
    "folderPath": "/Sites/Demo",
    "startIndex": 1,
    "maxResults": 50
  }
}
```

All fields inside `ViewExecuteRequest` are optional:

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

## Workflows (design catalog)

Workflow definitions used by **Developer → Workflows** (SY-04 browse) are exposed under
`/services/workflowmanagement/workflows`. This is the existing stepped-workflow catalog,
not a full graph editor and **not** an Object ACL surface (workflow DTOs have no GUID
in this release).

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/services/workflowmanagement/workflows/metadata` | List workflow metadata (`workflowName`, default flag, steps) |
| `GET` | `/services/workflowmanagement/workflows/{name}` | Load one workflow by name (steps, staging roles, default flag) |

JSON list and detail may wrap under Jackson / JAXB root `Workflow` (including nested
`{ "Workflow": { "Workflow": { … } } }` envelopes). The name field is `workflowName`;
some serializers also emit `name`. **Developer → Workflows** unwraps those envelopes
and binds `workflowName` (or the `name` alias) before rendering detail. A wrapped
payload without a top-level `workflowName` must still open **Default Workflow** /
**Simple Workflow** instead of **Could not load workflow**. There is **no Object ACL**
section on workflow detail.

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

POST body is the JAXB envelope `{ "SearchExecuteRequest": { … } }` (flat
`startIndex` / `folderPath` is also accepted). Optional fields: `folderPath`,
`startIndex` (≥ 1), `maxResults` (≥ 1), `sortColumn`, `sortOrder` (`asc` /
`desc`). `folderPath` may be repository form (`//Sites/...`) or Explorer form
(`/Sites/...`). Explorer **root** (`/` or `//`) is ignored so **All** /
`View_All` runs unscoped instead of failing `getIdByPath`. A successful execute
returns a paged `SearchExecuteResult` (`children[]`, `totalCount`,
`startIndex`) — an empty `children` array is a valid page, not an error.

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
| `POST` | `/content-explorer/folders` | Add folder. JSON root must be `AddFolderRequest` with `name` + `parentPath` (optional `sourcePath`). Explorer sends that wrap when folder mutations are enabled. |
| `POST` | `/content-explorer/folders/tree` | Add missing path segments (`path`) |
| `PUT` | `/content-explorer/folders/by-id/{id}` | Save name / description / community / locale / properties |
| `POST` | `/content-explorer/folders/move-children` | Multi-child move |
| `POST` | `/content-explorer/folders/add-children` | Multi-child attach |
| `POST` | `/content-explorer/folders/remove-children` | Multi-child detach (`purgeItems` optional) |
| `DELETE` | `/content-explorer/folders/by-id/{id}?purge=false` | Recursive delete; optional purge |

Prefer the generated **OpenAPI** schema for wire field names. Auth and folder ACLs of the underlying
content web service still apply.

### Dual-run mutation flag (Explorer clients — diagnostic)

Content Explorer can optionally route **folder mutations** (create / rename / move / delete)
under **Folders** and **Sites** to this façade while **browse / list / pagination** stay on
pathmanagement. This is a **QA / diagnostic dual-run leftover**, not a production operator
switch. **Do not enable `perc.explorer.rxFolderMutations` in production.** Operators use
the product Explorer URL with the flag **off**.

| Item | Value |
|------|--------|
| Operator property name | `perc.explorer.rxFolderMutations` |
| Default | **off** (pathmanagement only — zero behavior change) |
| Client enable (QA / dual-run only) | URL `?rxFolderMutations=1`, or `sessionStorage` / `localStorage` key `perc.explorer.rxFolderMutations=true`, or `window.__PERC_RX_FOLDER_MUTATIONS__ = true` |
| When on | Mutations under `/Folders` and `/Sites` (and `//Folders` / `//Sites`) use this REST surface. Do **not** treat `/Folders/$System$/Assets` or `//Folders/$System$/Assets` as RX Folders — those are the Assets library. |
| Still on pathmanagement | List/paginate, ACL folder-properties save, non-RX roots (`/Assets`, `/Folders/$System$/Assets`, `//Folders/$System$/Assets`, `/Design`, `/Recycling`, `/Folders/$System$/Recycling`, …) |
| Copy folder | `POST /rest/folders/copy/folder` with JSON root `CopyFolderItemRequest` (`itemPath`, `targetFolderPath`). Explorer Copy / Subfolder Copy must not POST a bare `sourcePath` object to pathmanagement `moveItem` (HTTP 400 unexpected element `sourcePath`). |
| Copy item | `POST /rest/folders/copy/item` with the same JSON root `CopyFolderItemRequest`. Explorer Copy of a selected page, file, or asset must use this endpoint — `copy/folder` 500s for non-folders. Server NewCopy check-in assigns the workflow initial state when the new row has no `CONTENTSTATEID`. |

See also [Content Explorer](id:admin-content-explorer) dual-run notes.

### Migration notes

- Content Explorer **browse / pagination** remains on pathmanagement until a deliberate client
  switch; mutation dual-run is **default off** (`perc.explorer.rxFolderMutations`). Operators
  stay on that default. Do **not** turn the diagnostic flag on in production.
- Do not confuse with foldermanagement workflow assignment (`/services/folders`) or CM1
  `FoldersResource` section APIs.

## Assembly preview location (Explorer)

Explorer **Preview** by **template** (legacy “variant”) uses a public REST location, not
Data Flow `sys_cxSupport/previewslotvariant.html`.

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/services/assembly/preview-location?contentId=&templateId=&revision=` | Build a context-root-relative assembler preview URL |

Response JSON (`PreviewLocation`):

| Field | Meaning |
|-------|---------|
| `previewUrl` | Path + query for `/assembler/render` with `sys_contentid`, `sys_template`, `sys_revision`, `sys_context=0`, `sys_itemfilter=preview` |
| `contentId` | Item id |
| `templateId` | Assembly template id |
| `revision` | Revision used (current revision when the query omits `revision`) |

`400` if `contentId` or `templateId` is missing or not positive. `404` if the item has no
summary/revision. Template **menus** remain `GET /services/actions/find/templates/{id}`. Explorer **Active Assembly** uses the same location with `isAA=true` template menus and opens the chrome-less SPA entry `spa.jsp?entry=assembly&contentId=&templateId=` (client path `/cm/app/assembly`). The AA overlay maps known scalar fields on the assembled HTML and saves with `PUT /services/itemmanagement/item/fields/{id}` (same contract as the React Content Editor). It does not call leftover Active Assembly or Content Editor HTML.

### Active Assembly slot relationships

Slot add / create / arrange use relationship REST, not Data Flow
`variantlistwithslots.html` / `itemassembly.html`.

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/services/assembly/slot-relationships/canvas?ownerId=&templateId=` | Slots on the owner template plus current AA relationships |
| `POST` | `/services/assembly/slot-relationships` | Add an existing item to a slot (`ownerId`, `dependentId`, `slotId`, `templateId`) |
| `DELETE` | `/services/assembly/slot-relationships/{relationshipId}` | Remove the relationship (Arrange Remove) |
| `POST` | `/services/assembly/slot-relationships/{relationshipId}/move` | Move up / down / to an index (`direction`: `UP`, `DOWN`, `INDEX`) |
| `POST` | `/services/assembly/slot-relationships/{relationshipId}/template-slot` | Change snippet template and/or slot |
| `GET` | `/services/assembly/slot-relationships/allowed-types?slotId=` | Content types allowed in the slot (Create) |
| `GET` | `/services/assembly/slot-relationships/allowed-templates?slotId=&contentTypeId=` | Snippet templates allowed in the slot (Add / Create / Change) |

See [Content Explorer](id:admin-content-explorer) server actions.

## Content editor fields (Explorer)

Explorer **Edit** uses itemmanagement field maps (same `PSContentItem` store as dates / copy), not Data Flow Content Editor HTML.

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/services/itemmanagement/item/fields/{id}` | Scalar fields for the React editor (`sys_*` except `sys_title` and `sys_communityid` omitted; binary omitted) |
| `PUT` | `/services/itemmanagement/item/fields/{id}` | Save scalar field updates, including HTML (TinyMCE), keyword values, and community id. Item must be checked out to the current user. |
| `GET` | `/services/itemmanagement/item/binary/{id}/{field}` | Filename / presence metadata for a binary field (file or image). Does not stream bytes. |
| `PUT` | `/services/itemmanagement/item/binary/{id}/{field}` | Multipart upload (`file`) that replaces the binary field. Item must be checked out to the current user. |
| `POST` | `/services/itemmanagement/item/create` | Create an item in a folder. JSON body is `{ "ItemCreateRequest": { contentType, folderPath, optional name, optional templateId } }` (JAXB root). Pages (`percPage`) require `templateId` and save through page management. Home → Create **Asset** and Explorer **New Item** both use this POST, then open `spa.jsp?entry=editor`. |

Checkout / check-in remain `GET /services/itemmanagement/workflow/checkOut/{id}` and `…/checkIn/{id}`. Content-type labels and control names come from `GET /services/contenttypes/{type}`. The React editor maps `sys_tinymce` to TinyMCE, `sys_File` / image controls to file upload, keyword and community names to pickers. The Active Assembly overlay reuses the same `GET`/`PUT` fields endpoints for known scalar text on the assembled preview. Neither host requests leftover Content Editor HTML.

## Assembly cache and navigation

Explorer **Flush Cache** (catalog display name **Refresh Item**) and **Nav Reset** use public
REST POSTs, not Data Flow `sys_uiSupport/flushcache.html` or `rxs_navSupport/navreset.html`.

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/services/assembly/flush-cache` | Flush **all** assembler pages (empty keys, same as `PSExitFlushAssemblerCache` with omitted keys). Not scoped to the selected item. |
| `POST` | `/services/assembly/nav-reset` | Same goal as classic `PSNavReset`. On 8.2 FastForward this is typically a **no-op** once navigation is loaded (`m_allVariants == null`). |

Response JSON (`AssemblyOperationResult`):

| Field | Meaning |
|-------|---------|
| `ok` | `true` when the call completed |
| `message` | Operator-facing status (`Assembler cache flushed` / `Managed navigation reset`) |

`503` if the adaptor is not configured. `500` on unexpected error. Authenticated `/services/assembly`
surface — menu ACL is not re-checked on the POST.

## Item copies and revisions

Explorer **Revisions**, **Audit Trail**, **New Copy**, and **Promotable Version** use
itemmanagement REST (sitemanage `PSItemService`), not Content Editor HTML.

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/services/itemmanagement/item/revisions/{id}` | Revisions plus workflow comments (`RevisionsSummary`) |
| `GET` | `/services/itemmanagement/item/restoreRevision/{id}` | Restore a prior revision (guid must include that revision) |
| `POST` | `/services/itemmanagement/item/newCopy/{id}` | New copy in the item's current folder |
| `POST` | `/services/itemmanagement/item/promotableVersion/{id}` | Promotable version in the item's current folder |

Copy / promotable response JSON (`ItemCopyResult`): `{ itemId, folderPath, promotable }`. Those
POSTs fail if the item has no folder path (`Item has no folder path and cannot be copied.`).

## Item publish now (Explorer)

Explorer **Publish Now** uses the existing sitemanage demand-publish GETs (same as classic Finder).
It does not open `/publisher/demandpublishing`. The operator must select a page or asset
row first — a Sites-folder selection does not publish. A 200 body whose `status` is
`FORBIDDEN`, `BADCONFIG`, `NOSTAGING_SERVERS`, or `INVALID` (plain or wrapped as
`SitePublishResponse`) is a preflight failure, not a started job. Explorer surfaces that
warning in the Server actions error region.

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/services/sitemanage/publish/page/{id}` | Publish Now for a page |
| `GET` | `/services/sitemanage/publish/resource/{id}` | Publish Now for an asset |

See [Content Explorer](id:admin-content-explorer) server actions.

## Testing tips

- Unit-test resources with Mockito and provide Spring test stubs for new adaptor interfaces on the
  rest test classpath.
- Exercise adaptor implementations in sitemanage tests.
- Run **standalone** `mvnw clean install` in each changed module before PR (see root `AGENTS.md`).

## Related

- [Extensions & packages](id:developer-extensions)
- [Build from source](id:developer-build-source)
- [Content Explorer](id:admin-content-explorer)
