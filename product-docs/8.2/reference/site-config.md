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

## Related

- [Frontmatter contract](id:reference-frontmatter)
- [Virtual Sites](id:developer-virtual-sites)
