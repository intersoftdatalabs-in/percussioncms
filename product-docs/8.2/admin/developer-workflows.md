---
id: admin-developer-workflows
title: Developer Workflows
description: Browse workflow definitions, create / copy / rename / update / delete workflows, set notify on a step role, and edit allowed content types from Developer Workflows chrome
version: "8.2"
order: 46
tags: [admin, developer, workflows]
---

# Developer Workflows

**Developer → Workflows** lists stepped workflow definitions (name, default flag,
description, staging roles, and steps). Open a row to inspect steps and to edit
**Allowed content types** for that workflow (SY-06). Admins can also **create**,
**copy** (steps and transitions included), **rename** one custom workflow,
**edit** the description, and **delete** a workflow from the catalog. The new,
copied, or renamed row opens and lists on the catalog. A copy or rename name
that already exists is rejected and does not overwrite.

**Developer → Workflows** detail shows a step list and a graph of states and transitions
(`GET .../workflows/{id}/graph`). On a **custom** workflow an Admin can
**add or update one transition** between existing steps, **add one absolute
aging transition** (interval in minutes), **change that minute interval**,
**delete that aging transition**, **delete one regular transition**, or
**delete one step** that no transition still uses. An Admin can also set
Reader or Assignee on a role already assigned to a step, turn **notify** on
or off for one role already on a step, add one existing workflow role onto a
step, and remove one Reader or Assignee role from a step. Inbox flags,
repeated or system-field aging, and writes on packaged workflows stay outside
this chrome.
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
detail form for custom workflows. Adding or updating one transition between
existing steps, deleting one existing transition, or deleting a step that no
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

## Product path — add or update a transition (slice 31)

1. Sign in as **Admin**.
2. Open **Developer → Workflows** and open a **custom** workflow (not Default
   Workflow, Simple Workflow, or Local Content, and not the system default).
3. Under **Graph**, choose **From step**, enter a **Transition label**, choose
   **To step**, and click **Add transition**. Both steps must already exist.
   The new edge appears (`from — label → to`). Reopening the workflow shows
   the same edge.
4. Click **Edit transition** on that edge, change the label and/or **To
   step**, and click **Save transition**. The previous edge is gone and the
   updated edge remains after reload. **Cancel** does not call the server.
5. The source step does not move. This does not create or delete steps, and
   it does not edit role assignment or aging intervals.
6. Packaged workflows do not show the form (`403` on the API). A missing
   workflow or step is `404`. A blank or invalid label, or a label that
   matches more than one edge without `to` on update, is `400`. An edge that
   already uses that from / label / to is `409`. Non-Admin callers receive
   `403`.

The public calls are `POST /services/workflows/{idOrName}/transitions` with a
`WorkflowTransitionWrite` body (`from`, `to`, `label`) and
`PUT /services/workflows/{idOrName}/transitions?from={step}&label={label}&to={step}`
with a `WorkflowTransitionWrite` body (`label` and `to` are the new values).

## Product path — add an absolute aging transition (slice 57)

1. Sign in as **Admin**.
2. Open **Developer → Workflows** and open a **custom** workflow (not Default
   Workflow, Simple Workflow, or Local Content, and not the system default).
3. Under **Graph**, in **Aging transitions**, choose **Aging from step**,
   choose **Aging to step** (both steps must already exist), enter a positive
   **Interval (minutes)**, and click **Add aging transition**.
4. The new row appears only after the server accepts it
   (`from — Aging {minutes} → to`, with the minute count). Reopening the
   workflow shows the same row. **Cancel** clears the draft and does not call
   the server.
5. A blank destination or a blank, zero, or negative interval is rejected in
   the form and does not show a saved notice. Packaged workflows do not show
   the form (`403` on the API). A missing workflow or step is `404`. An
   absolute aging transition that already uses that from, to, and interval is
   `409`. HTTP `400`, `403`, and `409` do not show the saved notice.
6. This does not create steps, change an existing aging interval, delete an
   aging transition, or assign roles. Comment required does not apply to
   aging transitions.

The public call is `POST /services/workflows/{idOrName}/aging-transitions`
with a `WorkflowAgingTransitionWrite` body (`from`, `to`, and a positive
`intervalMinutes`). The interval unit is minutes.

## Product path — change an absolute aging interval (slice 58)

1. Sign in as **Admin**.
2. Open **Developer → Workflows** and open a **custom** workflow that already
   has an absolute aging transition.
3. Under **Graph**, in **Aging transitions**, click **Change interval** on
   that row, enter a different positive **New interval (minutes)**, and click
   **Save interval**.
