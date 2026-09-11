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

## Edit from detail

Open a template row to **Lock**, then change label, description, template source, JEXL
bindings, contained slots, and **content-type associations**, then **Save template**.
**Export XML** is available on the same toolbar. Object ACL for the template is on the
detail panel.

### Content-type associations

The **Content types** section lists each associated content type with **name** and **GUID**.
Browse is available without a lock. After **Lock**:

1. Enter a content type **name** (for example `percImage`) or a GUID (`0-6-311`) and choose
   **Add content type**.
2. Choose **Remove** on a row to drop an association.
3. **Save template** replaces the full set. An empty list **clears** all associations.
   Omitting the field on REST leaves associations unchanged.

Unlocked save is **409**. Unknown content-type names or GUIDs are **400**. Non-Admin is
**403**. Unknown templates are **404**. Save does **not** release the lock.

Integrators read and write the same list on `GET` / `PUT /services/templates/{idOrName}` as
`associatedContentTypes` (see [REST API](id:developer-rest)). The inverse association
(content type → templates) remains on [Developer Content Types](id:admin-developer-content-types).

Automated H2 surface coverage lives in
`modules/perc-qa-automation/frontend/tests/developer-template-content-type-assoc.spec.js`.

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
