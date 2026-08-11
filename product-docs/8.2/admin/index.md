---
id: admin
title: Administration
description: Operating and administering Percussion CMS 8.2
version: "8.2"
order: 40
tags: [admin]
---

# Administration

Operator and administrator topics for Percussion CMS 8.2 — Sites, security, publishing, and
day-two server operations.

## Product Admin navigation (SPA)

The top navigation exposes a **single Admin** item for administrators:

1. **Admin** opens the **Admin tools** shell (scheduled tasks, execution logs, notification
   settings, and system tools such as the security audit log and consistency checker).
2. From Admin tools, use the **Administration** sibling link (page header, right side) to open
   workflow / users / roles / categories administration.
3. From Administration, use the **Admin tools** sibling link to return to the tools shell.

There are no separate top-nav entries for Administration and Admin tools. Both surfaces share
the consolidated Admin highlight in the top bar.

## Topics

- [Sites & content structure](id:admin-sites)
- [Content Explorer](id:admin-content-explorer)
- [Users, roles & security](id:admin-users-roles)
- [Publishing](id:admin-publishing)
- [Server operations](id:admin-server-ops)

## Day-two checklist

| Task | Frequency | Notes |
|------|-----------|--------|
| Review server logs | Daily | Jetty/base logs under the install tree |
| Backup DB + config | Per policy | Before upgrades and major package changes |
| User/role audit | Quarterly | Remove stale accounts; least privilege |
| Publish health | After deploy | Verify last successful pub jobs and delivery targets |
| Certificate expiry | Before TLS renewals | Edge proxy and any embedded keystores |

## Related

- [Getting Started](id:getting-started)
- [Reference](id:reference)
