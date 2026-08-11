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
 * Filter service error catalog bridging legacy {@code
 * com.percussion.services.filter.IPSFilterServiceErrors} package-local ints (1–8).
 *
 * <p>Every constant sets {@link #isAuditable()} to {@code false}: filter / authtype lookup and
 * rule graph failures are operational publishing noise.
 *
 * <p><strong>Flat registry collision:</strong> package-local ints {@code 1–8} already belong to
 * {@link WorkflowErrorCodes}. This catalog does <strong>not</strong> flat-register any ints.
 * Prefer this enum directly. Module code is {@link AuditModule#PUB}.
 */
public enum FilterServiceErrorCodes implements SystemErrorCode {

  FILTER_MISSING(
      1,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Filter not found: {}",
      "Filter missing name={}"),

  AUTHTYPE_MISSING(
      2,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unknown authentication type: {}",
      "Filter authtype missing value={}"),

  RULE_MISSING(
      3,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Filter rule not found: {}",
      "Filter rule missing name={}"),

  DATABASE(
      4,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Filter database operation failed",
      "Filter database error"),

  RULE_ARGUMENT_MISSING(
      5,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Required rule parameter missing",
      "Filter rule argument missing rule={} param={}"),

  ARGUMENT_MISSING(
      6,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Required filter argument missing",
      "Filter argument missing"),

  PARAMS_AUTHTYPE_OR_FILTER(
      7,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Missing auth type or filter parameter",
      "Filter params authtype or filter expected"),

  PROBABLE_CYCLE(
      8,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Probable cycle detected in filter rules",
      "Filter probable cycle in rule graph");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  FilterServiceErrorCodes(
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
   * No-op for the flat registry: package-local ints collide with earlier Phase 2b catalogs. Prefer
   * this enum directly. Safe to call repeatedly for bootstrap symmetry.
   */
  public static void ensureRegistered() {
    // Intentionally empty — package-local ints are not flat-registered.
  }

  @Override
  public AuditModule module() {
    return AuditModule.PUB;
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
