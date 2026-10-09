---
id: admin-developer-slots
title: Developer Slots
description: Create, rename, delete, set a label, set a description, and edit assembly slot finder, relationship, arguments, and named content-type/template associations from Developer Slots chrome
version: "8.2"
order: 44
tags: [admin, developer, slots]
---

# Developer Slots

**Developer → Slots** lists assembly slot definitions (label, unique name, and
description). Admins can **create** a slot, **rename** a non-system slot, and
**delete** a non-system slot from this chrome. Label, description, and name
can be saved on an existing user slot without a design lock. The catalog and
the committed name show the new name only after a successful save. **Cancel**
discards a typed name and does not call the server.

After **Lock**, Admins can edit **finder**, **relationship**, **finder
arguments**, and **content-type / template associations** (by **name** or GUID)
on an existing slot and **Save**. Create does not write finder fields or
associations. Unlock releases the design session without saving.

**Set slot label** on an existing slot saves the label only. The update does
not send the name, description, slot type, or finder, so those stay. The new
label shows only after that save succeeds. A blank label does not clear the
slot name. The catalog shows the name when the stored label is blank. **Cancel**
does not write. HTTP 400, 403, and 409 are not success and leave the previous
label.

**Set slot description** on an existing slot saves the description only. The
update does not send the name, label, slot type, or finder, so those stay.
The new description shows only after that save succeeds. A blank description
clears the description. **Cancel** does not write. HTTP 400, 403, and 409
are not success and leave the previous description.

## Product path — set a label

1. Open an existing slot. The committed label is shown under **Set slot
   label**.
2. Click **Set slot label**, edit the text, and click **Save label**. The
   request is `PUT /services/slots/{idOrName}` with `label` only. Reload shows
   that label. The name, description, slot type, and finder are unchanged.
3. A blank label clears the stored label and does not clear the name. The
   catalog then shows the name as the label. The same label is not written.
4. Click **Cancel** before save to keep the previous label. Cancel does not
   call the server.
5. HTTP 400, 403, and 409 do not show **Slot label saved** and do not replace
   the previous label.

## Product path — set a description

1. Open an existing slot. The committed description is shown under **Set slot
   description**.
2. Click **Set slot description**, edit the text, and click **Save
   description**. The request is `PUT /services/slots/{idOrName}` with
   `description` only. Reload shows that description. The name, label, slot
   type, and finder are unchanged.
3. A blank description clears the stored description. The same description is
   not written.
4. Click **Cancel** before save to keep the previous description. Cancel does
   not call the server.
5. HTTP 400, 403, and 409 do not show **Slot description saved** and do not
   replace the previous description.

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
5. Open an existing non-system slot and click **Delete**, then confirm in
   the in-app dialog (not a browser prompt). The
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

## Product path — content-type / template associations

1. Open an existing slot. Association add/remove is **read-only** until you
   hold the design lock. Rows show the content-type and template **names**
   (GUID is used only as a fallback when a name is missing).
2. Click **Lock**. Locked-by-another-user is **409**. Non-Admin is **403**.
3. Enter a content-type **name or GUID** and a template **name or GUID**, then
   **Add association**. Remove a row with **Remove**. Save applies a **full
   replace** of the association list.
4. Click **Save slot**. Unknown names or GUIDs are **400**. Unlocked or
   locked-by-another-user is **409**. Non-Admin is **403**. Missing slot is
   **404**. Following GET round-trips names and guids. Unchanged associations
   are omitted on a properties-only save so they are not wiped.
5. Click **Unlock** when finished.

## Product path — rename

1. Open an existing **non-system** slot. The name field is editable. A
   **system** slot name stays read-only.
2. Type a new name (unique, no spaces, no `*` or `%` wildcards). The committed
   name under the title stays the previous name until save succeeds.
3. Click **Save slot**. The catalog row uses the new name when you return to
   the list. A duplicate name is **409**. A blank, whitespace, or wildcard
   name is **400** and is not saved. A system-slot rename is **409**. A
   non-Admin session is **403**. None of those responses show **Slot saved**.
4. Click **Cancel** (or **Back**) before save to keep the previous name. Cancel
   does not send the rename.

## Limits

- System slot names cannot be renamed. User slot names can, with the same
  uniqueness rules as create.
- A blank slot label does not clear the slot name. The name is what the
  catalog shows when the stored label is blank.
- Create does not write finder, relationship, finder arguments, or associations.
- Finder and association writes require a lock you already hold. The save
  request does not acquire or steal the lock.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| List | `GET /services/slots` |
| Load | `GET /services/slots/{idOrName}` |
| Create | `POST /services/slots` (`name` required; optional `label`, `description`, `slotType`) |
| Lock | `POST /services/slots/{idOrName}/lock` |
| Unlock | `POST /services/slots/{idOrName}/unlock` |
| Save | `PUT /services/slots/{idOrName}` (label, description; optional `associations` / `finderName` / `relationshipName` / `finderArguments` when those fields changed) |
| Set label | `PUT /services/slots/{idOrName}` (`label` only; a blank value does not clear the name; name, description, type, and finder are omitted and stay) |
| Set description | `PUT /services/slots/{idOrName}` (`description` only; a blank value clears; name, label, type, and finder are omitted and stay) |
| Delete | `DELETE /services/slots/{idOrName}` (`204` on success) |

Integrator notes: [REST API — Slots](id:developer-rest).
