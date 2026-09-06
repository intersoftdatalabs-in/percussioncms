---
id: developer-virtual-sites
title: Virtual Sites
description: Git/filesystem Virtual Sites for documentation and external content
version: "8.2"
order: 52
tags: [developer, virtual-sites]
---

# Virtual Sites

**Virtual Sites** are Sites whose content originates outside the traditional Percussion content
repository. Phase 1 delivers a **Git / filesystem** adapter aimed at product documentation. A
**CSV / filesystem** adapter (`csv-filesystem`) discovers the same assemble pipeline from
CSV files. Operators can run it offline (CLI) or from CMS REST
`POST /sites/{nameOrId}/virtual/build` and `POST /sites/{nameOrId}/virtual/publish`.
Preview REST (`GET …/virtual/preview`) is last-output based and works for
`git-filesystem`, `csv-filesystem`, `sql-database`, `http-json`, `object-storage`,
`rss-atom`, `icalendar`, and `sitemap-xml` after a successful Build (CLI preview requires
the default output root). Developer **Sites** shows **Build Virtual
Site**, **Preview assembled site**, and **Publish Virtual Site** for **CSV filesystem**,
**SQL database**, **HTTP JSON**, Git filesystem, and **Object storage**. **RSS / Atom**
shows **Build Virtual Site**, **Preview assembled site**, and **Publish Virtual Site**.
**Sitemap XML** shows **Publish Virtual Site** (copies last-build local HTML after a
local `sitemap.xml` Build; leftover `virtual.remoteUrl` and crawl credentials fail
closed). Traditional **Repository** hides that chrome.

A **SQL / database** adapter (`sql-database`) discovers rows from a JDBC `SELECT` against
**in-memory H2** (`jdbc:h2:mem:`). Required columns match CSV (`id`, `title`, `body`).
Operators persist the kind with REST `PUT /sites/{nameOrId}/virtual` (`sourceKind=sql-database`
plus a safe `rootPath`); JDBC URL/user/query stay in `_config.yaml`. REST Build, preview,
and publish use the H2 adapter. Developer **Sites** can select **SQL database**, save
`sourceKind=sql-database`, and show **Build Virtual Site**, **Preview assembled site**,
and **Publish Virtual Site** (like the Git / CSV filesystem chrome; the **Publish**
control becomes useful after a successful Build). Repository kind still hides that
chrome. Oracle, MySQL, and SQL Server URLs are rejected.

An **HTTP JSON / Headless** adapter (`http-json`) discovers pages from an HTTP GET of a JSON
catalog or from a local JSON fixture under `virtual.rootPath`. Required page field `id`;
`title` + `body` assemble like CSV/SQL. Operators persist the kind with REST
`PUT /sites/{nameOrId}/virtual` and from **Developer → Sites** (`sourceKind=http-json`
plus a safe `rootPath`; GET round-trips the kind). After save, Developer **Sites** shows
**Build Virtual Site** (`POST …/virtual/build`), **Preview assembled site**, and
**Publish Virtual Site**. SPI/CLI
assemble is `PSVirtualSiteBuildMain … http-json`. REST **Build** runs the same adapter
against a local JSON fixture or loopback catalog (`pagesWritten > 0`). REST **Preview**
(`GET …/virtual/preview`) streams last-build HTML after that Build (`available=true`
+ home HTML; missing build is `available=false` HTTP 200). REST **Publish**
(`POST …/virtual/publish`) copies that last-build HTML to the Site filesystem root
(`IPSSite.root`). Developer Sites **Preview assembled site** and **Publish Virtual
Site** chrome are shown after save (same as Git/CSV/SQL). Open JSON only (no API keys). Remote URLs are SSRF
fail-closed (`http`/`https`, no userinfo, no off-loopback redirects).
`virtual.remoteUrl` stays **400** (no secrets on the REST envelope).

An **object-storage** adapter (`object-storage`) treats a local directory as an object-key
bucket. Object keys are portable relative paths under `virtual.rootPath` (NIO `Path` /
`Files`; no remaining `..`). Discover loads Markdown, HTML, and JSON (catalog or
single-page object). Page `id` comes from frontmatter / JSON `id` or the filename stem;
`title` + `body` assemble like HTTP JSON / CSV. Optional `_config.yaml` `objects.keys`
lists keys instead of walking version folders. **No cloud SDK, access keys, or network**
— this is a local fixture contract, not live S3/MinIO/Azure. SPI/CLI assemble is
`PSVirtualSiteBuildMain … object-storage`. Operators persist the kind with REST
`PUT` / `GET /sites/{nameOrId}/virtual` and a portable-safe local `rootPath`. REST
**Build** (`POST …/virtual/build`) runs the same adapter against that local bucket
(`pagesWritten > 0`). REST **Preview** (`GET …/virtual/preview`) streams last-build HTML
after a successful Build (`available=true` + home HTML; missing build is
`available=false` HTTP 200). REST **Publish** (`POST …/virtual/publish`) copies assembled
HTML to the Site filesystem root for a local object-key `rootPath` (leftover
`virtual.remoteUrl` is **400**; no cloud URLs, IAM, or access keys). Cloud URLs
(`s3://`, `gs://`, `azure://`, `http(s)://`) and credential properties (access keys,
secrets, connection strings) return **400**. `virtual.remoteUrl` is **400**. Developer
**Sites** can select **Object storage**, save `sourceKind=object-storage`, GET-roundtrip
the kind, then **Build Virtual Site**, **Preview assembled site**, and **Publish Virtual
Site** (local object-key directory only).

An **RSS / Atom** adapter (`rss-atom`) discovers pages from a **local RSS 2.0 or Atom XML
fixture** under `virtual.rootPath` (`feed.xml` / `atom.xml`, or `_config.yaml` `rss.file`).
Optional `rss.url` is **loopback HTTP only** (in-process test servers). This is a
syndication SPI — **no live cloud feeds, no Authorization / API keys, no userinfo**.
`virtual.remoteUrl` and credential properties are rejected. SPI/CLI assemble is
`PSVirtualSiteBuildMain … rss-atom` (`pagesWritten > 0` from a temp fixture). REST GET/PUT
persist round-trips this kind with a portable-safe local `rootPath`. REST **Build**
(`POST …/virtual/build`) runs the same local/loopback fixture (`pagesWritten > 0`). REST
**Preview** (`GET …/virtual/preview`) streams last-build HTML after a successful REST or
CLI assemble at the default output root (`available=true` + home HTML; missing build is
`available=false` HTTP 200). REST **Publish** (`POST …/virtual/publish`) copies that
last-build HTML to the Site filesystem root (`IPSSite.root`; leftover `virtual.remoteUrl`
and credentials are **400**; `_meta` skipped). Developer **Sites** can **Build Virtual
Site** after save (local fixture only), **Preview assembled site**, and **Publish Virtual Site**. No live remote feeds.

An **RSS / Atom** adapter (`rss-atom`) reads a **local** RSS or Atom fixture directory
under `virtual.rootPath` (portable path; no remaining `..`). REST **GET/PUT**
`/sites/{nameOrId}/virtual` round-trips `sourceKind=rss-atom` with that local
`rootPath`. Leftover `virtual.remoteUrl`, live feed URLs, and credential properties
are **400** (no secrets on the REST envelope). Developer **Sites** can select
**RSS / Atom**, save, GET-roundtrip the kind, and **Build Virtual Site** (local
`feed.xml` / `atom.xml` / `_config.yaml` `rss.file` only), then **Preview assembled
site** and **Publish Virtual Site**.

An **iCalendar** adapter (`icalendar`) discovers pages from a **local RFC 5545
`.ics` fixture** under `virtual.rootPath` (`calendar.ics`, or `_config.yaml`
`icalendar.file`). Each `VEVENT` maps `UID` / `SUMMARY` / `DTSTART` /
`DESCRIPTION` into assemble `id` / `title` / `body`. This is a **local fixture
SPI** — **no CalDAV, no live remote `.ics` URLs, no API keys**. `virtual.remoteUrl`
and credential properties are rejected. SPI/CLI assemble is
`PSVirtualSiteBuildMain … icalendar` (`pagesWritten > 0` from a temp fixture).
REST `GET` / `PUT /sites/{nameOrId}/virtual` round-trips `sourceKind=icalendar` with a
portable-safe local `rootPath` (leftover `virtual.remoteUrl`, credentials, and cloud URL
`rootPath` are **400**; no CalDAV). REST **Build** (`POST …/virtual/build`) writes HTML
from that local `calendar.ics` / `icalendar.file` fixture (`pagesWritten > 0`). REST
**Preview** (`GET …/virtual/preview`) reports last-build status (`available=true` after
Build; missing build is `available=false` HTTP **200**). REST **Publish**
(`POST …/virtual/publish`) copies assembled HTML to `IPSSite.root`. Developer **Sites**
can select **iCalendar**, save, GET-roundtrip the kind, and **Build Virtual Site** (local
`calendar.ics` / `icalendar.file` only), then **Preview assembled site** and **Publish
Virtual Site**.

A **sitemap XML** adapter (`sitemap-xml`) discovers pages from a **local `sitemap.xml`**
(urlset / sitemapindex of local file URLs) under `virtual.rootPath` (`sitemap.xml`, or
`_config.yaml` `sitemap.file`). Each `<loc>` that resolves to a portable file under the
site root (or a loopback `http(s)` test URL) assembles Markdown/HTML. Title comes from the
last path segment; optional `<lastmod>` is noted in the body. This is a **local sitemap
adapter** — **no live crawl, no robots.txt fetch, no authenticated remotes**.
`virtual.remoteUrl`, credential properties, `sitemap.url`, and non-loopback `http(s)` locs
are rejected. SPI/CLI assemble is `PSVirtualSiteBuildMain … sitemap-xml` (`pagesWritten > 0`
from a temp fixture). REST `GET` / `PUT /sites/{nameOrId}/virtual` round-trips
`sourceKind=sitemap-xml` with a portable-safe local `rootPath` (leftover `virtual.remoteUrl`,
credentials, and cloud URL `rootPath` are **400**; no live crawl). REST **Build**
(`POST …/virtual/build`) writes HTML from that local fixture (`pagesWritten > 0`). REST
**Preview** (`GET …/virtual/preview`) reports last-build status (`available=true` after
Build; missing build is `available=false` HTTP **200**) and streams last-build local HTML
(no live crawl). REST **Publish** (`POST …/virtual/publish`) copies assembled HTML to `IPSSite.root` after a local sitemap.xml Build (leftover `virtual.remoteUrl`, credentials, and cloud URL `rootPath` are **400**; no live crawl). Developer Sites can save and
GET-roundtrip `sourceKind=sitemap-xml` (local `rootPath` only; leftover `virtual.remoteUrl`
and crawl credentials are never sent), then **Build Virtual Site**, **Preview assembled site**,
and **Publish Virtual Site**.

