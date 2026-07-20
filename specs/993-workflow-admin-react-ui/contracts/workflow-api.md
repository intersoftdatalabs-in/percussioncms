# API Contract: Workflow Admin Shell

**Feature**: 993 — Unified Workflow & Admin React UI
**Consumed by**: `WorkflowAdminShell`, `WorkflowSection`, `RolesSection`, `UsersSection`, `CategoriesSection`
**Base path**: `/Rhythmyx/services/`

All endpoints require an authenticated CMS session cookie. CSRF tokens are auto-injected by `api/client.ts`. Responses use JSON; `Content-Type: application/json`.

---

## Workflow Management

### List All Workflows
`GET /Rhythmyx/services/workflowmanagement/workflows/metadata`

**Response** (200):
```json
[
  { "name": "Default Workflow", "isDefault": true, "stagingRoleId": null },
  { "name": "Blog Workflow",    "isDefault": false, "stagingRoleId": "Publishers" }
]
```

### Get Workflow Detail (with steps)
`GET /Rhythmyx/services/workflowmanagement/workflows/{workflowName}`

**Response** (200):
```json
{
  "name": "Default Workflow",
  "isDefault": true,
  "steps": [
    { "name": "Draft",    "roleNames": ["Editor", "Publisher"], "position": 1 },
    { "name": "Review",   "roleNames": ["Publisher"],            "position": 2 },
    { "name": "Approved", "roleNames": ["Publisher"],            "position": 3 }
  ]
}
```

### Create Workflow
`POST /Rhythmyx/services/workflowmanagement/workflows/{workflowName}`

**Body**: `WorkflowDefinition` (without steps; add steps separately)

**Response** (201): Created workflow detail

### Update Workflow
`PUT /Rhythmyx/services/workflowmanagement/workflows/{workflowName}`

**Body**: Updated `WorkflowDefinition`

**Response** (200): Updated workflow detail

### Delete Workflow
`DELETE /Rhythmyx/services/workflowmanagement/workflows/{workflowName}`

**Response** (204): No content

**Error** (409): `{ "error": "Cannot delete the default workflow" }`

---

## Workflow Step Management

### Create Step
`POST /Rhythmyx/services/workflowmanagement/workflows/{workflowName}/steps/{stepName}`

**Body**: `{ "roleNames": ["Editor"], "position": 2 }`

**Response** (201): Updated full workflow with new step

### Update Step
`PUT /Rhythmyx/services/workflowmanagement/workflows/{workflowName}/steps/{stepName}`

**Body**: `{ "roleNames": ["Editor", "Publisher"], "position": 2 }`

**Response** (200): Updated full workflow

### Delete Step
`DELETE /Rhythmyx/services/workflowmanagement/workflows/{workflowName}/steps/{stepName}`

**Response** (200): Updated full workflow without the step

---

## Folder/Site Workflow Assignment

### Start Assignment Job
`GET /Rhythmyx/services/foldermanagement/GetAssociatedFoldersJob/start/{workflowName}/{encodedPath}`

**Response** (200): `{ "jobId": "wfjob-123", "status": "RUNNING" }`

### Poll Assignment Progress
`GET /Rhythmyx/services/foldermanagement/workflowassignment/isInProgress`

**Response** (200):
```json
{ "isInProgress": true, "progressPercent": 45, "jobId": "wfjob-123" }
```
or
```json
{ "isInProgress": false }
```

---

## Role Management

### List / Find Roles
`POST /Rhythmyx/services/rolemanagement/role/find`

**Body**: `{ "name": "" }` (empty = all roles)

**Response** (200):
```json
[
  { "name": "Editor",    "description": "Content editors", "homePage": "/", "assignedUsers": ["alice"] },
  { "name": "Publisher", "description": "Approvers",       "homePage": "/", "assignedUsers": ["bob"] }
]
```

### Create Role
`POST /Rhythmyx/services/rolemanagement/role/create`

**Body**: `{ "name": "Reviewer", "description": "...", "homePage": "/" }`

**Response** (201): Created role

### Update Role (including user membership)
`POST /Rhythmyx/services/rolemanagement/role/update`

**Body**: Full role object with updated `assignedUsers`

**Response** (200): Updated role

### Delete Role
`POST /Rhythmyx/services/rolemanagement/role/delete`

**Body**: `{ "name": "Reviewer" }`

**Response** (200): `{ "success": true }`

**Error** (409): `{ "error": "Role is assigned to workflow steps: [Draft, Review]" }`

### Get Available Users for Role
`POST /Rhythmyx/services/rolemanagement/role/availableUsers`

**Body**: `{ "name": "Reviewer" }`

**Response** (200): `["alice", "charlie", "david"]`

---

## User Management

### List Users
`GET /Rhythmyx/services/user/user/users`

**Response** (200):
```json
[
  { "userName": "alice", "email": "alice@example.com", "assignedRoles": ["Editor"], "isExternalUser": false, "status": "ACTIVE" }
]
```

### Create User
`POST /Rhythmyx/services/user/user/create`

**Body**: `UserCreateRequest` (userName, password, confirmPassword, email, assignedRoles)

**Response** (201): Created user

### Update User
`POST /Rhythmyx/services/user/user/update`

**Body**: User object (omit password fields to keep existing password)

**Response** (200): Updated user

### Delete User
`POST /Rhythmyx/services/user/user/delete`

**Body**: `{ "userName": "alice" }`

**Response** (200): `{ "success": true }`

### Change Password
`POST /Rhythmyx/services/user/user/changepw`

**Body**: `{ "userName": "alice", "password": "newpass", "confirmPassword": "newpass" }`

**Response** (200): `{ "success": true }`

---

## LDAP / Directory

### Search Directory
`GET /Rhythmyx/services/user/user/external/find?nameFilter={pattern}`

**Response** (200):
```json
[
  { "userName": "ext_user1", "email": "ext@example.com", "isAlreadyImported": false }
]
```

### Import Directory Users
`POST /Rhythmyx/services/user/user/import`

**Body**: `{ "userNames": ["ext_user1", "ext_user2"] }`

**Response** (200): `{ "imported": 2, "errors": [] }`

### Directory Status
`GET /Rhythmyx/services/user/user/external/status`

**Response** (200):
```json
{ "isConfigured": true, "isReachable": true, "providerName": "LDAP" }
```

---

## Error Response Format

All API errors follow:
```json
{
  "error": "Human-readable error message",
  "code": "OPTIONAL_ERROR_CODE"
}
```

HTTP status codes used: `200` (success), `201` (created), `204` (deleted), `400` (validation error), `401` (not authenticated), `403` (not authorized), `404` (not found), `409` (conflict/dependency), `500` (server error).
