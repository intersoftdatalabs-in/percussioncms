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
 * WebDAV error catalog bridging legacy {@code com.percussion.webdav.error.IPSWebdavErrors} ints
 * (70001–70125: XML config, protocol methods, locks, content-type mapping).
 *
 * <p>Every constant sets {@link #isAuditable()} to {@code false}: WebDAV protocol / config failures
 * are operational noise. Authentication dual-write remains on {@link SecurityErrorCodes}.
 *
 * <p>All ints are globally unique and fully flat-registered in {@link LegacyErrorCodeRegistry} so
 * bare legacy ints resolve as non-auditable (safe skip). Module code is {@link AuditModule#SYS}.
 */
public enum WebdavErrorCodes implements SystemErrorCode {

  XML_ATTRIBUTE_MUST_BE_SPECIFIED(
      70001,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Xml attribute must be specified",
      "Xml attribute must be specified detail={}"),

  XML_INVALID_FORMAT(
      70002,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Xml invalid format",
      "Xml invalid format detail={}"),

  XML_ELEMENT_CANNOT_BE_EMPTY(
      70003,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Xml element cannot be empty",
      "Xml element cannot be empty detail={}"),

  XML_FAILED_CREATE_DOC_FROM_CONTENT(
      70004,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Xml failed create doc from content",
      "Xml failed create doc from content detail={}"),

  UNSUPPORTED_METHOD(
      70101,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unsupported method",
      "Unsupported method detail={}"),

  MIMETYPES_REQUIRED(
      70102,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Mimetypes required",
      "Mimetypes required detail={}"),

  CANNOT_HAVE_DUPLICATE_PROPERTIES(
      70103,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Cannot have duplicate properties",
      "Cannot have duplicate properties detail={}"),

  MISSING_REQUIRED_PROPERTY(
      70104,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Missing required property",
      "Missing required property detail={}"),

  CAN_ONLY_HAVE_ONE_DEFAULT_CONTENTTYPE(
      70105,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Can only have one default contenttype",
      "Can only have one default contenttype detail={}"),

  IO_EXCEPTION_OCCURED(
      70106,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Io exception occured",
      "Io exception occured detail={}"),

  SAX_EXCEPTION_OCCURED(
      70107,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Sax exception occured",
      "Sax exception occured detail={}"),

  FILE_DOES_NOT_EXIST(
      70108,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "File does not exist",
      "File does not exist detail={}"),

  PARSER_CONFIG_ERROR(
      70109,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Parser config error",
      "Parser config error detail={}"),

  DUPLICATE_CONTENTTYPE_NAMES(
      70110,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Duplicate contenttype names",
      "Duplicate contenttype names detail={}"),

  RESOURCE_NOT_FIND(
      70111,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Resource not find",
      "Resource not find detail={}"),

  FORBIDDEN_GET_FOLDER(
      70112,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Forbidden get folder",
      "Forbidden get folder detail={}"),

  HEADER_MISSING(
      70113,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Header missing",
      "Header missing detail={}"),

  FORBIDDEN_SRC_TARGET_SAME(
      70114,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Forbidden src target same",
      "Forbidden src target same detail={}"),

  METHOD_FAIL_CANNOT_OVERWRITE(
      70115,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Method fail cannot overwrite",
      "Method fail cannot overwrite detail={}"),

  ITEMFIELD_NOT_EXIST(
      70116,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Itemfield not exist",
      "Itemfield not exist detail={}"),

  LOCKSCOPE_NOT_ALLOWED(
      70117,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Lockscope not allowed",
      "Lockscope not allowed detail={}"),

  LOCKTYPE_NOT_ALLOWED(
      70118,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Locktype not allowed",
      "Locktype not allowed detail={}"),

  FIELDNAME_CANNOT_BE_EMPTY_OR_MISSING(
      70119,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Fieldname cannot be empty or missing",
      "Fieldname cannot be empty or missing detail={}"),

  CONTENTTYPE_NOT_CONFIGURED(
      70120,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Contenttype not configured",
      "Contenttype not configured detail={}"),

  UNKNOWN_BODY_IN_MKCOL_REQ(
      70121,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unknown body in mkcol req",
      "Unknown body in mkcol req detail={}"),

  UNKNOWN_URL_FROM_HEADER(
      70122,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unknown url from header",
      "Unknown url from header detail={}"),

  MALFORMED_URL_FROM_HEADER(
      70123,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Malformed url from header",
      "Malformed url from header detail={}"),

  NO_PUBLIC_AUTO_TRANSITION(
      70124,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "No public auto transition",
      "No public auto transition detail={}"),

  NO_QE_AUTO_TRANSITION(
      70125,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "No qe auto transition",
      "No qe auto transition detail={}");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  WebdavErrorCodes(
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
    for (WebdavErrorCodes code : values()) {
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