4. The row shows the new minute count (`from — Aging {minutes} → to`, and the
   minute count) only after the server accepts it. Reopening the workflow
   shows the same new interval. **Cancel** closes the editor and does not
   call the server. The destination step and the aging type do not change.
5. A blank, zero, negative, or unchanged interval is rejected in the form and
   does not show a saved notice. Packaged workflows do not show **Change
   interval** (`403` on the API). A missing workflow, step, or absolute aging
   edge is `404`. An absolute aging transition that already uses that from,
   to, and new interval is `409`. HTTP `400`, `403`, and `409` leave the
   previous minutes on the row.
6. This does not add or delete an aging transition, move the destination
   step, change repeated or system-field aging, or assign roles.

The public call is `PUT /services/workflows/{idOrName}/aging-transitions/interval`
with a `WorkflowAgingIntervalWrite` body (`from`, `to`, the current
`intervalMinutes`, and a different positive `newIntervalMinutes`).

## Product path — delete an absolute aging transition (slice 59)

1. Sign in as **Admin**.
2. Open **Developer → Workflows** and open a **custom** workflow that already
   has an absolute aging transition.
3. Under **Graph**, in **Aging transitions**, click **Delete aging
   transition** on that row.
4. **Cancel** closes the dialog and does not call the server. The row stays.
5. Confirm in the in-app dialog. The aging row disappears only after the
   server accepts the delete. Reopening the workflow shows the same row gone.
   A regular transition on the same steps stays, including its **Delete
   transition** action.
6. Packaged workflows do not show **Delete aging transition** (`403` on the
   API). A blank step or a non-positive interval is `400`. A missing workflow,
   step, or absolute aging edge is `404`. A repeated or system-field aging
   transition that uses the same interval is `409` and is not deleted. HTTP
   `400`, `403`, and `409` do not show the deleted notice.
7. This does not delete steps, remove a regular transition, change an aging
   interval, or assign roles.

The public call is
`DELETE /services/workflows/{idOrName}/aging-transitions?from={step}&to={step}&intervalMinutes={minutes}`.

## Product path — comment required on a transition (slice 38)

1. Sign in as **Admin**.
2. Open **Developer → Workflows** and open a **custom** workflow.
3. Under **Graph**, check **Comment required** on one edge.
4. The graph reloads with that edge still checked. Clearing the box makes the
   comment optional again.
5. When the flag is set, the Content Editor and Explorer transition dialogs
   block submit until the comment is not blank. Those dialogs read the flag
   from the item's allowed transitions, not from a hard-coded trigger name.
6. Packaged workflows do not show the checkbox (`403` on the API). Aging
   transitions cannot store the flag (`400`). A missing transition is `404`.

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

This description field updates only the description. A name that does not
match the path on `PUT /services/workflows/{idOrName}` is still `400`.
Renaming a custom workflow is a separate confirm on this detail (see below).
Setting Reader or Assignee on a role already assigned to a step is a separate
confirm (see below). Adding one existing role onto one step is a separate
confirm (see below). Removing one Reader or Assignee role from one step is a
separate confirm (see below). Notify for one role already on a step is a
separate confirm (see below). Inbox flags stay on the workflow-admin editor.
Step and transition edits are on the detail form and graph.

## Product path — rename a custom workflow (slice 60)

1. Sign in as **Admin**.
2. Open **Developer → Workflows** and open a **custom** workflow (not Default
   Workflow, Simple Workflow, or Local Content, and not the system default).
3. Under **Rename workflow**, the name field shows the current name. Change
   it and click **Rename**. The detail title and, after **Back to list**, the
   catalog row show the new name only after the server accepts it. Description,
   steps, transitions, and roles stay as they were.
4. Click **Cancel** to restore the current name. Cancel does not call the
   server.
5. Packaged workflows and the system default do not show **Rename** and do
   not call the server. They show that those workflows cannot be renamed.
6. A duplicate name is `409`. An invalid name (blank, too long, or characters
   other than letters, digits, underscore, hyphen, and space) is `400`.
   Non-Admin callers receive `403`. None of those responses claim the rename
   succeeded, and the title stays on the previous name.

The public call is `POST /services/workflows/{idOrName}/rename` with a
`WorkflowRename` body (`name` required). It is not the description-only
`PUT`.

## Product path — set assignment type (slice 61)

On one step of a **custom** workflow, change Reader or Assignee for one role
that is already assigned to that step. The table shows the new type only after
the server accepts it and the list reloads.

1. Sign in as **Admin**.
2. Open **Developer → Workflows** and open a **custom** workflow (not Default
   Workflow, Simple Workflow, or Local Content, and not the system default).
