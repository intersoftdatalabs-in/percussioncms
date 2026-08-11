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

## Virtual Sites (8.2)

A **Virtual Site** is a Site whose content originates **outside** the traditional repository
(for example Git/filesystem Markdown under `product-docs/`). Virtual items are discovered and
assembled without ingesting them as ordinary CMS content items.

Operators configure Virtual Sites through **Site properties** (not new database columns in Phase 1).
Authors of Virtual content use Git and Markdown tooling, not the classic page editor.

### Property keys operators set

| Property | Required | Example | Notes |
|----------|----------|---------|-------|
| `virtual.sourceKind` | Yes (for Virtual) | `git-filesystem` | Phase 1 allow-list: **`git-filesystem` only**. Blank or `repository` = traditional Site. |
| `virtual.rootPath` | Yes (when virtual) | absolute path to `product-docs` | Must be non-blank for Virtual Sites. Prefer absolute paths; use portable paths (Windows/Linux/macOS). Paths with `..` after normalize are rejected. |
| `virtual.configFile` | No | `_config.yaml` | Default `_config.yaml`. Simple file name under the root (no `..` or directory separators). |
| `virtual.siteKey` | No | `product-docs` | Optional participant key; defaults to the Site name. |

Invalid combinations (unknown source kind, missing root, unsafe path, config path traversal) are
rejected by server validation with clear error messages.

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
