# Tasks: Unified Workflow & Admin React UI

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/workflow-api.md, contracts/admin-api.md, quickstart.md

## Phase 1: Setup
- [x] T001 Identify owning module path(s) and read AGENTS hierarchy (root `AGENTS.md` and `WebUI/AGENTS.md`)
- [x] T002 Confirm branch JDK (JDK 21) and verify baseline tests in `WebUI/` (`cd WebUI && npm test`) and `perc-qa-automation/`

## Phase 2: Foundational (Blocking Prerequisites)
- [x] T003 Map existing REST APIs and client stubs in `WebUI/src/main/ts/api/paths.ts` and `api/client.ts`
- [x] T004 Confirm CSRF token auto-injection and auth state wrappers in `WebUI/src/main/ts/api/client.ts`
- [x] T005 Add paths for workflows, roles, users, and categories to `WebUI/src/main/ts/api/paths.ts`

## Phase 3: User Story 1 — Manage Workflow Definitions (Priority: P1)
**Goal**: Implement `WorkflowAdminShell` and the Workflow section allowing CRUD management of workflow definitions.
**Independent Test**: Navigate to `/cm/app/index.jsp?view=workflow` and perform full CRUD on a workflow definition, verifying creation, step configuration, step reordering, default setting, and deletion rules.

### Tests
- [x] T006 [P] [US1] Unit test for `WorkflowAdminShell` container component in `WebUI/src/test/ts/workflowAdmin/WorkflowAdminShell.test.tsx`
- [x] T007 [P] [US1] Unit test for `WorkflowSection` and `WorkflowEditor` in `WebUI/src/test/ts/workflowAdmin/WorkflowSection.test.tsx`
- [x] T008 [P] [US1] Playwright E2E spec for workflow CRUD verification in `modules/perc-qa-automation/frontend/tests/us1-workflow-definitions.spec.js`

### Implementation
- [x] T009 [P] [US1] Register `WorkflowAdminShell` in `WebUI/src/main/ts/registry.ts`
- [x] T010 [P] [US1] Define i18n TMX key mappings for workflow actions in `WebUI/src/main/ts/workflowAdmin/messages.ts`
- [x] T011 [P] [US1] Create the core shell wrapper `WebUI/src/main/ts/workflowAdmin/WorkflowAdminShell.tsx` with tab sections
- [x] T012 [P] [US1] Create the workflow section list view `WebUI/src/main/ts/workflowAdmin/workflow/WorkflowSection.tsx`
- [x] T013 [P] [US1] Create the workflow detail editor `WebUI/src/main/ts/workflowAdmin/workflow/WorkflowEditor.tsx` with CRUD forms
- [x] T014 [P] [US1] Create the step list and drag-reorder component `WebUI/src/main/ts/workflowAdmin/workflow/WorkflowStepList.tsx`
- [x] T015 [P] [US1] Add host JSP `WebUI/src/main/webapp/cm/app/adminWorkflowModern.jsp` to load modern bundle and mount `WorkflowAdminShell`
- [x] T016 [US1] Update `WebUI/src/main/webapp/cm/app/index.jsp` view map to route `workflow` to `adminWorkflowModern.jsp`
- [x] T017 [US1] Commit changes and submit PR for review, pausing downstream tasks
- [x] T018 [US1] Monitor Kilo Code check, address feedback, and resolve comments (pause on check failure)
- [x] T019 [US1] Verify human approval and merge of PR before starting next story

## Phase 4: User Story 2 — Assign Workflows to Sites and Folders (Priority: P1)
**Goal**: Add site/folder workflow assignment dialog and monitor background job.
**Independent Test**: Within `WorkflowAdminShell` workflow section, click "Assign to Site/Folder", choose a folder, start the job, and confirm the async progress overlay correctly reports status and completes.

### Tests
- [x] T020 [P] [US2] Unit test for `WorkflowSiteAssign` dialog component in `WebUI/src/test/ts/workflowAdmin/WorkflowSiteAssign.test.tsx`

