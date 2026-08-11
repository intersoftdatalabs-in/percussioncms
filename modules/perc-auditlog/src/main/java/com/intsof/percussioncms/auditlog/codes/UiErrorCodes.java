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
 * UI service error catalog bridging legacy {@code com.percussion.services.ui.IPSUiErrors}
 * package-local ints (1–8: hierarchy nodes).
 *
 * <p>Every constant sets {@link #isAuditable()} explicitly: access denied dual-writes; hierarchy
 * validation and lookup noise does not.
 *
 * <p><strong>Flat registry collision:</strong> package-local ints {@code 1–8} already belong to
 * {@link WorkflowErrorCodes}. This catalog does <strong>not</strong> flat-register any ints.
 * Prefer this enum directly (including {@link #ACCESS_DENIED}). Module code is {@link
 * AuditModule#SYS}.
 */
public enum UiErrorCodes implements SystemErrorCode {

  MISSING_HIERARCHY_NODE(
      1,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Missing hierarchy node: {}",
      "Missing hierarchy node id={}"),

  DUPLICATE_NODE_NAME(
      2,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Duplicate node name: {}",
      "Duplicate node name name={} parent={}"),

  INVALID_HIERARCHY_OPERATION(
      3,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid hierarchy operation",
      "Invalid hierarchy operation op={} reason={}"),

  NODE_TYPE_MISMATCH(
      4,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Node type mismatch",
      "Node type mismatch nodeId={} expected={} actual={}"),

  OPERATION_FAILED(
      5,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "UI operation failed",
      "UI operation failed op={} detail={}"),

  INVALID_NODE_NAME(
      6,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid node name: {}",
      "Invalid node name name={} rule={}"),

  CIRCULAR_REFERENCE(
      7,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Circular hierarchy reference",
      "Circular hierarchy reference source={} target={}"),

  ACCESS_DENIED(
      8,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "UI access denied",
      "UI access denied operation={} resource={}");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  UiErrorCodes(
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
