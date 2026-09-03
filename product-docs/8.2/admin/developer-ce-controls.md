---
id: admin-developer-ce-controls
title: Developer CE Controls
description: Create, update, and delete a user content-editor control from Developer CE Controls chrome
version: "8.2"
order: 49
tags: [admin, developer, controls]
---

# Developer CE Controls

**Developer → CE Controls** lists content editor control definitions
(Workbench **Controls**: packaged system controls and custom user controls).
Admins can **create**, **save**, and **delete** a user control from this chrome.
The **name** is required, must be unique across system **and** user controls
(case-insensitive), and must not contain spaces or wildcards (`*` / `%`).
Name cannot be renamed after create.

**System** controls (packaged defaults such as `sys_EditBox`) are listed but
**cannot** be created, edited, or deleted from this catalog. A write that
targets a system control is **409**.

This is **not** the Workbench XSL source editor. Create and save may include
optional `xslSource`; when omitted the server writes (or regenerates) a default
user-control stylesheet from the metadata.

## Product path — create

1. Sign in as **Admin** (write calls require the Admin role).
2. Open **Developer → CE Controls**, or deep-link
   `spa.jsp?entry=developer&section=ce-controls`.
3. Click **New user control**. Enter a **name**. Save stays disabled until the
   name is valid (no spaces, no `*` / `%`; letters, digits, underscore, period,
   and hyphen). Optional: display name, description, dimension (`single`
   default, `array`, `table`), choice set (`none` default, `required`,
   `optional`), and XSL source.
4. Click **Create user control**. A duplicate name is **409** and the editor
   shows that the control already exists. An invalid name is **400**. A
   non-Admin session is **403**. After a successful create, the name is
   read-only and the catalog lists the new control (`GET /services/cecontrols`
   and GET by name). Packaged controls such as **sys_EditBox** cannot be
   created again (**409**).

## Product path — update and delete

1. Open a **user** control row. Display name, description, dimension, choice
   set, and optional XSL source are editable. Name stays read-only.
2. Click **Save user control**. The chrome sends metadata on
   `PUT /services/cecontrols/{name}`. Leave XSL blank to regenerate the server
   default stylesheet (send XSL to keep a custom stylesheet). **403**, **404**,
   and system **409** appear in the detail error region.
3. Click **Delete user control**. Confirm in the in-app dialog (not the
   browser `window.confirm` prompt). A successful delete is **204**; a following
   GET is **404** and the catalog no longer lists the row.
4. Open a **system** row to view parameters. The detail is read-only; there is
   no create, save, or delete chrome on a system control.

## Limits

- Name is immutable after create.
- System controls cannot be created, edited, or deleted here.
- Optional XSL source is a text field, not a full IDE.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| List | `GET /services/cecontrols` |
| Load | `GET /services/cecontrols/{name}` |
| Create | `POST /services/cecontrols` (`name` required; unique, no spaces or wildcards) |
| Update | `PUT /services/cecontrols/{name}` (user controls; omitted `xslSource` regenerates the default stylesheet) |
| Delete | `DELETE /services/cecontrols/{name}` (**204**; following GET is **404**) |

Integrator notes: [REST API — Content editor controls](id:developer-rest).
