---
id: admin-developer-shared-fields
title: Developer Shared Fields
description: Create, save, and delete shared field groups, add or delete nested fields, and save control properties from Developer Shared Fields chrome
version: "8.2"
order: 44
tags: [admin, developer, shared-fields]
---

# Developer Shared Fields

**Developer → Shared Fields** lists content-editor shared field groups
(Workbench shared field files). Admins can **create**, **save**, and **delete**
a group from this chrome. On group **detail**, Admins can **add** or **delete**
nested fields and **save control properties** (and optional **choices**) for a
selected field.

The group **name** is required, must be unique (case-insensitive), and must not
contain spaces or path characters. Optional **filename** defaults to
`{name}.xml`. Nested field **name** must start with a letter and may contain
letters, digits, or underscore (max 50). Optional **data type** defaults to
`text`. Optional **occurrence** uses the same values as REST (`optional`,
`required`, `oneOrMore`, `zeroOrMore`, `count`).

This is **not** the Workbench stylesheet or system-definition editor. The
content-editor **system definition** is a separate catalog
(**Developer → System definition**).

## Product path — create, save, delete a group

1. Sign in as **Admin** (write calls require the Admin role).
2. Open **Developer → Shared Fields**, or deep-link
   `spa.jsp?entry=developer&section=shared-fields`.
3. Click **New shared field group**. Enter a **name**. Save stays disabled
   until the name is valid (no spaces, no `*`, no `/` or `..`). Optional:
   filename (`{name}.xml` when blank). The chrome rejects path separators
   client-side; REST `GET`/`PUT`/`DELETE /services/sharedfields/{name}` with
   `/` or `..` in the path is **404** (not invalid-name **400**) because the
   extra path segments do not match the group resource.
4. Click **Save**. A duplicate name is **409** and the editor shows that the
   group already exists. After a successful create, you can change the
   filename (or rename) and save again.
5. Click **Delete** and confirm in the in-app dialog (not a browser prompt).
   The catalog no longer lists that group.
   Delete of a missing group is **404**.

## Product path — nested fields and control properties

1. Open an existing group (or save a new group first so detail is writable).
2. To **add** a field, enter a **name**. Optional: data type and occurrence.
   **Add field** stays disabled until the name is valid. Click **Add field**.
   A duplicate name (including a name already defined in another shared group)
   is **409**. An invalid name is **400**. Unknown group is **404**.
3. To **delete** a field, click **Delete** on the field row and confirm in the
   in-app dialog. The catalog no longer lists that field. Unknown field is
   **404**.
4. To **save control properties**, select a field in **Control property
   values**, change parameter values (or add a name/value pair), optionally
   edit **choices**, and click **Save control properties**. GET does not
   require a lock. PUT acquires the shared-definition lock and **releases** it
   on save. **Omit choices** on save to leave the catalog unchanged. Set
   choices **type** to **none** to clear the catalog. Non-Admin callers receive
   **403**. Unknown group or field names are **404**. If another designer holds
   the shared-definition lock, writes are **409**.

## Limits

- Item-filter rules are a separate catalog (**Developer → Item filters**).
- The content-editor **system definition** is a separate catalog
  (**Developer → System definition** / `GET`/`PUT /services/systemdef`).

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| List | `GET /services/sharedfields` |
| Load | `GET /services/sharedfields/{name}` |
| Create | `POST /services/sharedfields` (`name` required; unique, no spaces) |
| Save | `PUT /services/sharedfields/{name}` (filename and optional rename) |
| Delete | `DELETE /services/sharedfields/{name}` (`204` on success) |
| Add field | `POST /services/sharedfields/{name}/fields` (`name` required; wrap root `SharedField`) |
| Delete field | `DELETE /services/sharedfields/{name}/fields/{fieldName}` (`204` on success) |
| Load control properties | `GET /services/sharedfields/{name}/fields/{fieldName}/controlProperties` (no lock) |
| Save control properties | `PUT /services/sharedfields/{name}/fields/{fieldName}/controlProperties` (request lock released on save; wrap root `SharedFieldControlProperties`; omit `choices` to leave unchanged; `choices.type` `none` clears) |

Writes lock the shared definition for the request and release it on save.

Integrator notes: [REST API — Shared fields](id:developer-rest).
