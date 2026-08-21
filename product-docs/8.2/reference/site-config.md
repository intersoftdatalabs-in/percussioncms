---
id: reference-site-config
title: Site configuration
description: product-docs _config.yaml and Virtual Site properties
version: "8.2"
order: 62
tags: [reference, docs, virtual-sites]
---

# Site configuration

## `_config.yaml` (Virtual Site root)

Located at `product-docs/_config.yaml`.

```yaml
site:
  title: Percussion CMS Documentation
  url: https://percussioncmshelp.intsof.com

versions:
  - id: "8.2"
    label: "8.2 (current)"
    path: 8.2
    default: true

nav:
  - title: Getting Started
    id: getting-started
  - title: Administration
    id: admin
  - title: Developer
    id: developer
  - title: Reference
    id: reference

theme:
  layout: page.html
```

### Rules

- `versions[].path` is a directory under the site root.
- If `nav` is omitted, navigation is derived from folders + frontmatter `order`.
- `theme.layout` is relative to `_theme/` (default `page.html`).
- `nav[].id` values should match section landing page frontmatter `id`s.

## `_redirects.yaml` (optional)

Located at `product-docs/_redirects.yaml`, beside `_config.yaml`. The file is **optional**:
if it is missing, Virtual Site build continues and writes no redirect artifacts.

```yaml
redirects:
  - from: /8.2/getting-started/installation.html
    to: /8.2/getting-started/install.html
    status: 301
```

### Rules

- `redirects` is a list of mappings. Each entry requires non-blank `from` and `to`.
- `from` is a site-relative path (leading `/` optional). A trailing `/` emits `index.html`
  under that directory. Path traversal (`..`) and Windows/absolute segments are rejected.
- `to` must be a **relative** site path or a **same-site** `http`/`https` URL whose host
  matches `site.url` in `_config.yaml`. Protocol-relative (`//host/…`), other hosts,
  `javascript:`, `data:`, and userinfo are **open redirects** and fail the build.
- `status` is optional (default `301`). Allowed: `301`, `302`, `307`, `308`. Used in
  `redirects.json` for operators/CDNs; static HTML always uses a meta refresh.
- A `from` that would overwrite a page the build just assembled is rejected.

### Build output

When the file is present and valid, Phase 1 build writes:

- Static redirect HTML at each `from` path (meta refresh + canonical link + body link)
- `redirects.json` next to the assembled site (from/to/status map)

**Publish Virtual Site** copies those files with the rest of the site (`_meta` is still
skipped). See [Virtual Sites](id:developer-virtual-sites) and [Publishing](id:admin-publishing).

### Theme placeholders (`_theme/page.html`)

The assembler binds dollar-brace placeholders (HTML-first, ADR-002) when it applies the
layout. Write these tokens in `_theme` HTML only — the same syntax in Markdown bodies is
substituted (missing bindings become empty).

| Placeholder | Value |
|-------------|--------|
| <code>&#36;{siteTitle}</code> | Site title from `_config.yaml` |
| <code>&#36;{pageTitle}</code> | Page frontmatter `title` |
| <code>&#36;{description}</code> | Page frontmatter `description` (empty when omitted) |
| <code>&#36;{content}</code> | Assembled Markdown HTML. h2–h3 headings receive stable `id` attributes for fragment links. |
| <code>&#36;{nav}</code> | Site sidebar navigation HTML |
| <code>&#36;{toc}</code> | In-page table of contents from that page’s h2–h3 headings. Empty when the page has none (no leftover token). |
| <code>&#36;{versionLabel}</code> | Current version label |
| <code>&#36;{versionSwitcher}</code> | Version `<select>` HTML when the site has more than one version; otherwise empty |

<code>&#36;{toc}</code> is a `<nav class="vs-toc" aria-label="On this page">` list of links
to `#heading-id`. Heading ids are derived from heading text (lowercase, hyphenated). Existing
safe `id` values on headings are kept. Duplicate titles get a numeric suffix (`install`,
`install-1`). Built-in theme `product-docs/_theme/page.html` includes <code>&#36;{toc}</code>
above the article.

