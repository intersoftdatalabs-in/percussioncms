---
id: admin-developer-extensions
title: Developer Extensions
description: Create, edit, and delete user server extensions from Developer Extensions chrome
version: "8.2"
order: 49
tags: [admin, developer, extensions]
---

# Developer Extensions

**Developer → Extensions** lists registered server extensions (Workbench
**Extension Registration**, FR SY-01): handler, context, category, interfaces,
and runtime parameters. Admins can **create**, **update**, and **delete**
**user** extensions (`context` `user/`) from this chrome. New registrations are
forced under `user/`. The **name** must be a valid Java identifier and is unique
with the handler and context (duplicate FQN is **409**).

**System** extensions (`global/percussion/...`) and **handler-owned** extensions
(`ExtensionHandler` / `Handlers`) are listed but **cannot** be updated or
deleted here. Save and Delete stay **disabled** on those rows; a mutate or
delete attempt against REST is **409**.

This chrome uses the fields already on the REST `Extension` wire DTO (name,
handler, interfaces, `initParameters.className`, deprecated). Full Workbench
parameter-dialog parity and method-map editing are later slices.

## Product path — create, save, delete

1. Sign in as **Admin** (write calls require the Admin role).
2. Open **Developer → Extensions**, or deep-link
   `spa.jsp?entry=developer&section=extensions`.
3. Click **New extension**. Enter a **name** (Java identifier), at least one
   **interface** (one per line), and for Java handlers a **class name**
   (`initParameters.className`). Handler defaults to `Java`. Save stays disabled
   until those fields are valid.
4. Click **Save**. A duplicate FQN is **409**. Invalid input is **400**. A
   non-Admin session is **403**. After a successful create, the name and handler
   fields are read-only and the editor notice confirms the save. The catalog
   lists the new extension (`GET /services/extensions/catalog` and GET by key).
5. Optional: change interfaces, class name, or deprecated and **Save** again.
   Identity (handler / context / name) is not renamed on update.
6. Click **Delete** and confirm in the in-app dialog (not a browser prompt).
   The catalog returns with a green **Extension deleted** notice. Delete of a
   missing extension is **404**. Delete of a **system** or **handler-owned**
   extension is blocked in the UI and would be **409** on REST.

## Limits

- Name and handler are immutable after create.
- System and handler-owned extensions cannot be updated or deleted here.
- Workbench parameter dialog parity beyond the wire DTO is not in this chrome.
- Extension method map editing is not in this chrome.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| List | `GET /services/extensions/catalog` |
| Load | `GET /services/extensions/catalog/item?key=` |
| Create | `POST /services/extensions` (`extensionName` + interfaces; Java needs `className`) |
| Save | `PUT /services/extensions/catalog/item?key=` (mutable fields; identity not renamed) |
| Delete | `DELETE /services/extensions/catalog/item?key=` (`204` on success) |

JSON bodies wrap under an `Extension` root. Detail and write keys use a **query**
parameter (`key`) because FQNs contain `/`.

Integrator notes: [REST API — Extensions](id:developer-rest).
