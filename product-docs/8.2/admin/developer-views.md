---
id: admin-developer-views
title: Developer Views
description: Create and delete Content Explorer views from Developer Views chrome
version: "8.2"
order: 47
tags: [admin, developer, views]
---

# Developer Views

**Developer → Views** lists Content Explorer view definitions (Workbench
**View** editor: unique name, label, type, and display format). Admins can
**create** a standard (field-criteria) view and **delete** a user view from
this chrome. The **name** is required, must be unique across views **and**
searches (case-insensitive), and must not contain spaces, wildcards (`*` /
`%`), or path characters. Name cannot be renamed after create. Searches stay
on **Developer → Searches**. Inbox-family and other custom URL views are
listed but **cannot** be updated or deleted from this catalog.

This is **not** the Workbench field-criterion designer. The field criteria
table on detail is **read-only**. Custom-URL views (Inbox, Outbox, Recent,
and the rest of the `sys_cxViews` family) stay protected.

## Product path — create, delete

1. Sign in as **Admin** (write calls require the Admin role).
2. Open **Developer → Views**, or deep-link
   `spa.jsp?entry=developer&section=views`.
3. Click **New view**. Enter a **name**. Save stays disabled until the name
   is valid (no spaces, no `*` / `%`, no `/` or `..`). Optional: label,
   description, type (`View` default), and display format id.
4. Click **Save**. A duplicate name is **409** and the editor shows that the
   view already exists. An invalid name is **400**. A non-Admin session is
   **403**. After a successful create, the name field is read-only and the
   catalog lists the new view.
5. Optional: change label, description, type, or display format id and
   **Save** again. Field criteria are not written.
6. Click **Delete** and confirm. The catalog no longer lists that view.
   Delete of a missing view is **404**. Inbox-family / custom URL views have
   no Delete control. A view still used as a dependent, or locked by another
   user, is **409**.

Existing **execute** of standard views and Inbox-family custom URL views
(Explorer Views tree) is unchanged.

## Limits

- Name is immutable after create.
- Field criterion editing is not in this chrome.
- Searches are a separate Developer catalog (UI-06).
- Custom URL is not collected or executed on write.
- Inbox-family and custom URL views cannot be updated or deleted here.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| List | `GET /services/views` |
| Load | `GET /services/views/{idOrName}` |
| Create | `POST /services/views` (`name` required; unique, no spaces) |
| Save | `PUT /services/views/{idOrName}` (label, description, type, display format) |
| Delete | `DELETE /services/views/{idOrName}` (`204` on success) |

Writes lock the view for the request and release it on save.

Integrator notes: [REST API — Views](id:developer-rest).
