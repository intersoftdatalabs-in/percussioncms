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
   item that opens **Admin tools** (`/admin`). **Editor**, **Design**, and
   **Widget Builder** are not top-nav items. See [Administration](id:admin).
   The template library is under **Developer → Design** — classic
   `?view=design` / `admin.jsp` bookmarks still redirect there. See
   [Design templates](id:admin-design-templates).
4. Create or open a **Site**, confirm Explorer navigation, and open the React Content Editor from Explorer **Edit** or **Home → Create** (page, blog, or asset). Those surfaces do not open leftover `?view=editor` or `editAsset.jsp`.
5. Open **Developer → Design** to list assembly templates and edit source, JEXL bindings, assembler,
   and slots. See [Design templates](id:admin-design-templates).
6. Review [Server operations](id:admin-server-ops) for ports, service control, and logs.

## Related

- [Administration](id:admin) — day-two operations
- [Developer](id:developer) — REST, extensions, Virtual Sites
