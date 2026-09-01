---
id: admin-developer-searches
title: Developer Searches
description: Create and delete Content Explorer searches from Developer Searches chrome
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

This is **not** the Workbench field-criterion / FTS designer. The field
criteria table on detail is **read-only**. Custom-URL searches are listed but
are not executed from this chrome.

## Product path — create, delete

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
   catalog lists the new search.
5. Optional: change label, description, type, or display format id and
   **Save** again. Field criteria are not written.
6. Click **Delete** and confirm. The catalog no longer lists that search.
   Delete of a missing search is **404**. A search still used as a dependent,
   or locked by another user, is **409**.

Existing **execute** of standard searches (Explorer Search panel) is unchanged.

## Limits

- Name is immutable after create.
- Field criterion editing is not in this chrome.
- Views are a separate Developer catalog (UI-07).
- Custom URL is not collected or executed on write.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| List | `GET /services/searches` (views omitted) |
| Load | `GET /services/searches/{idOrName}` |
| Create | `POST /services/searches` (`name` required; unique, no spaces) |
| Save | `PUT /services/searches/{idOrName}` (label, description, type, display format) |
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
