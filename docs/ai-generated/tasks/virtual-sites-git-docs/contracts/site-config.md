# Site configuration contract (`_config.yaml`)

Located at the Virtual Site root (`product-docs/_config.yaml`).

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

theme:
  layout: page.html
```

## Rules

- `versions[].path` is a directory under the site root.
- If `nav` is omitted, navigation is derived from folders + frontmatter `order`.
- `theme.layout` is relative to `_theme/` (default `page.html`).

## Site properties (CMS Site object)

When a Percussion Site is configured as virtual (Phase 1 property contract):

| Property name | Example | Meaning |
|---------------|---------|---------|
| `virtual.sourceKind` | `git-filesystem` | Non-blank ⇒ Virtual Site |
| `virtual.rootPath` | absolute or install-relative path to tree | Source root |
| `virtual.configFile` | `_config.yaml` | Optional; default `_config.yaml` |

Empty / missing `virtual.sourceKind` means traditional **repository** site.
