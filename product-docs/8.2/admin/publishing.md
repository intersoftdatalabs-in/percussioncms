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

## Virtual Sites and docs builds

For Git/filesystem, CSV/filesystem, SQL/database, or HTTP JSON Virtual Sites such as product documentation:

- Offline / CI builds use `scripts/build-cms-docs.bat` / `scripts/build-cms-docs.sh` to emit static HTML without a full CMS UI session. CSV trees can use `PSVirtualSiteBuildMain … csv-filesystem`. SQL trees use `PSVirtualSiteBuildMain … sql-database` (in-memory H2 only). Local object-key directories use `PSVirtualSiteBuildMain … object-storage` (no cloud credentials).
- **Build** (`POST /sites/{nameOrId}/virtual/build`) writes a staging tree under
  `{install}/tmp/virtual-sites/{siteKey}` (or an optional `outputRoot`). Each build re-reads the
  current Git/filesystem, CSV tree (`csv-filesystem`), H2 `SELECT` (`sql-database`), HTTP
  JSON catalog (`http-json`: local fixture / loopback `http.url`), or object-storage keys
  (`object-storage`: Markdown / HTML / JSON under a local `rootPath`). After
  `git pull`, a local Markdown edit, a CSV change, a `_config.yaml` change, a SQL
  `queryFile` / `sql.query` change, an H2 row change, a JSON catalog / `_config.yaml`
  edit, or an object-storage Markdown / HTML / JSON key or `_config.yaml`
  (`objects.keys`) edit, run Build (or Publish) again — no CMS restart. File
  watchers are not used. `sql-database` requires `_config.yaml` with a `sql:` mapping
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
  `virtual.remoteUrl` and credential properties are **400** (no live feeds). REST **GET/PUT**
  can persist `virtual.sourceKind=object-storage` with that local `rootPath` (cloud URLs and
  credentials are **400**); REST **Build** runs that local bucket. REST **Preview**
  streams last-build HTML for that kind after a successful Build (`available=true`;
  missing build is `available=false` HTTP 200). REST **Preview** also streams last-build
  HTML for `rss-atom` (local RSS 2.0 / Atom fixture or loopback feed; no live remote
  feeds). REST **Publish** copies that last-build HTML to `IPSSite.root` for `rss-atom`
  (`filesCopied > 0`; `_meta` skipped). Developer Sites can save **Object
  storage** (GET round-trips the kind), then **Build Virtual Site**, **Preview
  assembled site**, and **Publish Virtual Site**. Developer Sites Publish chrome for
  rss-atom stays a later phase. The endpoint does not accept an
  `outputRoot` body (always the default staging root).

### Publish a Virtual Site to the Site filesystem target

1. Sign in as **Admin**.
2. Configure the Site as a Git-filesystem, CSV-filesystem, SQL-database, HTTP JSON, object-storage, or RSS / Atom Virtual Site (see [Sites](id:admin-sites)). After you save **SQL database**, **Build Virtual Site** on Developer Sites runs the same in-memory H2 REST Build as Git/CSV. After you save **HTTP JSON**, **Build Virtual Site** then **Publish Virtual Site** copies assembled HTML to the Site filesystem root. After you save **Object storage**, **Build Virtual Site** then **Preview assembled site** and **Publish Virtual Site** run against that local object-key tree (`POST …/virtual/publish`; leftover `virtual.remoteUrl` is **400**; no cloud URLs or credentials). Integrators can persist **RSS / Atom** (`sourceKind=rss-atom`) and call REST **Publish** (`POST …/virtual/publish`) against a local RSS/Atom fixture (leftover `virtual.remoteUrl` and credentials are **400**; no live feeds). Developer Sites Publish chrome for rss-atom stays later.
3. Set the Site **publishing filesystem root** (Site root) to a dedicated directory on the CMS
   host. Relative roots (legacy values such as `../CI_Home`) are resolved against the CMS
   install directory. Do **not** point it at `virtual.rootPath` (the Markdown or CSV source tree).
4. Confirm the source root exists on the host and that the publish directory is writable.
5. From **Developer → Sites → Site detail**, choose **Publish Virtual Site** (visible for
   **Git filesystem**, **CSV filesystem**, **SQL database**, **HTTP JSON**, and
   **Object storage**; hidden for repository Sites). For **SQL database**, **HTTP JSON**,
   and **Object storage**, save the source, run **Build Virtual Site**, then
   **Publish Virtual Site**. The panel reports files copied and the destination path, or a
   clear error. Integrators can call `POST /services/sites/{nameOrId}/virtual/publish`
   instead (Git, CSV, SQL, HTTP JSON, object-storage, and rss-atom). Run **Build Virtual Site** first
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

## Failure modes to watch

- Missing template/variant or broken relationship links
- File permission errors on delivery paths
- Partial publish after mid-job failure (re-run after fixing root cause)
- DTS connectivity issues that surface as broken dynamic widgets on an otherwise static site

## Related

- [Sites & content structure](id:admin-sites)
- [Server operations](id:admin-server-ops)
