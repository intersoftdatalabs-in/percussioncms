---
id: admin-developer-roles
title: Developer Roles
description: Browse CMS security roles, create a role, and update one role's description
version: "8.2"
order: 46
tags: [admin, developer, roles, security]
---

# Developer Roles

**Developer → Roles** is the Security Design catalog of system roles.
It mirrors the classic Workbench **Security Design → Roles** navigator folders:

| Group | Meaning |
|-------|---------|
| **Community** | Role is assigned to at least one community |
| **Workflow** | Role is assigned to at least one workflow |
| **Unassigned** | Role is in neither community nor workflow membership |

A role that is both community- and workflow-assigned appears under **both** groups.
An Admin can **create** one role (name and description) and **edit the description**
of one existing role from this catalog.
This chrome does **not** delete a role or change membership —
use **Admin → Roles** for user membership and **Developer → Communities** detail for
community role association. Packaged roles such as Admin and Designer are not
created by this form. If the server rejects a description change (including a
restricted system role), the catalog keeps the previous description.

## Product path — browse

1. Sign in as **Admin** (the browse catalog requires the Admin role).
2. Open **Developer → Roles**, or deep-link
   `spa.jsp?entry=developer&section=roles` (aliases: `role`, `se03`).
3. The panel loads the full catalog and shows three expandable groups with
   role counts. Expand or collapse a group to show or hide its table.
4. Use the **All groups** / **Community** / **Workflow** / **Unassigned** filters
   to focus on one navigator folder.
5. Each row shows the role name, description (when known), communities that
   include the role, and workflows that include the role. Select a row to edit
   that role's description.

Non-Admin sessions receive **403** from the catalog API and the panel shows an
error. An empty catalog is a valid **200** with no rows.

## Product path — create a role

1. Sign in as **Admin**.
2. Open **Developer → Roles**.
3. Choose **Create role**.
4. Enter a role name. Description is optional. **Cancel** closes the form and
   does not call the server. A blank or whitespace-only name stays on the form;
   **Create role** stays disabled and no request is sent.
5. Choose **Create role** on the form. The catalog does not list the new role
   until the server returns success, then the catalog reloads.
6. The existing role service also adds the new role to workflows, so the row
   appears under **Workflow** (and under **Community** only if a community
   already includes that name). Delete and member changes are not part of this
   form.

HTTP **400** (blank, invalid, or duplicate name) and **403** (not Admin) leave
the form in an error state. The catalog does not show the role as created.

## Product path — update a description

1. Sign in as **Admin**.
2. Open **Developer → Roles** and wait for the catalog.
3. Select the row of an existing role. The description form shows that role's
   name (read-only) and its current description.
4. **Cancel** closes the form and does not call the server. The row keeps the
   previous description.
5. Change the description and choose **Save description**. The row does not
   show the new text until the server returns success and the catalog reloads.
   A blank description clears the stored description.
6. The save does not rename the role, change its home page, or change its
   members.

HTTP **400** (description longer than 255 characters, or the role service
rejects the update), **403** (not Admin), and **404** (the role no longer
exists) leave the form in an error state. The catalog row keeps the previous
description. `update=true` does not create a missing role.

## Limits

- Create is name and description only. Description save changes the description
  only. Membership CRUD remains on **Admin → Roles** and community **Save roles**.
- This form does not change the home page or delete a role.
- A duplicate name is rejected. It does not update the existing role.
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
| Create | `PUT /services/roles/?create=true` with a `Role` object: `name` (required) and optional `description` |
| Update description | `PUT /services/roles/?update=true` with a `Role` object: `name` (required) and `description` (blank clears) |

Create always uses the role create service (`create=true`). It does not update
an existing role. A blank name is **400**. A caller who is not Admin is **403**.
A duplicate or otherwise invalid name is **400**. `update=true` changes the
description of an existing role and does not create a missing name (**404**).
It does not change members or the role name. A description longer than 255
characters is **400**. Without `create=true` or `update=true`, `PUT` still
creates a role that is not already defined and updates the description of a
role that is. Do not send both `create=true` and `update=true` (**400**).

Integrator notes: [REST API](id:developer-rest) (Roles browse catalog). Related
chrome: [Developer Communities](id:admin-developer-communities),
[Users, roles & security](id:admin-users-roles).
