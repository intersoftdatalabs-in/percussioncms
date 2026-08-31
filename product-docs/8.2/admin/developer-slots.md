---
id: admin-developer-slots
title: Developer Slots
description: Create, delete, and edit assembly slot finder, relationship, and arguments from Developer Slots chrome
version: "8.2"
order: 44
tags: [admin, developer, slots]
---

# Developer Slots

**Developer → Slots** lists assembly slot definitions (label, unique name, and
description). Admins can **create** a slot and **delete** a non-system slot
from this chrome. Label, description, and content-type/template associations
can still be saved on an existing slot.

After **Lock**, Admins can edit **finder**, **relationship**, and **finder
arguments** on an existing slot and **Save**. Create does not write finder
fields. Unlock releases the design session without saving.

## Product path — create and delete

1. Sign in as **Admin** (create, delete, lock, and finder writes require the
   Admin role).
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

## Product path — finder, relationship, and arguments

1. Open an existing slot. Finder, relationship, and finder arguments are
   **read-only** until you hold the design lock.
2. Click **Lock**. Locked-by-another-user is **409**. Non-Admin is **403**.
3. Edit **Finder** (content-finder extension, for example
   `Java/global/percussion/slotcontentfinder/sys_RelationshipContentFinder`),
   **Relationship** (allowed relationship type, for example
   `ActiveAssembly`), and **Finder arguments** (name/value rows). An empty
   relationship **clears** the allowed relationship. Removing all argument
   rows **clears** finder arguments.
4. Click **Save slot**. The PUT omits unchanged finder fields so a
   label/description/association-only save does not wipe catalog finder
   values. Invalid finder extension is **400**. Unknown relationship type is
   **400**. Unlocked or locked-by-another-user is **409**. Non-Admin is
   **403**. Save does **not** release the lock.
5. Click **Unlock** when finished (or **Back**, which releases a lock you
   hold).

## Limits

- Name is immutable after create.
- Create does not write finder, relationship, or finder arguments.
- Finder writes require a lock you already hold. The save request does not
  acquire or steal the lock.
- Association GUID pairs can still be added or removed on an existing slot
  and saved with **Save slot** (full replace), including without a finder
  lock when finder fields are unchanged.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| List | `GET /services/slots` |
| Load | `GET /services/slots/{idOrName}` |
| Create | `POST /services/slots` (`name` required; optional `label`, `description`, `slotType`) |
| Lock | `POST /services/slots/{idOrName}/lock` |
| Unlock | `POST /services/slots/{idOrName}/unlock` |
| Save | `PUT /services/slots/{idOrName}` (label, description, associations; optional `finderName` / `relationshipName` / `finderArguments` when those fields changed) |
| Delete | `DELETE /services/slots/{idOrName}` (`204` on success) |

Integrator notes: [REST API — Slots](id:developer-rest).
