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
 * Beans error catalog bridging legacy {@code com.percussion.error.IPSBeansErrors} ints (only
 * {@code XML_PROCESSING_ERROR = 1001}).
 *
 * <p>All constants set {@link #isAuditable()} to {@code false}: beans XML processing failures are
 * operational noise, not security dual-write events.
 *
 * <p><strong>Flat registry collision:</strong> package-local int {@code 1001} already belongs to
 * {@link ServerErrorCodes#NATIVE_ERROR}. This catalog therefore does <strong>not</strong>
 * flat-register any ints. Prefer this enum directly until a composite-key registry exists. Module
 * code is {@link AuditModule#SYS}.
 */
public enum BeansErrorCodes implements SystemErrorCode {

  XML_PROCESSING_ERROR(
      1001,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Beans XML processing error",
      "Beans XML processing error detail={}");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  BeansErrorCodes(
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
   * No-op for the flat {@link com.intsof.percussioncms.auditlog.LegacyErrorCodeRegistry}: int
   * {@code 1001} collides with {@link ServerErrorCodes}. Prefer this enum directly. Safe to call
   * repeatedly for bootstrap symmetry.
   */
  public static void ensureRegistered() {
    // Intentionally empty — package-local int collides with ServerErrorCodes.NATIVE_ERROR.
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
