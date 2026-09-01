---
id: admin-developer-display-formats
title: Developer Display Formats
description: Add, remove, and reorder columns on user display formats from Developer Display Formats
version: "8.2"
order: 47
tags: [admin, developer, display-formats]
---

# Developer Display Formats

**Developer → Display Formats** lists Content Explorer display format
definitions (Workbench **Display Format** editor: internal name, label, and
column catalog). Admins can **add**, **remove**, and **reorder columns** on a
**user** format from this chrome. The column list is saved with
`PUT /services/displayformats/{idOrName}`.

**Packaged/system** formats (`Default`, `By_Author`, `CM1_Default`, and the
other installer catalog names) stay **read-only**. Create and delete of
formats, and allowed-community editing, are not in this chrome.

## Product path — edit columns on a user format

1. Sign in as **Admin** (write calls require the Admin role).
2. Open **Developer → Display Formats**, or deep-link
   `spa.jsp?entry=developer&section=display-formats`.
3. Open a **user** format (not a packaged/system name). Detail shows the
   column table plus **Add column**, move **up** / **down**, **Remove**, and
   **Save columns**.
4. Choose a field that is not already a column and click **Add column**.
   `sys_title` cannot be removed (the server always keeps it).
5. Click **Save columns**. After a successful save, a following
   `GET /services/displayformats/{name}` lists the columns in the saved
   order. An invalid source is **400**. A non-Admin session is **403**.
6. Open a packaged format such as **By_Author**. The column table is
   read-only; add/remove/save controls are not shown.

Object ACL on the same detail page is unchanged. See
[Users, roles & security](id:admin-users-roles).

## Limits

- Packaged/system formats cannot be column-edited from this catalog.
- Display format create/delete is a later slice.
- Allowed-community editing is a later slice.
- Search and view field-selection reuse this picker in later UI-08 work.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| List | `GET /services/displayformats` |
| Load | `GET /services/displayformats/{idOrName}` |
| Save columns | `PUT /services/displayformats/{idOrName}` (`columns` replaces the list) |

Writes lock the format for the request and release it on save. Name is not
renamed on PUT. Usage flags (`validForFolder`, `validForViewsAndSearches`,
`validForRelatedContent`) stay **derived from columns**.

Integrator notes: [REST API — Display formats](id:developer-rest).
