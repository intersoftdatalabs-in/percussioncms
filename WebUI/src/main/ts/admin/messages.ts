/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
  /**
   * Shell page title for Admin tools (#2784 / #2953).
   * Reuses the localized dashboard modern key so operators see "Admin tools"
   * (not "Administration") when top-nav Admin lands on {@code /admin}.
   */
  ADMIN_TITLE: "perc.ui.dashboard.modern@Admin tools",
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
  /** Per-log detail affordance (#6160: React replacement for TaskDetailLog.jsp). */
  LOG_START_TIME: "perc.ui.admin@Start Time",
  LOG_END_TIME: "perc.ui.admin@End Time",
  LOG_ELAPSED: "perc.ui.admin@Elapsed (HH:mm:ss)",
  LOG_DETAIL_MESSAGE: "perc.ui.admin@Detail Message",
  LOG_SHOW_DETAILS: "perc.ui.admin@Show log details",
  LOG_HIDE_DETAILS: "perc.ui.admin@Hide log details",
  
  NAME_REQUIRED: "perc.ui.admin@Task name is required.",
  CRON_REQUIRED: "perc.ui.admin@Cron expression is required.",
  TYPE_REQUIRED: "perc.ui.admin@Task type is required.",
  
  TEMPLATE_NAME: "perc.ui.admin@Template Name",
  SUBJECT: "perc.ui.admin@Subject",
  BODY: "perc.ui.admin@Body",
  SAVE: "perc.ui.admin@Save",
  CANCEL: "perc.ui.admin@Cancel",
  LOADING: "perc.ui.admin@Loading...",
  ERROR_GENERIC: "perc.ui.admin@An unexpected error occurred.",

  /** System tools tab chrome */
  TAB_TOOLS: "perc.ui.admin@System Tools",
  TOOL_CONSISTENCY: "perc.ui.admin.tools@Consistency Checker",
  TOOL_SECURITY_AUDIT: "perc.ui.admin.tools@Security Audit Log",
  TOOL_AUTH_READINESS: "perc.ui.admin.tools@Auth & MFA Readiness",
  /**
   * Isolated tab/tool failure (#3195 / peer #3229). Arg {0} is the section label.
   */
  SECTION_LOAD_FAILED:
    "perc.ui.admin@Unable to load {0}. Other Admin tabs remain available.",

  /** Security Audit Log viewer (Phase 4 / #2619) */
  AUDIT_TITLE: "perc.ui.admin.auditlog@Security Audit Log",
  AUDIT_DESCRIPTION:
    "perc.ui.admin.auditlog@Review durable system security audit events. Requires Admin or the sys_securityAuditLogViewer role property.",
  AUDIT_FILTER_FROM: "perc.ui.admin.auditlog@From",
  AUDIT_FILTER_TO: "perc.ui.admin.auditlog@To",
  AUDIT_FILTER_MODULE: "perc.ui.admin.auditlog@Module",
  AUDIT_FILTER_EVENT_TYPE: "perc.ui.admin.auditlog@Event type",
  AUDIT_FILTER_OUTCOME: "perc.ui.admin.auditlog@Outcome",
  AUDIT_FILTER_ACTOR: "perc.ui.admin.auditlog@Actor",
  AUDIT_FILTER_ALL: "perc.ui.admin.auditlog@All",
  AUDIT_APPLY_FILTERS: "perc.ui.admin.auditlog@Apply filters",
  AUDIT_RESET_FILTERS: "perc.ui.admin.auditlog@Reset",
  AUDIT_PAGE_SIZE: "perc.ui.admin.auditlog@Page size",
  AUDIT_PREV: "perc.ui.admin.auditlog@Previous",
  AUDIT_NEXT: "perc.ui.admin.auditlog@Next",
  /** Args: {0}=start, {1}=end, {2}=total */
  AUDIT_PAGE_SUMMARY: "perc.ui.admin.auditlog@Showing {0}–{1} of {2}",
  AUDIT_EMPTY: "perc.ui.admin.auditlog@No audit log entries match the current filters.",
  AUDIT_FORBIDDEN:
    "perc.ui.admin.auditlog@You do not have permission to view the security audit log.",
  AUDIT_DETAIL_TITLE: "perc.ui.admin.auditlog@Entry detail",
  AUDIT_COL_TIME: "perc.ui.admin.auditlog@Time",
  AUDIT_COL_MODULE: "perc.ui.admin.auditlog@Module",
  AUDIT_COL_EVENT_TYPE: "perc.ui.admin.auditlog@Event type",
  AUDIT_COL_OUTCOME: "perc.ui.admin.auditlog@Outcome",
  AUDIT_COL_ACTOR: "perc.ui.admin.auditlog@Actor",
  AUDIT_COL_TARGET: "perc.ui.admin.auditlog@Target",
  AUDIT_COL_USER_MESSAGE: "perc.ui.admin.auditlog@User message",
  AUDIT_COL_LOG_MESSAGE: "perc.ui.admin.auditlog@Log message",
  AUDIT_COL_ID: "perc.ui.admin.auditlog@Audit ID",
  AUDIT_COL_SOURCE_IP: "perc.ui.admin.auditlog@Source IP",
  AUDIT_COL_SERVER: "perc.ui.admin.auditlog@Server",

  /** Auth & MFA Readiness tool (Milestone 1) */
  AUTH_READINESS_TITLE: "perc.ui.admin.authreadiness@Auth & MFA Readiness",
  AUTH_READINESS_DESCRIPTION:
    "perc.ui.admin.authreadiness@Inspect transactional auth mail, WebAuthn origins, cluster encryption key fingerprint, schema tables, and account email/MFA coverage before enforcing MFA.",
  AUTH_READINESS_REFRESH: "perc.ui.admin.authreadiness@Refresh",
  AUTH_READINESS_FORBIDDEN:
    "perc.ui.admin.authreadiness@Admin role is required to view authentication readiness.",
  AUTH_READINESS_OVERALL_STATUS: "perc.ui.admin.authreadiness@Overall status",
  AUTH_READINESS_MFA_MODE: "perc.ui.admin.authreadiness@MFA mode",
  AUTH_READINESS_PASSWORD_POLICY: "perc.ui.admin.authreadiness@Min password length",
  AUTH_READINESS_KEY_FINGERPRINT: "perc.ui.admin.authreadiness@Cluster key fingerprint",
  AUTH_READINESS_TOTAL_ACCOUNTS: "perc.ui.admin.authreadiness@Total accounts",
  AUTH_READINESS_READY_ACCOUNTS: "perc.ui.admin.authreadiness@Ready accounts",
  AUTH_READINESS_MFA_ENROLLED: "perc.ui.admin.authreadiness@MFA enrolled",
  AUTH_READINESS_EMAIL_ISSUES: "perc.ui.admin.authreadiness@Email issues",
  AUTH_READINESS_INFRA_TITLE: "perc.ui.admin.authreadiness@Infrastructure prerequisites",
  AUTH_READINESS_COL_CHECK: "perc.ui.admin.authreadiness@Check",
  AUTH_READINESS_COL_STATUS: "perc.ui.admin.authreadiness@Status",
  AUTH_READINESS_COL_SUMMARY: "perc.ui.admin.authreadiness@Summary",
  AUTH_READINESS_COL_REMEDIATION: "perc.ui.admin.authreadiness@Remediation",
  AUTH_READINESS_MAIL_TEST_TITLE: "perc.ui.admin.authreadiness@Send test auth email",
  AUTH_READINESS_MAIL_TEST_LABEL: "perc.ui.admin.authreadiness@Recipient email",
  AUTH_READINESS_MAIL_TEST_SEND: "perc.ui.admin.authreadiness@Send test email",
  AUTH_READINESS_MAIL_TEST_SENDING: "perc.ui.admin.authreadiness@Sending…",
  AUTH_READINESS_ACCOUNTS_TITLE: "perc.ui.admin.authreadiness@Accounts requiring remediation",
  AUTH_READINESS_ISSUE_FILTER: "perc.ui.admin.authreadiness@Filter by issue",
  AUTH_READINESS_FILTER_ALL: "perc.ui.admin.authreadiness@All issues",
  AUTH_READINESS_NO_ISSUES:
    "perc.ui.admin.authreadiness@All evaluated accounts satisfy email and MFA readiness checks.",
  AUTH_READINESS_COL_USER: "perc.ui.admin.authreadiness@User",
  AUTH_READINESS_COL_PROVIDER: "perc.ui.admin.authreadiness@Provider",
  AUTH_READINESS_COL_EMAIL: "perc.ui.admin.authreadiness@Email",
  AUTH_READINESS_COL_VERIFIED: "perc.ui.admin.authreadiness@Verified",
  AUTH_READINESS_COL_FACTORS: "perc.ui.admin.authreadiness@MFA factors",
  AUTH_READINESS_COL_ISSUES: "perc.ui.admin.authreadiness@Issues",
} as const;
