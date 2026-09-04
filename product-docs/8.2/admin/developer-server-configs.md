---
id: admin-developer-server-configs
title: Developer Server Configs
description: Edit and save allow-listed server configuration file bodies from Developer Server Configs chrome
version: "8.2"
order: 50
tags: [admin, developer, server-configs]
---

# Developer Server Configs

**Developer → Server Configs** lists the fixed allow-listed server configuration
files (Workbench system design / `PSConfigurationTypes`: logging, tidy,
navigation, workflow, velocity macros, auth types, and related). Admins can
**open a row**, **edit the file body**, and **Save configuration**. The catalog
key and on-disk file name stay read-only; clients never supply a filesystem path.

Configuration **create** (adding new types outside the allow-list) and
**locking / concurrent edit** remain design gaps on this surface.

## Product path — edit and save

1. Sign in as **Admin** (write calls require the Admin role).
2. Open **Developer → Server Configs**, or deep-link
   `spa.jsp?entry=developer&section=server-configs`.
3. Open a listed row (for example **Logging configuration** / `LOG_CONFIG`).
   Metadata (key, file name, MIME type, encoding) is shown read-only. The
   **Content** editor shows the current file text (empty is allowed).
4. Edit the content and click **Save configuration**. The chrome sends
   `PUT /services/serverconfigs/{name}` with `{ "content": "…" }`. On success
   the detail refreshes from the server response and shows a saved notice.
5. Non-Admin sessions see **403**. Unknown or non-allow-listed names are
   **404**. Missing content on the wire is **400**.

## Limits

- Only allow-listed enum keys are writable; path traversal and unknown names
  are rejected (no arbitrary filesystem write).
- You cannot create a new configuration type from this chrome.
- Locking and concurrent-edit controls are not exposed.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| List | `GET /services/serverconfigs` |
| Load | `GET /services/serverconfigs/{name}` |
| Update | `PUT /services/serverconfigs/{name}` (**Admin**; body must include `content`) |

Integrator notes: [REST API](id:developer-rest).
