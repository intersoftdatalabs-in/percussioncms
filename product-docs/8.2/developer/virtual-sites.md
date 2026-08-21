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
`POST /sites/{nameOrId}/virtual/build`. Developer **Sites** shows **Build Virtual Site**
and **Preview assembled site** for **CSV filesystem** as well as Git filesystem.
Preview REST (`GET …/virtual/preview`) is last-output based (not git-only).

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
- Leave the door open for future adapters (SQL, API, object storage) without renaming Site → Channel.

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
| **Current filesystem** | Each build reloads `_config.yaml` and re-reads every Markdown/frontmatter file from disk. The CMS process does **not** keep a parsed-page cache across builds. After `git pull` or a local edit under `virtual.rootPath`, run **Build Virtual Site** (or the offline docs script) again — **no JVM / CMS restart** is required. File watchers are not used; the next explicit build is the refresh. |

Operators can treat the JSONL under the build meta directory as a diagnostic dump of stable ids after
an offline docs build. The registry is **not** a substitute for Git as the system of record.

## Site properties (CMS)

When a Percussion Site is configured as virtual (Phase 1 property contract — no new `RXSITES`
columns), set these Site properties. The server helper
`com.percussion.services.virtualsite.PSVirtualSiteHelper` validates the contract before a Site is
treated as a safe Virtual Site source.

| Property | Required | Example | Meaning |
|----------|----------|---------|---------|
| `virtual.sourceKind` | Yes (for Virtual) | `git-filesystem` or `csv-filesystem` | Adapter wire name. **Allow-list:** `git-filesystem`, `csv-filesystem`. Blank or `repository` ⇒ traditional repository Site. Unknown values are rejected. CMS **Build** REST (`POST …/virtual/build`) runs the matching adapter. Preview REST (`GET …/virtual/preview`) streams last-build HTML for both kinds. |
| `virtual.rootPath` | Yes when remote is blank | absolute path to `product-docs` (or install-relative) | Local filesystem root when `virtual.remoteUrl` is blank. When a remote is set, optional **relative** path inside the checkout (for example `product-docs`). |
| `virtual.remoteUrl` | No | `https://git.example.com/org/product-docs.git` | Optional Git remote. When set, **Build** clones or fetches into a contained work directory, then reuses git-filesystem discover. Blank keeps local-path mode. Allowed: `https://`, `ssh://`, `file://`, or `git@host:path`. `http` and other schemes are rejected. |
| `virtual.branch` | No | `main` | Branch to checkout when `remoteUrl` is set. Default `main`. Simple ref name only (no `..` or leading `-`). |
| `virtual.configFile` | No | `_config.yaml` | Config file name under the root; default `_config.yaml`. Must be a simple file name (no path separators or `..`). |
| `virtual.siteKey` | No | `product-docs` | Participant registry key; default = Site name, else `default`. |

Empty / missing `virtual.sourceKind` (or value `repository`) means a traditional repository Site.

### Validation rules

- **Source kind allow-list** — only registered adapter wire names are accepted for Virtual Sites
  (`git-filesystem`, `csv-filesystem`). Values such as future `sql` / `api` kinds are rejected until
  implemented. `csv-filesystem` does not accept `virtual.remoteUrl` (Git remotes apply to
  `git-filesystem` only).
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
parse cache).

CLI (optional `_config.yaml`):

```text
PSVirtualSiteBuildMain <siteRoot> <outputRoot> [siteKey] csv-filesystem
```

REST `PUT /sites/{nameOrId}/virtual` may store `sourceKind=csv-filesystem` (allow-listed).
Developer Sites can select **CSV filesystem** and save a root path. **Build Virtual Site**
(`POST …/virtual/build`) discovers CSV rows under `virtual.rootPath` and writes HTML
(`pagesWritten` in the JSON result). Developer **Sites** shows the same **Build Virtual Site**
control for **CSV filesystem** (and Git filesystem). Unknown kinds remain **400**.
Publish to the Site filesystem for CSV is a later slice.

## CMS-integrated build (REST and WebUI)

When a CMS Site has Virtual properties configured (`git-filesystem` or `csv-filesystem`), an
**Admin** can trigger the matching build path from the running server:

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
`virtual.remoteUrl` is not valid for CSV (HTTP 400). In-product `POST …/virtual/build` remains
`git-filesystem` only.

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

### Rebuild after git pull or a local edit (no CMS restart)

The Git/filesystem adapter always sees the **current** tree on the CMS host:

1. Update Markdown or frontmatter under `virtual.rootPath` (`git pull`, copy, or an editor),
   **or** change the remote branch and Build again so the server fetches.
2. Run **Build Virtual Site** again (UI, `POST …/virtual/build`, or `scripts/build-cms-docs.*`).
3. Preview or publish the new output.

You do **not** restart the CMS JVM for those file changes to appear. A restart is only needed
when you change server code, Site properties that were never saved, or the process itself.

After a successful build, **Preview assembled site** opens the last output home in a new tab
(`GET /sites/{nameOrId}/virtual/preview` for status; `GET …/virtual/preview/{relPath}` for the
assembled file stream). Preview is last-output based and works for **`git-filesystem` and
`csv-filesystem`** (not git-only). Missing output returns status `available=false` (HTTP 200)
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

1. Validates the Site is a Git-filesystem Virtual Site.
2. Selects the Site filesystem publish root (must be configured, safe after NIO normalize, and
   distinct from `virtual.rootPath`).
3. Runs the same build as `POST …/virtual/build`.
4. Copies assembled HTML/assets (not `_meta`) to the Site root using portable `java.nio.file.Path`.

The response includes `publishPath`, `buildOutputPath`, `pagesWritten`, `filesCopied`, and
link-problem fields. Failures return **400/403/404** with an operator-readable message (never a
silent no-op). See [Publishing](id:admin-publishing).

## What is not in Phase 1

- CMS UI editing of Virtual items as normal content types
- Automatic migration of the full legacy help site
- Publish assembled CSV output to the Site filesystem (`POST …/virtual/publish`)
- SQL/API adapters
- Fake classic content-list generators for virtual items

## Related

- [Site configuration reference](id:reference-site-config)
- [Build from source](id:developer-build-source)
- [REST API](id:developer-rest)
