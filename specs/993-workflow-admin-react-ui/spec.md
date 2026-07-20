# Feature Specification: Unified Workflow & Admin React UI

**Feature Branch**: `993-workflow-admin-react-ui`
**Created**: 2026-07-20
**Status**: Draft
**Input**: "We need a new spec for migrating the legacy Workflow and Admin screens and merging them into the WebUI Admin / Workflow screen in a new modernized react UI similar to content explorer and publishing react work. For 8.2 we want to ship only the React UI no dual mode or backward compatible mode in the UI for these screens. Please inventory and analyze both user interfaces, on delivery we want functional parity with both versions of the UI in the new UX. New layouts and UI structure are encouraged."

## Module Scope

- **Primary module(s)**: `WebUI/` (React TypeScript UI), `projects/sitemanage/` (middleware REST)
- **Secondary / integration modules**: `rest/` (public REST API), `modules/perc-i18n/` (TMX i18n), `system/` (workflow engine)
- **AGENTS files to apply**: root `AGENTS.md`, `WebUI/AGENTS.md` (if present), `projects/sitemanage/AGENTS.md` (if present)
- **User roles affected**: Admin, Publisher (workflow management), Editor (in-context transitions), Site Visitor (no impact)
- **Install / upgrade impact**: none (pure front-end replacement; existing REST APIs unchanged; legacy JSP files removed from distribution)

---

## Background & Inventory

### Legacy UI Screens Being Retired

The following JSP/jQuery screens are in scope for replacement. They will be fully removed from the 8.2 distribution — no dual-mode or backward-compatible fallback will ship.

#### Legacy Workflow Admin (`adminWorkflow.jsp` shell — four tabs)

| Tab | Key Features |
|-----|-------------|
| **Workflow** | Create, read, list, update, delete named stepped workflow definitions; mark a workflow as system default; configure staging-publish role permissions per workflow; assign workflows to sites and folder subtrees (async background job with progress tracking) |
| **Workflow Steps** | Add, name, configure (role assignments), reorder, and delete individual steps within a workflow definition |
| **Roles** | Create, read, list, update, delete roles; set role homepage; assign/remove users to/from a role |
| **Users** | Create, read, list, update, delete local users; set username, password, email, assigned roles; import users from an external LDAP/directory; manage LDAP integration status |

#### Legacy Admin UI (scheduler, tools, categories — currently in `admin.jsp` + `ui/admin/`)

| Area | Key Features |
|------|-------------|
| **Categories** | Hierarchical category tree CRUD with lock management; add, delete, reorder categories |
| **Scheduled Tasks** | Create, read, list, update, delete scheduled jobs; view task logs per job; view detailed log entries; configure and list email notifications per task; delete all logs; remove task confirmation |
| **Consistency Checker** | Run, view results, and review content consistency checks |
| **Admin Console** | Server admin console view |

#### In-Context Workflow (per content item — currently `workflowactions.jsp`)

| Feature | Detail |
|---------|--------|
| Check In / Check Out / Force Check Out | Lock management per item |
| Workflow Transitions | Execute named transitions (Submit, Approve, Reject, Live, etc.) |
| Transition Comments | Optional or required comment textarea per transition |
| Ad-hoc Assignees | Search users and assign them as ad-hoc reviewers for a transition |

### Modern React Components Already Shipped (reference implementations)

- `ContentExplorerShell.tsx` — pattern for shell + panel layout, tree navigation, detail list
- `PublishingShell.tsx` — pattern for tabbed workspace with React routing
- `Dashboard.tsx` — pattern for widget-based home page with data polling

### Existing REST APIs (fully in place — no backend changes required for parity)

All functional capabilities above are already backed by REST endpoints:
- Workflow management: `GET/POST/PUT/DELETE /Rhythmyx/services/workflowmanagement/workflows/`
- Item workflow: `GET /Rhythmyx/services/itemmanagement/workflow/`
- Folder workflow assignment: `GET /Rhythmyx/services/foldermanagement/`
- User management: `GET/POST /Rhythmyx/services/user/user/`, `/rest/users/`
- Role management: `POST /Rhythmyx/services/rolemanagement/role/`, `/rest/roles/`

---

## User Scenarios & Testing

Each story must be independently testable.

