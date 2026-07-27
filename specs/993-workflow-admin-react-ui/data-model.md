# Data Model: 993 — Unified Workflow & Admin React UI

**Branch**: `993-workflow-admin-react-ui`
**Date**: 2026-07-20

This document defines the client-side data model as used by the new React components. All types map directly to the existing REST API response shapes — no backend schema changes are required.

---

## Core Entities

### WorkflowDefinition

Represents a named stepped workflow governing content lifecycle transitions.

```ts
interface WorkflowDefinition {
  name: string;           // Unique workflow name (primary key)
  isDefault: boolean;     // True if this is the system default workflow
  stagingRoleId?: string; // Role allowed to publish-now to staging (optional)
  steps: WorkflowStep[];  // Ordered list of steps
}
```

**Validation rules**:
- `name` is required and must be unique across all workflows
- Exactly one workflow may have `isDefault = true`
- The system default workflow cannot be deleted

**State transitions**: See Workflow Lifecycle below.

---

### WorkflowStep

A named state within a workflow with assigned roles.

```ts
interface WorkflowStep {
  name: string;     // Step name (unique within workflow)
  roleNames: string[];  // Roles that can execute transitions from this step
  position: number;     // 1-based sort order
}
```

**Validation rules**:
- `name` is required and must be unique within its parent workflow
- At least one role must be assigned to a step before saving
- `position` is contiguous (1, 2, 3…); managed by the UI on drag-reorder

---

### WorkflowAssignmentJob

Represents an async background job assigning a workflow to a site or folder subtree.

```ts
interface WorkflowAssignmentJob {
  jobId: string;
  workflowName: string;
  targetPath: string;       // Site root or folder path being assigned
  status: "RUNNING" | "COMPLETE" | "FAILED";
  progressPercent?: number; // 0–100 when RUNNING
  errorMessage?: string;    // Populated when FAILED
}
```

**State transitions**: `RUNNING` → `COMPLETE` | `FAILED`

---

### Role

A named permission group with a homepage and user membership.

```ts
interface Role {
  name: string;         // Unique role name (primary key)
  description: string;
  homePage?: string;    // URL of the role's default landing page
  assignedUsers: string[];   // Usernames of members
  availableUsers?: string[]; // Populated during edit — users not yet in this role
}
```

**Validation rules**:
- `name` is required and must be unique
- A role assigned to any workflow step cannot be deleted without first removing it from those steps

---

### User

A local CMS user account.

```ts
interface User {
  userName: string;       // Unique username (primary key)
  email: string;
  firstName?: string;
  lastName?: string;
  assignedRoles: string[]; // Role names
  isExternalUser: boolean; // True if imported from LDAP/directory
  status: "ACTIVE" | "LOCKED";
}

interface UserCreateRequest {
  userName: string;
  password: string;
  confirmPassword: string;
  email: string;
  assignedRoles: string[];
}
```

**Validation rules**:
- `userName` is required and must be unique
- `password` and `confirmPassword` must match on create
- `email` must be a valid email address
- At least one role must be assigned

---

### DirectoryUser

A user record returned from an LDAP/external directory search.

```ts
interface DirectoryUser {
  userName: string;
  email: string;
  firstName?: string;
  lastName?: string;
  isAlreadyImported: boolean; // True if the user already exists as a local CMS user
}

interface DirectoryStatus {
  isConfigured: boolean;
  isReachable: boolean;
  providerName?: string;
}
```

---

### Category

A node in the global hierarchical content category taxonomy.

```ts
interface Category {
  id: string;               // Unique category ID
  name: string;
  parentId?: string;        // null for root categories
  children: Category[];     // Nested sub-categories (recursive)
  position: number;         // Sort order among siblings
  isLocked: boolean;        // True for system-managed categories (read-only)
  isExpanded?: boolean;     // UI state only — not persisted
}
```

**Validation rules**:
- `name` is required and must be unique among siblings
- Locked categories (`isLocked = true`) may not be renamed, moved, or deleted
- Deleting a parent category also deletes all descendants (cascading)

---

### ScheduledTask

A configured background job.

```ts
interface ScheduledTask {
  taskId: string;             // Unique task ID
  name: string;
  scheduleExpression: string; // Cron expression (e.g., "0 0 * * *")
  handlerClass: string;       // Fully qualified handler class reference
  parameters?: Record<string, string>; // Optional key/value config
  enabled: boolean;
  lastRunAt?: string;         // ISO-8601 timestamp
  lastRunStatus?: "SUCCESS" | "FAILURE" | "RUNNING";
}

interface TaskLog {
  logId: string;
  taskId: string;
  startedAt: string;         // ISO-8601
  completedAt?: string;
  status: "SUCCESS" | "FAILURE" | "RUNNING";
  message: string;
}

interface TaskNotification {
  notificationId: string;
  taskId: string;
  emailAddress: string;
  notifyOn: "SUCCESS" | "FAILURE" | "BOTH";
}
```

---

### WorkflowTransition

Represents an available transition action for a content item.

