/*
 * Copyright 1999-2026 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

export const WF_ADMIN_MSG = {
  TITLE: "perc.ui.workflowAdmin@Workflow & Administration",
  TAB_WORKFLOW: "perc.ui.workflow@Workflow",
  TAB_ROLES: "perc.ui.roles@Roles",
  TAB_USERS: "perc.ui.users@Users",
  TAB_CATEGORIES: "perc.ui.admin.workflow@Categories",
  
  // Workflows
  CREATE_WORKFLOW: "perc.ui.workflow.view@Add New Workflow",
  EDIT_WORKFLOW: "perc.ui.workflow@Edit Workflow Details",
  DELETE_WORKFLOW: "perc.ui.workflow.view@Delete Workflow",
  WORKFLOW_NAME: "perc.ui.workflow@Name",
  MAKE_DEFAULT: "perc.ui.workflow@Make Default",
  IS_DEFAULT: "perc.ui.workflow@Default",
  STAGING_ROLE: "perc.ui.workflow@Staging Role",
  ASSIGN_SITES_FOLDERS: "perc.ui.workflow.view@Assign to Site/Folder",
  ADD_STEP: "perc.ui.workflow.steps.view@Add New Step",
  STEP_NAME: "perc.ui.workflow.steps.view@Step Name",
  STEP_ROLES: "perc.ui.workflow.steps.view@Assigned Roles",

  // Common
  SAVE: "perc.ui.workflow.steps.view@Submit",
  CANCEL: "perc.ui.workflow.steps.view@Cancel",
  DELETE: "perc.ui.workflow.steps.view@Delete Step",
  LOADING: "perc.ui.workflow.view@Loading...",
  ERROR_GENERIC: "perc.ui.workflow.view@An error occurred while processing your request.",
} as const;
