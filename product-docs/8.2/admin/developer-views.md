---
id: admin-developer-views
title: Developer Views
description: Create, delete, and edit field criteria or custom URLs on Content Explorer views from Developer Views chrome
version: "8.2"
order: 47
tags: [admin, developer, views]
---

# Developer Views

**Developer → Views** lists Content Explorer view definitions (Workbench
**View** editor: unique name, label, type, display format, field criteria, or
a classic custom URL). Admins can **create** a standard (field-criteria) view
or a **user custom URL** view (`CustomView`), **delete** a user view, **edit
field criteria** on a user/standard view, and **update the URL** on a user
custom URL view from this chrome. The **name** is required, must be unique
across views **and** searches (case-insensitive), and must not contain
spaces, wildcards (`*` / `%`), or path characters. Name cannot be renamed
after create. Searches stay on **Developer → Searches**. Inbox-family and
other packaged `sys_cxViews` catalog keys (Inbox, Outbox, Recent, Session,
Checked Out By Me, Duplicate Folder Paths) are listed but **cannot** be
updated, deleted, or field-edited from this catalog.

The field picker uses the same CX system-field catalog as **Developer →
Display Formats** columns. Packaged custom-URL views in the `sys_cxViews`
family stay protected. User-created custom URL views hide the field-criteria
editor and collect a **URL** instead.

## Product path — create, delete

1. Sign in as **Admin** (write calls require the Admin role).
2. Open **Developer → Views**, or deep-link
   `spa.jsp?entry=developer&section=views`.
3. Click **New view**. Enter a **name**. Save stays disabled until the name
   is valid (no spaces, no `*` / `%`, no `/` or `..`). Optional: label,
   description, type (`View` default or `CustomView`), and display format id.
4. For a **CustomView**, enter a classic relative **URL** (for example
   `../sys_cxViews/myapp.xml`). Save stays disabled until the URL is
   non-blank and valid (no schemes, no multi-segment `../` traversal). Field
   criteria are not collected for custom URL views.
5. Click **Save**. A duplicate name is **409** and the editor shows that the
   view already exists (including when the name is already in the catalog
   list). An invalid name is **400**. A blank or invalid custom URL is
   **400** and the editor shows that a URL is required. A non-Admin session
   is **403**. After a successful create, the name field is read-only and the
   catalog lists the new view immediately (the save is persisted to the views
   catalog; leaving and returning to the list still shows the row). A second
   **New view** using that same name stays on the editor with the duplicate
   error. A custom URL create round-trips `url` and `customView` on GET.
6. Optional: change label, description, type (on create only), display format
   id, or (for a user custom URL view) the URL, then **Save** again.
7. Click **Delete** and confirm in the in-app dialog (not a browser prompt).
   The catalog no longer lists that view.
   Delete of a missing view is **404**. Inbox-family / packaged `sys_cxViews`
   views have no Delete control. A view still used as a dependent, or locked
   by another user, is **409**.

## Product path — field criteria

1. Open a **user/standard** view from the catalog (not Inbox or another
   packaged custom-URL view, and not a user custom URL view).
2. In **Field criteria**, choose a field from the picker, optional operator
   and value, then **Add field**. Use **Move up** / **Move down** to reorder
   and **Remove** to drop a row.
3. Click **Save fields**. The PUT body includes `fields` in picker order.
   The chrome locates the just-created view by GUID (`0-18-{id}`) and, if
   that lookup is **404**, retries by name. `GET /services/views/{name}` then
   lists those criteria in the same order. An unknown field is **400**. A
   non-Admin session is **403**.
4. Inbox-family / packaged `sys_cxViews` views show the criteria table
   **read-only** (no add/remove/reorder/save). User custom URL views hide the
   field editor and show a note to edit the URL instead. This chrome does
   **not** mutate Inbox packaged views.

Existing **execute** of standard views and Inbox-family custom URL views
(Explorer Views tree) is unchanged.

## Limits

- Name is immutable after create.
- Field criteria on user/standard views use the CX system-field catalog
  (same picker as display-format columns). Unknown field names are rejected.
- Searches are a separate Developer catalog (UI-06); search field-selection
  is not this chrome.
- User custom URL views collect and persist `url` (+ `customView`) from this
  chrome; field criteria are not used on those rows.
- Inbox-family and packaged `sys_cxViews` views cannot be updated, deleted, or
  field-edited here.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| List | `GET /services/views` |
| Load | `GET /services/views/{idOrName}` |
| Create | `POST /services/views` (`name` required; unique, no spaces; CustomView requires `url`) |
| Save | `PUT /services/views/{idOrName}` (label, description, type, display format, `url` for user custom URL views; optional `fields`) |
| Delete | `DELETE /services/views/{idOrName}` (`204` on success) |

Omitted `fields` on PUT leave existing criteria unchanged. An empty `fields`
array clears them. Writes lock the view for the request and release it on
save. Admin REST also persists user custom URL views; see
[REST API — Views](id:developer-rest).

Integrator notes: [REST API — Views](id:developer-rest).
