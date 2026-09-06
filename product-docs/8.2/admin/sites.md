---
id: admin-sites
title: Sites & content structure
description: Sites, folders, and content organization in Percussion CMS 8.2
version: "8.2"
order: 41
tags: [admin, sites]
---

# Sites & content structure

A **Site** is the primary organizational unit for published output and editorial structure in
Percussion CMS. Site properties include hostnames, permissions, publishing configuration, and
(optionally) Virtual Site source settings.

## Traditional Sites

Traditional Sites store pages and assets in the Percussion **content repository**:

- Editors work in the Web UI / Finder against Site folders and pages.
- Workflow, ACL permissions, and revisions apply to repository items.
- Publishing assembles templates/variants and delivers to filesystem, FTP, or other pub locations.

### Browse Sites in Content Explorer

1. Sign in as an administrator (or a role that can open **Explorer**).
2. Open **Content Explorer** (`spa.jsp?entry=explorer` or the **Explorer** product navigation entry).
3. In the tree, expand **Sites** (use the disclosure control next to the **Sites** label — selecting the row only opens the list, it does not expand child site nodes).
4. Select a site folder to browse its pages and folders in the detail list.
5. Expand a sample site (for example **Corporate_Investments** or **Enterprise_Investments**). The tree and detail list show that site's **real** folders (for example **AboutEnterpriseInvestments**, **Files**, **Images**) and pages, not an empty-folder message. Explorer also shows a **Pages** folder as site chrome. Opening **Pages** lists at least one page-type child from the sample site (the homepage or a page under an About section) so the detail list is not empty. Opening **Files** lists the FastForward Files folder items (or file children when that folder is missing).

After a standard install with **sample sites** (installer **Install sample sites** / silent `--demo-sites`), the Sites tree typically includes stock demo sites such as **Corporate Investments** and **Enterprise Investments**. Those FastForward sample sites are traditional **Rhythmyx** sites (not CM1 page-based). The installer loads the FastForward type/template seed, the **ObjectStore** editors for those types (`psx_cerffGeneric.xml`, `psx_cerffPressRelease.xml`, and the other rff editors — CM1 `perc*` types still come from packages), the **sample content** graph (site folders, Files/Images, About sections, navons, and pages), plus hashed sample binaries under `rxconfig/FastForward/importFiles` (imported on first server start). If sample items exist but the server log shows `Invalid content type id (311)` (or other 301–316 ids), those ObjectStore editors were not copied — reinstall with `--demo-sites` from an installer that includes this step. FastForward **navigation** types stay at ids 313–315 (`rffNavImage` / `rffNavon` / `rffNavTree`) and those editors must be **active** so Navigation can load the seeded tree (**Create section** stays disabled if type 315 cannot start). The `perc.nav` package installs `percNav*` under separate ids (typically 1015–1017); the sample seed must not rename 313–315 to those package names (that unique-name clash aborts the rest of the type table and drops **Press Release** id 316). Site **names** may use underscores (`Corporate_Investments`) while the repository folder is `//Sites/CorporateInvestments` — Explorer binds expand/list to the site folder root. The Sites list includes **all** sites — Rhythmyx and CM1 page-based, with or without a publishing server or navigation tree. Opening **Navigation** for a demo site that already has an `rffNavTree` / `percNavTree` shows that tree; if a site has a folder root but no NavTree, first open creates one — see [Navigation & site structure](id:admin-architecture-navigation). Fresh evaluation or H2 QA stacks without sample seed may show an empty Sites list until you create a site or reinstall with sample data. A running install that was seeded before this content pass is **not** backfilled — reinstall with `--demo-sites` (or a new H2 `qa-up`) to get FastForward pages and navons.

### Create a Site from Explorer (type picker)

Use **Content Explorer** or **Navigation → New Site** when you need a new Site without leaving the product shell. The first wizard step is **Site type**:

| Type | Managed navigation | Page template | After create |
|------|--------------------|---------------|--------------|
| **Traditional** | Optional checkbox (on by default) | Not prompted | Repository site under `/Sites/<name>` |
| **Page** | Locked on | Required (template name + base template) | Page-based site (`pageBased` on `POST /sitemanage/site/`) |
| **Virtual** | Hidden (not used) | Not prompted | Site folder without NavTree; optional Git root is saved with `PUT /sites/{name}/virtual` |

1. Open **Content Explorer** (or **Navigation**).
2. Choose **Content → Create Site** (Explorer) or **New Site** (Navigation).
3. On **Site type**, choose **Traditional**, **Page**, or **Virtual**.
4. On **Site details**, enter a unique **Site name** and optional description.
   - **Traditional:** **Include managed navigation** is checked by default. Uncheck it to create the site folder only — no NavTree and no homepage. You can add navigation later from Explorer.
   - **Page:** managed navigation stays checked and cannot be turned off.
   - **Virtual:** the managed-navigation checkbox is not shown. The discriminator is `virtual.sourceKind`, not a NavTree flag.
5. **Page** only: on **Base template**, enter a template name and pick a base template from the catalog.
6. On **Confirm**, review the type and name. **Virtual** also offers an optional **Git root path**. Leave it blank to finish source settings later on **Developer → Sites**. If you enter a path, Create Site sends the existing `{ "VirtualSiteProperties": { "sourceKind": "git-filesystem", "rootPath": "…" } }` envelope (`PUT /services/sites/{name}/virtual`) after the site is created.
7. Choose **Create site** and wait for progress to complete. Explorer opens the new site under `/Sites/<name>`.

With **Traditional** and **Include managed navigation** checked, the server seeds a NavTree, a site template, and the homepage (`index.html`) in that folder in one operation. If the folder already has a NavTree or Navon, Create Site returns HTTP 400 with a clear error (not HTTP 500).

Full Git `rootPath`, remote URL, branch, and config-file editing remains on **Developer → Sites** (Virtual Site source panel). See [Virtual Sites (developer)](id:developer-virtual-sites) and the Virtual Sites section below.

## Virtual Sites (8.2)

A **Virtual Site** is a Site whose content originates **outside** the traditional repository
(for example Git/filesystem Markdown under `product-docs/`). Virtual items are discovered and
assembled without ingesting them as ordinary CMS content items.

Operators configure Virtual Sites through **Site properties** (not new database columns in Phase 1).
Integrators can set the same properties over public Site REST
(`GET`/`PUT /sites/{nameOrId}/virtual`) without Workbench. Authors of Virtual content use Git
and Markdown tooling, not the classic page editor.

### Property keys operators set

