---
id: admin-developer-slots
title: Developer Slots
description: Create and delete assembly slots from Developer Slots chrome
version: "8.2"
order: 44
tags: [admin, developer, slots]
---

# Developer Slots

**Developer → Slots** lists assembly slot definitions (label, unique name, and
description). Admins can **create** a slot and **delete** a non-system slot
from this chrome. Label, description, and content-type/template associations
can still be saved on an existing slot.

Finder name, relationship type, and finder arguments remain **read-only** on
the detail panel in this slice. They are not written on create.

## Product path — create and delete

1. Sign in as **Admin** (create and delete require the Admin role).
2. Open **Developer → Slots**, or deep-link
   `spa.jsp?entry=developer&section=slots`.
3. Click **New slot**. Enter a **name** (unique, no spaces, no wildcards).
   Optional: **label**, **description**, and **slot type** (`REGULAR` or
   `INLINE`; omitted type defaults to `REGULAR`). Save stays disabled until
   the name is valid.
4. Click **Save slot**. A duplicate name is **409** and the editor shows that
   the slot already exists. An invalid name or slot type is **400**. A
   non-Admin session is **403**. After a successful create, the name field is
   read-only and the catalog includes the new row when you return to the list.
5. Open an existing non-system slot and click **Delete**, then confirm. The
   catalog no longer lists that name. Delete of a **system slot** is **409**
   (system slots cannot be deleted). Locked-by-another-user is **409**.
   Non-Admin is **403**. Missing id/name is **404**.

## Limits

- Name is immutable after create.
- Finder / relationship / finderArguments are **not** edited in this chrome
  (a later Developer slice). Create does not write those fields.
- Association GUID pairs can still be added or removed on an existing slot
  and saved with **Save slot** (full replace).

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| List | `GET /services/slots` |
| Load | `GET /services/slots/{idOrName}` |
| Create | `POST /services/slots` (`name` required; optional `label`, `description`, `slotType`) |
| Save | `PUT /services/slots/{idOrName}` (label, description, associations) |
| Delete | `DELETE /services/slots/{idOrName}` (`204` on success) |

Integrator notes: [REST API — Slots](id:developer-rest).
