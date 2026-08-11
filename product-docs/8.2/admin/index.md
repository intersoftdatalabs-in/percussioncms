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