| Property | Required | Example | Notes |
|----------|----------|---------|-------|
| `virtual.sourceKind` | Yes (for Virtual) | `git-filesystem`, `csv-filesystem`, `sql-database`, `http-json`, `object-storage`, `rss-atom`, `icalendar`, `sitemap-xml`, `robots-txt`, `llms-txt`, or `openapi-yaml` | Allow-list: **`git-filesystem`**, **`csv-filesystem`**, **`sql-database`**, **`http-json`**, **`object-storage`**, **`rss-atom`**, **`icalendar`**, **`sitemap-xml`**, **`robots-txt`**, **`llms-txt`**, **`openapi-yaml`**. **`openapi-yaml`** is a local OpenAPI 3 YAML SPI (`openapi.yaml` or `_config.yaml` `openapi.file`; no live spec fetch). REST **GET/PUT** `/sites/{nameOrId}/virtual` round-trips **`openapi-yaml`** with a portable-safe local `rootPath` (leftover `virtual.remoteUrl`, credentials, and cloud URL `rootPath` are **400**; no live spec fetch). Developer Sites can save and GET-roundtrip **OpenAPI YAML** (`sourceKind=openapi-yaml`) with a portable-safe local `rootPath`. REST **Build**, **Preview**, and **Publish** for `openapi-yaml` stay later slices. **`llms-txt`** is a local `llms.txt` SPI (`llms.txt` or `_config.yaml` `llms.file`; no live HTTP fetch). REST **GET/PUT** `/sites/{nameOrId}/virtual` round-trips **`llms-txt`** with a portable-safe local `rootPath` (leftover `virtual.remoteUrl`, credentials, and cloud URL `rootPath` are **400**; no live HTTP fetch). Developer Sites can save and GET-roundtrip **llms.txt** (`sourceKind=llms-txt`) with a portable-safe local `rootPath`, then **Build Virtual Site**, **Preview assembled site**, and **Publish Virtual Site**. REST **Publish** (`POST …/virtual/publish`) copies assembled HTML to `IPSSite.root` after a local llms.txt Build (leftover `virtual.remoteUrl`, credentials, and cloud URL `rootPath` are **400**; no live HTTP fetch; missing assemble is **400** and does not invent pages). REST **Build** (`POST …/virtual/build`) writes HTML from that local `llms.txt` / `llms.file` fixture (`pagesWritten > 0`; leftover `virtual.remoteUrl`, credentials, and cloud `rootPath` are **400**; no live HTTP fetch). REST **Preview** (`GET …/virtual/preview`) streams last-build local HTML (`available=true`; missing build is `available=false` HTTP 200). **`robots-txt`** is a local `robots.txt` SPI (`robots.txt` or `_config.yaml` `robots.file`; no live crawl). REST **GET/PUT** `/sites/{nameOrId}/virtual` round-trips **`robots-txt`** with a portable-safe local `rootPath` (leftover `virtual.remoteUrl`, credentials, and cloud URL `rootPath` are **400**; no live crawl). Developer Sites can save and GET-roundtrip **Robots.txt** (`sourceKind=robots-txt`) with a portable-safe local `rootPath`, then **Build Virtual Site**, **Preview assembled site**, and **Publish Virtual Site**. REST **Publish** (`POST …/virtual/publish`) copies assembled HTML to `IPSSite.root` after a local robots.txt Build (leftover `virtual.remoteUrl`, credentials, and cloud URL `rootPath` are **400**; no live crawl; missing assemble is **400** and does not invent pages). REST **Build** (`POST …/virtual/build`) writes HTML from that local `robots.txt` / `robots.file` fixture (`pagesWritten > 0`; leftover `virtual.remoteUrl`, credentials, and cloud `rootPath` are **400**; no live crawl). REST **Preview** (`GET …/virtual/preview`) streams last-build local HTML (`available=true`; missing build is `available=false` HTTP 200). **`sitemap-xml`** is a local `sitemap.xml` SPI (`sitemap.xml` or `_config.yaml` `sitemap.file`; no live crawl). SPI/CLI assemble is `PSVirtualSiteBuildMain … sitemap-xml`. REST **GET/PUT** `/sites/{nameOrId}/virtual` round-trips **`sitemap-xml`** with a portable-safe local `rootPath` (local sitemap fixture only; leftover `virtual.remoteUrl`, credentials, and cloud URL `rootPath` are **400**; no live crawl). REST **Build** (`POST …/virtual/build`) writes HTML from that local `sitemap.xml` / `sitemap.file` fixture (`pagesWritten > 0`; leftover `virtual.remoteUrl`, credentials, and cloud `rootPath` are **400**; no live crawl). REST **Preview** (`GET …/virtual/preview`) streams last-build local HTML (`available=true`; missing build is `available=false` HTTP 200; leftover `virtual.remoteUrl` and credentials are **400**; no live crawl). REST **Publish** (`POST …/virtual/publish`) copies assembled HTML to `IPSSite.root` after a local sitemap.xml Build (leftover `virtual.remoteUrl`, credentials, and cloud URL `rootPath` are **400**; no live crawl). Developer Sites can save and GET-roundtrip **Sitemap XML** (`sourceKind=sitemap-xml`) with a portable-safe local `rootPath` (no `virtual.remoteUrl`, no crawl credentials), then **Build Virtual Site**, **Preview assembled site**, and **Publish Virtual Site** (last-build local HTML; missing build stays unavailable). **`icalendar`** is a local RFC 5545 `.ics` SPI (`calendar.ics` or `_config.yaml` `icalendar.file`; no CalDAV). SPI/CLI assemble is `PSVirtualSiteBuildMain … icalendar`. REST **GET/PUT** `/sites/{nameOrId}/virtual` round-trips **`icalendar`** with a portable-safe local `rootPath` (local RFC 5545 fixture only; leftover `virtual.remoteUrl`, credentials, and cloud URL `rootPath` are **400**; no CalDAV). REST **Build** (`POST …/virtual/build`) runs **icalendar** against that local `calendar.ics` / `icalendar.file` fixture (`pagesWritten > 0`; leftover `virtual.remoteUrl`, credentials, and cloud `rootPath` are **400**; no CalDAV). REST **Preview** (`GET …/virtual/preview`) streams last-build HTML after assemble (`available=true`; missing build is `available=false` HTTP 200). REST **Publish** (`POST …/virtual/publish`) copies assembled HTML to `IPSSite.root`. Developer Sites can save **iCalendar** (`sourceKind=icalendar`); leftover `virtual.remoteUrl` and CalDAV credentials are **400**; Developer Sites **Build Virtual Site**, **Preview assembled site**, and **Publish Virtual Site** are shown after save. **`rss-atom`** is a local/loopback syndication SPI (`feed.xml` / `atom.xml` or `_config.yaml` `rss.file`; loopback `rss.url` only — no live feed credentials). REST **GET/PUT** `/sites/{nameOrId}/virtual` round-trips **`rss-atom`** with a portable-safe local `rootPath` (local/loopback only; leftover `virtual.remoteUrl`, live feed credentials, and cloud URL `rootPath` are **400**). Developer Sites can save **RSS / Atom** (`sourceKind=rss-atom`) and then **Build Virtual Site** (local `feed.xml` / `atom.xml` / `_config.yaml` `rss.file` only; no live feeds). REST **Build** (`POST …/virtual/build`) also runs **rss-atom** against that local/loopback fixture (`pagesWritten > 0`). REST **Preview** (`GET …/virtual/preview`) streams last-build HTML for **rss-atom** after a successful assemble (`available=true`; missing build is `available=false` HTTP 200; local/loopback fixture, no live feeds). Developer Sites **Preview assembled site** and **Publish Virtual Site** are shown for **RSS / Atom** after save (same last-build Preview and Publish as Object storage). REST **GET/PUT** also round-trips **`object-storage`** with a portable-safe local `rootPath` (cloud URLs and credential properties are **400**; `virtual.remoteUrl` is **400**). REST **Build** (`POST …/virtual/build`) also runs **object-storage** against that local bucket. REST **Preview** (`GET …/virtual/preview`) streams last-build HTML after a successful Build (`available=true`; missing build is `available=false` HTTP 200). REST **Publish** (`POST …/virtual/publish`) copies assembled HTML to the Site filesystem root for **object-storage** (local object-key `rootPath`; leftover `virtual.remoteUrl` is **400**; no AWS/IAM/secrets) and **rss-atom** (local RSS 2.0 / Atom fixture or loopback feed; leftover `virtual.remoteUrl` and credentials are **400**; no live feeds; `filesCopied > 0`). Developer Sites can save **Object storage** the same way as HTTP JSON (GET round-trips `object-storage`), then **Build Virtual Site**, **Preview assembled site**, and **Publish Virtual Site**. Blank or `repository` = traditional Site. Developer Sites can save Git, CSV, SQL, **HTTP JSON**, **Object storage**, or **RSS / Atom**. **Build Virtual Site** REST runs Git, CSV, SQL, **HTTP JSON**, **Object storage**, and **rss-atom** after save (local JSON fixture, loopback catalog, local object-key bucket, or local RSS/Atom fixture). Developer Sites **Build Virtual Site** runs Git, CSV, SQL, **HTTP JSON**, **Object storage**, and **RSS / Atom**. **Preview assembled site** REST streams last-build HTML for Git, CSV, SQL, **HTTP JSON**, **object-storage**, and **rss-atom**; Developer Sites Preview chrome runs Git, CSV, SQL, **HTTP JSON**, **object-storage**, and **RSS / Atom**. REST **Publish Virtual Site** (`POST …/virtual/publish`) runs Git, CSV, SQL, **HTTP JSON**, **Object storage**, and **rss-atom** after Build (copies assembled HTML to the Site filesystem root). Developer Sites **Publish Virtual Site** chrome runs Git, CSV, SQL, **HTTP JSON**, **Object storage**, and **RSS / Atom** after Build. REST **GET/PUT** `/sites/{nameOrId}/virtual` also round-trips **`http-json`** (safe `rootPath` JSON fixture; `virtual.remoteUrl` is **400** — catalog URL/file stay in `_config.yaml`, no secrets on the REST envelope). `http-json` assemble is SPI/CLI plus REST Build and REST Publish (see [Virtual Sites](id:developer-virtual-sites)). `sql-database` is in-memory H2 (`jdbc:h2:mem:`; JDBC URL/user/query in `_config.yaml` — never passwords on the REST envelope). CSV trees may omit `_config.yaml`. Unknown kinds are rejected. |
| `virtual.rootPath` | Yes when remote is blank | absolute path to `product-docs` | Local tree when `virtual.remoteUrl` is blank. Prefer absolute portable paths (Windows/Linux/macOS). Paths with `..` after normalize are rejected. When a remote is set, use a **relative** folder inside the checkout (for example `product-docs`). |
| `virtual.remoteUrl` | No | `https://git.example.com/org/product-docs.git` | Optional Git remote. Build clones or fetches into a contained server work directory, then discovers Markdown as usual. Blank = local-path mode. Allowed: `https://`, `ssh://`, `file://`, `git@host:path`. |
| `virtual.branch` | No | `main` | Branch to checkout when a remote is set. Default `main`. |
| `virtual.configFile` | No | `_config.yaml` | Default `_config.yaml`. Simple file name under the root (no `..` or directory separators). |
| `virtual.siteKey` | No | `product-docs` | Optional participant key; defaults to the Site name. |

