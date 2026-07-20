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

export const ADMIN_MSG = {
  ADMIN_TITLE: "perc.ui.admin@Administration",
  TAB_TASKS: "perc.ui.admin@Scheduled Tasks",
  TAB_LOGS: "perc.ui.admin@Execution Logs",
  TAB_NOTIFICATIONS: "perc.ui.admin@Notification Settings",
  
  CREATE_TASK: "perc.ui.admin@Create Task",
  EDIT_TASK: "perc.ui.admin@Edit Task",
  DELETE_TASK: "perc.ui.admin@Delete Task",
  CONFIRM_DELETE_TASK: "perc.ui.admin@Are you sure you want to delete task \"{0}\"?",
  
  TASK_NAME: "perc.ui.admin@Task Name",
  CRON_EXPRESSION: "perc.ui.admin@Cron Expression",
  TASK_TYPE: "perc.ui.admin@Task Type",
  NOTIFY_WHEN: "perc.ui.admin@Notify When",
  EMAIL_ADDRESSES: "perc.ui.admin@Email Addresses",
  NOTIFICATION_TEMPLATE: "perc.ui.admin@Notification Template",
  ENABLED: "perc.ui.admin@Enabled",
  SERVER: "perc.ui.admin@Server",
  
  RUN_NOW: "perc.ui.admin@Run Now",
  PURGE_LOGS: "perc.ui.admin@Purge Logs",
  CONFIRM_PURGE_LOGS: "perc.ui.admin@Are you sure you want to purge all execution logs?",
  
  LOG_TIME: "perc.ui.admin@Log Time",
  STATUS: "perc.ui.admin@Status",
  MESSAGE: "perc.ui.admin@Message",
  SERVER_NAME: "perc.ui.admin@Server Name",
  
  NAME_REQUIRED: "perc.ui.admin@Task name is required.",
  CRON_REQUIRED: "perc.ui.admin@Cron expression is required.",
  TYPE_REQUIRED: "perc.ui.admin@Task type is required.",
  
  TEMPLATE_NAME: "perc.ui.admin@Template Name",
  SUBJECT: "perc.ui.admin@Subject",
  BODY: "perc.ui.admin@BodyText",
  SAVE: "perc.ui.admin@Save",
  CANCEL: "perc.ui.admin@Cancel",
  LOADING: "perc.ui.admin@Loading...",
  ERROR_GENERIC: "perc.ui.admin@An unexpected error occurred.",
} as const;
