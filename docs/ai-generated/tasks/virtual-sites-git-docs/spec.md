# Specification: Virtual Sites & Git-Backed Documentation (8.2)

## 1. Purpose

Introduce **Virtual Sites** — Sites whose content originates outside the traditional Percussion content repository — and deliver the first concrete implementation for Git/Filesystem-backed documentation.

### Goals

- Keep Git as the source of truth for documentation (developer lockstep, PR review, continuous updates).
- Use Percussion’s assembler and publishing machinery as the site generator (dogfooding).
- Enable lightweight relationship/link integrity for documentation pages.
- Establish a general external-content-source pattern for later adapters (databases, APIs, other CMSes).
- Avoid ingesting documentation into the normal content repository as editable CMS items.

This work builds on assembler modernization [#2628](https://github.com/intersoftdatalabs-in/percussioncms/issues/2628) (HTML-first + Markdown assemblers).

## 2. Historical context: what a “Site” is

Historically in Percussion (Rhythmyx → CM1 → current):

- A **Site** defines a location where content is published (filesystem, FTP, database, etc.).
- It is also the organizational unit: the Site Folder tree in the Finder drives structure, navigation, and publishing.
- Site Properties include hostname, permissions, publishing configuration, etc.
- Managed Navigation and publishing are Site-centric.

**Decision:** Retain the term **Site**. Do not rename the core concept to “Channel.” External content becomes a **source type** on a Site.

## 3. Core concept: Virtual Site

A Virtual Site is a Site whose content does not live in the traditional Percussion content repository. The CMS reads content from an external source at discovery/assembly time (or via controlled sync).

### 3.1 Source types (extensible)

| Source type | Priority | Notes |
|-------------|----------|-------|
| Git / Filesystem | Phase 1 | Primary target (documentation) |
| SQL / Database | Future | Structured reference data, catalogs |
| API / Headless CMS | Future | Syndication |
| CSV / Flat files | Future | Simple structured lists |
| Object storage | Future | Large binary sets |

### 3.2 Responsibilities

- Declare source type + connection details.
- Discover items from the external source.
- Determine stable identity for each item.
- Project items into a form the assembler can consume (transient or lightly cached).
- Optionally register items with the relationship / virtual participant engine.
- Participate in normal Site-level publishing configuration where applicable.

Content remains owned by the external source.

## 4. Traditional Site vs Virtual Site

| Aspect | Traditional Site | Virtual Site |
|--------|------------------|--------------|
| Content origin | Percussion content repository | External (Git, DB, API, …) |
| Editing | CMS UI, workflow, permissions | External tools (Git, SQL client, etc.) |
| Folder / page tree | First-class CMS folders & pages | Projected from external structure |
| Identity | Content IDs | Source-defined stable ID (e.g. frontmatter `id`) |
| Relationship participation | Full | Optional / lightweight |
| Assembly & publishing | Yes | Yes (virtual publish path in Phase 1) |
| Site Properties / pub config | Yes | Yes (where applicable) |

Both appear as Sites. A Site has a source kind: **repository** (default) or a **Virtual Site** adapter.

## 5. Phase 1 scope: Git/Filesystem Virtual Site for documentation

### In scope

- Git or filesystem directory of Markdown files as a Virtual Site source.
- Navigable static documentation site via the assembler helpers.
- Stable page identities for basic relationship / broken-link support.
- Continuous documentation during 8.2 development.

### Out of scope

- Full CMS content-type mapping or UI editing of Virtual Site items.
- Automatic migration of existing help.intsof.com content.
- Additional source adapters (SQL, API, etc.).
- Advanced redirect management or full link rewriting.
- Fake content IDs through classic `IPSContentListGenerator` (see ADR-003).

## 6. Documentation tree contract

Root: **`product-docs/`** (repo root — isolated from internal `docs/`).

```text
product-docs/
├── _config.yaml
├── _redirects.yaml          # optional
├── _theme/                  # layout templates + CSS
├── assets/
│   └── ...
└── 8.2/
    ├── index.md
    ├── getting-started/
    │   ├── index.md
    │   ├── install.md
    │   └── upgrade.md
    ├── admin/
    ├── developer/
    └── reference/
```

- Folder structure is the primary navigation hierarchy.
- `index.md` is the landing page for a section.
- One top-level folder per major documentation version.

## 7. Frontmatter contract

```yaml
---
id: install-overview              # stable identity (required for relationships)
title: Installation Overview
description: How to install Percussion CMS 8.2
version: "8.2"                    # may be inherited from folder
sidebar: true                     # default true
order: 10                         # sort within parent
tags: [install, admin]
deprecated: false
---
```

`id` is mandatory for relationship participation. Paths may change; `id` should remain stable.

## 8. Site configuration (`_config.yaml`)

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

## 9. Assembler responsibilities (Phase 1)

Building on #2628 Markdown / HTML-first assemblers:

- Accept a filesystem or Git checkout of the docs tree as a Virtual Site source.
- Discover and parse Markdown + frontmatter.
- Build navigation from folder structure + order + optional nav config.
- Apply templates (layout, sidebar, version switcher, TOC, etc.).
- Emit static site output (HTML + assets).
- Register each page’s stable id and final published path with the virtual participant registry.
- Publish to the configured filesystem target.

## 10. Relationship engine participation

- On assembly, upsert a virtual participant keyed by frontmatter `id`.
- Record the published path/URL.
- Support detection of missing targets (broken links) at build or report time.
- Future: managed links that use stable ids, move/rename resilience, impact analysis.

Virtual Site items are a **thin projection**, not full first-class content items. Phase 1 does **not** use `PSX_MANAGEDLINK` (content-id based).

## 11. Naming & language

- Primary term remains **Site**.
- External-origin Sites are **Virtual Sites**.
- UI/docs language: “A Site may be backed by the content repository or by an external source (Virtual Site).”
- Do **not** introduce “Channel” as a replacement for Site.

## 12. Success criteria (Phase 1)

1. A Git-backed documentation tree can be configured as a Virtual Site source.
2. The assembler produces a navigable static documentation site from that tree.
3. Pages carry stable ids visible to the virtual participant registry.
4. Documentation can be updated via normal Git workflows in lockstep with product changes.
5. The design leaves a clear extension point for additional Virtual Site source types.
