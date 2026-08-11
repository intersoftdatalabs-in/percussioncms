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

Operators configure Virtual Sites through Site properties (source kind, root path, config file,
optional site key). Integrators can set the same properties over public Site REST
(`GET`/`PUT /sites/{nameOrId}/virtual`) without Workbench. Authors of Virtual content use Git
and Markdown tooling, not the classic page editor.

### Configure Virtual Site source in the product UI

1. Sign in as an administrator (or a role that can open **Developer**).
2. Open **Developer** → **Sites** (SPA entry `spa.jsp?entry=developer&section=sites`).
3. Select a Site row to open **Site detail**.
4. In the **Virtual Site source** section:
   - **Source kind** — leave **Repository (traditional)** for ordinary CMS Sites
     (blank/`repository` on the server). Choose **Git filesystem** for Phase 1 Virtual Sites.
   - **Root path** — absolute or install-relative path to the documentation tree
     (required when source kind is Virtual). Do not use `..` path segments.
   - **Config file** (optional) — simple file name under the root (default `_config.yaml`
     when unset). No path separators.
   - **Site key** (optional) — participant registry key; defaults to the Site name when blank.
5. Choose **Save Virtual Site source**. Reload the Site detail to confirm values persisted.
6. To return a Virtual Site to traditional repository mode, set source kind back to
   **Repository (traditional)** and save (clears `virtual.*` properties).

Validation matches the server helper (`PSVirtualSiteHelper`): allow-listed source kinds,
required root path when virtual, and safe path/config names. After root or config changes,
re-run the offline docs build or the CMS publish path to verify links.

See [Virtual Sites (developer)](id:developer-virtual-sites) and
[Site configuration reference](id:reference-site-config).

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