Invalid combinations (unknown source kind, missing root, unsafe path, config path traversal) are
rejected by server validation with clear error messages.

### Optional `_redirects.yaml`

Place `_redirects.yaml` beside `_config.yaml` in the Virtual Site source root (for product
docs: `product-docs/_redirects.yaml`). **Build Virtual Site** honors it when present:

- Missing file is a no-op (the build still succeeds).
- Each `from` becomes a static HTML redirect page; the build also writes `redirects.json`
  next to the assembled site.
- Targets must stay on this site (relative path or `site.url` host). Off-site / `//host`
  targets fail the build (open-redirect protection).
- **Publish Virtual Site** copies those artifacts with the rest of the HTML.

See [Site configuration](id:reference-site-config) for the YAML contract.

### Browse Sites in Developer

**Developer → Sites** lists **all** CMS Sites from `GET /services/sites` (traditional repository
Sites, CM1 page-based Sites, and Virtual Sites). The JSON envelope is a Jackson root-wrapped
array (`{"SiteList":[{"name":"…",…}]}`), not an `{empty:false}` bean. Sample / demo Sites
appear when they exist on the server (for example after **Install sample sites**). If that
public list is empty or unreadable, the catalog also consults `GET /sitemanage/site/`
(the same `SiteSummary` list Home and Explorer use) so existing Sites are not hidden.

1. Sign in as an administrator (or a role that can open **Developer**).
2. Open **Developer** → **Sites** (SPA entry `spa.jsp?entry=developer&section=sites`).
3. Confirm the catalog table shows site **name**, **description**, **base URL**, and flags.
4. Choose a row to open **Site detail** (URL defaults, object GUID, Virtual Site source,
   and **Object ACL**).

The **Object ACL** section on Site detail uses the site GUID from the catalog payload
(`guid.stringValue`, or `hostId-type-uuid` when `stringValue` is omitted). When a GUID
is present, the ACL table (or empty create path) is readable and editable — Design
access plus Runtime visibility. When the catalog row has no GUID parts, the section
still mounts with site-specific empty copy and does not crash the page. See
[Users, roles & security](id:admin-users-roles) for the same Object ACL behavior on
Display Formats.

Empty state (**No sites returned**) appears only when **every** list source has **zero**
named Sites. A successful HTTP 200 with Site entries — including `SiteList`, nested
`Site`/`sites`/`item` wraps, or `SiteSummary` — must populate the table (never a silent
blank). Load failures show **Could not load sites** rather than the empty state.

### Configure Virtual Site source in the product UI

