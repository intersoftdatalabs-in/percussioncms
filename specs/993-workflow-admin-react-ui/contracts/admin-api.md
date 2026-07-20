# API Contract: Admin Shell

**Feature**: 993 — Unified Workflow & Admin React UI
**Consumed by**: `AdminShell`, `TasksSection`, `TaskLogsSection`, `TaskNotifications`, `ConsistencyChecker`
**Base path**: `/Rhythmyx/services/` and `/Rhythmyx/ui/admin/`

> **Note**: The Scheduled Tasks and Consistency Checker admin features use endpoints under the legacy admin area. These endpoint paths should be confirmed during implementation using the existing `ScheduledTask.jsp` and `ConsistencyChecker.jsp` source files. The patterns below reflect the existing service conventions.

---

## Scheduled Tasks

### List Tasks
`GET /Rhythmyx/services/taskmanagement/tasks`

**Response** (200):
```json
[
  {
    "taskId": "task-001",
    "name": "Nightly Sitemap Generator",
    "scheduleExpression": "0 2 * * *",
    "handlerClass": "com.percussion.task.PSSitemapTask",
    "enabled": true,
    "lastRunAt": "2026-07-20T02:00:00Z",
    "lastRunStatus": "SUCCESS"
  }
]
```

### Get Task
`GET /Rhythmyx/services/taskmanagement/tasks/{taskId}`

**Response** (200): Single task object

### Create Task
`POST /Rhythmyx/services/taskmanagement/tasks`

**Body**: Task object (without `taskId`, `lastRunAt`, `lastRunStatus`)

**Response** (201): Created task with assigned `taskId`

### Update Task
`PUT /Rhythmyx/services/taskmanagement/tasks/{taskId}`

**Body**: Full task object

**Response** (200): Updated task

### Delete Task
`DELETE /Rhythmyx/services/taskmanagement/tasks/{taskId}`

**Response** (204): No content

---

## Task Logs

### List Logs for a Task
`GET /Rhythmyx/services/taskmanagement/tasks/{taskId}/logs`

**Response** (200):
```json
[
  {
    "logId": "log-001",
    "taskId": "task-001",
    "startedAt": "2026-07-20T02:00:00Z",
    "completedAt": "2026-07-20T02:01:23Z",
    "status": "SUCCESS",
    "message": "Generated 342 sitemap entries"
  }
]
```

### Get Log Detail
`GET /Rhythmyx/services/taskmanagement/tasks/{taskId}/logs/{logId}`

**Response** (200): Single log entry with full message body

### Delete All Logs for Task
`DELETE /Rhythmyx/services/taskmanagement/tasks/{taskId}/logs`

**Response** (204): No content

---

## Task Notifications

### List Notifications for Task
`GET /Rhythmyx/services/taskmanagement/tasks/{taskId}/notifications`

**Response** (200):
```json
[
  { "notificationId": "notif-001", "taskId": "task-001", "emailAddress": "admin@example.com", "notifyOn": "FAILURE" }
]
```

### Create Notification
`POST /Rhythmyx/services/taskmanagement/tasks/{taskId}/notifications`

**Body**: `{ "emailAddress": "admin@example.com", "notifyOn": "BOTH" }`

**Response** (201): Created notification

### Update Notification
`PUT /Rhythmyx/services/taskmanagement/tasks/{taskId}/notifications/{notificationId}`

**Body**: Full notification object

**Response** (200): Updated notification

### Delete Notification
`DELETE /Rhythmyx/services/taskmanagement/tasks/{taskId}/notifications/{notificationId}`

**Response** (204): No content

---

## Consistency Checker (P3)

### Start Check
`POST /Rhythmyx/ui/admin/tools/ConsistencyChecker`

**Response** (202): `{ "jobId": "check-001", "status": "RUNNING" }`

### Poll Check Status / Get Results
`GET /Rhythmyx/ui/admin/tools/ConsistencyChecker/{jobId}`

**Response** (200):
```json
{
  "jobId": "check-001",
  "status": "COMPLETE",
  "issues": [
    { "issueId": "i-001", "type": "ORPHAN_ITEM", "description": "Item 123 has no parent folder", "fixable": true }
  ]
}
```

### Apply Fix
`POST /Rhythmyx/ui/admin/tools/ConsistencyChecker/{jobId}/fix/{issueId}`

**Response** (200): `{ "success": true }`

---

## Error Response Format

Same as `workflow-api.md`:
```json
{
  "error": "Human-readable error message",
  "code": "OPTIONAL_ERROR_CODE"
}
```

> **Implementation Note**: Actual endpoint paths for Scheduled Tasks and Consistency Checker must be verified against `WebUI/src/main/webapp/ui/admin/` JSP source and `projects/sitemanage/` REST service classes during Phase 5/6 implementation. The paths above follow naming conventions and should be treated as provisional until confirmed.
