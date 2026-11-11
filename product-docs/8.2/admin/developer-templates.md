---
id: admin-developer-templates
title: Developer Templates
description: Lock, save, and unlock assembly templates from Developer Templates; browse and persist content-type associations; export and import design XML; edit source with Velocity snippet insert, bindings, and slots; assembler choice lives on Design
version: "8.2"
order: 45
tags: [admin, developer, templates]
---

# Developer Templates

**Developer → Templates** lists assembly templates from the public REST catalog
(`GET /services/templates`). Open a row to **lock**, edit label, description, source, JEXL
bindings, contained slots, and **content-type associations**, **save while the lock is held**,
then **unlock**. This catalog is **list + open** plus **AS-08 import and export**,
**design-session lock**, **content-type associations**, and **AS-09 snippet library** insert
into template source. Create and delete of modern templates stay on
[Design templates](id:admin-design-templates) (`POST` / `DELETE /services/templates`).

## Product path — catalog

1. Sign in as **Admin** (export and import require the Admin role).
2. Open **Developer → Templates**, or deep-link
   `spa.jsp?entry=developer&section=templates`.
3. The catalog lists label, name, id, and description. Open a row to edit.

If the catalog is empty, **Import XML** is still available so you can create the
first template from a design document.

## Load failure and retry

The catalog rows render only after `GET /services/templates` succeeds.
When that load fails, the panel names the failure (**Could not load
templates**, plus the status or transport reason) instead of showing an empty
catalog, and offers **Retry**, which reloads the list. There is no REST gap
behind this screen: both the thin list (`GET /services/templates`) and the
row detail (`GET /services/templates/{idOrName}`) are public REST. Write and
design-lock gaps (label/description save, source, bindings, slots,
associations) are documented under their own sections below, not here.

## Readback

The catalog table is a thin read: it lists label, name, id, and description
from `GET /services/templates` and carries no source, binding, slot,
association, or site payload. Opening a row loads the detail from
`GET /services/templates/{idOrName}` and shows the title, the committed name
with its numeric id and object GUID, the label and description text, and the
assembler. Packaged templates read back with zero writes: the row renders
from the thin list and the detail renders without creating, locking,
importing, or deleting anything. A template created through
`POST /services/templates` reads back in the detail (title, committed name,
label, description, defaulted assembler) and as a new catalog row; removing
it is `DELETE /services/templates/{idOrName}` (`204` on success), after which
the catalog no longer lists that name.

## Export template design XML (AS-08)

Export is read-only and does **not** steal a design lock.

1. Open a template from the catalog.
2. Choose **Export XML** on the detail toolbar.
3. The browser downloads Workbench-equivalent `<assembly-template>` XML named
   from the template (for example `perc.page.xml`).

Unknown names are **404**. Non-Admin sessions are **403**. Integrators can also
call `GET /services/templates/{idOrName}/export`.

## Import design XML (create only)

Import creates a **new** uniquely named template. It does **not** overwrite an
existing template and does **not** steal a design lock.

1. On the Templates catalog, choose a Workbench-equivalent
   `<assembly-template>` file under **Design XML file**.
2. Optionally enter a **Unique name** (must start with a letter; letters, digits,
   `.`, `_`, or `-` only; no spaces). If you leave it blank, the name in the XML
   is used.
3. Choose **Import template**.

| Result | What you see |
|--------|----------------|
| Success | **Template imported.** The catalog lists the new name. |
| Invalid XML | **400** — Invalid assembly-template design XML. |
| Duplicate name | **409** — A template with that name already exists (no overwrite). |
| Not Admin | **403** — Admin role is required. |

Integrators can also call `POST /services/templates/import` with
`Content-Type: application/xml`. See [REST API](id:developer-rest).

## Lock, save, and unlock

Template PUT requires a design-session lock owned by the current Admin user
(the same pattern as content types). Save does **not** release the lock.

1. Open a template from the catalog.
2. Choose **Lock**. Status shows **Locked by you**.
3. Change label, description, source, bindings, slots, or **content-type associations**, then
   **Save template**.
4. Choose **Unlock** when you are done. **Back** also releases a lock you still hold.

