---
id: admin-developer-searches
title: Developer Searches
description: Create, delete, and edit field criteria for Content Explorer searches from Developer Searches chrome
version: "8.2"
order: 46
tags: [admin, developer, searches]
---

# Developer Searches

**Developer → Searches** lists Content Explorer search definitions (Workbench
**Search** editor: unique name, label, type, and display format). Admins can
**create** a standard search and **delete** a user search from this chrome.
The **name** is required, must be unique across searches and views
(case-insensitive), and must not contain spaces, wildcards (`*` / `%`), or
path characters. Name cannot be renamed after create. Views stay on
**Developer → Views** and are not listed here (Inbox and other CX views are
not deleted from this catalog).

Admins can add, remove, and reorder **field criteria** on a user or standard
search from this chrome. Packaged/system searches (for example
`Default_Search` and `RC_Search`) stay read-only for field criteria. Custom-URL
searches are listed but are not executed from this chrome. This is **not** the
full Workbench FTS query designer.

## Product path — create, delete, field criteria

1. Sign in as **Admin** (write calls require the Admin role).
2. Open **Developer → Searches**, or deep-link
   `spa.jsp?entry=developer&section=searches`.
3. Click **New search**. Enter a **name**. Save stays disabled until the name
   is valid (no spaces, no `*` / `%`, no `/` or `..`). Optional: label,
   description, type (`StandardSearch` default, `CustomSearch`, or user
   `Search`), and display format id.
4. Click **Save**. A duplicate name is **409** and the editor shows that the
   search already exists. An invalid name is **400**. A non-Admin session is
   **403**. After a successful create, the name field is read-only and the
   catalog lists the new search (the save is persisted immediately; leaving
   and returning to the list still shows the row).
5. Optional: change label, description, type, or display format id and
   **Save** again.
6. On a user or standard search, use **Field criteria** to add a field, set
   operator and value, reorder with **Move up** / **Move down**, or **Remove**.
   Click **Save field criteria**. An unknown field name is **400**. A
   packaged/system search does not show the editor (PUT of `fields` is **409**
   and does not steal another user's design lock).
7. Click **Delete** and confirm. The catalog no longer lists that search.
   Delete of a missing search is **404**. A search still used as a dependent,
   or locked by another user, is **409**.

Existing **execute** of standard searches (Explorer Search panel) is unchanged.

## Limits

- Name is immutable after create.
- Packaged/system searches cannot be field-edited from this catalog.
- Views are a separate Developer catalog (UI-07).
- Custom URL is not collected or executed on write.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| List | `GET /services/searches` (views omitted) |
| Load | `GET /services/searches/{idOrName}` |
| Create | `POST /services/searches` (`name` required; unique, no spaces) |
| Save | `PUT /services/searches/{idOrName}` (label, description, type, display format; `fields` replaces criteria when present) |
| Delete | `DELETE /services/searches/{idOrName}` (`204` on success) |

Writes lock the search for the request and release it on save.

Integrator notes: [REST API — Searches](id:developer-rest).

## Community new-search defaults

Workbench could assign which searches are offered as **new search** for each
community (`cxNewSearch`). Admins and integrators read and replace that set with:

| Action | Request |
|--------|---------|
| Load | `GET /services/communities/{idOrName}/new-search-defaults` |
| Replace | `PUT /services/communities/{idOrName}/new-search-defaults` |

**Admin** only (**403** otherwise). An empty set is **200**, not 404. Unknown
search in the PUT body is **400**; unknown community is **404**. This chrome does
not yet include a community-defaults editor — use REST (or a later Developer
screen). Search create/delete remains the table above and does not write these
defaults.

Contract: [REST API — Community new-search defaults](id:developer-rest).
