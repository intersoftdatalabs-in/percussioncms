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
repository. Phase 1 delivers a **Git / filesystem** adapter aimed at product documentation.

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
| `virtual.sourceKind` | Yes (for Virtual) | `git-filesystem` | Adapter wire name. **Allow-list (Phase 1):** `git-filesystem` only. Blank or `repository` ⇒ traditional repository Site. Unknown values are rejected. |
| `virtual.rootPath` | Yes (when virtual) | absolute path to `product-docs` (or install-relative) | Filesystem root of the Virtual Site tree. Non-blank required when virtual. Resolved with NIO `Path` (cross-platform). |
| `virtual.configFile` | No | `_config.yaml` | Config file name under the root; default `_config.yaml`. Must be a simple file name (no path separators or `..`). |
| `virtual.siteKey` | No | `product-docs` | Participant registry key; default = Site name, else `default`. |

Empty / missing `virtual.sourceKind` (or value `repository`) means a traditional repository Site.

### Validation rules

- **Source kind allow-list** — only registered adapter wire names are accepted for Virtual Sites
  (Phase 1: `git-filesystem`). Values such as future `sql` / `api` kinds are rejected until
  implemented.
- **Required root** — when `virtual.sourceKind` is virtual, `virtual.rootPath` must be non-blank.
- **Safe paths** — `virtual.rootPath` is normalized with `java.nio.file.Path`. After normalize, empty
  paths and any remaining `..` segments are rejected (path traversal). Prefer absolute paths on
  Windows (`C:\…`) and Unix (`/opt/…`); relative paths under the install are allowed when they do not
  escape via `..`.
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

## CMS-integrated build (REST and WebUI)

When a CMS Site has Virtual properties configured, an **Admin** can trigger the same build path
from the running server:

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

### Rebuild after git pull or a local edit (no CMS restart)

The Git/filesystem adapter always sees the **current** tree on the CMS host:

1. Update Markdown or frontmatter under `virtual.rootPath` (`git pull`, copy, or an editor).
2. Run **Build Virtual Site** again (UI, `POST …/virtual/build`, or `scripts/build-cms-docs.*`).
3. Preview or publish the new output.

You do **not** restart the CMS JVM for those file changes to appear. A restart is only needed
when you change server code, Site properties that were never saved, or the process itself.

After a successful build, **Preview assembled site** opens the last output home in a new tab
(`GET /sites/{nameOrId}/virtual/preview` for status; `GET …/virtual/preview/{relPath}` for the
assembled file stream). Missing output returns status `available=false` (HTTP 200) or file HTTP
404 — not 500. See [Sites & content structure](id:admin-sites) and
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
- SQL/API adapters
- Fake classic content-list generators for virtual items

## Related

- [Site configuration reference](id:reference-site-config)
- [Build from source](id:developer-build-source)
- [REST API](id:developer-rest)
