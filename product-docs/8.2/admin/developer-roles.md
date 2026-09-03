---
id: admin-developer-roles
title: Developer Roles
description: Browse CMS security roles grouped by community, workflow, or unassigned
version: "8.2"
order: 46
tags: [admin, developer, roles, security]
---

# Developer Roles

**Developer → Roles** is a **read-only** Security Design catalog of system roles.
It mirrors the classic Workbench **Security Design → Roles** navigator folders:

| Group | Meaning |
|-------|---------|
| **Community** | Role is assigned to at least one community |
| **Workflow** | Role is assigned to at least one workflow |
| **Unassigned** | Role is in neither community nor workflow membership |

A role that is both community- and workflow-assigned appears under **both** groups.
This chrome does **not** create, delete, or edit role membership — use
**Admin → Roles** for user membership and **Developer → Communities** detail for
community role association.

## Product path — browse

1. Sign in as **Admin** (the browse catalog requires the Admin role).
2. Open **Developer → Roles**, or deep-link
   `spa.jsp?entry=developer&section=roles` (aliases: `role`, `se03`).
3. The panel loads the full catalog and shows three expandable groups with
   role counts. Expand or collapse a group to show or hide its table.
4. Use the **All groups** / **Community** / **Workflow** / **Unassigned** filters
   to focus on one navigator folder.
5. Each row shows the role name, description (when known), communities that
   include the role, and workflows that include the role.

Non-Admin sessions receive **403** from the catalog API and the panel shows an
error. An empty catalog is a valid **200** with no rows.

## Limits

- Read-only browse only. Membership CRUD remains on **Admin → Roles** and
  community **Save roles**.
- LocalContent (internal) workflow assignments are excluded from the workflow
  column, matching Workbench.
- Session community switch in the header is a separate membership list; it is
  not this catalog.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| Full catalog | `GET /services/roles/catalog` |
| Filtered | `GET /services/roles/catalog?group=community\|workflow\|unassigned` |

Integrator notes: [REST API](id:developer-rest) (Roles browse catalog). Related
chrome: [Developer Communities](id:admin-developer-communities),
[Users, roles & security](id:admin-users-roles).