1. Sign in as an administrator (or a role that can open **Developer**).
2. Open **Developer** → **Sites** (SPA entry `spa.jsp?entry=developer&section=sites`).
3. Select a Site row to open **Site detail**.
4. In the **Virtual Site source** section:
   - **Source kind** — leave **Repository (traditional)** for ordinary CMS Sites
     (blank/`repository` on the server). Choose **Git filesystem** for Git/Markdown
     Virtual Sites, **CSV filesystem** for a CSV tree on disk, **SQL database**
     for an in-memory H2 JDBC source, **HTTP JSON** for a local JSON fixture or
     loopback HTTP catalog, **Object storage** for a local object-key directory,
     **RSS / Atom** for a local RSS or Atom fixture directory, **iCalendar**
     for a local RFC 5545 `.ics` fixture directory (no CalDAV), or **Sitemap XML**
     for a local `sitemap.xml` fixture directory (no live crawl).
     Repository stays the default.
   - **Root path** — absolute or install-relative path to the documentation, CSV,
     SQL `_config.yaml`, HTTP JSON catalog, object-storage tree, RSS/Atom,
     iCalendar, or sitemap XML fixture (required when source kind is
     Virtual and no Git remote is set). Do not use `..` path segments. Shared by
     Git, CSV, SQL, HTTP JSON, Object storage, RSS / Atom, iCalendar, and Sitemap XML. When a **Remote URL** is set (Git only), this
     may be a relative folder inside the checkout.
   - **Remote URL** (optional, **Git filesystem** only) — Git remote (`https://`,
     `ssh://`, `file://`, or `git@host:path`). Hidden for **CSV filesystem**,
     **SQL database**, **HTTP JSON**, **Object storage**, **RSS / Atom**, **iCalendar**, and **Sitemap XML** (the server rejects `virtual.remoteUrl`
     on those kinds).
     Leave blank to keep the local **Root path**. When set, **Build Virtual Site**
     clones or fetches on the CMS host (`git` must be on the server `PATH`). The
     panel does not re-implement checkout.
   - **Branch** (optional, **Git filesystem** only) — branch to checkout when a
     remote is set. Defaults to `main` on the server when blank.
   - **Config file** (optional, **Git filesystem** only) — simple file name under
     the root (default `_config.yaml` when unset). No path separators.
   - **Site key** (optional, **Git filesystem** only) — participant registry key;
     defaults to the Site name when blank.
5. Choose **Save Virtual Site source**. For Git the SPA sends the Jackson/JAXB
   envelope `{ "VirtualSiteProperties": { "sourceKind": "git-filesystem",
   "rootPath": "…", "remoteUrl": "…", "branch": "…", … } }` (not a bare
   `sourceKind` object). For CSV it sends `{ "VirtualSiteProperties": {
   "sourceKind": "csv-filesystem", "rootPath": "…" } }` (no `remoteUrl`). For
   SQL it sends `{ "VirtualSiteProperties": { "sourceKind": "sql-database",
   "rootPath": "…" } }` (no `remoteUrl`, no password). JDBC URL, user, and
   query stay in `_config.yaml` under the root. For HTTP JSON it sends
   `{ "VirtualSiteProperties": { "sourceKind": "http-json", "rootPath": "…" } }`
   (no `remoteUrl`, no Authorization or API keys). Catalog URL (`http.url`) or
   local fixture (`http.file` / default `pages.json`) stay in `_config.yaml`.
   For Object storage it sends `{ "VirtualSiteProperties": {
   "sourceKind": "object-storage", "rootPath": "…" } }` (no `remoteUrl`, no
   cloud URLs, IAM, or access keys). For RSS / Atom it sends
   `{ "VirtualSiteProperties": { "sourceKind": "rss-atom", "rootPath": "…" } }`
   (no `remoteUrl`, no live feed URLs or credentials). For iCalendar it sends
   `{ "VirtualSiteProperties": { "sourceKind": "icalendar", "rootPath": "…" } }`
   (no `remoteUrl`, no CalDAV URLs or credentials). For sitemap XML it sends
   `{ "VirtualSiteProperties": { "sourceKind": "sitemap-xml", "rootPath": "…" } }`
   (no `remoteUrl`, no live crawl URLs or credentials). For robots.txt it sends
   `{ "VirtualSiteProperties": { "sourceKind": "robots-txt", "rootPath": "…" } }`
   (no `remoteUrl`, no live crawl URLs or credentials). For llms.txt it sends
   `{ "VirtualSiteProperties": { "sourceKind": "llms-txt", "rootPath": "…" } }`
   (no `remoteUrl`, no live HTTP fetch URLs or credentials). For OpenAPI YAML it sends
   `{ "VirtualSiteProperties": { "sourceKind": "openapi-yaml", "rootPath": "…" } }`
   (no `remoteUrl`, no live spec fetch URLs or credentials). After a successful save the panel reloads properties from GET so the kind
   and root persist without a full page reload. **Build Virtual Site**,
   **Preview assembled site**, and **Publish Virtual Site** appear for
   **Git filesystem**, **CSV filesystem**, **SQL database**, **HTTP JSON**,
   **Object storage**, **RSS / Atom**, **iCalendar**, **Sitemap XML**, and **Robots.txt**. **llms.txt** shows **Build Virtual Site** and **Preview assembled site** after save; **Publish Virtual Site** stays hidden for that kind. **OpenAPI YAML** save/GET-roundtrip is available; **Build Virtual Site**, **Preview assembled site**, and **Publish Virtual Site** stay hidden for that kind. Traditional **Repository** hides that chrome.
   After you save **Object storage**, **Build Virtual Site** runs against the local object-key
   directory (`virtual.rootPath`); **Preview assembled site** opens last-build home HTML
   the same way as Git/CSV/SQL/HTTP JSON. **Publish Virtual Site** (and REST
   `POST …/virtual/publish`) copies assembled files to the Site filesystem root
   (`IPSSite.root`) for Git, CSV, SQL, HTTP JSON, object-storage (local object-key
   `rootPath`; leftover `virtual.remoteUrl` is **400**; no AWS/IAM/secrets), and
   **rss-atom** (local RSS/Atom fixture; leftover `virtual.remoteUrl` and credentials
   are **400**; no live feeds), and **icalendar** (local RFC 5545 fixture; leftover
   `virtual.remoteUrl` and credentials are **400**; no CalDAV).
   After you save **RSS / Atom**, GET round-trips `sourceKind=rss-atom` with the
   local fixture `rootPath`; REST **Build**, **Preview**, and **Publish** run for this
   kind. **Build Virtual Site** runs against that local `feed.xml` / `atom.xml` /
   `_config.yaml` `rss.file` (no live feeds). **Preview assembled site** opens
   last-build home HTML the same way as Git/CSV/SQL/HTTP JSON/object-storage.
   **Publish Virtual Site** copies assembled files to the Site filesystem root
   (local RSS/Atom fixture only; leftover `virtual.remoteUrl` and credentials are
   **400**; no live feeds).
6. To return a Virtual Site to traditional repository mode, set source kind back to
   **Repository (traditional)** and save (clears `virtual.*` properties). Switching
   the select back to Repository hides virtual fields immediately; Save is still
   required to persist the clear.

To use a **Git remote** as the system of record (clone/fetch on Build), set **Remote URL**
and **Branch** on this panel (or `virtual.remoteUrl` / `virtual.branch` via
`PUT /services/sites/{name}/virtual`) — see
[Virtual Sites (developer)](id:developer-virtual-sites). Leave **Remote URL** blank to keep
a local Git checkout path.

