---
id: admin-developer-item-filters
title: Developer Item Filters
description: Create, save, delete, and edit assembly item-filter rules from Developer Item Filters chrome
version: "8.2"
order: 45
tags: [admin, developer, item-filters]
---

# Developer Item Filters

**Developer → Item Filters** lists assembly item filters (Workbench **Item
Filter** editor: unique name, description, parent filter, and rules). Admins
can **create**, **save**, and **delete** a filter from this chrome, and can
**add**, **edit**, and **remove** rule rows (rule name plus parameter
name/value pairs) on the detail panel. The **name** is required, must be
unique (case-insensitive), and must not contain spaces, wildcards (`*` / `%`),
or path characters. Name cannot be renamed after create.

Rule names are extension references (typically
`Java/global/percussion/itemfilter/…`). This chrome is a row editor over the
Admin REST contract — it is **not** the classic Workbench rule-builder UI.

## Product path — create, save, delete

1. Sign in as **Admin** (write calls require the Admin role).
2. Open **Developer → Item Filters**, or deep-link
   `spa.jsp?entry=developer&section=item-filters`.
3. Click **New item filter**. Enter a **name**. Save stays disabled until the
   name is valid (no spaces, no `*` / `%`, no `/` or `..`). Optional:
   description, parent filter name, and legacy authtype. You may also **Add
   rule** before the first save.
4. Click **Save**. A duplicate name is **409** and the editor shows that the
   filter already exists. An invalid name or rule is **400**. A non-Admin
   session is **403**. After a successful create, the name field is read-only.
5. Change the description (or parent / authtype / rules) and **Save** again.
   GET fields already on detail (`description`, `parentFilter`,
   `legacyAuthtype`, `rules`) round-trip on save.
6. Click **Delete** and confirm in the in-app dialog (not a browser prompt).
   The catalog no longer lists that filter.
   Delete of a missing filter is **404**. A filter still associated with a
   content list is **409**.

## Product path — rule rows

1. Open an existing filter (or stay on detail after create).
2. Click **Add rule**. Enter the extension rule name. Optionally **Add
   parameter** and fill **name** / **value** (blank parameter names are dropped
   on save).
3. Click **Save**. The Admin `PUT /services/itemfilters/{idOrName}` body
   includes `rules[]`. A following GET shows the same rows.
4. Edit a rule name or parameter in place, or **Remove rule** / **Clear all
   rules**, then **Save**. Clearing all rules and saving sends `rules: []`
   plus `clearRules: true`, which clears stored rules. Omitting `rules` on a
   PUT (REST clients that do not send the field) leaves stored rules unchanged
   — this chrome always sends the current draft.

## Limits

- Name is immutable after create.
- Rule names must resolve as item-filter extensions at runtime; unknown or
  blank rule names are **400**.
- Pipeline SQL filters are a different Developer surface.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| List | `GET /services/itemfilters` |
| Load | `GET /services/itemfilters/{idOrName}` |
| Create | `POST /services/itemfilters` (`name` required; unique, no spaces; optional `rules[]`) |
| Save | `PUT /services/itemfilters/{idOrName}` (description, parent, authtype, rules) |
| Delete | `DELETE /services/itemfilters/{idOrName}` (`204` on success) |

Writes lock the filter for the request and release it on save.

Integrator notes: [REST API — Item filters](id:developer-rest).
