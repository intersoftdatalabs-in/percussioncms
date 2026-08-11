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
import com.intsof.percussioncms.auditlog.LegacyErrorCodeRegistry;
import com.intsof.percussioncms.auditlog.SystemErrorCode;

/**
 * Servlet hook error catalog bridging legacy {@code com.percussion.hooks.IPSServletErrors} ints
 * (10151–10158: connection, port, request parameters, status).
 *
 * <p>Every constant sets {@link #isAuditable()} to {@code false}: servlet connection / request
 * failures are operational noise, not security dual-write events.
 *
 * <p>All ints are globally unique and fully flat-registered in {@link LegacyErrorCodeRegistry}.
 * Module code is {@link AuditModule#SYS}.
 */
public enum ServletErrorCodes implements SystemErrorCode {

  CONNECTION_ERROR(
      10151,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Connection error",
      "Connection error detail={}"),

  INVALID_PORT_NUMBER(
      10152,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid port number",
      "Invalid port number detail={}"),

  INVALID_REQUEST_PARAMETERS(
      10153,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid request parameters",
      "Invalid request parameters detail={}"),

  SERVLET_DESTROYED(
      10154,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Servlet destroyed",
      "Servlet destroyed detail={}"),

  CONNECTION_FAILURE(
      10155,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Connection failure",
      "Connection failure detail={}"),

  INVALID_STATUS_CODE(
      10156,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid status code",
      "Invalid status code detail={}"),

  SERVLET_INFORMATION(
      10157,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Servlet information",
      "Servlet information detail={}"),

  VERSION_STRING(
      10158,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Version string",
      "Version string detail={}");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  ServletErrorCodes(
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

  static {
    ensureRegistered();
  }

  /**
   * Register (or re-register) all constants in {@link LegacyErrorCodeRegistry}. Safe to call
   * repeatedly — used by registry bootstrap and tests after {@code clearForTests}.
   */
  public static void ensureRegistered() {
    for (ServletErrorCodes code : values()) {
      LegacyErrorCodeRegistry.register(code.numericCode(), code);
    }
  }

  @Override
  public AuditModule module() {
    return AuditModule.SYS;
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
