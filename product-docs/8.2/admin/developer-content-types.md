---
id: admin-developer-content-types
title: Developer Content Types
description: Lock, enable or disable, save allowed workflows and templates, and unlock a content type from Developer detail chrome
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
4. The **detail toolbar at the top of the panel** (sticky) shows **Lock**,
   **Save content type**, **Unlock**, and the **Enabled** checkbox. The status
   line starts as **Not locked**. Label, description, enabled, field flags, and
   association editors stay **read-only** until you hold the lock. You do not
   need to scroll past the fields table to lock or save.
5. Click **Lock**. Status becomes **Locked by you**. If another user already
   holds the lock, the panel shows an error and Save stays disabled (the product
   does **not** steal the lock).
6. Change the **Description**, **Enabled** checkbox, and any other unlocked
   fields, then click **Save content type**. Save writes while the lock is still
   held. It does **not** unlock. **Enabled** is written with a dedicated
   `PUT /services/contenttypes/{idOrName}/enabled` (CD-13), not the bulk
   content-type save. Without a lock the checkbox stays disabled and a toggle
   cannot persist.
7. To change **Allowed templates**, lock the type, add or remove existing
   template names or GUIDs (`type-host-uuid`, for example `0-10-347`), then
   **Save content type**. Save replaces the whole allowed-template set. An empty
   list clears associations. Unknown names return an error; the lock is not
   stolen. This is not the full Workbench template picker.
8. Click **Unlock** when you are done. Status returns to **Not locked** and the
   form is read-only again. **Back to list** also releases a lock you hold.

### Allowed workflows (after lock)

The **Allowed workflows** list is read-only until you hold the lock. Add,
Remove, and the workflow-name field stay **disabled** while status is **Not
locked**. After **Lock**:

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

## Enable or disable a content type

The **Enabled** checkbox on Developer Content Type detail controls whether the
type is available for runtime use.

1. Hold the design-session **Lock**. The checkbox is read-only until you do.
2. Toggle **Enabled**, then **Save content type**. The SPA calls
   `PUT /services/contenttypes/{idOrName}/enabled` (Jackson root
   `ContentTypeEnabled`) while the lock is still held.
3. A following `GET /services/contenttypes/{idOrName}` reflects the new
   `enabled` value.
4. If another user already holds the lock, **Lock** returns **409**. The
   product does **not** steal the lock, Save stays disabled, and **Enabled**
   remains read-only.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| Lock | `POST /services/contenttypes/{idOrName}/lock` |
| Save (label, description, fields) | `PUT /services/contenttypes/{idOrName}` (requires a held lock; does not send `enabled`, `allowedWorkflows`, or `allowedTemplates`) |
| Enable / disable | `PUT /services/contenttypes/{idOrName}/enabled` (CD-13; requires a held lock; does not acquire or release it) |
| Save allowed workflows | `PUT /services/contenttypes/{idOrName}/allowedWorkflows` (requires a held lock; does not unlock) |
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