Validation matches the server helper (`PSVirtualSiteHelper`): allow-listed source kinds
(`git-filesystem`, `csv-filesystem`, `sql-database`, `http-json`, `object-storage`, `rss-atom`, `icalendar`, `sitemap-xml`, `robots-txt`, `llms-txt`, `openapi-yaml`), required local root path when virtual and no remote,
safe remote URLs/branches for Git only (`csv-filesystem`, `sql-database`, `http-json`, `object-storage`, `rss-atom`, `icalendar`, `sitemap-xml`, `robots-txt`, `llms-txt`, and `openapi-yaml` reject `virtual.remoteUrl`), and
safe path/config names (no remaining `..` after NIO normalize). `object-storage` also
rejects cloud URLs and credential properties. `rss-atom` also rejects live feed
URLs, credential properties, and cloud URL `rootPath` (local fixture `rootPath` only).
`icalendar` is a local RFC 5545 `.ics` fixture (`calendar.ics` / `icalendar.file`; no CalDAV);
REST **GET/PUT** `/sites/{nameOrId}/virtual` round-trips `sourceKind=icalendar` with a
portable-safe local `rootPath` (leftover `virtual.remoteUrl`, credentials, and cloud URL
`rootPath` are **400**). `sitemap-xml` is a local `sitemap.xml` fixture (`sitemap.xml` / `sitemap.file`; no live crawl);
REST **GET/PUT** `/sites/{nameOrId}/virtual` round-trips `sourceKind=sitemap-xml` with a
portable-safe local `rootPath` (leftover `virtual.remoteUrl`, credentials, and cloud URL
`rootPath` are **400**). `robots-txt` is a local `robots.txt` fixture (`robots.txt` / `robots.file`; no live crawl);
REST **GET/PUT** `/sites/{nameOrId}/virtual` round-trips `sourceKind=robots-txt` with a
portable-safe local `rootPath` (leftover `virtual.remoteUrl`, credentials, and cloud URL
`rootPath` are **400**). Developer Sites can save **Robots.txt** (`sourceKind=robots-txt`)
and GET-roundtrip the kind (local fixture `rootPath` only), then **Build Virtual Site**, **Preview assembled site**, and **Publish Virtual Site**. `llms-txt` is a local `llms.txt` fixture (`llms.txt` / `llms.file`; no live HTTP fetch);
REST **GET/PUT** `/sites/{nameOrId}/virtual` round-trips `sourceKind=llms-txt` with a
portable-safe local `rootPath` (leftover `virtual.remoteUrl`, credentials, and cloud URL
`rootPath` are **400**). Developer Sites can save **llms.txt** (`sourceKind=llms-txt`)
and GET-roundtrip the kind (local fixture `rootPath` only), then **Build Virtual Site**, **Preview assembled site**, and **Publish Virtual Site**. `openapi-yaml` is a local OpenAPI 3 YAML fixture (`openapi.yaml` / `openapi.file`; no live spec fetch);
REST **GET/PUT** `/sites/{nameOrId}/virtual` round-trips `sourceKind=openapi-yaml` with a
portable-safe local `rootPath` (leftover `virtual.remoteUrl`, credentials, and cloud URL
`rootPath` are **400**). Developer Sites can save **OpenAPI YAML** (`sourceKind=openapi-yaml`)
and GET-roundtrip the kind (local fixture `rootPath` only). REST **Build**, **Preview**, and **Publish** for `openapi-yaml` stay later slices. REST **Publish** (`POST …/virtual/publish`) copies assembled HTML to `IPSSite.root` after a local llms.txt Build (leftover `virtual.remoteUrl`, credentials, and cloud URL `rootPath` are **400**; no live HTTP fetch; missing assemble is **400** and does not invent pages). REST **Build** (`POST …/virtual/build`) writes HTML from that local `llms.txt` / `llms.file` fixture (`pagesWritten > 0`; leftover `virtual.remoteUrl`, credentials, and cloud `rootPath` are **400**; no live HTTP fetch). REST **Preview** (`GET …/virtual/preview`) streams last-build local HTML (`available=true`; missing build is `available=false` HTTP 200). REST **Build** (`POST …/virtual/build`) also writes HTML from that local
`sitemap.xml` / `sitemap.file` fixture (`pagesWritten > 0`; leftover `virtual.remoteUrl`,
credentials, and cloud `rootPath` are **400**; no live crawl). REST **Preview**
(`GET …/virtual/preview`) streams last-build local HTML (`available=true`; missing build is
`available=false` HTTP 200; leftover `virtual.remoteUrl` and credentials are **400**; no
live crawl). REST **Publish** (`POST …/virtual/publish`) copies assembled HTML to `IPSSite.root` after a local sitemap.xml Build (leftover `virtual.remoteUrl`, credentials, and cloud URL `rootPath` are **400**; no live crawl). Developer Sites can save **Sitemap XML**, then **Build Virtual Site**, **Preview assembled site**, and **Publish Virtual Site** (last-build local HTML; missing build stays unavailable). REST **Build**, **Preview**, and **Publish** run for `icalendar`
(local fixture only; leftover `virtual.remoteUrl` and credentials are **400**). Developer
Sites can save **iCalendar** (`sourceKind=icalendar`) and then **Build Virtual Site**,
**Preview assembled site**, and **Publish Virtual Site**. Developer Sites can save
**Sitemap XML** (`sourceKind=sitemap-xml`) and GET-roundtrip the kind (local fixture
`rootPath` only), then **Build Virtual Site**, **Preview assembled site**, and **Publish Virtual Site**. After root, remote, or
config changes, re-run the offline docs build, the in-product **Build Virtual Site**
action (below), or the CMS publish path to verify links.

