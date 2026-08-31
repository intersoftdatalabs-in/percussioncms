---
id: admin-design-templates
title: Design templates
description: Create, edit, and delete modern assembly templates in the Design SPA; classic list bookmarks redirect here
version: "8.2"
order: 44
tags: [admin, design, templates, ui]
---

# Design templates

**Design** is the SPA surface for the modern assembly **template library**. Designers and
administrators list existing templates, **create new templates**, and **delete** templates
using the public REST catalog (`/services/templates`). Create and delete do **not** author
Widget definition XML.

In Percussion CMS 8.2 the primary template-list entry is the SPA shell. The classic
`admin.jsp` Design page and `?view=design` bookmarks hard-redirect into the SPA.

## Open Design (SPA)

1. Sign in as an **Admin** or **Designer**.
2. Open **Developer** in the product top navigation and choose **Design** (template
   library). Design is not a top-nav item. You can also open:
   - Query contract: `spa.jsp?entry=design&section=templates`
   - Path route: `/cm/app/design` or `/cm/app/design/templates`
   - Legacy bookmarks: `/cm/app/?view=design` and `/cm/app/admin.jsp` always
     redirect here. `admin.jsp` forces `view=design` (a bookmark such as
     `admin.jsp?view=admin` still opens Design, not Admin). The classic CM1
     Design list is not a product entry for these bookmarks.
3. The **Templates** tab lists assembly templates (label, name, id, description).
   The shell loads under the same product top nav as Explorer, Navigation, Developer, Publish, and Admin.

A stored **Design** default landing (from before this chrome change) still opens
the Design SPA after sign-in. New profile and role landing lists do not offer
Design as a destination — pick **Developer** surfaces via the remaining top-nav
apps, or clear the override.

## Template library

The **Templates** tab lists assembly templates from `GET /services/templates`.

| State | What you see |
|-------|----------------|
| List | A table of templates. Open a row to edit. |
| Empty | **No templates found.** Use **Create template**. |
| Error | An operator-facing message. The rest of the Design shell stays usable. |

## Baseline system templates (fresh 8.2)

A new CMS install (including QA H2 via `perc-devctl qa-up`) loads `perc.Baseline` with seven
system assembly templates. On **first assign** those templates keep the archive GUIDs:

| Template | GUID (`stringValue`) |
|----------|----------------------|
| `perc.page` | `0-4-602` |
| `perc.pageDatabase` | `0-4-604` |
| `perc.pageDispatcher` | `0-4-606` |
| `perc.pageXml` | `0-4-608` |
| `perc.sys.resource` | `0-4-610` |
| `perc.widget` | `0-4-612` |
| `perc.widgetDispatcher` | `0-4-614` |

Those Baseline templates ship in a **native** page package (`page.installMode=native`). Product
page packages do not dual-ship root `*.templateDef` files; see
[Product page packages](id:developer-page-packages).

**Existing databases are not remapped.** If an earlier install already assigned sequential type-4
UUIDs (for example `0-4-1001` for `perc.page`), that row keeps its id. Package reinstall matches by
template name. Do not rewrite GUIDs on customer or snapshot databases to force the table above.

The Design catalog and `GET /services/templates` show the live id. On a fresh 8.2 host, `perc.page`
is `0-4-602`.

## Create a template (no Widget XML)

1. On the Templates library, choose **Create template**.
2. Enter a **Name** that starts with a letter and uses only letters, digits, `.`, `_`, or `-`.
   Names cannot contain spaces and must be unique.
3. Optionally enter a **Label** and **Description**. If you leave the label empty, the
   server uses the name.
4. Choose an **Assembler**. New templates default to **HTML-first** (recommended). Markdown
   and Velocity are also supported modern assemblers. Prefer those over Legacy / XSL.
5. Choose **Create**.

The catalog list refreshes and shows the new row. Open the row to edit assembler, slots,
source, and JEXL bindings.

Create persists through `POST /services/templates` with a `TemplateDetail` body. The server
stores a shared assembly template (package/manifest model). **No Widget Builder XML file is
written.**

If create fails (duplicate name, invalid name, or server error), the dialog stays open and
shows an operator-facing message. Correct the fields and try again.

## Delete a template

1. On the Templates library, choose **Delete** on the row you want to remove. You can
   also open the template and choose **Delete** on the editor.
2. Confirm in the dialog. Delete permanently removes the assembly template from the
   catalog. **No Widget definition XML is written.**
3. After a successful delete the library list refreshes. The deleted name is gone.

Delete uses `DELETE /services/templates/{idOrName}` (`idOrName` is the unique name or
numeric id). If delete fails (template in use, not found, or server error), the confirm
dialog stays open with an operator-facing message. Choose **Cancel** to leave the
template unchanged.

Lock and content-type associations remain out of scope on this REST surface (see
`designGaps` on the template detail payload).

## Export template design XML (AS-08)

The Design SPA does **not** include an export wizard. **Developer → Templates**
does: open a template and choose **Export XML**. Administrators can also download
Workbench-equivalent design XML with:

`GET /services/templates/{idOrName}/export`

The response is `application/xml` with `Content-Disposition` named from the template
(for example `perc.page.xml`). Unknown names return **404**. Non-Admin sessions return
**403**. Export is read-only and does **not** steal a design lock.

See [Developer Templates](id:admin-developer-templates) and
[REST API](id:developer-rest) (Templates / AS-08 export).

## Import design XML (create only)

Administrators can import **one** Workbench-equivalent assembly-template design XML
document from **Developer → Templates** (**Import XML** on the catalog) or through
public REST (AS-08):

`POST /services/templates/import` with `Content-Type: application/xml`.

The document is the same `<assembly-template>` XML Workbench exports (and that
`GET /services/templates/{idOrName}/export` returns). The imported **name** must be unique
— a collision is **409** (the existing template is not replaced, and no design lock is
stolen). Non-Admin callers receive **403**. Invalid XML is **400**.

There is no Design SPA import wizard. See
[Developer Templates](id:admin-developer-templates) and [REST API](id:developer-rest).

## Edit assembler, slots, source, and bindings

Open a template row from the library. The editor (same Design tab) lets you:

- Change the **assembler**
- Edit **slot** layout and styles (orientation, columns, classes)
- Edit Velocity / HTML / Markdown **source**
- Add, edit, or remove **JEXL bindings** (saved as a full replace)

Choose **Save**. Success and validation errors stay on the editor. Use **Templates** to
return to the library. **Delete** on the editor asks for confirmation, then returns you
to the refreshed library.

The visual layout editor may still open residual classic hosts (`editTemplate.jsp` and
related upgrade-only JSPs) until those flows are signed off on the SPA. Bookmarks to the
**list** still land on the SPA.

## Related

- [Developer Templates](id:admin-developer-templates) — catalog export/import XML
- [REST API](id:developer-rest) — `GET`/`POST`/`PUT`/`DELETE /services/templates`, Admin `GET .../export`, and `POST /services/templates/import`
- [Extensions & packages](id:developer-extensions)
- [Product page packages](id:developer-page-packages)
- [Navigation & site structure](id:admin-architecture-navigation)
- [Administration](id:admin)
