---
id: admin-developer-application-files
title: Developer Application Files
description: Browse and save XML application CMS/resource files from Developer Application Files chrome
version: "8.2"
order: 51
tags: [admin, developer, application-files]
---

# Developer Application Files

**Developer → Application Files** browses **CMS/resource files under a catalog XML
application** (Workbench System Design → CMS / Resource File tree). Admins can
**pick an application**, **open a relative file**, **edit the UTF-8 body**, **Save
file**, **create/delete folders**, and **rename/move** files or folders. Paths are
always relative under the application root — clients never supply an absolute
filesystem path.

This surface is **distinct from Developer → Server Configs** (SY-02), which edits a
fixed allow-list of named server configuration descriptors (`LOG_CONFIG`,
`NAV_CONFIG`, and peers). Application Files never writes those keys.

**Design locking / concurrent edit** and **binary round-trip** remain design gaps
on this surface. **Admin PUT** can also **create a new file** when the relative
path does not yet exist under the application root (overwrite semantics). Listing
marks folders with `directory=true`. Admins use the file list to create a
relative folder, rename/move a path, or delete a file or folder (recursive).

## Product path — browse and save

1. Sign in as **Admin** (write calls require the Admin role; non-Admin sessions
   see Save disabled in the SPA).
2. Open **Developer → Application Files**, or deep-link
   `spa.jsp?entry=developer&section=application-files`.
3. Choose an application from the catalog (same object-store applications as
   **Developer → Pipelines**).
4. Open a listed **file** row (folders are listed read-only). Metadata
   (application, relative path, MIME type, encoding) is shown read-only. The
   **Content** editor shows the current UTF-8 text (empty is allowed). Files
   larger than **2 MB** are blocked from in-browser edit.
5. Edit the content and click **Save file**. The chrome sends
   `PUT /services/applicationfiles/{app}/content?path=` with
   `{ "ApplicationFile": { "content": "…" } }`. On success the detail refreshes
   from the server response and shows a saved notice. Saving to a path that does
   not yet exist under the application root **creates** that file (Admin only).
6. From the file list, Admins can enter a relative folder path and click **Create
   folder**, **Rename / move** a row to a new relative path, or **Delete** a file
   or folder (confirm dialog). The list refreshes after each action.
7. Non-Admin sessions cannot enable Save or folder actions (SPA) and receive
   **403** from the API if they call write endpoints directly. Unknown
   applications or missing paths are **404**. Traversal / unsafe paths on folder
   create, delete, and rename/move are **400**. Missing body/content/path on the
   wire is **400**. PUT of an unsafe path remains **404** (same as GET).

## Limits

- Only files under a resolved catalog application are writable; path traversal and
  absolute paths are rejected (no arbitrary filesystem write).
- Folder create/delete and rename/move are Admin-only. Traversal is **400**;
  unknown app/path is **404**.
- Binary-safe round-trip is not exposed here.
- Admin PUT may create a new relative file path under the application root.
- Locking and concurrent-edit controls are not exposed.
- Browser editor refuses bodies larger than 2 MB.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| List apps | `GET /services/pipelines` (application picker) |
| List files | `GET /services/applicationfiles/{app}` |
| Load | `GET /services/applicationfiles/{app}/content?path=` |
| Update | `PUT /services/applicationfiles/{app}/content?path=` (**Admin**; body must include `content`) |
| Create folder | `POST /services/applicationfiles/{app}/folders?path=` (**Admin**) |
| Delete file or folder | `DELETE /services/applicationfiles/{app}/content?path=` (**Admin**; folders are recursive) |
| Rename / move | `POST /services/applicationfiles/{app}/move` (**Admin**; body `{ "ApplicationFileMove": { "fromPath": "…", "toPath": "…" } }`) |

Integrator notes: [REST API](id:developer-rest) → Application CMS/resource files (SY-05).
