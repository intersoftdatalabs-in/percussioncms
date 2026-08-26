---
id: admin-developer-content-types
title: Developer Content Types
description: Lock, save, and unlock a content type from Developer detail chrome, including allowed workflows
version: "8.2"
order: 42
tags: [admin, developer, content-types]
---

# Developer Content Types

**Developer → Content types** lists design content types and opens a detail panel
for fields, allowed workflows, allowed templates, and Object ACL. Design edits
use an explicit **design-session lock** so two Admins cannot overwrite the same
type at once.

This is **not** the full Workbench field-rule editor. Field validation /
visibility / transform **expressions** stay read-only on the detail table.
Item-level pre/post exits and validations (CD-09) are exposed on REST
`GET`/`PUT /services/contenttypes/{idOrName}/itemExits` (held design lock for
write). This page does **not** add Properties-tab chrome for those exits.

## Product path — lock, save, unlock

1. Sign in as **Admin** (or another user with Developer access and Admin REST
   rights for design lock/save).
2. Open **Developer → Content types**, or deep-link
   `spa.jsp?entry=developer&section=content-types`.
3. Open a catalog row.
4. The detail toolbar shows **Lock**, **Save content type**, and **Unlock**.
   The status line starts as **Not locked**. Label, description, enabled, field
   flags, and association editors stay **read-only** until you hold the lock.
5. Click **Lock**. Status becomes **Locked by you**. If another user already
   holds the lock, the panel shows an error and Save stays disabled (the product
   does **not** steal the lock).
6. Change the **Description** (and any other unlocked fields), then click
   **Save content type**. Save writes while the lock is still held. It does
   **not** unlock.
7. Click **Unlock** when you are done. Status returns to **Not locked** and the
   form is read-only again. **Back to list** also releases a lock you hold.

### Allowed workflows (after lock)

The **Allowed workflows** list is read-only until you hold the lock. After
**Lock**:

1. Add a workflow by its existing name (for example **Standard Workflow**) and
   click **Add**, or **Remove** a row. Use **Default** to choose the default
   workflow.
2. Click **Save content type**. The product replaces the allowed-workflow set
   (`PUT /services/contenttypes/{idOrName}/allowedWorkflows`). Save does **not**
   unlock. Reloading the type (or GET detail) lists the new set.
3. Without a lock, Add / Remove / Save stay **disabled**. The product does
   **not** steal another user's lock (lock failure is **409**).

This is not a full Workbench workflow picker. The name you add must already
exist on the server. Template association chrome is a separate surface.

Locks expire after **30 minutes**. If Save fails because the lock expired,
click **Lock** again and retry.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| Lock | `POST /services/contenttypes/{idOrName}/lock` |
| Save (label, description, fields, templates) | `PUT /services/contenttypes/{idOrName}` (requires a held lock) |
| Save allowed workflows | `PUT /services/contenttypes/{idOrName}/allowedWorkflows` (requires a held lock; does not unlock) |
| Unlock | `POST /services/contenttypes/{idOrName}/unlock` |

Integrator notes: [REST API — Content types](id:developer-rest). Object ACL on
the same detail panel: [Object ACL & default template](id:admin-object-acl).

Field **control property values** and **choice catalogs** are not edited in this chrome.
Integrators use `GET` / `PUT /services/contenttypes/{idOrName}/fields/{fieldName}/controlProperties`
(hold the design-session lock before PUT). See [REST API — Content types](id:developer-rest).