A **robots.txt** adapter (`robots-txt`) discovers pages from a **local `robots.txt`**
under `virtual.rootPath` (`robots.txt`, or `_config.yaml` `robots.file`). Each
`User-agent` group maps into assemble `id` / `title` / `body`. A fixture with no
`User-agent` still emits one HTML page from the file. This is a **local fixture SPI**
— **no live crawl, no remote robots.txt fetch, no authenticated remotes**.
`virtual.remoteUrl`, credential properties, `robots.url`, cloud `rootPath` URLs, and
`Sitemap:` values with a remote/cloud scheme are rejected. SPI/CLI assemble is
`PSVirtualSiteBuildMain … robots-txt` (`pagesWritten > 0` from a temp fixture). REST
`GET` / `PUT /sites/{nameOrId}/virtual` round-trips `sourceKind=robots-txt` with a
portable-safe local `rootPath` (leftover `virtual.remoteUrl`, credentials, and cloud
URL `rootPath` are **400**; no live crawl). REST **Build** (`POST …/virtual/build`)
writes HTML from that local fixture (`pagesWritten > 0`). REST **Preview**
(`GET …/virtual/preview`) reports last-build status (`available=true` after Build;
missing build is `available=false` HTTP **200**) and streams last-build local HTML
(no live crawl). Developer Sites can save and GET-roundtrip `sourceKind=robots-txt`
(local `rootPath` only; leftover `virtual.remoteUrl` and crawl credentials are never
sent), then **Build Virtual Site** and **Preview assembled site**. Publish stays a
later slice.

A **llms.txt** adapter (`llms-txt`) discovers pages from a **local `llms.txt`**
under `virtual.rootPath` (`llms.txt`, or `_config.yaml` `llms.file`). Each
markdown list link (`- [title](href)`) maps into assemble `id` / `title` / `body`. A
fixture with no links still emits one HTML page from the file. This is a **local
fixture SPI** — **no live HTTP fetch, no remote llms.txt fetch, no authenticated
remotes**. `virtual.remoteUrl`, credential properties, `llms.url`, cloud `rootPath`
URLs, and link hrefs with a remote/cloud scheme are rejected. SPI/CLI assemble is
`PSVirtualSiteBuildMain … llms-txt` (`pagesWritten > 0` from a temp fixture). REST
persist, Build/Preview/Publish, and Developer Sites chrome stay later slices.

An **OpenAPI 3 YAML** adapter (`openapi-yaml`) discovers pages from a **local
`openapi.yaml`** under `virtual.rootPath` (`openapi.yaml`, or `_config.yaml`
`openapi.file`). Each path/operation maps into assemble `id` / `title` / `body`.
A fixture with no operations still emits one HTML page from `info`. This is a
**local fixture SPI** — **no live HTTP spec fetch, no remote OpenAPI URLs, no
authenticated remotes**. `virtual.remoteUrl`, credential properties, `openapi.url`,
cloud `rootPath` URLs, and remote `$ref` values are rejected. SPI/CLI assemble is
`PSVirtualSiteBuildMain … openapi-yaml` (`pagesWritten > 0` from a temp fixture).
REST persist, Build/Preview/Publish, and Developer Sites chrome stay later slices.

Operators can create a **Virtual** type from **Content Explorer → Create Site** or
**Navigation → New Site**. That flow does not prompt for managed navigation or a page template.
After the site folder is created, an optional Git root is saved with
`PUT /services/sites/{nameOrId}/virtual` using the `VirtualSiteProperties` envelope. Full
`rootPath` / config-file editing stays on **Developer → Sites**. See
[Sites & content structure](id:admin-sites).

## Goals

- Keep Git as the system of record for documentation (PR review, lockstep with product changes).
- Use Percussion assemblers as the site generator (Markdown → HTML).
- Provide stable page identities (`frontmatter.id`) for lightweight link checks / participants.
- Leave the door open for additional adapters (object storage) without renaming Site → Channel.
  SQL / H2 (`sql-database`) is implemented as an SPI and exposed on Site REST GET/PUT/Build
  and last-build Preview (`GET …/virtual/preview`). HTTP JSON (`http-json`) is implemented as
  an SPI (CLI assemble) and allow-listed on Site REST GET/PUT/Build plus last-build Preview
  (`sourceKind=http-json` plus a safe `rootPath`; local JSON fixture / loopback). Developer
  Sites can save/GET-roundtrip `sourceKind=http-json` and then **Build Virtual Site**,
  **Preview assembled site**, and **Publish Virtual Site**. REST **Publish** copies
  assembled HTML to `IPSSite.root`. Object storage (`object-storage`) is implemented as a
  local object-key SPI (CLI assemble) and REST **GET/PUT/Build/Preview/Publish**. REST
  **GET/PUT** round-trips `object-storage` (safe local `rootPath`; no cloud URLs or
  credentials). REST **Build** (`POST …/virtual/build`) writes HTML from that local bucket.
  REST **Preview** streams last-build HTML after a successful Build (`available=true`;
  missing build is `available=false` HTTP 200). REST **Publish** copies assembled HTML to
  `IPSSite.root` for a local object-key fixture. Developer Sites can save and GET-roundtrip
  `sourceKind=object-storage`, then **Build Virtual Site**, **Preview assembled
  site**, and **Publish Virtual Site**. RSS / Atom (`rss-atom`) is implemented as a
  local/loopback syndication SPI (CLI assemble) plus REST **GET/PUT** persist, REST **Build**
  (`POST …/virtual/build`) against a local RSS/Atom fixture (`pagesWritten > 0`; leftover
  `virtual.remoteUrl`, credentials, and cloud `rootPath` are **400**), and REST **Preview**
  (`GET …/virtual/preview`) after a successful assemble (`available=true`; missing build is
  `available=false` HTTP 200). REST **Publish** (`POST …/virtual/publish`) copies assembled
  HTML to `IPSSite.root` for a local RSS/Atom fixture (leftover `virtual.remoteUrl` and
  credentials are **400**; no live feeds). Developer Sites can **Build Virtual Site** for
  `rss-atom` after save (local fixture only), **Preview assembled site**, and **Publish Virtual Site**.
  iCalendar (`icalendar`) is a **local RFC 5545 `.ics` SPI** (CLI assemble from
  `calendar.ics` / `icalendar.file`; no CalDAV). REST **GET/PUT** `/sites/{nameOrId}/virtual`
  round-trips `sourceKind=icalendar` with a portable-safe local `rootPath` (leftover
  `virtual.remoteUrl`, credentials, and cloud URL `rootPath` are **400**). REST **Build**
  (`POST …/virtual/build`) writes HTML from that local fixture (`pagesWritten > 0`). REST
  **Preview** streams last-build HTML (`available=true`; missing build is `available=false`
  HTTP 200). REST **Publish** copies assembled HTML to `IPSSite.root`. Developer Sites
  can save and GET-roundtrip `sourceKind=icalendar`, then **Build Virtual Site**,
  **Preview assembled site**, and **Publish Virtual Site**. Sitemap XML (`sitemap-xml`) is a
  **local `sitemap.xml` SPI** (CLI assemble from `sitemap.xml` / `sitemap.file`; no live
  crawl). REST **GET/PUT** `/sites/{nameOrId}/virtual` round-trips `sourceKind=sitemap-xml`
  with a portable-safe local `rootPath` (leftover `virtual.remoteUrl`, credentials, and cloud
  URL `rootPath` are **400**; no live crawl). REST **Build** (`POST …/virtual/build`) writes
  HTML from that local fixture (`pagesWritten > 0`; leftover `virtual.remoteUrl`, credentials,
  and cloud `rootPath` are **400**). REST **Preview** (`GET …/virtual/preview`) streams
  last-build local HTML (`available=true`; missing build is `available=false` HTTP 200; leftover
  `virtual.remoteUrl` and credentials are **400**; no live crawl). REST **Publish**
  (`POST …/virtual/publish`) copies assembled HTML to `IPSSite.root`. Developer Sites can
  save and GET-roundtrip `sourceKind=sitemap-xml` (local `rootPath` only; leftover
  `virtual.remoteUrl` and crawl credentials are never sent), then **Build Virtual Site**,
  **Preview assembled site**, and **Publish Virtual Site** (last-build local HTML;
  missing build stays unavailable).

## Source tree contract

Repository root:

```text
product-docs/
  _config.yaml
  _redirects.yaml    # optional
  _theme/
  assets/
  8.2/
    index.md
    getting-started/
    admin/
    developer/
    reference/
```

- Folder structure drives navigation hierarchy.
- `index.md` is the landing page for a section.
- One top-level folder per documentation version (for example `8.2/`).
- Assembled pages bind theme placeholders including <code>&#36;{toc}</code> (h2–h3 heading TOC). See [Site configuration](id:reference-site-config).
- Optional `_redirects.yaml` next to `_config.yaml` emits static redirect HTML and
  `redirects.json`. Missing file is a no-op. Targets must be relative or same-site;
  open redirects fail the build. Contract: [Site configuration](id:reference-site-config).

## Stable identity

Every Markdown page requires YAML frontmatter with a unique **`id`** within the version.
Paths may change; `id` should not. Cross-page links use stable id links:

```markdown
See [Installation](id:install-overview).
```

Details: [Frontmatter reference](id:reference-frontmatter).

## Virtual participant registry lifetime

During a Virtual Site build, each page’s frontmatter **`id`** is registered against its published
HTML path in the **virtual participant registry** (`IPSVirtualParticipantService`). Phase 1 does
**not** create CMS content IDs or `PSX_MANAGEDLINK` rows.

| Mode | Behavior |
|------|----------|
| **Process-scoped (default)** | Registrations live in memory until the process exits, or until `clear(siteKey)` / `clearAll()` is called (SPI reset API). Unit tests and one-shot builds use this mode when no store directory is supplied. |
| **Path-backed (optional)** | Construct the registry with a portable `java.nio.file.Path` base (CLI uses `outputRoot/_meta`). Existing `participants-<siteKey>.jsonl` files are loaded on construct; `flush(siteKey)` rewrites that site’s file. Survives JVM restart when the same Path base is reused. |
| **Full rebuild** | A complete site build **clears** that site key, then upserts every discovered page, then flushes. A second build therefore does not keep pages removed from the source tree, and does not lose current ids. |
| **Current filesystem** | Each build reloads `_config.yaml` and re-reads every Markdown/frontmatter file, CSV row, sql-database `SELECT` (`sql.query` or current `sql.queryFile` bytes plus H2 rows), http-json catalog (`http.url` / `http.file` or default `pages.json`), object-storage blobs (Markdown / HTML / JSON keys under `virtual.rootPath`), rss-atom feeds (`rss.file` / `feed.xml` / `atom.xml` / loopback `rss.url`), and iCalendar fixtures (`icalendar.file` / `calendar.ics`), sitemap fixtures (`sitemap.file` / `sitemap.xml` loc, lastmod, and path), and robots.txt fixtures (`robots.file` / `robots.txt`), llms.txt fixtures (`llms.file` / `llms.txt`), and OpenAPI YAML fixtures (`openapi.file` / `openapi.yaml`). The CMS process does **not** keep a parsed-page cache across builds. After `git pull`, a CSV/`_config.yaml` edit, a SQL `_config.yaml`/`queryFile` or H2 row edit, a JSON catalog edit, an object-key edit, an RSS/Atom fixture edit, an iCalendar fixture edit, a sitemap.xml loc/lastmod/path or `_config.yaml` `sitemap.file` edit, a robots.txt or `_config.yaml` `robots.file` edit, an llms.txt or `_config.yaml` `llms.file` edit, an OpenAPI YAML or `_config.yaml` `openapi.file` edit, or a local Markdown edit under `virtual.rootPath`, run **Build Virtual Site** (or the offline docs script) again — **no JVM / CMS restart** is required. File watchers are not used; the next explicit build is the refresh. |

Operators can treat the JSONL under the build meta directory as a diagnostic dump of stable ids after
an offline docs build. The registry is **not** a substitute for Git as the system of record.

## Site properties (CMS)

When a Percussion Site is configured as virtual (Phase 1 property contract — no new `RXSITES`
columns), set these Site properties. The server helper
`com.percussion.services.virtualsite.PSVirtualSiteHelper` validates the contract before a Site is
treated as a safe Virtual Site source.

