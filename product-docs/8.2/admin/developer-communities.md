---
id: admin-developer-communities
title: Developer Communities
description: Create and delete CMS communities, edit roles, and set Content Explorer new-search defaults from Developer Communities chrome
version: "8.2"
order: 45
tags: [admin, developer, communities]
---

# Developer Communities

**Developer → Communities** lists CMS communities (label, unique name, id, and
description). Admins can **create** a community and **delete** one from this
chrome. Open an existing community to **edit role membership**, set **Content
Explorer new-search defaults**, and inspect **object visibility**. Per-object
COMMUNITY ACL entries stay on object detail panels (for example content types).

## Product path — create and delete

1. Sign in as **Admin** (create and delete require the Admin role).
2. Open **Developer → Communities**, or deep-link
   `spa.jsp?entry=developer&section=communities`.
3. Click **New community**. Enter a **name** (required, unique,
   case-insensitive). Spaces are allowed. Create stays disabled until the
   name is non-blank after trim.
4. Click **Create community**. A duplicate name is **409** and the editor
   shows that the community already exists. A blank name is **400**. A
   non-Admin session is **403**. After a successful create, the catalog
   includes the new row when you return to the list, and the detail panel
   still offers **role membership** save.
5. Open an existing community and click **Delete community**, then confirm
   in the in-app dialog (not a browser prompt).
   The catalog no longer lists that name. Delete of a missing community is
   **404**. A community that is still **in use** (dependencies) is **409**
   and remains — the SPA does **not** send `ignoredependencies` and does not
   steal. Non-Admin is **403**.

## Product path — role membership (assign / unassign)

1. Open an existing community on **Developer → Communities**.
2. Under role membership, check roles to **assign** and uncheck roles to
   **unassign**. The picker is the full security role catalog
   (`GET /services/communities/roles`).
3. Click **Save roles**. The server replaces the community’s role set with the
   checked roles (same full-set replace as Workbench dual-list save). Clearing
   every checkbox and saving removes all associations. The detail panel
   refreshes membership from the PUT response and shows a short saved-count
   status notice.
4. A missing community is **404**. A role entry without id/guid is **400**.

## Product path — new-search defaults

Workbench assigned which searches are offered as **new search** (`cxNewSearch`)
for each community. That assignment is now on the community detail panel.

1. Open **Developer → Communities** and open a community.
2. Under **New-search defaults**, check or uncheck searches from the Developer
   Searches catalog. This list does **not** create searches — create them on
   [Developer Searches](id:admin-developer-searches).
3. Click **Save new-search defaults**. The set is replaced immediately. Clearing
   every checkbox and saving **clears** explicit defaults for that community
   (HTTP **200**, empty set — not 404).
4. An unknown search in the saved set is **400**. A non-Admin session is **403**.
   A missing community is **404**. A design lock held by another user is **409**.

## Limits

- Create uses the existing bulk REST (`POST /services/communities/bulk`). The
  server persists on create (Workbench Finish create+save). Name is the catalog
  key. The SPA does not PUT the DTO back after create.
- Delete uses `DELETE /services/communities/bulk` with the community GUID
  and `ignoredependencies=false`.
- Community visibility remains a read-only lens. Object ACL for a COMMUNITY
  principal is edited on the object, not here.
- New-search defaults replace the whole set on save. Empty is a valid clear.
  Search create/delete stays on Developer Searches.
- Session community switch in the header is a separate membership list; it
  is not this catalog.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| List | `GET /services/communities/find?name=*` |
| Load | `GET /services/communities/{idOrName}` |
| Create | `POST /services/communities/bulk` (name list; server persists) |
| Available roles (picker) | `GET /services/communities/roles` |
| Assign / unassign roles | `PUT /services/communities/{idOrName}/roles` (full membership replace; `{"CommunityRoleList":[]}` clears) |
| New-search defaults | `GET` / `PUT /services/communities/{idOrName}/new-search-defaults` |
| Delete | `DELETE /services/communities/bulk` (GuidList; `ignoredependencies=false`) |

Integrator notes: [REST API](id:developer-rest). Related security chrome:
[Users, roles & security](id:admin-users-roles).
