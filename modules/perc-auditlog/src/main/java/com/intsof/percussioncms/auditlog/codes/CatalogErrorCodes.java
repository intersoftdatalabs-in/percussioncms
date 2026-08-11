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
 * Catalog error catalog bridging:
 *
 * <ul>
 *   <li>{@code com.percussion.services.catalog.IPSCatalogErrors} package-local ints (1–6)
 *   <li>{@code com.percussion.design.catalog.IPSCatalogErrors} design-client/server ints
 *       (4101–4311)
 * </ul>
 *
 * <p>Every constant sets {@link #isAuditable()} to {@code false}: catalog protocol and repository
 * noise is not a security dual-write event.
 *
 * <p><strong>Flat registry collision:</strong> service package-local ints {@code 1–6} already
 * belong to {@link WorkflowErrorCodes}. Only the design-catalog range ({@code 4101–4311}) is
 * flat-registered. Prefer this enum for service ints {@code 1–6}. Module code is {@link
 * AuditModule#SYS}.
 */
public enum CatalogErrorCodes implements SystemErrorCode {

  SUMMARY_ERROR(
      1,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Catalog summary enumeration error",
      "Catalog summary error"),

  UNKNOWN_TYPE(
      2,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unknown catalog type: {}",
      "Catalog unknown type name={}"),

  REPOSITORY(
      3,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Catalog repository error for object {}",
      "Catalog repository error objectId={}"),

  XML(
      4,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Catalog XML error",
      "Catalog XML error source={}"),

  IO(
      5,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Catalog I/O error for object {}",
      "Catalog IO error objectId={}"),

  TOXML(
      6,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Catalog serialize-to-XML error",
      "Catalog toXml error"),

  REQD_PROP_NOT_SPECIFIED(
      4101,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Required catalog property not specified: {}",
      "Catalog required property not specified prop={}"),

  REQ_CATEGORY_INVALID(
      4102,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Catalog request category invalid",
      "Catalog request category invalid expected={} specified={}"),

  REQ_TYPE_INVALID(
      4103,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Catalog request type invalid",
      "Catalog request type invalid expected={} specified={}"),

  REQ_HANDLER_EXCEPTION(
      4104,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Catalog request handler exception",
      "Catalog request handler exception category={} type={} detail={}"),

  CONN_OBJ_NULL(
      4105,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Cataloger connection object is null",
      "Cataloger connection object null"),

  REQ_DOC_MISSING(
      4301,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Catalog request document missing",
      "Catalog request document missing category={} type={} dtd={}"),

  REQ_DOC_MISSING_GENERIC(
      4302,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Catalog request document missing",
      "Catalog request document missing generic"),

  REQ_DOC_ROOT_MISSING_GENERIC(
      4303,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Catalog request document root missing",
      "Catalog request document root missing generic"),

  REQ_DOC_INVALID_TYPE(
      4304,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Catalog request document type invalid",
      "Catalog request document invalid type expected={} specified={}"),

  NO_REQ_HANDLER_FOUND(
      4305,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "No catalog request handler found",
      "Catalog no request handler category={} type={}"),

  PROPS_LOAD_EXCEPTION(
      4306,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Catalog properties load exception",
      "Catalog props load exception category={} type={} detail={}"),

  EXIT_HANDLER_CLASS_NOT_FOUND(
      4307,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Catalog exit handler class not found: {}",
      "Catalog exit handler class not found class={}"),

  IPSEXITHANDLER_NOT_IMPLEMENTED(
      4308,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exit handler interface not implemented: {}",
      "Catalog IPSExtensionHandler not implemented class={}"),

  EXIT_HANDLER_CLASS_LOAD_EXCEPTION(
      4309,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exit handler class load exception",
      "Catalog exit handler class load exception class={} detail={}"),

  CATALOG_ERROR(
      4310,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Catalog error",
      "Catalog error session={} category={} type={}"),

  CATALOG_EXCEPTION(
      4311,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Catalog exception: {}",
      "Catalog exception detail={}");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  CatalogErrorCodes(
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
   * Register non-colliding design-catalog ints in {@link LegacyErrorCodeRegistry}. Safe to call
   * repeatedly. Skips package-local service ints {@code 1–6} that collide with {@link
   * WorkflowErrorCodes}.
   */
  public static void ensureRegistered() {
    for (CatalogErrorCodes code : values()) {
      if (code.numericCode <= 6) {
        // Preserve WorkflowErrorCodes ownership of bare ints 1–6.
        continue;
      }
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
