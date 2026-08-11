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
 * Locale manager error catalog bridging legacy {@code com.percussion.i18n.IPSLocaleErrors} ints
 * (1801–1804: column restore, locale manager init).
 *
 * <p>Every constant sets {@link #isAuditable()} to {@code false}: locale persistence / init
 * failures are operational noise, not security dual-write events.
 *
 * <p>All ints are globally unique and fully flat-registered in {@link LegacyErrorCodeRegistry}.
 * Module code is {@link AuditModule#SYS}.
 */
public enum LocaleErrorCodes implements SystemErrorCode {

  INVALID_COLUMN_VALUE(
      1801,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid locale column value",
      "Invalid locale column value column={} value={}"),

  MISSING_COLUMN(
      1802,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Missing locale column",
      "Missing locale column name={}"),

  LOCALE_MGR_INIT(
      1803,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Locale manager init error",
      "Locale manager init error detail={}"),

  LOCALE_MGR_UNEXPECTED_ERROR(
      1804,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Locale manager unexpected error",
      "Locale manager unexpected error detail={}");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  LocaleErrorCodes(
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
    for (LocaleErrorCodes code : values()) {
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
