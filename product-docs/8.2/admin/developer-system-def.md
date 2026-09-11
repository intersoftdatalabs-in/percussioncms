---
id: admin-developer-system-def
title: Developer System Def
description: Save, add, and delete content-editor system fields, edit control properties, persist stylesheet associations, and persist application-flow redirects from Developer System definition chrome
version: "8.2"
order: 44
tags: [admin, developer, system-def]
---

# Developer System Def

**Developer → System definition** lists global content-editor system fields
(name, data type, occurrence, required, searchable, read-only). Admins can
**save** property patches on existing fields, **add** or **delete** a system
field, **view/save control properties** for a selected field, **view/save
command-handler stylesheet associations**, and **view/save application-flow
redirects**. Writes use a
**request lock that is released on save** (there is no separate Lock / Unlock
toolbar).

## Product path — save, add, delete

1. Sign in as **Admin** (write calls require the Admin role).
2. Open **Developer → System definition**, or deep-link
   `spa.jsp?entry=developer&section=system-def`.
3. To **add** a field, enter a **name** (letter, then letters, digits, or
   underscore; no spaces). Optional: data type (defaults to `text`), searchable,
   and required. **Add field** stays disabled until the name is valid.
4. Click **Add field**. A duplicate name is **409** and the panel shows that
   the field already exists. An invalid name is **400**. If another designer
   holds the system-definition lock, save/add/delete is **409**.
5. To **save** searchable/occurrence properties, change **Searchable** or
   **Occurrence** on a catalog row and click **Save fields**. The request lock
   is acquired and released on that save. Data type and read-only stay
   display-only.
6. To **save control properties**, select a field in **Control property
   values**, change at least one parameter value (or add a name/value pair),
   and click **Save control properties**. GET does not require a lock. PUT
   acquires the system-definition lock and **releases** it on save. Non-Admin
   callers receive **403**. Unknown field names are **404**.
7. To **save stylesheets**, edit a command-handler **href** in **Stylesheets**
   (or add a handler name + `file:../sys_resources/stylesheets/*.xsl` /
   `file:../rx_resources/stylesheets/*.xsl` href) and click **Save
   stylesheets**. **Remove** drops that handler (Workbench clear). At least one
   handler must remain (**400** if the set would be empty). Conditional
   stylesheet rows are shown read-only. GET does not require a lock. PUT
   acquires the system-definition lock and **releases** it on save. Non-Admin
   callers receive **403**. Invalid href or handler name is **400**.
8. To **save application flow**, edit a command-handler **redirect href** in
   **Application flow** (relative `../sys_*` or `../rx_*` CMS app path ending
   in `.html` / `.xml` / `.jsp`, or empty to keep an empty default path) and
   click **Save application flow**. **Remove** drops that handler. At least one
   handler must remain (**400** if the set would be empty). Conditional
   redirects are shown read-only. GET does not require a lock. PUT acquires the
   system-definition lock and **releases** it on save. Non-Admin callers
   receive **403**. Invalid href or handler name is **400**. Extra `..`,
   absolute paths, and `http(s)` URLs are **400**.
9. Click **Delete** on a row and confirm in the in-app dialog (not a
   browser prompt). The catalog no longer lists that
   field. System-mandatory and system-internal fields cannot be deleted
   (**400**).

## Limits

- Shared field groups are a separate catalog (**Developer → Shared fields**).
- There is no persistent design-session lock UI; each write holds the lock only
  for that request.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| Load | `GET /services/systemdef` |
| Save properties | `PUT /services/systemdef` (patch existing `fields[]`) |
| Add field | `POST /services/systemdef/fields` (`name` required) |
| Delete field | `DELETE /services/systemdef/fields/{fieldName}` (`204` on success) |
| Load control properties | `GET /services/systemdef/fields/{fieldName}/controlProperties` (no lock) |
| Save control properties | `PUT /services/systemdef/fields/{fieldName}/controlProperties` (request lock released on save; wrap root `SystemDefControlProperties`) |
| Load stylesheets | `GET /services/systemdef/stylesheets` (no lock) |
| Save stylesheets | `PUT /services/systemdef/stylesheets` (request lock released on save; wrap root `SystemDefStylesheets`; full replace of handlers) |
| Load application flow | `GET /services/systemdef/applicationFlow` (no lock) |
| Save application flow | `PUT /services/systemdef/applicationFlow` (request lock released on save; wrap root `SystemDefApplicationFlow`; full replace of handlers) |

Integrator notes: [REST API — System definition](id:developer-rest).
