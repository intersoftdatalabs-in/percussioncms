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

## CMS Site properties (Virtual)

When a Percussion Site is configured as virtual (Phase 1 — no new `RXSITES` columns):

| Property name | Required | Example | Meaning |
|---------------|----------|---------|---------|
| `virtual.sourceKind` | Yes (for Virtual) | `git-filesystem` | Adapter wire name. **Allow-list (Phase 1):** `git-filesystem`. Blank or `repository` ⇒ traditional repository Site. Unknown values rejected by `PSVirtualSiteHelper.validate`. |
| `virtual.rootPath` | Yes (when virtual) | absolute or install-relative path to tree | Filesystem source root. Non-blank when virtual. NIO `Path` normalize; no empty path / remaining `..` segments. |
| `virtual.configFile` | No | `_config.yaml` | Optional; default `_config.yaml`. Simple file name only (no separators / `..`). |
| `virtual.siteKey` | No | `product-docs` | Participant registry key; default Site name, else `default`. |

Empty / missing `virtual.sourceKind` means traditional **repository** site.

Cross-platform notes: prefer absolute paths (`C:\…` on Windows, `/opt/…` on Linux/macOS). Operators
should not hardcode OS path separators in scripts — use the repo `scripts/build-cms-docs.*` wrappers
or NIO/`Path` APIs.


### REST

Integrators can read and write these keys via public Site REST:

- `GET /sites/{nameOrId}/virtual`
- `PUT /sites/{nameOrId}/virtual` (JSON body: `sourceKind`, `rootPath`, `configFile`, `siteKey`)
- `POST /sites/{nameOrId}/virtual/build` (optional JSON body: `outputRoot`)

Site detail (`GET /sites/{nameOrId}`) also returns a nested `virtual` object. Validation is
enforced server-side (allow-listed source kinds, required root path when virtual, portable
path safety).

#### Build Virtual Site (`POST …/virtual/build`)

Runs the Phase 1 static build for a Site configured with `virtual.sourceKind=git-filesystem`
and a valid `virtual.rootPath` (directory must exist on the CMS host). Requires **Admin**.

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

The Developer Sites UI exposes this operation as **Build Virtual Site** when source kind is
Virtual (never for traditional repository Sites). When `hasLinkProblems` is true, the result
panel shows the problem **count** and an expandable list of `linkProblems` (same text as
`link-report.txt`). A clean build does not show that banner. See
[Sites & content structure](id:admin-sites).

## Related

- [Frontmatter contract](id:reference-frontmatter)
- [Virtual Sites](id:developer-virtual-sites)
