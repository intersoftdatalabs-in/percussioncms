---
id: admin-developer-ce-controls
title: Developer CE Controls
description: Create, update, delete, and edit user content-editor control XSL from Developer CE Controls chrome
version: "8.2"
order: 49
tags: [admin, developer, controls]
---

# Developer CE Controls

**Developer → CE Controls** lists content editor control definitions
(Workbench **Controls**: packaged system controls and custom user controls).
Admins can **create**, **save**, **delete**, and **edit XSL source** for a user
control from this chrome. The **name** is required, must be unique across
system **and** user controls (case-insensitive), and must not contain spaces or
wildcards (`*` / `%`). Name cannot be renamed after create.

**System** controls (packaged defaults such as `sys_EditBox`) are listed but
**cannot** be created, edited, or deleted from this catalog. A write that
targets a system control is **409** (packaged files are not mutated).

`GET /services/cecontrols/{name}` **round-trips** `xslSource` for **user**
controls. **System** control detail may include a **read-only** ControlMeta
snippet extracted from the packaged stylesheet (the chrome shows it in a
read-only editor; PUT/DELETE stay **409**). Create and save may include
`xslSource`; when omitted the server writes (or regenerates) a default
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
   set, and **XSL source** are editable. Name stays read-only. GET loads the
   persisted stylesheet into the source editor.
2. Click **Save user control**. The chrome sends metadata and the current XSL
   on `PUT /services/cecontrols/{name}`. Leave XSL blank to regenerate the
   server default stylesheet. Invalid XSL is **400**. **403**, **404**, and
   system **409** appear in the detail error region.
3. Click **Delete user control**. Confirm in the in-app dialog (not the
   browser `window.confirm` prompt). A successful delete is **204**; a following
   GET is **404** and the catalog no longer lists the row.
4. Open a **system** row to view parameters and a **read-only XSL / ControlMeta
   snippet** when the API returns it. The detail is read-only; there is no
   create, save, or delete chrome on a system control.

## Limits

- Name is immutable after create.
- System controls cannot be created, edited, or deleted here.
- User-control XSL is a monospace source editor (not a full IDE). Invalid
  stylesheets (not exactly one matching `psxctl:ControlMeta`) are **400**.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| List | `GET /services/cecontrols` |
| Load | `GET /services/cecontrols/{name}` (user-control `xslSource` round-trip) |
| Create | `POST /services/cecontrols` (`name` required; unique, no spaces or wildcards) |
| Update | `PUT /services/cecontrols/{name}` (user controls; send `xslSource` to persist; omitted regenerates the default stylesheet) |
| Delete | `DELETE /services/cecontrols/{name}` (**204**; following GET is **404**) |

Integrator notes: [REST API — Content editor controls](id:developer-rest).
