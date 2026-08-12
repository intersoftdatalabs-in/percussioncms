---
id: getting-started
title: Getting Started
description: Install and first steps for Percussion CMS 8.2
version: "8.2"
order: 10
tags: [getting-started]
---

# Getting Started

This section covers how to obtain, install, upgrade, and take first steps with Percussion CMS 8.2.

## Topics

- [Installation Overview](id:install-overview) — prerequisites, packages, first start
- [Upgrade Overview](id:upgrade-overview) — paths from prior releases into 8.2

## Who should read this

| Role | Recommended path |
|------|------------------|
| New operator | Install binaries from GitHub Releases → verify login → configure first Site |
| Upgrading customer | Read upgrade notes → back up → run installer upgrade path |
| Developer | [Build from source](id:developer-build-source) after a quick install overview |

## After install

1. Confirm the server process starts and logs under the install `jetty/base/logs` (or platform service logs) are clean.
2. Sign in with the administrative account created at install time.
3. Confirm the SPA top navigation starts with **Home**, then **Explorer** (adjacent).
   There is no **Dashboard** top-nav item. Administrators see a single **Admin**
   item that opens **Admin tools** (`/admin`). See [Administration](id:admin).
4. Create or open a **Site**, confirm Finder navigation, and open the Web UI editor.
5. Review [Server operations](id:admin-server-ops) for ports, service control, and logs.

## Related

- [Administration](id:admin) — day-two operations
- [Developer](id:developer) — REST, extensions, Virtual Sites
