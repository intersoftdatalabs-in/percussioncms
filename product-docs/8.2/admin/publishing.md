---
id: admin-publishing
title: Publishing
description: Publishing content and delivery targets in Percussion CMS 8.2
version: "8.2"
order: 43
tags: [admin, publishing]
---

# Publishing

Publishing is how Percussion turns assembled content into deliverables for websites and other
channels (static files, FTP, database, custom locations).

## Concepts

| Term | Meaning |
|------|---------|
| **Publish** | Run assembly for selected content and write results to configured destinations |
| **Edition / pub job** | Configured unit of work (what, where, when) |
| **Delivery location** | Filesystem path, FTP, or other target for assembled output |
| **Delivery Tier Service (DTS)** | Optional dynamic services (forms, comments, membership, metadata, polls, …) used by published sites |

## Operator workflow

1. Ensure content is in an **approved** (or otherwise publishable) workflow state.
2. Select the Site and publish scope (incremental vs full, as configured).
3. Run the publish job from the admin UI or scheduled task.
4. Verify logs for assembly errors and missing resources.
5. Spot-check delivered files or the live site.

Directory / Faculty Directory pages list people by **query** (organization and department on `percPerson`), not by unlinking members from the Directory asset. After a person is correctly non-matching, use full site publish or an explicit page publish, then purge CDN if disk HTML is clean but HTTP is stale. Procedure: [Faculty Directory membership and publish](id:admin-faculty-directory).

### Search the Sites list (Publishing shell)

From **Publish** (`spa.jsp?entry=publish`), the **Sites** section lists sites as cards (or a list).
Use **Filter Sites** to narrow the list by **name** or **id** (case-insensitive substring). Clearing
the box restores the full list. A search with no matches shows an empty state; it does not hide the
filter. Open a remaining card to configure servers and run Incremental / Full publish. Logs search
is a separate **Logs** filter (not this Sites box).

### Save a site delivery server (Sites)

From **Publish** (`spa.jsp?entry=publish`), open a **site** card, then **Add** (or **Edit Server**).
Enter a **server name** (required), production vs staging, File vs Database, driver fields, then
**Save**. Unsaved field edits mark the form dirty; leaving the editor prompts to discard. The shell
posts `POST …/publishmanagement/servers/{siteId}/{serverName}` (create) or
`PUT …/publishmanagement/servers/{siteId}/{serverId}` (update).

HTTP **403** (not Admin or Designer) and **409** (publish server name already exists on the site)
are shown in the server editor error region — not as a successful save. Incremental / Full publish
and Design delivery-type save are separate actions.

### Incremental site publish (Publishing shell)

From **Publish** (`spa.jsp?entry=publish`), open a site card, select a publish server, then choose **Incremental**. Confirm the dialog (**Confirm Incremental Publish**). The shell calls the incremental site publish API (`GET …/sitemanage/publish/incremental/publish/{site}/{server}`), optionally with related-item approval after **Incremental preview**. Success shows **Publish Job Started** plus the job id and refreshes the site **Status** list (active jobs). Dismissing confirm does not start a job. Application-level `FORBIDDEN` / `BADCONFIG` responses are failures in the workspace error region, not success. Full site publish remains a separate **Full** action.

### Save a publish edition (Design)

From **Publish** (`spa.jsp?entry=publish&section=design`), open **Design** then **Editions**.
Pick a site and **Add edition** (or open an existing edition). Enter a **name** (required),
optional comment and priority (1–5), then **Save**. The shell posts
`POST …/sitemanage/publishingdesign/editions` (create) or
`PUT …/sitemanage/publishingdesign/editions/{editionId}` (update).

HTTP **403** (not Admin or Designer) and **409** (edition name already exists) are shown in the
edition editor error region — not as a successful save. Content-list association, copy, and
Runtime start/stop are separate Design/Runtime actions.

### Save a content list (Design)

From **Publish** (`spa.jsp?entry=publish&section=design`), open **Design** then **Content lists**.
**Add content list** (or open an existing list). Enter a **name** (required), optional description,
type (modern vs legacy), generator or legacy URL, then **Save**. Unsaved field edits mark the form
dirty; leaving the editor prompts to discard. The shell posts
`POST …/sitemanage/publishingdesign/contentlists` (create) or
`PUT …/sitemanage/publishingdesign/contentlists/{contentListId}` (update).

