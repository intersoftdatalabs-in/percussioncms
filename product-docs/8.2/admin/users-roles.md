---
id: admin-users-roles
title: Users, roles & security
description: Accounts, roles, and access control for Percussion CMS 8.2
version: "8.2"
order: 42
tags: [admin, security]
---

# Users, roles & security

Percussion CMS enforces editorial and administrative access through users, roles, communities, and
ACL-style permissions on content and design objects.

## Accounts

- Create administrative and editorial accounts through the product security / user management UI
  (or directory integration when configured).
- Use **unique accounts** for operators; avoid shared passwords for auditability.
- Disable or remove accounts that leave the organization.

## Roles and least privilege

Assign the minimum role set needed for each job function:

| Audience | Typical needs |
|----------|----------------|
| Content authors | Edit assigned folders/pages; workflow submit |
| Approvers | Transition workflow; limited publish rights |
| Site admins | Site properties, folder ACL, publish jobs |
| System admins | Server config, extensions, security admin |

Revisit role membership when Sites or departments reorganize.

## Workflow

Workflow states gate who can edit and publish. Common patterns:

1. Draft → Review → Approved → Live (names vary by package/configuration).
2. Reject paths return content to authors with comments.
3. Scheduled or manual publish after approval depending on Site configuration.

## Authentication notes

- Local authentication is available out of the box for many installs.
- Enterprise deployments often integrate external identity providers; follow the security module
  documentation for your configured authenticator.
- Protect the login endpoint behind TLS at the reverse proxy or edge.

## Session community (top nav)

After sign-in, the header user menu shows the **current community** next to **Signed in as**.
Use **Switch** to change the session community **without signing out**.

- The switch list contains **only** communities you can access through your user and role
  membership (the same list as **My profile → Account**). It is not the full community catalog
  from Developer.
- After a successful switch, the header name updates immediately. Content Explorer and other
  screens that filter by the active community then use the new community — you do not need to
  log out and back in.
- If the switch is not allowed (unknown community or you are not a member of a role in that
  community), the header shows an error and keeps the previous community.
- Setting a **default** community on the profile, or remembering the last community at the next
  login, is not part of this header control.

## My profile — Security (password)

Authenticated users open **My profile** from the header user menu (or deep link
`spa.jsp?entry=profile`). The **Security** section behaves by account type:

| Account type | What the user sees |
|--------------|--------------------|
| **Internal** (local auth) | Change-password form: new password + confirm. The product calls the existing self-only `PUT /user/user/changepw` endpoint; you cannot change another user’s password from this page. |
| **Directory / SSO** | A localized explanation that credentials are managed by the identity provider or IT — **no** password form that would always fail. |

Password rules enforced by the server (complexity / history filters when configured)
still apply. Client-side checks require a non-empty password of at least six characters
and a matching confirmation before submit. After a **successful** change the Security
section shows a success confirmation (not an error); the current session stays signed
in, and the new password is required at the next sign-in. Failed validation (too short,
mismatch, or a server-side rule) stays an error and does not change the stored
password. Success and failure messages are announced to assistive technology via a
live region.

## My profile — Preferences (default landing)

On the same **My profile** hub, open **Preferences** (or deep-link
`spa.jsp?entry=profile#perc-profile-preferences`). Admins can also set a user's
landing from **Admin → Users**.

| Control | What it does |
|---------|----------------|
| **Default landing page** | Where you go after sign-in. Choose a product screen you are allowed to open (Home, Editor, **Navigation**, and additional screens when your roles include Designer or Admin). **Navigation** is stored as homepage type `Architecture` and opens the site Navigation SPA. **Use role default** clears your personal override so the role homepage applies. |
| **Save preferences** | Writes the landing override for the signed-in user only. The value is reloaded from the server after save so a failed persist is not shown as success. |

The stored-preference count is informational (existing preference entries for your
account). A problem loading that list does **not** block changing the landing page.

Login without a deep-link return URL posts to `/cm/app/` so the dispatcher applies
this preference.

Language and density controls are not product-backed yet. Navigation **site**
section landing pages (Architecture tree) are a separate site-structure setting —
not this profile control. See [Architecture & site navigation](id:admin-architecture-navigation).

## Design-object ACL (Developer)

System Definition (**Developer**) shows an **Object ACL** section on securable design
objects, including **Content types**, **Templates**, **Display Formats**, **Sites**,
**Action Menus**, and **Views**.

### Content types and Templates

