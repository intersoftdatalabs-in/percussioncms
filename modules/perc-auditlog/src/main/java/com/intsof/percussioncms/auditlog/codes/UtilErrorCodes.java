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
 * Util error catalog bridging legacy {@code com.percussion.util.IPSUtilErrors} ints (10001–10203:
 * Base64 encode/decode, collection class load, purgable temp dir, HTTP receive/post).
 *
 * <p>Every constant sets {@link #isAuditable()} to {@code false}: util encode / I/O failures are
 * operational noise, not security dual-write events.
 *
 * <p>All ints are globally unique (distinct from {@link ServletErrorCodes} 10151+) and fully
 * flat-registered in {@link LegacyErrorCodeRegistry}. Module code is {@link AuditModule#SYS}.
 */
public enum UtilErrorCodes implements SystemErrorCode {

  BASE64_ENCODING_EXCEPTION(
      10001,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Base64 encoding exception",
      "Base64 encoding exception input={} detail={}"),

  BASE64_DECODING_EXCEPTION(
      10002,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Base64 decoding exception",
      "Base64 decoding exception input={} detail={}"),

  COLLECTION_CLASS_NOT_FOUND(
      10051,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Collection class not found",
      "Collection class not found class={}"),

  PURGABLE_TEMP_DIR_IS_FILE(
      10101,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Purgable temp dir is a file",
      "Purgable temp dir is a file"),

  RECEIVE_DATA_ERROR(
      10202,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Receive data error",
      "Receive data error received={} expected={}"),

  POST_DATA_ERROR(
      10203,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Post data error",
      "Post data error code={} message={}");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  UtilErrorCodes(
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
    for (UtilErrorCodes code : values()) {
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