HTTP **403** (not Admin or Designer) and **409** (content list name already exists) are shown in the
content-list editor error region — not as a successful save. Edition save and delivery-type save
are separate Design actions.

### Save a delivery type (Design)

From **Publish** (`spa.jsp?entry=publish&section=design`), open **Design** then **Delivery types**.
**Add** (or open an existing type). Enter a **name** and **bean name** (required), optional
description, then **Save**. Unsaved field edits mark the form dirty; leaving the editor prompts to
discard. The shell posts `POST …/sitemanage/publishingdesign/deliverytypes` (create) or
`PUT …/sitemanage/publishingdesign/deliverytypes/{deliveryTypeId}` (update).

HTTP **403** (not Admin or Designer) and **409** (delivery type name already exists) are shown in the
delivery-type editor error region — not as a successful save. Content-list save and edition save
are separate Design actions.

### Save a publishing context (Design)

From **Publish** (`spa.jsp?entry=publish&section=design`), open **Design** then
**Contexts / schemes**. **Add context** (or **Edit context** for the selected context).
Enter a **name** (required) and optional description, then **Save**. Unsaved field edits
mark the form dirty; leaving the editor prompts to discard. The shell posts
`POST …/sitemanage/publishingdesign/contexts` (create) or
`PUT …/sitemanage/publishingdesign/contexts/{contextId}` (update).

HTTP **403** (not Admin or Designer) and **409** (publishing context name already exists)
are shown in the context editor error region — not as a successful save. Location-scheme
save, delivery-type save, and edition save are separate Design actions.

### Save a location scheme (Design)

From **Publish** (`spa.jsp?entry=publish&section=design`), open **Design** then
**Contexts / schemes**. Choose a **context**, then **Add scheme** (or open an existing scheme).
Enter a **name** and **generator** (required), optional description, content type id, template id,
and parameters, then **Save**. Unsaved field edits mark the form dirty; leaving the editor prompts
to discard. The shell posts
`POST …/sitemanage/publishingdesign/contexts/{contextId}/schemes` (create) or
`PUT …/sitemanage/publishingdesign/schemes/{schemeId}` (update).

HTTP **403** (not Admin or Designer) and **409** (location scheme name already exists in that
context) are shown in the scheme editor error region — not as a successful save. Context save,
delivery-type save, and edition save are separate Design actions.

### Start or stop a publish job (Runtime)

From **Publish** (`spa.jsp?entry=publish&section=runtime`), choose a **site** and
**publish server**. The Runtime list shows editions for that server. **Start** queues
`POST …/sitemanage/publishingdesign/runtime/editions/{editionId}/start`. When a job is
running, **Stop** posts `POST …/sitemanage/publishingdesign/runtime/jobs/{jobId}/stop`
(falls back to ops `stopPublishing` if needed). The **Last result** status region shows
started/cancelled (or the job state) plus job id. Listing may pass `pubServerId` so only
editions on the selected server appear. Design edition save and Sites list filter are
separate sections.

### Search and filter publish logs

From **Publish** (`spa.jsp?entry=publish&section=logs`), the **Logs** section lists historical
publish jobs. Choose a **site**, optional **server id**, **days** window, and **max count**, then
click **Logs** to load rows (`POST …/sitemanage/pubstatus/logs`). Use **Status** (All / Failed /
Success) and **Search** to filter the loaded table by site, server, job id, or status without
another round trip. **Failures only (server)** sets `showOnlyFailures` on the logs request so the
server returns failed jobs only. Open **details** on a row for item-level log lines (those details
have their own text filter).

### Filter current jobs by site (Status)

From **Publish** → **Status** (`spa.jsp?entry=publish&section=status`), the current-jobs
table lists jobs already returned by `GET …/sitemanage/pubstatus/current`. **Filter Sites**
narrows that table by site name or site id (case-insensitive; no extra publish request).
Clear the field to restore the full list. When jobs are loaded but none match, Status shows
**No jobs match this site filter**. When the server returns no jobs at all, Status still shows
**No active publishing jobs**. **Stop** is unchanged and applies only to visible running jobs.

