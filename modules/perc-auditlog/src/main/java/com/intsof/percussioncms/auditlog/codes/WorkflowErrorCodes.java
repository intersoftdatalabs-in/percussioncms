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
 * Workflow error / audit catalog for Phase 2a high-level transition events and Phase 2b legacy
 * {@code com.percussion.services.workflow.IPSWorkflowErrors} bridges.
 *
 * <p><strong>Numbering (aligned with Phase 2a call-site migrate):</strong>
 *
 * <ul>
 *   <li>{@code 4001} — intentional workflow transition audit event
 *   <li>{@code 1–10} — package-local {@code IPSWorkflowErrors} ints (preserved for exception /
 *       bundle lockstep). These package-local ints are registered in the flat {@link
 *       LegacyErrorCodeRegistry} for this slice because no other Phase 2b catalog claims the same
 *       bare ints; residual slices must not re-register colliding package-local codes without a
 *       composite-key registry.
 * </ul>
 *
 * <p>Every constant sets {@link #isAuditable()} explicitly: access-denied and invalid-transition
 * security-relevant failures dual-write; load/config operational noise does not.
 */
public enum WorkflowErrorCodes implements SystemErrorCode {

  // --- Phase 2a high-level workflow audit event (WF-4001) ---

  TRANSITION(
      4001,
      true,
      AuditEventType.WORKFLOW_TRANSITION,
      AuditOutcome.SUCCESS,
      "Workflow transition for content {}",
      "Workflow transition contentId={} guid={} from={} to={}"),

  // --- legacy com.percussion.services.workflow.IPSWorkflowErrors (1–10) ---

  WORKFLOW_NOT_FOUND(
      1,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Workflow not found: {}",
      "Workflow not found workflowId={}"),

  /**
   * Workflow state not found. Legacy alias {@code IPSWorkflowErrors.ERROR_LOADING_WORKFLOW_STATE}
   * uses the same int ({@code 2}).
   */
  STATE_NOT_FOUND(
      2,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Workflow state not found",
      "Workflow state not found stateId={} workflowId={}"),

  INVALID_STATE(
      3,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid workflow state operation",
      "Invalid workflow state stateId={} workflowId={} reason={}"),

  OPERATION_FAILED(
      4,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Workflow operation failed: {}",
      "Workflow operation failed operation={} detail={}"),

  VALIDATION_FAILED(
      5,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Workflow validation failed",
      "Workflow validation failed workflowId={} message={}"),

  ACCESS_DENIED(
      6,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "Access denied to workflow {}",
      "Workflow access denied workflowId={} user={}"),

  TRANSITION_NOT_FOUND(
      7,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Workflow transition not found",
      "Workflow transition not found transitionId={} stateId={} workflowId={}"),

  INVALID_TRANSITION(
      8,
      true,
      AuditEventType.WORKFLOW_TRANSITION,
      AuditOutcome.FAILURE,
      "Invalid workflow transition",
      "Invalid workflow transition transitionId={} currentStateId={} reason={}"),

  CONFIGURATION_ERROR(
      9,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Workflow configuration error",
      "Workflow configuration error detail={}"),

  ASSIGNMENT_ERROR(
      10,
      true,
      AuditEventType.OTHER,
      AuditOutcome.FAILURE,
      "Content workflow assignment error",
      "Workflow assignment error contentId={} workflowId={} detail={}");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  WorkflowErrorCodes(
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
    for (WorkflowErrorCodes code : values()) {
      LegacyErrorCodeRegistry.register(code.numericCode(), code);
    }
  }

  @Override
  public AuditModule module() {
    return AuditModule.WF;
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