3. Under **Assignment type**, the table lists each assigned role and its stored
   type (Reader, Assignee, Admin, or None).
4. Choose a step and a role whose current type is Reader or Assignee. Choose
   the other type and click **Set assignment type**. That row shows the new
   type only after the server accepts it. The step name and every other role
   stay as they were. Notify, inbox, and ad-hoc flags are not changed by this
   call. Notify is a separate confirm.
5. Click **Cancel** to restore the previous choice. Cancel does not call the
   server.
6. Packaged workflows and the system default do not show **Set assignment
   type** and do not call the server.
7. An unchanged type, a blank role, or a type other than Reader or Assignee is
   `400`. A role whose current type is Admin or None is `409` and is not
   changed. A missing workflow, step, or role is `404`. Non-Admin callers and
   packaged workflows receive `403`. None of those responses claim the new
   type.

The public calls are `GET /services/workflows/{idOrName}/role-assignments`
and `PUT /services/workflows/{idOrName}/steps/{stepName}/role-assignment`
(`WorkflowStepRoleAssignmentWrite`: `roleName` and `assignmentType`). This is
not `PUT .../steps/{stepName}`, which replaces the step's role list, and it
does not add a role that is not already on the step.

## Product path — set notify on a step role (slice 65)

On one step of a **custom** workflow, turn notify on or off for one role that
is already assigned to that step. The table shows the stored notify flag only
after the server accepts the write and the list reloads. The assignment type
and the inbox flag are not changed.

1. Sign in as **Admin**.
2. Open **Developer → Workflows** and open a **custom** workflow (not Default
   Workflow, Simple Workflow, or Local Content, and not the system default).
3. Under **Assignment type**, the table lists each assigned role, its stored
   type, and **Notify** (**On** or **Off**).
4. Under **Notify**, choose that step and role. Choose the other flag and
   click **Set notify**. That row's notify flag changes only after the server
   accepts it. The assignment type, the step name, and every other role stay
   as they were. Inbox is not edited.
5. Click **Cancel** to restore the previous choice. Cancel does not call the
   server, and the table flag does not change.
6. Packaged workflows and the system default do not show **Set notify** and
   do not call the server.
7. An unchanged flag, a blank role, or a missing notify value is `400`. A
   missing workflow, step, or role is `404`. Non-Admin callers and packaged
   workflows receive `403`. None of those responses claim the new flag.

The public calls are `GET /services/workflows/{idOrName}/role-assignments`
(each row includes `notify`) and
`PUT /services/workflows/{idOrName}/steps/{stepName}/role-notify`
(`WorkflowStepRoleNotifyWrite`: `roleName` and `notify`). This is not
`PUT .../steps/{stepName}/role-assignment`, which only changes Reader or
Assignee.

## Product path — add one role to a step (slice 63)

On a **custom** workflow, add one role that already exists on the workflow and
is not already assigned to the chosen step. The step role table shows that
role only after the server accepts the write and the list reloads.

1. Sign in as **Admin**.
2. Open **Developer → Workflows** and open a **custom** workflow (not Default
   Workflow, Simple Workflow, or Local Content, and not the system default).
3. Under **Assignment type**, the table lists each role already on a step.
4. Under **Add role**, choose a step and a role that is not already on that
   step. Choose **Reader** or **Assignee** and click **Add role**. That row
   appears only after the server accepts it. Other roles, the step name, and
   notify and inbox flags are not edited. Notify and inbox on the new row stay
   at the stored defaults.
5. Click **Cancel** to restore the previous choice. Cancel does not call the
   server, and the table does not show the role.
6. Packaged workflows and the system default do not show **Add role** and do
   not call the server.
7. A blank role, or a type other than Reader or Assignee, is `400`. A role
   already on the step is `409` and is not added again. A missing workflow,
   step, or role is `404`. Non-Admin callers and packaged workflows receive
   `403`. None of those responses claim the role was added.

The public call is
`POST /services/workflows/{idOrName}/steps/{stepName}/roles`
(`WorkflowStepRoleAdd`: `roleName` and `assignmentType`). This is not
`PUT .../steps/{stepName}/role-assignment`, which only changes Reader or
Assignee on a role that is already on the step.

## Product path — remove one role from a step (slice 64)

On a **custom** workflow, remove one Reader or Assignee role from one step.
The step role table drops that role only after the server accepts the delete
and the list reloads. The same role on another step stays.

1. Sign in as **Admin**.
2. Open **Developer → Workflows** and open a **custom** workflow (not Default
   Workflow, Simple Workflow, or Local Content, and not the system default).
