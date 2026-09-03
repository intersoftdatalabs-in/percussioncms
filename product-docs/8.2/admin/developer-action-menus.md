---
id: admin-developer-action-menus
title: Developer Action Menus
description: Create, save usage/command/visibility, compose cascading children, and delete Content Explorer action menus from Developer Action Menus chrome
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

On a **user cascading MENU** (menu type `MENU` and a blank URL), the editor
**Children** section composes ordered child menus (add, remove, reorder, Save).
That write calls `PUT /services/actions/{idOrName}/children`; identity Save
does not persist children. **System** parents keep Children **read-only** and
Save children disabled (a mutate attempt is **409**). Invalid graphs (unknown
child, duplicate, cycle, or a non-cascading parent) surface **400**.

Admins can set **Usage**, **Command**, and **Visibility** on a **user** menu from this
chrome. Those fields round-trip on **Save** (`PUT` then `GET`).

## Product path — create, delete, usage / command / visibility

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
5. Optional: change label, description, or menu type. Open the **Usage**,
   **Command**, and **Visibility** tabs to set handler (`CLIENT` / `SERVER`),
   accelerator / mnemonic / tooltip / icon / launch-window / multi-select /
   refresh hint, URL and URL parameters, visibility contexts (name alias such
   as `community`, `role`, `locale`, `workflow`, or `publishableType` plus a
   value), and optional numeric mode/context UI mappings.
   Click **Save**. GET after save matches those fields. Child menu composition
   is not written from this Save.
6. For a user cascading `MENU` (blank URL), use **Children**: pick an existing
   catalog menu, **Add child**, reorder with Move up / Move down, or Remove,
   then **Save children**. GET detail lists those children in the saved order.
   A system parent (for example **Edit**) shows Children read-only.
7. Click **Delete** and confirm in the in-app dialog (not a browser prompt).
   The catalog returns with a green **Action
   menu deleted** notice and no longer lists that user menu. Delete of a
   missing menu is **404**. Delete of a **system** menu is **409** and the row
   remains. A menu still used as a dependent, or locked by another user, is
   **409**. On **Save** in edit mode (name is read-only), a **400** shows the
   server message rather than an invalid-name hint.

## Limits

- Name is immutable after create.
- Children Save is available only on **user** cascading `MENU` parents (blank URL).
- Usage, command, and visibility are editable here for **user** menus. A
  **system** menu save or delete is **409**; the design lock is not stolen.
  Non-Admin save is **403**. Invalid handler, visibility context name, or
  uiContext id is **400**.
- System menus cannot be updated, deleted, or given children here.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| List | `GET /services/actions/catalog` |
| Load | `GET /services/actions/catalog/{idOrName}` |
| Create | `POST /services/actions` (`name` required; unique, no spaces) |
| Save | `PUT /services/actions/{idOrName}` (label, description, menuType, url, handler, parameters, command properties, visibilityContexts, uiContexts) |
| Save children | `PUT /services/actions/{idOrName}/children` (ordered `{ActionMenuList:[…]}` by name; cycle/unknown/duplicate child is 400) |
| Delete | `DELETE /services/actions/{idOrName}` (`204` on success) |

Writes lock the menu for the request (`overrideLock=false`) and release it on
save. System menus are **409**; the lock is not stolen.

Integrator notes: [REST API — Action menus](id:developer-rest).
