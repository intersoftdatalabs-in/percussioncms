---
id: admin-design-templates
title: Design templates
description: Create and edit modern assembly templates in the Design SPA without Widget definition XML
version: "8.2"
order: 44
tags: [admin, design, templates, ui]
---

# Design templates

**Design** is the SPA surface for the modern assembly **template library**. Designers and
administrators list existing templates and **create new templates** using the public REST
catalog (`/services/templates`). Create does **not** author Widget definition XML.

The product name is **Design**. The SPA route is `/design` (and
`spa.jsp?entry=design&section=templates`).

## Open Design (SPA)

1. Sign in as an **Admin** or **Designer**.
2. Choose **Design** in the product top navigation, or open:
   - Query contract: `spa.jsp?entry=design&section=templates`
   - Path route: `/cm/app/design` or `/cm/app/design/templates`
   - Legacy bookmarks: `/cm/app/?view=design` and `/cm/app/admin.jsp` redirect here
     when the Design list cutover is installed. `admin.jsp` then always forces
     `view=design` (a bookmark such as `admin.jsp?view=admin` still opens Design, not
     Admin). Until that cutover is on the server, `admin.jsp` may still show the
     classic CM1 Design list.
3. The **Templates** tab lists assembly templates (label, name, id, description).
   The shell loads under the same product top nav as Explorer, Navigation, Publish, and Admin.

Default landing can also be set to **Design** for a user or role. After sign-in
with no deep-link return URL, the dispatcher applies that preference and opens the
Design SPA.

## Template library

The **Templates** tab lists assembly templates from `GET /services/templates`.

| State | What you see |
|-------|----------------|
| List | A table of templates. Open a row to edit. |
| Empty | **No templates found.** Use **Create template** when that control is present. |
| Error | An operator-facing message. The rest of the Design shell stays usable. |

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

If **Create template** is not on this deployment yet, new templates remain on residual
classic hosts (`editTemplate.jsp` and related upgrade-only JSPs) until that flow is
installed. The library and editor on this page still apply.

## Edit assembler, slots, source, and bindings

Open a template row from the library. The editor (same Design tab) lets you:

- Change the **assembler**
- Edit **slot** layout and styles (orientation, columns, classes)
- Edit Velocity / HTML / Markdown **source**
- Add, edit, or remove **JEXL bindings** (saved as a full replace)

Choose **Save**. Success and validation errors stay on the editor. Use **Templates** to
return to the library.

Content-type associations, delete, and lock are not available on this REST surface yet
(see `designGaps` on the template detail payload).

## Related

- [REST API](id:developer-rest) — `GET`/`POST`/`PUT /services/templates`
- [Extensions & packages](id:developer-extensions)
- [Navigation & site structure](id:admin-architecture-navigation)
- [Administration](id:admin)
