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
 * Error catalog bridging legacy {@code com.percussion.data.IPSDataErrors} ints
 * (5201–6046: back-end data processing + XML/general data).
 *
 * <p>{@link #numericCode()} preserves historical ints. Every constant sets {@link #isAuditable()}
 * explicitly. Data pipeline / result-set / XML processing failures are operational noise (all non-auditable).
 *
 * <p>All ints in this catalog are flat-registered in {@link LegacyErrorCodeRegistry} (no
 * package-local collision with already-bootstrapped catalogs). Module code is
 * {@link AuditModule#SYS}.
 */
public enum DataErrorCodes implements SystemErrorCode {

  QUERY_PROCESSING_ERROR(
      5201,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Query processing error",
      "Query processing error detail={}"),
  UPDATE_PROCESSING_ERROR(
      5202,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Update processing error",
      "Update processing error detail={}"),
  COLMAPPER_BE_COL_NOT_STMTCOL(
      5203,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Colmapper be col not stmtcol",
      "Colmapper be col not stmtcol detail={}"),
  REQ_LINK_BE_VALS_INVALID(
      5204,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Req link be vals invalid",
      "Req link be vals invalid detail={}"),
  REPLACEMENT_VALUE_REQD(
      5205,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Replacement value reqd",
      "Replacement value reqd detail={}"),
  REQ_LINK_SOURCE_DS_NULL(
      5206,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Req link source ds null",
      "Req link source ds null detail={}"),
  REQ_LINK_TARGET_DS_NULL(
      5207,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Req link target ds null",
      "Req link target ds null detail={}"),
  BE_COL_EXTR_INVALID_COL(
      5209,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Be col extr invalid col",
      "Be col extr invalid col detail={}"),
  BE_COL_EXTR_EXCEPTION(
      5210,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Be col extr exception",
      "Be col extr exception detail={}"),
  BE_COL_GET_INDEX_EXCEPTION(
      5211,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Be col get index exception",
      "Be col get index exception detail={}"),
  REQ_LINK_TYPE_UNSUPPORTED(
      5212,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Req link type unsupported",
      "Req link type unsupported detail={}"),
  QUERY_LINK_TARGET_NOT_QUERY(
      5213,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Query link target not query",
      "Query link target not query detail={}"),
  QUERY_LINK_TARGET_SELECTOR_REQD(
      5214,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Query link target selector reqd",
      "Query link target selector reqd detail={}"),
  UPDATE_LINK_TARGET_NOT_UPDATE(
      5215,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Update link target not update",
      "Update link target not update detail={}"),
  UPDATE_LINK_TARGET_SYNC_REQD(
      5216,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Update link target sync reqd",
      "Update link target sync reqd detail={}"),
  UPDATE_LINK_KEY_NOT_MAPPED(
      5217,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Update link key not mapped",
      "Update link key not mapped detail={}"),
  INDEX_JOINER_RESULT_SET_REQD(
      5218,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Index joiner result set reqd",
      "Index joiner result set reqd detail={}"),
  INDEX_JOINER_LCOL_NOT_FOUND(
      5219,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Index joiner lcol not found",
      "Index joiner lcol not found detail={}"),
  SORTED_JOINER_2_RESULT_SETS_REQD(
      5220,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Sorted joiner 2 result sets reqd",
      "Sorted joiner 2 result sets reqd detail={}"),
  SORTED_JOINER_LCOL_NOT_FOUND(
      5221,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Sorted joiner lcol not found",
      "Sorted joiner lcol not found detail={}"),
  SORTED_JOINER_RCOL_NOT_FOUND(
      5222,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Sorted joiner rcol not found",
      "Sorted joiner rcol not found detail={}"),
  SORTED_JOINER_COL_COUNT_MISMATCH(
      5223,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Sorted joiner col count mismatch",
      "Sorted joiner col count mismatch detail={}"),
  JOINED_ROW_BUF_RCOL_COUNT_MISMATCH(
      5224,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Joined row buf rcol count mismatch",
      "Joined row buf rcol count mismatch detail={}"),
  CACHER_LOAD_XML_EXCEPTION(
      5225,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Cacher load xml exception",
      "Cacher load xml exception detail={}"),
  CACHER_STORE_XML_EXCEPTION(
      5226,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Cacher store xml exception",
      "Cacher store xml exception detail={}"),
  CACHER_LOAD_RESPAGE_EXCEPTION(
      5227,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Cacher load respage exception",
      "Cacher load respage exception detail={}"),
  CACHER_STORE_RESPAGE_EXCEPTION(
      5228,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Cacher store respage exception",
      "Cacher store respage exception detail={}"),
  CACHER_FULL(
      5229,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Cacher full",
      "Cacher full detail={}"),
  CACHER_FILE_REMOVE_EXCEPTION(
      5230,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Cacher file remove exception",
      "Cacher file remove exception detail={}"),
  EXEC_PLAN_LOG_DTD_OCCURS(
      5231,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exec plan log dtd occurs",
      "Exec plan log dtd occurs detail={}"),
  EXEC_PLAN_LOG_COLLAPSED_XML_FIELD(
      5232,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exec plan log collapsed xml field",
      "Exec plan log collapsed xml field detail={}"),
  CACHE_FILE_SEND_EXCEPTION(
      5233,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Cache file send exception",
      "Cache file send exception detail={}"),
  EXEC_PLAN_LOG_UPDATE_XML_WALKER_ROOT(
      5234,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exec plan log update xml walker root",
      "Exec plan log update xml walker root detail={}"),
  EXEC_PLAN_LOG_UPDATE_XML_STMT_WALKER(
      5235,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exec plan log update xml stmt walker",
      "Exec plan log update xml stmt walker detail={}"),
  EXEC_PLAN_LOG_REBASED_STMT_WALKER(
      5236,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exec plan log rebased stmt walker",
      "Exec plan log rebased stmt walker detail={}"),
  UDF_HANDLER_NOT_LOADED(
      5237,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Udf handler not loaded",
      "Udf handler not loaded detail={}"),
  EXEC_PLAN_IGNORE_NO_UPDCOL_UPDATE(
      5238,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exec plan ignore no updcol update",
      "Exec plan ignore no updcol update detail={}"),
  EXEC_PLAN_LOG_SQL_PLAN(
      5239,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exec plan log sql plan",
      "Exec plan log sql plan detail={}"),
  EXEC_PLAN_NO_INDEX_LOOKUP_FULL_OUTER(
      5240,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exec plan no index lookup full outer",
      "Exec plan no index lookup full outer detail={}"),
  EXEC_PLAN_NO_INDEX_LOOKUP_RIGHT_OUTER(
      5241,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exec plan no index lookup right outer",
      "Exec plan no index lookup right outer detail={}"),
  CANNOT_LOAD_TABLE_META(
      5242,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Cannot load table meta",
      "Cannot load table meta detail={}"),
  CANNOT_LOAD_INDEX_META(
      5243,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Cannot load index meta",
      "Cannot load index meta detail={}"),
  EXEC_PLAN_NO_INDEX_LOOKUP_INDICES(
      5244,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exec plan no index lookup indices",
      "Exec plan no index lookup indices detail={}"),
  EXEC_PLAN_JOIN_CARDINALITY_NOT_FOUND(
      5245,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exec plan join cardinality not found",
      "Exec plan join cardinality not found detail={}"),
  EXEC_PLAN_LOG_JOIN_CARDINALITY(
      5246,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exec plan log join cardinality",
      "Exec plan log join cardinality detail={}"),
  EXEC_PLAN_LOG_TABLE_CARDINALITY(
      5247,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exec plan log table cardinality",
      "Exec plan log table cardinality detail={}"),
  EXEC_PLAN_LOG_UNIQUE_ROW_ESTIMATE(
      5248,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exec plan log unique row estimate",
      "Exec plan log unique row estimate detail={}"),
  EXEC_PLAN_LOG_SELECTIVITY(
      5249,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exec plan log selectivity",
      "Exec plan log selectivity detail={}"),
  INSUFFICIENT_JOINS_FOR_TABLES(
      5250,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Insufficient joins for tables",
      "Insufficient joins for tables detail={}"),
  EXEC_PLAN_JOIN_CARDINALITY_EXCEPTION(
      5251,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exec plan join cardinality exception",
      "Exec plan join cardinality exception detail={}"),
  EXEC_PLAN_JOIN_SELECTIVITY_EXCEPTION(
      5252,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exec plan join selectivity exception",
      "Exec plan join selectivity exception detail={}"),
  SQL_BUILDER_HOMOGENEOUS_JOIN_ONLY(
      5253,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Sql builder homogeneous join only",
      "Sql builder homogeneous join only detail={}"),
  SQL_BUILDER_MULTIPLE_OUTERS_NOT_SUPPORTED(
      5254,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Sql builder multiple outers not supported",
      "Sql builder multiple outers not supported detail={}"),
  SQL_BUILDER_XLATOR_UNSUPPORTED_IN_HOMEGENEOUS(
      5255,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Sql builder xlator unsupported in homegeneous",
      "Sql builder xlator unsupported in homegeneous detail={}"),
  NO_JOIN_PATH_BETWEEN_TABLES(
      5256,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "No join path between tables",
      "No join path between tables detail={}"),
  OPTIMIZER_TABLE_STATS_NOT_LOADED(
      5257,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Optimizer table stats not loaded",
      "Optimizer table stats not loaded detail={}"),
  WHERE_VAR_MUST_BE_BACKEND_COL(
      5258,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Where var must be backend col",
      "Where var must be backend col detail={}"),
  EXECDATA_PRIVATE_OBJ_KEY_NULL(
      5259,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Execdata private obj key null",
      "Execdata private obj key null detail={}"),
  UNKNOWN_OPCODE_LOAD_TYPE(
      5260,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unknown opcode load type",
      "Unknown opcode load type detail={}"),
  WRONG_OPERATOR_USAGE(
      5261,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Wrong operator usage",
      "Wrong operator usage detail={}"),
  WRONG_DATA_COMPARISON(
      5262,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Wrong data comparison",
      "Wrong data comparison detail={}"),
  LVALUE_INVALID_TYPE(
      5263,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Lvalue invalid type",
      "Lvalue invalid type detail={}"),
  RVALUE_INVALID_TYPE(
      5264,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Rvalue invalid type",
      "Rvalue invalid type detail={}"),
  UNSUPPORTED_CONVERSION(
      5265,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unsupported conversion",
      "Unsupported conversion detail={}"),
  TYPE_COMPARISON_UNSUPPORTED(
      5266,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Type comparison unsupported",
      "Type comparison unsupported detail={}"),
  OPERATOR_INVALID_FOR_TYPE(
      5267,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Operator invalid for type",
      "Operator invalid for type detail={}"),
  USER_CTX_INVALID_TYPE(
      5268,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "User ctx invalid type",
      "User ctx invalid type detail={}"),
  DATA_EXTRACTOR_CREATE_ERROR(
      5269,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Data extractor create error",
      "Data extractor create error detail={}"),
  HTML_GENERATION_ERROR(
      6003,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Html generation error",
      "Html generation error detail={}"),
  NO_DATA_FOR_CONVERSION(
      6004,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "No data for conversion",
      "No data for conversion detail={}"),
  CANNOT_CONVERT_MULTIPLE_RESULT_SETS(
      6005,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Cannot convert multiple result sets",
      "Cannot convert multiple result sets detail={}"),
  HTML_CONV_EXT_NOT_SUPPORTED(
      6006,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Html conv ext not supported",
      "Html conv ext not supported detail={}"),
  XML_CONV_EXT_NOT_SUPPORTED(
      6007,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Xml conv ext not supported",
      "Xml conv ext not supported detail={}"),
  NO_RESPONSE_OBJECT(
      6008,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "No response object",
      "No response object detail={}"),
  HTML_GEN_BAD_STYLESHEET_URL(
      6009,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Html gen bad stylesheet url",
      "Html gen bad stylesheet url detail={}"),
  HTML_GEN_NO_STYLESHEET(
      6010,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Html gen no stylesheet",
      "Html gen no stylesheet detail={}"),
  SEND_RESPONSE_EXCEPTION(
      6011,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Send response exception",
      "Send response exception detail={}"),
  XML_CONV_EXCEPTION(
      6012,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Xml conv exception",
      "Xml conv exception detail={}"),
  STYLESHEET_MERGE_EXCEPTION(
      6013,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Stylesheet merge exception",
      "Stylesheet merge exception detail={}"),
  COLMAPPER_XML_FIELD_NOT_STRING(
      6014,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Colmapper xml field not string",
      "Colmapper xml field not string detail={}"),
  REDIRECT_NOT_SUPPORTED_BY_CONVERTERS(
      6015,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Redirect not supported by converters",
      "Redirect not supported by converters detail={}"),
  MIME_CONV_INVALID_OUTPUT_TYPE(
      6016,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Mime conv invalid output type",
      "Mime conv invalid output type detail={}"),
  MIME_CONV_ONE_PIPE_REQD(
      6017,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Mime conv one pipe reqd",
      "Mime conv one pipe reqd detail={}"),
  MIME_CONV_QUERY_PIPE_REQD(
      6018,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Mime conv query pipe reqd",
      "Mime conv query pipe reqd detail={}"),
  MIME_CONV_ONE_MAPPING_REQD(
      6019,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Mime conv one mapping reqd",
      "Mime conv one mapping reqd detail={}"),
  MIME_CONV_ONE_COLUMN_REQD(
      6020,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Mime conv one column reqd",
      "Mime conv one column reqd detail={}"),
  MIME_CONV_MULTICOL_RESULT(
      6021,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Mime conv multicol result",
      "Mime conv multicol result detail={}"),
  MIME_CONV_MULTIROW_RESULT(
      6022,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Mime conv multirow result",
      "Mime conv multirow result detail={}"),
  XML_TWO_ROOT_ELEMENTS(
      6023,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Xml two root elements",
      "Xml two root elements detail={}"),
  XML_VAR_LINK_AND_MAPPING(
      6024,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Xml var link and mapping",
      "Xml var link and mapping detail={}"),
  XML_PARENT_MAPPING_NOT_SUPPORTED(
      6029,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Xml parent mapping not supported",
      "Xml parent mapping not supported detail={}"),
  VFS_CONVERT_PATH_ERROR(
      6030,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Vfs convert path error",
      "Vfs convert path error detail={}"),
  DATA_INVALID_CONVERSION(
      6031,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Data invalid conversion",
      "Data invalid conversion detail={}"),
  WARN_CALL_MAPPED_KEY_COLUMN_ON_LINK(
      6032,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Warn call mapped key column on link",
      "Warn call mapped key column on link detail={}"),
  DATA_CANNOT_CONVERT_WITH_REASON(
      6033,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Data cannot convert with reason",
      "Data cannot convert with reason detail={}"),
  CANNOT_RETURN_MULTIPLE_RESULT_SETS(
      6034,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Cannot return multiple result sets",
      "Cannot return multiple result sets detail={}"),
  INVALID_INTERNAL_RESULT_CALL(
      6035,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid internal result call",
      "Invalid internal result call detail={}"),
  INTERNAL_RESULT_CALL_EXCEPTION(
      6036,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Internal result call exception",
      "Internal result call exception detail={}"),
  INTERNAL_REQUEST_CALL_EXCEPTION(
      6037,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Internal request call exception",
      "Internal request call exception detail={}"),
  INTERNAL_REQUEST_AUTHORIZATION_EXCEPTION(
      6038,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Internal request authorization exception",
      "Internal request authorization exception detail={}"),
  VIEW_NOT_FOUND(
      6039,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "View not found",
      "View not found detail={}"),
  INTERNAL_REQUEST_AUTHENTICATION_FAILED_EXCEPTION(
      6040,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Internal request authentication failed exception",
      "Internal request authentication failed exception detail={}"),
  MACRO_EXTRACTOR_CLASS_NOT_FOUND(
      6041,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Macro extractor class not found",
      "Macro extractor class not found detail={}"),
  MACRO_EXTRACTOR_INSTANTIATION_FAILED(
      6042,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Macro extractor instantiation failed",
      "Macro extractor instantiation failed detail={}"),
  MACRO_EXTRACTOR_ILLEGAL_ACCESS(
      6043,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Macro extractor illegal access",
      "Macro extractor illegal access detail={}"),
  MACRO_EXTRACTOR_INVOCATION_TARGET_ERROR(
      6044,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Macro extractor invocation target error",
      "Macro extractor invocation target error detail={}"),
  MACRO_EXTRACTOR_NO_SUCH_METHOD(
      6045,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Macro extractor no such method",
      "Macro extractor no such method detail={}"),
  MACRO_EXTRACTOR_INVALID_PARAMETER(
      6046,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Macro extractor invalid parameter",
      "Macro extractor invalid parameter detail={}");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  DataErrorCodes(
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
    for (DataErrorCodes code : values()) {
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