| Property | Required | Example | Meaning |
|----------|----------|---------|---------|
| `virtual.sourceKind` | Yes (for Virtual) | `git-filesystem`, `csv-filesystem`, `sql-database`, `http-json`, `object-storage`, `rss-atom`, `icalendar`, `sitemap-xml`, `robots-txt`, `llms-txt`, or `openapi-yaml` | Adapter wire name. **Allow-list:** `git-filesystem`, `csv-filesystem`, `sql-database`, `http-json`, `object-storage`, `rss-atom`, `icalendar`, `sitemap-xml`, `robots-txt`, `llms-txt`, `openapi-yaml`. **`openapi-yaml`** is a local OpenAPI 3 YAML SPI (`openapi.yaml` or `_config.yaml` `openapi.file`; no live HTTP spec fetch). SPI/CLI assemble is `PSVirtualSiteBuildMain … openapi-yaml` (`pagesWritten > 0`). Leftover `virtual.remoteUrl`, credentials, and cloud URL `rootPath` are rejected. REST persist/Build/Preview/Publish stay later slices. **`llms-txt`** is a local `llms.txt` SPI (`llms.txt` or `_config.yaml` `llms.file`; no live HTTP fetch). SPI/CLI assemble is `PSVirtualSiteBuildMain … llms-txt` (`pagesWritten > 0`). Leftover `virtual.remoteUrl`, credentials, and cloud URL `rootPath` are rejected. REST persist/Build/Preview/Publish and Developer Sites chrome stay later slices. **`robots-txt`** is a local `robots.txt` SPI (`robots.txt` or `_config.yaml` `robots.file`; no live crawl). SPI/CLI assemble is `PSVirtualSiteBuildMain … robots-txt` (`pagesWritten > 0`). Leftover `virtual.remoteUrl`, credentials, and cloud URL `rootPath` are rejected. REST **GET/PUT** `/sites/{nameOrId}/virtual` round-trips `robots-txt` with a portable-safe local `rootPath` (leftover `virtual.remoteUrl`, credentials, and cloud URL `rootPath` are **400**; no live crawl). REST **Build** (`POST …/virtual/build`) writes HTML from that local `robots.txt` / `robots.file` fixture (`pagesWritten > 0`). REST **Preview** (`GET …/virtual/preview`) streams last-build local HTML (`available=true`; missing build is `available=false` HTTP 200). Developer Sites can save and GET-roundtrip `sourceKind=robots-txt`, then **Build Virtual Site** and **Preview assembled site** (Publish stays a later slice). **`sitemap-xml`** is a local `sitemap.xml` SPI (`sitemap.xml` or `_config.yaml` `sitemap.file`; no live crawl). SPI/CLI assemble is `PSVirtualSiteBuildMain … sitemap-xml`. REST **GET/PUT** `/sites/{nameOrId}/virtual` round-trips `sitemap-xml` with a portable-safe local `rootPath` (leftover `virtual.remoteUrl`, credentials, and cloud URL `rootPath` are **400**; no live crawl). REST **Build** (`POST …/virtual/build`) writes HTML from that local `sitemap.xml` / `sitemap.file` fixture (`pagesWritten > 0`; leftover `virtual.remoteUrl`, credentials, and cloud `rootPath` are **400**; no live crawl). REST **Preview** (`GET …/virtual/preview`) streams last-build local HTML (`available=true`; missing build is `available=false` HTTP 200; leftover `virtual.remoteUrl` and credentials are **400**; no live crawl). REST **Publish** (`POST …/virtual/publish`) copies assembled HTML to `IPSSite.root` after a local sitemap.xml Build (leftover `virtual.remoteUrl`, credentials, and cloud URL `rootPath` are **400**; no live crawl). Developer Sites can save and GET-roundtrip `sourceKind=sitemap-xml` (local `rootPath` only; leftover `virtual.remoteUrl` and crawl credentials are never sent), then **Build Virtual Site**, **Preview assembled site**, and **Publish Virtual Site** (last-build local HTML; missing build stays unavailable). **`icalendar`** is a local RFC 5545 `.ics` SPI (`calendar.ics` or `_config.yaml` `icalendar.file`; no CalDAV). SPI/CLI assemble is `PSVirtualSiteBuildMain … icalendar`. REST **GET/PUT** `/sites/{nameOrId}/virtual` round-trips `icalendar` with a portable-safe local `rootPath` (leftover `virtual.remoteUrl`, credentials, and cloud URL `rootPath` are **400**; no CalDAV). REST **Build** (`POST …/virtual/build`) runs **`icalendar`** against that local `calendar.ics` / `icalendar.file` fixture (`pagesWritten > 0`; leftover `virtual.remoteUrl`, credentials, and cloud `rootPath` are **400**). REST **Preview** streams last-build HTML after Build (`available=true`; missing build is `available=false` HTTP 200). REST **Publish** (`POST …/virtual/publish`) copies assembled HTML to `IPSSite.root`. Developer Sites can save and GET-roundtrip `sourceKind=icalendar` and then **Build Virtual Site**, **Preview assembled site**, and **Publish Virtual Site**. Blank or `repository` ⇒ traditional repository Site. Unknown values are rejected. CMS **Build** REST (`POST …/virtual/build`) runs git, CSV, SQL (H2), HTTP JSON (local JSON fixture or loopback catalog), **object-storage** (local object-key bucket; `virtual.remoteUrl` is **400**), **`rss-atom`** (local RSS 2.0 / Atom fixture or loopback `rss.url`; leftover `virtual.remoteUrl`, credentials, and cloud `rootPath` are **400**), and **`icalendar`** (local RFC 5545 `calendar.ics` / `icalendar.file`; leftover `virtual.remoteUrl`, credentials, and cloud `rootPath` are **400**; no CalDAV). Preview REST streams last-build HTML for git, CSV, SQL, HTTP JSON, `object-storage`, **`rss-atom`**, and **`icalendar`**. REST **Publish** (`POST …/virtual/publish`) copies assembled HTML to the Site filesystem root for git, CSV, SQL, HTTP JSON, `object-storage` (local object-key `rootPath`; leftover `virtual.remoteUrl` is **400**), **`rss-atom`** (local RSS/Atom fixture; leftover `virtual.remoteUrl` and credentials are **400**; no live feeds), and **`icalendar`** (local `.ics` fixture; leftover `virtual.remoteUrl` and credentials are **400**; no CalDAV). Developer Sites can save and build Git, CSV, SQL, HTTP JSON, and object-storage, then **Preview assembled site** and **Publish Virtual Site**. Developer Sites can also save and GET-roundtrip `object-storage`, then **Build Virtual Site**, **Preview assembled site**, and **Publish Virtual Site**. REST **GET/PUT** `/sites/{nameOrId}/virtual` also round-trips `http-json` (safe `rootPath` JSON fixture; `virtual.remoteUrl` is **400`), `object-storage` (portable-safe local `rootPath`; cloud URLs and credential properties are **400**; `virtual.remoteUrl` is **400**), and `rss-atom` (portable-safe local `rootPath`; leftover `virtual.remoteUrl`, credentials, and cloud URL `rootPath` are **400**; local/loopback only, no live feed credentials). Developer Sites can **Build Virtual Site** for `rss-atom` after save (local fixture only), **Preview assembled site**, and **Publish Virtual Site**. SPI/CLI assemble for `object-storage` is `PSVirtualSiteBuildMain … object-storage`. SPI/CLI assemble for `rss-atom` is `PSVirtualSiteBuildMain … rss-atom`. |
| `virtual.rootPath` | Yes when remote is blank | absolute path to `product-docs` (or install-relative) | Local filesystem root when `virtual.remoteUrl` is blank. When a remote is set, optional **relative** path inside the checkout (for example `product-docs`). |
| `virtual.remoteUrl` | No | `https://git.example.com/org/product-docs.git` | Optional Git remote. When set, **Build** clones or fetches into a contained work directory, then reuses git-filesystem discover. Blank keeps local-path mode. Allowed: `https://`, `ssh://`, `file://`, or `git@host:path`. `http` and other schemes are rejected. |
| `virtual.branch` | No | `main` | Branch to checkout when `remoteUrl` is set. Default `main`. Simple ref name only (no `..` or leading `-`). |
| `virtual.configFile` | No | `_config.yaml` | Config file name under the root; default `_config.yaml`. Must be a simple file name (no path separators or `..`). |
| `virtual.siteKey` | No | `product-docs` | Participant registry key; default = Site name, else `default`. |

Empty / missing `virtual.sourceKind` (or value `repository`) means a traditional repository Site.

### Validation rules

- **Source kind allow-list** — only registered adapter wire names are accepted for Virtual Sites
  (`git-filesystem`, `csv-filesystem`, `sql-database`, `http-json`, `object-storage`,
  `rss-atom`, `icalendar`, `sitemap-xml`). Unknown values are rejected. `csv-filesystem`, `sql-database`, `http-json`,
  `object-storage`, `rss-atom`, `icalendar`, and `sitemap-xml` do not accept `virtual.remoteUrl` (Git remotes apply to
  `git-filesystem` only). `sitemap-xml` is a local `sitemap.xml` fixture only (no live crawl, no
  non-loopback `http(s)` locs). `icalendar` is a local `.ics` fixture only (no CalDAV, no live remote
  `.ics` URLs). `sql-database` is in-memory H2 only (`jdbc:h2:mem:`); Oracle / MySQL /
  SQL Server URLs are rejected. `http-json` fetches open JSON only (no secrets); remote catalogs
  must be `http`/`https` without userinfo. Catalog URL/file live in `_config.yaml` (no secrets
  on the REST envelope). `object-storage` reads a local object-key directory only (no AWS SDK,
  access keys, or network). `rss-atom` persist is local/loopback only (no live feed credentials).
  REST persist for `object-storage`, `rss-atom`, `icalendar`, and `sitemap-xml` uses a local filesystem `rootPath` only (no
  remaining `..`); cloud URLs (`s3://`, `gs://`, `azure://`, `http(s)://`) and credential
  properties are **400**. `rss-atom` is a local RSS 2.0 / Atom fixture or loopback HTTP GET (no
  live cloud feeds, no credentials). REST **Build** (`POST …/virtual/build`) runs `rss-atom`
  against that local/loopback fixture. REST **Preview** streams last-build HTML after a
  successful assemble. REST **Publish** (`POST …/virtual/publish`) copies assembled HTML
  to `IPSSite.root` (`filesCopied > 0`; leftover `virtual.remoteUrl` and credentials are
  **400**).
- **Required root** — when `virtual.sourceKind` is virtual and `virtual.remoteUrl` is blank,
  `virtual.rootPath` must be non-blank.
- **Optional Git remote** — `virtual.remoteUrl` + `virtual.branch` fetch or clone before Build.
  URLs must not contain `..`, whitespace, or shell metacharacters, and must not start with `-`.
  The CMS never logs credentials (userinfo is redacted). `git` must be on the CMS host `PATH`.
- **Safe paths** — `virtual.rootPath` is normalized with `java.nio.file.Path`. After normalize, empty
  paths and any remaining `..` segments are rejected (path traversal). Prefer absolute paths on
  Windows (`C:\…`) and Unix (`/opt/…`) for **local** roots; relative paths under the install are
  allowed when they do not escape via `..`. When a remote is set, `virtual.rootPath` must be a
  relative path inside the checkout (not `C:\…` / `/opt/…`).
- **Config file name** — when set, `virtual.configFile` must not contain `/`, `\`, or `..`.

## Offline build

From the repository root (after the `system` module can compile):

```bat
scripts\build-cms-docs.bat
```

```bash
scripts/build-cms-docs.sh
```

Default output: `tmp/product-docs-site/`. The build fails non-zero when internal `id:` or relative
Markdown links cannot be resolved.

### Offline build from CSV (`csv-filesystem`)

The `csv-filesystem` adapter discovers pages from `*.csv` files under each version folder.
`_config.yaml` is **optional** (when omitted, each immediate child folder other than `_theme` /
`assets` is a version). `_theme` is optional (built-in layout). Do not require a Markdown docs
tree — CSV `body` is the page source. Git remotes are not used (`virtual.remoteUrl` is rejected
for this kind).

Required CSV columns (header row, case-insensitive):

| Column | Required | Meaning |
|--------|----------|---------|
| `id` | Yes | Stable page id within the version (same role as Markdown frontmatter `id`) |
| `title` | Yes | Page title |
| `body` | Yes | Markdown body (quote the field when it contains commas or newlines) |
| `path` | No | Site-relative page path (`getting-started/install` or `8.2/getting-started/install.md`). Omitted ⇒ `{version}/{id}.md`. Must be relative (no `..`, no `C:\…` / `/…`). |
| `order` | No | Integer nav order; default `0` |

Missing required columns, blank `id`/`title`, duplicate ids, or an unsafe `path` fail the build
(`VirtualSiteException`). Each discover/load re-reads the current CSV bytes (no process-lifetime
parse cache). After you edit a CSV file or `_config.yaml` on the CMS host, run **Build Virtual
Site** again (UI, `POST …/virtual/build`, or `PSVirtualSiteBuildMain … csv-filesystem`). The next
build always sees the current files — **no CMS process restart**. File watchers are not used.

CLI (optional `_config.yaml`):

```text
PSVirtualSiteBuildMain <siteRoot> <outputRoot> [siteKey] csv-filesystem
```

REST `PUT /sites/{nameOrId}/virtual` may store `sourceKind=csv-filesystem` (allow-listed).
Developer Sites can select **CSV filesystem** and save a root path. **Build Virtual Site**
(`POST …/virtual/build`) discovers CSV rows under `virtual.rootPath` and writes HTML
(`pagesWritten` in the JSON result). Developer **Sites** shows the same **Build Virtual Site**
control for **CSV filesystem** (and Git filesystem). **Publish Virtual Site**
(`POST …/virtual/publish`) then copies assembled HTML to the Site filesystem root.
Unknown kinds remain **400**.

### Offline build from SQL (`sql-database`)

The `sql-database` adapter discovers pages from a JDBC `SELECT` against **in-memory H2**
(`jdbc:h2:mem:name;DB_CLOSE_DELAY=-1`). `_config.yaml` is **required** and must include a
`sql:` mapping. Git remotes are not used (`virtual.remoteUrl` is rejected for this kind).
Do not point this adapter at live Oracle, MySQL, or SQL Server — those URLs fail closed.

Required query result columns (labels case-insensitive; optional `sql.columns` remaps them):

| Column | Required | Meaning |
|--------|----------|---------|
| `id` | Yes | Stable page id within the version (same role as Markdown frontmatter `id`) |
| `title` | Yes | Page title |
| `body` | Yes | Markdown body |
| `path` | No | Site-relative page path. Omitted ⇒ `{version}/{id}.md`. Must be relative (no `..`, no `C:\…` / `/…`). |
| `order` | No | Integer nav order; default `0` |
| `version` | No | Must match a `_config.yaml` version `id`. Omitted ⇒ the default version. |

Either `sql.query` (inline SELECT) or `sql.queryFile` (portable NIO path under the site
root) is required — not both. `queryFile` must be relative (no remaining `..`, no
Windows/Unix absolute roots). The query must be a single `SELECT` (no extra statements,
no `INIT`/`RUNSCRIPT` in the JDBC URL). Missing required columns, blank `id`/`title`,
duplicate ids, or an unsafe `path` fail closed (`VirtualSiteException`). Each
discover/load re-reads the current `_config.yaml` query (or `sql.queryFile` bytes) and
re-runs the SELECT (no process-lifetime row cache). After you edit `_config.yaml`,
`sql.queryFile`, or H2 rows on the CMS host, run **Build Virtual Site** again — **no JVM
restart**. File watchers are not used. Credentials are not written to logs; put the H2
user in `sql.user` (not in the JDBC URL).

Example `_config.yaml` fragment:

```yaml
sql:
  jdbcUrl: jdbc:h2:mem:virtual_pages;DB_CLOSE_DELAY=-1
  user: sa
  query: SELECT id, title, body, path, sort_order AS "order" FROM pages
```

CLI:

```text
PSVirtualSiteBuildMain <siteRoot> <outputRoot> [siteKey] sql-database
```

Site property validation allow-lists `sql-database` (same helper as Git/CSV/`http-json`). REST
`PUT` / `GET /sites/{nameOrId}/virtual` round-trips `sourceKind=sql-database` with a
portable-safe `rootPath`. JDBC URL, user, and query stay in `_config.yaml` (never on the
REST envelope; passwords are not logged). In-product `POST …/virtual/build` (and publish /
preview of last-build output) runs the H2 adapter. After a successful Build,
`GET …/virtual/preview` returns `available=true` plus `homePath` and
`GET …/virtual/preview/{relPath}` streams the assembled HTML. Missing build is
`available=false` (HTTP **200**), not 500.

**Publish** (`POST /sites/{nameOrId}/virtual/publish`) runs that same H2 Build (no
`outputRoot` body), then copies assembled HTML/assets to the Site filesystem publish
root (`IPSSite.root`) using portable NIO `Path` / `Files`. Staging `_meta` is not copied.
JDBC passwords from `_config.yaml` are not written into published HTML. Oracle / MySQL /
SQL Server JDBC URLs return **400**. Traditional repository Sites still cannot publish on
this path. Integrators get `publishPath`, `filesCopied`, and `pagesWritten` on HTTP **200**.

Developer Sites **Build Virtual Site** and
**Preview assembled site** are shown after you save **SQL database** (same chrome as
Git/CSV; repository stays hidden). Unknown kinds remain **400**.

### Offline build from HTTP JSON (`http-json`)

The `http-json` adapter discovers pages from a JSON catalog. `_config.yaml` is **required**
(versions / site title). Git remotes are not used (`virtual.remoteUrl` is rejected for this
kind). REST **GET/PUT** `/sites/{nameOrId}/virtual` round-trips `sourceKind=http-json`.
REST **Build** (`POST …/virtual/build`) runs the adapter against a local JSON fixture or
loopback `http.url` (`pagesWritten > 0`). REST **Preview** (`GET …/virtual/preview`)
streams last-build HTML after that Build (`available=true`; missing build is
`available=false` HTTP 200). Developer Sites can select **HTTP JSON**, save a
safe `rootPath`, GET-roundtrip the kind, and then **Build Virtual Site**. After a
successful Build, **Preview assembled site** opens last-build home HTML. REST
**Publish** (`POST …/virtual/publish`) copies assembled HTML to `IPSSite.root`.
Developer Sites **Preview assembled site** and **Publish Virtual Site** chrome
are shown after save.

Supply **one** of:

| `_config.yaml` key | Meaning |
|--------------------|---------|
| `http.url` | HTTP GET of the catalog (`http` or `https` only). |
| `http.file` | Portable NIO path under the site root (for example `catalog.json`). Must be relative (no remaining `..`, no Windows/Unix absolute roots). |
| *(omitted)* | Default local file `pages.json` under `virtual.rootPath`. |

Do not set both `http.url` and `http.file`. Catalogs larger than 2 MB fail closed. Each
discover/load re-reads the current HTTP body or file bytes (no process-lifetime cache).
After you edit the JSON catalog (`http.file` / default `pages.json` / loopback `http.url`
body) or `_config.yaml` on the CMS host, run **Build Virtual Site** again (UI,
`POST …/virtual/build`, or `PSVirtualSiteBuildMain … http-json`) — **no JVM restart**.
File watchers are not used; the next explicit build is the refresh.

JSON contract:

```json
{
  "pages": [
    { "id": "home", "path": "index.html", "title": "Home", "body": "<p>Hello</p>" }
  ]
}
```

| Field | Required | Meaning |
|-------|----------|---------|
| `id` | Yes | Stable page id within the version (same role as Markdown frontmatter `id`) |
| `title` | Yes | Page title |
| `body` | No | Markdown/HTML body assembled like CSV/SQL |
| `path` | No | Site-relative page path. Omitted ⇒ `{version}/{id}.html`. Must be relative (no `..`, no `C:\…` / `/…`). |
| `order` | No | Integer nav order; default `0` |
| `version` | No | Must match a `_config.yaml` version `id`. Omitted ⇒ the default version. |

Missing `pages`, blank `id`/`title`, duplicate ids, or an unsafe `path` fail closed
(`VirtualSiteException`). Remote catalogs are **SSRF fail-closed**: no `file`/`ftp`/other
schemes, no userinfo (secrets in the URL), cloud metadata hosts rejected, and redirects
that leave loopback refused. This slice does not send Authorization headers or API keys.

Example `_config.yaml` fragment (local fixture):

```yaml
http:
  file: pages.json
```

CLI:

```text
PSVirtualSiteBuildMain <siteRoot> <outputRoot> [siteKey] http-json
```

Site property validation allow-lists `http-json` (same helper as Git/CSV/SQL). Unknown
kinds remain rejected.

### Offline build from object storage (`object-storage`)

The `object-storage` adapter treats `virtual.rootPath` as a local object-key bucket. Object
keys are portable relative paths (logical `/`, NIO `Path` / `Files`; no remaining `..`, no
Windows/Unix absolute roots). There is **no** AWS/S3 SDK, access key, IAM, signed URL, or
network fetch — live cloud buckets are out of scope.

`_config.yaml` is **required** (versions / site title). Optional `objects.keys` lists keys
to load; when omitted, the adapter walks each version folder for `*.md`, `*.html` /
`*.htm`, and `*.json`. Files under `_theme`, `assets`, and names starting with `_` or `.`
are skipped. Git remotes are not used (`virtual.remoteUrl` is rejected). REST GET/PUT
persist of `sourceKind=object-storage` uses a portable-safe local `rootPath` (cloud URLs
and credential properties are **400**). REST **Build** (`POST …/virtual/build`) runs the
adapter against that local bucket (`pagesWritten > 0`). Developer Sites can save
**Object storage** and then **Build Virtual Site**, **Preview assembled site**, and
**Publish Virtual Site**.

Page identity:

| Source | `id` | `title` | `body` |
|--------|------|---------|--------|
| Markdown with YAML frontmatter | frontmatter `id` | frontmatter `title` | Markdown after fences |
| Markdown / HTML without frontmatter | filename stem | first heading, HTML `<title>`, or stem | file bytes |
| JSON catalog (`pages` array) | required `id` | required `title` | optional `body` (same as http-json / CSV) |
| JSON single object | required `id` | required `title` | optional `body` |

JSON catalog fields match HTTP JSON (`path` optional ⇒ `{version}/{id}.html`; `order`;
`version`). Missing `id`/`title`, duplicate ids, unsafe keys, or objects larger than 2 MB
fail closed (`VirtualSiteException`). Each discover/load re-reads current file bytes (no
process-lifetime cache). After you edit a Markdown, HTML, or JSON object key or
`_config.yaml` (`objects.keys` or site title) on the CMS host, run **Build Virtual Site**
again (UI, `POST …/virtual/build`, or `PSVirtualSiteBuildMain … object-storage`) — **no JVM
restart**. File watchers are not used; the next explicit build is the refresh.

Example `_config.yaml` fragment (optional key list):

```yaml
objects:
  keys:
    - 8.2/index.md
    - 8.2/getting-started/install.html
    - 8.2/pages.json
```

CLI:

```text
PSVirtualSiteBuildMain <siteRoot> <outputRoot> [siteKey] object-storage
```

Site property validation allow-lists `object-storage` (same helper as Git/CSV/SQL/HTTP
JSON). Unknown kinds remain rejected.

### Offline build from RSS / Atom (`rss-atom`)

The `rss-atom` adapter discovers pages from a local RSS 2.0 or Atom XML fixture. `_config.yaml`
is **required** (versions / site title). Git remotes are not used (`virtual.remoteUrl` is
rejected). This is a **local/loopback syndication adapter** — no live cloud feeds, no API
keys, no Basic/OAuth, no userinfo. REST **Build** (`POST …/virtual/build`) runs this adapter
against a portable-safe local `rootPath` (`pagesWritten > 0`). REST **Preview** (`GET
…/virtual/preview`) streams last-build HTML after a successful REST or CLI assemble at the
default output root. Developer Sites can **Build Virtual Site** after save (local
`feed.xml` / `atom.xml` / `_config.yaml` `rss.file` only), **Preview assembled site**, and **Publish Virtual Site**.
Operators can also assemble offline with the CLI.

Supply **one** of:

| `_config.yaml` key | Meaning |
|--------------------|---------|
| `rss.url` | Loopback HTTP GET of the feed (`http` or `https` to `127.0.0.1` / `localhost` / `::1` only). Cloud hosts are rejected. |
| `rss.file` | Portable NIO path under the site root (for example `feed.xml`). Must be relative (no remaining `..`, no Windows/Unix absolute roots). |
| *(omitted)* | Default local file `feed.xml`, then `atom.xml`, under `virtual.rootPath`. |

Do not set both `rss.url` and `rss.file`. Feeds larger than 2 MB fail closed. Each
discover/load re-reads the current file or HTTP body (no process-lifetime cache). After you
edit the RSS/Atom fixture (`feed.xml` / `atom.xml` / `rss.file`) or `_config.yaml` (`rss.file`
/ loopback `rss.url` or site title) on the CMS host, run **Build Virtual Site** again (REST
`POST …/virtual/build`, or `PSVirtualSiteBuildMain … rss-atom`) — **no JVM restart**. File
watchers are not used; the next explicit build is the refresh.

Item / entry mapping:

| Feed field | Assemble field |
|------------|----------------|
| RSS `guid` or `link`; Atom `id` or `link@href` | required `id` |
| `title` | required `title` |
| RSS `content:encoded` / `description`; Atom `content` / `summary` | `body` (Markdown/HTML) |

Omitted path defaults to `{version}/{slug(id)}.html`. Missing `id`/`title`, duplicate ids,
unsafe feed paths, non-loopback `rss.url`, userinfo, or a `<!DOCTYPE>` / XXE payload fail
closed (`VirtualSiteException`). XML parse is XXE fail-closed.

Example `_config.yaml` fragment (local fixture):

```yaml
rss:
  file: feed.xml
```

CLI:

```text
PSVirtualSiteBuildMain <siteRoot> <outputRoot> [siteKey] rss-atom
```

Site property validation allow-lists `rss-atom` (same helper as Git/CSV/SQL/HTTP JSON /
object-storage). Unknown kinds remain rejected.

### Offline build from iCalendar (`icalendar`)

The `icalendar` adapter discovers pages from a **local RFC 5545 `.ics` fixture**. `_config.yaml`
is **required** (versions / site title). Git remotes are not used (`virtual.remoteUrl` is
rejected). This is a **local calendar adapter** — no CalDAV, no live remote `.ics` URLs, no
API keys, no Basic/OAuth. REST `GET` / `PUT /sites/{nameOrId}/virtual` round-trips
`sourceKind=icalendar` with a portable-safe local `rootPath` (leftover `virtual.remoteUrl`,
credentials, and cloud URL `rootPath` are **400**). REST **Build** (`POST …/virtual/build`)
writes HTML from the local fixture (`pagesWritten > 0`). REST **Preview** reports last-build
status (`available=false` HTTP **200** when no assemble). REST **Publish** copies assembled
HTML to `IPSSite.root`. Developer **Sites** can save and GET-roundtrip
`sourceKind=icalendar`, then **Build Virtual Site**, **Preview assembled site**, and
**Publish Virtual Site**. Operators can also assemble offline with the CLI.

| `_config.yaml` key | Meaning |
|--------------------|---------|
| `icalendar.file` | Portable NIO path under the site root (for example `calendar.ics`). Must be relative (no remaining `..`, no Windows/Unix absolute roots). |
| *(omitted)* | Default local file `calendar.ics` under `virtual.rootPath`. |
| `icalendar.url` | **Rejected.** Live CalDAV / remote `.ics` URLs are out of scope. |

Calendars larger than 2 MB fail closed. Each discover/load re-reads the current file (no
process-lifetime cache). After you edit the fixture (`calendar.ics` / `icalendar.file`) or
`_config.yaml` on the CMS host, run `PSVirtualSiteBuildMain … icalendar` again — **no JVM
restart**. File watchers are not used; the next explicit build is the refresh.

`VEVENT` mapping:

| iCalendar field | Assemble field |
|-----------------|----------------|
| `UID` | required `id` |
| `SUMMARY` | required `title` |
| `DTSTART` | included in the Markdown body (`Starts: …`) |
| `DESCRIPTION` | `body` (Markdown/HTML; RFC 5545 unfolding and `\n` / `\,` / `\;` unescape) |

Omitted path defaults to `{version}/{slug(id)}.html`. Missing `UID`/`SUMMARY`, duplicate
uids, unsafe calendar paths, empty calendars (no `VEVENT`), or `icalendar.url` fail closed
(`VirtualSiteException`).

Example `_config.yaml` fragment (local fixture):

```yaml
icalendar:
  file: calendar.ics
```

CLI:

```text
PSVirtualSiteBuildMain <siteRoot> <outputRoot> [siteKey] icalendar
```

Site property validation allow-lists `icalendar` (same helper as Git/CSV/SQL/HTTP JSON /
object-storage / rss-atom). Unknown kinds remain rejected.

### Offline build from sitemap XML (`sitemap-xml`)

The `sitemap-xml` adapter discovers pages from a **local sitemap.xml fixture**. `_config.yaml`
is **required** (versions / site title). Git remotes are not used (`virtual.remoteUrl` is
rejected). This is a **local sitemap adapter** — no live crawl, no robots.txt fetch, no
authenticated remotes, no cloud sitemap URLs. REST **GET/PUT** `/sites/{nameOrId}/virtual`
round-trips `sourceKind=sitemap-xml` with a portable-safe local `rootPath` (leftover
`virtual.remoteUrl`, credentials, and cloud URL `rootPath` are **400**). REST **Build**
(`POST …/virtual/build`) writes HTML from that local fixture (`pagesWritten > 0`). REST
**Preview** (`GET …/virtual/preview`) streams last-build local HTML (`available=true`;
missing build is `available=false` HTTP **200**). REST **Publish** (`POST …/virtual/publish`) copies assembled HTML to `IPSSite.root` after a local sitemap.xml Build (leftover `virtual.remoteUrl`, credentials, and cloud URL `rootPath` are **400**; no live crawl).
Developer Sites can save and GET-roundtrip `sourceKind=sitemap-xml` (local `rootPath` only;
leftover `virtual.remoteUrl` and crawl credentials are never sent), then **Build Virtual Site**,
**Preview assembled site**, and **Publish Virtual Site** (last-build local HTML; missing build
stays unavailable). Operators can also assemble offline with the CLI.

| `_config.yaml` key | Meaning |
|--------------------|---------|
| `sitemap.file` | Portable NIO path under the site root (for example `sitemap.xml`). Must be relative (no remaining `..`, no Windows/Unix absolute roots). |
| *(omitted)* | Default local file `sitemap.xml` under `virtual.rootPath`. |
| `sitemap.url` | **Rejected.** Live remote sitemap crawls are out of scope. |

Sitemaps larger than 2 MB fail closed. Each discover/load re-reads the current file (no
process-lifetime cache of parsed loc/lastmod/path pages). After you edit `sitemap.xml`
(`<loc>`, `<lastmod>`, or path) or `_config.yaml` `sitemap.file` on the CMS host, run
REST **Build** (`POST …/virtual/build`), in-product **Build Virtual Site**, or
`PSVirtualSiteBuildMain … sitemap-xml` again — **no JVM restart**. File watchers are not
used; the next explicit build is the refresh.

`urlset` / `sitemapindex` mapping:

| Sitemap field | Assemble field |
|---------------|----------------|
| `<loc>` last path segment | required `id` / `title` (slug of the last segment) |
| `<lastmod>` | included in the Markdown body (`Last modified: …`) |
| referenced local file (or loopback `http(s)` loc body) | `body` |

`<loc>` entries must resolve to portable files under `virtual.rootPath`. Non-loopback
`http(s)` locs, `file:` URLs that escape the site root, leftover `virtual.remoteUrl`,
credential properties, empty sitemaps, duplicate ids, and `sitemap.url` fail closed
(`VirtualSiteException`).

Example `_config.yaml` fragment (local fixture):

```yaml
sitemap:
  file: sitemap.xml
```

CLI:

```text
PSVirtualSiteBuildMain <siteRoot> <outputRoot> [siteKey] sitemap-xml
```

Site property validation allow-lists `sitemap-xml` (same helper as Git/CSV/SQL/HTTP JSON /
object-storage / rss-atom / icalendar). Unknown kinds remain rejected.

### Offline build from robots.txt (`robots-txt`)

The `robots-txt` adapter discovers pages from a **local robots.txt fixture**. `_config.yaml`
is **required** (versions / site title). Git remotes are not used (`virtual.remoteUrl` is
rejected). This is a **local robots adapter** — no live crawl, no remote robots.txt fetch,
no authenticated remotes, no cloud robots URLs. REST **GET/PUT** `/sites/{nameOrId}/virtual`
round-trips `sourceKind=robots-txt` with a portable-safe local `rootPath` (leftover
`virtual.remoteUrl`, credentials, and cloud URL `rootPath` are **400**). REST **Build**
(`POST …/virtual/build`) writes HTML from that local fixture (`pagesWritten > 0`). REST
**Preview** (`GET …/virtual/preview`) streams last-build local HTML (`available=true`;
missing build is `available=false` HTTP **200**). Developer Sites can save and
GET-roundtrip `sourceKind=robots-txt` (local `rootPath` only; leftover
`virtual.remoteUrl` and crawl credentials are never sent), then **Build Virtual Site**
and **Preview assembled site**. Publish stays a later slice. Operators can also assemble
offline with the CLI.

| `_config.yaml` key | Meaning |
|--------------------|---------|
| `robots.file` | Portable NIO path under the site root (for example `robots.txt`). Must be relative (no remaining `..`, no Windows/Unix absolute roots). |
| *(omitted)* | Default local file `robots.txt` under `virtual.rootPath`. |
| `robots.url` | **Rejected.** Live remote robots crawls are out of scope. |

Robots files larger than 2 MB fail closed. Each discover/load re-reads the current file (no
process-lifetime cache of parsed User-agent groups). After you edit `robots.txt` or
`_config.yaml` `robots.file` on the CMS host, run `PSVirtualSiteBuildMain … robots-txt`
again — **no JVM restart**. File watchers are not used; the next explicit build is the
refresh.

`User-agent` mapping:

| robots.txt field | Assemble field |
|------------------|----------------|
| `User-agent` | required `id` / `title` (slug of the agent plus group order; `*` → `star`) |
| `Allow` / `Disallow` / other group rules | Markdown list in `body` |
| `Sitemap:` (local or site-relative) | noted in the body (not fetched) |

`Sitemap:` values with `http(s)://`, `s3://`, or other remote/cloud schemes, leftover
`virtual.remoteUrl`, credential properties, empty files, `robots.url`, and unsafe
`robots.file` paths fail closed (`VirtualSiteException`). A fixture with no `User-agent`
still emits one page from the file so CLI assemble writes HTML (`pagesWritten > 0`).

Example `_config.yaml` fragment (local fixture):

```yaml
robots:
  file: robots.txt
```

CLI:

```text
PSVirtualSiteBuildMain <siteRoot> <outputRoot> [siteKey] robots-txt
```

Site property validation allow-lists `robots-txt` (same helper as Git/CSV/SQL/HTTP JSON /
object-storage / rss-atom / icalendar / sitemap-xml). Unknown kinds remain rejected.

### REST persist for robots.txt (`robots-txt`)

REST `PUT` / `GET /sites/{nameOrId}/virtual` round-trips `sourceKind=robots-txt` with a
portable-safe local `rootPath`. Leftover `virtual.remoteUrl`, credential properties, and
cloud URL `rootPath` are **400**. This is a **local robots.txt fixture** only — no live
crawl. Developer Sites can save and GET-roundtrip `sourceKind=robots-txt` (local fixture
`rootPath` only); leftover crawl URLs and credentials are never sent on the REST envelope.

### REST Build for robots.txt (`robots-txt`)

`POST /sites/{nameOrId}/virtual/build` runs `sourceKind=robots-txt` against a **local
robots.txt fixture** under `virtual.rootPath` (`robots.txt` or `_config.yaml`
`robots.file`). A successful assemble returns HTTP **200** with `pagesWritten > 0`.
Missing fixture, unsafe `rootPath` (`..` after NIO normalize), leftover
`virtual.remoteUrl`, credential properties, and cloud `rootPath` URLs are **400**. No live
crawl and no secrets on the REST envelope. Each Build re-reads the current `robots.txt`
and `_config.yaml` `robots.file` (no JVM restart; no Jetty restart; no file watchers).
Developer Sites **Build Virtual Site** is shown so operators can produce last-build HTML
for **Preview assembled site**. Publish stays a later slice.

### REST Preview for robots.txt (`robots-txt`)

After a successful REST or CLI assemble at the default output root, `GET
/sites/{nameOrId}/virtual/preview` reports `available=true` plus `homePath` (typically
`8.2/star-1.html` for a single `User-agent: *` group — Preview uses the sole assembled
HTML page when `index.html` is absent). `GET …/virtual/preview/{relPath}` streams the
assembled HTML. Missing build is `available=false` with HTTP **200** (not 500). Unknown
`sourceKind` remains **400**. Paths use portable NIO `Path` under the last output root
(no remaining `..`). `../` traversal is **400**. Preview is **last-build local HTML
only** (no live crawl). Leftover `virtual.remoteUrl` and credential properties are
**400**. Developer Sites **Preview assembled site** uses that last-build Preview after
**Build Virtual Site**. Missing build stays unavailable (`available=false` HTTP **200**;
no fake preview).

### Offline build from llms.txt (`llms-txt`)

The `llms-txt` adapter discovers pages from a **local llms.txt fixture**. `_config.yaml`
is **required** (versions / site title). Git remotes are not used (`virtual.remoteUrl` is
rejected). This is a **local llms.txt adapter** — no live HTTP fetch, no remote llms.txt
fetch, no authenticated remotes, no cloud llms URLs. REST persist, Build, Preview,
Publish, and Developer Sites chrome stay later slices. Operators assemble offline with
the CLI.

| `_config.yaml` key | Meaning |
|--------------------|---------|
| `llms.file` | Portable NIO path under the site root (for example `llms.txt`). Must be relative (no remaining `..`, no Windows/Unix absolute roots). |
| *(omitted)* | Default local file `llms.txt` under `virtual.rootPath`. |
| `llms.url` | **Rejected.** Live remote llms.txt fetches are out of scope. |

llms.txt files larger than 2 MB fail closed. Each discover/load re-reads the current file (no
process-lifetime cache of parsed links). After you edit `llms.txt` or `_config.yaml`
`llms.file` on the CMS host, run `PSVirtualSiteBuildMain … llms-txt` again — **no JVM
restart**. File watchers are not used; the next explicit build is the refresh.

Link mapping:

| llms.txt field | Assemble field |
|----------------|----------------|
| Markdown list link title | required `id` / `title` (slug of the title plus link order) |
| Local or site-relative `href` | noted in the body as `Link:` (not fetched) |
| Optional `: notes` after the link | Markdown notes in `body` |

Link hrefs with `http(s)://`, `s3://`, or other remote/cloud schemes, leftover
`virtual.remoteUrl`, credential properties, empty files, `llms.url`, and unsafe
`llms.file` paths fail closed (`VirtualSiteException`). A fixture with no markdown
list links still emits one page from the file so CLI assemble writes HTML
(`pagesWritten > 0`).

Example `_config.yaml` fragment (local fixture):

```yaml
llms:
  file: llms.txt
```

CLI:

```text
PSVirtualSiteBuildMain <siteRoot> <outputRoot> [siteKey] llms-txt
```

Site property validation allow-lists `llms-txt` (same helper as Git/CSV/SQL/HTTP JSON /
object-storage / rss-atom / icalendar / sitemap-xml / robots-txt). Unknown kinds remain
rejected.

### Offline build from OpenAPI 3 YAML (`openapi-yaml`)

The `openapi-yaml` adapter discovers pages from a **local OpenAPI 3 YAML fixture**.
`_config.yaml` is **required** (versions / site title). Git remotes are not used
(`virtual.remoteUrl` is rejected). This is a **local OpenAPI adapter** — no live HTTP
spec fetch, no remote OpenAPI URLs, no authenticated remotes, no cloud spec URLs. REST
persist, Build, Preview, Publish, and Developer Sites chrome stay later slices.
Operators assemble offline with the CLI.

| `_config.yaml` key | Meaning |
|--------------------|---------|
| `openapi.file` | Portable NIO path under the site root (for example `openapi.yaml`). Must be relative (no remaining `..`, no Windows/Unix absolute roots). |
| *(omitted)* | Default local file `openapi.yaml` under `virtual.rootPath`. |
| `openapi.url` | **Rejected.** Live remote OpenAPI fetches are out of scope. |

OpenAPI YAML files larger than 2 MB fail closed. Each discover/load re-reads the current
file (no process-lifetime cache of parsed operations). After you edit `openapi.yaml` or
`_config.yaml` `openapi.file` on the CMS host, run `PSVirtualSiteBuildMain … openapi-yaml`
again — **no JVM restart**. File watchers are not used; the next explicit build is the
refresh.

Path/operation mapping:

| OpenAPI field | Assemble field |
|---------------|----------------|
| `operationId` (or `METHOD` + path) | required `id` (slug plus operation order) |
| `summary` (else `operationId`, else `METHOD path`) | `title` |
| method, path, description | Markdown in `body` |

Remote `$ref` values (`http(s)://`, `s3://`, or other remote/cloud schemes), leftover
`virtual.remoteUrl`, credential properties, empty files, `openapi.url`, non-OpenAPI-3
documents, and unsafe `openapi.file` paths fail closed (`VirtualSiteException`). A
fixture with no path operations still emits one page from `info` so CLI assemble writes
HTML (`pagesWritten > 0`).

Example `_config.yaml` fragment (local fixture):

```yaml
openapi:
  file: openapi.yaml
```

CLI:

```text
PSVirtualSiteBuildMain <siteRoot> <outputRoot> [siteKey] openapi-yaml
```

Site property validation allow-lists `openapi-yaml` (same helper as Git/CSV/SQL/HTTP JSON /
object-storage / rss-atom / icalendar / sitemap-xml / robots-txt / llms-txt). Unknown kinds
remain rejected.

### REST Build for iCalendar (`icalendar`)

`POST /sites/{nameOrId}/virtual/build` runs `sourceKind=icalendar` against a **local RFC 5545
`.ics` fixture** under `virtual.rootPath` (`calendar.ics` or `_config.yaml` `icalendar.file`).
A successful assemble returns HTTP **200** with `pagesWritten > 0`. Missing fixture, unsafe
`rootPath` (`..` after NIO normalize), leftover `virtual.remoteUrl`, credential properties,
and cloud `rootPath` URLs are **400**. No CalDAV, no live remotes, and no secrets on the REST
envelope. Each Build re-reads the current fixture and `_config.yaml` (no parsed-page cache;
no JVM restart; no file watchers). Git, CSV, SQL, HTTP JSON, object-storage, and rss-atom
Build paths are unchanged. REST persist of `icalendar` is covered on GET/PUT
`/sites/{nameOrId}/virtual`. Developer Sites **Build Virtual Site** is shown after save.

### REST Preview for iCalendar (`icalendar`)

After a successful REST or CLI assemble at the default output root, `GET
/sites/{nameOrId}/virtual/preview` reports `available=true` plus `homePath` (typically
`8.2/index.html`). `GET …/virtual/preview/{relPath}` streams the assembled HTML. Missing
build is `available=false` with HTTP **200** (not 500). Unknown `sourceKind` remains
**400**. Paths use portable NIO `Path` under the last output root (no remaining `..`).
`../` traversal is **400**. This is last-output based — the same Preview contract as git,
CSV, SQL, HTTP JSON, object-storage, and rss-atom. No CalDAV.

Preview home is assembled `{version}/index.html`. Give a `VEVENT` a `UID` that slugs to
`index` so last-build Preview can report `available=true`. Other UIDs assemble to
`{version}/{slug}.html` and remain streamable at `GET …/virtual/preview/{relPath}`.

Developer Sites **Preview assembled site** uses that last-build Preview after **Build Virtual Site**.

### REST Publish for iCalendar (`icalendar`)

`POST /sites/{nameOrId}/virtual/publish` builds the local RFC 5545 fixture then NIO-copies
assembled HTML to `IPSSite.root`. HTTP **200** returns `filesCopied > 0` and an assembled
`index.html` under the Site filesystem root when the calendar UID slugs to `index`. Staging
`_meta` is not copied. Leftover `virtual.remoteUrl`, credential properties, and an unsafe
Site root (`..` after NIO normalize) are **400**. No CalDAV. Git, CSV, SQL, HTTP JSON,
object-storage, rss-atom, and icalendar Publish paths copy assembled HTML to `IPSSite.root`.
Developer Sites **Publish Virtual Site** is shown after save.

### REST Build for RSS / Atom (`rss-atom`)

`POST /sites/{nameOrId}/virtual/build` runs `sourceKind=rss-atom` against a **local RSS 2.0 or
Atom XML fixture** under `virtual.rootPath` (`feed.xml` / `atom.xml` or `_config.yaml`
`rss.file`; `rss.url` loopback only). A successful assemble returns HTTP **200** with
`pagesWritten > 0`. Missing fixture, unsafe `rootPath` (`..` after NIO normalize), leftover
`virtual.remoteUrl`, credential properties, and cloud `rootPath` URLs are **400**. No live
internet feeds and no secrets on the REST envelope. Each Build re-reads the current
fixture and `_config.yaml` (no parsed-page cache; no JVM restart; no file watchers). Git,
CSV, SQL, HTTP JSON, and object-storage Build paths are unchanged. REST persist of
`rss-atom` is covered on GET/PUT `/sites/{nameOrId}/virtual`. Developer Sites **Build
Virtual Site**, **Preview assembled site**, and **Publish Virtual Site** are shown after save.

### REST Preview for RSS / Atom (`rss-atom`)

After a successful REST or CLI assemble at the default output root, `GET
/sites/{nameOrId}/virtual/preview` reports `available=true` plus `homePath` (typically
`8.2/index.html`). `GET …/virtual/preview/{relPath}` streams the assembled HTML. Missing
build is `available=false` with HTTP **200** (not 500). Unknown `sourceKind` remains
**400**. Paths use portable NIO `Path` under the last output root (no remaining `..`).
`../` traversal is **400**. This is last-output based — the same Preview contract as git,
CSV, SQL, HTTP JSON, and object-storage. No live remote feeds.

Preview home is assembled `{version}/index.html`. Give a feed item an id that slugs to
`index` (for example RSS `<guid>index</guid>` or Atom `<id>index</id>`) so last-build
Preview can report `available=true`. Other item ids assemble to `{version}/{slug}.html`
and remain streamable at `GET …/virtual/preview/{relPath}`.

Developer Sites **Preview assembled site** uses that last-build Preview after **Build
Virtual Site**.

### REST Publish for RSS / Atom (`rss-atom`)

`POST /sites/{nameOrId}/virtual/publish` builds the local RSS 2.0 / Atom fixture (or
loopback `rss.url`) then NIO-copies assembled HTML to `IPSSite.root`. HTTP **200** returns
`filesCopied > 0` and an assembled `index.html` under the Site filesystem root. Staging
`_meta` is not copied. Leftover `virtual.remoteUrl`, credential properties, and an unsafe
Site root (`..` after NIO normalize) are **400**. No live internet feeds. Git, CSV, SQL,
HTTP JSON, object-storage, and rss-atom Publish paths copy assembled HTML to `IPSSite.root`. Developer Sites **Publish
Virtual Site** chrome runs the same action after Build.

### REST persist for HTTP JSON (`http-json`)

REST `PUT` / `GET /sites/{nameOrId}/virtual` round-trips `sourceKind=http-json` with a
portable-safe `rootPath` (JSON fixture directory; no remaining `..` after NIO
`Path.normalize()`). `virtual.remoteUrl` is **400** — Git remotes apply to
`git-filesystem` only; catalog HTTP URL or file path stay in `_config.yaml`
(`http.url` / `http.file` or default `pages.json`). Never send secrets, userinfo, or
Authorization on this envelope. Unknown kinds remain **400**. Developer Sites can
save and GET-roundtrip `http-json`, then **Build Virtual Site**. REST **Build**
(`POST …/virtual/build`), REST **Preview** (`GET …/virtual/preview`), and REST
**Publish** (`POST …/virtual/publish`) are available for `http-json`. Developer Sites
**Preview assembled site** uses that last-build Preview. **Publish Virtual Site**
chrome is shown after save.

### REST persist for object storage (`object-storage`)

REST `PUT` / `GET /sites/{nameOrId}/virtual` round-trips `sourceKind=object-storage` with a
portable-safe `rootPath` (local object-key directory; no remaining `..` after NIO
`Path.normalize()`). `virtual.remoteUrl` is **400** — Git remotes apply to
`git-filesystem` only. Cloud URLs (`s3://`, `gs://`, `azure://`, `http(s)://`) and
credential properties (access keys, secrets, connection strings) are **400**. Never send
AWS/IAM/secrets on this envelope. Unknown kinds remain **400**. Git, CSV, SQL, and
`http-json` kinds are unchanged. Developer Sites can save and GET-roundtrip
`sourceKind=object-storage` and then **Build Virtual Site**, **Preview assembled
site**, and **Publish Virtual Site**. REST **Build** (`POST …/virtual/build`) is
available for `object-storage`. REST **Preview** (`GET …/virtual/preview`) streams
last-build HTML after a successful assemble (`available=true` + home HTML; missing
build is `available=false` HTTP 200). REST **Publish** (`POST …/virtual/publish`) copies
assembled HTML to `IPSSite.root` for a local object-key fixture (leftover
`virtual.remoteUrl` is **400**; no cloud URLs, IAM, or access keys). Developer Sites
**Publish Virtual Site** chrome runs the same action after Build.

### REST persist for RSS / Atom (`rss-atom`)

REST `PUT` / `GET /sites/{nameOrId}/virtual` round-trips `sourceKind=rss-atom` with a
portable-safe local `rootPath` (NIO `Path.normalize()`; no remaining `..`). This is
**local/loopback only** — leftover `virtual.remoteUrl`, credential properties, and cloud
URL `rootPath` values (`s3://`, `gs://`, `azure://`, `http(s)://`) are **400**. Never send
live feed credentials, Authorization, or API keys on this envelope. Unknown kinds remain
**400**. Git, CSV, SQL, `http-json`, and `object-storage` persist are unchanged. REST
**Build**, **Preview**, and **Publish** (`POST …/virtual/publish`) are available for
`rss-atom` (local/loopback fixture; leftover `virtual.remoteUrl` and credentials are
**400**). Developer Sites can **Build Virtual Site**, **Preview assembled site**, and
**Publish Virtual Site**.

### REST persist for iCalendar (`icalendar`)

REST `PUT` / `GET /sites/{nameOrId}/virtual` round-trips `sourceKind=icalendar` with a
portable-safe local `rootPath` (NIO `Path.normalize()`; no remaining `..`). This is a
**local RFC 5545 `.ics` fixture only** — leftover `virtual.remoteUrl`, credential
properties, and cloud URL `rootPath` values (`s3://`, `gs://`, `azure://`, `http(s)://`,
`caldav://`) are **400**. Never send CalDAV credentials, Authorization, or API keys on this
envelope. Unknown kinds remain **400**. Git, CSV, SQL, `http-json`, `object-storage`, and
`rss-atom` persist are unchanged. REST **Build**, **Preview**, and **Publish**
(`POST …/virtual/publish`) are available for `icalendar` (local RFC 5545 fixture; leftover
`virtual.remoteUrl` and credentials are **400**; no CalDAV). Developer **Sites** can save
and GET-roundtrip `sourceKind=icalendar` (local fixture `rootPath` only) and then
**Build Virtual Site**, **Preview assembled site**, and **Publish Virtual Site**.

### REST persist for sitemap XML (`sitemap-xml`)

REST `PUT` / `GET /sites/{nameOrId}/virtual` round-trips `sourceKind=sitemap-xml` with a
portable-safe local `rootPath` (NIO `Path.normalize()`; no remaining `..`). This is a
**local `sitemap.xml` fixture only** — leftover `virtual.remoteUrl`, credential
properties, and cloud URL `rootPath` values (`s3://`, `gs://`, `azure://`, `http(s)://`)
are **400**. Never send live crawl credentials, Authorization, or API keys on this
envelope. Unknown kinds remain **400**. Git, CSV, SQL, `http-json`, `object-storage`,
`rss-atom`, and `icalendar` persist are unchanged. REST **Build** (`POST …/virtual/build`)
is available for `sitemap-xml` (local sitemap fixture; leftover `virtual.remoteUrl`,
credentials, and cloud `rootPath` are **400**; no live crawl). REST **Preview**
(`GET …/virtual/preview`) streams last-build local HTML after assemble (`available=true`;
missing build is `available=false` HTTP 200; leftover `virtual.remoteUrl` and credentials
are **400**; no live crawl). REST **Publish** (`POST …/virtual/publish`) copies assembled HTML to `IPSSite.root` after a local sitemap.xml Build (leftover `virtual.remoteUrl`, credentials, and cloud URL `rootPath` are **400**; no live crawl). Developer **Sites** can
save and GET-roundtrip `sourceKind=sitemap-xml` (local fixture `rootPath` only),
then **Build Virtual Site**, **Preview assembled site**, and **Publish Virtual Site** (last-build local HTML;
missing build stays unavailable).

### REST Build for sitemap XML (`sitemap-xml`)

`POST /sites/{nameOrId}/virtual/build` runs `sourceKind=sitemap-xml` against a **local
sitemap.xml fixture** under `virtual.rootPath` (`sitemap.xml` or `_config.yaml`
`sitemap.file`; urlset of portable files). A successful assemble returns HTTP **200** with
`pagesWritten > 0`. Missing fixture, unsafe `rootPath` (`..` after NIO normalize), leftover
`virtual.remoteUrl`, credential properties, and cloud `rootPath` URLs are **400**. No live
crawl, no robots.txt fetch, and no secrets on the REST envelope. Each Build re-reads the
current `sitemap.xml` loc/lastmod/path and `_config.yaml` `sitemap.file` (no parsed-page
cache; no JVM restart; no Jetty restart; no file watchers). A second Admin `POST /sites/{nameOrId}/virtual/build`
after a Path/Files edit of `sitemap.xml` loc/lastmod, `sitemap.file`, or referenced page
Markdown returns `pagesWritten > 0` HTML that matches the **current** files — no Jetty
restart. Git, CSV, SQL, HTTP JSON, object-storage, rss-atom, and icalendar Build paths
are unchanged. REST persist of `sitemap-xml` is covered on GET/PUT
`/sites/{nameOrId}/virtual`. Developer Sites **Build Virtual Site** is shown so operators
can produce last-build HTML for **Preview assembled site** and **Publish Virtual Site**.

### REST Publish for sitemap XML (`sitemap-xml`)

`POST /sites/{nameOrId}/virtual/publish` builds the local `sitemap.xml` fixture then NIO-copies
assembled HTML to `IPSSite.root`. HTTP **200** returns `filesCopied > 0` and assembled HTML
under the Site filesystem root (for example `8.2/index.html` when a `<loc>` slugs to `index`).
Staging `_meta` is not copied. Leftover `virtual.remoteUrl`, credential properties, cloud URL
`rootPath`, and an unsafe Site root (`..` after NIO normalize) are **400**. No live crawl.
Git, CSV, SQL, HTTP JSON, object-storage, rss-atom, and icalendar Publish paths are unchanged.
Developer Sites **Publish Virtual Site** copies last-build local HTML to `IPSSite.root`
after a local sitemap.xml Build (leftover `virtual.remoteUrl` and credentials fail closed).

### REST Preview for sitemap XML (`sitemap-xml`)

After a successful REST or CLI assemble at the default output root, `GET
/sites/{nameOrId}/virtual/preview` reports `available=true` plus `homePath` (typically
`8.2/index.html`). `GET …/virtual/preview/{relPath}` streams the assembled HTML. Missing
build is `available=false` with HTTP **200** (not 500). Unknown `sourceKind` remains
**400**. Paths use portable NIO `Path` under the last output root (no remaining `..`).
`../` traversal is **400**. This is last-output based — the same Preview contract as git,
CSV, SQL, HTTP JSON, object-storage, rss-atom, and icalendar. Preview is **last-build
local HTML only** (no live crawl). Leftover `virtual.remoteUrl` and credential properties
are **400**.

Preview home is assembled `{version}/index.html`. Give a sitemap `<loc>` a last path
segment that slugs to `index` (for example `pages/index.md`) so last-build Preview can
report `available=true`. Other locs assemble to `{version}/{slug}.html` and remain
streamable at `GET …/virtual/preview/{relPath}`.

Developer Sites **Preview assembled site** uses that last-build Preview after **Build Virtual Site**. Missing build stays unavailable (`available=false` HTTP **200**; no fake preview). **Publish Virtual Site** copies last-build HTML to `IPSSite.root`.

### REST Preview for object storage (`object-storage`)

After a successful REST or CLI assemble at the default output root, `GET
/sites/{nameOrId}/virtual/preview` reports `available=true` plus `homePath` (typically
`8.2/index.html`). `GET …/virtual/preview/{relPath}` streams the assembled HTML. Missing
build is `available=false` with HTTP **200** (not 500). Unknown `sourceKind` remains
**400**. Paths use portable NIO `Path` under the last output root (no remaining `..`).
Developer Sites **Preview assembled site** uses that last-build Preview after **Build Virtual Site**.

## CMS-integrated build (REST and WebUI)

When a CMS Site has Virtual properties configured (`git-filesystem`, `csv-filesystem`,
`sql-database`, `http-json`, `object-storage`, `rss-atom`, `icalendar`, `sitemap-xml`, or `robots-txt`), an **Admin** can trigger the
matching build path from the running server. `sql-database` discovers rows from the in-memory
H2 `SELECT` in `_config.yaml` (required `sql:` mapping; `pagesWritten > 0` when the query
returns rows) and writes HTML under the output root. `http-json` discovers pages from a
local JSON fixture or loopback catalog (`http.url` / `http.file`; `pagesWritten > 0`).
`object-storage` discovers Markdown / HTML / JSON object keys under a portable-safe local
`rootPath` (`pagesWritten > 0`; leftover `virtual.remoteUrl` is **400**). `rss-atom`
discovers pages from a local RSS 2.0 / Atom fixture (`feed.xml` / `atom.xml` or `rss.file`;
loopback `rss.url` only; `pagesWritten > 0`; leftover `virtual.remoteUrl`, credentials, and
cloud `rootPath` are **400**). `icalendar` discovers pages from a local RFC 5545 fixture
(`calendar.ics` or `icalendar.file`; `pagesWritten > 0`; leftover `virtual.remoteUrl`,
credentials, and cloud `rootPath` are **400**; no CalDAV). `sitemap-xml` discovers pages from
a local sitemap.xml fixture (`sitemap.xml` or `sitemap.file`; urlset of portable files;
`pagesWritten > 0`; leftover `virtual.remoteUrl`, credentials, and cloud `rootPath` are
**400**; no live crawl). `robots-txt` discovers pages from a local robots.txt fixture (`robots.txt` or `robots.file`; `pagesWritten > 0`; leftover `virtual.remoteUrl`, credentials, and cloud `rootPath` are **400**; no live crawl). Unknown `sourceKind` values stay
**400**. Git and CSV builds are unchanged:

```http
POST /sites/{nameOrId}/virtual/build
Content-Type: application/json

{
  "outputRoot": "C:/tmp/product-docs-site"
}
```

The body is optional. Without `outputRoot`, the server writes under
`{install}/tmp/virtual-sites/{siteKey}` (portable NIO paths). The response summarizes
`pagesWritten`, link problems, and the absolute `outputPath`. Traditional repository Sites and
invalid/missing virtual configuration return **400**.

A successful assemble with unresolved internal links still returns **HTTP 200** with
`hasLinkProblems=true` and a `linkProblems` array (the same lines as `link-report.txt` under
the output root). The Sites UI lists those lines in an expandable **Link problems** block so
operators do not have to open the report file on the server. Do not treat that 200 as a
server error.

Operators can run the same operation from **Developer → Sites → Site detail → Build Virtual Site**
(visible only when source kind is Virtual). Save Virtual Site source before building so the server
uses the latest properties.

`PUT /sites/{nameOrId}/virtual` requires the wire root name **`VirtualSiteProperties`**:

```json
{
  "VirtualSiteProperties": {
    "sourceKind": "git-filesystem",
    "rootPath": "C:/workspaces/product-docs",
    "configFile": "_config.yaml",
    "siteKey": "product-docs"
  }
}
```

A bare `{ "sourceKind": "git-filesystem", … }` body is rejected (**400**, JAXB unexpected
element `sourceKind`). GET returns the same envelope (or a plain object the SPA unwraps).
After save, the Developer Sites panel GET-roundtrips so `virtual=true` and Build chrome appear
without reloading the page.

CSV trees use the same envelope with `"sourceKind": "csv-filesystem"` and a portable-safe
`rootPath` (no remaining `..` after NIO normalize). `GET` after `PUT` returns `csv-filesystem`.
`virtual.remoteUrl` is not valid for CSV (HTTP 400). SQL trees use `"sourceKind": "sql-database"`
the same way; JDBC settings stay in `_config.yaml` (H2 mem only). HTTP JSON trees use
`"sourceKind": "http-json"` with a portable-safe `rootPath` JSON fixture; catalog URL/file
stay in `_config.yaml` (`http.url` / `http.file`). `virtual.remoteUrl` is **400** for
`http-json` (no secrets on this envelope). Object-storage trees use `"sourceKind":
"object-storage"` with a portable-safe local `rootPath`; cloud URLs and credential
properties are **400**. In-product
`POST …/virtual/build` runs for `git-filesystem`, `csv-filesystem`, `sql-database`,
`http-json`, `object-storage`, `rss-atom`, `icalendar`, and `sitemap-xml`. `POST …/virtual/publish` runs for
`git-filesystem`, `csv-filesystem`, `sql-database`, `http-json`, `object-storage`,
`rss-atom`, `icalendar`, and `sitemap-xml` (copies assembled HTML to `IPSSite.root`; leftover `virtual.remoteUrl` on
`http-json`, `object-storage`, `rss-atom`, `icalendar`, and `sitemap-xml` is **400**; leftover credentials on
`rss-atom`, `icalendar`, and `sitemap-xml` are **400**; leftover cloud URL `rootPath` on `sitemap-xml` is **400**; no live crawl).

### Git remote fetch before Build

Operators can point a Virtual Site at a **remote Git URL + branch** instead of (or in addition to)
a pre-checked-out local path. Configure via `PUT /sites/{nameOrId}/virtual`:

```json
{
  "VirtualSiteProperties": {
    "sourceKind": "git-filesystem",
    "remoteUrl": "https://git.example.com/org/product-docs.git",
    "branch": "main",
    "rootPath": "product-docs"
  }
}
```

On **Build Virtual Site** (`POST …/virtual/build`) the server:

1. Validates the URL and branch (fail-closed; never logs credentials).
2. Clones or fetches into a contained work directory (`{install}/tmp/virtual-site-checkouts/{siteKey}`,
   or `{java.io.tmpdir}/percussion-virtual-site-checkouts/{siteKey}`).
3. Uses optional `virtual.rootPath` as a **relative** sub-folder inside that checkout.
4. Reuses the existing git-filesystem discover / assemble path.

Leave `remoteUrl` blank (or omit it) to keep **local-path** `git-filesystem` behavior — Build
reads `virtual.rootPath` on the CMS host as before. Send `"remoteUrl": ""` to clear a stored remote.

The Developer → Sites source panel does not yet expose remote URL / branch fields. Configure
those properties over REST (or a later UI slice). If a remote is stored, do not save an absolute
local root from the panel until the remote is cleared.

The CMS host must have `git` on `PATH`. Checkouts are server-managed; do not point `remoteUrl` at
untrusted remotes.

### Rebuild after git pull, a CSV/SQL/JSON edit, or a local edit (no CMS restart)

The Git/filesystem, CSV/filesystem, SQL/database, and HTTP JSON adapters always see the
**current** source on the CMS host:

1. Update Markdown or frontmatter, a CSV file, `_config.yaml`, a SQL `queryFile` / inline
   `sql.query`, in-memory H2 rows, or an HTTP JSON catalog (`http.file` / `pages.json`) under
   `virtual.rootPath` (`git pull`, copy, or an editor), **or** change the remote branch and
   Build again so the server fetches (Git only).
2. Run **Build Virtual Site** again (UI, `POST …/virtual/build`, `scripts/build-cms-docs.*`,
   `PSVirtualSiteBuildMain … csv-filesystem`, `PSVirtualSiteBuildMain … sql-database`,
   `PSVirtualSiteBuildMain … http-json`, `PSVirtualSiteBuildMain … object-storage`,
   `PSVirtualSiteBuildMain … rss-atom`, `PSVirtualSiteBuildMain … icalendar`,
   `PSVirtualSiteBuildMain … sitemap-xml`, `PSVirtualSiteBuildMain … robots-txt`,
   `PSVirtualSiteBuildMain … llms-txt`, or `PSVirtualSiteBuildMain … openapi-yaml`).
3. Preview or publish the new output.

You do **not** restart the CMS JVM for those file or H2 row changes to appear. A restart is
only needed when you change server code, Site properties that were never saved, or the
process itself.

After a successful build, **Preview assembled site** opens the last output home in a new tab
(`GET /sites/{nameOrId}/virtual/preview` for status; `GET …/virtual/preview/{relPath}` for the
assembled file stream). Preview is last-output based and works for **`git-filesystem`**,
**`csv-filesystem`**, **`sql-database`**, **`http-json`**, **`object-storage`**,
**`rss-atom`**, **`icalendar`**, **`sitemap-xml`**, and **`robots-txt`** (not
git-only). Missing output returns status `available=false` (HTTP 200)
or file HTTP 404 — not 500. Path traversal (`../`) is **400**. Repository and unknown
`sourceKind` values are **400**. See [Sites & content structure](id:admin-sites) and
[Site configuration](id:reference-site-config).

## CMS-integrated publish (Site filesystem target)

Build writes a **staging** tree. To publish that tree to the Site's configured filesystem
location (`IPSSite.root` / Site publishing root), an **Admin** uses
**Developer → Sites → Site detail → Publish Virtual Site** (visible only when source kind
is Virtual; hidden for repository Sites) or calls:

```http
POST /sites/{nameOrId}/virtual/publish
```

The server:

1. Validates the Site is a Git-filesystem, CSV-filesystem, SQL-database, HTTP JSON, or
   object-storage Virtual Site (repository and unknown kinds stay **400**).
2. Selects the Site filesystem publish root (must be configured, safe after NIO normalize, and
   distinct from `virtual.rootPath`).
3. Runs the same build as `POST …/virtual/build`.
4. Copies assembled HTML/assets (not `_meta`) to the Site root using portable `java.nio.file.Path`.

For `http-json`, catalog URL/file stay in `_config.yaml`; leftover `virtual.remoteUrl` is
**400** (no secrets on the REST envelope). For `object-storage`, REST Publish uses a
portable-safe local object-key `rootPath` (no cloud URLs, IAM, or access keys); leftover
`virtual.remoteUrl` is **400**. Developer Sites **Publish Virtual Site** and
**Preview assembled site** chrome are shown for Git, CSV, SQL, HTTP JSON,
object-storage, rss-atom, icalendar, and sitemap-xml after save (Publish copies last-build HTML after a successful Build for Git/CSV/SQL/HTTP JSON/object-storage/rss-atom/icalendar/sitemap-xml).

The response includes `publishPath`, `buildOutputPath`, `pagesWritten`, `filesCopied`, and
link-problem fields. Failures return **400/403/404** with an operator-readable message (never a
silent no-op). See [Publishing](id:admin-publishing).

## What is not in Phase 1

- CMS UI editing of Virtual items as normal content types
- Automatic migration of the full legacy help site
- SQL/API adapters
- Fake classic content-list generators for virtual items

## Related

- [Site configuration reference](id:reference-site-config)
- [Build from source](id:developer-build-source)
- [REST API](id:developer-rest)