1. Open **Developer → Content types** or **Developer → Templates**.
2. Open a catalog row.
3. The detail header **GUID** field shows the object GUID when the server has one
   (nested `guid.stringValue`, synthesizable `hostId` / `type` / `uuid`, list-row
   fallback, or — for templates — `0-4-{templateId}` when only the numeric id is
   present).
4. **Object ACL** below the detail form:
   - Shows the ACL table when entries exist (Design access and Runtime visibility).
   - When an ACL document exists but has **no principals yet**, the table headers
     (including **Runtime visibility → Visible** for content types and templates)
     still appear; add a principal below the empty body.
   - Shows an empty create path when the object has no ACL yet.
   - Does **not** say “Object GUID not available” when the header GUID is present.

### Display Formats

1. Open **Developer → Display Formats**.
2. Open a format such as **By_Author** (or any other listed format).
3. The detail header **GUID** field shows the object GUID when the server has one
   (never a silent dash when a GUID exists).
4. **Object ACL** below the column list:
   - Shows the ACL table when entries exist (Design access and Runtime visibility).
   - Shows an empty create path when the object has no ACL yet.
   - Shows an explicit load error if the ACL service fails.
   - Does **not** say “Object GUID not available” when the header GUID is present.
   - If the object truly has no GUID, the section still mounts with kind-aware copy
     (display format) and does not crash the detail page.
5. **Save** persists **Default**, **AnyCommunity**, and added USER/ROLE principals
   onto the existing object ACL (the server updates that row; it does not insert
   a second empty ACL). After Back and reopen, those three rows must still be
   present (not HTTP 400/500 and not an empty table). If the format has no ACL
   yet, use **Create** with an owner, then add specials and **Save**.

Use this section to inspect and edit design-time and runtime visibility permissions
for that display format.

### Sites

1. Open **Developer → Sites**.
2. Open a Site row (catalog from `GET /services/sites`).
3. The detail header **GUID** field shows the site object GUID when the list payload
   includes `guid.stringValue` or synthesizable `hostId` / `type` / `uuid` parts.
4. **Object ACL** on Site detail:
   - Loads and is readable/editable when a GUID is present (same Design / Runtime
     columns as other runtime-relevant objects).
   - If the catalog row has no GUID parts, the section still mounts with site-specific
     empty copy and does not crash Site detail.

See also [Sites & content structure](id:admin-sites).

### Action Menus and Views

1. Open **Developer → Action Menus** or **Developer → Views**.
2. Open a catalog row.
3. The detail header **GUID** field shows the object GUID when the server or
   catalog has one (nested `guid.stringValue`, plain `guidString`, list-row
   fallback, or — for menus — `0-107-{actionId}` / for views — `0-18-{viewId}`
   when only the numeric id is present).
4. **Object ACL** below the detail form:
   - Shows the ACL table when entries exist (Design access and Runtime visibility).
   - Shows an empty create path when the object has no ACL yet (404 empty is OK).
   - Does **not** say “Object GUID not available” when the header GUID is present.
   - If the object truly has no GUID, the section still mounts with kind-aware copy
     and does not crash the detail page.

Object ACL on Action Menu and View requires a GUID. The catalog and detail
responses must expose one (or a synthesizable id) before ACL can load.

## Object ACL (design objects)

Developer catalog objects use a layered **Object ACL** (Design access vs Runtime
visibility). Operators set a **default ACL template** under
**Developer → Preferences → Security**. After **Save default ACL template**,
**Runtime visibility → Visible** must still match after a page reload.

See [Object ACL & default template](id:admin-object-acl).

## Folder ACL (Content Explorer)

Site admins edit **folder** access lists from Content Explorer:

1. Open **Explorer** (`spa.jsp?entry=explorer`).
2. Select the folder in the tree or list.
3. Choose **Security** on the view-tools toolbar, or **View → Folder Security**.

The panel shows Admin / Write / Read / View principals. Saving with yourself
removed from every grant prompts a self-lockout confirmation. See
[Content Explorer](id:admin-content-explorer).

## Hardening checklist

- [ ] Change default/install admin passwords immediately.
- [ ] Restrict admin UI exposure (VPN, IP allow lists, SSO).
- [ ] Keep the 8.2 / 8.1.x line patched for security fixes.
- [ ] Review extension JARs and third-party libraries periodically.
- [ ] Ensure backups of security configuration and role maps.

## Related

- [Server operations](id:admin-server-ops)
- [Sites & content structure](id:admin-sites)
- [Architecture & navigation](id:admin-architecture-navigation) (site section landing pages)
