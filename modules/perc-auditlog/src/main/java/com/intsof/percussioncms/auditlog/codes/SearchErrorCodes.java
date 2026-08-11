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
 * Search engine error catalog bridging legacy {@code com.percussion.search.IPSSearchErrors} ints
 * (16001–16054: general search engine, init/config). Lucene-specific ints live in {@link
 * LuceneErrorCodes} (16311+).
 *
 * <p>{@link #SEARCH_ENGINE_AUTHENTICATION_FAILED} dual-writes ({@link
 * AuditEventType#AUTH_FAILURE}); all other constants are operational noise ({@link
 * #isAuditable()} {@code false}).
 *
 * <p>All ints are globally unique and fully flat-registered in {@link LegacyErrorCodeRegistry}.
 * Module code is {@link AuditModule#SYS}.
 */
public enum SearchErrorCodes implements SystemErrorCode {

  SEARCH_ENGINE_UNIMPLEMENTED_OPERATION(
      16001,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Search engine unimplemented operation",
      "Search engine unimplemented operation"),

  SEARCH_ENGINE_OBJECT_NOT_FOUND(
      16002,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Search engine object not found",
      "Search engine object not found library={}"),

  SEARCH_ENGINE_NO_SEARCH_TERMS(
      16003,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Search engine no search terms",
      "Search engine no search terms"),

  SEARCH_ENGINE_FATAL_ERROR(
      16004,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Search engine fatal error",
      "Search engine fatal error"),

  SEARCH_ENGINE_BAD_PARAMETERS(
      16005,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Search engine bad parameters",
      "Search engine bad parameters"),

  SEARCH_ENGINE_WILDCARD_LIMIT(
      16006,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Search engine wildcard limit",
      "Search engine wildcard limit"),

  SEARCH_ENGINE_UNEXPECTED_ERROR(
      16007,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Search engine unexpected error",
      "Search engine unexpected error code={} system={}"),

  SEARCH_ENGINE_QUERY_PARSE_ERROR(
      16008,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Search engine query parse error",
      "Search engine query parse error engine={} detail={}"),

  ADMIN_HANDLER_LOCKED(
      16009,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Search admin handler locked",
      "Search admin handler locked"),

  UNRELEASED_OBJECTS(
      16010,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Search unreleased objects",
      "Search unreleased objects admin={} query={} indexer={}"),

  SEARCH_ENGINE_REQUIRED(
      16011,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Search engine required",
      "Search engine required"),

  INVALID_INDEX_CONTENTTYPE(
      16012,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid index content type",
      "Invalid index content type contentId={} contentTypeId={}"),

  SEARCH_ENGINE_FAILED_INIT(
      16051,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Search engine failed init",
      "Search engine failed init class={} detail={}"),

  SEARCH_ENGINE_AUTHENTICATION_FAILED(
      16052,
      true,
      AuditEventType.AUTH_FAILURE,
      AuditOutcome.FAILURE,
      "Search engine authentication failed",
      "Search engine authentication failed"),

  HTML_SEARCH_MISSING_PARAMETER(
      16053,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Html search missing parameter",
      "Html search missing parameter name={} type={}"),

  USE_GET_INSTANCE(
      16054,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Use getInstance instead of new",
      "Use getInstance instead of new");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  SearchErrorCodes(
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
    for (SearchErrorCodes code : values()) {
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
