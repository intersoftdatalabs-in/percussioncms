---
id: admin-developer-workflows
title: Developer Workflows
description: Browse workflow definitions, create / copy / update / delete workflows, and edit allowed content types from Developer Workflows chrome
version: "8.2"
order: 46
tags: [admin, developer, workflows]
---

# Developer Workflows

**Developer → Workflows** lists stepped workflow definitions (name, default flag,
description, staging roles, and steps). Open a row to inspect steps and to edit
**Allowed content types** for that workflow (SY-06). Admins can also **create**,
**copy** (steps and transitions included), **edit** the description, and
**delete** a workflow from the catalog. The new or copied row opens and lists
on the catalog. A copy name that already exists is rejected and does not
overwrite.

Workflow renaming stays outside this chrome. **Developer → Workflows** detail
shows a step list and a graph of states and transitions
(`GET .../workflows/{id}/graph`). On a **custom** workflow an Admin can
**delete one transition** between existing steps, or **delete one step** that
no transition still uses. Creating transitions, and deletes on packaged
workflows, stay outside this chrome.
The graph badge says **Packaged workflow** for Default Workflow, Simple
Workflow, Local Content, and any workflow the server marks as the default;
other workflows show **Custom workflow**. Missing workflows (`404`) and
non-Admin callers (`403`) surface as section alerts — not a blank success body.

## Product path — browse steps (read-only)

1. Sign in as **Admin**.
2. Open **Developer → Workflows**, or deep-link
   `spa.jsp?entry=developer&section=workflows`.
3. Open a workflow row (for example **Simple Workflow**).
4. Under **Steps**, confirm each state is listed. Transition names appear in
   the **Transitions** column when the REST payload includes
   `stepRoles[].roleTransitions` (Workbench-style `transitionPermission`).
5. If the workflow has no steps, the section shows **None** (not an empty
   success table). Load errors (`403` / `404`) appear in the detail alert.

This is a catalog preview of steps. Adding or renaming steps remains on the
detail form for custom workflows. Creating transitions remains on the
workflow-admin editor. Deleting one existing transition, or a step that no
longer has transitions, is on the graph (see below).

## Product path — browse the graph

1. Sign in as **Admin**.
2. Open **Developer → Workflows** and open a workflow (for example **Simple
   Workflow**).
3. Under **Graph**, confirm each state is a node and each transition is an
   edge (`from — label → to`).
4. Confirm the badge: **Packaged workflow** for stock/default workflows,
   **Custom workflow** otherwise.
5. If the workflow has no states, the section shows **No states in this
   workflow graph** (not a blank success). Load errors (`403` / `404`) appear
   in the graph alert.

## Product path — delete one transition (slice 33)

1. Sign in as **Admin**.
2. Open **Developer → Workflows** and open a **custom** workflow (not Default
   Workflow, Simple Workflow, or Local Content).
3. Under **Graph**, click **Delete transition** on one edge.
4. Confirm in the in-app dialog. The edge disappears. The source and
   destination steps remain under **Steps** and as graph nodes. Reopening the
   workflow shows the same edge gone.
5. Packaged workflows do not show **Delete transition**. Calling the delete
   API on them returns `403`. A missing workflow, step, or transition returns
   `404`. A blank `from` or `label`, or a label that matches more than one
   edge without `to`, returns `400`. Non-Admin callers receive `403`.

## Product path — delete one step (slice 34)

1. Sign in as **Admin**.
2. Open **Developer → Workflows** and open a **custom** workflow.
3. Remove every transition that starts or ends on the step you want to delete
   (**Delete transition** on the graph). A step that still has a transition
   cannot be deleted.
4. Under **Graph**, click **Delete step** on that state and confirm.
5. The state disappears from the graph. Reopening the workflow shows it gone.
6. Packaged workflows do not show **Delete step** (`403` on the API). A missing
   workflow or step is `404`. An invalid step name is `400`. A step that a
   transition still uses is `409`. Non-Admin callers receive `403`.

## Product path — copy a workflow (slice 36)

1. Sign in as **Admin**.
2. Open **Developer → Workflows**, or deep-link
   `spa.jsp?entry=developer&section=workflows`.
3. On a catalog row, click **Copy** (the source workflow is not changed).
4. Enter a **Name** that does not already exist (same character rules as
   create: letters, digits, underscore, hyphen, and space; max 50 characters)
   and an optional **Description**. When description is left blank, the copy
   keeps the source description.
5. Click **Copy workflow**. The server copies that workflow's steps and
   transitions onto the new name and opens the copy. **Cancel** returns to the
   catalog and does not create a workflow.
6. A name that already exists returns `409`. The form stays open and neither
   the existing workflow nor the source is overwritten. An invalid name is
   `400`. A missing source is `404`. Non-Admin callers receive `403`.

The public call is `POST /services/workflows/{idOrName}/copy` with a
`WorkflowCreate` body (`name` required).

## Product path — create a workflow (slice 21)

1. Sign in as **Admin**.
2. Open **Developer → Workflows**, or deep-link
   `spa.jsp?entry=developer&section=workflows`.
3. Click **New workflow**. The button is also shown when the catalog is empty.
4. Enter a **Name** (required, unique; letters, digits, underscore, hyphen,
   and space; max 50 characters) and an optional **Description**. The form
   lists the default steps the server will copy (**Draft**, **Review**,
   **Pending**, **Live**, **Quick Edit**, **Archive**). That set is not
   editable on create.
