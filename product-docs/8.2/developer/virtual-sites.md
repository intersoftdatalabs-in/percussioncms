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

## Stable identity

Every Markdown page requires YAML frontmatter with a unique **`id`** within the version.
Paths may change; `id` should not. Cross-page links use stable id links:

```markdown
See [Installation](id:install-overview).
```

Details: [Frontmatter reference](id:reference-frontmatter).

## Site properties (CMS)

When a Percussion Site is configured as virtual:

| Property | Example | Meaning |
|----------|---------|---------|
| `virtual.sourceKind` | `git-filesystem` | Non-blank ⇒ Virtual Site |
| `virtual.rootPath` | path to tree | Source root |
| `virtual.configFile` | `_config.yaml` | Optional; default `_config.yaml` |
| `virtual.siteKey` | `product-docs` | Optional participant key; default = site name |

Empty / missing `virtual.sourceKind` means a traditional repository Site.

### Public REST (integrators)

Virtual Site properties are exposed on the public Site REST API (no Workbench/SOAP required):

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/sites/{nameOrId}` | Site detail; includes nested `virtual` object when loaded |
| `GET` | `/sites/{nameOrId}/virtual` | Read `sourceKind`, `rootPath`, `configFile`, `siteKey` |
| `PUT` | `/sites/{nameOrId}/virtual` | Create/update virtual properties (validated) |

`nameOrId` is the Site name or GUID string. Validation matches the server helper
(`PSVirtualSiteHelper`): Phase 1 allow-listed `sourceKind` (`git-filesystem`), required
`rootPath` when virtual, safe NIO path (no `..` after normalize), and simple `configFile`
names only. Set `sourceKind` to blank or `repository` to clear virtual configuration.

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

## What is not in Phase 1

- CMS UI editing of Virtual items as normal content types
- Automatic migration of the full legacy help site
- SQL/API adapters
- Fake classic content-list generators for virtual items

## Related

- [Site configuration reference](id:reference-site-config)
- [Build from source](id:developer-build-source)
