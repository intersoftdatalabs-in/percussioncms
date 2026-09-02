---
id: admin-developer-content-types
title: Developer Content Types
description: Create, rename, or delete a content type from Developer chrome, lock, enable or disable, toggle type-level search indexing, set icon strategy, add or delete local fields from Developer Content Type detail, include system or shared fields from Developer detail, export or import design XML from Developer Content Types chrome, save allowed workflows, templates, item-level exits, control property values, field choice catalogs, and field-rule expressions, and unlock a content type from Developer detail chrome
version: "8.2"
order: 42
tags: [admin, developer, content-types]
---

# Developer Content Types

**Developer → Content types** lists design content types, **creates** a new type,
and opens a detail panel
for fields, allowed workflows, allowed templates, **icon strategy**, **item-level exits**, **control property values**,
the **choice catalog**, **field-rule expressions**, and Object ACL. Design edits and **delete** use an explicit **design-session lock** so two
Admins cannot overwrite or remove the same type at once.

Admins can **create** a content type from this catalog (**New content type**) or with
`POST /services/contenttypes`
(JSON `name` required; unique, no spaces; optional `label` / `description` / `enabled`).
That call persists the type (Workbench Finish). Create and save share the packed
NODEDEF design-object id for the new type (the default editor template id `0` is
not used as the lock or persist identity). A successful create is then
`GET /services/contenttypes/{name}` **200** and the catalog lists the new row.
Duplicate or reserved system names
(for example **Folder**) are **409**. Invalid names (blank, spaces, wildcard)
are **400**. Non-Admin callers are **403**. Integrators can also **import** one
Workbench-equivalent `ItemDefData` design XML with Admin
`POST /services/contenttypes/import` (CD-14; create only; duplicate name **409**;
invalid XML **400**; the new object's create lock is released and existing types
are not stolen). This chrome includes **export** (detail **Export XML**) and **create-only import**
(catalog **Import XML**). After a held **Lock**, operators can **rename** the type
(detail **Rename content type**), **add** and **delete local fields** from Content
Type detail (see **Local fields** below).
The add call creates the backend column **before** the content editor application is
re-initialized. After a held lock, the detail **Include system or shared field**
picker includes an existing catalog field
(`POST /services/contenttypes/{idOrName}/fields/include`; origin stays
system/shared). Duplicate include is **409**. Unknown catalog field is **404**.
Invalid `fieldType` is **400**. Integrators can also call
`POST /services/contenttypes/{idOrName}/fields` (JSON `name` required; origin
is always `local`) and
`DELETE /services/contenttypes/{idOrName}/fields/{fieldName}`. Duplicate field
names are **409**. Unlocked is **409**. System and shared fields cannot be removed
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
fields (backend column + display mapping). Control property values use
`GET`/`PUT /services/sharedfields/{idOrName}/fields/{fieldName}/controlProperties`
(request lock released on save). **Developer → Shared Fields** chrome can
create, save, and delete a **group**; nested field and control-property
editors are not in that chrome. The content-editor **system definition** (global system fields) is a
separate singleton: Admin-only `GET /services/systemdef` and
`PUT /services/systemdef` (patch existing field properties under a request lock).
Nested `POST /services/systemdef/fields` and
`DELETE /services/systemdef/fields/{fieldName}` add or remove system fields
(backend column on `CONTENTSTATUS` plus display mapping). POST creates the
column when it is missing; DELETE drops it when present and still succeeds if
the column was never created (other drop failures do not save the catalog).
Field names cannot be SQL reserved words (`SELECT`, `USER`, `TABLE`, `ORDER`).
Duplicate field names are **409**.
System-mandatory and system-internal fields cannot be deleted (**400**).
**Developer → System definition** exposes save / add / delete for those field
properties (request lock released on save). Control, stylesheet, and flow
editors are not in that chrome. See
[Developer System Def](id:admin-developer-system-def). Admin `GET /services/contenttypes/{idOrName}/export`
downloads Workbench-equivalent design XML (CD-14; no lock steal). REST import of
that XML is `POST /services/contenttypes/import` (above). Developer **Content
types** chrome exposes the same pair: **Export XML** on the detail toolbar and
**Import XML** on the catalog (create only; duplicate name **409**; invalid XML
**400**). See
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
unchanged. Apply-when conditions remain read-only. Field **choice catalogs**
(source, local entries, filter, null-entry, default-selected) are edited
from **Control property values** after **Lock** (see **Field choice catalog**
below).

