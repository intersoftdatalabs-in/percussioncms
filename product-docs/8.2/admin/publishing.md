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

**Empty and error states:** If the item has never been published, the panel says there is no publishing history. A failed lookup (HTTP **404** unknown id, HTTP **403** forbidden, or another HTTP error / unexpected server message) is shown as an error in the panel — it is not treated as success or as empty history.

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

**Empty and error states:** Blank item id is not a lookup. Invalid dates return **HTTP 400** (the panel shows the server validation message — past dates, unparseable values, or removal before publish). Forbidden updates return **HTTP 403** (or an application-level `FORBIDDEN` body) — the panel shows that error and does **not** treat the save as success. Clearing both dates and saving removes the schedule.

**Deep links:**

- `spa.jsp?entry=publish&section=sites&siteId={siteId}&itemId={id}`
- Path form: `/cm/app/publish/sites?siteId={siteId}&itemId={id}`

Item publishing history stays on Status / Logs. Site-workspace takedown is below.

## Take down from the Publishing site workspace

From **Publish** (`spa.jsp?entry=publish`), open a **site workspace** (Sites, then a site). The **Remove From Site** panel takes down (unpublishes) a page or asset using the same sitemanage paths as Content Explorer and classic Finder (`GET /services/sitemanage/publish/takedown/page/{id}` or `/resource/{id}`; `PUT` of the linked-page list when that list is non-empty). Linked pages come from `GET /services/itemmanagement/item/findLinkedItems/{id}` and are listed on confirm (up to ten). A failed linked-item lookup still proceeds with the confirm (classic Finder).

1. Sign in as an operator who can open Publish.
2. Open **Publish → Sites** and select a site (or deep-link `section=sites` with `siteId`).
3. Enter the item id (content GUID such as `16777215-101-9`), choose **Page** or **Asset**, and choose **Review take down**.
4. Confirm lists linked page paths when present. Choose **Take down** to unpublish.

**Empty and error states:** Blank item id is not a takedown. HTTP 200 with application-level `FORBIDDEN`, `BADCONFIG`, `NOSTAGING_SERVERS`, or `INVALID` is a failure — the panel shows the server warning and does **not** treat the item as unpublished. HTTP **403** is the same (not success). Content Explorer Take Down remains the selection-based action for a Finder row.

**Deep links:**

- `spa.jsp?entry=publish&section=sites&siteId={siteId}&itemId={id}`
- Path form: `/cm/app/publish/sites?siteId={siteId}&itemId={id}`

Item publishing history stays on Status / Logs. Schedule dates stay on the same site workspace.

## Failure modes to watch

- Missing template/variant or broken relationship links
- File permission errors on delivery paths
- Partial publish after mid-job failure (re-run after fixing root cause)
- DTS connectivity issues that surface as broken dynamic widgets on an otherwise static site

## Related

- [Sites & content structure](id:admin-sites)
- [Server operations](id:admin-server-ops)
