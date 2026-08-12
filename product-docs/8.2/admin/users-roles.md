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

## My profile — Security (password)

Authenticated users open **My profile** from the header user menu (or deep link
`spa.jsp?entry=profile`). The **Security** section behaves by account type:

| Account type | What the user sees |
|--------------|--------------------|
| **Internal** (local auth) | Change-password form: new password + confirm. The product calls the existing self-only `PUT /user/user/changepw` endpoint; you cannot change another user’s password from this page. |
| **Directory / SSO** | A localized explanation that credentials are managed by the identity provider or IT — **no** password form that would always fail. |

Password rules enforced by the server (complexity / history filters when configured)
still apply. Client-side checks require a non-empty password of at least six characters
and a matching confirmation before submit. Success and failure messages are announced
to assistive technology via a live region.

## Default landing (homepage)

In **My profile → Preferences**, or **Admin → Users** when editing a user, set
**Default landing page**. Choose **Navigation** (stored as homepage type
`Architecture`) to open the site Navigation SPA after sign-in. Leave **Use role
default** to keep the role homepage. Login without a deep-link return URL posts
to `/cm/app/` so the dispatcher applies this preference.

See [Architecture & site navigation](id:admin-architecture-navigation).

## Design-object ACL (Developer)

System Definition (**Developer**) shows an **Object ACL** section on securable design
objects, including **Display Formats** and **Sites**.

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

## Object ACL (design objects)

Developer catalog objects use a layered **Object ACL** (Design access vs Runtime
visibility). Operators set a **default ACL template** under
**Developer → Preferences → Security**. After **Save default ACL template**,
**Runtime visibility → Visible** must still match after a page reload.

See [Object ACL & default template](id:admin-object-acl).

## Hardening checklist

- [ ] Change default/install admin passwords immediately.
- [ ] Restrict admin UI exposure (VPN, IP allow lists, SSO).
- [ ] Keep the 8.2 / 8.1.x line patched for security fixes.
- [ ] Review extension JARs and third-party libraries periodically.
- [ ] Ensure backups of security configuration and role maps.

## Related

- [Server operations](id:admin-server-ops)
- [Sites & content structure](id:admin-sites)
