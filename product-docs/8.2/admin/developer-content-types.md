---
id: admin-developer-content-types
title: Developer Content Types
description: Lock, enable or disable, rename via REST, add or delete local fields via REST, include system or shared fields via REST, save allowed workflows, templates, item-level exits, control property values, and field-rule expressions, and unlock a content type from Developer detail chrome
version: "8.2"
order: 42
tags: [admin, developer, content-types]
---

# Developer Content Types

**Developer → Content types** lists design content types and opens a detail panel
for fields, allowed workflows, allowed templates, **item-level exits**, **control property values**,
**field-rule expressions**, and Object ACL. Design edits use an explicit **design-session lock** so two
Admins cannot overwrite the same type at once.

Integrators can **create** a content type with Admin `POST /services/contenttypes`
(JSON `name` required; unique, no spaces; optional `label` / `description`).
That call persists the type (Workbench Finish). A successful create is then
`GET /services/contenttypes/{name}` **200**. Duplicate or reserved system names
(for example **Folder**) are **409**. Invalid names (blank, spaces, wildcard)
are **400**. Non-Admin callers are **403**. This chrome does **not** include a
create wizard; rename, delete, **local field create/delete**, and **include
system/shared fields** are REST-only (no SPA field editor or field picker).
After a held lock, integrators add a local field with
`POST /services/contenttypes/{idOrName}/fields` (JSON `name` required) and
remove one with `DELETE /services/contenttypes/{idOrName}/fields/{fieldName}`.
Duplicate field names are **409**. System and shared fields cannot be removed
here (**400**). Include an existing system or shared field with
`POST /services/contenttypes/{idOrName}/fields/include` (JSON `name` and
`fieldType` `system` or `shared`; origin is not copied as local). Duplicate
include is **409**; unknown catalog field is **404**. Optional `fieldSet` on
local create targets or creates a named child field set.
Shared field
**files** are a separate design object: Admin-only `GET /services/sharedfields` and
`GET /services/sharedfields/{idOrName}` (catalog), plus
`POST /services/sharedfields`, `PUT /services/sharedfields/{idOrName}`, and
`DELETE /services/sharedfields/{idOrName}` (create / save / delete; the shared
definition is locked for the request). Nested
`POST /services/sharedfields/{idOrName}/fields` and
`DELETE /services/sharedfields/{idOrName}/fields/{fieldName}` add or remove
fields (backend column + display mapping). The SPA editor is not in this
chrome. The content-editor **system definition** (global system fields) is a
separate singleton: Admin-only `GET /services/systemdef` and
`PUT /services/systemdef` (patch existing field properties under a request lock).
System-field create/delete and an SPA editor are not in this chrome. See
[REST API](id:developer-rest).

This is **not** the full Workbench field-rule editor. The detail table still
shows rule **flags** (validation / visibility / transforms present). After
**Lock**, **Field rule expressions** lets you edit validation, visibility,
input translation, and output translation **text** (one expression per line)
and save with the same **Save content type** control. Integrators can also
call REST
`GET`/`PUT /services/contenttypes/{idOrName}/fields/{fieldName}/ruleExpressions`
(held design lock for PUT). Item-level pre/post exits and validations (CD-09)
are edited from this detail chrome after **Lock** (see below), or via
`GET`/`PUT /services/contenttypes/{idOrName}/itemExits` after a held design
lock. Item-level input translations must be request pre-processors (for
example `sys_cleanReservedHtmlClasses` or `sys_itemHTMLEncodeTransformer`),
not field UDFs such as `sys_ToUpperCase` (those stay on field rule
expressions). Omitting `preExits`/`postExits` leaves pipe extensions
unchanged. Apply-when conditions remain read-only. Choice-catalog filter /
null-entry / default-selected writes are not in this chrome.

## Product path — lock, save, unlock

1. Sign in as **Admin** (or another user with Developer access and Admin REST
   rights for design lock/save).
2. Open **Developer → Content types**, or deep-link
   `spa.jsp?entry=developer&section=content-types`.
3. Click **Open** on a catalog row (the type label). The detail panel includes
   **Object ACL** as soon as the type opens, including while the field catalog
   is still loading.
4. The **detail toolbar at the top of the panel** (sticky) shows **Lock**,
   **Save content type**, **Unlock**, and the **Enabled** checkbox. The type
   name and **Allowed templates** add/remove chrome are visible immediately
   (add/remove stay **disabled** until the type body has loaded and you hold
   the lock). The status line starts as **Not locked**. Label, description,
   enabled, field flags, and association editors stay **read-only** until you
   hold the lock. You do not need to scroll past the fields table to lock or
   save. Enabled stays disabled until you hold the lock; a failed lock (**409**)
   does not steal another user's lock or enable the checkbox, template
   add/remove, control property value editors, or Save.
