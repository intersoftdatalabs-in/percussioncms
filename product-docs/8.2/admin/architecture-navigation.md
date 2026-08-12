---
id: admin-architecture-navigation
title: Architecture & site navigation
description: Modern Architecture SPA for browsing site navigation trees (navons); editing in follow-on slices
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

## Browse a site navigation tree (read-only)

1. Open **Architecture**.
2. Choose a site from the **Site** list (or open a deep link with `?site=YourSiteName`
   or `/architecture/YourSiteName`).
3. The **Navigation tree** panel loads the site’s sections (navons) from the server.
4. Expand and collapse nodes with the mouse or keyboard (Enter/Space, Arrow Left/Right).
5. Use **Refresh** to reload the tree after external changes.

Empty, loading, and error states are shown explicitly when the site list or tree
cannot be loaded, or when a site has no sections.

**Note:** This release provides a **read-only** tree. Create, edit, reorder, and
delete of sections (and landing-page / section-link workflows) ship in follow-on
Architecture slices.

## Current status (migration)

| Capability | Status |
|------------|--------|
| SPA route + top-nav entry under product chrome | **Available** |
| Role gate (Admin / Designer) | **Available** |
| Site picker | **Available** |
| Site navigation tree browse (navons / sections) | **Available** (read-only) |
| Structure editing (create / edit / move / delete) | **Coming soon** |
| Landing page / section-link parity | **Coming soon** |
| Legacy `siteArchitecture.jsp` retirement | **Planned** after SPA parity |

## Related

- [Sites & content structure](id:admin-sites)
- [Content Explorer](id:admin-content-explorer)
- [Users, roles & security](id:admin-users-roles) (default landing options include Architecture)
