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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intsof.percussioncms.auditlog;

/**
 * Catalog of auditable event classes (NIST SP 800-53 AU-2 oriented).
 *
 * <p>Only used when {@link SystemErrorCode#isAuditable()} is {@code true}.
 */
public enum AuditEventType {
  AUTH_LOGIN,
  AUTH_LOGOUT,
  AUTH_FAILURE,
  AUTH_SESSION_TIMEOUT,
  ACCESS_ALLOWED,
  ACCESS_DENIED,
  USER_CREATE,
  USER_UPDATE,
  USER_DELETE,
  USER_DISABLE,
  ROLE_ASSIGN,
  ROLE_REMOVE,
  CONFIG_CHANGE,
  ACL_CHANGE,
  CONTENT_CREATE,
  CONTENT_UPDATE,
  CONTENT_DELETE,
  CONTENT_RECYCLE,
  CONTENT_PUBLISH,
  WORKFLOW_TRANSITION,
  DESIGN_CREATE,
  DESIGN_UPDATE,
  DESIGN_DELETE,
  AUDIT_VIEW,
  AUDIT_EXPORT,
  AUDIT_RETENTION,
  AUDIT_SINK_FAILURE,
  OTHER
}