```ts
interface WorkflowTransition {
  trigger: string;       // Transition trigger name (e.g., "Submit", "Approve")
  label: string;         // Human-readable label
  requiresComment: boolean;
  supportsAdhocAssignees: boolean;
  toStepName: string;    // Target step name after transition
}

interface ItemWorkflowState {
  itemId: string;
  currentStep: string;
  isCheckedOut: boolean;
  checkedOutBy?: string;   // Username if checked out by another user
  availableTransitions: WorkflowTransition[];
}

interface TransitionRequest {
  itemId: string;
  trigger: string;
  comment?: string;
  adhocAssignees?: string[]; // Usernames for ad-hoc reviewers
}
```

---

## UI State Models

### WorkflowAdminShell State

```ts
type WorkflowAdminSection = "workflow" | "roles" | "users" | "categories";

interface WorkflowAdminShellState {
  activeSection: WorkflowAdminSection;
  isDirty: boolean;  // True if unsaved changes exist in active section
}
```

### WorkflowSection State

```ts
interface WorkflowSectionState {
  workflows: WorkflowDefinition[];
  selectedWorkflowName: string | null;
  editMode: "none" | "create" | "edit";
  assignmentJob: WorkflowAssignmentJob | null;
  isLoading: boolean;
  error: string | null;
}
```

### UsersSection State

```ts
interface UsersSectionState {
  users: User[];
  selectedUserName: string | null;
  editMode: "none" | "create" | "edit";
  directoryStatus: DirectoryStatus | null;
  ldapImportOpen: boolean;
  isLoading: boolean;
  error: string | null;
}
```

---

## Workflow Lifecycle Diagram

```
         ┌──────────────────────────────────────────┐
         │            WORKFLOW DEFINITION            │
         │                                          │
  (Admin creates) ──► [DRAFT]                       │
                         │                          │
                  (set as default) ──► [DEFAULT]    │
                         │                          │
                  (delete — not default) ──► [GONE] │
         └──────────────────────────────────────────┘

         ┌──────────────────────────────────────────┐
         │           CONTENT ITEM WORKFLOW           │
         │                                          │
  [CHECKED IN] ──checkOut──► [CHECKED OUT (me)]     │
  [CHECKED OUT (me)] ──checkIn──► [CHECKED IN]      │
  [CHECKED OUT (other)] ──forceCheckOut──► [CHECKED OUT (me)]
                                                    │
  [CHECKED IN] ──transition(trigger)──► [NEXT STEP] │
         └──────────────────────────────────────────┘
```

---

## REST API Mapping

|        Entity         | Operation |                                      REST Endpoint                                      |
|-----------------------|-----------|-----------------------------------------------------------------------------------------|
| WorkflowDefinition    | List      | `GET /Rhythmyx/services/workflowmanagement/workflows/metadata`                          |
| WorkflowDefinition    | Get       | `GET /Rhythmyx/services/workflowmanagement/workflows/{name}`                            |
| WorkflowDefinition    | Create    | `POST /Rhythmyx/services/workflowmanagement/workflows/{name}`                           |
| WorkflowDefinition    | Update    | `PUT /Rhythmyx/services/workflowmanagement/workflows/{name}`                            |
| WorkflowDefinition    | Delete    | `DELETE /Rhythmyx/services/workflowmanagement/workflows/{name}`                         |
| WorkflowStep          | Create    | `POST /Rhythmyx/services/workflowmanagement/workflows/{name}/steps/{stepName}`          |
| WorkflowStep          | Update    | `PUT /Rhythmyx/services/workflowmanagement/workflows/{name}/steps/{stepName}`           |
| WorkflowStep          | Delete    | `DELETE /Rhythmyx/services/workflowmanagement/workflows/{name}/steps/{stepName}`        |
| WorkflowAssignmentJob | Start     | `GET /Rhythmyx/services/foldermanagement/GetAssociatedFoldersJob/start/{wfName}/{path}` |
| WorkflowAssignmentJob | Poll      | `GET /Rhythmyx/services/foldermanagement/workflowassignment/isInProgress`               |
| Role                  | List      | `POST /Rhythmyx/services/rolemanagement/role/find`                                      |
| Role                  | Create    | `POST /Rhythmyx/services/rolemanagement/role/create`                                    |
| Role                  | Update    | `POST /Rhythmyx/services/rolemanagement/role/update`                                    |
| Role                  | Delete    | `POST /Rhythmyx/services/rolemanagement/role/delete`                                    |
| User                  | List      | `GET /Rhythmyx/services/user/user/users`                                                |
| User                  | Create    | `POST /Rhythmyx/services/user/user/create`                                              |
| User                  | Update    | `POST /Rhythmyx/services/user/user/update`                                              |
| User                  | Delete    | `POST /Rhythmyx/services/user/user/delete`                                              |
| DirectoryUser         | Search    | `GET /Rhythmyx/services/user/user/external/find`                                        |
| DirectoryUser         | Import    | `POST /Rhythmyx/services/user/user/import`                                              |
| DirectoryStatus       | Get       | `GET /Rhythmyx/services/user/user/external/status`                                      |
| ItemWorkflowState     | Get       | `GET /Rhythmyx/services/itemmanagement/workflow/getTransitions/{id}`                    |
| WorkflowTransition    | Execute   | `GET /Rhythmyx/services/itemmanagement/workflow/transitionWithComments/{id}/{trigger}`  |
| Item check in/out     | Ops       | `/Rhythmyx/services/itemmanagement/workflow/checkIn|checkOut|forceCheckOut/{id}`        |

