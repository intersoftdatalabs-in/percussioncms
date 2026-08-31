---
id: admin-developer-shared-fields
title: Developer Shared Fields
description: Create, save, and delete shared field groups from Developer Shared Fields chrome
version: "8.2"
order: 44
tags: [admin, developer, shared-fields]
---

# Developer Shared Fields

**Developer → Shared Fields** lists content-editor shared field groups
(Workbench shared field files). Admins can **create**, **save**, and **delete**
a group from this chrome. The group **name** is required, must be unique
(case-insensitive), and must not contain spaces or path characters. Optional
**filename** defaults to `{name}.xml`.

This is **not** the Workbench field/control editor. The field catalog on
detail is **read-only** in this chrome. Nested field create/delete and
control-property write remain REST-only.

## Product path — create, save, delete

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
5. Click **Delete** and confirm. The catalog no longer lists that group.
   Delete of a missing group is **404**.

## Limits

- Field add/remove and control-property / choice editors are not in this
  chrome (REST `POST`/`DELETE …/fields` and `GET`/`PUT …/controlProperties`).
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

Writes lock the shared definition for the request and release it on save.

Integrator notes: [REST API — Shared fields](id:developer-rest).
