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
package com.intsof.percussioncms.auditlog.codes;

import com.intsof.percussioncms.auditlog.AuditEventType;
import com.intsof.percussioncms.auditlog.AuditModule;
import com.intsof.percussioncms.auditlog.AuditOutcome;
import com.intsof.percussioncms.auditlog.SystemErrorCode;

/**
 * User-management administrative audit codes (create / update / delete / password re-encrypt).
 * Outcome is set at the call site when success and failure share the same code.
 */
public enum UserManagementErrorCodes implements SystemErrorCode {
  CREATE(
      3001,
      true,
      AuditEventType.USER_CREATE,
      AuditOutcome.SUCCESS,
      "User {} created",
      "User create actor={} target={}"),

  UPDATE(
      3002,
      true,
      AuditEventType.USER_UPDATE,
      AuditOutcome.SUCCESS,
      "User {} updated",
      "User update actor={} target={} activity={}"),

  DELETE(
      3003,
      true,
      AuditEventType.USER_DELETE,
      AuditOutcome.SUCCESS,
      "User {} deleted",
      "User delete actor={} target={}"),

  DISABLE(
      3004,
      true,
      AuditEventType.USER_DISABLE,
      AuditOutcome.SUCCESS,
      "User {} disabled",
      "User disable actor={} target={}"),

  REVOKE(
      3005,
      true,
      AuditEventType.ROLE_REMOVE,
      AuditOutcome.SUCCESS,
      "Permissions revoked for user {}",
      "User revoke actor={} target={} activity={}");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  UserManagementErrorCodes(
      int numericCode,
      boolean auditable,
      AuditEventType eventType,
      AuditOutcome defaultOutcome,
      String userMessageTemplate,
      String logMessageTemplate) {
    this.numericCode = numericCode;
    this.auditable = auditable;
    this.eventType = eventType;
    this.defaultOutcome = defaultOutcome;
    this.userMessageTemplate = userMessageTemplate;
    this.logMessageTemplate = logMessageTemplate;
  }

  @Override
  public AuditModule module() {
    return AuditModule.USER;
  }

  @Override
  public int numericCode() {
    return numericCode;
  }

  @Override
  public String userMessageTemplate() {
    return userMessageTemplate;
  }

  @Override
  public String logMessageTemplate() {
    return logMessageTemplate;
  }

  @Override
  public boolean isAuditable() {
    return auditable;
  }

  @Override
  public AuditEventType eventType() {
    return eventType;
  }

  @Override
  public AuditOutcome defaultOutcome() {
    return defaultOutcome;
  }
}
