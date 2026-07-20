# Quickstart Validation Guide: 993 — Unified Workflow & Admin React UI

**Branch**: `993-workflow-admin-react-ui`
**Date**: 2026-07-20

This guide documents how to validate that the new Workflow Admin and Admin shells are working end-to-end. It covers prerequisites, build steps, and scenario-based validation for each user story.

---

## Prerequisites

- A running Percussion CMS 8.2 instance (development or staging)
- An administrator account (username/password)
- Node.js 20+ and npm 10+ installed
- `modules/perc-qa-automation/frontend/` dependencies installed (`npm install`)
- The `BASE_URL` environment variable set to the CMS root (e.g., `http://localhost:9992`)

---

## Build & Test

### Unit Tests (Vitest)
```bash
cd WebUI
npm test
```
Expected: All tests pass. Coverage ≥ 80% for new components.

### Build the Bundle
```bash
cd WebUI
npm run build:modern
```
Expected: `war/modern/assets/perc-modern-ui.js` produced without TypeScript or Vite errors.

### i18n Lint (verify no hard-coded strings)
```bash
# Verify no hard-coded English strings in new TSX files
grep -r '"[A-Z]' WebUI/src/main/ts/workflowAdmin/ WebUI/src/main/ts/admin/ WebUI/src/main/ts/workflowActions/ \
  --include="*.tsx" | grep -v "message(" | grep -v "//\|MSG\." | wc -l
# Expected: 0
```

### E2E Tests (Playwright)
```bash
cd modules/perc-qa-automation/frontend
BASE_URL=http://localhost:9992 npx playwright test tests/us1-workflow-definitions.spec.js
BASE_URL=http://localhost:9992 npx playwright test tests/us2-roles-management.spec.js
BASE_URL=http://localhost:9992 npx playwright test tests/us3-users-management.spec.js
BASE_URL=http://localhost:9992 npx playwright test tests/us4-item-transitions.spec.js
BASE_URL=http://localhost:9992 npx playwright test tests/us5-categories-admin.spec.js
BASE_URL=http://localhost:9992 npx playwright test tests/us6-scheduled-tasks.spec.js
```
Expected: All tests pass against the running CMS instance.

---

## Manual Validation Scenarios

### Scenario A: WorkflowAdminShell loads at correct URL

1. Log in to CMS as admin.
2. Navigate to `{BASE_URL}/cm/app/index.jsp?view=workflow`
3. **Expected**: The React `WorkflowAdminShell` renders with four navigation sections: Workflow, Roles, Users, Categories. No legacy jQuery tabs visible. No JavaScript console errors.

### Scenario B: Workflow CRUD (US1)

1. On the Workflow section, click "Create Workflow".
2. Enter a unique name (e.g., "Test QA Workflow"), leave default flag unchecked.
3. Save. **Expected**: New workflow appears in the list.
4. Open the workflow. Add a step named "Draft" with role "Editor". Save.
5. **Expected**: Step appears in the step list with correct role.
6. Drag "Draft" step to position 2 (if another step exists). **Expected**: Order updates visually and persists on refresh.
7. Delete the workflow. **Expected**: Workflow removed from list. Deletion of system default workflow should show an error.

### Scenario C: Workflow Site Assignment (US2)

1. Open a non-default workflow.
2. Click "Assign to Site/Folder". Select a site from the browser.
3. Click Assign. **Expected**: Progress indicator appears. On completion, an in-app notification confirms success.

### Scenario D: Roles CRUD (US3)

1. Navigate to the Roles section.
2. Create a new role with name "QA Reviewer", description, and homepage set to `/cm/app`.
3. **Expected**: Role appears in list.
4. Open the role. Move a user from Available to Assigned. Save.
5. **Expected**: User now listed as assigned.
6. Attempt to delete a role assigned to a workflow step. **Expected**: Error shows dependency warning.

### Scenario E: Users CRUD and LDAP Import (US4)

1. Navigate to the Users section.
2. Create a new user with username, password (confirmed), email, and at least one role.
3. **Expected**: User appears in list.
4. Edit the user — change email address. Save. **Expected**: Change persists.
5. If LDAP is configured: Click "Import from Directory". Search for an external user pattern. Select a result. Click Import.
6. **Expected**: Imported user appears in the user list with `isExternalUser = true` indicator.

### Scenario F: In-Context Item Transitions (US5)

1. Open any content item in the editor/preview context.
2. Open the Workflow Actions panel.
3. **Expected**: Current workflow step and available transitions shown.
4. Check out the item. **Expected**: Lock icon appears; "Check In" action becomes available.
5. Execute a transition that requires a comment. Leave comment blank. Click submit.
6. **Expected**: Submission blocked with "Comment required" validation error.
7. Enter a comment and submit. **Expected**: Item transitions to the next step.

### Scenario G: Legacy JSP Removal Verification (SC-002)

```bash
# Verify legacy JSP files are absent from the distribution WAR
jar -tf WebUI/target/*.war | grep -E "adminWorkflow|percWorkflow|percRoles|percUsers|percCategories|workflowactions|adhocsearch|adhocresults" | wc -l
# Expected: 0
```

### Scenario H: Deep-Link Navigation (SC-007)

1. Navigate directly to `{BASE_URL}/cm/app/index.jsp?view=workflow&section=roles`
2. **Expected**: WorkflowAdminShell opens with the Roles section active.
3. Navigate to `{BASE_URL}/cm/app/index.jsp?view=admin`
4. **Expected**: AdminShell loads with Scheduled Tasks section active.

### Scenario I: AdminShell — Scheduled Tasks (US7)

1. Navigate to `{BASE_URL}/cm/app/index.jsp?view=admin`
2. Create a scheduled task with a test name and cron expression.
3. **Expected**: Task appears in list and runs at next scheduled time.
4. View task logs after a run. **Expected**: Log entries with status and timestamp visible.

---

## References

- Data model: [data-model.md](./data-model.md)
- Workflow API contracts: [contracts/workflow-api.md](./contracts/workflow-api.md)
- Admin API contracts: [contracts/admin-api.md](./contracts/admin-api.md)
- Spec: [spec.md](./spec.md)
- Plan: [plan.md](./plan.md)