Integrators persist CSV trees the same way as Git: `PUT /services/sites/{name}/virtual`
with `{ "VirtualSiteProperties": { "sourceKind": "csv-filesystem", "rootPath": "…" } }`.
`GET` returns the same `sourceKind`. SQL trees use `"sourceKind": "sql-database"` and a
safe `rootPath` that holds `_config.yaml` with the `sql:` mapping (H2 mem JDBC URL and
user; do not put passwords on the REST envelope). In-product REST **Build Virtual Site**
runs for `git-filesystem`, `csv-filesystem`, `sql-database`, `http-json`,
`object-storage`, `rss-atom`, `icalendar`, `sitemap-xml`, `robots-txt`, and `llms-txt` (`POST …/virtual/build`). A second sitemap-xml Build after an in-process `sitemap.xml` / page edit returns `pagesWritten > 0` HTML that matches the current files (no Jetty restart). CSV assemble does not require a Git remote;
`_config.yaml` is optional for CSV and required for SQL, HTTP JSON, object-storage, rss-atom, icalendar, sitemap-xml, robots-txt, and llms-txt.
HTTP JSON trees use `"sourceKind": "http-json"` and a safe `rootPath`; `_config.yaml` is
required (versions plus `http.url` or `http.file` / default `pages.json`). Developer Sites
can save HTTP JSON the same way as SQL (GET round-trips `http-json`) and then **Build
Virtual Site**. Integrators persist object-storage with `"sourceKind": "object-storage"`
and a safe local `rootPath` (GET round-trips the kind; no AWS/IAM/secrets) and then run
REST **Build Virtual Site** against that bucket. Developer Sites can save **Object storage**
the same way (GET round-trips `object-storage`) and then **Build Virtual Site**,
**Preview assembled site**, and **Publish Virtual Site**. REST **Publish**
(`POST …/virtual/publish`) copies assembled HTML onto the Site filesystem root for a
local object-key `rootPath` (`virtual.remoteUrl` is **400**) and for **rss-atom**
(local RSS 2.0 / Atom fixture or loopback feed; leftover `virtual.remoteUrl` and
credentials are **400**; no live feeds). After a successful Build,
**Preview assembled site** opens last-build home HTML, and **Publish Virtual Site**
copies assembled HTML onto the Site filesystem root for Git, CSV, SQL, HTTP JSON,
object-storage, rss-atom, and icalendar. Integrators persist RSS / Atom with `"sourceKind": "rss-atom"`
and a safe local `rootPath` (GET round-trips the kind; no live feed URLs or credentials;
`virtual.remoteUrl` is **400**). Developer Sites can save **RSS / Atom** the same way
(GET round-trips `rss-atom`) and then **Build Virtual Site** (local `feed.xml` /
`atom.xml` / `_config.yaml` `rss.file` only), **Preview assembled site**, and
**Publish Virtual Site**. REST **Build**, **Preview**, and **Publish** run for rss-atom.
Integrators persist iCalendar with `"sourceKind": "icalendar"` and a safe local `rootPath`
(GET round-trips the kind; leftover `virtual.remoteUrl`, credentials, and cloud URL
`rootPath` are **400**; no CalDAV). REST **Build** (`POST …/virtual/build`) writes HTML
from `calendar.ics` / `icalendar.file` (`pagesWritten > 0`). REST **Preview** reports
last-build status (`available=false` HTTP **200** when no assemble). REST **Publish**
copies assembled HTML to `IPSSite.root`. Developer Sites can save **iCalendar** the same
way (GET round-trips `icalendar`) and then **Build Virtual Site** (local `calendar.ics` /
`icalendar.file` only), **Preview assembled site**, and **Publish Virtual Site**.

### Build a Virtual Site from the product UI

When **Source kind** is **Git filesystem**, **CSV filesystem**, **SQL database**,
**HTTP JSON**, **Object storage**, **RSS / Atom**, **iCalendar**, **Sitemap XML**, **Robots.txt**, or **llms.txt**, the Site detail panel shows **Build Virtual Site**
and **Preview assembled site** after save. **Publish Virtual Site** is also shown for those kinds including **llms.txt**. Traditional
**Repository** Sites do **not** show these controls (no misleading virtual-build or
virtual-publish chrome). After you save **SQL database**, **Build Virtual Site**
runs `POST /services/sites/{name}/virtual/build` against in-memory H2
(`jdbc:h2:mem:` in `_config.yaml` — no Oracle/MySQL live matrix on this path).
After you save **HTTP JSON**, the same Build action runs against a local JSON
catalog (`http.file` or default `pages.json`) or loopback `http.url` in
`_config.yaml` (no Authorization or API keys on the REST envelope). After you save
**Object storage**, Build runs against a local object-key directory (Markdown / HTML /
JSON; no cloud URLs or access keys). After you save **RSS / Atom**, Build runs against
a local RSS 2.0 or Atom XML fixture (`feed.xml` / `atom.xml` or `_config.yaml` `rss.file`;
loopback `rss.url` only — no live feed credentials). After you save **iCalendar**, Build
runs against a local RFC 5545 fixture (`calendar.ics` or `_config.yaml` `icalendar.file`;
no CalDAV). After you save **Sitemap XML**, Build runs against a local `sitemap.xml` fixture
(`sitemap.xml` or `_config.yaml` `sitemap.file`; no live crawl). After you save **Robots.txt**,
Build runs against a local `robots.txt` fixture (`robots.txt` or `_config.yaml` `robots.file`;
no live crawl). After you save **llms.txt**, Build runs against a local `llms.txt` fixture
(`llms.txt` or `_config.yaml` `llms.file`; no live HTTP fetch). Edit loc, lastmod, or
the referenced page files on the CMS host and choose **Build Virtual Site** again — assembled
output updates without a Jetty restart. After that Git/CSV/SQL/HTTP JSON/object-storage/rss-atom/icalendar/sitemap-xml/robots-txt/llms-txt Build succeeds,
**Preview assembled site** streams last-build home HTML. **Publish Virtual Site** copies
that HTML to the Site filesystem root for Git/CSV/SQL/HTTP JSON/object-storage/rss-atom/icalendar/sitemap-xml/robots-txt/llms-txt.

1. Sign in as an **Admin** (the build REST operation requires Admin).
2. Open **Developer** → **Sites** and open the Virtual Site detail.
3. Confirm **Virtual Site source** is saved:
   - **Git filesystem** — a valid **Root path** on the CMS host **or** a saved
     **Remote URL** and **Branch**. When a remote is set, Build clones or fetches
     first (`git` must be on the CMS `PATH`).
   - **CSV filesystem** — a valid **Root path** to a CSV tree on the CMS host
     (required columns `id`, `title`, `body` under each version folder). Git remotes
     are hidden; `_config.yaml` is optional.
   - **SQL database** — a valid **Root path** to a tree whose `_config.yaml`
     contains the `sql:` mapping (in-memory H2 `jdbc:h2:mem:` URL, user, and
     `SELECT` with required columns `id`, `title`, `body`). Git remotes are
     hidden. Do not put JDBC passwords on the REST envelope.
   - **HTTP JSON** — a valid **Root path** to a tree whose `_config.yaml`
     declares versions and either `http.file` (or default `pages.json`) or a
     loopback `http.url`. Git remotes are hidden. Do not put Authorization or
     API keys on the REST envelope.
   - **Object storage** — a valid **Root path** to a local object-key directory
     whose `_config.yaml` declares versions (Markdown / HTML / JSON keys). Git
     remotes are hidden. Do not put cloud URLs, IAM, or access keys on the REST
     envelope.
   - **RSS / Atom** — a valid **Root path** to a local RSS 2.0 or Atom XML fixture
     (`feed.xml` / `atom.xml` or `_config.yaml` `rss.file`). Git remotes are hidden.
     Do not put live feed URLs or credentials on the REST envelope.
   - **iCalendar** — a valid **Root path** to a local RFC 5545 `.ics` fixture
     (`calendar.ics` or `_config.yaml` `icalendar.file`). Git remotes are hidden.
     Do not put CalDAV URLs or credentials on the REST envelope.
   - **Sitemap XML** — a valid **Root path** to a local `sitemap.xml` fixture
     (`sitemap.xml` or `_config.yaml` `sitemap.file`). Git remotes are hidden.
     Do not put live crawl URLs or credentials on the REST envelope.
   - **Robots.txt** — a valid **Root path** to a local `robots.txt` fixture
     (`robots.txt` or `_config.yaml` `robots.file`). Git remotes are hidden.
     Do not put live crawl URLs or credentials on the REST envelope.
   - **llms.txt** — a valid **Root path** to a local `llms.txt` fixture
     (`llms.txt` or `_config.yaml` `llms.file`). Git remotes are hidden.
     Do not put live HTTP fetch URLs or credentials on the REST envelope.
   If you just edited properties, choose **Save Virtual Site source** first — the
   build uses the **saved** server properties, not unsaved form fields.
