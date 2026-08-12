---
id: admin-architecture-navigation
title: Architecture & site navigation
description: Modern Architecture SPA for browsing and editing site navigation trees (navons / sections)
version: "8.2"
order: 43
tags: [admin, architecture, navigation, ui]
---

# Architecture & site navigation

**Architecture** (site navigation / navon tree editor) is migrating into the modern SPA
product chrome. In Percussion CMS 8.2 the primary entry is the SPA shell — not the
legacy CM1 Architecture page.

## Open Architecture (SPA)

1. Sign in as an **Admin** or **Designer**.
2. Choose **Architecture** in the product top navigation, or open the SPA entry:
   - Query contract: `spa.jsp?entry=architecture`
   - Path route: `/cm/app/architecture` (optional site segment or `?site=` for context)
3. The shell loads under the same product top nav as Explorer, Design, Publish, and Admin.

Default landing can also be set to **Architecture** for a user or role (homepage type
`Architecture`); login then resolves to the SPA Architecture entry.

## Browse a site navigation tree

1. Open **Architecture**.
2. Choose a site from the **Site** list (or open a deep link with `?site=YourSiteName`
   or `/architecture/YourSiteName`).
3. The **Navigation tree** panel loads the site’s sections (navons) from the server.
4. Expand and collapse nodes with the mouse or keyboard (Enter/Space, Arrow Left/Right).
5. Use **Refresh** to reload the tree after external changes.

Empty, loading, and error states are shown explicitly when the site list or tree
cannot be loaded, or when a site has no sections.

## Edit navigation structure

With a site selected, use the structure action bar above the tree:

| Action | Behavior |
|--------|----------|
| **Create section** | Opens a dialog to add a regular section (title, URL name, template) under the selected section, or under the site root when nothing is selected. Requires a site template. |
| **Rename** | Renames the selected regular section (updates section title / landing link title). |
| **Move up / Move down** | Reorders the selected section among its siblings under the same parent. |
| **Delete** | Deletes the selected non-root section after confirmation. Section links use the section-link delete path. |

Server errors from create, rename, move, or delete are shown in the panel (no silent
failure). The tree reloads after a successful mutation.

**Not in this surface yet:** landing-page assignment, section-link / external-link
create and edit dialogs, convert folder, and full section security preferences.
Those follow in a later Architecture slice.

## Current status (migration)

| Capability | Status |
|------------|--------|
| SPA route + top-nav entry under product chrome | **Available** |
| Role gate (Admin / Designer) | **Available** |
| Site picker | **Available** |
| Site navigation tree browse (navons / sections) | **Available** |
| Structure editing (create / rename / reorder / delete) | **Available** |
| Landing page / section-link parity | **Coming soon** |
| Legacy `siteArchitecture.jsp` retirement | **Planned** after SPA parity |

## Related

- [Sites & content structure](id:admin-sites)
- [Content Explorer](id:admin-content-explorer)
- [Users, roles & security](id:admin-users-roles) (default landing options include Architecture)
