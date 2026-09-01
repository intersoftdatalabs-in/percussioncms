---
id: admin-developer-display-formats
title: Developer Display Formats
description: Create, delete, edit columns, and set allowed communities on Content Explorer display formats from Developer Display Formats chrome
version: "8.2"
order: 47
tags: [admin, developer, display-formats]
---

# Developer Display Formats

**Developer → Display Formats** lists Content Explorer display format definitions
(Workbench **Display Format** editor: unique internal name, label, description,
and column catalog). Admins can **create** a user display format, **delete** a
selected user format, **add**, **remove**, and **reorder columns**, and set
**allowed communities** on a **user** format from this chrome. The **name** is required, must be unique
(case-insensitive), and must not contain spaces, wildcards (`*` / `%`), or path
characters. Name cannot be renamed after create.

**Packaged/system** formats (`Default`, `By_Author`, `CM1_Default`, and the
other installer catalog names) stay **read-only** for columns and allowed
communities.

## Product path — create, delete

1. Sign in as **Admin** (write calls require the Admin role).
2. Open **Developer → Display Formats**, or deep-link
   `spa.jsp?entry=developer&section=display-formats`.
3. Click **New display format**. Enter a **name**. Save stays disabled until
   the name is valid (no spaces, no `*` / `%`, no `/` or `..`). Optional:
   label and description.
4. Click **Save**. A duplicate name is **409** and the editor shows that the
   display format already exists. An invalid name is **400**. A non-Admin
   session is **403**. After a successful create, the name field is read-only
   and the catalog lists the new format. `GET /services/displayformats/{name}`
   returns that user format (not **404**, and not a packaged format such as
   **By_Author**).
5. Optional: change label or description and **Save** again.
6. Click **Delete** and confirm. The catalog no longer lists that format.
   Delete of a missing format is **404**. A format still used as a dependent,
   or locked by another user, is **409**. Packaged system formats that REST
   rejects stay locked; the chrome surfaces that conflict and does not steal
   locks.

## Product path — edit columns on a user format

1. Open a **user** format (not a packaged/system name). Detail shows the
   column table plus **Add column**, move **up** / **down**, **Remove**, and
   **Save columns**.
2. Choose a field that is not already a column and click **Add column**.
   `sys_title` cannot be removed (the server always keeps it).
3. Click **Save columns**. After a successful save, a following
   `GET /services/displayformats/{name}` lists the columns in the saved
   order. An invalid source is **400**. A non-Admin session is **403**.
4. Open a packaged format such as **By_Author**. The column table is
   read-only; add/remove/save controls are not shown.

## Product path — allowed communities on a user format

1. Open a **user** format (not a packaged/system name). Detail shows **Allowed
   communities** with **All communities** and a checkbox for each community.
2. Clear **All communities** and select one or more communities. Click **Save
   communities**. A following `GET /services/displayformats/{name}` lists those
   communities in `allowedCommunities`. An unknown community is **400**. A
   non-Admin session is **403**.
3. Check **All communities** (or clear every community checkbox — that is the
   same persist state, not a third “none” visibility) and **Save communities**.
   GET then returns an empty `allowedCommunities` array, meaning every
   community (Workbench `sys_community=-1`).
4. Open a packaged format such as **By_Author**. Allowed communities are
   read-only; the editor and save control are not shown.

Existing **list** and **detail** GET (column catalog and Object ACL) are
unchanged. See [Users, roles & security](id:admin-users-roles).

## Limits

- Name is immutable after create.
- Packaged/system formats cannot be column-edited or community-edited from this catalog.
- Empty allowed-communities and all-communities are the same persist state.
  There is no “visible to no communities” value.
- Usage flags on GET (`validForFolder`, `validForViewsAndSearches`,
  `validForRelatedContent`) are derived from columns the same way Workbench
  computes them — they are not independently persisted on save.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| List | `GET /services/displayformats` |
| Load | `GET /services/displayformats/{idOrName}` |
| Create | `POST /services/displayformats` (`name` required; unique, no spaces) |
| Save | `PUT /services/displayformats/{idOrName}` (label, description) |
| Save columns | `PUT /services/displayformats/{idOrName}` (`columns` replaces the list) |
| Save communities | `PUT /services/displayformats/{idOrName}` (`allowedCommunities` array; empty array is all communities) |
| Delete | `DELETE /services/displayformats/{idOrName}` (`204` on success) |

Writes lock the format for the request and release it on save.

Integrator notes: [REST API — Display formats](id:developer-rest).
