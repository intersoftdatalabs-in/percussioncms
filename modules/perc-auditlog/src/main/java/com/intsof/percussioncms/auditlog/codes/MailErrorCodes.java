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
 * Mail error catalog bridging legacy {@code com.percussion.mail.IPSMailErrors} ints (3501–3508:
 * address validation, send failures, mail server connectivity).
 *
 * <p>Every constant sets {@link #isAuditable()} to {@code false}: mail address / transport failures
 * are operational noise, not security dual-write events.
 *
 * <p>All ints are globally unique and fully flat-registered in {@link LegacyErrorCodeRegistry}.
 * Module code is {@link AuditModule#SYS}.
 */
public enum MailErrorCodes implements SystemErrorCode {

  MAIL_ADDRESS_EMPTY(
      3501,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Mail address empty",
      "Mail address empty"),

  MAIL_ADDRESS_INVALID(
      3502,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Mail address invalid",
      "Mail address invalid address={}"),

  MAIL_CUSTOM_TO_HEADER_INVALID(
      3503,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Mail custom To header invalid",
      "Mail custom To header invalid header={}"),

  MAIL_CUSTOM_TO_HEADER_EMPTY(
      3504,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Mail custom To header empty",
      "Mail custom To header empty"),

  MAIL_SEND_UNEXPECTED_EXCEPTION(
      3505,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Mail send unexpected exception",
      "Mail send unexpected exception detail={}"),

  MAIL_SERVER_UP_EXCEPTION(
      3506,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Mail server up exception",
      "Mail server up exception detail={}"),

  MAIL_SERVER_CONNECTION_ERROR(
      3507,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Mail server connection error",
      "Mail server connection error detail={}"),

  HOST_NOT_VALID(
      3508,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Mail host not valid",
      "Mail host not valid detail={}");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  MailErrorCodes(
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
    for (MailErrorCodes code : values()) {
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