Custom layouts may omit <code>&#36;{toc}</code>; heading ids are still written into
<code>&#36;{content}</code>.

## CMS Site properties (Virtual)

When a Percussion Site is configured as virtual (Phase 1 — no new `RXSITES` columns):

| Property name | Required | Example | Meaning |
|---------------|----------|---------|---------|
| `virtual.sourceKind` | Yes (for Virtual) | `git-filesystem` or `csv-filesystem` | Adapter wire name. **Allow-list:** `git-filesystem`, `csv-filesystem`. Blank or `repository` ⇒ traditional repository Site. Unknown values rejected by `PSVirtualSiteHelper.validate`. `csv-filesystem` is the offline CSV source (required columns `id`, `title`, `body`); REST Build remains git-filesystem. Preview REST (`GET …/virtual/preview`) is last-output based for both kinds. |
| `virtual.rootPath` | Yes when remote is blank | absolute or install-relative path to tree | Local filesystem source root when `virtual.remoteUrl` is blank. NIO `Path` normalize; no empty path / remaining `..`. When a remote is set, optional relative path inside the checkout. |
| `virtual.remoteUrl` | No | `https://git.example.com/org/product-docs.git` | Optional Git remote. Build clones/fetches into `{install}/tmp/virtual-site-checkouts/{siteKey}`. Allowed: `https://`, `ssh://`, `file://`, `git@host:path`. Fail-closed on `..`, `http`, option injection. Credentials are never logged. |
| `virtual.branch` | No | `main` | Branch to checkout when `remoteUrl` is set. Default `main`. |
| `virtual.configFile` | No | `_config.yaml` | Optional; default `_config.yaml`. Simple file name only (no separators / `..`). |
| `virtual.siteKey` | No | `product-docs` | Participant registry key; default Site name, else `default`. |

Empty / missing `virtual.sourceKind` means traditional **repository** site.

## CMS Site properties (traditional navigation)

Traditional (repository) sites may store:

| Property name | Required | Example | Meaning |
|---------------|----------|---------|---------|
| `navigation.managed` | No | `false` | When `false`, the site is created and treated as **without** a CMS NavTree/homepage. Absent or `true` is the default (include managed navigation). **Not used for Virtual Sites** — omit this flag when `virtual.sourceKind` is set; Virtual nav comes from the Git/Markdown tree. |

Create Site (`POST /Rhythmyx/sitemanage/site/`) accepts `managedNavigation` on the `Site` body (`true` default). **Virtual** create from the type picker sends `managedNavigation: false` and does not send `pageBased`. When a Git root is supplied, the wizard then `PUT`s `/sites/{nameOrId}/virtual` with the `VirtualSiteProperties` envelope. Public Site detail (`GET /sites/{nameOrId}`) returns `managedNavigation` for traditional sites only (`null`/omitted when Virtual).

Cross-platform notes: prefer absolute paths (`C:\…` on Windows, `/opt/…` on Linux/macOS). Operators
should not hardcode OS path separators in scripts — use the repo `scripts/build-cms-docs.*` wrappers
or NIO/`Path` APIs.


### REST

Integrators can read and write these keys via public Site REST:

- `GET /sites/{nameOrId}/virtual`
- `PUT /sites/{nameOrId}/virtual` (JSON body **must** use the `VirtualSiteProperties` root wrap — not a bare `sourceKind` object)
- `POST /sites/{nameOrId}/virtual/build` (optional JSON body: `outputRoot`)
- `GET /sites/{nameOrId}/virtual/preview` (last-build preview status)
- `GET /sites/{nameOrId}/virtual/preview/{relPath}` (assembled file stream)
- `POST /sites/{nameOrId}/virtual/publish` (build then copy to Site filesystem root)

PUT JSON (Jackson/JAXB) must wrap fields under the DTO root name:

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

