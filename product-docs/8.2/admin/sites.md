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

### Browse Sites in Content Explorer

1. Sign in as an administrator (or a role that can open **Explorer**).
2. Open **Content Explorer** (`spa.jsp?entry=explorer` or the **Explorer** product navigation entry).
3. In the tree, expand **Sites**.
4. Select a site folder to browse its pages and folders in the detail list.

After a standard install with **sample sites** (installer **Install sample sites** / silent `--demo-sites`), the Sites tree typically includes stock demo sites such as **Corporate Investments** and **Enterprise Investments**. Fresh evaluation or H2 QA stacks without sample seed may show an empty Sites list until you create a site or reinstall with sample data.

### Create a traditional Site from Explorer

Use **Content Explorer** when you need a new traditional (repository) Site without leaving the product shell:

1. Open **Content Explorer**.
2. Choose **Content → Create Site**.
3. On **Details**, enter a unique **Site name**, optional description, and the site **template name** (defaults from the site name).
4. On **Base template**, pick a base template from the catalog (or accept the default when the catalog is empty).
5. On **Confirm**, review the summary (repository kind is **Traditional** — this flow does not create Virtual Sites).
6. Choose **Create** and wait for progress to complete. Explorer opens the new site under `/Sites/<name>`.

Virtual Site source settings (Git/filesystem) are configured later on the Site properties / Developer Sites surface — not in this Create Site wizard. See [Virtual Sites (developer)](id:developer-virtual-sites) and the Virtual Sites section below.

## Virtual Sites (8.2)

A **Virtual Site** is a Site whose content originates **outside** the traditional repository
(for example Git/filesystem Markdown under `product-docs/`). Virtual items are discovered and
assembled without ingesting them as ordinary CMS content items.

Operators configure Virtual Sites through **Site properties** (not new database columns in Phase 1).
Integrators can set the same properties over public Site REST
(`GET`/`PUT /sites/{nameOrId}/virtual`) without Workbench. Authors of Virtual content use Git
and Markdown tooling, not the classic page editor.

### Property keys operators set

| Property | Required | Example | Notes |
|----------|----------|---------|-------|
| `virtual.sourceKind` | Yes (for Virtual) | `git-filesystem` | Phase 1 allow-list: **`git-filesystem` only**. Blank or `repository` = traditional Site. |
| `virtual.rootPath` | Yes (when virtual) | absolute path to `product-docs` | Must be non-blank for Virtual Sites. Prefer absolute paths; use portable paths (Windows/Linux/macOS). Paths with `..` after normalize are rejected. |
| `virtual.configFile` | No | `_config.yaml` | Default `_config.yaml`. Simple file name under the root (no `..` or directory separators). |
| `virtual.siteKey` | No | `product-docs` | Optional participant key; defaults to the Site name. |

Invalid combinations (unknown source kind, missing root, unsafe path, config path traversal) are
rejected by server validation with clear error messages.

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
