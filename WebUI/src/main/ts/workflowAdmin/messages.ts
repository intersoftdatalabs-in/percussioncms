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
  WORKFLOWS_TITLE: "perc.ui.workflow.view@Workflows",
  CREATE_WORKFLOW: "perc.ui.workflow.view@Add New Workflow",
  EDIT_WORKFLOW: "perc.ui.workflow@Edit Workflow Details",
  DELETE_WORKFLOW: "perc.ui.workflow.view@Delete Workflow",
  WORKFLOW_NAME: "perc.ui.workflow@Name",
  MAKE_DEFAULT: "perc.ui.workflow@Make Default",
  IS_DEFAULT: "perc.ui.workflow@Default",
  STAGING_ROLE: "perc.ui.workflow@Staging Role",
  ASSIGN_SITES_FOLDERS: "perc.ui.workflow.view@Assign to Site/Folder",
  ASSIGN_TITLE: "perc.ui.workflow.view@Assign Workflow to Site or Folder",
  SELECT_SITE_OR_FOLDER: "perc.ui.workflow.view@Select a target site or folder to assign this workflow:",
  START_JOB: "perc.ui.workflow.view@Start Assignment",
  JOB_IN_PROGRESS: "perc.ui.workflow.view@Assignment job in progress...",
  JOB_COMPLETE: "perc.ui.workflow.view@Workflow assigned successfully!",
  JOB_FAILED: "perc.ui.workflow.view@Workflow assignment job failed.",
  NAME_REQUIRED: "perc.ui.workflow.view@Workflow name is required.",
  NO_WORKFLOWS_FOUND: "perc.ui.workflow.view@No workflows found.",
  CONFIRM_DELETE_WORKFLOW: "perc.ui.workflow.view@Are you sure you want to delete workflow \"{0}\"?",
  CANNOT_DELETE_DEFAULT: "perc.ui.workflow.view@Cannot delete the system default workflow.",
  DELETE_FAILED: "perc.ui.workflow.view@Failed to delete workflow.",
  SAVE_FAILED: "perc.ui.workflow.view@Failed to save workflow.",

  // Roles
  ROLES_TITLE: "perc.ui.roles.view@Roles",
  CREATE_ROLE: "perc.ui.role.view@Create Role",
  EDIT_ROLE: "perc.ui.role.view@Edit Role",
  ROLE_NAME: "perc.ui.role.view@Role Name",
  ROLE_MEMBERS: "perc.ui.role.view@Assigned Users",
  AVAILABLE_USERS: "perc.ui.role.view@Available Users",
  CONFIRM_DELETE_ROLE: "perc.ui.role.view@Are you sure you want to delete role \"{0}\"?",
  ROLE_NAME_REQUIRED: "perc.ui.role.view@Role name is required.",

  // Steps
  SECTION_STEPS: "perc.ui.workflow.steps.view@Workflow Steps",
  ADD_STEP: "perc.ui.workflow.steps.view@Add New Step",
  ADD_STEP_TITLE: "perc.ui.workflow.steps.view@Add Step",
  EDIT_STEP_TITLE: "perc.ui.workflow.steps.view@Edit Step",
  STEP_NAME: "perc.ui.workflow.steps.view@Step Name",
  STEP_ROLES: "perc.ui.workflow.steps.view@Assigned Roles",
  NO_STEPS_DEFINED: "perc.ui.workflow.steps.view@No steps defined. Click \"Add New Step\" to create one.",

  // Common & Table
  EDIT: "perc.ui.workflow.steps.view@Edit",
  DELETE: "perc.ui.workflow.steps.view@Delete",
  NONE: "perc.ui.workflow@None",
  TABLE_HASH: "perc.ui.workflow@#",
  TABLE_ACTIONS: "perc.ui.workflow@Actions",
  NO_ROLES_AVAILABLE: "perc.ui.roles@No roles available",
  SAVE: "perc.ui.workflow.steps.view@Submit",
  CANCEL: "perc.ui.workflow.steps.view@Cancel",
  LOADING: "perc.ui.workflow.view@Loading...",
  ERROR_GENERIC: "perc.ui.workflow.view@An error occurred while processing your request.",
} as const;
