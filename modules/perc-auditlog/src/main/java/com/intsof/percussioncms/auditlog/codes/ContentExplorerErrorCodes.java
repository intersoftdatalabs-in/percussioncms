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
 * Content Explorer error catalog bridging legacy {@code
 * com.percussion.cx.error.IPSContentExplorerErrors} ints (20001–20011: options, actions, search,
 * catalog, wizard, path, site-def).
 *
 * <p>Every constant sets {@link #isAuditable()} to {@code false}: Content Explorer UI / client
 * operational failures are not security dual-write events.
 *
 * <p>All ints are globally unique and fully flat-registered in {@link LegacyErrorCodeRegistry}.
 * Module code is {@link AuditModule#SYS}.
 */
public enum ContentExplorerErrorCodes implements SystemErrorCode {

  GENERAL_ERROR(
      20001,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Content explorer general error",
      "Content explorer general error detail={}"),

  PSCLASS_INSTANTIATION_ERROR(
      20002,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Content explorer class instantiation error",
      "Content explorer class instantiation error class={} node={}"),

  MISC_PROCESSING_OPTIONS_ERROR(
      20003,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Content explorer options processing error",
      "Content explorer options processing error detail={}"),

  OPTIONS_LOAD_ERROR(
      20004,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Content explorer options load error",
      "Content explorer options load error detail={}"),

  OPTIONS_SAVE_ERROR(
      20005,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Content explorer options save error",
      "Content explorer options save error detail={}"),

  ACTION_GET_CHILDREN(
      20006,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Content explorer action get children error",
      "Content explorer action get children error detail={}"),

  SEARCH_ERROR(
      20007,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Content explorer search error",
      "Content explorer search error detail={}"),

  CATALOG_ERROR(
      20008,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Content explorer catalog error",
      "Content explorer catalog error detail={}"),

  WIZARD_VALIDATION_ERROR(
      20009,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Content explorer wizard validation error",
      "Content explorer wizard validation error detail={}"),

  INCOMPATIBLE_PATHS(
      20010,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Content explorer incompatible paths",
      "Content explorer incompatible paths root={} relative={}"),

  SITEDEF_UPDATE_FAILURES(
      20011,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Content explorer site definition update failures",
      "Content explorer site definition update failures detail={}");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  ContentExplorerErrorCodes(
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
    for (ContentExplorerErrorCodes code : values()) {
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