### User Story 1 — Manage Workflow Definitions (Priority: P1)

As an Administrator, I need to create, configure, and organize stepped workflows so that content items follow controlled review and publishing paths.

**Acceptance Scenarios**:

1. **Given** I am on the Workflow admin screen, **When** I click "Create Workflow", **Then** a workflow creation form appears with fields for name, default flag, and staging role configuration.
2. **Given** a workflow definition exists, **When** I open it, **Then** I can see all steps listed in order with their assigned roles.
3. **Given** I am viewing a workflow, **When** I add a new step with a name and role selection and save, **Then** the step appears in the list and persists on page refresh.
4. **Given** I am viewing a workflow, **When** I drag and drop a step to reorder it, **Then** the new order is saved.
5. **Given** I delete a workflow that is not the default, **When** confirmed, **Then** the workflow is removed from the list.
6. **Given** I try to delete the system default workflow, **Then** the system prevents deletion with an informative error.
7. **Given** I mark a workflow as the default, **When** saved, **Then** only this workflow shows the default indicator and the previous default no longer does.

### User Story 2 — Assign Workflows to Sites and Folders (Priority: P1)

As an Administrator, I need to assign workflow definitions to entire sites or specific folder trees so that content in those locations follows the correct workflow.

**Acceptance Scenarios**:

1. **Given** I am managing a workflow, **When** I click "Assign to Site/Folder", **Then** I can browse the site tree and select a target site or folder subtree.
2. **Given** I initiate a folder workflow assignment, **When** the background job starts, **Then** I see a progress indicator and can remain on the page doing other work.
3. **Given** the assignment job completes, **Then** I receive an in-app notification with success or failure status.

### User Story 3 — Manage Roles (Priority: P1)

As an Administrator, I need to create, edit, and delete roles and control which users belong to each role.

**Acceptance Scenarios**:

1. **Given** I am on the Roles screen, **When** I create a new role with a name, description, and homepage assignment, **Then** the role appears in the list.
2. **Given** I am editing a role, **When** I move a user from "Available Users" to "Assigned Users" and save, **Then** the user is associated with the role.
3. **Given** a role is assigned to workflow steps, **When** I attempt to delete it, **Then** the system warns me about its dependencies before proceeding.

### User Story 4 — Manage Users (Priority: P1)

As an Administrator, I need to create, edit, and delete local user accounts and assign them to roles, as well as import users from a connected directory service.

**Acceptance Scenarios**:

1. **Given** I create a new user with username, password, email, and at least one assigned role, **Then** the user appears in the user list.
2. **Given** a user exists, **When** I edit their email or roles and save, **Then** the changes persist.
3. **Given** an LDAP/directory is configured, **When** I open the import dialog and search for a user, **Then** matching directory users are shown and I can select and import them.
4. **Given** I attempt to delete a user who is checked out with locked content, **Then** the system warns me before proceeding.
5. **Given** I change my own password, **When** I submit the new password twice correctly, **Then** the change is accepted and I remain logged in.

### User Story 5 — Perform In-Context Item Workflow Transitions (Priority: P1)

As an Editor or Publisher, when working with a content item I need to check it in/out and execute workflow transitions without leaving the editing context.

**Acceptance Scenarios**:

1. **Given** I am viewing a content item, **When** I open the workflow panel, **Then** I see the current state and all available transitions for my role.
2. **Given** I check out an item, **Then** the item is locked to me and the check-in action becomes available.
3. **Given** a transition requires a comment, **When** I execute that transition without entering a comment, **Then** submission is blocked and I see a required-field error.
4. **Given** a transition supports ad-hoc assignees, **When** I search for a user by name, **Then** matching users appear and I can select one or more as assignees.
5. **Given** another user has an item checked out, **When** I force check-out (with appropriate permissions), **Then** their lock is released and I gain the lock.

### User Story 6 — Manage Categories (Priority: P2)

As an Administrator, I need to manage the hierarchical content category tree so that content can be organized and tagged appropriately.

**Acceptance Scenarios**:

1. **Given** I am on the Categories screen, **When** I add a new child category under an existing parent, **Then** the new category appears in the correct position.
2. **Given** categories are locked by the system, **When** I view the category tree, **Then** locked categories show a locked indicator and cannot be renamed or deleted.
3. **Given** I reorder categories under a parent, **Then** the order is saved and persists on refresh.

