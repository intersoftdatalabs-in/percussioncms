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

## Open Design (SPA)

1. Sign in as an **Admin** or **Designer**.
2. Choose **Design** in the product top navigation, or open:
   - Query contract: `spa.jsp?entry=design&section=templates`
   - Path route: `/cm/app/design` or `/cm/app/design/templates`
3. The **Templates** tab lists assembly templates (label, name, id, description).

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

## After create

Use the template editor on the same Design tab to:

- Change the assembler
- Edit slot layout and styles
- Edit Velocity / HTML / Markdown source
- Maintain JEXL bindings

Content-type associations, delete, and lock are not available on this REST surface yet.

## Related

- [REST API](id:developer-rest) — `GET`/`POST`/`PUT /services/templates`
- [Extensions & packages](id:developer-extensions)
- [Navigation & site structure](id:admin-architecture-navigation)