### Implementation
- [x] T021 [P] [US2] Create site/folder selection browser tree component `WebUI/src/main/ts/workflowAdmin/workflow/WorkflowSiteAssign.tsx`
- [x] T022 [P] [US2] Implement API calls for assignment job status and start endpoints in `WebUI/src/main/ts/api/client.ts`
- [x] T023 [US2] Wire folder assignment action and background progress overlay into `WebUI/src/main/ts/workflowAdmin/workflow/WorkflowEditor.tsx`
- [x] T024 [US2] Commit changes and submit PR for review, pausing downstream tasks
- [x] T025 [US2] Monitor Kilo Code check, address feedback, and resolve comments (pause on check failure)
- [x] T026 [US2] Verify human approval and merge of PR before proceeding

## Phase 5: User Story 3 — Manage Roles (Priority: P1)
**Goal**: Implement `RolesSection` inside the shell for role configuration and user assignment.
**Independent Test**: Navigate to Roles tab, create a new role, assign users via dual list, save, verify persistent relationship and dependency checks on delete.

### Tests
- [x] T021a [P] [US3] Unit test for `RolesSection` and `RoleEditor` components in `WebUI/src/test/ts/workflowAdmin/RolesSection.test.tsx`
- [x] T022a [P] [US3] Playwright E2E spec for role CRUD validation in `modules/perc-qa-automation/frontend/tests/us2-roles-management.spec.js`

### Implementation
- [x] T023a [P] [US3] Create roles list view in `WebUI/src/main/ts/workflowAdmin/roles/RolesSection.tsx`
- [x] T024a [P] [US3] Create role form and user membership dual list in `WebUI/src/main/ts/workflowAdmin/roles/RoleEditor.tsx`
- [x] T025a [US3] Wire roles tab component into main shell `WebUI/src/main/ts/workflowAdmin/WorkflowAdminShell.tsx`
- [x] T026a [US3] Commit changes and submit PR for review, pausing downstream tasks
- [x] T027 [US3] Monitor Kilo Code check, address feedback, and resolve comments (pause on check failure)
- [x] T028 [US3] Verify human approval and merge of PR before proceeding

## Phase 6: User Story 4 — Manage Users (Priority: P1)
**Goal**: Implement `UsersSection` supporting local user CRUD and LDAP integration/directory user imports.
**Independent Test**: Navigate to Users tab, create local user with password and email, import an LDAP user via search dialog, modify user parameters, save, and confirm updates.

### Tests
- [x] T029 [P] [US4] Unit test for `UsersSection`, `UserEditor`, and `LdapImportDialog` in `WebUI/src/test/ts/workflowAdmin/UsersSection.test.tsx`
- [x] T030 [P] [US4] Playwright E2E spec for user operations and LDAP search in `modules/perc-qa-automation/frontend/tests/us3-users-management.spec.js`

### Implementation
- [x] T031 [P] [US4] Create user list and table view in `WebUI/src/main/ts/workflowAdmin/users/UsersSection.tsx`
- [x] T032 [P] [US4] Create user form and role assignment checkboxes in `WebUI/src/main/ts/workflowAdmin/users/UserEditor.tsx`
- [x] T033 [P] [US4] Create LDAP directory search and select modal `WebUI/src/main/ts/workflowAdmin/users/LdapImportDialog.tsx`
- [x] T034 [US4] Wire users tab component into main shell `WebUI/src/main/ts/workflowAdmin/WorkflowAdminShell.tsx`
- [x] T035 [US4] Commit changes and submit PR for review, pausing downstream tasks
- [x] T036 [US4] Monitor Kilo Code check, address feedback, and resolve comments (pause on check failure)
- [x] T037 [US4] Verify human approval and merge of PR before proceeding

## Phase 7: User Story 5 — Perform In-Context Item Workflow Transitions (Priority: P1)
**Goal**: Rebuild the Dojo-based item toolbar workflow actions panel in React.
**Independent Test**: Open content item edit context, open workflow drawer, lock/unlock item, select a transition, fill comments if required, search and select ad-hoc reviewers, submit and confirm workflow state updates.

### Tests
- [x] T038 [P] [US5] Unit test for `WorkflowActionsPanel` in `WebUI/src/test/ts/workflowActions/WorkflowActionsPanel.test.tsx`
- [x] T039 [P] [US5] Unit test for `TransitionDialog` and search in `WebUI/src/test/ts/workflowActions/TransitionDialog.test.tsx`
- [x] T040 [P] [US5] Playwright E2E spec for in-context transitions in `modules/perc-qa-automation/frontend/tests/us4-item-transitions.spec.js`

