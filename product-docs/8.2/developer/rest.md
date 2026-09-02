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

## Locales (design catalog)

CMS locale definitions (Workbench **Locales** / content design) are exposed under `/services/locales`.
The REST layer is a thin contract over the content **design** web service (`IPSContentDesignWs`) —
create, update, and delete use the same design locks and session identity classic tools use (CD-18
write). The singleton **auto-translation set** (locale × content-type rows) is Admin GET/PUT
`/services/locales/auto-translations` (`loadTranslationSettings` / `saveTranslationSettings`, held
design lock released on save).

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/services/locales` | List CMS locales (language string, label, status, base flag) |
| `GET` | `/services/locales/{idOrLang}` | Load one locale by language string (e.g. `en-us`) or numeric locale id |
| `POST` | `/services/locales` | **Admin.** Create a locale (`languageString` and `label` required; optional description, status, `baseLocale`) |
| `PUT` | `/services/locales/{idOrLang}` | **Admin.** Update label / description / status / `baseLocale`. `languageString` is immutable |
| `DELETE` | `/services/locales/{idOrLang}` | **Admin.** Delete a locale (`204` on success) |
| `GET` | `/services/locales/auto-translations` | **Admin.** Load the singleton auto-translation set (locale × content type, plus workflow and community). Returns every existing `PSX_AUTOTRANSLATION` row (not an empty list when rows exist). |
| `PUT` | `/services/locales/auto-translations` | **Admin.** Replace the auto-translation set. Empty list (`[]`) **clears** all rows. Same-user leftover design locks are stolen so a retry after a failed save can succeed; another user's lock is **409** (not **500**). |

### Request / response shape

JSON objects use the `LocaleDetail` / `LocaleSummary` wire types (fields include `id`,
`languageString`, `label`, `description`, `status` (`active` / `inactive`), `baseLocale`,
`hasFormatProfile`, optional `format`, and `designGaps[]` on detail). Auto-translation rows use
`AutoTranslationRow` (`locale`, `contentTypeId` / `contentTypeName`, `workflowId` / `workflowName`,
`communityId` / `communityName`). PUT accepts **name or id** for content type, workflow, and
community. Prefer the generated OpenAPI schema as the integration source of truth.

POST/PUT JSON is wrapped under a `LocaleDetail` root (JAXB/Jackson UNWRAP_ROOT_VALUE). A flat
`{ "label": "..." }` body fails with unexpected element `label`.

Example create body:

```json
{
  "LocaleDetail": {
    "languageString": "fr-ca",
    "label": "French (Canada)",
    "description": "Canadian French",
    "status": "active",
    "baseLocale": false
  }
}
```

Example auto-translation PUT body (JSON array; empty `[]` clears the set):

```json
[
  {
    "locale": "fr-fr",
    "contentTypeName": "percPage",
    "workflowName": "Default Workflow",
    "communityName": "Default"
  }
]
```

### Status codes and authorization

| Status | Typical meaning |
|--------|-----------------|
| `200` | List / get / create / update / auto-translation GET/PUT success |
| `204` | Delete success |
| `400` | Invalid input (missing language string or label, invalid status, immutable language change, unknown auto-translation locale or content type, duplicate locale/content-type row) |
| `403` | Caller is not Admin, or the request has no session/user for the design session |
| `404` | Locale not found |
| `409` | Duplicate language string, design lock held by another user, remaining dependents, or auto-translation set lock conflict |
| `500` | Design service or server failure |
| `503` | Locales or auto-translations adaptor not configured (deployment miswire) |

- Callers must be authenticated. Locale **GET** is a catalog read. Locale **write** and **auto-translation
  GET/PUT** require the **Admin** role and (for writes) a request session and user identity for the
  design web service (same pattern as shared fields / content-type design writes).
- Create/update load or create the locale with a **held design lock** and release it on save. Auto-translation
  PUT uses the same pattern (`loadTranslationSettings` with lock, `saveTranslationSettings` with
  release). There is no separate lock/unlock REST pair on this catalog (unlike content types).
- Format-profile (`RXLOCALEFORMAT`) create/edit remains unsupported (`designGaps` on locale detail).
  Auto-translation configuration is **GET/PUT** `/services/locales/auto-translations`.
  The Developer **Locales** SPA chrome uses that GET/PUT surface to view and replace the
  set (add/remove locale × content-type rows; empty list clears).

### Integrator notes

- After create/update the server reloads the locale so the response includes the assigned `id` and
  current format-profile flag.
- Prefer language string or numeric id for update/delete. Language string is the catalog key and
  cannot be renamed via PUT.
- Deleting a locale that still has dependents is **409** (`ignoreDependencies=false`).
- Auto-translation PUT is a full replace of the singleton set. GET after PUT returns the persisted
  rows (names filled from the locale, content-type, workflow, and community catalogs). Duplicate
  locale/content-type pairs in the request are **400**.
- The Developer SPA Locales editor uses these endpoints; integrators can call the same surface
  without the UI. Auto-translation set editing is not part of this chrome.

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
| Virtual properties | `GET` / `PUT /services/sites/{nameOrId}/virtual` | Virtual Site source bag. PUT JSON is `{ "VirtualSiteProperties": { "sourceKind", "rootPath", "remoteUrl", "branch", "configFile", "siteKey" } }` (Jackson/JAXB root wrap). Allow-listed `sourceKind`: `git-filesystem`, `csv-filesystem`, `sql-database`, `http-json`, `object-storage`, `rss-atom`, `icalendar`, `sitemap-xml` (GET round-trips the kind after PUT). `sitemap-xml` persist is a local sitemap.xml fixture only (leftover `virtual.remoteUrl`, credentials, and cloud URL `rootPath` are **400**; no live crawl). REST **Build** (`POST …/virtual/build`) writes HTML from that local `sitemap.xml` / `sitemap.file` fixture (`pagesWritten > 0`; leftover `virtual.remoteUrl`, credentials, and cloud `rootPath` are **400**; no live crawl). REST **Preview** (`GET …/virtual/preview`) streams last-build local HTML (`available=true`; missing build is `available=false` HTTP 200; leftover `virtual.remoteUrl` and credentials are **400**; no live crawl). REST **Publish** and Developer Sites chrome for `sitemap-xml` stay later slices. SPI/CLI assemble remains `PSVirtualSiteBuildMain … sitemap-xml`. JDBC URL/user/query for `sql-database` live in `_config.yaml` under `rootPath` (in-memory H2 `jdbc:h2:mem:` only; never send passwords on this envelope). `http-json` catalog URL/file live in `_config.yaml` (`http.url` / `http.file` or default `pages.json`); REST persists a portable-safe `rootPath` JSON fixture (no remaining `..`). `object-storage` persists a portable-safe local `rootPath` (cloud URLs and credential properties are **400**; no AWS/IAM/secrets). `rss-atom` persists a portable-safe local `rootPath` (local/loopback only; leftover `virtual.remoteUrl`, credentials, and cloud URL `rootPath` are **400**; no live feed credentials). `icalendar` persists a portable-safe local `rootPath` (local RFC 5545 `.ics` fixture only; leftover `virtual.remoteUrl`, credentials, and cloud URL `rootPath` are **400**; no CalDAV). REST **Build** (`POST …/virtual/build`) writes HTML from that local `calendar.ics` / `icalendar.file` fixture (`pagesWritten > 0`). REST **Preview** streams last-build HTML (`available=true`; missing build is `available=false` HTTP 200). REST **Publish** copies assembled HTML to `IPSSite.root`. Developer Sites can save `icalendar` and then **Build Virtual Site**, **Preview assembled site**, and **Publish Virtual Site**. Unknown kinds and `csv-filesystem` / `sql-database` / `http-json` / `object-storage` / `rss-atom` / `icalendar` / `sitemap-xml` + `remoteUrl` return **400** (no secrets on this envelope). A flat `{ "sourceKind": … }` body returns **400** unexpected element `sourceKind`. Optional `remoteUrl` + `branch` clone/fetch before Git Build; omit `remoteUrl` to keep a stored remote; send `""` to clear. Developer Sites can save `http-json` and then **Build Virtual Site**, **Preview assembled site**, and **Publish Virtual Site**. Developer Sites can save `object-storage` (GET round-trips the kind) and then **Build Virtual Site**, **Preview assembled site**, and **Publish Virtual Site**. REST **Publish** (`POST …/virtual/publish`) copies assembled HTML to `IPSSite.root` for a local object-key `rootPath` (`virtual.remoteUrl` is **400**) and for `rss-atom` (local RSS/Atom fixture; leftover `virtual.remoteUrl` and credentials are **400**). REST Preview streams last-build HTML after REST or in-product Build, including `object-storage`, `rss-atom`, and `icalendar`. |
| Virtual build | `POST /services/sites/{nameOrId}/virtual/build` | Admin-only; `git-filesystem`, `csv-filesystem`, `sql-database`, `http-json`, `object-storage`, `rss-atom`, `icalendar`, or `sitemap-xml`. Git fetches `remoteUrl` when set; CSV reads a local CSV tree (`rootPath`; optional `_config.yaml`); SQL reads required `_config.yaml` `sql:` mapping (in-memory H2 `jdbc:h2:mem:` only; Oracle/MySQL/SQL Server URLs **400**). HTTP JSON reads a local JSON fixture or loopback catalog from required `_config.yaml` (`http.url` / `http.file` / default `pages.json`); `virtual.remoteUrl` is **400**. Object-storage reads a local object-key bucket (`rootPath`; required `_config.yaml`; optional `objects.keys`); `virtual.remoteUrl` is **400** (no cloud URLs or credentials). `rss-atom` reads a local RSS 2.0 / Atom fixture (`feed.xml` / `atom.xml` or `_config.yaml` `rss.file`; `rss.url` loopback only); leftover `virtual.remoteUrl`, credentials, and cloud `rootPath` are **400** (no live remote feeds). `icalendar` reads a local RFC 5545 fixture (`calendar.ics` or `_config.yaml` `icalendar.file`); leftover `virtual.remoteUrl`, credentials, and cloud `rootPath` are **400** (no CalDAV). `sitemap-xml` reads a local sitemap.xml fixture (`sitemap.xml` or `_config.yaml` `sitemap.file`; urlset of portable files under `rootPath`); leftover `virtual.remoteUrl`, credentials, and cloud `rootPath` are **400** (no live crawl). Each Build re-reads the current fixture and `_config.yaml` (no JVM restart; no file watchers). Same action as **Developer → Sites → Build Virtual Site** after save for Git, CSV, SQL, HTTP JSON, Object storage, RSS / Atom, and iCalendar (`sitemap-xml` REST Build is available; Developer Sites chrome stays a later slice). Unknown `sourceKind` is **400**. |
| Virtual preview status | `GET /services/sites/{nameOrId}/virtual/preview` | Admin-only; last-build availability for `git-filesystem`, `csv-filesystem`, `sql-database`, `http-json`, `object-storage`, `rss-atom`, `icalendar`, and `sitemap-xml` (not git-only). After a successful Build, `http-json`, SQL, `object-storage`, `rss-atom`, `icalendar`, and `sitemap-xml` return `available=true` + `homePath`. `rss-atom` is a local RSS 2.0 / Atom fixture or loopback feed (no live remote feeds). `icalendar` is a local RFC 5545 `calendar.ics` fixture (no CalDAV). `sitemap-xml` is last-build local HTML only (`sitemap.xml` / `sitemap.file`; leftover `virtual.remoteUrl` and credentials are **400**; no live crawl). Developer Sites **Preview assembled site** uses this last-build status for `rss-atom` and `icalendar` after Build (`sitemap-xml` Preview chrome stays a later slice). Missing build → `available=false` (HTTP 200). Repository / unknown `sourceKind` → 400. CLI assemble is previewable only at the default output root (no last-output pointer). |
| Virtual preview file | `GET /services/sites/{nameOrId}/virtual/preview/{relPath}` | Admin-only; assembled file stream from last output (`git-filesystem`, `csv-filesystem`, `sql-database`, `http-json`, `object-storage`, `rss-atom`, `icalendar`, `sitemap-xml`). `sitemap-xml` is last-build local HTML only (no live crawl). Traversal (`../`) or file over 20 MB → 400; missing file → 404 (not 500) |
| Virtual publish | `POST /services/sites/{nameOrId}/virtual/publish` | Admin-only; `git-filesystem`, `csv-filesystem`, `sql-database` (in-memory H2 `jdbc:h2:mem:` only; Oracle/MySQL/SQL Server **400**), `http-json` (local JSON fixture or loopback catalog; catalog URL/file stay in `_config.yaml`; `virtual.remoteUrl` is **400**; no secrets on the envelope), `object-storage` (portable-safe local object-key `rootPath`; no cloud URLs, IAM, or access keys; leftover `virtual.remoteUrl` is **400**), `rss-atom` (local RSS 2.0 / Atom fixture or loopback feed; `feed.xml` / `atom.xml` or `_config.yaml` `rss.file`; leftover `virtual.remoteUrl` and credentials are **400**; no live feeds), or `icalendar` (local RFC 5545 `calendar.ics` / `icalendar.file`; leftover `virtual.remoteUrl` and credentials are **400**; no CalDAV). Builds then copies assembled files to the Site filesystem root (`IPSSite.root`). No `outputRoot` body. Same action as **Developer → Sites → Publish Virtual Site** for Git, CSV, SQL, HTTP JSON, Object storage, RSS / Atom, and iCalendar after Build. Repository / unknown kinds are **400**. |

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
| `GET` | `/services/templates/{idOrName}/export` | **Admin.** AS-08 export: Workbench-equivalent design XML (no lock steal). Import is not on this path |
| `PUT` | `/services/templates/{idOrName}` | Update label, description, templateSource, assembler, bindings, and/or slots |
| `POST` | `/services/templates` | Create a modern assembly template (**when installed**) — no Widget XML |
| `DELETE` | `/services/templates/{idOrName}` | Delete a modern assembly template — no Widget XML |
| `POST` | `/services/templates/import` | **Admin.** Import one Workbench-equivalent `assembly-template` design XML (AS-08) |
| `POST` | `/services/templates/summaries-by-filter` | List summaries matching a `TemplateFilter` |

`PUT` omits unchanged fields. Name/id remain unsupported. Delete returns **204** when
the template is removed and **404** when it is not found. Lock remains unsupported
(`designGaps` code `TPL_LOCK`).
Create (`POST /services/templates`) is the Design **Create template** contract when that
slice is on the server; otherwise create stays on residual classic hosts.

**AS-08 export:** `GET /services/templates/{idOrName}/export` downloads Workbench-equivalent
design XML (Admin only). Unknown templates are `404`; non-Admin callers are `403`. The
server does not steal design locks. Import uses a separate path
(`POST /services/templates/import`). Details are in the Templates (assembly catalog)
section below.

### AS-08 template design XML import

`POST /services/templates/import` is the Admin REST equivalent of the Workbench **import
template** wizard for **one** assembly-template design document. The body is
`application/xml` (or `text/xml`) — the same `<assembly-template>` document that Workbench
exports and that `GET /services/templates/{idOrName}/export` returns when that export
slice is installed. Load and save go through existing `IPSAssemblyDesignWs`
(`createAssemblyTemplates` then `saveAssemblyTemplates` with `release=true`). No new SOAP
surface.

**Admin (Design) only.** There is no global JAX-RS Admin filter on this path — the
sitemanage adaptor checks `IPSUserService.isAdminUser` and maps a non-Admin caller to
**403**.

**Create only — name collision is 409.** Import does **not** replace an existing template
and does **not** steal a design lock held by another session. The new object is locked
only for the importing user long enough to persist, then released. There is no overwrite
query parameter on this path.

| Status | Meaning |
|--------|---------|
| `200` | Imported. JSON `TemplateDetail`; `name` round-trips from the XML |
| `400` | Missing body, not `assembly-template` XML, or invalid name in the document |
| `403` | Caller is not Admin, or request session/user is missing |
| `409` | A template with that name already exists (no replace) |
| `500` | Unexpected server failure |

Example:

```http
POST /Rhythmyx/rest/templates/import
Content-Type: application/xml
Authorization: …