### User Story 7 — Manage Scheduled Tasks (Priority: P2)

As an Administrator, I need to create, schedule, and monitor automated background tasks and review their execution logs.

**Acceptance Scenarios**:

1. **Given** I create a scheduled task with a name, schedule expression, and handler, **Then** it appears in the task list and will run at its configured interval.
2. **Given** a task has run, **When** I view its logs, **Then** I see a list of execution entries with timestamps and status.
3. **Given** I configure an email notification for a task, **When** the task runs, **Then** the notification record is associated with the task.
4. **Given** I delete all logs for a task, **When** confirmed, **Then** all log entries for that task are removed.

### User Story 8 — Run System Consistency Check (Priority: P3)

As an Administrator, I need to run a consistency check on the CMS content store and review and act on any reported inconsistencies.

**Acceptance Scenarios**:

1. **Given** I trigger a consistency check, **When** it completes, **Then** I see a structured report of any detected issues.
2. **Given** the report shows issues, **When** I select a fixable inconsistency and apply the fix, **Then** the issue is resolved and no longer appears in the report.

### Edge Cases

- What happens when a workflow assignment job is still in progress and the user navigates away? (Job continues; in-app notification on completion)
- What happens when the LDAP directory service is unreachable? (Import dialog shows connectivity error; local user management remains fully functional)
- What happens when two admins edit the same role simultaneously? (Last write wins with a stale-data warning to the second editor)
- What happens when a workflow has items currently in-flight and is deleted? (System prevents deletion; items must be transitioned to another workflow first)
- What happens when a user's session expires mid-transition? (Transition fails gracefully; user is redirected to login without data loss)

---

## Requirements

### Functional Requirements

- **FR-001**: The new React UI MUST provide complete functional parity with all features documented in the Legacy Workflow Admin and Legacy Admin UI sections above, covering Workflow CRUD, Steps CRUD, Roles CRUD, Users CRUD, Category management, Scheduled Tasks, and In-Context Item Workflow.
- **FR-002**: The 8.2 release distribution MUST ship exclusively the new React UI for these admin and workflow screens; all legacy JSP/jQuery/Dojo-based equivalents MUST be removed from the distribution with no dual-mode or backward-compatible UI fallback for end users.
- **FR-003**: The Workflow admin area MUST be unified as a single React shell (`WorkflowAdminShell`) hosting sections for Workflow Definitions, Roles, Users, and Categories, following the structural pattern of `ContentExplorerShell` and `PublishingShell`.
- **FR-004**: The Admin area MUST be unified as a single React shell (`AdminShell`) hosting sections for Scheduled Tasks and System Tools (Consistency Checker, Admin Console).
- **FR-005**: The system MUST allow administrators to create, read, list, update, and delete named stepped workflow definitions, including marking one workflow as the system default.
- **FR-006**: The system MUST allow administrators to add, configure (name, role assignment), reorder, and delete individual steps within a workflow definition.
- **FR-007**: The system MUST allow administrators to assign workflow definitions to sites and folder subtrees via an async background job with real-time progress feedback.
- **FR-008**: The system MUST allow administrators to create, read, list, update, and delete roles, including setting a role's homepage and managing its user membership.
- **FR-009**: The system MUST allow administrators to create, read, list, update, and delete local user accounts, including assigning them to roles and changing their passwords.
- **FR-010**: The system MUST allow administrators to search for and import users from a configured LDAP/directory service and display directory connectivity status.
- **FR-011**: The system MUST provide an in-context workflow action panel accessible from a content item that allows editors and publishers to check in, check out, force check out (with permission), and execute named workflow transitions.
- **FR-012**: Workflow transitions MUST support optional or required comments and ad-hoc assignee search and selection.
- **FR-013**: The system MUST allow administrators to manage the hierarchical content category tree, including adding, renaming, reordering, and deleting categories, with read-only indicators for locked system categories.
- **FR-014**: The system MUST allow administrators to create, schedule, update, delete, and monitor scheduled tasks, including viewing per-task execution logs and configuring email notifications.
- **FR-015**: The system MUST provide a consistency checker tool that runs an integrity check, displays a structured results report, and supports applying fixes to identified issues.
- **FR-016**: All user-visible strings in the new React UI MUST use the `perc-i18n` TMX-based localization pattern; no hard-coded English strings are permitted in production components.
- **FR-017**: All new React components MUST be accessible (WCAG 2.1 AA minimum), including full keyboard navigation, ARIA roles, focus management, and adequate color contrast.
- **FR-018**: The new UI MUST reuse the existing typed API client layer and REST endpoints; no new backend services or REST endpoints are required unless a gap is discovered during implementation.
- **FR-019**: The system MUST display informative error messages when background jobs fail, when the directory service is unreachable, or when a deletion is blocked by dependencies.
- **FR-020**: Each admin and workflow screen MUST be reachable via stable deep-linkable URLs within the WebUI routing scheme.
- **FR-021**: The new UI MUST include automated Playwright E2E browser tests for all primary user flows (US1–US8) and Vitest/React Testing Library unit tests for all component logic.

