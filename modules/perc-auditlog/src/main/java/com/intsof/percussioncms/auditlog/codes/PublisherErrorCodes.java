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
 * Publisher service error catalog bridging legacy {@code
 * com.percussion.services.publisher.IPSPublisherServiceErrors} package-local ints (10–24).
 *
 * <p>Every constant sets {@link #isAuditable()} explicitly: publishing job and item publish
 * failures dual-write as {@link AuditEventType#CONTENT_PUBLISH}; lookup / filter / repository
 * operational noise does not.
 *
 * <p><strong>Flat registry collision:</strong> package-local ints {@code 10–24} already belong to
 * {@link WorkflowErrorCodes} (10), {@link JobErrorCodes} (11), and {@link AssemblyErrorCodes}
 * (12–24) in the flat map. This catalog therefore does <strong>not</strong> flat-register any
 * ints. Prefer this enum directly for dual-write until a composite-key registry exists. Module
 * code is {@link AuditModule#PUB}.
 */
public enum PublisherErrorCodes implements SystemErrorCode {

  LIST_MISSING(
      10,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Content list not found: {}",
      "Content list missing name={}"),

  BAD_QUERY(
      11,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid query: {}",
      "Invalid publisher query query={}"),

  REPOSITORY(
      12,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Repository access error: {}",
      "Publisher repository error detail={}"),

  SITE_LOAD(
      13,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Site loading failed: {}",
      "Publisher site load failed siteGuid={}"),

  MISSING_EXTENSION(
      14,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Extension not found: {}",
      "Publisher missing extension name={} context={} iface={}"),

  EXTENSION_LOOKUP(
      15,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Extension lookup failed",
      "Publisher extension lookup failed"),

  ROW_RETRIEVAL(
      16,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Query row retrieval failed",
      "Publisher row retrieval failed"),

  DB(
      17,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Database operation failed",
      "Publisher database error"),

  FILTER_FAILED(
      18,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Content filter failed: {}",
      "Publisher filter failed filter={}"),

  JOB_FAILED(
      19,
      true,
      AuditEventType.CONTENT_PUBLISH,
      AuditOutcome.FAILURE,
      "Publishing job failed: {}",
      "Publishing job failed jobId={} detail={}"),

  ITEM_PUBLISH_FAILED(
      20,
      true,
      AuditEventType.CONTENT_PUBLISH,
      AuditOutcome.FAILURE,
      "Content item publishing failed",
      "Content item publishing failed contentId={} siteId={} detail={}"),

  RUNTIME_ERROR(
      21,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Runtime exception during publishing: {}",
      "Publisher runtime error detail={}"),

  SITE_MISSING(
      22,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Site not found",
      "Publisher site missing"),

  CONTEXT_MISSING(
      23,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Publishing context missing",
      "Publisher context missing"),

  UNEXPECTED(
      24,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unexpected error during publishing: {}",
      "Publisher unexpected error detail={}");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  PublisherErrorCodes(
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
    return AuditModule.PUB;
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