<assembly-template>
  <name>imported.one</name>
  <label>Imported One</label>
  <assembler>Java/global/percussion/assembly/htmlAssembler</assembler>
  <template>#set($x=1)$x</template>
  …
</assembly-template>
```

Widget definition XML / package compile is out of scope. The **Developer → Templates**
catalog exposes create-only **Import XML** (and detail **Export XML**). There is no
Design SPA import wizard — operators can use Developer Templates, this REST path, or
Workbench. See [Developer Templates](id:admin-developer-templates).

## Slots (design catalog)

Assembly **slots** used by **Developer → Slots** are exposed under `/services/slots`.

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/services/slots` | List slot summaries (label, name, description) |
| `GET` | `/services/slots/{idOrName}` | Design detail (finder, associations, `designGaps`) |
| `PUT` | `/services/slots/{idOrName}` | Update label, description, layout/styles, associations, and (Admin, held lock) `finderName` / `relationshipName` / `finderArguments` |
| `POST` | `/services/slots/{idOrName}/lock` | **Admin.** Acquire a design-session lock (does not steal) |
| `POST` | `/services/slots/{idOrName}/unlock` | **Admin.** Release a lock owned by the current user/session |
| `POST` | `/services/slots` | **Admin.** Create a slot (`IPSAssemblyDesignWs.createSlots` then `saveSlots`) |
| `DELETE` | `/services/slots/{idOrName}` | **Admin.** Delete a slot (`IPSAssemblyDesignWs.deleteSlots`) |

Create (`POST /services/slots`) persists immediately (Workbench Finish, not an unsaved stub).
JSON body requires `name` (unique, case-insensitive; **no whitespace**). Optional `label`,
`description`, and `slotType` (`REGULAR` or `INLINE`) are applied before save. Omitted
`slotType` defaults to `REGULAR`. Duplicate name is **409**. Blank / whitespace / wildcard
names and invalid `slotType` are **400**. Missing request session/user is **403**. Non-Admin
is **403**. The new slot is then `GET /services/slots/{name}` **200**.

Delete (`DELETE /services/slots/{idOrName}`) returns **204** when removed; a following
`GET` is **404**. Unknown id/name is **404**. System slots cannot be deleted (**409**).
Locked-by-another-user is **409** (the lock is not stolen). Non-Admin is **403**.

**Finder / relationship write (AS-01):** Admin `PUT /services/slots/{idOrName}` writes
`finderName`, `relationshipName`, and `finderArguments` when those JSON fields are present
(null omits them; empty `relationshipName` or empty `finderArguments` clears). Typical
flow: **lock → PUT (repeatable) → unlock**. The PUT does **not** acquire or release the
lock and does **not** steal another user's lock. Invalid finder extension is **400**.
Unknown relationship type is **400**. Unknown slot is **404**. Unlocked or locked-by-another
user is **409**. Non-Admin is **403**. Following `GET /services/slots/{idOrName}` round-trips
the written finder, relationship, and arguments. **Developer → Slots** catalog
create and non-system delete use `POST /services/slots` and
`DELETE /services/slots/{idOrName}`. Finder / relationship / arguments are
editable on slot detail after **Lock** — see [Developer Slots](id:admin-developer-slots).
Detail `designGaps` no longer includes `SLOT_FINDER_RELATIONSHIP_WRITE`
(`SLOT_CREATE_DELETE` is already retired). Remaining gap `SLOT_ASSOC_GUIDS_ONLY`
records GUID-only associations.

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

## Communities (design catalog)

CMS **communities** used by **Developer → Communities** are exposed under
`/services/communities`. Create and delete reuse the existing bulk design
surface (`ICommunityAdaptor.createCommunities` / `saveCommunities` /
`deleteCommunities`). Do not invent a second REST resource.

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/services/communities/find?name=*` | List community summaries |
| `GET` | `/services/communities/{idOrName}` | Detail with role membership |
| `POST` | `/services/communities/bulk` | **Admin.** Create from a name list (`{"List":["Name"]}`); server persists |
| `PUT` | `/services/communities/bulk` | **Admin.** Persist edited communities (`release` header) |
| `PUT` | `/services/communities/{idOrName}/roles` | Replace role membership |
| `DELETE` | `/services/communities/bulk` | **Admin.** Delete by GuidList (`ignoredependencies` header) |

Create (`POST /services/communities/bulk`) persists on the server (Workbench
Finish create+save). JSON create body is a name list. The SPA create path does
not PUT the DTO back. Blank / whitespace-only names are **400**. Duplicate name
(case-insensitive) is **409**. Non-Admin is **403**. The new community is then
`GET /services/communities/find?name=*` **200**.

Delete (`DELETE /services/communities/bulk`) accepts a GuidList. The SPA sends
`ignoredependencies=false`. Success omits the community from a following find.
Missing is **404**. In-use (dependencies) without ignore is **409** and the
community remains (the lock is not stolen). Non-Admin is **403**.

**Developer → Communities** catalog create and delete use these bulk endpoints.
Role-association save stays `PUT /services/communities/{idOrName}/roles`. See
[Developer Communities](id:admin-developer-communities).

## Item filters (design catalog)

Assembly **item filters** (Workbench **Item Filter** editor: name / description / rules /
parent filter) are exposed under `/services/itemfilters` (AS-07). The REST layer is a
thin contract over the system **design** web service (`IPSSystemDesignWs`) — create,
update, and delete use the same `createItemFilters` / `loadItemFilters` /
`saveItemFilters` / `deleteItemFilters` operations SOAP uses. There is no new SOAP
surface. GET list/detail remain a catalog read of the filter service.

Workbench catalog **label** is an alias of **name** (the unique catalog key).

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/services/itemfilters` | List item filters (name, description, rules, parent) |
| `GET` | `/services/itemfilters/{idOrName}` | Load one filter by unique name or GUID string (`type-host-uuid`) |
| `POST` | `/services/itemfilters` | **Admin.** Create a filter (`createItemFilters` then `saveItemFilters`) |
| `PUT` | `/services/itemfilters/{idOrName}` | **Admin.** Update description, rules, parent filter, and/or `legacyAuthtype` |
| `DELETE` | `/services/itemfilters/{idOrName}` | **Admin.** Delete a filter (`deleteItemFilters`, `ignoreDependencies=false`) |

Create (`POST /services/itemfilters`) persists immediately (Workbench Finish, not an
unsaved stub). JSON body requires `name` (unique, case-insensitive; **no whitespace** or
wildcards). Optional `description`, `rules[]` (`name` + `params[]` of `name`/`value`),
`parentFilter` (by nested `name` or `filterId`), and `legacyAuthtype` are applied before
save. Duplicate name is **409**. Blank / whitespace / wildcard names are **400**. Missing
request session/user is **403**. Non-Admin is **403**. The new filter is then
`GET /services/itemfilters/{name}` **200**.

Update (`PUT /services/itemfilters/{idOrName}`) loads with a design lock and releases it
on save. Name is not renamed on PUT. Omitted `rules` / `parentFilter` leave the stored
values unchanged; send `parentFilter` with no name or id to clear the parent; send
`rules: []` to clear rules.

Delete (`DELETE /services/itemfilters/{idOrName}`) returns **204** when removed; a
following `GET` is **404**. Unknown id/name is **404**. A filter still associated with a
content list (or other dependents) is **409**. Locked-by-another-user is **409** (the
lock is not stolen). Non-Admin is **403**.

**Developer → Item Filters** chrome uses list, load, create, save, and delete
(see [Developer Item Filters](id:admin-developer-item-filters)). Rule rows on
detail are read-only in the SPA and round-tripped on save.

### Request / response shape

JSON objects use the `ItemFilter` wire type (fields include `filterId`, `name`,
`description`, `legacyAuthtype`, `rules[]`, and nested `parentFilter`). Prefer the
generated OpenAPI schema as the integration source of truth.

POST/PUT JSON is wrapped under an `ItemFilter` root (JAXB/Jackson
UNWRAP_ROOT_VALUE). A flat `{ "name": "..." }` body fails with unexpected
element `name`.

Example create body:

```json
{
  "ItemFilter": {
    "name": "previewPublic",
    "description": "Public preview items",
    "parentFilter": { "name": "public" },
    "rules": [
      {
        "name": "sys_filterByPublishDate",
        "params": [{ "name": "maxAge", "value": "30" }]
      }
    ]
  }
}
```

### Status codes and authorization

