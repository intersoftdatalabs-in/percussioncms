---
id: admin-developer-display-formats
title: Developer Display Formats
description: Create and delete Content Explorer display formats from Developer Display Formats chrome
version: "8.2"
order: 47
tags: [admin, developer, display-formats]
---

# Developer Display Formats

**Developer → Display Formats** lists Content Explorer display format definitions
(Workbench **Display Format** editor: unique internal name, label, and
description). Admins can **create** a user display format and **delete** a
selected user format from this chrome. The **name** is required, must be unique
(case-insensitive), and must not contain spaces, wildcards (`*` / `%`), or path
characters. Name cannot be renamed after create.

This is **not** the Workbench column picker. Column and sort rows on detail are
**read-only**. Shared field-selection / column designer completeness is a later
slice (UI-08).

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
   and the catalog lists the new format.
5. Optional: change label or description and **Save** again. Columns are not
   written.
6. Click **Delete** and confirm. The catalog no longer lists that format.
   Delete of a missing format is **404**. A format still used as a dependent,
   or locked by another user, is **409**. Packaged system formats that REST
   rejects stay locked; the chrome surfaces that conflict and does not steal
   locks.

Existing **list** and **detail** GET (column catalog and Object ACL) are
unchanged.

## Limits

- Name is immutable after create.
- Column and sort configuration write is not in this chrome (UI-08).
- Allowed-community editing is not in this chrome.
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
| Delete | `DELETE /services/displayformats/{idOrName}` (`204` on success) |

Writes lock the format for the request and release it on save.

Integrator notes: [REST API — Display formats](id:developer-rest).
