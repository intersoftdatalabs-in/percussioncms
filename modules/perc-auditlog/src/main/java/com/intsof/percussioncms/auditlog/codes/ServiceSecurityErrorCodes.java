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
 * Security-service error catalog bridging legacy {@code
 * com.percussion.services.security.IPSSecurityErrors} package-local ints (1–14).
 *
 * <p>Every constant sets {@link #isAuditable()} explicitly: access-denied / authentication /
 * authorization / session / policy dual-write; ACL lookup and configuration operational noise does
 * not.
 *
 * <p><strong>Flat registry collision:</strong> package-local ints {@code 1–14} already belong to
 * {@link WorkflowErrorCodes} (1–10) and residual catalogs. This catalog does <strong>not</strong>
 * flat-register any ints. Prefer this enum directly (including {@link #ACCESS_DENIED}). Distinct
 * from {@link SecurityErrorCodes} (9000+ {@code com.percussion.security.IPSSecurityErrors}). Module
 * code is {@link AuditModule#SEC}.
 */
public enum ServiceSecurityErrorCodes implements SystemErrorCode {

  MISSING_COMMUNITY(
      1,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Missing community: {}",
      "Missing community id={}"),

  ACL_NOT_FOUND(
      2,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "ACL not found: {}",
      "ACL not found aclId={}"),

  OBJECT_ACL_NOT_FOUND(
      3,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Object ACL not found",
      "Object ACL not found guid={} type={}"),

  ACL_SAVE_ERROR(
      4,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "ACL save error",
      "ACL save error aclGuid={} detail={}"),

  ACL_DELETE_ERROR(
      5,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "ACL delete error",
      "ACL delete error aclGuid={} detail={}"),

  ACCESS_DENIED(
      6,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "Access denied",
      "Access denied objectGuid={} user={}"),

  AUTHENTICATION_FAILED(
      7,
      true,
      AuditEventType.AUTH_FAILURE,
      AuditOutcome.FAILURE,
      "Authentication failed",
      "Authentication failed user={} reason={}"),

  AUTHORIZATION_FAILED(
      8,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "Authorization failed",
      "Authorization failed user={} operation={}"),

  ACL_OPERATION_FAILED(
      9,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "ACL operation failed",
      "ACL operation failed operation={} detail={}"),

  CONFIGURATION_ERROR(
      10,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Security configuration error: {}",
      "Security configuration error detail={}"),

  ROLE_MANAGEMENT_ERROR(
      11,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Role management error",
      "Role management error role={} operation={}"),

  PRINCIPAL_VALIDATION_ERROR(
      12,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Principal validation error",
      "Principal validation error principal={} detail={}"),

  SESSION_SECURITY_ERROR(
      13,
      true,
      AuditEventType.AUTH_FAILURE,
      AuditOutcome.FAILURE,
      "Session security error",
      "Session security error sessionId={} detail={}"),

  SECURITY_POLICY_VIOLATION(
      14,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "Security policy violation",
      "Security policy violation policy={} detail={}");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  ServiceSecurityErrorCodes(
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
    return AuditModule.SEC;
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