5. Click **Lock**. Status becomes **Locked by you**. If another user already
   holds the lock, the panel shows an error and Save stays disabled (the product
   does **not** steal the lock).
6. Change the **Description**, **Enabled** checkbox, and any other unlocked
   fields, then click **Save content type**. Save writes while the lock is still
   held. It does **not** unlock. **Enabled** is written with a dedicated
   `PUT /services/contenttypes/{idOrName}/enabled` (CD-13), not the bulk
   content-type save. Without a lock the checkbox stays disabled and a toggle
   cannot persist.
7. To change **Allowed templates**, follow **Allowed templates (after lock)**
   below. Save does **not** unlock.
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

### Allowed templates (after lock)

The **Allowed templates** list and add field are **disabled** until you hold
the lock (including while the type is still loading). After **Lock**:

1. Add a template by its existing name or GUID (`type-host-uuid`, for example
   `0-10-347`) and click **Add**, or **Remove** a row.
2. Click **Save content type**. The product replaces the whole allowed-template
   set (`PUT /services/contenttypes/{idOrName}/allowedTemplates`). Save does
   **not** unlock. A following `GET .../allowedTemplates` lists the new set.
3. Without a lock, Add / Remove / Save stay **disabled**. The product does
   **not** steal another user's lock (lock failure is **409**). An empty list
   clears associations. Unknown names return an error; the lock is not stolen.

This is not the full Workbench template picker.

### Item-level exits (after lock)

The **Item-level exits** lists (input translations, output translations,
validations, pipe pre-exits, and post-exits) are **read-only** until you hold
the lock. After **Lock**:

1. Add an extension by its fully-qualified name (for example
   `Java/global/percussion/generic/sys_ToUpperCase`) and an optional parameter
   value (for example `sys_title`), then click **Add**, or **Remove** a row.
2. Click **Save content type**. The product replaces the item-level exits set
   (`PUT /services/contenttypes/{idOrName}/itemExits`). Save does **not**
   unlock. A following `GET .../itemExits` lists the new set.
3. Without a lock, Add / Remove / Save stay **disabled**. The product does
   **not** steal another user's lock (lock failure is **409**). An empty list
   clears that list. Unknown extension FQNs return an error; the lock is not
   stolen.

Apply-when conditions on exits are **read-only** and are not written on save.
This is not the full Workbench Properties-tab condition editor.

### Control property values (after lock)

The **Control property values** list is at the top of the detail panel with
**Allowed templates**. Pick a field to view its display-control parameter
**name and value** pairs (not names only). Value editors, Add, and Remove stay
**disabled** until you hold the lock.

After **Lock**:

1. Select the field. The product loads
   `GET /services/contenttypes/{idOrName}/fields/{fieldName}/controlProperties`.
2. Edit a value, **Add** a parameter name/value, or **Remove** a row.
3. Click **Save content type**. The product replaces the property list
   (`PUT .../fields/{fieldName}/controlProperties`) while the lock is still
   held. Save does **not** send the choice catalog, so existing choices stay
   unchanged. A following GET on the same path lists the new values.
4. Without a lock, value editors and Save stay **disabled**. The product does
   **not** steal another user's lock (lock failure is **409**). An unlocked
   edit does not persist.

This is not the full Workbench Properties tab. Choice catalogs show as
read-only (type only) in this chrome; Save omits `choices`. Integrators write
choice filter, null-entry, and default-selected on
`PUT .../fields/{fieldName}/controlProperties` by sending `choices` — see
[REST API — Content types](id:developer-rest).

### Field rule expressions (after lock)

The **Field rule expressions** section lists the type's fields. Choose a field
to load its current validation, visibility, input translation, and output
translation expressions. The text areas are **read-only** until you hold the
lock.

1. Click **Lock**. Status becomes **Locked by you**.
2. Select a field. Each list is one expression per line:
   * Validation / visibility **conditionals**: `variable operator value` (for
     example `sys_title <> ""`). Operator `!=` is stored as `<>`.
   * Extension call: `ext:Java/global/percussion/generic/sys_ToUpperCase`
     (optional literal parameter after `|`).
   * Named validation rule: `ref:ruleName` (validation only; visibility
     rejects `ref:`).
   * Input / output translation: one extension FQN per line, optional
     `| parameter`.
