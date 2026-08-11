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
 * Lucene search-engine error catalog bridging legacy {@code
 * com.percussion.search.lucene.IPSLuceneErrors} ints (16311–16456: index directory, indexing,
 * query).
 *
 * <p>Every constant sets {@link #isAuditable()} to {@code false}: Lucene index / query failures are
 * operational noise. Search-engine authentication dual-write remains on {@link
 * SearchErrorCodes#SEARCH_ENGINE_AUTHENTICATION_FAILED}.
 *
 * <p>All ints are globally unique and fully flat-registered in {@link LegacyErrorCodeRegistry}.
 * Module code is {@link AuditModule#SYS}.
 */
public enum LuceneErrorCodes implements SystemErrorCode {

  INDEX_DIR_PARAM_INVALID_MISSING(
      16311,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Index directory parameter invalid or missing",
      "Index directory parameter invalid or missing param={}"),

  INVALID_INDEX_DIRECTORY(
      16366,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid index directory",
      "Invalid index directory contentTypeId={} path={}"),

  INDEX_CURRUPTED_EXCEPTION_INDEXING(
      16402,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Corrupt index during indexing",
      "Corrupt index during indexing contentTypeId={}"),

  INDEX_IO_EXCEPTION_INDEXING(
      16403,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Index IO exception during indexing",
      "Index IO exception during indexing contentTypeId={}"),

  INDEX_OPTIMIZATION_ERROR(
      16404,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Index optimization error",
      "Index optimization error contentTypeIds={}"),

  INDEX_CURRUPTED_EXCEPTION_SEARCHING(
      16451,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Corrupt index during search",
      "Corrupt index during search contentTypeId={}"),

  INDEX_IO_EXCEPTION_SEARCHING(
      16452,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Index IO exception during search",
      "Index IO exception during search contentTypeId={}"),

  REPOSITORY_EXCEPTION(
      16453,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Lucene repository exception",
      "Lucene repository exception"),

  HITS_IOEXCEPTION(
      16454,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Hits IO exception",
      "Hits IO exception"),

  SEARCH_QUERY_PARSEEXCEPTION(
      16455,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Search query parse exception",
      "Search query parse exception"),

  SEARCH_QUERY_MULTISEARCHER(
      16456,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Search query multi-searcher error",
      "Search query multi-searcher error");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  LuceneErrorCodes(
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
    for (LuceneErrorCodes code : values()) {
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
