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

When a Percussion Site is configured as virtual:

| Property name | Example | Meaning |
|---------------|---------|---------|
| `virtual.sourceKind` | `git-filesystem` | Non-blank ⇒ Virtual Site |
| `virtual.rootPath` | absolute or install-relative path to tree | Source root |
| `virtual.configFile` | `_config.yaml` | Optional; default `_config.yaml` |
| `virtual.siteKey` | `product-docs` | Optional participant key; default = site name |

Empty / missing `virtual.sourceKind` means traditional **repository** site.

### REST

Integrators can read and write these keys via public Site REST:

- `GET /sites/{nameOrId}/virtual`
- `PUT /sites/{nameOrId}/virtual` (JSON body: `sourceKind`, `rootPath`, `configFile`, `siteKey`)

Site detail (`GET /sites/{nameOrId}`) also returns a nested `virtual` object. Validation is
enforced server-side (allow-listed source kinds, required root path when virtual, portable
path safety).

## Related

- [Frontmatter contract](id:reference-frontmatter)
- [Virtual Sites](id:developer-virtual-sites)