### Cancel or stop an in-flight publish job

From **Publish** → **Status**, or from a site workspace **Status** list, **Stop** is shown only for jobs whose status is running (not completed, failed, or already stopping). Confirm the dialog. The shell posts `POST …/publishmanagement/servers/stopPublishing/{jobId}`. Success refreshes the Status list. Dismissing the confirm does not call the server. Jobs that are not running have no Stop control. HTTP **403** (forbidden), **404** (unknown job), and **409** (job cannot be stopped — already finished or already stopping) are shown as errors in Status; they are **not** treated as success.

### Item Publish Now and Take Down (Content Explorer)

From **Content Explorer** (`spa.jsp?entry=explorer`), select a **page** or **asset** (not a folder). **Publish Now** demand-publishes the item; **Take Down** unpublishes it from its site. **Schedule** sets or clears item publish and removal dates (`GET …/getitemdates/{id}`, `POST …/setitemdates`). **Publishing History** shows item-level publish/takedown rows (`GET …/item/pubhistory/{id}`). Publish Now, Take Down, Stage, and Schedule confirm first. Application-level `FORBIDDEN` / `BADCONFIG` / `INVALID` responses are failures (the Server actions error region), not success. HTTP **404** / **403** on publishing history are errors in the history dialog, not empty success. See [Content Explorer](id:admin-content-explorer) for the exact URLs, linked-page confirm, schedule fields, and history dialog. Stage from Explorer is a separate action.

From the **React Content Editor** (`spa.jsp?entry=editor`) in **Edit** mode, **Publish now** demand-publishes the already-open page or asset after confirm (same sitemanage `publish/page/{id}` or `publish/resource/{id}` GETs). **View** mode stays read-only. `FORBIDDEN` / `BADCONFIG` is a failure on the editor host, not success.

## Virtual Sites and docs builds

For Git/filesystem, CSV/filesystem, SQL/database, or HTTP JSON Virtual Sites such as product documentation:

- Offline / CI builds use `scripts/build-cms-docs.bat` / `scripts/build-cms-docs.sh` to emit static HTML without a full CMS UI session. CSV trees can use `PSVirtualSiteBuildMain … csv-filesystem`. SQL trees use `PSVirtualSiteBuildMain … sql-database` (in-memory H2 only). Local object-key directories use `PSVirtualSiteBuildMain … object-storage` (no cloud credentials). Local iCalendar fixtures use `PSVirtualSiteBuildMain … icalendar` (`calendar.ics` or `_config.yaml` `icalendar.file`; no CalDAV).
- **Build** (`POST /sites/{nameOrId}/virtual/build`) writes a staging tree under
  `{install}/tmp/virtual-sites/{siteKey}` (or an optional `outputRoot`). Each build re-reads the
  current Git/filesystem, CSV tree (`csv-filesystem`), H2 `SELECT` (`sql-database`), HTTP
  JSON catalog (`http-json`: local fixture / loopback `http.url`), object-storage keys
  (`object-storage`: Markdown / HTML / JSON under a local `rootPath`), or rss-atom feeds
  (`rss-atom`: local `feed.xml` / `atom.xml` / `rss.file` or loopback `rss.url`), or
  iCalendar fixtures (`icalendar`: local `calendar.ics` / `icalendar.file`; no CalDAV). After
  `git pull`, a local Markdown edit, a CSV change, a `_config.yaml` change, a SQL
  `queryFile` / `sql.query` change, an H2 row change, a JSON catalog / `_config.yaml`
  edit, an object-storage Markdown / HTML / JSON key or `_config.yaml`
  (`objects.keys`) edit, an RSS/Atom fixture / `_config.yaml` edit, or an iCalendar
  fixture (`calendar.ics` / `icalendar.file`) / `_config.yaml` edit, run Build (or
  Publish) again — no CMS restart. File watchers are not used. `sql-database` requires `_config.yaml` with a `sql:` mapping
  (`jdbc:h2:mem:`; Oracle / MySQL / SQL Server URLs return **400**). `http-json` requires
  `_config.yaml` (versions plus `http.url` or `http.file`); `virtual.remoteUrl` is **400**.
  `object-storage` requires `_config.yaml` and a portable-safe local `rootPath`; leftover
  `virtual.remoteUrl` is **400**.
