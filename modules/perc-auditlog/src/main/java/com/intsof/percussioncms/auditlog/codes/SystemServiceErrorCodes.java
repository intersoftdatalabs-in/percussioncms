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
 * System service error catalog bridging legacy {@code
 * com.percussion.services.system.IPSSystemErrors} package-local ints ({@code
 * MISSING_SHARED_PROPERTY = 1}, {@code ERROR_DETERMINING_FOLDER_READ = 4}).
 *
 * <p>Every constant sets {@link #isAuditable()} to {@code false}: system-service property / folder
 * read failures are operational noise, not security dual-write events.
 *
 * <p><strong>Flat registry collision:</strong> package-local ints {@code 1} and {@code 4} already
 * belong to {@link WorkflowErrorCodes} in {@link LegacyErrorCodeRegistry}. This catalog therefore
 * does <strong>not</strong> flat-register any ints. Call sites should prefer this enum directly
 * until a composite-key registry exists. Module code is {@link AuditModule#SYS}.
 */
public enum SystemServiceErrorCodes implements SystemErrorCode {

  MISSING_SHARED_PROPERTY(
      1,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Missing shared property",
      "Missing shared property id={}"),

  ERROR_DETERMINING_FOLDER_READ(
      4,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Error determining folder read",
      "Error determining folder read contentId={}");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  SystemServiceErrorCodes(
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
   * Bootstrap / test hook. Does not flat-register package-local ints {@code 1} / {@code 4} (WF
   * ownership). Safe to call repeatedly.
   */
  public static void ensureRegistered() {
    // No-op flat register — preserve WorkflowErrorCodes ownership of bare ints 1 and 4.
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