### Implementation
- [x] T041 [P] [US5] Create workflow status actions drawer `WebUI/src/main/ts/workflowActions/WorkflowActionsPanel.tsx`
- [x] T042 [P] [US5] Create transition parameters overlay `WebUI/src/main/ts/workflowActions/TransitionDialog.tsx`
- [x] T043 [P] [US5] Create ad-hoc user search field `WebUI/src/main/ts/workflowActions/AdhocSearch.tsx`
- [x] T044 [US5] Wire new React workflow actions panel into core edit/preview frame, replacing legacy JSP and Dojo scripts
- [x] T045 [US5] Commit changes and submit PR for review, pausing downstream tasks
- [x] T046 [US5] Monitor Kilo Code check, address feedback, and resolve comments (pause on check failure)
- [x] T047 [US5] Verify human approval and merge of PR before proceeding

## Phase 8: User Story 6 — Manage Categories (Priority: P2)
**Goal**: Implement `CategoriesSection` for taxonomy tree management.
**Independent Test**: Navigate to Categories tab, add node, reorder siblings, verify lock indicators on system categories.

### Tests
- [x] T048 [P] [US6] Unit test for tree structure and lock states in `WebUI/src/test/ts/workflowAdmin/CategoriesSection.test.tsx`
- [x] T049 [P] [US6] Playwright E2E spec for categories in `modules/perc-qa-automation/frontend/tests/us5-categories-admin.spec.js`

### Implementation
- [x] T050 [P] [US6] Create tree explorer layout `WebUI/src/main/ts/workflowAdmin/categories/CategoriesSection.tsx`
- [x] T051 [P] [US6] Create interactive category item row with lock validation `WebUI/src/main/ts/workflowAdmin/categories/CategoryNode.tsx`
- [x] T052 [US6] Wire categories tab component into main shell `WebUI/src/main/ts/workflowAdmin/WorkflowAdminShell.tsx`
- [x] T053 [US6] Commit changes and submit PR for review, pausing downstream tasks
- [x] T054 [US6] Monitor Kilo Code check, address feedback, and resolve comments (pause on check failure)
- [x] T055 [US6] Verify human approval and merge of PR before proceeding

## Phase 9: User Story 7 — Manage Scheduled Tasks (Priority: P2)
**Goal**: Implement `AdminShell` and `TasksSection` for scheduler configuration.
**Independent Test**: Open `/cm/app/index.jsp?view=admin`, create a cron task, configure alerts, view log entries, verify log deletion.

### Tests
- [ ] T056 [P] [US7] Unit test for scheduled task CRUD and form in `WebUI/src/test/ts/admin/TasksSection.test.tsx`
- [ ] T057 [P] [US7] Unit test for log list and notification configuration in `WebUI/src/test/ts/admin/TaskLogsSection.test.tsx`
- [ ] T058 [P] [US7] Playwright E2E spec for scheduler in `modules/perc-qa-automation/frontend/tests/us6-scheduled-tasks.spec.js`

### Implementation
- [ ] T059 [P] [US7] Register `AdminShell` in `WebUI/src/main/ts/registry.ts`
- [ ] T060 [P] [US7] Define i18n TMX key mappings for admin tool actions in `WebUI/src/main/ts/admin/messages.ts`
- [ ] T061 [P] [US7] Create the core admin shell `WebUI/src/main/ts/admin/AdminShell.tsx`
- [ ] T062 [P] [US7] Create scheduled tasks list and settings screen `WebUI/src/main/ts/admin/tasks/TasksSection.tsx`
- [ ] T063 [P] [US7] Create scheduled task editor form `WebUI/src/main/ts/admin/tasks/TaskEditor.tsx`
- [ ] T064 [P] [US7] Create task log records viewer `WebUI/src/main/ts/admin/tasks/TaskLogsSection.tsx`
- [ ] T065 [P] [US7] Create email alerts setting component `WebUI/src/main/ts/admin/tasks/TaskNotifications.tsx`
- [ ] T066 [P] [US7] Add host JSP `WebUI/src/main/webapp/cm/app/adminModern.jsp` to load modern bundle and mount `AdminShell`
- [ ] T067 [US7] Update `WebUI/src/main/webapp/cm/app/index.jsp` view map to route `admin` to `adminModern.jsp`
- [ ] T068 [US7] Commit changes and submit PR for review, pausing downstream tasks
- [ ] T069 [US7] Monitor Kilo Code check, address feedback, and resolve comments (pause on check failure)
- [ ] T070 [US7] Verify human approval and merge of PR before proceeding

