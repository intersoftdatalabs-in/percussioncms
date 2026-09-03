---
id: admin-developer-relationship-types
title: Developer Relationship Types
description: Create, edit, and delete user relationship types from Developer Relationship Types chrome
version: "8.2"
order: 44
tags: [admin, developer, relationship-types]
---

# Developer Relationship Types

**Developer → Relationship Types** lists system and user relationship type
definitions (name, label, category, cloning flags, effects, and properties).
Admins can **create**, **save**, and **delete** **user** relationship types
from this chrome. Packaged **system** types remain **read-only** — open them
to inspect effects and properties, then create a user type (optionally
**Copy from** a system type) to customize.

## Product path — create, save, delete

1. Sign in as **Admin** (write calls require the Admin role).
2. Open **Developer → Relationship Types**, or deep-link
   `spa.jsp?entry=developer&section=relationship-types`.
3. Click **New relationship type**. Enter a **name** (no spaces or wildcards;
   cannot be renamed later). Either:
   - Choose a **category** (for example `Generic` / `rs_generic`), or
   - Select **Copy from** an existing type (Workbench copy-from-system).
4. Optional: label, description, and cloning / revision flags (when not
   copying). Click **Save**. A duplicate name is **409**. After a successful
   create, the name field is read-only and the type appears in the catalog.
5. Open a **user** type, change the label (or category / flags), and **Save**
   again.
6. Click **Delete** and confirm in the in-app dialog (not a browser prompt).
   The catalog no longer lists that type. Delete of a missing type is **404**.
   A **system** type cannot be deleted (**409**); the editor hides Save/Delete
   for system types.

## Limits

- System relationship types are immutable in this chrome (and via REST).
- Name is immutable after create.
- Cloning field-override editor and effect condition / execution-context edit
  are not in this chrome (see `designGaps` on detail).
- Deep effect/function property dialogs remain Workbench parity debt.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| List | `GET /services/relationshiptypes` |
| Load | `GET /services/relationshiptypes/{idOrName}` |
| Create | `POST /services/relationshiptypes` (`name` + `category` or `copyFrom`) |
| Save | `PUT /services/relationshiptypes/{idOrName}` (name immutable) |
| Delete | `DELETE /services/relationshiptypes/{idOrName}` (`204` on success) |

Integrator notes: [REST API — Relationship types](id:developer-rest).
