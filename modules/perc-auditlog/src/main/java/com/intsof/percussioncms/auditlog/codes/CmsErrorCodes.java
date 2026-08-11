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
 * CMS error catalog bridging residual legacy {@code com.percussion.cms.IPSCmsErrors} ints
 * (13001–14000 general / objectstore / server-side OS handlers).
 *
 * <p>Path / folder / item permission codes remain owned by {@link PathItemErrorCodes} (registered
 * first; this catalog skips those ints). Legacy {@code SQL_EXCEPTION_WRAPPER = 1002} collides with
 * {@link ServerErrorCodes#SQL_PROBLEM} and is not re-registered here — prefer Server for flat
 * dual-write of 1002.
 *
 * <p>Every constant sets {@link #isAuditable()} to {@code false}: residual CMS codes are operational
 * objectstore / AA / relationship noise. Security dual-write for folder/community denials stays on
 * {@link PathItemErrorCodes}. Module code is {@link AuditModule#CONT}.
 *
 * <p>All residual ints in the 13001–14000 range are globally unique and fully flat-registered.
 */
public enum CmsErrorCodes implements SystemErrorCode {

  CORRUPT_DATABASE_ENTRY(
      13001,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Corrupt database entry",
      "Corrupt database entry detail={}"),
  INVALID_CONTENT_TYPE_ID(
      13002,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid content type id",
      "Invalid content type id detail={}"),
  ERROR_SEND_DATA(
      13003,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Error send data",
      "Error send data detail={}"),
  RECEIVED_UNKNOWN_DATA(
      13004,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Received unknown data",
      "Received unknown data detail={}"),
  INVALID_CONTENT_TYPE(
      13102,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid content type",
      "Invalid content type detail={}"),
  CONTENT_TYPE_CANNOT_BE_OPENED(
      13103,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Content type cannot be opened",
      "Content type cannot be opened detail={}"),
  DATA_EXTRACTION_ERROR_NULL_DATAPIPE(
      13106,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Data extraction error null datapipe",
      "Data extraction error null datapipe detail={}"),
  PSFIELDVALUE_TO_STRING_ERROR(
      13105,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Psfieldvalue to string error",
      "Psfieldvalue to string error detail={}"),
  REQUIRED_DOCUMENT_MISSING_ERROR(
      13107,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Required document missing error",
      "Required document missing error detail={}"),
  MALFORMED_XML_DOCUMENT_UKNOWN_NODE_TYPE(
      13108,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Malformed xml document uknown node type",
      "Malformed xml document uknown node type detail={}"),
  CMS_INTERNAL_REQUEST_ERROR(
      13109,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Cms internal request error",
      "Cms internal request error detail={}"),
  KEY_PARTS_NOT_MATCH(
      13110,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Key parts not match",
      "Key parts not match detail={}"),
  KEY_NOT_ASSIGNED(
      13111,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Key not assigned",
      "Key not assigned detail={}"),
  UNSUPPORTED_COMPONENT_TYPE(
      13112,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unsupported component type",
      "Unsupported component type detail={}"),
  MISSING_PROPERTY(
      13113,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Missing property",
      "Missing property detail={}"),
  SERIALIZED_COMPONENTS_WRONG_XML_DOC(
      13114,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Serialized components wrong xml doc",
      "Serialized components wrong xml doc detail={}"),
  EMPTY_XML_DOCUMENT(
      13115,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Empty xml document",
      "Empty xml document detail={}"),
  REQUIRED_RESOURCE_MISSING(
      13116,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Required resource missing",
      "Required resource missing detail={}"),
  PROCESSOR_CONFIG_MISSING(
      13117,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Processor config missing",
      "Processor config missing detail={}"),
  XML_PARSING_ERROR(
      13118,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Xml parsing error",
      "Xml parsing error detail={}"),
  DUPLICATE_PROCESSOR_PROPERTY(
      13119,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Duplicate processor property",
      "Duplicate processor property detail={}"),
  DUPLICATE_PROCESSOR_ENTRY(
      13120,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Duplicate processor entry",
      "Duplicate processor entry detail={}"),
  NO_PROCESSOR_ENTRY(
      13121,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "No processor entry",
      "No processor entry detail={}"),
  PROCESSOR_NO_SUCH_METHOD(
      13122,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Processor no such method",
      "Processor no such method detail={}"),
  PROCESSOR_BAD_HERITAGE(
      13123,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Processor bad heritage",
      "Processor bad heritage detail={}"),
  PROCESSOR_INSTANTIATION_ERROR(
      13124,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Processor instantiation error",
      "Processor instantiation error detail={}"),
  COMPONENT_INSTANTIATION_ERROR(
      13125,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Component instantiation error",
      "Component instantiation error detail={}"),
  KEY_MISMATCH(
      13126,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Key mismatch",
      "Key mismatch detail={}"),
  TOO_MANY_FOREIGN_KEY_PARTS(
      13127,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Too many foreign key parts",
      "Too many foreign key parts detail={}"),
  TOO_FEW_FOREIGN_KEY_PARTS(
      13128,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Too few foreign key parts",
      "Too few foreign key parts detail={}"),
  LIST_ENTRY_INSTANTIATION_ERROR(
      13129,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "List entry instantiation error",
      "List entry instantiation error detail={}"),
  INVALID_ENTRY_CLASSNAME(
      13130,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid entry classname",
      "Invalid entry classname detail={}"),
  MISSING_LOOKUP_KEY(
      13131,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Missing lookup key",
      "Missing lookup key detail={}"),
  MISMATCH_BETWEEN_KEY_AND_DATA(
      13132,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Mismatch between key and data",
      "Mismatch between key and data detail={}"),
  COMM_ERROR_WITH_SERVER(
      13133,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Comm error with server",
      "Comm error with server detail={}"),
  SAX_PROCESSING_EXCEPTION(
      13134,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Sax processing exception",
      "Sax processing exception detail={}"),
  MISSING_HTML_PARAMETER(
      13135,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Missing html parameter",
      "Missing html parameter detail={}"),
  FAIL_GET_COMPONENT_SUMMARIES(
      13136,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Fail get component summaries",
      "Fail get component summaries detail={}"),
  CONTENTTYPE_DEFINITION_NOT_FOUND(
      13140,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Contenttype definition not found",
      "Contenttype definition not found detail={}"),
  UNKNOWN_RELATED_TYPE(
      13146,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unknown related type",
      "Unknown related type detail={}"),
  INVALID_RELATED_TYPE(
      13147,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid related type",
      "Invalid related type detail={}"),
  FOLDERID_REF_BY_CROSS_SITE_LINK(
      13144,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Folderid ref by cross site link",
      "Folderid ref by cross site link detail={}"),
  SITEID_REF_BY_CROSS_SITE_LINK(
      13145,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Siteid ref by cross site link",
      "Siteid ref by cross site link detail={}"),
  INVALID_CHILD_TYPE(
      13148,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid child type",
      "Invalid child type detail={}"),
  UNKNOWN_RELATIONSHIP_TYPE(
      13201,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unknown relationship type",
      "Unknown relationship type detail={}"),
  UNEXPECTED_KEY_TYPE(
      13202,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unexpected key type",
      "Unexpected key type detail={}"),
  PERSISTED_KEY_EXPECTED(
      13203,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Persisted key expected",
      "Persisted key expected detail={}"),
  INVALID_INSERT_RELATIONSHIP_TYPE(
      13204,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid insert relationship type",
      "Invalid insert relationship type detail={}"),
  ID_GENERATOR_FAILED(
      13205,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Id generator failed",
      "Id generator failed detail={}"),
  UNEXPECTED_ERROR(
      13206,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unexpected error",
      "Unexpected error detail={}"),
  INVALID_AA_RELATIONSHIP_TYPE(
      13207,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid aa relationship type",
      "Invalid aa relationship type detail={}"),
  UNKNOWN_AA_COMMAND(
      13208,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unknown aa command",
      "Unknown aa command detail={}"),
  MISSING_AA_PARAMETER(
      13209,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Missing aa parameter",
      "Missing aa parameter detail={}"),
  PROCESSOR_CONFIG_IO_ERROR(
      13210,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Processor config io error",
      "Processor config io error detail={}"),
  UNDEFINED_DEFAULT_TRANSITION(
      13211,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Undefined default transition",
      "Undefined default transition detail={}"),
  FAIL_DELETE_NON_FOLDER(
      13213,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Fail delete non folder",
      "Fail delete non folder detail={}"),
  GET_SUMMARIES_ERROR(
      13214,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Get summaries error",
      "Get summaries error detail={}"),
  UNEXPECTED_CATALOG_ERROR(
      13215,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unexpected catalog error",
      "Unexpected catalog error detail={}"),
  INVALID_RELATIONSHIP_PROP_VALUE(
      13217,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid relationship prop value",
      "Invalid relationship prop value detail={}"),
  NO_ORIGINATING_RELATIONSHIP(
      13218,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "No originating relationship",
      "No originating relationship detail={}"),
  VALIDATION_ERROR(
      13219,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Validation error",
      "Validation error detail={}"),
  SEARCH_ERROR(
      13220,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Search error",
      "Search error detail={}"),
  MODIFY_ERROR_DUPLICATED_CHILDNAME(
      13221,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Modify error duplicated childname",
      "Modify error duplicated childname detail={}"),
  FAILED_GET_SUMMARY(
      13223,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Failed get summary",
      "Failed get summary detail={}"),
  RELATIONSHIP_EXISTENCE_CHECK_FAILED(
      13225,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Relationship existence check failed",
      "Relationship existence check failed detail={}"),
  NON_EXITING_OWNER(
      13226,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Non exiting owner",
      "Non exiting owner detail={}"),
  NON_EXITING_DEPENDENT(
      13227,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Non exiting dependent",
      "Non exiting dependent detail={}"),
  INVALID_AUTHTYPE(
      13229,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid authtype",
      "Invalid authtype detail={}"),
  INVALID_AA_RELATIONSHIP(
      13230,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid aa relationship",
      "Invalid aa relationship detail={}"),
  VARIANT_LOOKUP_FAILED(
      13231,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Variant lookup failed",
      "Variant lookup failed detail={}"),
  INVALID_AA_RELATIONSHIP_SLOT_VARIANT(
      13232,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid aa relationship slot variant",
      "Invalid aa relationship slot variant detail={}"),
  INVALID_CONTEXT_FOR_AA_PROXY(
      13233,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid context for aa proxy",
      "Invalid context for aa proxy detail={}"),
  ERROR_RECURSIVEASSEMBLY(
      13234,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Error recursiveassembly",
      "Error recursiveassembly detail={}"),
  FAILED_GET_REL_CONFIG_FROM_XML(
      13236,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Failed get rel config from xml",
      "Failed get rel config from xml detail={}"),
  UNKOWN_SYS_REL_CONFIG_ID(
      13238,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unkown sys rel config id",
      "Unkown sys rel config id detail={}"),
  FAILED_DELETE_REL_CONFIG_NAME(
      13240,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Failed delete rel config name",
      "Failed delete rel config name detail={}"),
  INVALID_REL_CONFIG_ID(
      13241,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid rel config id",
      "Invalid rel config id detail={}"),
  INVALID_REL_CONFIG_NAME(
      13242,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid rel config name",
      "Invalid rel config name detail={}"),
  ERROR_LOADING_SITES(
      13243,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Error loading sites",
      "Error loading sites detail={}"),
  CANNOT_MOVE_CHECKEDOUT_ITEMS(
      13244,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Cannot move checkedout items",
      "Cannot move checkedout items detail={}"),
  CANNOT_MOVE_PUBLIC_ITEMS(
      13245,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Cannot move public items",
      "Cannot move public items detail={}"),
  FORCE_MOVE_REMOVE_REQUIED(
      13246,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Force move remove requied",
      "Force move remove requied detail={}"),
  ERROR_SAVING_RELATIONSHIPS(
      13247,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Error saving relationships",
      "Error saving relationships detail={}"),
  LOAD_AA_RELATIONSHIP_FAILED(
      13248,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Load aa relationship failed",
      "Load aa relationship failed detail={}"),
  CROSSSITE_LINK_PROCESS_MULTI_ERROR(
      13249,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Crosssite link process multi error",
      "Crosssite link process multi error detail={}"),
  FAILED_GET_NAVON_CIRCULAR_AA_RELATIONSHIP(
      13250,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Failed get navon circular aa relationship",
      "Failed get navon circular aa relationship detail={}");


  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  CmsErrorCodes(
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
   * Register residual CMS ints in {@link LegacyErrorCodeRegistry}. Safe to call repeatedly.
   * Does not include PathItem-owned ints or SQL_EXCEPTION_WRAPPER (1002).
   */
  public static void ensureRegistered() {
    for (CmsErrorCodes code : values()) {
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