A flat `{ "sourceKind": "git-filesystem", … }` body is **400** (`unexpected element sourceKind`).
GET uses the same envelope. The Developer Sites **Save Virtual Site source** action sends this
wrap and then GET-roundtrips so Build chrome appears without a full reload.

Site detail (`GET /sites/{nameOrId}`) also returns a nested `virtual` object. Validation is
enforced server-side (allow-listed source kinds, required local root path when virtual and
remote is blank, safe remote URL/branch, portable path safety).

#### Build Virtual Site (`POST …/virtual/build`)

Runs the Phase 1 static build for a Site configured with `virtual.sourceKind=git-filesystem`.
When `virtual.remoteUrl` is set, the server clones or fetches that branch into a contained
work directory, then discovers Markdown from the checkout (optional relative `virtual.rootPath`).
When remote is blank, `virtual.rootPath` must be an existing directory on the CMS host.
Requires **Admin**. `git` must be on the CMS `PATH` for remotes.

| Status | When |
|--------|------|
| `200` | Build finished; response includes `pagesWritten`, `linkProblemCount`, `linkProblems`, `outputPath`, `hasLinkProblems` |
| `400` | Traditional repository Site, unknown/invalid virtual config, missing root directory, or unsafe `outputRoot` |
| `403` | Caller is not Admin |
| `404` | Site not found |

Optional body field `outputRoot` overrides the default output directory
(`{install}/tmp/virtual-sites/{siteKey}`, or the JVM temp tree when the install root is
unavailable). Link problems are reported in the JSON result (and in `link-report.txt` under
the output root) without failing the HTTP status when the build itself succeeds.

Each build re-reads current Markdown/frontmatter from `virtual.rootPath` (no CMS restart after
`git pull` or a local edit). The Developer Sites UI exposes this operation as **Build Virtual
Site** when source kind is Virtual (never for traditional repository Sites). When
`hasLinkProblems` is true, the result panel shows the problem **count** and an expandable list
of `linkProblems` (same text as `link-report.txt`). A clean build does not show that banner.
See [Sites & content structure](id:admin-sites).

#### Preview assembled Virtual Site (`GET …/virtual/preview`)

Admin-only. Reports whether the last build output can be opened (`available`, `homePath`,
`outputPath`). Preview is **last-output based** and works for both **`git-filesystem`** and
**`csv-filesystem`** Virtual Sites (not git-only). Missing or failed builds return **200**
with `available=false` and a message (not 500). Traditional repository Sites and unknown
`virtual.sourceKind` values return **400**.

`GET …/virtual/preview/{relPath}` streams one file from that last output root (portable NIO
resolution, no `..` after normalize). HTML root-relative `href`/`src` values are rewritten to
the preview prefix so the assembled site is navigable in the browser. Missing files return
**404**. Path traversal (`../`) is rejected with **400**. The Developer UI **Preview assembled
site** control uses these endpoints. After a successful `csv-filesystem` assemble (CLI
`PSVirtualSiteBuildMain … csv-filesystem`, or REST Build when that kind is enabled), the same
preview URLs stream the last CSV-assembled HTML.

#### Publish Virtual Site (`POST …/virtual/publish`)

Runs the same build, then copies the assembled tree to the Site **filesystem publish location**
(`IPSSite.root`). Requires **Admin**. Staging `_meta` is not copied.

| Status | When |
|--------|------|
| `200` | Published; response includes `publishPath`, `filesCopied`, `pagesWritten`, `hasLinkProblems` |
| `400` | Not a Virtual Site, Site root missing/unsafe/not a directory, or overlap with `virtual.rootPath` / build output |
| `403` | Caller is not Admin |
| `404` | Site not found |

Configure a dedicated Site root (not the Markdown source path). Operators can run the same
action from **Developer → Sites → Site detail → Publish Virtual Site** (Admin; hidden for
repository Sites). See [Publishing](id:admin-publishing).

## Related

- [Frontmatter contract](id:reference-frontmatter)
- [Virtual Sites](id:developer-virtual-sites)