## Product path — create a content type

1. Sign in as **Admin**.
2. Open **Developer → Content types**, or deep-link
   `spa.jsp?entry=developer&section=content-types`.
3. Click **New content type**.
4. Enter a unique **Name** (letters, digits, underscore, and period; no spaces).
   Optional **Label**, **Description**, and **Enabled** (defaults to on).
5. Click **Create content type**. The product calls
   `POST /services/contenttypes` and opens the new type's detail panel.
   **Back to list** shows the type in the catalog.
6. Duplicate names (including reserved types such as **Folder**) show an error
   (**409**). Invalid names (blank, spaces, wildcard) cannot be submitted; if
   REST rejects them they are **400**. Non-Admin callers are **403**.

## Product path — rename a content type

Admins rename a type from Developer Content Type **detail** after holding the
design-session lock. **Save content type** does **not** change the internal
name (bulk `PUT /services/contenttypes/{idOrName}` still does not rename).

1. Open the type and click **Lock**. Status becomes **Locked by you**.
2. Change **Name** (letters, digits, underscore, and period; no spaces) and
   click **Rename content type**. The product calls
   `PUT /services/contenttypes/{idOrName}/name` (Jackson root `ContentTypeName`)
   while the lock is still held.
3. A following `GET /services/contenttypes/{newName}` is **200**. `GET` by the
   previous name is **404**. `GET` by id returns the new name.
4. Duplicate or reserved names (for example **Folder**, or another catalog type)
   show an error (**409** on the SPA; REST may also return **400** with an
   already-exists message). Blank, spaces, or wildcard names cannot be
   submitted; if REST rejects them they are **400**. Non-Admin callers are
   **403**.
5. Without a lock, Name stays read-only and Rename stays **disabled**. An
   unlocked REST `PUT .../name` is **409** and does not acquire the lock.

## Product path — lock, save, unlock

1. Sign in as **Admin** (or another user with Developer access and Admin REST
   rights for design lock/save).
2. Open **Developer → Content types**, or deep-link
   `spa.jsp?entry=developer&section=content-types`.
3. Click **Open** on a catalog row (the type label), or create a type (above).
   The detail panel includes
   **Object ACL** as soon as the type opens, including while the field catalog
   is still loading.
4. The **detail toolbar at the top of the panel** (sticky) shows **Lock**,
   **Save content type**, **Unlock**, **Export XML**, **Rename content type**,
   **Delete content type**, the **Enabled** checkbox, **Search indexing**, and
   **Icon strategy**. The type name and
   **Allowed templates** add/remove chrome are visible immediately (add/remove
   stay **disabled** until the type body has loaded and you hold the lock). The
   status line starts as **Not locked**. Label, description, enabled, type-level
   search indexing, icon strategy, field flags, and association editors stay
   **read-only** until you hold the lock. You do not need to scroll past the
   fields table to lock or save. Enabled, Search indexing, and Icon strategy
   stay disabled until you hold the lock; a failed lock (**409**) does not steal
   another user's lock or enable the checkboxes, icon source/value, template
   add/remove, control property value editors, or Save.
5. Click **Lock**. Status becomes **Locked by you**. If another user already
   holds the lock, the panel shows an error and Save stays disabled (the product
   does **not** steal the lock).
