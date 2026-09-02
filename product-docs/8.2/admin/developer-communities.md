---
id: admin-developer-communities
title: Developer Communities
description: Create and delete CMS communities from Developer Communities chrome
version: "8.2"
order: 45
tags: [admin, developer, communities]
---

# Developer Communities

**Developer → Communities** lists CMS communities (label, unique name, id, and
description). Admins can **create** a community and **delete** one from this
chrome. Open an existing community to **edit role membership** and inspect
**object visibility**. Per-object COMMUNITY ACL entries stay on object detail
panels (for example content types).

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

Role membership save on the same detail panel is unchanged: check roles and
**Save roles**.

## Limits

- Create uses the existing bulk REST (`POST /services/communities/bulk`). The
  server persists on create (Workbench Finish create+save). Name is the catalog
  key. The SPA does not PUT the DTO back after create.
- Delete uses `DELETE /services/communities/bulk` with the community GUID
  and `ignoredependencies=false`.
- Community visibility remains a read-only lens. Object ACL for a COMMUNITY
  principal is edited on the object, not here.
- Session community switch in the header is a separate membership list; it
  is not this catalog.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| List | `GET /services/communities/find?name=*` |
| Load | `GET /services/communities/{idOrName}` |
| Create | `POST /services/communities/bulk` (name list; server persists) |
| Roles | `PUT /services/communities/{idOrName}/roles` |
| Delete | `DELETE /services/communities/bulk` (GuidList; `ignoredependencies=false`) |

Integrator notes: [REST API](id:developer-rest). Related security chrome:
[Users, roles & security](id:admin-users-roles).