3. Under **Assignment type**, the table lists each role already on a step.
4. Under **Remove role**, choose a step and a Reader or Assignee role on that
   step. Click **Remove role**.
5. **Cancel** closes the dialog and does not call the server. The role stays
   listed.
6. Confirm in the in-app dialog. That row disappears only after the server
   accepts the delete. Reopening the workflow shows the same row gone. Other
   roles on the step, and the same role on another step, stay. Notify and
   inbox flags are not edited. The step name does not change.
7. Packaged workflows and the system default do not show **Remove role** and
   do not call the server.
8. A blank step or role is `400`. An Admin or None assignment is `409` and is
   not removed. A missing workflow, step, or role is `404`. Non-Admin callers
   and packaged workflows receive `403`. None of those responses claim the
   role was removed.

The public call is
`DELETE /services/workflows/{idOrName}/steps/{stepName}/roles/{roleName}`.
This is not `PUT .../steps/{stepName}/role-assignment`, which only changes
Reader or Assignee, and it is not `POST .../steps/{stepName}/roles`, which
adds a role.

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
  Adding one transition between existing steps is `POST
  .../workflows/{idOrName}/transitions`. Updating that transition's label or
  destination is `PUT .../workflows/{idOrName}/transitions`. Deleting one
  existing transition on a custom workflow is `DELETE
  .../workflows/{idOrName}/transitions`. Deleting one step that no transition
  still uses is `DELETE .../workflows/{idOrName}/steps/{stepName}` (`409` when
  a transition still references the step).
- Rename one **custom** workflow from detail
  (`POST .../workflows/{idOrName}/rename`). Packaged **Default Workflow**,
  **Simple Workflow**, **Local Content**, and the current system default
  cannot be renamed (`403`). A duplicate name is `409`. The description-only
  `PUT` still rejects a body name that does not match the path (`400`).
- Add one existing workflow role onto one step of a custom workflow
  (`POST .../workflows/{idOrName}/steps/{stepName}/roles`). The role must
  already exist on the workflow and must not already be on that step.
  Assignment type is Reader or Assignee. Notify and inbox stay at their
  defaults and are not edited. The table shows the role only after the list
  reloads. Packaged workflows and the system default are `403`. A role already
  on the step is `409`. A blank role or any other type is `400`. A missing
  workflow, step, or role is `404`.
- Remove one Reader or Assignee role from one step of a custom workflow
  (`DELETE .../workflows/{idOrName}/steps/{stepName}/roles/{roleName}`). The
  table drops that role only after the list reloads. The same role on another
  step stays. Packaged workflows and the system default are `403`. An Admin
  or None assignment is `409` and is not removed. A blank step or role is
  `400`. A missing workflow, step, or role is `404`.
- Set Reader or Assignee on one role already assigned to one step of a custom
  workflow (`PUT .../workflows/{idOrName}/steps/{stepName}/role-assignment`).
  The table updates only after the list reloads. Packaged workflows and the
  system default are `403`. Admin or None roles are `409` and are not changed.
  That call does not change notify or inbox. Inbox flags, and repeated or
  system-field aging, stay outside this chrome.
- Turn notify on or off for one role already assigned to one step of a custom
  workflow (`PUT .../workflows/{idOrName}/steps/{stepName}/role-notify`). The
  table shows the stored flag only after the list reloads. Packaged workflows
  and the system default are `403`. An unchanged flag or a blank role is
  `400`. A missing workflow, step, or role is `404`. Assignment type and inbox
  are not edited.