3. Click **Save content type**. The product replaces that field's four lists
   (`PUT /services/contenttypes/{idOrName}/fields/{fieldName}/ruleExpressions`).
   Empty text clears that list. Save does **not** unlock. A following GET of
   the same path reflects the new expressions.
4. Without a lock, the text areas and Save stay **disabled**. The product does
   **not** steal another user's lock (lock failure is **409**).

This is expression **text**, not the Workbench visual rule builder. Apply-when
conditions on field validation are not written.

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

## Rename a content type (REST)

Developer Content Type chrome does **not** rename the type. Integrators rename
with REST after a held design-session lock:

1. `POST /services/contenttypes/{idOrName}/lock`
2. `PUT /services/contenttypes/{idOrName}/name` with Jackson root
   `ContentTypeName` (`name` required). The new name must be unique
   (case-insensitive) and must not contain spaces.
3. `GET /services/contenttypes/{id}` returns the new name. `GET` by the
   previous name is **404**.
4. `POST .../unlock` when done.

Bulk `PUT /services/contenttypes/{idOrName}` still does **not** change name.
Unlocked or another user's lock is **409**. Collision or spaces is **400**.

## Delete a content type (REST)

The Developer Content types chrome does **not** expose delete in this release.
Integrators delete a type over REST after holding the design-session lock:

1. `POST /services/contenttypes/{idOrName}/lock` as **Admin**.
2. `DELETE /services/contenttypes/{idOrName}`. Success is **204**. The lock is
   not stolen if another user holds it (**409**). Missing types are **404**.
3. A following `GET /services/contenttypes/{idOrName}` is **404**.
4. Types that still have dependents fail with **400**. The product does **not**
   cascade-delete items.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| Lock | `POST /services/contenttypes/{idOrName}/lock` |
| Save (label, description, fields) | `PUT /services/contenttypes/{idOrName}` (requires a held lock; does not send `enabled`, `allowedWorkflows`, or `allowedTemplates`) |
| Enable / disable | `PUT /services/contenttypes/{idOrName}/enabled` (CD-13; requires a held lock; does not acquire or release it) |
| Type-level search indexing | `GET` / `PUT /services/contenttypes/{idOrName}/searchIndexing` (CD-10; REST-only; PUT requires a held lock; default on; not the per-field searchable flag; no SPA Properties checkbox) |
| Load icon strategy | `GET /services/contenttypes/{idOrName}/icon` (CD-11; no lock; `none` / `specified` / `fromFileField`) |
| Set icon strategy | `PUT /services/contenttypes/{idOrName}/icon` (CD-11; Admin; held lock; `none` clears value; no binary upload; no SPA picker) |
| Rename | `PUT /services/contenttypes/{idOrName}/name` (CD-01; Admin; held lock; unique name, no spaces; bulk PUT does not rename) |
| Save allowed workflows | `PUT /services/contenttypes/{idOrName}/allowedWorkflows` (requires a held lock; does not unlock) |
| Replace allowed templates | `PUT /services/contenttypes/{idOrName}/allowedTemplates` (held lock; full replace) |
| Confirm allowed templates | `GET /services/contenttypes/{idOrName}/allowedTemplates` |
| Replace item-level exits | `PUT /services/contenttypes/{idOrName}/itemExits` (CD-09; held lock; full replace of translations/validations; empty lists clear) |
| Confirm item-level exits | `GET /services/contenttypes/{idOrName}/itemExits` |
| Load field control properties | `GET /services/contenttypes/{idOrName}/fields/{fieldName}/controlProperties` (CD-07; no lock) |
| Save field control properties | `PUT /services/contenttypes/{idOrName}/fields/{fieldName}/controlProperties` (held lock; full replace of values; does not send `choices`) |
| Load field rule expressions | `GET /services/contenttypes/{idOrName}/fields/{fieldName}/ruleExpressions` |
| Save field rule expressions | `PUT /services/contenttypes/{idOrName}/fields/{fieldName}/ruleExpressions` (held lock; full replace of validation, visibility, inputTranslation, outputTranslation) |
| Unlock | `POST /services/contenttypes/{idOrName}/unlock` |
| Delete | `DELETE /services/contenttypes/{idOrName}` (Admin; held lock; 204; 409 if unlocked or another user holds the lock; 400 if dependents; no SPA chrome) |

Integrator notes: [REST API — Content types](id:developer-rest). Object ACL on
the same detail panel: [Object ACL & default template](id:admin-object-acl).

See [REST API — Content types](id:developer-rest) for the dedicated
`itemExits`, `controlProperties`, and `ruleExpressions` surfaces.
