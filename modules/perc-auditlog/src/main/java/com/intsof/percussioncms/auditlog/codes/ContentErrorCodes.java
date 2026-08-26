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
 * Content error / audit catalog for Phase 2a high-level lifecycle events and Phase 2b legacy {@code
 * IPSContentErrors} bridges.
 *
 * <p><strong>Numbering (aligned with Phase 2a call-site migrate):</strong>
 *
 * <ul>
 *   <li>{@code 2001–2006} — intentional content lifecycle audit events (create/update/delete/…)
 *   <li>{@code 17001–17010} — legacy {@code com.percussion.content.IPSContentErrors} conversion /
 *       extraction ints (globally unique per {@code IPSGlobalErrorsMap})
 * </ul>
 *
 * <p>{@link #numericCode()} for the legacy conversion range preserves historical ints so exception
 * constructors and bundles stay stable. Every constant sets {@link #isAuditable()} explicitly:
 * lifecycle events dual-write; conversion/config noise does not.
 *
 * <p>Package-local {@link #MISSING_KEYWORD} ({@code
 * com.percussion.services.content.IPSContentErrors.MISSING_KEYWORD} = {@code 1}) is
 * <strong>not</strong> registered in the flat {@link LegacyErrorCodeRegistry} (would collide with
 * {@link WorkflowErrorCodes}). Prefer this enum directly for that leftover.
 */
public enum ContentErrorCodes implements SystemErrorCode {

  // --- Phase 2a high-level content lifecycle audit events (CONT-200x) ---

  CREATE(
      2001,
      true,
      AuditEventType.CONTENT_CREATE,
      AuditOutcome.SUCCESS,
      "Content item {} created",
      "Content create guid={} contentId={} path={}"),

  UPDATE(
      2002,
      true,
      AuditEventType.CONTENT_UPDATE,
      AuditOutcome.SUCCESS,
      "Content item {} updated",
      "Content update guid={} contentId={} path={}"),

  DELETE(
      2003,
      true,
      AuditEventType.CONTENT_DELETE,
      AuditOutcome.SUCCESS,
      "Content item {} deleted",
      "Content delete guid={} contentId={} path={}"),

  RECYCLE(
      2004,
      true,
      AuditEventType.CONTENT_RECYCLE,
      AuditOutcome.SUCCESS,
      "Content item {} recycled",
      "Content recycle guid={} contentId={} path={}"),

  PAGE_PUBLISH_SCHEDULE(
      2005,
      true,
      AuditEventType.CONTENT_PUBLISH,
      AuditOutcome.SUCCESS,
      "Page publish scheduled for {}",
      "Page publish schedule guid={} contentId={} path={}"),

  PAGE_REMOVAL_SCHEDULE(
      2006,
      true,
      AuditEventType.CONTENT_PUBLISH,
      AuditOutcome.SUCCESS,
      "Page removal scheduled for {}",
      "Page removal schedule guid={} contentId={} path={}"),

  // --- legacy com.percussion.content.IPSContentErrors (17001–17010) ---

  UNSUPPORTED_FILE_TYPE(
      17001,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unsupported file type for content conversion",
      "Unsupported file type for conversion"),

  CONTENT_CONVERSION_FAILED_NO_MESSAGE(
      17002,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Content conversion failed",
      "Content conversion failed errorCode={} fileType={}"),

  CONTENT_CONVERSION_FAILED_WITH_MESSAGE(
      17003,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Content conversion failed: {}",
      "Content conversion failed errorCode={} fileType={} message={}"),

  CONTENT_CONVERSION_UNEXPECTED_ERROR(
      17004,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Content conversion unexpected error",
      "Content conversion unexpected exceptionClass={} message={}"),

  INVALID_SEARCH_CONFIG_PARAM(
      17005,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid search configuration parameter",
      "Invalid search config param={} value={}"),

  CONTENT_CONVERSION_INCOMPLETE(
      17006,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Content conversion incomplete",
      "Content conversion incomplete"),

  UNSUPPORTED_MIMETYPE(
      17007,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unsupported MIME type for conversion",
      "Unsupported MIME type for conversion"),

  UNSUPPORTED_CONVERT_METHOD(
      17008,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unsupported content convert method",
      "Unsupported content convert method"),

  UNSUPPORTED_EXTRACTION_EXIT(
      17009,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unsupported text extraction exit",
      "Unsupported text extraction exit"),

  UNSUPPORTED_CONVERT_CONSTRUCTOR(
      17010,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unsupported content converter constructor",
      "Unsupported content converter constructor"),

  /**
   * Package-local {@code com.percussion.services.content.IPSContentErrors.MISSING_KEYWORD}. Not
   * flat-registered (collides with {@link WorkflowErrorCodes#WORKFLOW_NOT_FOUND}).
   */
  MISSING_KEYWORD(
      1,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Missing keyword: {}",
      "Missing keyword id={}");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  ContentErrorCodes(
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
   * Register (or re-register) globally unique constants in {@link LegacyErrorCodeRegistry}. Skips
   * package-local {@link #MISSING_KEYWORD} ({@code 1}) that collides with {@link
   * WorkflowErrorCodes}. Safe to call repeatedly.
   */
  public static void ensureRegistered() {
    for (ContentErrorCodes code : values()) {
      if (code.numericCode == 1) {
        continue;
      }
      LegacyErrorCodeRegistry.register(code.numericCode(), code);
    }
  }

  @Override
  public AuditModule module() {
    return AuditModule.CONT;
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
