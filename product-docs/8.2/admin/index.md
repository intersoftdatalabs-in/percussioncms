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

The top navigation exposes a **single Admin** item for administrators. Admin opens one
unified **Admin** product shell with tabs for:

| Tab | Purpose |
|-----|---------|
| Scheduled Tasks | Create and run scheduled CMS tasks |
| Execution Logs | Review task run history |
| Notification Settings | Task email notification templates |
| System Tools | Security audit log, consistency checker |
| Workflow | Workflow definitions and site/folder assignment |
| Roles | Role membership |
| Users | User accounts and default landing |
| Categories | Category tree administration |

Legacy bookmarks and deep links under `/workflow` and `/workflow/:tab` redirect into the
matching Admin tab (for example `/admin/workflow`, `/admin/roles`). There is no separate
Workflow administration shell or sibling cross-link between Admin tools and Administration.

Non-administrators never see the Admin top-nav item or these configuration surfaces.

## Topics

- [Sites & content structure](id:admin-sites)
- [Content Explorer](id:admin-content-explorer)
- [Architecture & site navigation](id:admin-architecture-navigation)
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
