---
id: admin-architecture-navigation
title: Architecture & site navigation
description: Modern Architecture SPA shell for managing site navigation trees (migration in progress)
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

## Current status (migration)

| Capability | Status |
|------------|--------|
| SPA route + top-nav entry under product chrome | **Available** |
| Role gate (Admin / Designer) | **Available** |
| Site navigation tree browse / edit (navons) | **Coming soon** (follow-on slices) |
| Landing page / section-link parity | **Coming soon** |
| Legacy `siteArchitecture.jsp` retirement | **Planned** after SPA parity |

Until the navigation tree editor ships, the Architecture shell shows an in-progress
message. Operators should use this SPA entry as the primary path; the legacy JSP is
no longer the top-nav destination.

## Related

- [Sites & content structure](id:admin-sites)
- [Content Explorer](id:admin-content-explorer)
- [Users, roles & security](id:admin-users-roles) (default landing options include Architecture)