Unlocked save is refused (**409**). A lock held by another designer is **409** and is not
stolen. Non-Admin sessions are **403**. Unknown names are **404**.

Integrators can call `POST /services/templates/{idOrName}/lock`, `PUT
/services/templates/{idOrName}`, and `POST /services/templates/{idOrName}/unlock`.

## Set a template description

**Save description** writes the description only. Lock the template first.

1. Open an existing template and choose **Lock**.
2. Choose **Set template description**. The description on the page stays the stored value until save succeeds.
3. Enter a new description, or leave it blank to clear the stored description. **Cancel** does not call the server. Saving the same description does not call the server.
4. Choose **Save description**. The request is `PUT /services/templates/{idOrName}` with the description only. Name, label, bindings, slots, content-type associations, and source are omitted, so they stay. After a successful save, the detail and the catalog description show the new value. HTTP 400, 403, and 409 leave the previous description.

**Save template** still saves the other fields on the form, including a description typed in the description box. **Save description** does not change those fields.

## Set a template label

**Save label** writes the label only. Lock the template first.

1. Open an existing template and choose **Lock**.
2. Choose **Set template label**. The label on the page stays the stored value until save succeeds.
3. Enter a new label. **Cancel** does not call the server. Saving the same label does not call the server.
4. Choose **Save label**. The request is `PUT /services/templates/{idOrName}` with the label only. Name, description, bindings, slots, content-type associations, and source are omitted, so they stay. After a successful save, the detail and the catalog label show the new value. HTTP 400, 403, and 409 leave the previous label.
5. A blank label clears the stored label and does not change the template name. The catalog returns the template name when the stored label is blank, so the page shows that name. The name itself is not cleared.

**Save template** still saves the other fields on the form. **Save label** does not change those fields.

## Edit from detail

Open a template row to **Lock**, then change label, description, template source, JEXL
bindings, contained slots, and **content-type associations**, then **Save template**.
**Export XML** is available on the same toolbar. Object ACL for the template is on the
detail panel.

### Assembler label

The detail header shows the template **assembler** as a normalized label instead of the
raw extension path: **Velocity (power path)**, **HTML-first**, **Markdown**,
**Legacy XSL**, **Page**, **Page variant**, **Page database**, **Binary**, **Dispatch**, **Database**,
**Resource**, or **Debug**. A custom assembler outside this catalog still shows its stored
value so existing templates round-trip; a blank assembler shows an em-dash. The label is
display-only — it never changes the stored value. To change the assembler, use
[Design templates](id:admin-design-templates).

Automated H2 surface coverage lives in
`modules/perc-qa-automation/frontend/tests/developer-template-assembler-label.spec.js`
(`npm run test:surface -- --path tests/developer-template-assembler-label.spec.js`).

### Content-type associations

The **Content types** section lists each associated content type with **name** and **GUID**.
Browse is available without a lock. After **Lock**:

1. Enter a content type **name** (for example `percImage`) or a GUID (`0-6-311`) and choose
   **Add content type**.
2. Choose **Remove** on a row to drop an association.
3. **Save template** replaces the full set (the SPA always sends the remaining
   `associatedContentTypes` list, including an empty list after the last **Remove**).
   An empty list **clears** all associations. Omitting the field on REST leaves
   associations unchanged. After **Save**, a subsequent GET of the same template
   must match the remaining list (add and remove both persist).

Unlocked save is **409**. Unknown content-type names or GUIDs are **400** (the rest of
the save is not applied). Non-Admin is
**403**. Unknown templates are **404**. Save does **not** release the lock. Add by GUID
sends the GUID only (name is filled after a successful save). If association load
fails, the list may be empty and the detail shows design gap `TPL_CT_ASSOC_LOAD`.

Integrators read and write the same list on `GET` / `PUT /services/templates/{idOrName}` as
`associatedContentTypes` (see [REST API](id:developer-rest)). The inverse association
(content type → templates) remains on [Developer Content Types](id:admin-developer-content-types).

Automated H2 surface coverage lives in
`modules/perc-qa-automation/frontend/tests/developer-template-content-type-assoc.spec.js`.

### Contained slots

