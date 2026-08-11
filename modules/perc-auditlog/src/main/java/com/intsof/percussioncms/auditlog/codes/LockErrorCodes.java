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
 * Object lock service error catalog bridging legacy {@code
 * com.percussion.services.locking.IPSLockErrors} package-local ints (1–9).
 *
 * <p>Every constant sets {@link #isAuditable()} explicitly: invalid session and permission denied
 * dual-write; already-locked / not-locked operational concurrency noise does not.
 *
 * <p><strong>Flat registry collision:</strong> package-local ints {@code 1–9} already belong to
 * {@link WorkflowErrorCodes}. This catalog does <strong>not</strong> flat-register any ints.
 * Prefer this enum directly (including {@link #PERMISSION_DENIED}). Module code is {@link
 * AuditModule#SYS}.
 */
public enum LockErrorCodes implements SystemErrorCode {

  OBJECT_ALREADY_LOCKED(
      1,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Object is already locked",
      "Object already locked objectId={} locker={}"),

  LOCK_EXTENSION_NOT_LOCKED(
      2,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Object is not locked for extension",
      "Lock extension not locked objectId={}"),

  LOCK_EXTENSION_LOCKED_BY_SOMEBODY_ELSE(
      3,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Object locked by somebody else",
      "Lock extension locked by somebody else objectId={} locker={}"),

  LOCK_EXTENSION_INVALID_SESSION(
      4,
      true,
      AuditEventType.AUTH_FAILURE,
      AuditOutcome.FAILURE,
      "Invalid session for lock extension",
      "Lock extension invalid session objectId={}"),

  OBJECT_NOT_LOCKED(
      5,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Object is not locked",
      "Object not locked objectId={}"),

  MULTI_OPERATION(
      6,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Multi-object lock operation mixed results",
      "Lock multi-operation mixed results"),

  LOCK_NOT_FOUND(
      7,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Lock not found",
      "Lock not found"),

  LOCK_EXPIRED(
      8,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Lock expired",
      "Lock expired"),

  PERMISSION_DENIED(
      9,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "Lock permission denied",
      "Lock permission denied");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  LockErrorCodes(
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
