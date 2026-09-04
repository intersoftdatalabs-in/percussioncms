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
**pick an application**, **open a relative file**, **edit the UTF-8 body**, and
**Save file**. Paths are always relative under the application root — clients never
supply an absolute filesystem path.

This surface is **distinct from Developer → Server Configs** (SY-02), which edits a
fixed allow-list of named server configuration descriptors (`LOG_CONFIG`,
`NAV_CONFIG`, and peers). Application Files never writes those keys.

**Design locking / concurrent edit**, **binary round-trip**, and **folder
create/delete/rename** remain design gaps on this surface.

## Product path — browse and save

1. Sign in as **Admin** (write calls require the Admin role).
2. Open **Developer → Application Files**, or deep-link
   `spa.jsp?entry=developer&section=application-files`.
3. Choose an application from the catalog (same object-store applications as
   **Developer → Pipelines**).
4. Open a listed **file** row (folders are listed read-only). Metadata
   (application, relative path, MIME type, encoding) is shown read-only. The
   **Content** editor shows the current UTF-8 text (empty is allowed).
5. Edit the content and click **Save file**. The chrome sends
   `PUT /services/applicationfiles/{app}/content?path=` with
   `{ "ApplicationFile": { "content": "…" } }`. On success the detail refreshes
   from the server response and shows a saved notice.
6. Non-Admin sessions see **403**. Unknown applications or unsafe/missing paths
   are **404**. Missing content on the wire is **400**.

## Limits

- Only files under a resolved catalog application are writable; path traversal and
  absolute paths are rejected (no arbitrary filesystem write).
- Folder create/delete/rename and binary-safe round-trip are not exposed here.
- Locking and concurrent-edit controls are not exposed.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| List apps | `GET /services/pipelines` (application picker) |
| List files | `GET /services/applicationfiles/{app}` |
| Load | `GET /services/applicationfiles/{app}/content?path=` |
| Update | `PUT /services/applicationfiles/{app}/content?path=` (**Admin**; body must include `content`) |

Integrator notes: [REST API](id:developer-rest) → Application CMS/resource files (SY-05).
