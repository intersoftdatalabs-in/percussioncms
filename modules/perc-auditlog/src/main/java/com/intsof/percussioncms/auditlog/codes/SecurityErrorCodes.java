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
 * Phase 2b auth/security error catalog bridging legacy {@code IPSSecurityErrors} ints (9001–10000
 * range: general auth, host/OS/role providers, directory auth, and directory cataloger).
 *
 * <p>{@link #numericCode()} preserves the historical int so legacy exception constructors and
 * resource bundles remain stable. Every constant sets {@link #isAuditable()} explicitly: only
 * security-relevant authentication/authorization failures dual-write; operational/provider config
 * noise does not.
 *
 * <p>High-level login success/failure audit events remain on {@link AuthenticationErrorCodes}
 * (AUTH-100x). This catalog is the exception/error-code bridge for central handlers.
 *
 * <p>Note: the reserved ACL range 9301–9500 has no historical constants in {@code
 * IPSSecurityErrors}; host provider starts at 9501.
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
      "Directory authentication failed directory={} auth={} error={}"),

  // --- host address provider (9501–9550) ---

  HOST_ADDR_FILTER_INVALID(
      9501,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid host address filter",
      "Host address filter invalid filter={}"),

  // --- OS security provider (9601–9650) ---

  OS_IMPERSONATE_FAILURE(
      9601,
      true,
      AuditEventType.AUTH_FAILURE,
      AuditOutcome.FAILURE,
      "OS impersonation failed",
      "OS impersonate failure"),

  OSMETA_GET_OBJECTS_FAILURE(
      9602,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "OS metadata getObjects failed",
      "OS metadata getObjects failure"),

  OSMETA_GET_OBJECT_TYPES_FAILURE(
      9603,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "OS metadata getObjectTypes failed",
      "OS metadata getObjectTypes failure"),

  OSMETA_GET_SERVERS_FAILURE(
      9604,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "OS metadata getServers failed",
      "OS metadata getServers failure"),

  OSMETA_GET_ATTRIBUTES_FAILURE(
      9605,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "OS metadata getAttributes failed",
      "OS metadata getAttributes failure"),

  // --- role security provider (9701–9750) ---

  LOCAL_ROLE_NOT_DEFINED(
      9701,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Local role not defined",
      "Local role not defined role={} application={}"),

  GLOBAL_ROLE_NOT_DEFINED(
      9702,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Global role not defined",
      "Global role not defined role={}"),

  LOCAL_ROLE_ALREADY_DEFINED(
      9703,
      true,
      AuditEventType.ROLE_ASSIGN,
      AuditOutcome.FAILURE,
      "Local role already defined",
      "Local role already defined role={} application={}"),

  GLOBAL_ROLE_ALREADY_DEFINED(
      9704,
      true,
      AuditEventType.ROLE_ASSIGN,
      AuditOutcome.FAILURE,
      "Global role already defined",
      "Global role already defined role={}"),

  ROLE_NAME_REQD(
      9705,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Role name required",
      "Role name required"),

  ROLE_DEF_REQD(
      9706,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Role definition required",
      "Role definition required"),

  ROLE_OVERWRITE_REQD(
      9707,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Role overwrite required",
      "Role overwrite required for existing role"),

  ROLE_METADATA_RESOURCE_NOT_FOUND(
      9708,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Role metadata resource not found",
      "Role metadata resource not found"),

  // --- directory provider leftovers (9803, 9805–9809; 9801/9802/9804 already above) ---

  DIR_PASSWORD_FILTER_INIT_ERROR(
      9803,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Directory password filter initialization failed",
      "Directory password filter init class={} detail={}"),

  DIR_GET_OBJECTS_FAILED(
      9805,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Directory get objects failed",
      "Directory get objects failed error={}"),

  DIR_REFERENCED_DIRECTORYSET_NOT_FOUND(
      9806,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Referenced directory set not found",
      "Referenced directory set not found name={}"),

  DIR_REFERENCED_DIRECTORY_NOT_FOUND(
      9807,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Referenced directory not found",
      "Referenced directory not found name={}"),

  DIR_REFERENCED_AUTHENTICATION_NOT_FOUND(
      9808,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Referenced authentication not found",
      "Referenced authentication not found name={}"),

  DIR_REFERENCED_ROLEPROVIDER_NOT_FOUND(
      9809,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Referenced role provider not found",
      "Referenced role provider not found name={}"),

  // --- directory / backend table provider (9851–9861; 9862 already above) ---

  BETABLE_ERROR_CLOSING_RESOURCES(
      9851,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Backend table provider resource close error",
      "Backend table close resources instance={} detail={}"),

  BETABLE_ERROR_UID_NOT_UNIQUE(
      9852,
      true,
      AuditEventType.AUTH_FAILURE,
      AuditOutcome.FAILURE,
      "Backend table user id not unique",
      "Backend table uid not unique instance={} userId={}"),

  NO_EMAIL_ATTRIBUTE_NAME(
      9853,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "No email attribute name configured",
      "No email attribute name configured"),

  BETABLE_DIRECTORY_CATALOGER_ERROR(
      9854,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Backend directory cataloger error",
      "Backend directory cataloger error detail={}"),

  CATALOG_PROVIDER_CLASS_NOT_FOUND(
      9855,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Catalog provider class not found",
      "Catalog provider class not found class={} stack={}"),

  CATALOG_PROVIDER_INSTANTIATION_FAILED(
      9856,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Catalog provider instantiation failed",
      "Catalog provider instantiation failed class={} stack={}"),

  CATALOG_PROVIDER_ILLEGAL_ACCESS(
      9857,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Catalog provider illegal access",
      "Catalog provider illegal access class={} stack={}"),

  CATALOG_PROVIDER_INVOCATION_TARGET_ERROR(
      9858,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Catalog provider invocation target error",
      "Catalog provider invocation target class={} stack={}"),

  CATALOG_PROVIDER_NO_SUCH_METHOD(
      9859,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Catalog provider constructor not found",
      "Catalog provider no such method class={} stack={}"),

  REFERENCED_DIRECTORY_NOT_FOUND(
      9860,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Referenced directory not found",
      "Referenced directory not found name={}"),

  REFERENCED_AUTHENTICATION_NOT_FOUND(
      9861,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Referenced authentication not found",
      "Referenced authentication not found name={}"),

  // --- directory cataloger (9901–9950) ---

  MISSING_REQUIRED_ATTRIBUTE(
      9901,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Missing required directory attribute",
      "Missing required attribute name={}"),

  UNKNOWN_NAMING_ERROR(
      9902,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unknown naming error",
      "Unknown naming error detail={}"),

  PARSE_JNDI_PROVIDER_URL_ERROR(
      9903,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Failed to parse JNDI provider URL",
      "Parse JNDI provider URL error detail={}");

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
