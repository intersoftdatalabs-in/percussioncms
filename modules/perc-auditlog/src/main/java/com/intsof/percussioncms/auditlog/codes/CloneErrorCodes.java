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
 * Error catalog bridging legacy {@code com.percussion.server.clone.IPSCloneErrors} ints
 * (17501–17506: clone source id, authz, internal request, resource, role).
 *
 * <p>{@link #numericCode()} preserves historical ints. Every constant sets {@link #isAuditable()}
 * explicitly. {@link #NOT_AUTHENTICACATED} and {@link #NOT_AUTHORIZED} dual-write; parse/resource noise does not.
 *
 * <p>All ints in this catalog are flat-registered in {@link LegacyErrorCodeRegistry} (no
 * package-local collision with already-bootstrapped catalogs). Module code is
 * {@link AuditModule#SYS}.
 */
public enum CloneErrorCodes implements SystemErrorCode {

  INVALID_CLONESOURCEID(
      17501,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid clonesourceid",
      "Invalid clonesourceid detail={}"),
  /** Legacy IPS name misspells “authenticated”; message uses correct spelling. */
  NOT_AUTHENTICACATED(
      17502,
      true,
      AuditEventType.AUTH_FAILURE,
      AuditOutcome.FAILURE,
      "Not authenticated",
      "Not authenticated detail={}"),
  NOT_AUTHORIZED(
      17503,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "Not authorized",
      "Not authorized detail={}"),
  INTERNAL_REQUEST_ERROR(
      17504,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Internal request error",
      "Internal request error detail={}"),
  REQUIRED_RESOURCE_MISSING(
      17505,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Required resource missing",
      "Required resource missing detail={}"),
  ROLE_CREATION_ERROR(
      17506,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Role creation error",
      "Role creation error detail={}");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  CloneErrorCodes(
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
    for (CloneErrorCodes code : values()) {
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
