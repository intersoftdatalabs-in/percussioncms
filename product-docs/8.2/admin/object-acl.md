---
id: admin-object-acl
title: Object ACL & default template
description: Design access and Runtime visibility on design objects, and the Developer default ACL template
version: "8.2"
order: 43
tags: [admin, security, acl, developer]
---

# Object ACL & default template

Design objects (content types, templates, and other Developer catalog peers) carry an
**Object ACL**. The editor splits permissions into two layers that match Workbench:

| Layer | Columns | Meaning |
|-------|---------|---------|
| **Design access** | Read, Update, Delete, Modify ACL | Who can see and change the design object in the CMS |
| **Runtime visibility** | Visible | Whether the object is visible at runtime (for example to a community) |

Special rows such as **Default** (USER) and **AnyCommunity** (COMMUNITY) stay protected
on content-type ACLs. You can change their permission checkboxes; you cannot remove
those system principals.

## Product path — open Object ACL

1. Sign in as a user with Developer access (for example **Admin**).
2. Open **Developer** from product navigation, or deep-link
   `spa.jsp?entry=developer`.
3. Open a catalog that supports Object ACL:
   - **Content types** → first type → **Open**
   - **Templates** → first template → **Open**
   - **Display Formats** → a format such as **By_Author** → **Open**
   - **Action Menus** → first menu → **Open**
   - **Views** → first view → **Open**
4. On the detail panel, confirm the header **GUID** is populated when the object
   has an id — the product uses the nested Guid, a plain `guidString`, the catalog
   list GUID, or a typed id synthesis (`0-4-{templateId}`, `0-107-{actionId}`,
   `0-18-{viewId}`).
5. Expand **Object ACL**. When that GUID is present, the table (or empty create
   path) loads — it must **not** say “Object GUID not available”.
6. Confirm the table shows **Design access** and **Runtime visibility** column groups
   (runtime-relevant object kinds such as content type, template, and display
   format always show **Visible**). Those layer headers stay visible even when
   the ACL has **no entries yet** (no draft row required) — only the body is empty
   until you add a principal.
7. To persist permissions: add **Default**, **AnyCommunity**, and any extra USER
   or ROLE principal, then click **Save**. Reopen the same object — those entries
   must still be present (Default, AnyCommunity, and the USER you added). The
   product updates the existing object ACL; it does not create a second empty
   ACL. If the object has no ACL yet, create one first (owner principal) and then
   save the additional entries.

Deep links:

- Content types: `spa.jsp?entry=developer&section=content-types`
- Templates: `spa.jsp?entry=developer&section=templates`
- Display formats: `spa.jsp?entry=developer&section=display-formats`
- Action Menus: `spa.jsp?entry=developer&section=action-menus`
- Views: `spa.jsp?entry=developer&section=views`

## Product path — default ACL template (Preferences)

New object ACLs can seed from a **per-user default template**. That template is
stored as the user preference `developer.defaultObjectAclTemplate`.

1. Open **Developer → Preferences** (`spa.jsp?entry=developer&section=preferences`).
2. Under **Security**, review the **default ACL template** table.
3. The table uses the same **Design access** / **Runtime visibility** groups as
   Object ACL on a design object.
4. The system default is:
   - **Default** (USER): Read, Update, Delete, Modify ACL (Visible off)
   - **AnyCommunity** (COMMUNITY): Visible on
5. To change Runtime visibility for **Default**, check or clear **Visible**, then
   click **Save default ACL template**.
6. Reload **Developer → Preferences** (full page refresh or leave and return).
   **Visible** must match what you saved. The source line should indicate a saved
   preference, not only the system default.

If Save appears to succeed but **Visible** is unchecked after reload, the preference
did not persist — check that you are still signed in, then retry. The product loads
the named preference first and falls back to the full preference list so a saved
template is not dropped on reload.

## Related

- [Users, roles & security](id:admin-users-roles)
- [Sites & content structure](id:admin-sites)
