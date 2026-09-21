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
**pick an application**, **open a relative file**, **Lock**, **edit the UTF-8
body** (or **Download / Replace** a binary body), **Save file**, **Unlock**,
**create/delete folders**, and **rename/move** files or folders. Paths are always
relative under the application root — clients never supply an absolute filesystem
path.

This surface is **distinct from Developer → Server Configs** (SY-02), which edits a
fixed allow-list of named server configuration descriptors (`LOG_CONFIG`,
`NAV_CONFIG`, and peers). Application Files never writes those keys.

**Binary files** (non-UTF-8 or NUL-containing bodies) open in a download/replace
view instead of the text editor: the chrome shows a **Size** row and **Download**
/ **Replace file** actions backed by `GET/PUT …/binary?path=`. **Admin PUT** can also
**create a new file** when the relative
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
   (application, relative path, MIME type, encoding, size) is shown read-only. For
   **text** files, the **Content** editor shows the current UTF-8 text (empty is
   allowed); files larger than **2 MB** are blocked from in-browser edit. **Binary**
   files (non-UTF-8 bodies) instead show a **Download** button and a **Replace
   file** picker — the raw bytes are fetched/sent as octet-stream and are never
   loaded into the text editor.
5. Click **Lock** (Admin). Save stays disabled until the lock is held. Edit the
   content and click **Save file**. The chrome sends
   `PUT /services/applicationfiles/{app}/content?path=` with
   `{ "ApplicationFile": { "content": "…" } }` while the lock is held. On success
   the detail refreshes from the server response and shows a saved notice. Saving
   to a path that does not yet exist under the application root **creates** that
   file (Admin only). Click **Unlock** to release the design session (or **Back**,
   which best-effort unlocks). A stale or missing lock on PUT is **409**.
6. From the file list, Admins can enter a relative folder path and click **Create
   folder**, **Rename / move** a row to a new relative path, or **Delete** a file
   or folder (confirm dialog). The list refreshes after each action.
7. **Binary files**: click **Download** to fetch the raw body through the browser
   (the suggested filename matches the file name). Click **Lock**, then **Replace
   file** to pick a local file and send its raw bytes (requires the same held
   lock as Save). If the replacement body is valid UTF-8 text, the file flips
   back to the text editor for subsequent edits.
8. Non-Admin sessions cannot enable Save, Download, or folder actions (SPA) and receive
   **403** from the API if they call write endpoints directly. Unknown
   applications or missing paths are **404**. Traversal / unsafe paths on folder
   create, delete, and rename/move are **400**. Missing body/content/path on the
   wire is **400**. PUT of an unsafe path remains **404** (same as GET).

## Limits

- Only files under a resolved catalog application are writable; path traversal and
  absolute paths are rejected (no arbitrary filesystem write).
- Folder create/delete and rename/move are Admin-only. Traversal is **400**;
  unknown app/path is **404**.
- Binary bodies are downloaded/replaced as raw octet-stream; they are never edited
  as text in the browser. Binary download and replace require Admin; replace also
  requires a held design lock.
- Admin PUT may create a new relative file path under the application root.
- Save requires a held design lock (POST lock). Stale lock is **409**.
- Browser editor refuses text bodies larger than 2 MB (binary bodies are not
  subject to the editor ceiling — they round-trip as bytes).

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| List apps | `GET /services/pipelines` (application picker) |
| List files | `GET /services/applicationfiles/{app}` |
| Load | `GET /services/applicationfiles/{app}/content?path=` |
| Download binary | `GET /services/applicationfiles/{app}/binary?path=` (**Admin**; octet-stream bytes for `binary=true` files) |
| Lock | `POST /services/applicationfiles/{app}/lock?path=` (**Admin**) |
| Update | `PUT /services/applicationfiles/{app}/content?path=` (**Admin**; requires held lock; body must include `content`) |
| Replace binary | `PUT /services/applicationfiles/{app}/binary?path=` (**Admin**; raw octet-stream body; requires held lock) |
| Unlock | `POST /services/applicationfiles/{app}/unlock?path=` (**Admin**) |
| Create folder | `POST /services/applicationfiles/{app}/folders?path=` (**Admin**) |
| Delete file or folder | `DELETE /services/applicationfiles/{app}/content?path=` (**Admin**; folders are recursive) |
| Rename / move | `POST /services/applicationfiles/{app}/move` (**Admin**; body `{ "ApplicationFileMove": { "fromPath": "…", "toPath": "…" } }`) |

Integrator notes: [REST API](id:developer-rest) → Application CMS/resource files (SY-05).
