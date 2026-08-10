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
 * Self-codes for the audit subsystem (viewer/export access, retention, sink failures).
 *
 * <p>Phase 5 / #2716: {@link #VIEWER_ACCESS}, {@link #VIEWER_ACCESS_DENIED}, {@link #EXPORT_ACCESS},
 * and {@link #EXPORT_ACCESS_DENIED} make audit-log viewer and export access itself auditable
 * (audit-of-audit). Nested dual-write is suppressed by {@code DefaultAuditLogService} reentrancy
 * guard so access events cannot storm.
 */
public enum AuditSubsystemErrorCodes implements SystemErrorCode {
  /**
   * Successful list or detail view of the system security audit log.
   *
   * <p><strong>Log template note (Phase 5 / #2716):</strong> the durable log message shape is
   * {@code actor={} action={} detail={}} (action = {@code list}/{@code detail}, detail = filter
   * summary or audit id). Pre-#2716 main only had a two-arg {@code actor={} filters={}} form with
   * no list/detail split. SIEM / log parsers for {@link AuditEventType#AUDIT_VIEW} success must
   * match the three-arg template; structured attributes on the audit row still carry {@code
   * action} and {@code detail} independently of free-text log format.
   */
  VIEWER_ACCESS(
      1,
      true,
      AuditEventType.AUDIT_VIEW,
      AuditOutcome.SUCCESS,
      "Audit log viewed by {}",
      "Audit log viewed actor={} action={} detail={}"),

  SINK_FAILURE(
      2,
      true,
      AuditEventType.AUDIT_SINK_FAILURE,
      AuditOutcome.ERROR,
      "Audit sink failure",
      "Audit sink failure sink={} logId={} detail={}"),

  /** Explicit deny when a principal lacks Admin / sys_securityAuditLogViewer. */
  VIEWER_ACCESS_DENIED(
      3,
      true,
      AuditEventType.AUDIT_VIEW,
      AuditOutcome.FAILURE,
      "Audit log view denied for {}",
      "Audit log view denied actor={} action={} reason={}"),

  /** Successful CSV/JSON export of the system security audit log. */
  EXPORT_ACCESS(
      4,
      true,
      AuditEventType.AUDIT_EXPORT,
      AuditOutcome.SUCCESS,
      "Audit log exported by {}",
      "Audit log export actor={} format={} detail={}"),

  /** Explicit deny on export when the principal is not authorized. */
  EXPORT_ACCESS_DENIED(
      5,
      true,
      AuditEventType.AUDIT_EXPORT,
      AuditOutcome.FAILURE,
      "Audit log export denied for {}",
      "Audit log export denied actor={} reason={}");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  AuditSubsystemErrorCodes(
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
    return AuditModule.AUDIT;
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