| Status | Typical meaning |
|--------|-----------------|
| `200` | List / get / create / update success |
| `204` | Delete success |
| `400` | Invalid input (missing name, whitespace/wildcard name, unknown parent, invalid rule) |
| `403` | Caller is not Admin, or the request has no session/user for the design session |
| `404` | Item filter not found |
| `409` | Duplicate name, design lock held by another user, or in-use (content-list dependents) |
| `500` | Design service or server failure |

- Callers must be authenticated. Item-filter **GET** is a catalog read. Item-filter
  **write** requires the **Admin** role and a request session and user identity for the
  design web service (same pattern as slots / locales design writes).
- Create/update load or create the filter with a **held design lock** and release it on
  save. There is no separate lock/unlock REST pair on this catalog.

## Content types (design catalog)

| Operation | Path | Notes |
|-----------|------|--------|
| List | `GET /services/contenttypes` | Name, label, description, guid |
| Create | `POST /services/contenttypes` | **Admin.** Creates and **saves** a content type (`IPSContentDesignWs.createContentTypes` then `saveContentTypes` — Workbench Finish, not an unsaved stub). JSON body requires `name` (unique, case-insensitive; no spaces). Optional `label`, `description`, and `enabled` are applied before save. Omitted `enabled` **defaults to true** so the new type is usable. `200` + `ContentTypeDetail`. The new type is then `GET /services/contenttypes/{name}` **200**. Create locks the new type with the same packed NODEDEF design GUID that save looks up (the default editor template's `contentType="0"` is replaced with the assigned type id; the create lock is not stolen from an existing type). Duplicate name is **409** (catalog check and persist-time unique-name failure), including reserved system types such as **Folder**. Blank / whitespace / wildcard names are **400**. Missing request session/user is **403**. Non-Admin is **403**. Rename uses `PUT .../name`. Delete uses `DELETE .../{idOrName}`. |
| Import design XML | `POST /services/contenttypes/import` | **Admin** (CD-14). Create-only import of one Workbench-equivalent `ItemDefData` content-type design XML (`application/xml` or `text/xml`). Same document as Workbench export and as `GET /services/contenttypes/{idOrName}/export`. Persist via `IPSContentDesignWs.createContentTypes` then `saveContentTypes` with `release=true`. Duplicate name is **409** (no replace). Invalid XML is **400**. Non-Admin is **403**. Does **not** steal locks on existing types. The imported type is then `GET /services/contenttypes/{name}` **200**. |
| Detail | `GET /services/contenttypes/{idOrName}` | Field catalog, associations, `enabled`, `designGaps` |
| Export | `GET /services/contenttypes/{idOrName}/export` | **Admin.** CD-14 export of Workbench-equivalent design XML (`IPSContentDesignWs.loadContentTypes` with `lock=false`, `overrideLock=false` — the lock is **not** stolen). `Content-Type: application/xml` and `Content-Disposition: attachment` with a filename derived from the **type name** (for example `percPage.xml`). Path separators and Windows-invalid characters (`* ? < > \| :`) are replaced with `_`. The header includes an ASCII `filename` fallback and RFC 5987 `filename*` for non-ASCII names. Unknown id/name is **404**. Non-Admin is **403**. Import is a separate `POST /services/contenttypes/import`. Developer **Content types** detail chrome exposes **Export XML** for this path. |
| Allowed templates | `GET /services/contenttypes/{idOrName}/allowedTemplates` | Read-only list of associated templates (CD-12). No lock required. Empty list means none. Same set as `ContentTypeDetail.allowedTemplates`. |
| Item-level exits | `GET /services/contenttypes/{idOrName}/itemExits` | Item-level input/output translations, validations, and pipe pre/post exits (CD-09). No lock required. Empty lists mean none. Apply-when conditions are a read-only summary. Jackson root wrap is `ContentTypeItemExits`. |
| Replace item-level exits | `PUT /services/contenttypes/{idOrName}/itemExits` | Full replace of item-level translations/validations via `IPSContentDesignWs.saveContentTypes` (CD-09). Requires a **held** design-session lock. Empty lists clear. **409** if unlocked or locked by another user. **400** if required lists are missing or an extension FQN is invalid. `preExits`/`postExits` omitted leave pipe extensions unchanged. Apply-when is not written. Does not acquire or release the lock. |
| Replace allowed templates | `PUT /services/contenttypes/{idOrName}/allowedTemplates` | Full replace of associated templates (CD-12). Requires a **held** design-session lock. Empty list clears associations. **409** if unlocked or locked by another user. **400** if a template name/guid cannot be resolved. Does not acquire or release the lock. |
| Lock | `POST /services/contenttypes/{idOrName}/lock` | **Admin.** Self-only design-session lock (`IPSContentDesignWs.loadContentTypes` with `lock=true`, `overrideLock=false`). Does **not** save. `200` + `ObjectLockSummary` (`session`, `locker`, `remainingTime` minutes from the lock service). Locks expire after **30 minutes** (`PSObjectLock.LOCK_INTERVAL`). Re-lock by the same session user extends the lock. |
| Save | `PUT /services/contenttypes/{idOrName}` | **Admin.** Requires a lock already held by the current user. Saves label, description, enabled, per-field searchable/occurrence, workflows, and templates. Does **not** change name (use `PUT .../name`). Does **not** release the lock. POST `/lock` and PUT share the packed NODEDEF design-object id so a lock you hold is found on save. The save load (`lock=true`) **extends** a still-valid lock; a PUT after expiry returns `409` and the client must re-lock. Field rule expressions use the dedicated path below (not this PUT). |
| Allowed workflows | `PUT /services/contenttypes/{idOrName}/allowedWorkflows` | **Admin** (CD-08 design action). Requires a held design-session lock (`POST .../lock` first). Does **not** acquire or release the lock. Full replace of `allowedWorkflows` (empty list clears). Optional `defaultWorkflow`. Workflow name/guid must exist. `200` + `ContentTypeDetail` with the new `allowedWorkflows` / `defaultWorkflow` (lock still held). |
| Enable/disable | `PUT /services/contenttypes/{idOrName}/enabled` | **Admin** (CD-13 design action). Requires a held design-session lock — `POST .../lock` first, then `PUT .../enabled`, then `POST .../unlock` when done. Does **not** acquire or release the lock. `200` + `ContentTypeDetail` with the new `enabled` value (lock still held). |
| Search indexing | `GET` / `PUT /services/contenttypes/{idOrName}/searchIndexing` | **Admin** PUT (CD-10). Type-level search indexing — Workbench Properties **Enable searching for this Content Type** (root field-set `isUserSearchable`). **Default is on.** Distinct from per-field `searchable` on PUT detail. GET does not require a lock. PUT requires a **held** design-session lock and does **not** acquire or release it. Missing `searchIndexing` boolean is **400**. Jackson root wrap is `ContentTypeSearchIndexing`. The Developer SPA Content type detail **Search indexing** checkbox uses this surface after lock. |
| Icon strategy | `GET /services/contenttypes/{idOrName}/icon` | Content type icon source and value (CD-11). No lock required. `source` is `none`, `specified` (file path/name), or `fromFileField` (file field name). `none` has no value. Does **not** return icon binaries. Jackson root wrap is `ContentTypeIcon`. |
| Set icon strategy | `PUT /services/contenttypes/{idOrName}/icon` | **Admin** (CD-11 design action). Requires a held design-session lock — `POST .../lock` first, then `PUT .../icon`, then `POST .../unlock` when done. Does **not** acquire or release the lock. `none` clears value. Non-`none` with a blank value is **400**. Invalid `source` is **400**. Does **not** upload icon binaries. `200` + `ContentTypeIcon` (lock still held). |
| Rename | `PUT /services/contenttypes/{idOrName}/name` | **Admin** (CD-01). Requires a **held** design-session lock. Sets the internal name. Unique (case-insensitive); **no spaces** or wildcards. Bulk `PUT .../{idOrName}` does **not** change name. After success, `GET` by the previous name is **404**; `GET` by id returns the new name. Does not acquire or release the lock. |
| Add local field | `POST /services/contenttypes/{idOrName}/fields` | **Admin** (CD-03). Requires a **held** design-session lock. Adds a persistable **local** field (backend column + display mapping). The backend column is created (`ALTER TABLE … ADD COLUMN`) **before** `IPSContentDesignWs.saveContentTypes` re-inits the content editor application. Body `name` is required (letter, then letters/digits/underscore; unique case-insensitive on the type). Optional `dataType` defaults to `text`. Optional `searchable` and `occurrence`/`required` use the same rules as PUT field patches. Optional `fieldSet` names an existing child field set, or **creates** a named complex child when missing. Duplicate field is **409**. Unlocked (or locked by another session) is **409**. Include an existing system or shared field with `POST .../fields/include` (CD-04). Does not acquire or release the lock. |
| Include system/shared field | `POST /services/contenttypes/{idOrName}/fields/include` | **Admin** (CD-04). Requires a **held** design-session lock (`POST .../lock` first; the lock is not stolen). Includes an existing **system** or **shared** field by `name` and `fieldType` (`system` or `shared`). Origin stays system/shared (not copied as local). Persist via `IPSContentDesignWs.loadContentTypes` / `saveContentTypes`. Duplicate include is **409**. Unknown catalog field is **404**. Invalid `fieldType` (including `local`) is **400**. Does not acquire or release the lock. The Developer SPA Content types detail chrome exposes this picker after **Lock**. |
| Delete local field | `DELETE /services/contenttypes/{idOrName}/fields/{fieldName}` | **Admin** (CD-03). Requires a **held** design-session lock. Removes a **local** field and its display mapping. System/shared fields are **400**. Missing type or field is **404**. Does not acquire or release the lock. `204` on success (lock still held). |
| Field control properties | `GET /services/contenttypes/{idOrName}/fields/{fieldName}/controlProperties` | Control parameter **name/value** pairs and the choice catalog for one field (CD-07). No lock required. Empty `properties` means none. `choices` omitted when none. GET round-trips `filter`, `nullEntry`, and `defaultSelected` when present. |
| Replace field control properties | `PUT /services/contenttypes/{idOrName}/fields/{fieldName}/controlProperties` | **Admin** (CD-07). Requires a **held** design-session lock. Full replace of `properties` (empty clears). `choices` omitted leaves the catalog unchanged; present replaces including choice filter, null-entry, and default-selected; `type: none` clears. **409** if unlocked or locked by another user. Does not acquire or release the lock. |
| Field rule expressions | `GET /services/contenttypes/{idOrName}/fields/{fieldName}/ruleExpressions` | Field-level validation, visibility, and input/output translation expressions (CD-05–07). No lock required. Empty lists mean none. Unknown field is **404**. Jackson root wrap is `ContentTypeFieldRuleExpressions`. |
| Replace field rule expressions | `PUT /services/contenttypes/{idOrName}/fields/{fieldName}/ruleExpressions` | **Admin** (CD-05–07). Requires a **held** design-session lock. Full replace of `validation`, `visibility`, `inputTranslation`, and `outputTranslation` (empty lists clear). Unknown field names are **400**. **409** if unlocked or locked by another user. Does not acquire or release the lock. |
| Unlock | `POST /services/contenttypes/{idOrName}/unlock` | **Admin.** Releases a lock owned by the current session user (Workbench `releaseLocks`). Does **not** save. `204` on success. |
| Delete | `DELETE /services/contenttypes/{idOrName}` | **Admin.** Requires a **held** design-session lock (`POST .../lock` first). Calls `IPSContentDesignWs.deleteContentTypes` with `ignoreDependencies=false`. **204** on success; a following `GET .../{idOrName}` is **404**. **409** if unlocked or locked by another user (the lock is not stolen). **404** if missing. **400** if the design web service rejects an in-use type (dependents). Does **not** cascade item delete. |

Typical design-session flow: **lock → PUT save (repeatable) → unlock**. Existing PUT clients that previously lock-save-unlocked in a single request must now `POST .../lock` before PUT and `POST .../unlock` after. **Create** is a separate `POST /services/contenttypes` (persisted immediately). **Import** is a separate `POST /services/contenttypes/import` (create-only XML; persisted immediately; the new object's create lock is released). The Developer SPA **Content types** catalog exposes **New content type** (`POST /services/contenttypes`) and **Import XML** (`POST /services/contenttypes/import`). Detail chrome exposes **Lock**, **Save**, **Unlock**, **Export XML**, **Rename content type**, and **Delete content type** (held lock) for that flow — see [Developer Content Types](id:admin-developer-content-types). Enable/disable from that chrome uses the dedicated `PUT .../enabled` after a held lock (not the bulk content-type PUT). Type-level search indexing uses the dedicated `GET`/`PUT .../searchIndexing` after a held lock (CD-10; the SPA **Search indexing** checkbox; not per-field `searchable`). Icon strategy uses the same lock → save → unlock chrome: **lock → PUT .../icon → unlock** (SPA Properties picker; no binary upload). Control property **values** and the field **choice catalog** use `GET`/`PUT .../fields/{fieldName}/controlProperties` after a held lock (CD-07). The Developer SPA sends `choices` when the catalog changed (`type: none` clears); a properties-only save omits `choices` so the catalog is not wiped. Local field create/delete from Developer Content Type detail is **lock → POST .../fields** or **DELETE .../fields/{fieldName} → unlock** (origin always `local`; see [Developer Content Types](id:admin-developer-content-types)). Include system/shared uses **lock → POST .../fields/include → unlock**; the Developer SPA Content types detail chrome exposes that picker after a held lock. **CD-14 export/import** is also on Developer **Content types** chrome: detail **Export XML** (`GET .../export`) and catalog **Import XML** (`POST .../import`, create-only; unique name; **400** invalid XML; **409** duplicate). SPA **rename** uses **lock → PUT .../name → GET new 200 / old 404** from detail chrome (bulk PUT still does not rename). SPA catalog **create** uses `POST /services/contenttypes`. SPA **delete** uses **lock → DELETE → GET 404** from detail chrome.

Lock / save / unlock / create / import / rename / delete status codes:

| Status | Typical meaning |
|--------|-----------------|
| `200` | Lock acquired (body is `ObjectLockSummary`), PUT save / enable / disable / icon / rename succeeded (lock still held), POST create or POST import succeeded (`ContentTypeDetail`), POST local field succeeded, POST include field succeeded, or GET export returned design XML |
| `204` | Unlock success, content type deleted, or local field deleted |
| `400` | Invalid PUT body (unknown field name, bad workflow/template ref, missing `enabled` or `searchIndexing` flag, invalid icon `source` or blank non-none icon `value`, invalid or colliding rename), invalid create name (blank, spaces, wildcard), invalid or missing `ItemDefData` import XML, invalid local-field name/`dataType`/origin, invalid include `fieldType`/`name`, DELETE field of a system/shared field, or DELETE type rejected because the type has dependents |
| `403` | Caller is not Admin, or the request has no session/user for the design session |
| `404` | Content type not found, or include of an unknown system/shared catalog field |
| `409` | No lock held, or locked by another user/session (self-only; the lock is not stolen); POST create or POST import duplicate name (catalog or persist-time), including reserved system types such as Folder; POST local field when the field name already exists on the type; POST include when the field is already on the type |
| `500` | Design service or server failure |

### Content type design XML export (CD-14)

`GET /services/contenttypes/{idOrName}/export` is the Workbench **export content type**
equivalent: it returns the design-object XML for one content type loaded through the
existing content design web service (`IPSContentDesignWs`). The download uses
`Content-Type: application/xml` and `Content-Disposition: attachment` with a filename
derived from the **content type name** (for example `percPage.xml`). Characters that
Windows Explorer rejects in a download name (`* ? < > | :` plus path separators) are
replaced with `_`. The header always includes an ASCII `filename=` fallback and an
RFC 5987 `filename*=UTF-8''…` parameter so non-ASCII type names decode in browsers.

| Status | Meaning |
|--------|---------|
| `200` | Design XML body |
| `403` | Caller is not Admin |
| `404` | Unknown id or name |

The load is **read-only**. The server does **not** acquire or steal a design lock
(`lock=false`, `overrideLock=false`). **Import** of the same XML is a separate
`POST /services/contenttypes/import` (create-only; see below). Operators can also
**Export XML** from Developer **Content types** detail chrome — see
[Developer Content Types](id:admin-developer-content-types).

See also [Developer Content Types](id:admin-developer-content-types).

### Import content-type design XML (CD-14)

`POST /services/contenttypes/import` is the Admin REST equivalent of the Workbench
**import content type** wizard for **one** content-type design document. The body is
`application/xml` (or `text/xml`) — the same `<ItemDefData>` document that Workbench
exports and that `GET /services/contenttypes/{idOrName}/export` returns. Load and save
go through existing `IPSContentDesignWs` (`createContentTypes` then `saveContentTypes`
with `release=true`). No new SOAP surface.

**Admin (Design) only.** There is no global JAX-RS Admin filter on this path — the
sitemanage adaptor checks `IPSUserService.isAdminUser` and maps a non-Admin caller to
**403**.

**Create only — name collision is 409.** Import does **not** replace an existing
content type and does **not** steal a design lock held by another session. The
imported document's type id is remapped to the GUID allocated by create so save
uses the new object's lock. The new object is locked only for the importing user
long enough to persist, then released. There is no overwrite query parameter on
this path. Developer **Content types** catalog chrome exposes the same import
(**Import XML**, optional unique name) — see
[Developer Content Types](id:admin-developer-content-types).

| Status | Meaning |
|--------|---------|
| `200` | Imported. JSON `ContentTypeDetail`; `name` round-trips from the XML. `GET /services/contenttypes/{name}` is then 200 |
| `400` | Missing body, not `ItemDefData` XML, or invalid name in the document |
| `403` | Caller is not Admin, or request session/user is missing |
| `409` | A content type with that name already exists (no replace) |
| `500` | Unexpected server failure |

Example:

```http
POST /Rhythmyx/rest/contenttypes/import
Content-Type: application/xml
Authorization: …

<ItemDefData appName="psx_ceimportedOne" isHidden="false" objectType="1">
  <PSXItemDefSummary name="importedOne" label="Imported One" … />
  …
</ItemDefData>
```

There is no Developer SPA import wizard on this slice — operators and integrators
call the REST path (or Workbench). Template AS-08 import is a separate path
(`POST /services/templates/import`).

### Rename (CD-01)

`PUT /services/contenttypes/{idOrName}/name` changes the content type **internal
name**. This is a dedicated design action. Bulk `PUT /services/contenttypes/{idOrName}`
does **not** rename. Hold the design-session lock first; the rename keeps the lock
so you can continue editing, then unlock. Developer Content Type detail exposes
this as **Rename content type** after **Lock**.

Typical flow: `POST .../lock` → `PUT .../name` → (optional further design writes)
→ `POST .../unlock`.

The new name must be unique (case-insensitive) and must not contain spaces or
wildcards (`*` / `%`). Allowed characters are letters, digits, underscore, and
dot. A colliding or invalid name is **400**. An unlocked type (or a lock held by
another user) is **409** — the lock is not stolen.

After a successful rename:

* `GET /services/contenttypes/{id}` returns `200` with the new `name`.
* `GET /services/contenttypes/{oldName}` returns **404**.

Jackson root wrap:

```json
{
  "ContentTypeName": {
    "name": "percRenamedPage"
  }
}
```

### Local fields (CD-03)

`POST /services/contenttypes/{idOrName}/fields` adds a **local** field to an
existing content type. `DELETE /services/contenttypes/{idOrName}/fields/{fieldName}`
removes one. Both require a **held** design-session lock (`POST .../lock` first).
The lock is **not** released. Including system or shared fields into a type
uses `POST .../fields/include` (CD-04).

Typical flow: `POST .../lock` → `POST .../fields` (or `DELETE .../fields/{name}`)
→ (optional further design writes) → `POST .../unlock`.

Add-field body is a `ContentTypeField`:

```json
{
  "name": "rx_note",
  "label": "Note",
  "dataType": "text",
  "searchable": true,
  "required": false
}
```

Optional `fieldSet` names an existing child field set, or creates a named
complex child when missing. Duplicate field names on the type are **409**.
POST without a held lock is **409**. The server **ALTERs** the content type's
backend table to add the field's column **before** the content editor
application is saved and re-initialized, so a following `GET` catalog includes
the field and the editor starts. Unknown field on DELETE is **404**. Deleting a
system or shared field is **400**. Field **order** in the editor remains
Workbench-only.

### Include system or shared fields (CD-04)

`POST /services/contenttypes/{idOrName}/fields/include` includes an existing
**system** or **shared** field into a content type (Workbench “include field”).
It does **not** create a local copy. After success, `GET` detail shows
`fieldType` `system` or `shared` for that field.

Requires a **held** design-session lock (`POST .../lock` first). The lock is
**not** released and is **not** stolen from another user. Catalog lookup uses
the design web service system def / shared def (read, unlocked). Persist is
`IPSContentDesignWs.saveContentTypes` only.

Typical flow: `POST .../lock` → `POST .../fields/include` → (optional further
design writes) → `POST .../unlock`.

Include body is a `ContentTypeField`:

```json
{
  "name": "sys_title",
  "fieldType": "system"
}
```

Shared fields may use a simple name or `group.field`:

```json
{
  "name": "displaytitle",
  "fieldType": "shared"
}
```

`fieldType` is required and must be `system` or `shared`. `local` is **400**
(use `POST .../fields`). Duplicate include is **409**. Unknown catalog field is
**404**. Non-Admin is **403**. Unlocked or locked by another user is **409**.
There is no SPA field-picker chrome for this surface.

### Enable or disable (CD-13)

`PUT /services/contenttypes/{idOrName}/enabled` sets whether the content type is
enabled for runtime use. This is a dedicated design action, not a read-only
catalog field. Hold the design-session lock first; save keeps the lock so you
can continue editing, then unlock.

Typical flow: `POST .../lock` → `PUT .../enabled` → (optional further design
writes) → `POST .../unlock`.

Jackson root wrap:

```json
{
  "ContentTypeEnabled": {
    "enabled": false
  }
}
```

### Type-level search indexing (CD-10)

`GET /services/contenttypes/{idOrName}/searchIndexing` and `PUT
.../searchIndexing` read and write whether items of this content type may be
indexed for search. This is the Workbench Content Type editor Properties
checkbox **Enable searching for this Content Type**. It maps the **root**
mapper field-set `isUserSearchable` flag. It is **not** the per-field
`searchable` flag already available on PUT content-type detail and local-field
create.

The flag **defaults to on** when a content type has no mapper field-set (same
as Workbench). PUT is **Admin** only and requires a **held** design-session
lock (`POST .../lock` first). The lock is **not** acquired or released by this
call. GET does not require a lock.

Typical flow: `POST .../lock` → `PUT .../searchIndexing` → (optional further
design writes) → `POST .../unlock`. Then `GET .../searchIndexing` round-trips
the saved value.

Jackson root wrap:

```json
{
  "ContentTypeSearchIndexing": {
    "searchIndexing": false
  }
}
```

Missing `searchIndexing` on PUT is **400**. Unlocked or another user's lock is
**409**. Unknown type is **404**. Non-Admin PUT is **403**. The Developer SPA
Content type detail **Search indexing** checkbox uses this GET/PUT after
**Lock** (see [Developer Content Types](id:admin-developer-content-types)). It
is distinct from the per-field **Searchable** column.

### Icon strategy (CD-11)

`GET /services/contenttypes/{idOrName}/icon` reads the content type **icon
strategy**. `PUT /services/contenttypes/{idOrName}/icon` sets it. This is a
dedicated design action (Workbench Properties tab: None / Specified file /
From File Field). Bulk `PUT /services/contenttypes/{idOrName}` does **not**
change the icon. The Developer SPA **Content types** detail **Icon strategy**
control (after lock) uses this path. There is no binary upload —
`value` is a file path/name or a file field name only.

Hold the design-session lock first for PUT; save keeps the lock so you can
continue editing, then unlock.

Typical flow: `POST .../lock` → `PUT .../icon` → (optional further design
writes) → `POST .../unlock`. `GET .../icon` does not require a lock.

`source` values:

| `source` | `value` | Meaning |
|----------|---------|---------|
| `none` | omitted / empty (cleared) | No content-type icon |
| `specified` | required file path or name | Use that icon file |
| `fromFileField` | required file field name | Derive the icon from that file field's extension |

A blank `value` when `source` is not `none` is **400**. An unknown `source` is
**400**. An unlocked type (or a lock held by another user) is **409** — the
lock is not stolen. Missing types are **404**. Non-Admin PUT is **403**.

Jackson root wrap:

```json
{
  "ContentTypeIcon": {
    "source": "specified",
    "value": "rx_resources/images/ContentTypeIcons/page.gif"
  }
}
```

`GET /services/contenttypes` (the catalog list) may be a JSON array or a Jackson
root envelope (`ContentTypeList` and/or `ContentType`). The Developer **Content
Types** catalog unwraps those shapes so the table loads (and the first row can
open Object ACL). Clients must not assume a bare array or call `.map` on the
raw object.

List and detail JSON list fields (`fields`, `childFieldSets`, `allowedWorkflows`,
`allowedTemplates`, `designGaps`) are arrays. Some Jackson/JAXB envelopes
historically serialize a one-item list as a single object, a
`{ NamedObjectRef: … }` / `{ ContentTypeField: … }` / `{ DesignGap: … }` wrapper,
or an empty-collection bean (`{ "empty": false }`). **Developer → Content Types**
detail treats those non-array shapes as an empty list or unwraps the single item.
The content type form stays on screen (or shows an in-panel error). It does
**not** replace the Content Types section with **Unable to load Content Types**.
Capability gaps still render as the human-readable **message** (fallback **code**).

### Allowed workflows (CD-08)

`PUT /services/contenttypes/{idOrName}/allowedWorkflows` replaces the content
type's allowed-workflow associations. Hold the design-session lock first; save
keeps the lock so you can continue editing, then unlock. This dedicated action
does **not** acquire or release the lock. The Developer SPA **Content types**
detail chrome uses this PUT after **Lock** (not the generic content-type PUT)
when the allowed-workflow set changes.

Typical flow: `POST .../lock` → `PUT .../allowedWorkflows` → (optional further
design writes) → `POST .../unlock`.

Jackson root wrap:

```json
{
  "ContentTypeWorkflows": {
    "allowedWorkflows": [
      { "name": "Simple Workflow", "guid": { "stringValue": "0-23-4" } }
    ],
    "defaultWorkflow": { "name": "Simple Workflow" }
  }
}
```

| Status | Typical meaning |
|--------|-----------------|
| `200` | Updated; response is `ContentTypeDetail` with the new `allowedWorkflows` / `defaultWorkflow`; lock still held |
| `400` | Missing `allowedWorkflows`, unknown workflow id/name, invalid id, or wildcard name |
| `403` | Caller is not Admin |
| `404` | Content type not found |
| `409` | No design lock, or locked by another user |
| `500` | Unexpected error (not inferred from names that happen to contain "lock") |

### Allowed templates (CD-12)

`PUT /services/contenttypes/{idOrName}/allowedTemplates` replaces the content type's
allowed template associations. Body is a JSON array of named refs (`name` and/or
`guid`). Full-replace semantics: the supplied list becomes the new set; an empty
array clears associations.

Typical flow:

1. Hold a design lock on the content type (`POST .../lock` from Developer, or SOAP/Workbench).
2. `PUT /services/contenttypes/{idOrName}/allowedTemplates` with the new set.
3. `GET` the same path (or `GET /services/contenttypes/{idOrName}`) to confirm.
4. Release the design lock when editing is finished.

**Developer → Content types** detail chrome follows that flow after **Lock**: add or
remove existing template names/GUIDs, **Save content type** (dedicated PUT, then GET
lists the new set), then **Unlock**. Save is disabled until the lock is held; the
product does not steal another user's lock. See [Developer Content Types](id:admin-developer-content-types).

`409` means the current session user does not hold the design lock. `400` means
a template id or name in the body does not exist. The lock stays held after a
successful PUT so further association or design edits can continue.

### Item-level exits and validations (CD-09)

`GET /services/contenttypes/{idOrName}/itemExits` returns item-level **input
translations**, **output translations**, **validations**, and dataset-pipe
**pre/post exits** (Workbench Content Type Properties tab). No lock is
required. Empty arrays mean none are configured.

Each exit is an object with `extension` (fully-qualified ref such as
`Java/global/percussion/content/sys_cleanReservedHtmlClasses`), optional `name`,
`parameters[]` (`name`/`value`), a read-only `condition` summary, optional
`maxErrorsToStop`, and a human-readable `summary`. `maxErrorsToStopValidation`
is the item-validation stop count.

Item-level **input** translations and **pre-exits** must implement
`IPSRequestPreProcessor` / `IPSItemInputTransformer`. Item-level **output**
translations and **post-exits** must implement `IPSResultDocumentProcessor`.
Field-level UDFs such as `Java/global/percussion/generic/sys_ToUpperCase`
belong on `PUT .../fields/{field}/ruleExpressions`, not on this item-level
path — using them here fails design-save validation (**400**).

`PUT` on the same path replaces `inputTranslations`, `outputTranslations`, and
`validations` (empty list clears). Hold the design-session lock first
(`POST .../lock`). `preExits` / `postExits` omitted leave pipe extensions
unchanged; a non-null list is a full replace (content-editor pipes use the
CE-specific input-data setter; `setInputDataExtensions` is not supported on
percPage). Each exit needs a resolvable extension FQN; parameter values are
stored as literals. Unchanged GET rows keep their original apply-when and
parameter types. **Apply-when conditions are not written** for new rows (see
`designGaps` code `CT_ITEM_EXIT_CONDITIONS`).

Typical flow: `POST .../lock` → `PUT .../itemExits` → `POST .../unlock`.

**Developer → Content types** detail chrome follows that flow after **Lock**: add
or remove item-level input/output translations, validations, and pipe pre/post
exits by extension FQN, **Save content type** (dedicated PUT, then GET lists the
new set), then **Unlock**. Save is disabled until the lock is held; the product
does not steal another user's lock. Apply-when conditions stay read-only. See
[Developer Content Types](id:admin-developer-content-types).

Jackson root wrap:

```json
{
  "ContentTypeItemExits": {
    "inputTranslations": [
      {
        "extension": "Java/global/percussion/content/sys_itemHTMLEncodeTransformer",
        "parameters": [{ "value": "sys_title" }]
      }
    ],
    "outputTranslations": [],
    "validations": [],
    "maxErrorsToStopValidation": 10
  }
}
```

| Status | Typical meaning |
|--------|-----------------|
| `200` | GET or PUT success (PUT keeps the lock held) |
| `400` | Missing required lists, invalid extension FQN, `maxErrorsToStopValidation` ≤ 0, or design-save validation (wrong item-level extension interface) |
| `403` | Caller is not Admin (PUT) |
| `404` | Content type not found |
| `409` | No design lock, or locked by another user |
| `500` | Design service or server failure |

Content type **detail** still lists `designGaps` code `CT_ITEM_EXITS` pointing
at this dedicated path. Developer Content Types detail chrome consumes that
path after a held design lock (CD-09). Apply-when write remains a gap
(`CT_ITEM_EXIT_CONDITIONS`).

### Field rule expressions (CD-05–07)

Content type **detail** field rows include boolean rule **flags** and, when rules exist,
human-readable **expression summaries**. Those summary strings are **not** written by
`PUT /contenttypes/{idOrName}`. Use the dedicated field path to persist rules.

| Field | Meaning |
|-------|---------|
| `hasValidation` / `validationExpression` | Validation rules present / summary of conditionals or extension calls |
| `hasVisibilityRules` / `visibilityExpression` | Visibility rules present / summary |
| `hasInputTranslation` / `inputTranslationExpression` | Input transform present / extension call summary |
| `hasOutputTranslation` / `outputTranslationExpression` | Output transform present / extension call summary |
| `control` | Display control name |
| `controlPropertyNames` | Control parameter names (compat; prefer `controlProperties`) |
| `controlProperties` | Control parameter **name and value** pairs |

`GET /services/contenttypes/{idOrName}/fields/{fieldName}/ruleExpressions` returns the
structured rules for one field. No design lock is required. Empty arrays mean none.
`404` means the content type or field was not found. GET also repeats the same summary
strings as the detail field row.

`PUT` on the same path replaces validation, visibility, and input/output translation
expressions. Hold the design-session lock first; save keeps the lock so you can continue
editing, then unlock.

Typical flow: `POST .../lock` → `PUT .../fields/{fieldName}/ruleExpressions` → (optional
further design writes) → `POST .../unlock`.

Jackson root wrap:

```json
{
  "ContentTypeFieldRuleExpressions": {
    "fieldName": "sys_title",
    "validation": [
      {
        "type": "conditional",
        "conditionals": [
          { "variable": "sys_title", "operator": "<>", "value": "" }
        ]
      }
    ],
    "visibility": [],
    "inputTranslation": [
      {
        "extension": "Java/global/percussion/generic/sys_ToUpperCase",
        "parameters": [{ "value": "sys_title" }]
      }
    ],
    "outputTranslation": []
  }
}
```

`validation`, `visibility`, `inputTranslation`, and `outputTranslation` are required on
PUT (empty list clears). Rule `type` is `conditional`, `extension`, or `reference`
(validation only; visibility rejects `reference`). Conditional `variable` / `value` are
stored as text literals. Operator `!=` is accepted as `<>`. Apply-when on field
validation is not written (see `designGaps` code `CT_FIELD_RULE_APPLY_WHEN`).

| Status | Typical meaning |
|--------|-----------------|
| `200` | GET envelope, or PUT replaced (lock still held) |
| `400` | Missing required lists, invalid rule, or unknown field name |
| `403` | Caller is not Admin (PUT) |
| `404` | Content type not found (PUT), or content type / field not found (GET) |
| `409` | No design lock, or locked by another user |
| `500` | Unexpected error |

**Developer → Content types** detail chrome edits these four lists as expression
**text** after **Lock** (one line per rule or extension call). **Save content type**
calls this PUT, then GET reflects the new expressions. Save stays disabled until
the lock is held; the product does not steal another user's lock. This is not
the Workbench visual rule builder. See [Developer Content Types](id:admin-developer-content-types).

Control property **values** and the field **choice catalog** use the dedicated
CD-07 path below and the Developer Content Types **Control property values** /
**Choice catalog** chrome after a held lock. A properties-only save omits
`choices` so catalogs stay unchanged. When the operator edits the catalog, Save
sends `choices` (including filter, null-entry, and default-selected);
`type: none` clears. See [Developer Content Types](id:admin-developer-content-types).

### Control property values (CD-07)

`GET /services/contenttypes/{idOrName}/fields/{fieldName}/controlProperties` returns the
display-control parameter **values** (not names only) and the field's choice catalog.
No design lock is required. `404` means the content type or field mapping was not found.

`PUT` on the same path replaces property values. Hold the design-session lock first; save
keeps the lock so you can continue editing, then unlock.

Typical flow: `POST .../lock` → `PUT .../fields/{fieldName}/controlProperties` → (optional
further design writes) → `POST .../unlock`.

Jackson root wrap:

```json
{
  "ContentTypeFieldControlProperties": {
    "fieldName": "sys_title",
    "control": "sys_DropDownSingle",
    "properties": [
      { "name": "height", "value": "200" }
    ],
    "choices": {
      "type": "local",
      "sortOrder": "ascending",
      "entries": [
        { "value": "open", "label": "Open" }
      ],
      "nullEntry": {
        "value": "",
        "label": "None",
        "includeWhen": "always",
        "sortOrder": "first"
      },
      "defaultSelected": [
        { "type": "nullEntry" },
        { "type": "text", "text": "open" }
      ],
      "filter": {
        "dependentFields": [
          { "fieldRef": "sys_communityid", "dependencyType": "optional" }
        ],
        "lookupHref": "../sys_lookup/filter.xml",
        "lookupName": "choiceFilter"
      }
    }
  }
}
```

`properties` is required on PUT (empty list clears parameters). `choices` omitted leaves
the catalog unchanged. Choice `type` is `global` (keyword table id in `globalId`),
`local` (inline `entries`), `lookup` / `internalLookup` (`lookupHref`), or `tableinfo`
(`table` with `tableName` / `labelColumn` / `valueColumn`). `type: none` clears the
catalog. When `choices` is present, `filter`, `nullEntry`, and `defaultSelected` are
written as part of that replace (omit/null/empty clears those extras). `filter.dependentFields`
need `fieldRef` plus `dependencyType` `optional` or `required`, and `lookupHref`.
`nullEntry.includeWhen` is `always` or `onlyIfNull`; `nullEntry.sortOrder` is `first`,
`last`, or `sorted`. `defaultSelected[].type` is `nullEntry`, `sequence` (needs
`sequence` ≥ 0), or `text` (needs `text`).

| Status | Typical meaning |
|--------|-----------------|
| `200` | GET envelope, or PUT replaced (lock still held) |
| `400` | Missing `properties`, invalid choice catalog, or field has no display control |
| `403` | Caller is not Admin (PUT) |
| `404` | Content type or field not found |
| `409` | No design lock, or locked by another user |
| `500` | Unexpected error |

## Shared fields (design catalog)

Content-editor **shared field groups** (Workbench shared field files, CD-15) are a
separate design object from content types. Public REST exposes a catalog **and write**
under `/services/sharedfields`. Load and save use the same content **design** web
service as Workbench (`IPSContentDesignWs.loadContentEditorSharedDef` /
`saveContentEditorSharedDef`). Writes acquire the shared-definition design lock for
the request and **release** it on save (unlike content-type PUT, which requires a
previously held lock).

**Admin (Design) only.** There is no global JAX-RS Admin filter on this path — the
sitemanage adaptor checks `IPSUserService.isAdminUser` for the current user and maps
a non-Admin caller to **403**. **Developer → Shared Fields** chrome uses list,
load, create, save, and delete for **groups** (see [Developer Shared Fields](id:admin-developer-shared-fields)).
Nested field create/delete persist a backend column
mapping and a default `sys_EditBox` display mapping. Control property **values**
and optional choice catalogs use `GET`/`PUT
.../fields/{fieldName}/controlProperties` (CD-15 remainder / CD-07 on shared defs).
System-def field-property save and field create/delete are a
separate catalog (`PUT /services/systemdef`, `POST /services/systemdef/fields`,
`DELETE /services/systemdef/fields/{fieldName}`, CD-16).

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/services/sharedfields` | List shared field groups (`name`, `filename`, `fieldCount`) |
| `GET` | `/services/sharedfields/{idOrName}` | Load one group by **name** (case-insensitive). Shared groups have no numeric design id on this catalog. |
| `POST` | `/services/sharedfields` | Create an empty shared field group (`name` required, unique, no spaces). Optional `filename` defaults to `{name}.xml`. |
| `PUT` | `/services/sharedfields/{idOrName}` | Save filename, optional rename (`body.name`), and patches to **existing** fields (`searchable`, occurrence / required). Null `fields` leaves the catalog unchanged. Does not create or delete fields. |
| `DELETE` | `/services/sharedfields/{idOrName}` | Delete the group (**204**). |
| `POST` | `/services/sharedfields/{idOrName}/fields` | Add a field to an existing group (`name` required, unique across shared groups). Optional `dataType` defaults to `text`. Optional `searchable` and occurrence / required use the same rules as PUT patches. |
| `DELETE` | `/services/sharedfields/{idOrName}/fields/{fieldName}` | Remove a field and its display mapping (**204**). |
| `GET` | `/services/sharedfields/{idOrName}/fields/{fieldName}/controlProperties` | Control parameter **name/value** pairs and the choice catalog for one shared field (CD-15). No lock required. Empty `properties` means none. `choices` omitted when none. `designGaps` lists remaining shared-field work (SPA editor and system-def catalog) — same notes as group detail, not an empty content-type control-property list. |
| `PUT` | `/services/sharedfields/{idOrName}/fields/{fieldName}/controlProperties` | **Admin** (CD-15). Acquires the shared-definition lock for this request and **releases** it on save. Full replace of `properties` (empty clears). `choices` omitted leaves the catalog unchanged; `type: none` clears. Blank group or field path name is **400**. **409** if the shared def is locked by another user. |

`{idOrName}` is the shared field group name (for example a product set such as
`shared`). Path separators and `..` are rejected as not found (**404**), not as a
directory listing. A blank path name on PUT or DELETE is **400** (same invalid-name
rule as POST).

Create body is a `SharedFieldGroupDetail`:

```json
{
  "name": "customShared",
  "filename": "customShared.xml"
}
```

PUT may include `fields[]` to patch existing fields by `name`. Unknown field names
are **400**. PUT does **not** create or delete fields — use nested POST/DELETE
`.../fields`. Field `name` on create must start with a letter and may contain
letters, digits, or underscore (no spaces or path characters). Duplicate field
names (case-insensitive, including names already defined in another shared group)
are **409**. `occurrence` and `required` map to the same dimension: when both are
sent they must agree (`required=true` with `required` / `oneOrMore`;
`required=false` with `optional` / `zeroOrMore` / `count`) or the request is
**400**. `occurrence` is applied when present; `required` is used only when
`occurrence` is omitted.

Add-field body is a `SharedFieldSummary`:

```json
{
  "name": "rx_note",
  "dataType": "text",
  "searchable": true,
  "required": false
}
```

### Response shape

List entries use `SharedFieldGroupSummary`. Detail uses `SharedFieldGroupDetail`:

- `name`, `filename`
- `fields[]`: `name`, `dataType`, `searchable`, `required`, `readOnly`, `occurrence`
  (`optional` / `required` / `oneOrMore` / `zeroOrMore` / `count` / `unknown`)
- `designGaps[]` strings — choice filters / null-entry / default-selected and the
  SPA field/control editor remain later slices. **Developer → Shared Fields**
  chrome can create, save, and delete a **group** (this catalog). Nested field
  and control-property writes stay REST-only. System-def field-property save is
  `PUT /services/systemdef`; field create/delete are
  `POST /services/systemdef/fields` and `DELETE /services/systemdef/fields/{fieldName}`.

Control property GET/PUT uses Jackson wrap `SharedFieldControlProperties` (same
nested `properties` / `choices` shape as content-type CD-07). Typical write:

```json
{
  "SharedFieldControlProperties": {
    "properties": [
      { "name": "height", "value": "200" }
    ]
  }
}
```

Omit `choices` to leave the catalog unchanged. Send `"choices": { "type": "none" }`
to clear it.

Prefer the generated OpenAPI schema as the integration source of truth.

### Status codes and authorization

| Status | Typical meaning |
|--------|-----------------|
| `200` | List (possibly empty), group detail, create, save, add-field, or control-property GET/PUT |
| `204` | Group or field deleted |
| `400` | Invalid name/filename (including blank path name on PUT/DELETE), unknown field on PUT group, invalid field name/`dataType`, conflicting `occurrence`/`required`, missing `properties` on control PUT, or invalid choice catalog |
| `403` | Caller is not Admin, or the request has no session/user (writes) |
| `404` | Group or field not found (unknown or unsafe name). Non-Admin callers receive **403**, not 404 |
| `409` | Duplicate group or field name, or the shared definition is locked by another user |
| `500` | Design service or server failure |

Authenticated non-Admin sessions (Editor, Contributor, and similar) must not read or
write this catalog. Callers should treat 403 as “no Design Admin rights,” not as a
missing group. Treat 409 as “retry after the other designer releases the shared-def
lock” (Workbench also locks the whole shared definition, not one group).

## System definition (design catalog)

Content-editor **system definition** (Workbench global / system fields, CD-16) is a
singleton design object. Public REST exposes a catalog **and write** under
`/services/systemdef`. Load and save use the same content **design** web
service as Workbench (`IPSContentDesignWs.loadContentEditorSystemDef` /
`saveContentEditorSystemDef`). Writes acquire the system-definition design lock for
the request and **release** it on save (same request-lock pattern as shared-field
PUT; unlike content-type PUT, which requires a previously held lock). The
Developer SPA **System definition** chrome uses these calls for field save /
add / delete — see [Developer System Def](id:admin-developer-system-def).

**Admin (Design) only.** There is no global JAX-RS Admin filter on this path — the
sitemanage adaptor checks `IPSUserService.isAdminUser` for the current user and maps
a non-Admin caller to **403**. Nested field create/delete persist a backend column
mapping (default table `CONTENTSTATUS`) and a default `sys_EditBox` display
mapping. **POST** creates the `CONTENTSTATUS` column when it is absent; **DELETE**
drops that column when present and still saves the XML catalog if the column is
already missing. Other `DROP COLUMN` failures (permissions, lock, or connection)
fail the request with **500** and do **not** save the catalog. A **PUT** with a
null or empty `fields` array does not rewrite
the system-definition file (the catalog is unchanged). Control properties,
stylesheets, and application flow remain unsupported.

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/services/systemdef` | Load the system definition field catalog (`fieldCount`, `cacheTimeoutMinutes`, `fields[]`) |
| `PUT` | `/services/systemdef` | Patch **existing** fields (`searchable`, occurrence / required). Null or empty `fields` leaves the catalog unchanged. Does not create or delete fields. |
| `POST` | `/services/systemdef/fields` | Add a field (`name` required, unique). Optional `dataType` defaults to `text`. Optional `searchable` and occurrence / required use the same rules as PUT patches. |
| `DELETE` | `/services/systemdef/fields/{fieldName}` | Remove a field and its display mapping (**204**). |

PUT may include `fields[]` to patch existing fields by `name`. Unknown field names
are **400**. PUT does **not** create or delete fields — use nested POST/DELETE
`.../fields`. Field `name` on create must start with a letter and may contain
letters, digits, or underscore (no spaces, path characters, or SQL reserved
words such as `SELECT`, `USER`, `TABLE`, or `ORDER`). Duplicate field
names (case-insensitive) are **409**. `occurrence` and `required`
map to the same dimension: when both are sent they must agree (`required=true`
with `required` / `oneOrMore`; `required=false` with `optional` / `zeroOrMore` /
`count`) or the request is **400**. `occurrence` is applied when present;
`required` is used only when `occurrence` is omitted. `dataType`, `readOnly`, and
`cacheTimeoutMinutes` are read-only on PUT. System-mandatory and system-internal
fields cannot be deleted (**400**). Unknown `{fieldName}` on DELETE is **400**.

Example PUT body:

```json
{
  "fields": [
    { "name": "sys_title", "searchable": true, "occurrence": "required" }
  ]
}
```

Add-field body is a `SystemDefFieldSummary`:

```json
{
  "name": "sys_custom",
  "dataType": "text",
  "searchable": true,
  "required": false
}
```

### Response shape

Detail uses `SystemDefDetail`:

- `fieldCount`, `cacheTimeoutMinutes` (read-only)
- `fields[]`: `name`, `dataType`, `searchable`, `required`, `readOnly`, `occurrence`
  (`optional` / `required` / `oneOrMore` / `zeroOrMore` / `count` / `unknown`)
- `designGaps[]` strings — control/stylesheet/application flow, and shared-field
  groups (separate catalog)

Prefer the generated OpenAPI schema as the integration source of truth.

### Status codes and authorization

| Status | Typical meaning |
|--------|-----------------|
| `200` | Catalog, save, or add-field |
| `204` | Field deleted |
| `400` | Missing body, unknown field, invalid name/`dataType` (including SQL reserved identifiers), conflicting `occurrence`/`required`, or delete of a system-mandatory / system-internal field |
| `403` | Caller is not Admin, or the request has no session/user (writes) |
| `409` | Duplicate field name, system definition locked by another user, or design lock required for save |
| `500` | Design service or server failure |

Authenticated non-Admin sessions (Editor, Contributor, and similar) must not read or
write this catalog. Callers should treat 403 as “no Design Admin rights.” Treat 409
as “retry after the other designer releases the system-def lock” (Workbench locks
the whole system definition).

## Templates (assembly catalog)

Assembly templates used by Design and Developer are exposed under `/services/templates`.
Create uses the modern package/manifest model — **no Widget definition XML**.

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/services/templates` | List template summaries (design catalog) |
| `POST` | `/services/templates/summaries-by-filter` | Filter summaries (for example by content id) |
| `GET` | `/services/templates/{idOrName}` | Load design detail (source, bindings, slots, assembler) |
| `GET` | `/services/templates/{idOrName}/export` | **Admin.** AS-08 export of Workbench-equivalent design XML |
| `POST` | `/services/templates/import` | **Admin.** AS-08 import of one Workbench-equivalent `assembly-template` XML |
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

### Template design XML export (AS-08)

`GET /services/templates/{idOrName}/export` is the Workbench **export template** equivalent:
it returns the design-object XML for one assembly template loaded through the existing
assembly design web service (`IPSAssemblyDesignWs`). The download uses
`Content-Type: application/xml` and `Content-Disposition: attachment` with a filename
derived from the **template name** (for example `perc.page.xml`).

| Status | Meaning |
|--------|---------|
| `200` | Design XML body |
| `403` | Caller is not Admin |
| `404` | Unknown id or name |

The load is **read-only**. The server does **not** acquire or steal a design lock
(`lock=false`, `overrideLock=false`). **Import** of the same XML is
`POST /services/templates/import` (create only; name collision is **409**).
**Developer → Templates** exposes **Export XML** on template detail and **Import XML**
on the catalog. There is no Design SPA export wizard.

See also [Developer Templates](id:admin-developer-templates) and
[Design templates](id:admin-design-templates).

The Design SPA **Create template** action uses POST; **Delete** uses this DELETE. See
[Design templates](id:admin-design-templates).

## Display formats (design catalog)

Content Explorer **display format** definitions (Developer **Display Formats**) are exposed
under `/services/displayformats`. The REST layer is a thin contract over the UI **design**
web service (`IPSUiDesignWs`) — create and update use the same
`createDisplayFormats` / `loadDisplayFormats` / `saveDisplayFormats` operations SOAP uses.
Admin delete loads the format and persists component XML (Workbench processor path) so
`updateDisplayFormats` receives a document. There is no new SOAP surface. GET list/detail
remain a catalog read.

Responses include a nested `guid` object and a plain `guidString` (`host-type-uuid`) so
clients can load **Object ACL** via `GET /services/acls/object/{guid}`.

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/services/displayformats` | List formats (optional `validForFolder` / `validForViewsAndSearches`) |
| `GET` | `/services/displayformats/{idOrName}` | Load one format by internal name or GUID string |
| `POST` | `/services/displayformats` | **Admin.** Create a format (`createDisplayFormats` then `saveDisplayFormats`) |
| `PUT` | `/services/displayformats/{idOrName}` | **Admin.** Update `label`/`displayName` and/or `description`; `columns` replaces the column list when present; `allowedCommunities` replaces community visibility when present |
| `DELETE` | `/services/displayformats/{idOrName}` | **Admin.** Delete a user format (loaded component XML persist; dependents `ignoreDependencies=false`) |

JSON wraps the list as `DisplayFormatList` (`{"DisplayFormatList":[…]}`) including the empty
catalog (`{"DisplayFormatList":[]}`, not a bare `[]`) and a single item as `DisplayFormat`.
Integrators should unwrap those envelopes and read `guid.stringValue` or `guidString` (never
assume the GUID is missing when `displayId` is present). See [Users, roles & security](id:admin-users-roles)
for the operator Object ACL steps.

Create (`POST /services/displayformats`) persists immediately (Workbench Finish, not an
unsaved stub) and returns **201 Created** with a `Location` header pointing at
`GET /services/displayformats/{name}`. JSON body requires `name` or `internalName` (unique,
case-insensitive; **no whitespace** or wildcards). Optional `label` / `displayName` and
`description` are applied before save. Duplicate name is **409**. Blank / whitespace /
wildcard names are **400**. Missing request session/user is **403**. Non-Admin is **403**.
The new format is then `GET /services/displayformats/{name}` **200** with **that** name
(not **404**, and not a packaged format such as `By_Author`). `GET` by the created GUID
returns the same user format.

Update (`PUT /services/displayformats/{idOrName}`) loads with a design lock
(`overrideLock=false`) and releases it on save. Name is not renamed on PUT. `label` /
`displayName` and `description` round-trip. When `columns` is present, the column list
is **replaced** (add, remove, and reorder). Omit `columns` to leave the stored list
unchanged. Invalid column `source` (blank, whitespace, wildcards, or path characters)
is **400**. Duplicate sources in the same list are **400**. When `allowedCommunities`
is present, community visibility is **replaced**. The value is a JSON **array** of
`{guid,name}` rows (GUID string or community name). An **empty array** is all
communities (Workbench `sys_community=-1`). Omit `allowedCommunities` to leave
visibility unchanged. There is no distinct “no communities” state — empty and
all-communities persist the same way. Unknown community is **400**. Non-Admin is
**403**. GET returns an empty array when the format is visible to all communities,
and the restricted rows when it is not. The Developer SPA edits columns and allowed communities on
**user** formats only; packaged/system formats stay read-only in that catalog. See
[Developer Display Formats](id:admin-developer-display-formats). Usage flags on GET
(`validForFolder`, `validForViewsAndSearches`, `validForRelatedContent`) are **derived
from columns** the same way Workbench computes them — they are not independently
persisted on PUT.

Delete (`DELETE /services/displayformats/{idOrName}`) returns **204** when the format is
removed from the catalog; a following `GET` is **404**. The REST adaptor loads the format
with a design lock, marks it for deletion, and persists the **component XML** (the same
Workbench objectstore path `updateDisplayFormats` expects). Locator-only SOAP
`deleteDisplayFormats` is not used for this REST path — that request supplies no XML
document and does not persist. Unknown id/name is **404**. A format that still has
dependents is **409**. Locked-by-another-user is **409** (the lock is not stolen).
Non-Admin is **403**. Do **not** delete packaged system formats (for example `By_Author`)
to prove this path — create a uniquely named user format with `POST`, then `DELETE` that
name.

**Developer → Display Formats** chrome creates and deletes user display formats
(and saves label / description), edits columns on **user** formats, and sets
**allowed communities** on **user** formats. Packaged formats stay read-only.
The catalog lists user-created formats and omits a row after a successful REST
or SPA delete. See [Developer Display Formats](id:admin-developer-display-formats).

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

Admin **write** persists through `IPSUiDesignWs` (`createActions` / `loadActions` /
`saveActions` / `deleteActions`) — the same design web service SOAP uses. Durable
rows are written to `RXMENUACTION` so Hibernate `findActionMenusTree` (GET
`/services/actions/catalog` and GET by name) includes a user menu immediately
after POST, and omits it after DELETE. There is no new SOAP surface.
**Developer → Action Menus** chrome creates and deletes user menus (and saves
label / description / menuType / url); cascading children composition (UI-04)
and usage/command/visibility tab completeness (UI-03) are later slices — see
[Developer Action Menus](id:admin-developer-action-menus). Finder helpers
(`GET /services/actions/find`, content-type and template finders) are unchanged.
After POST the editor notice confirms the save. Packaged menus (for example
**Copy**) are **409** on PUT/DELETE, not **404**.

Write is **Admin** only. Name is unique (case-insensitive) and must not contain
whitespace or wildcards. Duplicate name is **409**. Invalid name or menu type is
**400**. Missing id/name is **404**. Non-Admin (or missing request session/user)
is **403**. **System** menus (Workbench `Menus/System` hierarchy) cannot be
updated or deleted — **409**; the design lock is not stolen (`overrideLock=false`).
If Workbench path resolution fails, PUT/DELETE also return **409** (fail closed)
so a lookup error cannot bypass that protection.
PUT round-trips GET detail fields already exposed (`label`, `description`,
`menuType`, `url`). Name is the catalog key and is not renamed on PUT.

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/services/actions/catalog` | List action menus (tree roots with children) |
| `GET` | `/services/actions/catalog/{idOrName}` | Load one menu by name, numeric id, or GUID string |
| `POST` | `/services/actions` | **Admin.** Create a user action menu (`createActions` then `saveActions`) |
| `PUT` | `/services/actions/{idOrName}` | **Admin.** Update label, description, menuType, and/or url |
| `DELETE` | `/services/actions/{idOrName}` | **Admin.** Delete a user action menu (`deleteActions`, `ignoreDependencies=false`) |

JSON may wrap a single item as `ActionMenu`. **Create** `POST /services/actions`
sends that envelope (or a flat object with `name`). Do not post
`allowedWorkflowTransitionsRequest` on the collection path — that finder lives at
`POST /services/actions/find/transitions`. Integrators should unwrap the
`ActionMenu` envelope and read `guid.stringValue` (never assume the GUID is
missing when `id` is present). See [Object ACL & default template](id:admin-object-acl).

## Views (design catalog)

Content Explorer **view** definitions (Workbench / Developer **Views**, UI-07) are exposed under
`/services/views` (public servlet path `/rest/views`). This is a **separate catalog** from saved
**searches** (`/services/searches`). Do not execute a view through the search execute endpoint.
List and detail include a nested `guid` (`PSTypeEnum.VIEW_DEF` = 18, string form
`0-18-{viewId}`) so clients can load **Object ACL**. Unwrap Jackson `ViewDef` envelopes
and read `guid.stringValue` or synthesize from `id` when the Guid is omitted.

Admin **write** persists through `IPSUiDesignWs` (`createViews` / `loadViews` / `saveViews` /
`deleteViews`) — the same design web service SOAP uses. There is no new SOAP surface.
**Developer → Views** chrome creates and deletes standard views, saves label /
description / type / display format, and edits **field criteria** on
user/standard views — see [Developer Views](id:admin-developer-views). Inbox-family
and custom URL views cannot be mutated from that catalog. Execute is **not**
invoked when creating, updating, or deleting a view. Create is durable:
`GET /services/views` lists the new name after POST.

Operators open Inbox from Explorer **Views → My Content → Inbox** (see
[Content Explorer](id:admin-content-explorer)). Integrators run the same assignment list
with the execute call below. `GET /services/views` includes the Inbox design view (name
`Inbox`, custom URL `../sys_cxViews/inbox.xml`) even when the design-WS load path
collapses sibling CX views to `View_All`. After Admin POST, GET list includes the
created name (durable persist; a second POST of that name is **409**).

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/services/views` | List view definitions (name, category, standard vs custom URL; includes `guid`) |
| `GET` | `/services/views/{idOrName}` | Load one view by name, numeric id, or GUID string (includes `guid` for Object ACL) |
| `POST` | `/services/views` | **Admin.** Create a standard (field-criteria) view (`createViews` then `saveViews`) |
| `PUT` | `/services/views/{idOrName}` | **Admin.** Update label, description, type, display format, and/or `fields` (omit to leave criteria unchanged; empty array clears; unknown field is 400) |
| `DELETE` | `/services/views/{idOrName}` | **Admin.** Delete a user/standard view (`deleteViews`, `ignoreDependencies=false`) |
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
| `200` | List / get / create / update / execute success |
| `204` | Delete success |
| `400` | Invalid input (missing name, whitespace/wildcard name, invalid or search type, custom URL on create, **unknown field** on PUT `fields`), invalid execute body, or an **unsupported** custom URL view |
| `403` | Caller is not Admin, or the request has no session/user for the design session |
| `404` | View not found or unsafe key (blank, path separators, `..`) |
| `409` | Duplicate name, design lock held by another user, dependents, or Inbox/system custom URL write |
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

### View write contract (Admin)

Create (`POST /services/views`) persists immediately (Workbench Finish, not an unsaved
stub). JSON body requires `name` (unique across views **and** searches, case-insensitive;
**no whitespace** or wildcards). Optional `label`, `description`, `type`, and
`displayFormatId` are applied before save. Default `type` is `View`. Accepted types:
`View` (`standard`, `standardView`). Search types (`StandardSearch`, `Search`) are
**400** — searches stay on `/services/searches`. Custom URL (`url` set, or type
`custom` / `CustomView`) is **400**. Duplicate name is **409**. Blank /
whitespace / wildcard names are **400**. Missing request session/user is **403**.
Non-Admin is **403**. The new view is then `GET /services/views/{name}` **200**
and is included in `GET /services/views` (the create is durable; a second POST
with the same name is **409**).

Update (`PUT /services/views/{idOrName}`) loads with a design lock (`overrideLock=false`)
and releases it on save. Name is not renamed on PUT. Omitted label / description / type /
display format leave stored values unchanged. Unknown id/name is **404**. Inbox-family
and other custom URL views are **409** (not mutated; the lock is not stolen).
Locked-by-another-user is **409**. Non-Admin is **403**.

Delete (`DELETE /services/views/{idOrName}`) returns **204** when removed; a following
`GET` is **404**. Unknown id/name is **404**. Inbox-family and other custom URL views are
**409** (not deleted). Dependents or a lock held by another user are **409**. Non-Admin
is **403**.

Create/update load or create the view with a **held design lock** and release it on
save. There is no separate lock/unlock REST pair on this catalog. Field criterion
editing is not supported on write. Execute (`POST …/execute`) is unchanged.

JSON objects use the `ViewDef` wire type. POST/PUT JSON is wrapped under a `ViewDef`
root (JAXB/Jackson UNWRAP_ROOT_VALUE). Prefer the generated OpenAPI schema as the
integration source of truth.

Example create body:

```json
{
  "ViewDef": {
    "name": "MyView",
    "label": "My View",
    "description": "Created via REST",
    "type": "View",
    "displayFormatId": "1"
  }
}
```

### Integrator notes

- Keys may be the view **name**, numeric **id**, or GUID string (including untyped GUID).
- Admin write is POST/PUT/DELETE on this resource and from **Developer → Views**
  (create / save / delete). Inbox-family and custom URL views cannot be updated
  or deleted (`409`). Field criterion editing remains a `designGaps` note on detail.
- Admin write is durable on H2 and other supported databases: after POST, GET
  list includes the name. A POST that cannot be cataloged is not **200**.
- **Developer → Views** chrome can create and delete standard views (field
  criteria stay read-only). Inbox-family views stay **409** on mutate/delete.
- Operator Inbox run-from-tree is Explorer **Views → My Content → Inbox**, not a
  free-floating Inbox root.

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
      "message": "Item-level exits/validations: GET/PUT /contenttypes/{idOrName}/itemExits (held lock for write). Apply-when conditions are read-only"
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

## Content editor controls (catalog)

Content editor **control** definitions (Workbench / Developer **Controls**, UI-01) are exposed
under `/services/cecontrols`. List and detail include system (packaged) and user (custom)
controls. Backing is **ALT** — `PSSystemControlManager` / `PSCustomControlManager` — there is
no SOAP design twin and this API does not invent one.

Admin **write** persists **user** controls as an XSL file under
`rx_resources/stylesheets/controls` plus the custom-control import list
(`PSCustomControlManager.writeImports`). Packaged **system** controls are read-only.
**Do not** treat this as a Developer Controls SPA; chrome for create/save/delete is a later
sibling. Full XSL source-editor UX is not provided by this API.

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/services/cecontrols` | List system and user CE controls |
| `GET` | `/services/cecontrols/{name}` | Load one control by name |
| `POST` | `/services/cecontrols` | **Admin.** Create a user control (XSL file + import list) |
| `PUT` | `/services/cecontrols/{name}` | **Admin.** Update a user control (metadata and/or optional `xslSource`) |
| `DELETE` | `/services/cecontrols/{name}` | **Admin.** Delete a user control (removes the user XSL file and refreshes imports) |

JSON objects use the `ControlDef` wire type. POST/PUT JSON is wrapped under a `ControlDef`
root (JAXB/Jackson UNWRAP_ROOT_VALUE). Prefer the generated OpenAPI schema as the
integration source of truth.

### Control write contract (Admin)

Create (`POST /services/cecontrols`) persists immediately. JSON body requires `name` (unique
across system **and** user controls, case-insensitive; **no whitespace** or wildcards).
Optional `displayName`, `description`, `dimension` (`single` default; `array`; `table`),
`choiceSet` (`none` default; `required`; `optional`), and `xslSource` are applied. When
`xslSource` is omitted the server writes a default user-control stylesheet from that
metadata. Duplicate name is **409**. Blank / whitespace / wildcard names are **400**.
Non-Admin is **403**. The new control is then `GET /services/cecontrols/{name}` **200** and
appears on `GET /services/cecontrols`.

Update (`PUT /services/cecontrols/{name}`) updates a **user** control. Name is the catalog
key and is not renamed on PUT. Omitted `xslSource` regenerates a default stylesheet from
metadata (send `xslSource` to keep a custom stylesheet). Unknown name is **404**. A
**system** control is **409** (packaged files are not mutated). Non-Admin is **403**.

Delete (`DELETE /services/cecontrols/{name}`) returns **204** when a user control is
removed; a following `GET` is **404**. Unknown name is **404**. A **system** control is
**409** (not deleted). Non-Admin is **403**.

There is **no** Developer SPA controls catalog create/delete in this slice — operators and
integrators call the REST path (or Workbench).

Example create body:

```json
{
  "ControlDef": {
    "name": "myUserControl",
    "displayName": "My User Control",
    "description": "Created via REST",
    "dimension": "single",
    "choiceSet": "none"
  }
}
```

| Status | Typical meaning |
|--------|-----------------|
| `200` | List / get / create / update success |
| `204` | Delete success |
| `400` | Invalid input (missing name, whitespace/wildcard name, invalid dimension/choiceSet/xslSource) |
| `403` | Caller is not Admin |
| `404` | User control not found |
| `409` | Duplicate name, or attempt to mutate/delete a packaged system control |
| `500` | Control manager or file I/O failure |
| `503` | Control adaptor not configured |

## Searches catalog and execute

CX design **searches** (and optionally **views** on GET/execute) are exposed under
`/services/searches`. Admin **write** (UI-06) persists through `IPSUiDesignWs`
(`createSearches` / `loadSearches` / `saveSearches` / `deleteSearches`) — the same design
web service SOAP uses. There is no new SOAP surface. **Developer → Searches** chrome
creates and deletes searches (and saves label / description / type / display format)
and field criteria on user/standard searches — see
[Developer Searches](id:admin-developer-searches).

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/services/searches` | List search definitions (Developer catalog; views omitted) |
| `GET` | `/services/searches?includeViews=true` | List searches **and** views (Explorer saved-search picker) |
| `GET` | `/services/searches/{idOrName}` | Load one search or view by name, label, GUID, or numeric id |
| `POST` | `/services/searches` | **Admin.** Create a search (`createSearches` then `saveSearches`) |
| `PUT` | `/services/searches/{idOrName}` | **Admin.** Update label, description, type, display format, and/or field criteria |
| `DELETE` | `/services/searches/{idOrName}` | **Admin.** Delete a search (`deleteSearches`, `ignoreDependencies=false`) |
| `POST` | `/services/searches/{idOrName}/execute` | Execute a standard/user search or view (not a custom URL) |

`includeViews=true` is required for the Explorer Search panel so the default **All** view
(`View_All`) is in the picker. Developer **Searches** remains searches-only; views stay on
`/services/views`.

If the search catalog fails independently of views (or the reverse), `includeViews=true`
still returns whichever side loaded so the picker is not a silent empty list or 500 for
the default All view.

Execute looks up the same combined catalog (searches first, then views). Keys accepted:
internal name (`View_All`), display label (`All`), GUID string, or numeric id. Custom URL
searches and custom views return **400**. Execute is **not** invoked when creating,
updating, or deleting a search.

### Search write contract (Admin)

Create (`POST /services/searches`) persists immediately (Workbench Finish, not an unsaved
stub). JSON body requires `name` (unique across searches **and** views, case-insensitive;
**no whitespace** or wildcards). Optional `label`, `description`, `type`, and
`displayFormatId` are applied before save. Default `type` is `StandardSearch`. Accepted
types: `StandardSearch` (`standard`), `CustomSearch` (`custom`), `Search` (user search).
`View` is **400** — views stay on `/services/views`. Duplicate name is **409**. Blank /
whitespace / wildcard names are **400**. Missing request session/user is **403**.
Non-Admin is **403**. The new search is then `GET /services/searches/{name}` **200**
and is included in `GET /services/searches` (the create is durable; a second POST
with the same name is **409**).

Update (`PUT /services/searches/{idOrName}`) loads with a design lock (`overrideLock=false`)
and releases it on save. Name is not renamed on PUT. Omitted label / description / type /
display format leave stored values unchanged. When `fields` is present it replaces field
criteria in order (`fieldName`, `operator`, `fieldValue`, `position`). Omitted `fields`
leaves stored criteria unchanged; an empty array clears them. Unknown / invalid field
name is **400**. Packaged/system searches (`Default_Search`, `RC_Search`) reject field
mutation with **409** (the lock is not stolen). Unknown id/name is
**404**. A view key is **400**. Locked-by-another-user is **409**. Non-Admin is **403**.

Delete (`DELETE /services/searches/{idOrName}`) returns **204** when removed; a following
`GET` is **404**. Unknown id/name is **404**. Dependents or a lock held by another user are
**409**. Non-Admin is **403**.

Create/update load or create the search with a **held design lock** and release it on
save. There is no separate lock/unlock REST pair on this catalog.

JSON objects use the `SearchDef` wire type. POST/PUT JSON is wrapped under a `SearchDef`
root (JAXB/Jackson UNWRAP_ROOT_VALUE). Prefer the generated OpenAPI schema as the
integration source of truth.

Example create body:

```json
{
  "SearchDef": {
    "name": "MySearch",
    "label": "My Search",
    "description": "Created via REST",
    "type": "StandardSearch",
    "displayFormatId": "1"
  }
}
```

| Status | Typical meaning |
|--------|-----------------|
| `200` | List / get / create / update success |
| `204` | Delete success |
| `400` | Invalid input (missing name, whitespace/wildcard name, invalid or View type, unknown field) |
| `403` | Caller is not Admin, or the request has no session/user for the design session |
| `404` | Search not found |
| `409` | Duplicate name, packaged/system field mutation, design lock held by another user, or dependents |
| `500` | Design service or server failure |

## Community new-search defaults (UI-09)

Admin REST for **which Content Explorer searches are the “new search” defaults for a
community**. This is the Workbench community-search assignment (`cxNewSearch` on the
search definition), persisted through `IPSUiDesignWs` load/save searches — the same
design path SOAP uses. It does **not** create or delete searches (see
[Search write contract](#search-write-contract-admin) and sibling search persist).

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/services/communities/{idOrName}/new-search-defaults` | **Admin.** Current default-search set for the community |
| `PUT` | `/services/communities/{idOrName}/new-search-defaults` | **Admin.** Replace the set (empty `searches` clears explicit defaults) |

`{idOrName}` is numeric community id, GUID string, or exact name (same lookup as
`GET /services/communities/{idOrName}`). GET of a community with **no** explicit
defaults is **200** with `searches: []`, not 404.

PUT body is `CommunityNewSearchDefaults`. Each `searches[]` entry may identify a
search by `name`, numeric `id`, or `guid.stringValue` (same keys as
`/services/searches/{idOrName}`). A second identical PUT is idempotent **200**.
Unknown or duplicate search is **400**. Unknown community is **404**. Non-Admin is
**403**. Design lock held by another user is **409**.

JSON uses the `CommunityNewSearchDefaults` wire type (JAXB/Jackson
UNWRAP_ROOT_VALUE). Prefer the generated OpenAPI schema as the integration source
of truth.

Example GET / PUT body:

```json
{
  "CommunityNewSearchDefaults": {
    "communityId": 10,
    "communityName": "Default",
    "searches": [
      { "name": "SimpleSearch", "id": 42 }
    ]
  }
}
```

| Status | Typical meaning |
|--------|-----------------|
| `200` | GET / PUT success (empty `searches` is a valid empty set) |
| `400` | Invalid body, unknown search, or duplicate search in the PUT set |
| `403` | Caller is not Admin, or the request has no session/user for PUT |
| `404` | Community not found |
| `409` | Design lock held by another user |
| `500` | Design service or server failure |

There is no Developer SPA for this assignment in this release; operators and
integrators call the REST surface above.

### Search execute body

Execute POST body is the JAXB envelope `{ "SearchExecuteRequest": { … } }` (flat
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

## Content Explorer translations

Explorer **Translations** (View → Translations, or Translate on Server actions) lists locale
variants and creates new ones through the public REST façade:

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/rest/content-explorer/translations/{itemId}` | Current locale plus translation-category dependents |
| `POST` | `/rest/content-explorer/translations` | Create locale variants (`itemIds` numeric content ids, optional `locales`) |

`{itemId}` on GET may be a hyphenated Percussion GUID (`16777215-101-551`) **or** a bare numeric
content id (`551`). Explorer list rows expose the full GUID on the detail-row identity
(`data-testid` / `data-item-id`); clients must send that full GUID on GET. Stripping to the last
segment (`GET …/translations/551`) can return **404 Item not found** while the GUID form returns
**200**. Create-variant POST still uses numeric `itemIds`.

See [Content Explorer](id:admin-content-explorer) → Translations.

## Testing tips

- Unit-test resources with Mockito and provide Spring test stubs for new adaptor interfaces on the
  rest test classpath.
- Exercise adaptor implementations in sitemanage tests.
- Run **standalone** `mvnw clean install` in each changed module before PR (see root `AGENTS.md`).

## Related

- [Extensions & packages](id:developer-extensions)
- [Build from source](id:developer-build-source)
- [Content Explorer](id:admin-content-explorer)
