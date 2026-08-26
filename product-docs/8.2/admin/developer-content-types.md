---
id: admin-developer-content-types
title: Developer Content Types
description: Lock, save, and unlock a content type from Developer detail chrome
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
visibility / transform **expressions** stay summary-only on the detail table.
Integrators write them with REST
`GET`/`PUT /services/contenttypes/{idOrName}/fields/{fieldName}/ruleExpressions`
(held design lock for PUT). Item-level pre/post exits and validations (CD-09)
use `GET`/`PUT /services/contenttypes/{idOrName}/itemExits`. This page does
**not** add Properties-tab or expression-editor chrome.

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
7. To change **Allowed templates**, lock the type, add or remove existing
   template names or GUIDs (`type-host-uuid`, for example `0-10-347`), then
   **Save content type**. Save replaces the whole allowed-template set. An empty
   list clears associations. Unknown names return an error; the lock is not
   stolen. This is not the full Workbench template picker.
8. Click **Unlock** when you are done. Status returns to **Not locked** and the
   form is read-only again. **Back to list** also releases a lock you hold.

Locks expire after **30 minutes**. If Save fails because the lock expired,
click **Lock** again and retry.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| Lock | `POST /services/contenttypes/{idOrName}/lock` |
| Save (label, description, fields, workflows) | `PUT /services/contenttypes/{idOrName}` (requires a held lock) |
| Replace allowed templates | `PUT /services/contenttypes/{idOrName}/allowedTemplates` (held lock; full replace) |
| Confirm allowed templates | `GET /services/contenttypes/{idOrName}/allowedTemplates` |
| Unlock | `POST /services/contenttypes/{idOrName}/unlock` |

Integrator notes: [REST API — Content types](id:developer-rest). Object ACL on
the same detail panel: [Object ACL & default template](id:admin-object-acl).

Field **control property values**, **choice catalogs**, and **rule expressions** are not
edited in this chrome. Integrators use:

* `GET` / `PUT /services/contenttypes/{idOrName}/fields/{fieldName}/controlProperties`
* `GET` / `PUT /services/contenttypes/{idOrName}/fields/{fieldName}/ruleExpressions`

Hold the design-session lock before either PUT. See [REST API — Content types](id:developer-rest).