The **Slots** section lists every catalog slot with a **Member** checkbox for
whether the template contains it. Browse is available without a lock. After
**Lock**:

1. Check one unassigned slot to assign it to the template (or uncheck a member
   to remove it). Other members, label, description, source, bindings, and
   content-type associations stay.
2. **Save template** replaces the full set (the SPA sends the remaining `slots`
   list only when a checkbox changed; omitting the field on REST leaves slot
   membership unchanged). After **Save**, a subsequent GET of the same template
   must list the assigned slot (assign and remove both persist).

Unlocked save is refused (**Save template** stays disabled without a held lock,
and a stolen lock is **409**). Unknown slot names are **400** (the rest of the
save is not applied). Non-Admin is **403**. Unknown templates are **404**. Save
does **not** release the lock. Integrators read and write the same list on
`GET` / `PUT /services/templates/{idOrName}` as `slots`
(see [REST API](id:developer-rest)).

Automated H2 surface coverage lives in
`modules/perc-qa-automation/frontend/tests/developer-template-slot-assign.spec.js`
(`npm run test:surface -- --path tests/developer-template-slot-assign.spec.js`).

The **assembler** extension name (for example
`Java/global/percussion/assembly/htmlAssembler`) is **read-only** on this
catalog. To create a template or to switch **HTML-first**, **Markdown**, or
**Velocity** (and to avoid **Legacy / XSL** for new work), use
[Design templates](id:admin-design-templates). Existing XSL variants that still
work stay on Legacy / XSL until you follow
[XSL and legacyAssembler support](id:admin-xsl-legacy-assembler).

Create and delete of modern assembly templates remain on
[Design templates](id:admin-design-templates) — this Developer catalog does not
add those actions.

### Site associations

The **Sites** section lists each site associated with the template for publishing
usage with **name** and **GUID**. Browse is available without a lock. The list is
**read-only** — site-association changes stay in Workbench until a follow-on slice.
Saving label, description, source, bindings, slots, or content-type associations
never sends `associatedSites`, and REST `PUT /services/templates/{idOrName}`
ignores it. Integrators read the same list on
`GET /services/templates/{idOrName}` as `associatedSites`
(see [REST API](id:developer-rest)). If site-association load fails, the list may
be empty and the detail shows design gap `TPL_SITE_ASSOC_LOAD`.

Automated H2 surface coverage lives in
`modules/perc-qa-automation/frontend/tests/developer-template-sites.spec.js`
(`npm run test:surface -- --path tests/developer-template-sites.spec.js`).

## Insert a Velocity snippet (AS-09)

While editing **Template source**, choose **Insert snippet** to open the built-in
**Snippet library**. The library loads the read-only catalog from
`GET /services/velocity/snippets` (field, slot, and misc macros aligned with
shipped assembly macros).

1. Open a template from **Developer → Templates**.
2. Place the caret (or select text to replace) in the source editor.
3. Choose **Insert snippet**.
4. Optionally filter by category (**Field** / **Slot** / **Misc**) or search by
   title, id, or insert text.
5. Select a row to preview the insert text, then choose **Insert** (or
   double-click the row).

The selected macro text is inserted at the caret (or replaces the selection).
Save the template when you are ready. Snippets are **Velocity** macros — they
belong on templates whose assembler is **Velocity**. Do not insert them into
HTML-first or Markdown source; change the assembler on
[Design templates](id:admin-design-templates) first if you need macros.

The snippet library does **not** edit
System/User Velocity configuration files (SY-02); it only inserts catalog text
into the template body. Integrators can call the same REST catalog directly —
see [REST API](id:developer-rest) (Velocity snippets).

Automated H2 surface coverage lives in
`modules/perc-qa-automation/frontend/tests/developer-template-snippet-library.spec.js`
(`npm run test:surface -- --path tests/developer-template-snippet-library.spec.js`).

## Related

- [Design templates](id:admin-design-templates) — create/delete and HTML-first / Markdown / Velocity assembler picker
- [XSL and legacyAssembler support](id:admin-xsl-legacy-assembler) — 8.2 Legacy/XSL support and migration cookbook
- [REST API](id:developer-rest)
- [Object ACL & default template](id:admin-object-acl)
- [Administration](id:admin)