## Phase 10: User Story 8 — Run System Consistency Check (Priority: P3)
**Goal**: Integrate Consistency Checker and system tools section in AdminShell.
**Independent Test**: Open system tools section, trigger consistency verification, view reported issues, apply a fix, verify resolved state.

### Tests
- [ ] T071 [P] [US8] Integration smoke tests for consistency tool views in `WebUI/src/test/ts/admin/AdminShell.test.tsx`

### Implementation
- [ ] T072 [P] [US8] Create tools navigation layout `WebUI/src/main/ts/admin/tools/ToolsSection.tsx`
- [ ] T073 [P] [US8] Create consistency check results display `WebUI/src/main/ts/admin/tools/ConsistencyChecker.tsx`
- [ ] T074 [US8] Wire system tools view into main shell `WebUI/src/main/ts/admin/AdminShell.tsx`
- [ ] T075 [US8] Commit changes and submit PR for review, pausing downstream tasks
- [ ] T076 [US8] Monitor Kilo Code check, address feedback, and resolve comments (pause on check failure)
- [ ] T077 [US8] Verify human approval and merge of PR before proceeding

## Phase 11: Polish & Cross-Cutting Concerns
- [ ] T078 [P] Update WebUI README.md with modernization info and routing paths
- [ ] T079 [P] Remove all legacy JSP/JS files specified in plan.md legacy removal checklist
- [ ] T080 Verify no JSP compilation or build path breakages after files cleanup
- [ ] T081 Compile the full production build (`npm run build`) and verify build sizes
- [ ] T082 Run full spotless code formatter pass on code tree
- [ ] T083 Verify i18n lint checks pass and TMX file contains no missing strings
- [ ] T084 Run accessibility checker against Workflow and Admin panels, verify zero failures
- [ ] T085 Run the full suite of automated Playwright integration tests and check results

---

## Dependencies & Execution Order
- **Setup (Phase 1)**: No dependencies.
- **Foundational (Phase 2)**: Depends on Phase 1. Blocks all other phases.
- **Workflow Definitions (Phase 3)**: Depends on Phase 2. Blocks Phase 4 (Site Assign) and User/Role/Category phases.
- **Workflow Assignment (Phase 4)**: Depends on Phase 3.
- **Roles (Phase 5)**: Depends on Phase 2. Blocks User management (needs roles dual list).
- **Users (Phase 6)**: Depends on Phase 5.
- **In-Context Transitions (Phase 7)**: Depends on Phase 2.
- **Categories (Phase 8)**: Depends on Phase 2.
- **Scheduled Tasks (Phase 9)**: Depends on Phase 2. Blocks Consistency checker tools phase.
- **Consistency Check (Phase 10)**: Depends on Phase 9.
- **Polish (Phase 11)**: Depends on all implementation phases.

---

## Parallel Execution Examples
```bash
# Developer A builds Role management views
Task: "Create roles list view in WebUI/src/main/ts/workflowAdmin/roles/RolesSection.tsx"
Task: "Unit test for RolesSection in WebUI/src/test/ts/workflowAdmin/RolesSection.test.tsx"

# Developer B builds Categories tree component in parallel
Task: "Create tree explorer layout WebUI/src/main/ts/workflowAdmin/categories/CategoriesSection.tsx"
Task: "Unit test for tree structure in WebUI/src/test/ts/workflowAdmin/CategoriesSection.test.tsx"
```

---

## Implementation Strategy
- **MVP First**: Establish Phase 1 (Setup) -> Phase 2 (Foundational) -> Phase 3 (Workflow Definitions CRUD) -> Verify E2E.
- **Incremental Delivery**: Deliver each user story phase in order, committing and integrating separately to isolate regressions.
