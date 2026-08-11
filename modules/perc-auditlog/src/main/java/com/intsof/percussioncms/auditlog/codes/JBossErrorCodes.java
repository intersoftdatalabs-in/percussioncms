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
 * Legacy JBoss container error catalog bridging {@code
 * com.percussion.utils.container.jboss.IPSJBossErrors} (only {@code APP_POLICY_ELEMENT_MISSING =
 * 1}).
 *
 * <p><strong>Retired-container note:</strong> JBoss is no longer the product runtime (Jetty is).
 * This catalog exists so residual legacy ints remain type-safe and explicitly non-auditable if
 * any install/upgrade path still loads JBoss error bundles.
 *
 * <p>All constants set {@link #isAuditable()} to {@code false}.
 *
 * <p><strong>Flat registry collision:</strong> package-local int {@code 1} already belongs to
 * {@link WorkflowErrorCodes}. This catalog therefore does <strong>not</strong> flat-register any
 * ints. Prefer this enum directly. Module code is {@link AuditModule#CFG}.
 */
public enum JBossErrorCodes implements SystemErrorCode {

  APP_POLICY_ELEMENT_MISSING(
      1,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "JBoss app policy element missing",
      "JBoss app policy element missing policy={} file={}");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  JBossErrorCodes(
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
   * No-op for the flat {@link com.intsof.percussioncms.auditlog.LegacyErrorCodeRegistry}: package-local
   * int {@code 1} collides with {@link WorkflowErrorCodes}. Prefer this enum directly. Safe to call
   * repeatedly for bootstrap symmetry.
   */
  public static void ensureRegistered() {
    // Intentionally empty — package-local int collides with WorkflowErrorCodes.
  }

  @Override
  public AuditModule module() {
    return AuditModule.CFG;
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