- **Publish** (`POST /sites/{nameOrId}/virtual/publish`) runs that build, then copies the
  assembled HTML/assets to the Site **filesystem publish location** (`IPSSite.root` / Site
  publishing root). Staging `_meta` files are not copied. Redirect HTML and `redirects.json`
  from optional `_redirects.yaml` are copied with the site. `sql-database` Publish is
  in-memory H2 only (`jdbc:h2:mem:`); Oracle / MySQL / SQL Server URLs return **400**.
  `http-json` Publish uses a local JSON fixture or loopback catalog (`http.url` /
  `http.file` in `_config.yaml`); leftover `virtual.remoteUrl` is **400** (no secrets on
  the envelope). `object-storage` Publish uses a portable-safe local object-key `rootPath`
  (Markdown / HTML / JSON keys; no cloud URLs, IAM, or access keys); leftover
  `virtual.remoteUrl` is **400**. `rss-atom` Publish uses a local RSS 2.0 / Atom
  fixture or loopback feed (`feed.xml` / `atom.xml` or `_config.yaml` `rss.file`); leftover
  `virtual.remoteUrl` and credential properties are **400** (no live feeds). `icalendar`
  Publish uses a local RFC 5545 fixture (`calendar.ics` or `_config.yaml` `icalendar.file`);
  leftover `virtual.remoteUrl` and credential properties are **400** (no CalDAV). REST **GET/PUT**
  can persist `virtual.sourceKind=object-storage` with that local `rootPath` (cloud URLs and
  credentials are **400**); REST **Build** runs that local bucket. REST **Preview**
  streams last-build HTML for that kind after a successful Build (`available=true`;
  missing build is `available=false` HTTP 200). REST **Preview** also streams last-build
  HTML for `rss-atom` (local RSS 2.0 / Atom fixture or loopback feed; no live remote
  feeds) and for `icalendar` (local RFC 5545 `calendar.ics` / `icalendar.file`; no CalDAV).
  REST **Publish** copies that last-build HTML to `IPSSite.root` for `rss-atom` and
  `icalendar` (`filesCopied > 0`; `_meta` skipped). Developer Sites **Preview assembled site** is
  shown for **RSS / Atom**, **iCalendar**, and **Sitemap XML** after a successful Build. Developer Sites can save **Object
  storage** (GET round-trips the kind), then **Build Virtual Site**, **Preview
  assembled site**, and **Publish Virtual Site**. Developer Sites can save **RSS / Atom**
  the same way, then **Build Virtual Site**, **Preview assembled site**, and
  **Publish Virtual Site** (local RSS/Atom fixture; leftover `virtual.remoteUrl` and
  credentials are **400**; no live feeds). Developer Sites can save **iCalendar**
  the same way, then **Build Virtual Site**, **Preview assembled site**, and
  **Publish Virtual Site** (local RFC 5545 fixture; leftover `virtual.remoteUrl` and
  credentials are **400**; no CalDAV). Developer Sites can save **Sitemap XML**
  the same way, then **Build Virtual Site**, **Preview assembled site**, and
  **Publish Virtual Site** (local `sitemap.xml` fixture; leftover `virtual.remoteUrl`
  and credentials are **400**; no live crawl). The endpoint does not accept an
  `outputRoot` body (always the default staging root).

### Publish a Virtual Site to the Site filesystem target

