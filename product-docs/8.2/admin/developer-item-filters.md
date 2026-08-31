---
id: admin-developer-item-filters
title: Developer Item Filters
description: Create, save, and delete assembly item filters from Developer Item Filters chrome
version: "8.2"
order: 45
tags: [admin, developer, item-filters]
---

# Developer Item Filters

**Developer → Item Filters** lists assembly item filters (Workbench **Item
Filter** editor: unique name, description, parent filter, and rules). Admins
can **create**, **save**, and **delete** a filter from this chrome. The
**name** is required, must be unique (case-insensitive), and must not contain
spaces, wildcards (`*` / `%`), or path characters. Name cannot be renamed
after create.

This is **not** the Workbench rule editor. The rule catalog on detail is
**read-only** in this chrome; **Save** round-trips the loaded rules so they
are not cleared. Nested rule create/edit remain REST-only.

## Product path — create, save, delete

1. Sign in as **Admin** (write calls require the Admin role).
2. Open **Developer → Item Filters**, or deep-link
   `spa.jsp?entry=developer&section=item-filters`.
3. Click **New item filter**. Enter a **name**. Save stays disabled until the
   name is valid (no spaces, no `*` / `%`, no `/` or `..`). Optional:
   description, parent filter name, and legacy authtype.
4. Click **Save**. A duplicate name is **409** and the editor shows that the
   filter already exists. An invalid name is **400**. A non-Admin session is
   **403**. After a successful create, the name field is read-only.
5. Change the description (or parent / authtype) and **Save** again. GET
   fields already on detail (`description`, `parentFilter`, `legacyAuthtype`,
   `rules`) round-trip on save.
6. Click **Delete** and confirm. The catalog no longer lists that filter.
   Delete of a missing filter is **404**. A filter still associated with a
   content list is **409**.

## Limits

- Name is immutable after create.
- Rule and parameter rows are displayed and round-tripped; they are not
  edited in this chrome.
- Pipeline SQL filters are a different Developer surface.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| List | `GET /services/itemfilters` |
| Load | `GET /services/itemfilters/{idOrName}` |
| Create | `POST /services/itemfilters` (`name` required; unique, no spaces) |
| Save | `PUT /services/itemfilters/{idOrName}` (description, parent, authtype, rules) |
| Delete | `DELETE /services/itemfilters/{idOrName}` (`204` on success) |

Writes lock the filter for the request and release it on save.

Integrator notes: [REST API — Item filters](id:developer-rest).
