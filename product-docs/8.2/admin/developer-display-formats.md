---
id: admin-developer-display-formats
title: Developer Display Formats
description: Create, delete, edit columns, set default sort, set a label, set a description, and set allowed communities on Content Explorer display formats from Developer Display Formats chrome
version: "8.2"
order: 47
tags: [admin, developer, display-formats]
---

# Developer Display Formats

**Developer → Display Formats** lists Content Explorer display format definitions
(Workbench **Display Format** editor: unique internal name, label, description,
and column catalog). Admins can **create** a user display format, **delete** a
selected user format, **add**, **remove**, and **reorder columns**, set the
**default sort column and direction**, **set a label**, **set or clear the description**, and set
**allowed communities** on a **user** format from this chrome. The **name** is required, must be unique
(case-insensitive), and must not contain spaces, wildcards (`*` / `%`), or path
characters. Name cannot be renamed after create.

**Packaged/system** formats (`Default`, `By_Author`, `CM1_Default`, and the
other installer catalog names) stay **read-only** for columns, default sort, and
allowed communities.

## Product path — create, delete

1. Sign in as **Admin** (write calls require the Admin role).
2. Open **Developer → Display Formats**, or deep-link
   `spa.jsp?entry=developer&section=display-formats`.
3. Click **New display format**. Enter a **name**. Save stays disabled until
   the name is valid (no spaces, no `*` / `%`, no `/` or `..`) and at least one
   column is present. A blank name does not save. The create form starts with
   **Content Title** (`sys_title`). Optional: label, description, and extra
   columns from the field list. **Cancel** returns to the catalog and does not
   create the format.
4. Click **Save**. The POST body includes the name and the column list. A
   duplicate name is **409** and the editor shows that the display format
   already exists. An invalid name is **400**. A non-Admin session is **403**.
   Those errors do not add a catalog row and do not show the saved notice.
   After a successful create, the name field is read-only and the catalog
   lists the new format (including its columns) only then.
   `GET /services/displayformats/{name}` returns that user format (not **404**,
   and not a packaged format such as **By_Author**).
5. Optional: change the label and **Save** again. That save sends the label
   with the description already stored. It does not replace columns or
   communities.
6. Click **Delete** and confirm in the in-app dialog (not a browser prompt).
   REST `DELETE /services/displayformats/{name}` returns **204**; a following
   GET is **404** and the catalog no longer lists that format.
   Delete of a missing format is **404**. A format still used as a dependent,
   or locked by another user, is **409**. Packaged system formats that REST
   rejects stay locked; the chrome surfaces that conflict and does not steal
   locks.

## Product path — edit columns on a user format

1. Open a **user** format (not a packaged/system name). Detail shows the
   column table plus **Add column**, move **up** / **down**, **Remove**,
   **Default sort**, **Direction**, and **Save columns**.
2. Choose a field that is not already a column and click **Add column**.
   `sys_title` cannot be removed (the server always keeps it).
3. Select **Default** on the column that should sort the list, and set
   **Direction** to **Ascending** or **Descending**. If no default is stored
   yet (new user format, or `sortedColumnNames` missing / not in the column
   list), the chrome treats the **first column ascending** as the default and
   sends that on **Save columns**.
4. Click **Save columns**. After a successful save, a following
   `GET /services/displayformats/{name}` lists the columns in the saved
   order, returns `sortedColumnNames` for the default sort column, and
   `ascendingSort` / `descendingSort` for the format and that column.
   An invalid source or unknown sort column is **400**. A non-Admin session
   is **403**.
5. Open a packaged format such as **By_Author**. The column table is
   read-only (including default sort); add/remove/save controls are not shown.

## Product path — set a label

1. Open an existing display format. Detail shows the stored label and **Set
   display format label**. The label on the page is the stored value. Typing a
   new label does not change that text until save succeeds.
2. Click **Set display format label**. Enter the new label, or leave it blank.
   **Cancel** closes the editor and does not call the server. Saving the same
   label does not call the server.
3. Click **Save label**. The request is
   `PUT /services/displayformats/{name}` with the label only. The name stays
   the catalog key. The description, columns, and allowed communities are
   omitted, so those stored values stay. After a successful save, the detail
   and the catalog label show the new value.
4. A blank label does not clear the name. A display format cannot store an
   empty display name, so the catalog shows the name as the label. HTTP 400,
   403, and 409 leave the previous label on the page and do not show **Display
   format label saved**.

## Product path — set a description

1. Open an existing display format. Detail shows the stored description and
   **Set display format description**. The description on the page is the
   stored value. Typing a new description does not change that text until
   save succeeds.
2. Click **Set display format description**. Enter the new description, or
   leave it blank to clear the stored description. **Cancel** closes the
   editor and does not call the server. Saving the same description does not
   call the server.
3. Click **Save description**. The request is
   `PUT /services/displayformats/{name}` with the description only. The name
   stays the catalog key. Columns and allowed communities are omitted, so the
   stored lists stay. An empty description clears the stored description.
   After a successful save, the detail and the catalog description show the
   new value. HTTP 400, 403, and 409 leave the previous description on the
   page.

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
- **Save label** writes the label only. A blank label does not clear the name; the catalog shows the name. Omitting the description, columns, or allowed communities on that update does not remove them.
- **Save description** writes the description only. A blank description clears it. Omitting columns or allowed communities on that update does not remove them.
- Packaged/system formats cannot be column-edited, sort-edited, or community-edited from this catalog.
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
| Save | `PUT /services/displayformats/{idOrName}` (label, and the description already on the form) |
| Save label | `PUT /services/displayformats/{idOrName}` (`label` only; omit `description`, `columns`, and `allowedCommunities`; a blank label does not clear the name) |
| Save description | `PUT /services/displayformats/{idOrName}` (`description` only; omit `columns` and `allowedCommunities`; blank description clears) |
| Save columns | `PUT /services/displayformats/{idOrName}` (`columns` replaces the list; `sortedColumnNames` persists default sort — with `columns` uses that column's `ascendingSort`; without `columns` matches the stored list) |
| Save communities | `PUT /services/displayformats/{idOrName}` (`allowedCommunities` array; empty array is all communities) |
| Delete | `DELETE /services/displayformats/{idOrName}` (`204` on success) |

Writes lock the format for the request and release it on save.

Integrator notes: [REST API — Display formats](id:developer-rest).
