---
id: admin-developer-file-explorer
title: Developer File Explorer
description: Admin REST browse of allow-listed File Explorer roots (not SY-05 application files or SY-02 server configs)
version: "8.2"
order: 51
tags: [admin, developer, file-explorer]
---

# Developer File Explorer

**File Explorer** browse (classic Workbench supporting navigator) is an **Admin**
catalog of **allow-listed** server directories. Operators open it from
**Developer → File Explorer**. Listing is **read-only**. Drag-drop into CMS and
file write/upload are not available on this surface.

This is **not** Developer Server Configs (SY-02) and **not** XML application
CMS/resource files (SY-05).

## Configure allow-listed roots

In `rxconfig/server.properties`:

```properties
fileExplorer.allowListedRoots=rx_resources=rx_resources;drop=/absolute/path/to/drop
```

- Each entry is `id=path` separated by `;`.
- `id` is the catalog token used in REST (`letters`, digits, `_`, `-`; starts with a letter).
- Relative `path` values resolve against the CMS install root.
- Entries that contain `..` are ignored.
- If the property is missing or empty, `GET /services/fileexplorer` returns an empty list
  — the server does not walk the filesystem.

Restart the CMS after changing the property.

## Developer SPA

1. Sign in as **Admin**.
2. Open **Developer** and select the **File Explorer** tab
   (`/cm/app/developer/file-explorer`).
3. The table lists configured roots (`id` and display name). Responses never
   show the server filesystem path.
4. Open a root to list immediate children. Directories open; files are listed
   only (name, type, size). Use the breadcrumb or **Up one folder** to move.
5. If no roots are configured, the panel is empty until
   `fileExplorer.allowListedRoots` is set and the CMS is restarted.

Non-Admin users receive **403** from REST; the panel shows a load error.

## REST (Admin)

| Action | Request |
|--------|---------|
| List roots | `GET /services/fileexplorer` |
| List children | `GET /services/fileexplorer/{rootId}/children?path=` |

Omit `path` (or send blank) to list the root. Relative paths use `/`. Unsafe paths
(`..`, absolute, drive letter, UNC) are **400**. Unknown roots or missing directories
are **404**. Responses never include the raw filesystem path.

Non-Admin is **403**.

Integrator notes: [REST API](id:developer-rest).
