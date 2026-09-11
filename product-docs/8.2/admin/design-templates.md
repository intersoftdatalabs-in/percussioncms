---
id: admin-design-templates
title: Design templates
description: Create, edit, and delete modern assembly templates; choose HTML-first, Markdown, or Velocity assemblers (prefer over Legacy/XSL)
version: "8.2"
order: 44
tags: [admin, design, templates, ui, assembler]
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
4. Choose an **Assembler**. New templates default to **HTML-first** (recommended, ★).
   Markdown and Velocity are also recommended modern assemblers. Prefer those over
   **Legacy / XSL**. See [Choose an assembler](#choose-an-assembler-html-first-markdown-velocity).
5. Choose **Create**.

The catalog list refreshes and shows the new row. Open the row to edit assembler, slots,
source, and JEXL bindings. If you omit **Assembler** on REST create
(`POST /services/templates`), the server also defaults to HTML-first
(`Java/global/percussion/assembly/htmlAssembler`).

Create persists through `POST /services/templates` with a `TemplateDetail` body. The server
stores a shared assembly template (package/manifest model). **No Widget Builder XML file is
written.**

If create fails (duplicate name, invalid name, or server error), the dialog stays open and
shows an operator-facing message. Correct the fields and try again.

## Choose an assembler (HTML-first, Markdown, Velocity)

Each assembly template stores an **assembler** (a render plugin). Percussion CMS 8.2
evaluates the template’s **JEXL bindings** in order, then the assembler turns the
template **source** into output (usually HTML).

On **Design**, the **Assembler** list on **Create template** and on the template
editor shows the catalog below. Recommended modern choices are marked with a star
(★). The editor option text also includes the short extension name
(`htmlAssembler`, `markdownAssembler`, `velocityAssembler`, …).

**Prefer HTML-first, Markdown, or Velocity for new templates.** Use **Legacy / XSL**
only when an existing stylesheet-based template still needs that path. Do not start
new Design work on Legacy / XSL.

### Recommended modern assemblers

| Assembler (picker) | Extension stored on the template | Use when | Source language |
|--------------------|----------------------------------|----------|-----------------|
| **HTML-first** ★ (default) | `Java/global/percussion/assembly/htmlAssembler` | Mostly static HTML with a few bound values; no Velocity macros | HTML plus <code>&#36;{dotted.path}</code> placeholders |
| **Markdown** ★ | `Java/global/percussion/assembly/markdownAssembler` | Prose or structured text that should become HTML | CommonMark Markdown; placeholders first, then Markdown → HTML |
| **Velocity** ★ | `Java/global/percussion/assembly/velocityAssembler` | Macros, loops, `#parse`, Active Assembly slot macros, existing Velocity snippets | Velocity 2.x plus JEXL-bound `$` variables |

HTML-first is the **create default** on Design and on `POST /services/templates` when
the body omits `assembler`.

### How to choose

1. You need macros, loops, `#parse`, or Active Assembly Velocity macros → **Velocity**.
2. The body is mostly fixed HTML with a handful of field or system values → **HTML-first**.
3. The body is long-form content (headings, lists, links) that should render as HTML → **Markdown**.
4. The template is an existing XSL / XML-application variant that still works → leave **Legacy / XSL**.
   Do not convert it on this page. See [XSL and legacyAssembler support](id:admin-xsl-legacy-assembler)
   for the 8.2 support statement and a short migration cookbook.

You can change the assembler later on the Design editor. Changing assembler does **not**
rewrite source automatically — update **Template source** so it matches the new language
(HTML placeholders, Markdown, or Velocity), then **Save**.

### HTML-first and Markdown placeholders

HTML-first and Markdown fill **only** dollar-brace dotted paths from JEXL binding
results (missing keys become empty).

| Rule | Detail |
|------|--------|
| Form | <code>&#36;{title}</code>, <code>&#36;{sys.mimetype}</code> |
| Not supported | Bare `$title` (too easy to confuse with HTML or scripts), Mustache `{{ }}`, Velocity directives (`#if`, `#foreach`, `#parse`) |
| Lookup | Binding variable `title` or `$title`; nested maps via `sys` / `$sys` then child keys |
| Missing | Empty string (the token does not stay in the output) |

Do **not** paste Velocity macros into an HTML-first or Markdown template. If you need
`#parse` or slot macros, switch the assembler to **Velocity** and author Velocity source.

Markdown runs **placeholders first**, then CommonMark → HTML. Write Markdown in the
source editor (headings, lists, links). HTML-first leaves the source as HTML after
placeholder substitution (no Markdown pass).

### JEXL bindings

Bindings stay **JEXL** for HTML-first, Markdown, and Velocity. On the Design editor,
add, edit, or remove rows (variable + expression), then **Save** (full replace).
Bindings run **before** the assembler renders source.

Typical modules you will see in expressions:

| Module | Role |
|--------|------|
| `$sys` | Assembly context (`template`, `mimetype`, `charset`, `site`, slot helpers, …) |
| `$rx` | JEXL tools (`asmhelper`, `codec`, `link`, `location`, `nav`, `string`, …) |
| `$perc` | CM1 page context (regions / widgets / theme helpers) — mainly with **Page (CM1)** |

### Change the assembler on an existing template

1. Open **Developer → Design** (template library) and open the template row.
2. Under **Assembler**, choose **HTML-first**, **Markdown**, or **Velocity** (★).
3. Adjust **Template source** for that language. For Velocity, you can insert built-in
   macros from **Developer → Templates** (**Insert snippet**). See
   [Developer Templates](id:admin-developer-templates).
4. Confirm **JEXL bindings** still match the names you reference in source.
5. Choose **Save**. Success and validation stay on the editor.

**Developer → Templates** shows the stored assembler extension name as read-only
metadata. Change the assembler on **Design**, then reopen Developer Templates if you
need export XML or snippet insert.

### Other entries in the Assembler list

These remain in the picker for existing templates and specialized output. They are
**not** the default for new snippet-style design.

| Assembler (picker) | Extension | Operator note |
|--------------------|-----------|----------------|
| **Page (CM1)** | `Java/global/percussion/assembly/pageAssembler` | CM1 page context and `$perc` (page/region composition). Keep on shipped page templates such as `perc.page`. It is page context plus a text render path — not a fourth authoring language. |
| **Legacy / XSL** | `Java/global/percussion/assembly/legacyAssembler` | Compatibility only. Existing XML applications and stylesheets continue to run. Do not choose this for new templates. Support statement and migration: [XSL and legacyAssembler support](id:admin-xsl-legacy-assembler). |
| **Binary** | `Java/global/percussion/assembly/binaryAssembler` | Binary / resource output. |
| **Dispatch** | `Java/global/percussion/assembly/dispatchAssembler` | Dispatch to another template. |
| **Database** | `Java/global/percussion/assembly/databaseAssembler` | Database result-set output. |

If a template already uses a custom extension that is not in this list, Design keeps
that value as **Current (custom)** so you can save without forcing a catalog assembler.

### What not to do

- Do not delete or disable **Legacy / XSL** on the server to “clean up” — existing
  sites may still assemble through it. See
  [XSL and legacyAssembler support](id:admin-xsl-legacy-assembler).
- Do not treat definition-XML shims or Workbench XML export as the way to pick a
  modern assembler. Create and edit assemblers on Design (or REST `assembler` on
  `POST` / `PUT /services/templates`).
- Do not mix Velocity directives into HTML-first or Markdown source.

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

- Change the **assembler** (prefer HTML-first, Markdown, or Velocity — see
  [Choose an assembler](#choose-an-assembler-html-first-markdown-velocity))
- Edit **slot** layout and styles (orientation, columns, classes)
- Edit Velocity / HTML / Markdown **source** to match the assembler
- Add, edit, or remove **JEXL bindings** (saved as a full replace)

Choose **Save**. Success and validation errors stay on the editor. Use **Templates** to
return to the library. **Delete** on the editor asks for confirmation, then returns you
to the refreshed library.

For the built-in **Velocity snippet library** (AS-09 insert into source), use
**Developer → Templates** — see [Developer Templates](id:admin-developer-templates).

The visual layout editor may still open residual classic hosts (`editTemplate.jsp` and
related upgrade-only JSPs) until those flows are signed off on the SPA. Bookmarks to the
**list** still land on the SPA.

## Related

- [XSL and legacyAssembler support](id:admin-xsl-legacy-assembler) — 8.2 XSL support statement and migration cookbook
- [Developer Templates](id:admin-developer-templates) — catalog export/import XML and Velocity snippet insert
- [REST API](id:developer-rest) — `GET`/`POST`/`PUT`/`DELETE /services/templates`, Admin `GET .../export`, and `POST /services/templates/import`
- [Extensions & packages](id:developer-extensions)
- [Developer Extensions](id:admin-developer-extensions)
- [Product page packages](id:developer-page-packages)
- [Convert definition XML (dual-run)](id:admin-definition-xml-dual-run) — customer Widget XML → modern packages (shim stays)
- [Glossary](id:reference-glossary)
- [Navigation & site structure](id:admin-architecture-navigation)
- [Administration](id:admin)