### Key Entities

- **Workflow**: A named, ordered collection of steps governing content lifecycle transitions. Has a system-default flag and staging-role configuration.
- **Workflow Step**: A named state within a workflow with one or more assigned roles that can execute transitions into/out of the step.
- **Role**: A named group with a homepage URL and a membership list of users. Roles gate workflow transitions and UI permissions.
- **User**: A local account with credentials, email, and role assignments. May also be sourced from a directory (LDAP).
- **Category**: A node in the global hierarchical category taxonomy. May be system-locked (read-only) or user-managed.
- **Scheduled Task**: A configured background job with a name, schedule expression, handler reference, and an execution log.
- **Workflow Transition**: A named action that moves a content item from one step to another; may require comments or ad-hoc assignees.

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: All features listed in FR-001 (full parity with legacy screens) are exercised by passing automated E2E Playwright tests with no manual-only verification for P1 user stories.
- **SC-002**: The 8.2 distribution contains no JSP files from the retired workflow/admin screens (`adminWorkflow.jsp`, `percWorkflow.jsp`, `percRoles.jsp`, `percUsers.jsp`, `percCategories.jsp`, `admin.jsp`, `ui/admin/*.jsp`, `workflowactions.jsp`, `adhocsearch.jsp`, `adhocresults.jsp`); verified by automated distribution inventory check.
- **SC-003**: All user-visible strings in the new components resolve to TMX keys with no hard-coded English strings remaining; verified by i18n lint in CI.
- **SC-004**: All new React components achieve WCAG 2.1 AA compliance verified by automated accessibility audit with zero critical violations.
- **SC-005**: The new UI fully replaces the legacy screens for administrators; zero regression issues are reported for the core workflows (Workflow CRUD, Role CRUD, User CRUD, Item Transitions) in UAT.
- **SC-006**: Automated unit tests (Vitest/React Testing Library) cover all component business logic; component test coverage is ≥ 80% for new components.
- **SC-007**: All workflow and admin screens are navigable via deep links in the WebUI routing scheme and render correctly when accessed directly by URL.

---

## Assumptions

- The existing REST API surface (workflow, user, role, folder, scheduling) is sufficient to implement full functional parity; no new backend endpoints are required unless gaps are found during implementation.
- The React TypeScript patterns established by `ContentExplorerShell` and `PublishingShell` are the canonical reference for shell structure, routing, and state management.
- Legacy JSP/jQuery/Dojo files will be removed as part of this feature's story delivery (not deferred to a separate cleanup task).
- The LDAP import feature will be conditionally rendered based on the directory service connectivity status returned by the existing API; no new admin configuration UI for LDAP settings is in scope.
- The `perc-qa-automation` Playwright module is available and is the designated E2E testing framework.
- The `perc-i18n` TMX keys that currently cover the legacy screens (`perc.ui.workflow.*`, `perc.ui.users.*`, `perc.ui.roles.*`, etc.) will be reused or extended as needed; no TMX key renames that break backward compatibility.
- "Consistency Checker" and "Admin Console" are lower-priority (P3) features that may ship as pass-through iframes or minimal wrappers if full React implementation is not feasible within the release timeline.
- The content item in-context workflow action panel (US5) replaces `workflowactions.jsp` and its Dojo dependency and integrates into the existing content editor/preview UI entry points.
