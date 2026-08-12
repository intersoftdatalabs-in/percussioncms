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

## Hardening checklist

- [ ] Change default/install admin passwords immediately.
- [ ] Restrict admin UI exposure (VPN, IP allow lists, SSO).
- [ ] Keep the 8.2 / 8.1.x line patched for security fixes.
- [ ] Review extension JARs and third-party libraries periodically.
- [ ] Ensure backups of security configuration and role maps.

## Related

- [Server operations](id:admin-server-ops)
- [Sites & content structure](id:admin-sites)
