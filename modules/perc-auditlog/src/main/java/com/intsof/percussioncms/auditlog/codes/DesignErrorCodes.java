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
 * Design-object error / audit catalog: high-level design lifecycle events plus legacy {@code
 * IPSObjectStoreErrors} ACL and security-structure ints (range 2001–3000).
 *
 * <p><strong>Numbering:</strong>
 *
 * <ul>
 *   <li>{@code 2901–2903} — intentional design lifecycle audit events (create/update/delete);
 *       unused by historical {@code IPSObjectStoreErrors} (max legacy constant is 2848)
 *   <li>{@code 2201–2356} (subset) — ACL / server-ACL / security-provider-instance structure codes
 *       from {@code IPSObjectStoreErrors}
 * </ul>
 *
 * <p>Every constant sets {@link #isAuditable()} explicitly. Full object-store XML/validation noise
 * (hundreds of codes) is <strong>not</strong> bulk-registered here; unregistered legacy ints remain
 * non-auditable via {@link LegacyErrorCodeRegistry} safe default. Residual slices may expand
 * coverage without changing this contract.
 */
public enum DesignErrorCodes implements SystemErrorCode {

  // --- intentional design lifecycle (DESN-290x) ---

  CREATE(
      2901,
      true,
      AuditEventType.DESIGN_CREATE,
      AuditOutcome.SUCCESS,
      "Design object {} created",
      "Design create type={} name={} guid={}"),

  UPDATE(
      2902,
      true,
      AuditEventType.DESIGN_UPDATE,
      AuditOutcome.SUCCESS,
      "Design object {} updated",
      "Design update type={} name={} guid={}"),

  DELETE(
      2903,
      true,
      AuditEventType.DESIGN_DELETE,
      AuditOutcome.SUCCESS,
      "Design object {} deleted",
      "Design delete type={} name={} guid={}"),

  // --- application / objectstore ACL structure (legacy IPSObjectStoreErrors) ---

  ACL_ENTRYLIST_NULL(
      2201,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "ACL entry list is null",
      "ACL entry list null"),

  ACL_ENTRYLIST_EMPTY(
      2202,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "ACL entry list is empty",
      "ACL entry list empty"),

  APP_ACL_NO_MANAGER(
      2203,
      true,
      AuditEventType.ACL_CHANGE,
      AuditOutcome.FAILURE,
      "Application ACL has no manager entry",
      "Application ACL no manager"),

  ACL_ENTRYLIST_DUPLICATE(
      2204,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Duplicate ACL entry",
      "ACL entry list duplicate"),

  ACL_ENTRY_NAME_EMPTY(
      2205,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "ACL entry name empty",
      "ACL entry name empty"),

  ACL_ENTRY_SP_INVALID(
      2206,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "ACL entry security provider invalid",
      "ACL entry security provider invalid"),

  ACL_ENTRY_SPINST_TOO_BIG(
      2207,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "ACL entry security provider instance name too long",
      "ACL entry SP instance too big"),

  ACL_ENTRY_LEVEL_NOT_FOUND(
      2208,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "ACL entry access level not found",
      "ACL entry level not found"),

  APP_ACL_NULL(
      2213,
      true,
      AuditEventType.ACL_CHANGE,
      AuditOutcome.FAILURE,
      "Application ACL is null",
      "Application ACL null"),

  APP_ACL_EMPTY(
      2214,
      true,
      AuditEventType.ACL_CHANGE,
      AuditOutcome.FAILURE,
      "Application ACL is empty",
      "Application ACL empty"),

  ACL_ENTRY_NAME_TOO_BIG(
      2218,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "ACL entry name too long",
      "ACL entry name too big"),

  ACL_TYPE_INVALID(
      2327,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "ACL type invalid",
      "ACL type invalid"),

  // --- server ACL ---

  SRV_ACL_NULL(
      2351,
      true,
      AuditEventType.ACL_CHANGE,
      AuditOutcome.FAILURE,
      "Server ACL is null",
      "Server ACL null"),

  SRV_ACL_EMPTY(
      2352,
      true,
      AuditEventType.ACL_CHANGE,
      AuditOutcome.FAILURE,
      "Server ACL is empty",
      "Server ACL empty"),

  SRV_ACL_NO_ADMIN(
      2353,
      true,
      AuditEventType.ACL_CHANGE,
      AuditOutcome.FAILURE,
      "Server ACL has no admin entry",
      "Server ACL no admin"),

  SPINST_NAME_TOO_BIG(
      2354,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Security provider instance name too long",
      "Security provider instance name too big"),

  SPINST_TYPE_INVALID(
      2355,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Security provider instance type invalid",
      "Security provider instance type invalid"),

  ACL_SECURITY_LEVEL_INVALID(
      2356,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "ACL security level invalid",
      "ACL security level invalid");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  DesignErrorCodes(
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
    for (DesignErrorCodes code : values()) {
      LegacyErrorCodeRegistry.register(code.numericCode(), code);
    }
  }

  @Override
  public AuditModule module() {
    return AuditModule.DESN;
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