1. Sign in as **Admin**.
2. Configure the Site as a Git-filesystem, CSV-filesystem, SQL-database, HTTP JSON, object-storage, RSS / Atom, iCalendar, or Sitemap XML Virtual Site (see [Sites](id:admin-sites)). After you save **SQL database**, **Build Virtual Site** on Developer Sites runs the same in-memory H2 REST Build as Git/CSV. After you save **HTTP JSON**, **Build Virtual Site** then **Publish Virtual Site** copies assembled HTML to the Site filesystem root. After you save **Object storage**, **Build Virtual Site** then **Preview assembled site** and **Publish Virtual Site** run against that local object-key tree (`POST …/virtual/publish`; leftover `virtual.remoteUrl` is **400**; no cloud URLs or credentials). After you save **RSS / Atom**, **Build Virtual Site** then **Preview assembled site** and **Publish Virtual Site** run against that local RSS/Atom fixture (`POST …/virtual/publish`; leftover `virtual.remoteUrl` and credentials are **400**; no live feeds). After you save **iCalendar**, **Build Virtual Site** then **Preview assembled site** and **Publish Virtual Site** run against that local RFC 5545 fixture (`POST …/virtual/publish`; leftover `virtual.remoteUrl` and credentials are **400**; no CalDAV).
3. Set the Site **publishing filesystem root** (Site root) to a dedicated directory on the CMS
   host. Relative roots (legacy values such as `../CI_Home`) are resolved against the CMS
   install directory. Do **not** point it at `virtual.rootPath` (the Markdown or CSV source tree).
4. Confirm the source root exists on the host and that the publish directory is writable.
5. From **Developer → Sites → Site detail**, choose **Publish Virtual Site** (visible for
   **Git filesystem**, **CSV filesystem**, **SQL database**, **HTTP JSON**,
   **Object storage**, **RSS / Atom**, **iCalendar**, **Sitemap XML**, **Robots.txt**, **llms.txt**, **OpenAPI YAML**, **AsyncAPI YAML**, and **GraphQL SDL**; hidden for repository Sites). For **SQL database**, **HTTP JSON**,
   **Object storage**, **RSS / Atom**, and **iCalendar**, save the source, run **Build Virtual Site**, then
   **Publish Virtual Site**. For **Sitemap XML**, save the source, run **Build Virtual Site**, then **Publish Virtual Site**
   (builds then copies last-build local HTML; leftover `virtual.remoteUrl` and credentials
   fail closed; no live crawl). For **Robots.txt**, save the source, run **Build Virtual Site**, then **Publish Virtual Site**
   (builds then copies last-build local HTML; leftover `virtual.remoteUrl` and credentials
   fail closed; missing assemble is **400**; no live crawl). For **llms.txt**, save the source, run **Build Virtual Site**, then **Publish Virtual Site**
   (builds then copies last-build local HTML; leftover `virtual.remoteUrl` and credentials
   fail closed; missing assemble is **400**; no live HTTP fetch). For **OpenAPI YAML**, save the source, run **Build Virtual Site**, then **Publish Virtual Site**
   (builds then copies last-build local HTML; leftover `virtual.remoteUrl` and credentials
   fail closed; missing assemble is **400**; no live spec fetch). For **AsyncAPI YAML**, save the source, run **Build Virtual Site**, then **Publish Virtual Site**
   (builds then copies last-build local HTML; leftover `virtual.remoteUrl` and credentials
   fail closed; missing assemble is **400**; no live spec fetch). For **GraphQL SDL**, save the source, run **Build Virtual Site**, then **Publish Virtual Site**
   (builds then copies last-build local HTML; leftover `virtual.remoteUrl`, credentials, and `graphql.url`
   fail closed; missing assemble is **400**; no live GraphQL HTTP). For **JSON Schema**, save the source, run **Build Virtual Site**, then **Publish Virtual Site**
   (builds then copies last-build local HTML; leftover `virtual.remoteUrl`, credentials, `jsonschema.url`, and remote `$ref`/`$id` HTTP
   fail closed; missing assemble is **400**; no live HTTP schema fetch). The panel reports files copied and the destination path, or a
   clear error. Integrators can call `POST /services/sites/{nameOrId}/virtual/publish`
   instead (Git, CSV, SQL, HTTP JSON, object-storage, rss-atom, icalendar, sitemap-xml, robots-txt, llms-txt, openapi-yaml, asyncapi-yaml, graphql-sdl, and json-schema).
   `sitemap-xml` REST Publish copies assembled HTML from a local `sitemap.xml` fixture (leftover
   `virtual.remoteUrl`, credentials, and cloud URL `rootPath` are **400**; no live crawl).
   `robots-txt` REST Publish copies assembled HTML from a local `robots.txt` fixture (leftover
   `virtual.remoteUrl`, credentials, and cloud URL `rootPath` are **400**; no live crawl).
   `llms-txt` REST Publish copies assembled HTML from a local `llms.txt` fixture (leftover
   `virtual.remoteUrl`, credentials, and cloud URL `rootPath` are **400**; no live HTTP fetch).
   `openapi-yaml` REST Publish copies assembled HTML from a local `openapi.yaml` fixture (leftover
   `virtual.remoteUrl`, credentials, and cloud URL `rootPath` are **400**; no live spec fetch).
   `asyncapi-yaml` REST Publish copies assembled HTML from a local `asyncapi.yaml` fixture (leftover
   `virtual.remoteUrl`, credentials, and cloud URL `rootPath` are **400**; no live spec fetch).
   `graphql-sdl` REST Publish copies assembled HTML from a local `schema.graphql` fixture (leftover
   `virtual.remoteUrl`, credentials, cloud URL `rootPath`, and `graphql.url` are **400**; no live GraphQL HTTP).
   `json-schema` REST Publish copies assembled HTML from a local `schema.json` fixture (leftover
   `virtual.remoteUrl`, credentials, cloud URL `rootPath`, `jsonschema.url`, and remote `$ref`/`$id` HTTP are **400**; no live HTTP schema fetch).
   Developer Sites **Publish Virtual Site** is shown for **Sitemap XML**, **Robots.txt**, **llms.txt**, **OpenAPI YAML**, **AsyncAPI YAML**, **GraphQL SDL**, and **JSON Schema**. Run **Build Virtual Site** first
   if you only want staging output.
