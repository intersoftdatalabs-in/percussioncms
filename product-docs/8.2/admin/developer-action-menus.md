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
usage / command / visibility tabs (UI-03). Parameters and properties on
detail stay **read-only**.

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
   row may lag until GitHub issue 4119 (Hibernate `RXMENUACTION` vs design-WS
   `saveActions`); leaving and returning to the list does not always show the
   new menu yet.
5. Optional: change label, description, menu type, or URL and **Save** again.
   Child entries, parameters, properties, and visibility are not written.
6. Click **Delete** and confirm. The catalog no longer lists that user menu.
   Delete of a missing menu is **404**. Delete of a **system** menu is **409**
   and the row remains. A menu still used as a dependent, or locked by
   another user, is **409**.

## Limits

- Name is immutable after create.
- Cascading child menu composition is not in this chrome (UI-04).
- Usage / command / visibility tab editing is not in this chrome (UI-03).
- System menus cannot be updated or deleted here.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| List | `GET /services/actions/catalog` |
| Load | `GET /services/actions/catalog/{idOrName}` |
| Create | `POST /services/actions` (`name` required; unique, no spaces) |
| Save | `PUT /services/actions/{idOrName}` (label, description, menuType, url) |
| Delete | `DELETE /services/actions/{idOrName}` (`204` on success) |

Writes lock the menu for the request (`overrideLock=false`) and release it on
save. System menus are **409**; the lock is not stolen.

Integrator notes: [REST API — Action menus](id:developer-rest).