6. Change the **Description**, **Enabled** checkbox, **Search indexing**
   checkbox, **Icon strategy**, and any other unlocked fields, then click
   **Save content type**. Save writes while the lock is still held. It does
   **not** unlock. **Enabled** is written with a dedicated
   `PUT /services/contenttypes/{idOrName}/enabled` (CD-13), not the bulk
   content-type save. **Search indexing** is the Workbench Properties **Enable
   searching for this Content Type** flag (root field-set `isUserSearchable`;
   default on). It is written with
   `GET`/`PUT /services/contenttypes/{idOrName}/searchIndexing` (CD-10) after a
   held lock, and is **not** the per-field **Searchable** column on the fields
   table. **Icon strategy** is written with
   `PUT /services/contenttypes/{idOrName}/icon` (CD-11; `none` / `specified` /
   `fromFileField`). `none` clears the value. A blank value when source is not
   `none` is **400**. This chrome does **not** upload icon binaries. Without a
   lock both checkboxes and icon editors stay disabled and a toggle cannot
   persist. An unlocked or lost-lock save is **409** and the panel shows the
   error. If `GET .../searchIndexing` fails (403 / 404 / 5xx), the checkbox
   stays at the Workbench default (**on**) and the panel shows a non-blocking
   load notice. A failed search-indexing save restores the last loaded value.
7. To change **Allowed templates**, follow **Allowed templates (after lock)**
   below. Save does **not** unlock.
8. Click **Unlock** when you are done. Status returns to **Not locked** and the
   form is read-only again. **Back to list** also releases a lock you hold.

### Icon strategy (after lock)

The **Icon strategy** control is read-only until you hold the lock. **Source**
and **Value** stay **disabled** while status is **Not locked**. After **Lock**:

1. Choose **None**, **Specified file**, or **From file field**.
2. For **Specified file**, enter a file path or name (for example
   `rx_resources/images/ContentTypeIcons/page.gif`). For **From file field**,
   enter the file field name (for example `item_file_attachment`). **None**
   clears the value; the value field is disabled.
3. Click **Save content type**. Save writes
   `PUT /services/contenttypes/{idOrName}/icon` while the lock is still held.
   A blank value when source is not **None** is **400**. Unlocked or another
   user's lock is **409**. This chrome does **not** upload icon binaries.

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

### Local fields (add / delete after lock)

The **Fields** catalog lists local, system, and shared fields. **Add local
field** and **Delete** on a local row stay **disabled** until you hold the
lock. Origin of a created field is always **local**. Include of existing
system or shared fields is REST-only (`POST .../fields/include`) in this
release.

After **Lock**:

1. Enter a unique field **name** (letter, then letters, digits, or underscore),
   optional **label**, **data type** (default `text`), and **control** (default
   `sys_EditBox`), then click **Add local field**. The product POSTs
   `POST /services/contenttypes/{idOrName}/fields` while the lock is still
   held and does **not** unlock. The field catalog shows the new row. A
   following `GET /services/contenttypes/{idOrName}` includes it.
2. Duplicate names return **409**. The lock is still held. Fix the name and
   retry, or **Unlock**.
3. Click **Delete** on a **local** field and confirm in the in-app dialog
   (not a browser prompt). The product DELETEs
   `DELETE /services/contenttypes/{idOrName}/fields/{fieldName}` (`204`). The
   catalog omits that field. System and shared fields have no Delete control
   (REST **400** if you try).
4. Without a lock, Add / Delete stay **disabled**. The product does **not**
   steal another user's lock (lock failure is **409**). An unlocked add or
   delete does not persist.

This is not the full Workbench field editor. Child field-set reorder and
include of system/shared fields are later slices.

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
   held. If you did **not** change the choice catalog, Save omits `choices` so
   existing choices stay unchanged. A following GET on the same path lists the
   new values.
4. Without a lock, value editors and Save stay **disabled**. The product does
   **not** steal another user's lock (lock failure is **409**). An unlocked
   edit does not persist.

### Field choice catalog (after lock)