4. Choose **Build Virtual Site**.
5. After a `git pull` or a local Markdown/frontmatter edit on the host — or after the remote
   branch moves — or after a CSV file or `_config.yaml` change — or after the SQL
   `_config.yaml`, `sql.queryFile`, `SELECT`, or H2 rows change — or after an HTTP JSON
   catalog (`pages.json` / `http.file` / loopback `http.url`) or `_config.yaml` change —
   or after an object-storage Markdown / HTML / JSON object key or `_config.yaml`
   (`objects.keys` or site title) change — or after an RSS / Atom fixture
   (`feed.xml` / `atom.xml` / `rss.file` / loopback `rss.url`) or `_config.yaml`
   change — or after an iCalendar fixture (`calendar.ics` / `icalendar.file`) or
   `_config.yaml` change — or after a sitemap.xml `<loc>` / `<lastmod>` / path edit
   or a `_config.yaml` `sitemap.file` change (or referenced local page files) — or after
   a robots.txt / `robots.file` change — or after an llms.txt / `llms.file` change — choose
   **Build Virtual Site** again. The build re-reads the current tree
   (and re-fetches when a Git remote is configured) — **do not restart the CMS** (and do
   **not** restart Jetty) just to pick up those edits. There is no file watcher; the next
   explicit build is the refresh.
6. Wait for the busy indicator, then review:
   - **Success** — pages written, absolute output path (default under
     `{install}/tmp/virtual-sites/{siteKey}` when no custom output is set).
   - **Link problems** — a count appears when internal `id:` or relative links fail.
     Expand **Show link problem details** to read the same lines written to
     `link-report.txt` in the output directory. **Copy link problems** puts those
     lines on the clipboard. The build still completes with HTTP 200
     (`hasLinkProblems=true`); this is not a 500 and is not a failed save.
     A clean build does **not** show a link-problem banner.
   - **Error** — clear message when the Site is not virtual, the root is missing/invalid,
     or the caller lacks Admin (for example 400/403 from REST).

### Preview the assembled Virtual Site

After a successful **Build Virtual Site**, operators can open the assembled home from the
same Site detail panel (no CLI, no `file://` path). Preview is last-output based: it works
for **Git filesystem**, **CSV filesystem**, **SQL database**, **HTTP JSON**,
**object-storage**, **`rss-atom`**, **`icalendar`**, **`sitemap-xml`**, **`robots-txt`**, and **`llms-txt`** (`sql-database` / `http-json` / `object-storage` /
`rss-atom` / `icalendar` / `sitemap-xml` / `robots-txt` / `llms-txt` REST last-build) — preview is not git-only. Traditional **Repository** Sites hide **Build Virtual Site**,
**Preview assembled site**, and **Publish Virtual Site**. Developer Sites **Preview
assembled site** appears for Git, CSV, SQL, HTTP JSON, Object storage, RSS / Atom, iCalendar, Sitemap XML, Robots.txt, and llms.txt after a successful
Build. **Publish Virtual Site** is shown for those same kinds including llms.txt. After REST or in-product Build for `sql-database` (in-memory)
H2), `http-json` (local JSON fixture or loopback catalog), `object-storage` (local
object-key bucket), `rss-atom` (local RSS 2.0 / Atom fixture or loopback feed),
`icalendar` (local RFC 5545 `calendar.ics` / `icalendar.file`), or
`sitemap-xml` (local `sitemap.xml` / `sitemap.file`; no live crawl), or
`robots-txt` (local `robots.txt` / `robots.file`; no live crawl),
`GET /services/sites/{name}/virtual/preview` returns `available=true`
and `homePath`
(typically `{version}/index.html` for the configured default version) and
`GET …/virtual/preview/{path}` streams the assembled HTML. Missing build is
`available=false` with HTTP **200** (not 500). In-product REST Build records the last
output path (including a custom `outputRoot`).

Offline CLI assemble (`PSVirtualSiteBuildMain`) does **not** write that last-output
pointer. CLI output is previewable only when `outputRoot` is the default
`{install}/tmp/virtual-sites/{siteKey}` (or `{java.io.tmpdir}/percussion-virtual-sites/{siteKey}`
when the install root is unavailable). A custom CLI output directory is not previewable
until REST Build records it.

1. Stay on **Developer → Sites → Site detail** for the Virtual Site (Admin).
2. Confirm **Source kind** is saved and **Build Virtual Site** completed, then choose
   **Preview assembled site**:
   - **Git filesystem** — saved **Root path** (or remote) and a successful Git Build.
   - **CSV filesystem** — saved **Root path** to a CSV tree and a successful CSV Build.
   - **SQL database** — saved **Root path** to a tree whose `_config.yaml` has the
     `sql:` mapping (in-memory H2), then a successful SQL Build. JDBC URL, user, and
     query stay in `_config.yaml`; the REST envelope never carries a password.
   - **HTTP JSON** — saved **Root path** to a tree whose `_config.yaml` declares
     versions and either `http.file` (or default `pages.json`) or a loopback
     `http.url`, then a successful HTTP JSON Build. Catalog URL/file stay in
     `_config.yaml`; the REST envelope never carries Authorization or API keys.
   - **Object storage** — saved **Root path** to a local object-key directory
     whose `_config.yaml` declares versions (optional `objects.keys`), then a successful
     object-storage Build. Cloud URLs and credentials stay off this envelope.
     **Preview assembled site** opens last-build home HTML the same way as Git/CSV/SQL/HTTP JSON.
   - **RSS / Atom** — saved **Root path** to a local RSS 2.0 or Atom XML fixture
     (`feed.xml` / `atom.xml` or `_config.yaml` `rss.file`; loopback `rss.url` only),
     then a successful rss-atom Build. **Preview assembled site** opens last-build home
     HTML the same way as Git/CSV/SQL/HTTP JSON/object-storage. Give a feed item an id
     that slugs to `index` (for example RSS `<guid>index</guid>`) so last-build Preview
     can report `available=true` with `{version}/index.html`. No live remote feeds.
   - **Sitemap XML** — saved **Root path** to a local `sitemap.xml` fixture
     (`sitemap.xml` or `_config.yaml` `sitemap.file`), then a successful sitemap-xml Build.
     **Preview assembled site** opens last-build local HTML only. Give a `<loc>` a last
     path segment that slugs to `index` (for example `pages/index.md`) so last-build Preview
     can report `available=true` with `{version}/index.html`. No live crawl.
     **Publish Virtual Site** copies last-build HTML to `IPSSite.root`.
   - **Robots.txt** — saved **Root path** to a local `robots.txt` fixture
     (`robots.txt` or `_config.yaml` `robots.file`), then a successful robots-txt Build.
     **Preview assembled site** opens last-build local HTML only (a single `User-agent`
     group typically assembles `{version}/star-1.html`; Preview uses that sole HTML page
     when `index.html` is absent). No live crawl.
     **Publish Virtual Site** copies last-build HTML to `IPSSite.root`.
   - **llms.txt** — saved **Root path** to a local `llms.txt` fixture
     (`llms.txt` or `_config.yaml` `llms.file`), then a successful llms-txt Build.
     **Preview assembled site** opens last-build local HTML only (a single markdown list
     link typically assembles `{version}/Quickstart-1.html`; Preview uses that sole HTML
     page when `index.html` is absent). No live HTTP fetch.
     **Publish Virtual Site** copies last-build HTML to `IPSSite.root`.
   Traditional **Repository** hides **Preview assembled site** (same as Build/Publish).
