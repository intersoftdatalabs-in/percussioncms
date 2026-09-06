---
id: admin-developer-problems
title: Developer Problems
description: Admin read-only list of design/validation problems for the open Developer editor and session
version: "8.2"
order: 53
tags: [admin, developer, problems]
---

# Developer Problems

**Problems** (classic Workbench supporting panel) is an **Admin** catalog of
**design and validation problems** for the open Developer editor and session.
Operators open it from **Developer → Problems**. The list is **read-only**.
Save, repair, and write tools are not available on this surface.

This is **not** the **Problems** section on a Developer **Pipelines** application
detail page (`GET /services/pipelines/{id}/validation`).

## Developer SPA

1. Sign in as **Admin**.
2. Open **Developer** and select the **Problems** tab
   (`/cm/app/developer/problems`).
3. The table lists session problems: severity, source, message, and optional
   location.
4. When a peer editor exists, **Open source** switches to that Developer tab
   (for the known invalid open-editor fixture, **Content Types**).
5. This increment always includes a **known invalid open-editor/session fixture**
   so the list is not empty on a fresh session. Later slices bind live editor
   validators.

Non-Admin users receive **403** from REST; the panel shows a load error.

## REST (Admin)

| Action | Request |
|--------|---------|
| List session problems | `GET /services/problems` |
| Known invalid-session fixture | `GET /services/problems?fixture=invalid-session` |

Unsafe fixture tokens (parent traversal, paths, JDBC-like text) are **400**.
Error bodies do **not** echo filesystem paths or JDBC URLs.

Non-Admin is **403**.

Integrator notes: [REST API](id:developer-rest).