The **Choice catalog** editor is under **Control property values** for the same
selected field. Source, local entries, keyword id, lookup, table, null-entry,
default-selected, and filter stay **disabled** until you hold the lock.

After **Lock**:

1. Select the field. The product loads the current catalog from the same
   `GET .../controlProperties` response (`choices` is omitted when the field
   has none).
2. Choose a source:
   * **None (clear catalog)** — next save sends `choices.type` `none` and
     removes the catalog.
   * **Local list** — add value/label entries.
   * **Keyword (global)** — set the keyword / lookup table id.
   * **Lookup** / **Internal lookup** — set the lookup href (optional name).
   * **Table** — set table name, label column, and value column.
3. Optionally include a **null entry**, **default selected** rows, and a
   **choice filter** (lookup href plus dependent fields).
4. Click **Save content type**. When the catalog changed, the product sends
   `choices` on `PUT .../fields/{fieldName}/controlProperties` while the lock
   is still held. A following GET lists the new catalog. A properties-only
   save still omits `choices`, so the catalog is not cleared.
5. Without a lock, choice editors and Save stay **disabled**. The product does
   **not** steal another user's lock (lock failure is **409**).

This is not the full Workbench Properties visual chooser. Shared-field choice
editors are a separate surface. Integrator notes:
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

## Export or import design XML

**Export** does **not** require a design lock and does **not** steal one.

1. Open **Developer → Content types** and open a type.
2. Click **Export XML** on the detail toolbar. The browser downloads
   Workbench-equivalent `ItemDefData` XML (`GET /services/contenttypes/{idOrName}/export`).
   The filename is derived from the type name (for example `percPage.xml`).
3. Unknown types are **404**. Non-Admin callers are **403**.

**Import** is **create only**. It does **not** overwrite an existing type.

1. Return to the catalog (**Back to list**).
2. Under **Import XML**, choose an `ItemDefData` XML file.
3. Enter a **unique name** (no spaces or wildcards) when the XML name already
   exists on the server. The chrome rewrites `PSXItemDefSummary@name` before
   `POST /services/contenttypes/import`.
4. Click **Import content type**. The catalog reloads and lists the new type.
5. Invalid XML is **400**. A name that already exists is **409** (no replace).
   The product does **not** steal locks on existing types.

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

Integrators can also rename with REST after a held design-session lock
(the same contract the SPA uses):

1. `POST /services/contenttypes/{idOrName}/lock`
2. `PUT /services/contenttypes/{idOrName}/name` with Jackson root
   `ContentTypeName` (`name` required). The new name must be unique
   (case-insensitive) and must not contain spaces.
3. `GET /services/contenttypes/{id}` returns the new name. `GET` by the
   previous name is **404**.
4. `POST .../unlock` when done.

Bulk `PUT /services/contenttypes/{idOrName}` still does **not** change name.
Unlocked or another user's lock is **409**. Invalid names (blank, spaces,
wildcard) are **400**. Duplicate catalog names are **400** on REST (already
exists) and **409** when the SPA maps a duplicate/reserved conflict.

## Delete a content type

Admins delete a type from Developer Content Type **detail** after holding the
design-session lock. The catalog **Delete** control stays disabled until **Lock**
succeeds. The product does **not** steal another user's lock.

1. Open the type and click **Lock**. Status becomes **Locked by you**.
2. Click **Delete content type** and confirm in the in-app dialog (not a
   browser prompt). The product calls
   `DELETE /services/contenttypes/{idOrName}`. Success is **204**; the catalog
   omits the type and a following `GET /services/contenttypes/{idOrName}` is
   **404**.
3. Without a lock, Delete stays **disabled**. An unlocked REST `DELETE` is
   **409** and does not acquire the lock.
4. If another user already holds the lock, **Lock** and **Delete** are **409**.
   The lock is not stolen.
5. Types that still have dependents fail with **400**. The product does **not**
   cascade-delete items. Non-Admin callers are **403**.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| Create | `POST /services/contenttypes` (Admin; unique name, no spaces; 409 duplicate; 400 invalid; 403 non-Admin) |