5. Click **Create workflow**. States, transitions, and roles come from the
   product base-workflow template (same backend the workflow-admin editor
   uses). **Cancel** returns to the catalog and does not create a workflow.
   The catalog refreshes and the new workflow opens.
6. Errors such as duplicate names (`409`), invalid names (`400`), or
   non-Admin callers (`403`) appear in the section alert.

## Product path — edit a workflow description (slice 21)

1. Sign in as **Admin**.
2. Open **Developer → Workflows**, or deep-link
   `spa.jsp?entry=developer&section=workflows`.
3. Open a workflow row.
4. Under **Description**, change the text. The **Save** button enables once
   the description diverges from the stored value.
5. Click **Save**. The new description is persisted immediately and the
   field becomes read-only again until the next edit.
6. Errors such as mismatched names on the update body (`400`), missing
   workflow (`404`), or non-Admin callers (`403`) appear in the section
   alert.

Renaming, step / transitions / roles editing, and full graph design stay
on the workflow-admin editor — this chrome updates only the description.

## Product path — set the system default (slice 37)

The catalog **Default** column shows **Yes** for the single system default
workflow (`DEFAULT_WORKFLOW` in workflow properties). Detail shows the same
flag.

1. Sign in as **Admin**.
2. Open **Developer → Workflows**.
3. Open a workflow that is not already the default.
4. Next to **Default**, click **Set as default**.
5. The detail flag becomes **Yes**. Go **Back to list**: that row shows
   **Yes** and the previous default row no longer does.
6. Missing workflows (`404`) and non-Admin callers (`403`) appear in the
   section alert.

Setting the default does not move existing content items onto the new
workflow. Content-type default workflow (which workflow a type uses for new
items) stays on Content Type detail. The public call is
`POST /services/workflows/{idOrName}/default` (no body). The graph treats the
system default as a packaged workflow, so step and transition deletes stay
off for whichever workflow currently holds the flag.

## Product path — delete a workflow (slice 21)

1. Sign in as **Admin**.
2. Open **Developer → Workflows**, or deep-link
   `spa.jsp?entry=developer&section=workflows`.
3. Open a workflow row.
4. Click **Delete workflow**.
5. Confirm in the in-app dialog. The catalog refreshes and the row is removed.
6. Errors such as system-workflow deletes (`409`), workflows that still own
   content items (`409`), missing workflows (`404`), or non-Admin callers
   (`403`) appear in the section alert.

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

- Add or rename a **step** on a custom (non-packaged) workflow from the
  detail panel. Packaged **Default Workflow**, **Simple Workflow**, and
  **Local Content** stay protected (`403`). Invalid names return `400`.
  Creating transitions stays on the workflow-admin editor. Deleting one
  existing transition on a custom workflow is `DELETE
  .../workflows/{idOrName}/transitions`. Deleting one step that no transition
  still uses is `DELETE .../workflows/{idOrName}/steps/{stepName}` (`409` when
  a transition still references the step).
- Workflow rename and full graph design (states, transitions, roles) are
  not in this chrome; they stay on the workflow-admin editor.
- Object ACL is not available on workflow detail (no workflow GUID in this
  release).
- Association save requires Admin and the SY-06 REST surface
  (`/services/workflows/{idOrName}/allowedContentTypes`).
- Create / update / delete require Admin and the slice 21 REST surface
  (`/services/workflows`).

## REST

| Action | Request |
|--------|---------|
| List metadata | `GET /services/workflowmanagement/workflows/metadata` |
| Load detail | `GET /services/workflowmanagement/workflows/{name}` |
| Create workflow | `POST /services/workflows` (`WorkflowCreate` wrap; Admin; duplicate `409`) |
| Update description | `PUT /services/workflows/{idOrName}` (`WorkflowUpdate` wrap; Admin; name must match; missing workflow `404`) |
| Delete workflow | `DELETE /services/workflows/{idOrName}` (Admin; system workflows and item owners return `409`) |
| Create step | `POST /services/workflows/{idOrName}/steps` (`WorkflowStepWrite` wrap; Admin; packaged workflows `403`) |
| Update step | `PUT /services/workflows/{idOrName}/steps/{stepName}` (`WorkflowStepWrite` wrap; Admin; packaged workflows `403`) |
| Read graph | `GET /services/workflows/{idOrName}/graph` (Admin; states and transitions; `packaged` true for stock or default workflows) |
| Delete one transition | `DELETE /services/workflows/{idOrName}/transitions?from={step}&label={label}&to={step}` (Admin; does not delete steps; packaged workflows `403`; missing workflow/step/transition `404`; blank or ambiguous label `400`) |
| Delete one step | `DELETE /services/workflows/{idOrName}/steps/{stepName}` (Admin; only when no transition still uses the step; returns the updated graph; packaged workflows `403`; missing workflow or step `404`; invalid step name `400`; step still referenced `409`) |
| List allowed content types | `GET /services/workflows/{idOrName}/allowedContentTypes` |
| Replace allowed content types | `PUT /services/workflows/{idOrName}/allowedContentTypes` (`WorkflowContentTypes` wrap) |

Integrator notes: [REST API — Workflows](id:developer-rest).
