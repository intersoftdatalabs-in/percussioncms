---
id: admin-developer-system-def
title: Developer System Def
description: Save, add, and delete content-editor system fields from Developer System definition chrome
version: "8.2"
order: 44
tags: [admin, developer, system-def]
---

# Developer System Def

**Developer → System definition** lists global content-editor system fields
(name, data type, occurrence, required, searchable, read-only). Admins can
**save** property patches on existing fields and **add** or **delete** a system
field from this chrome. Writes use a **request lock that is released on save**
(there is no separate Lock / Unlock toolbar).

This is **not** the Workbench control, stylesheet, or application-flow editor.
Those remain later slices.

## Product path — save, add, delete

1. Sign in as **Admin** (write calls require the Admin role).
2. Open **Developer → System definition**, or deep-link
   `spa.jsp?entry=developer&section=system-def`.
3. To **add** a field, enter a **name** (letter, then letters, digits, or
   underscore; no spaces). Optional: data type (defaults to `text`), searchable,
   and required. **Add field** stays disabled until the name is valid.
4. Click **Add field**. A duplicate name is **409** and the panel shows that
   the field already exists. An invalid name is **400**. If another designer
   holds the system-definition lock, save/add/delete is **409**.
5. To **save** properties, change **Searchable** or **Occurrence** on a catalog
   row and click **Save fields**. The request lock is acquired and released on
   that save. Data type and read-only stay display-only.
6. Click **Delete** on a row and confirm. The catalog no longer lists that
   field. System-mandatory and system-internal fields cannot be deleted
   (**400**).

## Limits

- Control properties, stylesheets, and application flow are not in this chrome.
- Shared field groups are a separate catalog (**Developer → Shared fields**).
- There is no persistent design-session lock UI; each write holds the lock only
  for that request.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| Load | `GET /services/systemdef` |
| Save properties | `PUT /services/systemdef` (patch existing `fields[]`) |
| Add field | `POST /services/systemdef/fields` (`name` required) |
| Delete field | `DELETE /services/systemdef/fields/{fieldName}` (`204` on success) |

Integrator notes: [REST API — System definition](id:developer-rest).