| Lock | `POST /services/contenttypes/{idOrName}/lock` |
| Save (label, description, fields) | `PUT /services/contenttypes/{idOrName}` (requires a held lock; does not send `enabled`, type-level `searchIndexing`, `allowedWorkflows`, or `allowedTemplates`) |
| Enable / disable | `PUT /services/contenttypes/{idOrName}/enabled` (CD-13; requires a held lock; does not acquire or release it) |
| Type-level search indexing | `GET` / `PUT /services/contenttypes/{idOrName}/searchIndexing` (CD-10; Developer detail **Search indexing** checkbox after lock; PUT requires a held lock; default on; not the per-field searchable flag) |
| Load icon strategy | `GET /services/contenttypes/{idOrName}/icon` (CD-11; no lock; `none` / `specified` / `fromFileField`) |
| Set icon strategy | `PUT /services/contenttypes/{idOrName}/icon` (CD-11; Admin; held lock; `none` clears value; blank non-none is **400**; no binary upload; SPA Properties picker after lock) |
| Rename | `PUT /services/contenttypes/{idOrName}/name` (CD-01; Admin; held lock; unique name, no spaces; bulk PUT does not rename; SPA **Rename content type** on detail) |
| Save allowed workflows | `PUT /services/contenttypes/{idOrName}/allowedWorkflows` (requires a held lock; does not unlock) |
| Replace allowed templates | `PUT /services/contenttypes/{idOrName}/allowedTemplates` (held lock; full replace) |
| Confirm allowed templates | `GET /services/contenttypes/{idOrName}/allowedTemplates` |
| Replace item-level exits | `PUT /services/contenttypes/{idOrName}/itemExits` (CD-09; held lock; full replace of translations/validations; empty lists clear) |
| Confirm item-level exits | `GET /services/contenttypes/{idOrName}/itemExits` |
| Load field control properties | `GET /services/contenttypes/{idOrName}/fields/{fieldName}/controlProperties` (CD-07; no lock) |
| Save field control properties | `PUT /services/contenttypes/{idOrName}/fields/{fieldName}/controlProperties` (held lock; full replace of values; omit `choices` to leave the catalog unchanged; `type: none` clears) |
| Load field rule expressions | `GET /services/contenttypes/{idOrName}/fields/{fieldName}/ruleExpressions` |
| Save field rule expressions | `PUT /services/contenttypes/{idOrName}/fields/{fieldName}/ruleExpressions` (held lock; full replace of validation, visibility, inputTranslation, outputTranslation) |
| Include system or shared field | `POST /services/contenttypes/{idOrName}/fields/include` (CD-04; held lock; origin stays system/shared; duplicate 409; unknown catalog field 404; invalid `fieldType` 400) |
| Add local field | `POST /services/contenttypes/{idOrName}/fields` (CD-03; held lock; origin always `local`; duplicate name **409**) |
| Delete local field | `DELETE /services/contenttypes/{idOrName}/fields/{fieldName}` (CD-03; held lock; **204**; system/shared **400**) |
| Unlock | `POST /services/contenttypes/{idOrName}/unlock` |
| Export design XML | `GET /services/contenttypes/{idOrName}/export` (Admin; no lock; `application/xml` attachment) |
| Import design XML | `POST /services/contenttypes/import` (Admin; create-only ItemDefData XML; 400 invalid; 409 duplicate) |
| Delete | `DELETE /services/contenttypes/{idOrName}` (Admin; held lock; 204; 409 if unlocked or another user holds the lock; 400 if dependents; SPA Delete on detail after Lock) |

Integrator notes: [REST API — Content types](id:developer-rest). Object ACL on
the same detail panel: [Object ACL & default template](id:admin-object-acl).

See [REST API — Content types](id:developer-rest) for the dedicated
`itemExits`, `controlProperties`, and `ruleExpressions` surfaces.
