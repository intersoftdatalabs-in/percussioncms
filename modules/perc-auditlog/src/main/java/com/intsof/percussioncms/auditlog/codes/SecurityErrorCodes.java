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
 * Phase 2b auth/security error catalog bridging legacy {@code IPSSecurityErrors} ints (9001–9026
 * general range plus directory-auth codes used on login paths).
 *
 * <p>{@link #numericCode()} preserves the historical int so legacy exception constructors and
 * resource bundles remain stable. Every constant sets {@link #isAuditable()} explicitly: only
 * security-relevant authentication/authorization failures dual-write; operational/provider config
 * noise does not.
 *
 * <p>High-level login success/failure audit events remain on {@link AuthenticationErrorCodes}
 * (AUTH-100x). This catalog is the exception/error-code bridge for central handlers.
 */
public enum SecurityErrorCodes implements SystemErrorCode {

  // --- general security / auth (9001–9026) ---

  AUTHENTICATION_NOT_SUPPORTED(
      9001,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Authentication not supported for provider {}",
      "Authentication not supported providerType={}"),

  AUTHENTICATION_FAILED(
      9002,
      true,
      AuditEventType.AUTH_FAILURE,
      AuditOutcome.FAILURE,
      "Authentication failed for user {}",
      "Authentication failed providerType={} instance={} user={}"),

  PROVIDER_NOT_SUPPORTED_BY_CLASS(
      9003,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Security provider not supported",
      "Provider not supported class={} requestedType={}"),

  FILTERS_NOT_SUPPORTED(
      9004,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Filters not supported for provider {}",
      "Filters not supported providerType={}"),

  USERS_NOT_SUPPORTED(
      9005,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Users not supported for provider {}",
      "Users not supported providerType={}"),

  GROUPS_NOT_SUPPORTED(
      9006,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Groups not supported for provider {}",
      "Groups not supported providerType={}"),

  AUTHENTICATION_REQUIRED(
      9007,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "Authentication required for {} {}",
      "Authentication required resourceType={} resourceName={}"),

  SESS_NOT_AUTHORIZED(
      9008,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "Session not authorized for {} {}",
      "Session not authorized resourceType={} resourceName={} session={}"),

  USER_NOT_AUTHORIZED(
      9009,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "User not authorized for {} {}",
      "User not authorized resourceType={} resourceName={} provider={} user={}"),

  PROVIDER_INIT_EXCEPTION(
      9010,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Security provider initialization failed",
      "Provider init failed type={} instance={} detail={}"),

  PROVIDER_UNKNOWN(
      9011,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unknown security provider",
      "Unknown provider type={} instance={}"),

  AUTHENTICATION_FAILED_WITH_MSG(
      9012,
      true,
      AuditEventType.AUTH_FAILURE,
      AuditOutcome.FAILURE,
      "Authentication failed for user {}",
      "Authentication failed providerType={} instance={} user={} reason={}"),

  PROVIDER_INIT_CATALOG_DISABLED(
      9013,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Security provider catalog disabled",
      "Provider catalog disabled type={} instance={} detail={}"),

  PROVIDER_INSTANCE_NAME_DUPLICATED(
      9014,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Duplicate security provider instance name",
      "Duplicate provider instance name={}"),

  NATIVE_AUTHENTICATION_FAILURE(
      9015,
      true,
      AuditEventType.AUTH_FAILURE,
      AuditOutcome.FAILURE,
      "Native authentication failed",
      "Native authentication failure"),

  SSL_KEY_STRENGTH_TOO_WEAK(
      9016,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "SSL key strength too weak",
      "SSL key strength too weak session={} requiredBits={} suppliedBits={}"),

  DATA_ENCRYPTION_ERROR(
      9017,
      true,
      AuditEventType.OTHER,
      AuditOutcome.ERROR,
      "Data encryption error",
      "Data encryption handler error"),

  SECURITY_NOT_INITIALIZED(
      9018,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Security subsystem not initialized",
      "Security subsystem not initialized"),

  SECRET_KEY_INVALID_SIZE(
      9019,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid secret key size",
      "Invalid secret key size expected={} actual={}"),

  MULTI_AUTHENTICATION_FAILED(
      9020,
      true,
      AuditEventType.AUTH_FAILURE,
      AuditOutcome.FAILURE,
      "Authentication failed",
      "Multi-provider authentication failed detail={}"),

  GENERIC_AUTHENTICATION_FAILED(
      9021,
      true,
      AuditEventType.AUTH_FAILURE,
      AuditOutcome.FAILURE,
      "Authentication failed",
      "Generic authentication failed"),

  METADATA_UNAVAILABLE(
      9022,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Security metadata unavailable",
      "Security metadata unavailable detail={}"),

  FILTER_WILDCARD_INVALID(
      9023,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid filter wildcard",
      "Invalid filter wildcard={} providerType={}"),

  GET_GROUPS_FAILURE(
      9024,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Failed to get groups",
      "Get groups failure groupProvider={} directory={} error={}"),

  CHECK_GROUP_MEMBER_FAILURE(
      9025,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Group membership check failed",
      "Check group member failure type={} name={} user={} group={} error={}"),

  GROUP_PROVIDER_MISSING(
      9026,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Group provider missing",
      "Group provider missing name={}"),

  // --- directory auth used on login paths ---

  DIR_AUTHENTICATION_FAILED(
      9801,
      true,
      AuditEventType.AUTH_FAILURE,
      AuditOutcome.FAILURE,
      "Directory authentication failed for user {}",
      "Directory authentication failed user={}"),

  DIR_MULTIPLE_ENTRIES_RETURNED(
      9802,
      true,
      AuditEventType.AUTH_FAILURE,
      AuditOutcome.FAILURE,
      "Directory authentication failed for user {}",
      "Directory multiple entries for user={}"),

  DIR_PASSWORD_REQUIRED(
      9804,
      true,
      AuditEventType.AUTH_FAILURE,
      AuditOutcome.FAILURE,
      "Password required for authentication",
      "Directory password required providerType={}"),

  DIRECTORY_AUTHENTICATION_FAILED(
      9862,
      true,
      AuditEventType.AUTH_FAILURE,
      AuditOutcome.FAILURE,
      "Directory authentication failed",
      "Directory authentication failed directory={} auth={} error={}");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  SecurityErrorCodes(
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
    for (SecurityErrorCodes code : values()) {
      LegacyErrorCodeRegistry.register(code.numericCode(), code);
    }
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