3. The CMS opens the last build’s home (typically `8.2/index.html`, or root `index.html`
   when present) in a new tab. Navigation stays on the same-origin preview URL
   (`GET /services/sites/{name}/virtual/preview/{path}`).
4. If no build has been run yet (or the last output is missing), the panel shows a clear
   empty state — **No assembled site to preview. Run Build Virtual Site first.** — and
   does not return HTTP 500.

The preview stream reads the last recorded `outputPath` from the build (default
`{install}/tmp/virtual-sites/{siteKey}`). After a Git, CSV, SQL, HTTP JSON,
object-storage, or rss-atom assemble, `GET /services/sites/{name}/virtual/preview` reports
`available` + `homePath`, and `GET …/virtual/preview/{path}` streams the HTML. It is
**Admin-only**, path-traversal safe, and does not invent a second assembler.

Integrators can call the same operations over REST:
`POST /sites/{nameOrId}/virtual/build` (optional JSON body `outputRoot`), then
`GET /sites/{nameOrId}/virtual/preview` (status) and
`GET /sites/{nameOrId}/virtual/preview/{relPath}` (HTML). See
[Site configuration reference](id:reference-site-config) and
[Virtual Sites (developer)](id:developer-virtual-sites).

### Publish a Virtual Site to the Site filesystem target

Build output under `{install}/tmp/virtual-sites/` is **staging**. To deliver a navigable static
site to the Site's configured filesystem publish location:

1. Set the Site **publishing filesystem root** (Site root / `IPSSite.root`) to a dedicated
   directory on the CMS host (not the Markdown `virtual.rootPath`).
2. As **Admin**, open **Developer → Sites → Site detail** for the Virtual Site.
3. Confirm **Source kind** is **Git filesystem**, **CSV filesystem**, **SQL database**,
   **HTTP JSON**, **Object storage**, **RSS / Atom**, **iCalendar**, **Sitemap XML**, or **Robots.txt** and **Save Virtual Site source** if you changed
   properties. Traditional **Repository** Sites never show Publish chrome. **SQL database**
   requires `_config.yaml` under `virtual.rootPath` with an in-memory H2 `sql:` mapping
   (`jdbc:h2:mem:`); Oracle / MySQL / SQL Server URLs return **400**. **HTTP JSON**
   requires `_config.yaml` under `virtual.rootPath` (versions plus `http.url` or
   `http.file` / default `pages.json`); leftover `virtual.remoteUrl` is **400**.
   **Object storage** requires `_config.yaml` under a portable-safe local `rootPath`
   (Markdown / HTML / JSON keys; leftover `virtual.remoteUrl` is **400**; no AWS/IAM/secrets).
4. Choose **Publish Virtual Site**. The panel shows a busy state, then success with
   **files copied** and the **destination path**, or a clear error (not Admin, still a
   repository Site on the server, missing or unsafe Site root).
   For **SQL database**, **HTTP JSON**, **Object storage**, and **RSS / Atom**, run **Build Virtual Site**
   first so the panel shows pages written, then **Publish Virtual Site** to copy assembled
   HTML (typically `8.2/index.html` for the default version) onto the Site filesystem root.
   For **Sitemap XML**, **Publish Virtual Site** builds then copies last-build local HTML
   from a `sitemap.xml` / `sitemap.file` fixture (leftover `virtual.remoteUrl` and crawl
   credentials fail closed; no live crawl).
   For **Robots.txt**, **Publish Virtual Site** builds then copies last-build local HTML
   from a `robots.txt` / `robots.file` fixture (typically `{version}/star-1.html` for a
   single `User-agent` group; leftover `virtual.remoteUrl` and crawl credentials fail
   closed; missing assemble is **400** and does not invent pages; no live crawl).
   Traditional **Repository** Sites never show this control.
5. Integrators can call the same operation over REST: `POST /sites/{nameOrId}/virtual/publish`.
   The server **builds then copies** HTML/assets to that Site root and returns `publishPath`
   and `filesCopied`. Missing or unsafe Site root, overlap with the source tree, or a
   non-virtual Site returns **400** with a readable message (not HTTP 500 / silent no-op).
   For **HTTP JSON**, REST Publish uses a local JSON fixture under a portable-safe
   `rootPath` (catalog URL/file stay in `_config.yaml`; leftover `virtual.remoteUrl` is
   **400**; no Authorization or API keys on the envelope). For **object-storage**, REST
   Publish uses a portable-safe local object-key `rootPath` (no cloud URLs, IAM, or access
   keys; leftover `virtual.remoteUrl` is **400**). For **rss-atom**, REST Publish uses a
   local RSS 2.0 / Atom fixture or loopback feed (`feed.xml` / `atom.xml` or `_config.yaml`
   `rss.file`; leftover `virtual.remoteUrl` and credentials are **400**; no live feeds).
   Developer Sites **Publish Virtual Site** chrome runs the same action after Build for
   Git, CSV, SQL, HTTP JSON, Object storage, and RSS / Atom.

See [Publishing](id:admin-publishing) for the operator checklist.

Offline scripts (`scripts/build-cms-docs.*`) remain available for developer workstations
without a running CMS.

See [Virtual Sites (developer)](id:developer-virtual-sites) and
[Site configuration reference](id:reference-site-config) for full contract and offline build steps.

## Folders, pages, and assets

| Concept | Role |
|---------|------|
| **Site root / folders** | Hierarchy in Finder; drives navigation and pub structure |
| **Pages** | Assembled HTML (or other) destinations for site visitors |
| **Assets** | Shared content fragments (images, rich text, shared widgets) referenced by pages |
| **Templates / variants** | Presentation used during assembly and publishing |

## Operational tips

- Prefer clear folder naming that matches the public URL tree when possible.
- Limit deep nesting that confuses editors and slows Finder browsing.
- Document which Sites are production vs staging vs documentation Virtual Sites.
- After property changes that affect Virtual roots, re-run the offline docs build or CMS pub path to verify links.

## Related

- [Content Explorer](id:admin-content-explorer) — product shell for browsing Sites/folders and running server actions
- [Publishing](id:admin-publishing)
- [Users, roles & security](id:admin-users-roles)
