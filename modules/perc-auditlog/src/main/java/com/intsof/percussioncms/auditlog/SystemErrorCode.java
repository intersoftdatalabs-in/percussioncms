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
 * System-wide error / message code implemented by package-local {@code *ErrorCodes} enums.
 *
 * <p>Unifies product error reporting and audit logging. When {@link #isAuditable()} is {@code
 * true}, reporting this code dual-writes to {@code server.log} and the durable audit store. When
 * {@code false}, the code is operational only (exception text / app log) and must not create an
 * audit row.
 *
 * <p>Message templates use sequential {@code {}} placeholders (SLF4J-style).
 */
public interface SystemErrorCode {

  /** Module that owns this code (drives the {@code [PUB-…]} prefix). */
  AuditModule module();

  /** Numeric code within the module (e.g. {@code 1001}). */
  int numericCode();

  /**
   * User-facing message template with {@code {}} placeholders. Prepared and sanitized for end users
   * / Admin UI.
   */
  String userMessageTemplate();

  /**
   * Forensic message template with {@code {}} placeholders. Prepared and sanitized for {@code
   * server.log} and the durable audit store (may include more detail than the user message).
   */
  String logMessageTemplate();

  /**
   * When {@code true}, dual-write an audit record via {@link AuditLogService}. When {@code false},
   * operational error only — no audit row.
   */
  boolean isAuditable();

  /**
   * Event type for audit catalog (AU-2). Required when {@link #isAuditable()} is {@code true}; may
   * return {@code null} when not auditable.
   */
  AuditEventType eventType();

  /**
   * Default outcome when not overridden at the call site. Auditable failures typically use {@link
   * AuditOutcome#FAILURE}; success-path codes use {@link AuditOutcome#SUCCESS}.
   */
  default AuditOutcome defaultOutcome() {
    return isAuditable() ? AuditOutcome.FAILURE : AuditOutcome.UNKNOWN;
  }

  /** Optional TMX / bundle key for the user message. */
  default String userMessageKey() {
    return "audit." + module().code() + "." + numericCode() + ".user";
  }

  /** Optional TMX / bundle key for the log message. */
  default String logMessageKey() {
    return "audit." + module().code() + "." + numericCode() + ".log";
  }

  /** Qualified code such as {@code PUB-1001}. */
  default String qualifiedCode() {
    return module().code() + "-" + numericCode();
  }
}