- Changing the minute interval on one existing absolute
  aging transition is
  `PUT .../workflows/{idOrName}/aging-transitions/interval`. Deleting that
  absolute aging transition is
  `DELETE .../workflows/{idOrName}/aging-transitions?from={step}&to={step}&intervalMinutes={minutes}`.
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
| Update description | `PUT /services/workflows/{idOrName}` (`WorkflowUpdate` wrap; Admin; name must match the path or the call is `400`; missing workflow `404`. This call does not rename) |
| Rename workflow | `POST /services/workflows/{idOrName}/rename` (`WorkflowRename` wrap: required new `name`; Admin; custom workflows only; description, steps, transitions, and roles unchanged; packaged or system default `403`; duplicate `409`; invalid name `400`; missing workflow `404`) |
| Delete workflow | `DELETE /services/workflows/{idOrName}` (Admin; system workflows and item owners return `409`) |
| Create step | `POST /services/workflows/{idOrName}/steps` (`WorkflowStepWrite` wrap; Admin; packaged workflows `403`) |
| Update step | `PUT /services/workflows/{idOrName}/steps/{stepName}` (`WorkflowStepWrite` wrap; Admin; packaged workflows `403`) |
| List assignment types | `GET /services/workflows/{idOrName}/role-assignments` (Admin; every assigned role, including Reader, plus stored `notify`; missing workflow `404`) |
| Set assignment type | `PUT /services/workflows/{idOrName}/steps/{stepName}/role-assignment` (`WorkflowStepRoleAssignmentWrite` wrap: `roleName` and `assignmentType` of `READER` or `ASSIGNEE`; Admin; one role already on that step; does not rename the step or replace the role list; packaged or system default `403`; Admin or None current type `409`; unchanged or invalid type `400`; missing workflow, step, or role `404`) |
| Set notify | `PUT /services/workflows/{idOrName}/steps/{stepName}/role-notify` (`WorkflowStepRoleNotifyWrite` wrap: `roleName` and `notify`; Admin; one role already on that step; updates `ISNOTIFYON` only; does not change assignment type or inbox; packaged or system default `403`; unchanged flag, blank role, or missing notify `400`; missing workflow, step, or role `404`) |
| Add one role | `POST /services/workflows/{idOrName}/steps/{stepName}/roles` (`WorkflowStepRoleAdd` wrap: `roleName` and `assignmentType` of `READER` or `ASSIGNEE`; Admin; role must already exist on the workflow and must not already be on that step; notify and inbox stay at entity defaults; does not rename the step; packaged or system default `403`; role already on the step `409`; blank role or other type `400`; missing workflow, step, or role `404`) |
| Remove one role | `DELETE /services/workflows/{idOrName}/steps/{stepName}/roles/{roleName}` (Admin; one Reader or Assignee already on that step; other steps keep the role; does not rename the step or edit notify and inbox; packaged or system default `403`; Admin or None `409`; blank step or role `400`; missing workflow, step, or role `404`) |
| Read graph | `GET /services/workflows/{idOrName}/graph` (Admin; states and transitions; `packaged` true for stock or default workflows) |
| Create one transition | `POST /services/workflows/{idOrName}/transitions` (`WorkflowTransitionWrite` wrap: required `from`, `to`, `label`; both steps must exist; duplicate edge `409`; packaged workflows `403`; missing step `404`; invalid name `400`) |
| Update one transition | `PUT /services/workflows/{idOrName}/transitions?from={step}&label={label}&to={step}` (`WorkflowTransitionWrite` wrap: new `label` and `to`; does not move the source step; ambiguous label `400`; missing transition `404`; colliding edge `409`; packaged workflows `403`) |
| Delete one transition | `DELETE /services/workflows/{idOrName}/transitions?from={step}&label={label}&to={step}` (Admin; does not delete steps; packaged workflows `403`; missing workflow/step/transition `404`; blank or ambiguous label `400`) |
| Create one absolute aging transition | `POST /services/workflows/{idOrName}/aging-transitions` (`WorkflowAgingTransitionWrite` wrap: required `from`, `to`, and a positive `intervalMinutes`; duplicate absolute edge `409`; packaged workflows `403`) |
| Change one absolute aging interval | `PUT /services/workflows/{idOrName}/aging-transitions/interval` (`WorkflowAgingIntervalWrite` wrap: `from`, `to`, current `intervalMinutes`, and a different positive `newIntervalMinutes`; does not move the destination or change the aging type; duplicate new interval `409`; non-positive or unchanged interval `400`; missing edge `404`; packaged workflows `403`) |
| Delete one absolute aging transition | `DELETE /services/workflows/{idOrName}/aging-transitions?from={step}&to={step}&intervalMinutes={minutes}` (Admin; does not delete steps or regular transitions; repeated or system-field match `409` and is not deleted; non-positive interval `400`; missing edge `404`; packaged workflows `403`) |
| Comment required | `PUT /services/workflows/{idOrName}/transitions/comment-required?from={step}&label={label}&to={step}` (`WorkflowTransitionComment` wrap `{ "commentRequired": true }`; Admin; existing transition only; packaged workflows `403`; aging transitions `400`; missing transition `404`) |
| Delete one step | `DELETE /services/workflows/{idOrName}/steps/{stepName}` (Admin; only when no transition still uses the step; returns the updated graph; packaged workflows `403`; missing workflow or step `404`; invalid step name `400`; step still referenced `409`) |
| List allowed content types | `GET /services/workflows/{idOrName}/allowedContentTypes` |
| Replace allowed content types | `PUT /services/workflows/{idOrName}/allowedContentTypes` (`WorkflowContentTypes` wrap) |

Integrator notes: [REST API — Workflows](id:developer-rest).