6. On success, the result includes `publishPath`, `filesCopied`, `pagesWritten`, and any
   link problems (`hasLinkProblems` can be true with HTTP 200).
7. Spot-check `index.html` (and version folders such as `8.2/`) under the Site root. If the
   source tree includes `_redirects.yaml`, also spot-check a redirect HTML path and
   `redirects.json`.

**Clear operator errors (HTTP 400, not a silent no-op):**

- Site is not Virtual (`virtual.sourceKind` blank or `repository`)
- Site filesystem publish root is not configured
- Publish root is unsafe (`..` after normalize) or is a file rather than a directory
- Publish root overlaps `virtual.rootPath` or the build staging tree
- Caller is not Admin (403)

See [Virtual Sites](id:developer-virtual-sites) and [Build product docs](id:developer-build-source#product-docs-build).

## Item publishing history

From **Publish** (`spa.jsp?entry=publish`), open **Status** or **Logs**. The **Item publishing history** panel looks up a page or asset by item id using the existing item-management API (`GET /services/itemmanagement/item/pubhistory/{id}`). It replaces the classic jQuery publishing-history dialog for this shell. **Content Explorer** uses the same API and panel: select a page or asset and choose **Publishing History**.

1. Sign in as an operator who can open Publish.
2. Open **Publish → Status** or **Publish → Logs**.
3. Enter the item id (content GUID such as `16777215-101-9`) and choose **View History**.
4. Rows show server, location, revision, date, operation, and status (newest first). A **FAILURE** row keeps the server error on the status cell title.

**Empty and error states:** If the item has never been published, the panel says there is no publishing history. A failed lookup is an error, not empty history and not success:

- HTTP **400** — the item id is blank or not a content id
- HTTP **403** — the caller is not allowed to read that item
- HTTP **404** — the item id is unknown
- Any other HTTP error keeps the server message

**Deep links** (same panel on Status or Logs):

- `spa.jsp?entry=publish&section=status&itemId={id}`
- `spa.jsp?entry=publish&section=logs&itemId={id}`
- Path form: `/cm/app/publish/status?itemId={id}` and `/cm/app/publish/logs?itemId={id}`

Use **Open Logs** / **Open Status** in the panel to switch those sections without leaving the lookup. Site-level job status and publish logs stay on the same tabs. Schedule dates and site-workspace takedown (below) are separate Publishing steps.

## Schedule publish dates (Publishing site workspace)

From **Publish** (`spa.jsp?entry=publish`), open a **site workspace** (Sites, then a site). The **Schedule** panel gets and sets item publish and removal dates using the existing item-management APIs (`GET /services/itemmanagement/item/getitemdates/{id}`, `POST /services/itemmanagement/item/setitemdates`). It replaces the classic jQuery `PercScheduleDialog` for this shell. Explorer still has its own Schedule action for a selected page or asset.

1. Sign in as an operator who can open Publish.
2. Open **Publish → Sites** and select a site (or deep-link `section=sites` with `siteId`).
3. Enter the item id (content GUID such as `16777215-101-9`) and choose **Load dates**.
4. Set or clear **Publish date** and **Removal date** (optional comments, 500 characters). Removal must be after publish when both are set. **Save dates** posts the `ItemDates` envelope.

**Empty and error states:** Blank item id is not a lookup. Invalid dates return **HTTP 400** (the panel shows the server validation message — past dates, unparseable values, or removal before publish). Forbidden updates return **HTTP 403** (reader or no assignment, or an application-level `FORBIDDEN` body). If another user has the item checked out, save returns **HTTP 409** and the panel shows that conflict. None of those responses are treated as success. After a successful save the panel reloads the stored publish and removal dates. The optional comment is the workflow note sent with the save; it is not returned by the dates lookup, so it stays in the form until the next lookup. Clearing both dates and saving removes the schedule.

**Deep links:**

- `spa.jsp?entry=publish&section=sites&siteId={siteId}&itemId={id}`
- Path form: `/cm/app/publish/sites?siteId={siteId}&itemId={id}`

Item publishing history stays on Status / Logs. Site-workspace **Publish now** and takedown are below.

## Publish now from the Publishing site workspace

From **Publish** (`spa.jsp?entry=publish`), open a **site workspace** (Sites, then a site). The **Publish Now** panel demand-publishes a selected **page** or **asset** using the same sitemanage paths as Content Explorer and the React editor (`GET /services/sitemanage/publish/page/{id}` or `/resource/{id}`). It replaces the classic jQuery `PercItemPublisherService.publishItem` action for this shell. Explorer still has its own **Publish Now** action for a Finder row.

1. Sign in as an **Admin** (or an operator who can demand-publish).
2. Open **Publish → Sites** and select a site (or deep-link `section=sites` with `siteId`).
3. Enter the item id (content GUID such as `16777215-101-9`), choose **Page** or **Asset**, and choose **Review publish now**.
4. Confirm **Publish this item now?** then **Publish Now**. The site workspace job list refreshes after a successful start.

**Empty and error states:** Blank or invalid item id is not a publish. HTTP **403** (non-Admin or locked/forbidden item) and HTTP **400** (validation) are shown in the panel — they are **not** treated as success. HTTP **404** (unknown id) shows **Item not found**. HTTP 200 with application-level `FORBIDDEN` or `BADCONFIG` is also a failure (same as Explorer). Stage / unstage have their own panels; the **Available publishing actions** menu above them jumps to each panel for the same item.

**Deep links:**

- `spa.jsp?entry=publish&section=sites&siteId={siteId}&itemId={id}`
- Path form: `/cm/app/publish/sites?siteId={siteId}&itemId={id}`

## Take down from the Publishing site workspace

From **Publish** (`spa.jsp?entry=publish`), open a **site workspace** (Sites, then a site). The **Remove From Site** panel takes down (unpublishes) a page or asset using the same sitemanage paths as Content Explorer and classic Finder (`GET /services/sitemanage/publish/takedown/page/{id}` or `/resource/{id}`; `PUT` of the linked-page list when that list is non-empty). Take down does **not** delete the CMS content item. Linked pages come from `GET /services/itemmanagement/item/findLinkedItems/{id}` and are listed on confirm (up to ten). A failed linked-item lookup still proceeds with the confirm (classic Finder).

1. Sign in as an operator who can open Publish.
2. Open **Publish → Sites** and select a site (or deep-link `section=sites` with `siteId`).
3. Enter the item id (content GUID such as `16777215-101-9`), choose **Page** or **Asset**, and choose **Review take down**.
4. Confirm lists linked page paths when present. Choose **Take down** to unpublish.

**Empty and error states:** Blank item id is not a takedown. HTTP 200 with application-level `FORBIDDEN`, `BADCONFIG`, `NOSTAGING_SERVERS`, or `INVALID` is a failure — the panel shows the server warning and does **not** treat the item as unpublished. HTTP **400** (validation), **403** (forbidden), **404** (item not found), and **409** (someone else is editing the item) are shown in the panel and are **not** success. The content item remains in the CMS. Content Explorer Take Down remains the selection-based action for a Finder row.

**Deep links:**

- `spa.jsp?entry=publish&section=sites&siteId={siteId}&itemId={id}`
- Path form: `/cm/app/publish/sites?siteId={siteId}&itemId={id}`

Item publishing history stays on Status / Logs. Schedule dates stay on the same site workspace.

## Stage and Remove from Staging (Publishing site workspace)

From **Publish** (`spa.jsp?entry=publish`), open a **site workspace** (Sites, then a site). The **Stage / Remove From Staging** panel stages or removes-from-staging a selected **page** or **asset** using the same sitemanage paths as Content Explorer and classic Finder (`GET /services/sitemanage/publish/page/staging/{id}` or `/resource/staging/{id}` to stage; `GET /services/sitemanage/publish/takedown/page/staging/{id}` or `/takedown/resource/staging/{id}` to remove from staging). It replaces the classic jQuery `PercItemPublisherService.stageItem` / `removeItemFromStaging` actions for this shell. Explorer still has its own **Stage** and **Remove from Staging** actions for a Finder row.

1. Sign in as an **Admin** (or an operator who can stage).
2. Open **Publish → Sites** and select a site (or deep-link `section=sites` with `siteId`).
3. Enter the item id (content GUID such as `16777215-101-9`), choose **Page** or **Asset**, choose **Stage** or **Remove from staging**, and choose **Review stage**.
4. Confirm then submit. The site workspace job list refreshes after a successful stage / unstage.

**Empty and error states:** Blank or invalid item id is not a stage. HTTP **403** (non-Admin or locked item) and HTTP **400** (validation) are shown in the panel — they are **not** treated as success. HTTP **404** (unknown id) shows **Item not found**. HTTP 200 with application-level `FORBIDDEN`, `BADCONFIG`, `NOSTAGING_SERVERS`, or `INVALID` is also a failure (same as Explorer and classic Finder). Take down and publish-now are separate panels on the same workspace.

**Deep links:**

- `spa.jsp?entry=publish&section=sites&siteId={siteId}&itemId={id}&action={stage|unstage}`
- Path form: `/cm/app/publish/sites?siteId={siteId}&itemId={id}&action={stage|unstage}`

## Available publishing actions menu (Publishing site workspace)

From **Publish** (`spa.jsp?entry=publish`), open a **site workspace** (Sites, then a site). The **Available publishing actions** menu calls the existing sitemanage endpoint (`GET /services/sitemanage/publish/publishingActions/{id}`) and renders one button per server row — **Publish**, **Schedule...**, **Remove from Site**, **Stage**, **Remove from Staging**. It replaces the classic jQuery `PercItemPublisherService` get-publishing-actions menu for this shell; it adds no new REST and reuses the publish-now, schedule, takedown, and stage panels below it.

1. Sign in as an **Admin** (or an operator with publish rights on the item).
2. Open **Publish → Sites** and select a site (or deep-link `section=sites` with `siteId` and `itemId`).
3. Enter the item id (or follow the deep-linked one) and choose **Load actions**.
4. Rows the server marks unavailable render **disabled** — they cannot be clicked. Unknown action names are skipped.
5. Choose an enabled action to jump to its panel (publish now, schedule, takedown, stage) for the same item.

**Empty and error states:** Blank or invalid item id is not a lookup. HTTP **403** (non-Admin / no publish rights) shows **Publish Forbidden** — it is **not** treated as success. HTTP **404** (unknown id) shows **Item not found**. An item with no applicable actions shows **No publishing actions for this item**.

## Failure modes to watch

- Missing template/variant or broken relationship links
- File permission errors on delivery paths
- Partial publish after mid-job failure (re-run after fixing root cause)
- DTS connectivity issues that surface as broken dynamic widgets on an otherwise static site

## Related

- [Sites & content structure](id:admin-sites)
- [Server operations](id:admin-server-ops)
