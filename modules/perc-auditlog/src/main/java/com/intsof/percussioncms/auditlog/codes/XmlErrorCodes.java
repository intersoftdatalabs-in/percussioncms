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
 * XML error catalog bridging two legacy {@code IPSXmlErrors} sources:
 *
 * <ul>
 *   <li>{@code com.percussion.utils.xml.IPSXmlErrors} package-local ints {@code 1–6} (element /
 *       attribute restore noise)
 *   <li>{@code com.percussion.xml.IPSXmlErrors} ints {@code 6001–6028} (raw dump, processing, DTD)
 * </ul>
 *
 * <p>Every constant sets {@link #isAuditable()} to {@code false}: XML parse / DTD / restore
 * failures are operational noise, not security dual-write events.
 *
 * <p><strong>Flat registry collision:</strong> package-local ints {@code 1–6} already belong to
 * {@link WorkflowErrorCodes}. Only the unique system range ({@code 6001+}) is flat-registered.
 * Prefer this enum directly for utils package-local codes. Module code is {@link
 * AuditModule#SYS}.
 */
public enum XmlErrorCodes implements SystemErrorCode {

  // --- utils package-local (com.percussion.utils.xml.IPSXmlErrors) — not flat-registered ---

  XML_ELEMENT_MISSING(
      1,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "XML element missing",
      "XML element missing tag={}"),

  XML_ELEMENT_INVALID_VALUE(
      2,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "XML element invalid value",
      "XML element invalid value tag={} value={}"),

  XML_TWO_ROOT_ELEMENTS(
      3,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "XML two root elements",
      "XML two root elements first={} second={}"),

  XML_ELEMENT_INVALID_ATTR(
      4,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "XML element invalid attribute",
      "XML element invalid attribute tag={} attr={} value={}"),

  XML_ELEMENT_ATTR_INVALID_VAL(
      5,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "XML element attribute invalid value",
      "XML element attribute invalid value tag={} attr={} attrValue={} elementValue={}"),

  XML_RESTORE_ERROR(
      6,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "XML restore error",
      "XML restore error class={} detail={} xml={}"),

  // --- system (com.percussion.xml.IPSXmlErrors) — flat-registered ---

  RAW_XML_DUMP(
      6001,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Raw XML dump",
      "Raw XML dump data={}"),

  XML_PROCESSING_ERROR(
      6002,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "XML processing error",
      "XML processing error session={}"),

  DTD_IO_ERROR(
      6025,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "DTD IO error",
      "DTD IO error detail={}"),

  DTD_ROOTNOTFOUND_ERROR(
      6026,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "DTD root not found",
      "DTD root not found root={}"),

  DTD_MULTIPLE_OCCURRENCE_NOTSUPPORTED_ERROR(
      6027,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "DTD multiple occurrence not supported",
      "DTD multiple occurrence not supported first={} second={}"),

  DTD_ELEMENT_NOTFOUND_ERROR(
      6028,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "DTD element not found",
      "DTD element not found element={}");

  /** Lowest flat-registered system XML int ({@code com.percussion.xml.IPSXmlErrors}). */
  private static final int SYSTEM_XML_RANGE_START = 6001;

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  XmlErrorCodes(
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
   * Register unique system XML ints ({@code 6001+}) only. Package-local {@code 1–6} are skipped so
   * {@link WorkflowErrorCodes} keeps ownership of bare low ints. Safe to call repeatedly.
   */
  public static void ensureRegistered() {
    for (XmlErrorCodes code : values()) {
      if (code.numericCode() >= SYSTEM_XML_RANGE_START) {
        LegacyErrorCodeRegistry.register(code.numericCode(), code);
      }
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
