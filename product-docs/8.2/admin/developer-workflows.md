---
id: admin-developer-workflows
title: Developer Workflows
description: Browse workflow definitions and edit allowed content types from Developer Workflows chrome
version: "8.2"
order: 46
tags: [admin, developer, workflows]
---

# Developer Workflows

**Developer → Workflows** lists stepped workflow definitions (name, default flag,
description, staging roles, and steps). Open a row to inspect steps and to edit
**Allowed content types** for that workflow (SY-06). Admins can also **create**
a named workflow from the catalog; the new row opens and lists on the catalog.

Full workflow graph design and workflow update / delete stay outside
this chrome.

## Product path — create a workflow (slice 21)

1. Sign in as **Admin**.
2. Open **Developer → Workflows**, or deep-link
   `spa.jsp?entry=developer&section=workflows`.
3. Click **New workflow**.
4. Enter a **Name** (required, unique; letters, digits, underscore, hyphen,
   and space; max 50 characters) and an optional **Description**.
5. Click **Create workflow**. States, transitions, and roles come from the
   product base-workflow template (same backend the workflow-admin editor
   uses). The catalog refreshes and the new workflow opens.
6. Errors such as duplicate names (`409`), invalid names (`400`), or
   non-Admin callers (`403`) appear in the section alert.

## Product path — allowed content types (SY-06)

Content-type ↔ workflow associations can be edited from **either** side:

| Side | Surface |
|------|---------|
| Content type → workflows | [Developer Content Types](id:admin-developer-content-types) (CD-08; requires a held content-type design lock) |
| Workflow → content types | **Developer → Workflows** detail (SY-06; Admin; no client-held lock) |

To associate content types from the workflow side:

1. Sign in as **Admin**.
2. Open **Developer → Workflows**, or deep-link
   `spa.jsp?entry=developer&section=workflows`.
3. Open a workflow (for example **Simple Workflow**).
4. Under **Allowed content types**, add content types by **name** or remove
   existing rows. The list is a full-replace set on save.
5. Click **Save content types**. The server acquires and releases a design lock
   on each affected content type. An empty list clears associations for this
   workflow.
6. Confirm the list refreshes with the saved set. Errors such as unknown content
   type names (`400`), non-Admin callers (`403`), missing workflows (`404`), or
   design-lock conflicts (`409`) appear in the section alert.

Content-type side editing (default workflow, held lock) remains on Content Type
detail — see [Developer Content Types](id:admin-developer-content-types).

## Limits

- Workflow update / delete and full graph design are not in this chrome.
- Object ACL is not available on workflow detail (no workflow GUID in this
  release).
- Association save requires Admin and the SY-06 REST surface
  (`/services/workflows/{idOrName}/allowedContentTypes`).
- Create requires Admin and the slice 21 REST surface
  (`POST /services/workflows`).

## REST

| Action | Request |
|--------|---------|
| List metadata | `GET /services/workflowmanagement/workflows/metadata` |
| Load detail | `GET /services/workflowmanagement/workflows/{name}` |
| Create workflow | `POST /services/workflows` (`WorkflowCreate` wrap; Admin; duplicate `409`) |
| List allowed content types | `GET /services/workflows/{idOrName}/allowedContentTypes` |
| Replace allowed content types | `PUT /services/workflows/{idOrName}/allowedContentTypes` (`WorkflowContentTypes` wrap) |

Integrator notes: [REST API — Workflows](id:developer-rest).
