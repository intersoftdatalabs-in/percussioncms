---
id: admin-developer-action-menus
title: Developer Action Menus
description: Create and delete Content Explorer action menus from Developer Action Menus chrome
version: "8.2"
order: 48
tags: [admin, developer, action-menus]
---

# Developer Action Menus

**Developer → Action Menus** lists Content Explorer action menu definitions
(Workbench **Menus** editor: unique internal name, label, description, and
menu type). Admins can **create** a user action menu and **delete** a selected
user menu from this chrome. The **name** is required, must be unique
(case-insensitive), and must not contain spaces, wildcards (`*` / `%`), or
path characters. Name cannot be renamed after create.

**System** menus (Workbench `Menus/System` hierarchy) are listed but **cannot**
be updated or deleted from this catalog. A mutate or delete attempt is **409**;
the product does not steal the design lock (`overrideLock=false`).

This is **not** the Workbench cascading-children composer (UI-04) or the
SPA usage / command / visibility tabs. Integrators can persist usage and
command fields on **PUT** (handler, URL, URL parameters, command properties);
those tabs in this chrome remain later. Visibility contexts are not written.

## Product path — create, delete

1. Sign in as **Admin** (write calls require the Admin role).
2. Open **Developer → Action Menus**, or deep-link
   `spa.jsp?entry=developer&section=action-menus`.
3. Click **New action menu**. Enter a **name**. Save stays disabled until the
   name is valid (no spaces, no `*` / `%`, no `/` or `..`). Optional: label,
   description, menu type (`MENUITEM` default, `MENU`, `CONTEXTMENU`, or
   `DYNAMICMENU`), and URL.
4. Click **Save**. A duplicate name is **409** and the editor shows the server
   conflict message (duplicate, system menu, or lock). An invalid name is
   **400**. A non-Admin session is **403**. After a successful create, the name
   field is read-only and the editor notice confirms the save. The catalog
   lists the new name immediately (`GET /services/actions/catalog` and GET by
   name); packaged menus such as **Copy** cannot be deleted (**409**).
5. Optional: change label, description, menu type, or URL and **Save** again.
   Child entries are not written from this chrome. REST **PUT** can persist
   handler, URL parameters, command properties, visibility contexts, and
   mode-uicontexts.
6. Click **Delete** and confirm in the in-app dialog (not a browser prompt).
   The catalog returns with a green **Action
   menu deleted** notice and no longer lists that user menu. Delete of a
   missing menu is **404**. Delete of a **system** menu is **409** and the row
   remains. A menu still used as a dependent, or locked by another user, is
   **409**. On **Save** in edit mode (name is read-only), a **400** shows the
   server message rather than an invalid-name hint.

## Limits

- Name is immutable after create.
- Cascading child menu composition is not in this chrome (UI-04).
- Usage / command / visibility tab editing is not in this chrome; REST PUT
  honors usage, command, and visibility fields (see
  [REST API — Action menus](id:developer-rest)).
- System menus cannot be updated or deleted here.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| List | `GET /services/actions/catalog` |
| Load | `GET /services/actions/catalog/{idOrName}` |
| Create | `POST /services/actions` (`name` required; unique, no spaces) |
| Save | `PUT /services/actions/{idOrName}` (label, description, menuType, url, handler, parameters, command properties, visibilityContexts, uiContexts) |
| Delete | `DELETE /services/actions/{idOrName}` (`204` on success) |

Writes lock the menu for the request (`overrideLock=false`) and release it on
save. System menus are **409**; the lock is not stolen.

Integrator notes: [REST API — Action menus](id:developer-rest).
