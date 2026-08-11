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
 * Object-store error catalog bridging legacy {@code
 * com.percussion.design.objectstore.IPSObjectStoreErrors} general + PSObjectStore + object-store
 * structure / validation ints (batches A–C).
 *
 * <p><strong>Batch A scope</strong> (issue #2898):
 *
 * <ul>
 *   <li>{@code 2011–2021} — general XML/collection/validation errors used across objectstore
 *   <li>{@code 2101–2103}, {@code 2200} — {@code PSObjectStore} / pipe name
 *   <li>{@code 2209–2260} (subset) — application / backend / dataset / pipe / notifier structure
 *       codes not already owned by {@link DesignErrorCodes}
 * </ul>
 *
 * <p><strong>Batch B scope</strong> (issue #2899):
 *
 * <ul>
 *   <li>{@code 2261–2320} — roles / recipients / requestor / reqlink / backend tank / Java exit /
 *       roleset / conditionals / exit params / sorted-update columns / data selector / CGI-HTML-XML
 *       cookie names / literals / app root / page tank / stylesheet / exit call / JDBC driver /
 *       backend conn / login webpage / logger / notifier recipients / update-pipe sync (60 codes)
 * </ul>
 *
 * <p><strong>Batch C scope</strong> (issue #2912):
 *
 * <ul>
 *   <li>{@code 2321–2330} (skip Design {@code 2327}) — conditionals / UDF / tank joins / data mapping
 *       / cache age / sync columns / XML param / relationship / macro (9 codes)
 *   <li>{@code 2350} — server request root length
 *   <li>{@code 2357–2380} (skip Design {@code 2351–2356}) — app version / Java exit / datasets /
 *       request roots / extension call / query pipe / custom error URL / subject type / app roles /
 *       database functions / JDBC-JNDI datasource / legacy app login/creds (24 codes)
 * </ul>
 *
 * <p><strong>Collision / ownership notes:</strong>
 *
 * <ul>
 *   <li>{@link DesignErrorCodes} already flat-registers objectstore ACL / server-ACL / SP-instance
 *       ints ({@code 2201–2208}, {@code 2213–2214}, {@code 2218}, {@code 2327}, {@code 2351–2356})
 *       including the auditable ACL failures. This enum <strong>does not</strong> re-register those
 *       ints so Design keeps flat-registry ownership.
 *   <li>{@link ContentErrorCodes} lifecycle ints {@code 2001–2006} are intentionally outside
 *       objectstore general range (objectstore starts at {@code 2011}).
 *   <li>Remaining objectstore ranges after batch C: content-editor {@code 2401–2500}, handlers
 *       {@code 2801–2848} (max legacy constant) → residual batches D/E.
 * </ul>
 *
 * <p>Every constant in this catalog sets {@link #isAuditable()} to {@code false}: XML parse /
 * field-length / structural validation noise is operational only. Security-relevant ACL dual-write
 * remains on {@link DesignErrorCodes}. Module code is {@link AuditModule#DESN}.
 */
public enum ObjectStoreErrorCodes implements SystemErrorCode {

  XML_ELEMENT_NULL(
      2011,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Xml Element Null",
      "Xml Element Null"),

  XML_ELEMENT_WRONG_TYPE(
      2012,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Xml Element Wrong Type",
      "Xml Element Wrong Type"),

  XML_ELEMENT_INVALID_ID(
      2013,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Xml Element Invalid Id",
      "Xml Element Invalid Id"),

  XML_ELEMENT_INVALID_ATTR(
      2014,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Xml Element Invalid Attr",
      "Xml Element Invalid Attr"),

  XML_ELEMENT_INVALID_CHILD(
      2015,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Xml Element Invalid Child",
      "Xml Element Invalid Child"),

  XML_ELEMENT_VALUE_TOO_BIG(
      2016,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Xml Element Value Too Big",
      "Xml Element Value Too Big"),

  COLL_BAD_CONTENT_TYPE(
      2017,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Coll Bad Content Type",
      "Coll Bad Content Type"),

  COLL_CONTENT_TYPE_NOT_FOUND(
      2018,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Coll Content Type Not Found",
      "Coll Content Type Not Found"),

  VALIDATION_NOT_IMPLEMENTED(
      2019,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Validation Not Implemented",
      "Validation Not Implemented"),

  APP_VERSION_DOES_NOT_MATCH(
      2020,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "App Version Does Not Match",
      "App Version Does Not Match"),

  OBJECT_CLONING_NOT_ALLOWED(
      2021,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Object Cloning Not Allowed",
      "Object Cloning Not Allowed"),

  GET_APP_LOG_NO_DATA(
      2101,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Get App Log No Data",
      "Get App Log No Data"),

  CONN_OBJ_NULL(
      2102,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Conn Obj Null",
      "Conn Obj Null"),

  MALFORMED_RESPONSE_DOCUMENT(
      2103,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Malformed Response Document",
      "Malformed Response Document"),

  PIPE_NAME_EMPTY(
      2200,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Pipe Name Empty",
      "Pipe Name Empty"),

  APP_NAME_EMPTY(
      2209,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "App Name Empty",
      "App Name Empty"),

  APP_NAME_TOO_BIG(
      2210,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "App Name Too Big",
      "App Name Too Big"),

  APP_DESC_TOO_BIG(
      2211,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "App Desc Too Big",
      "App Desc Too Big"),

  APP_ROOT_TOO_BIG(
      2212,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "App Root Too Big",
      "App Root Too Big"),

  BE_CRED_ALIAS_NULL(
      2215,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Be Cred Alias Null",
      "Be Cred Alias Null"),

  BE_CRED_ALIAS_TOO_BIG(
      2216,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Be Cred Alias Too Big",
      "Be Cred Alias Too Big"),

  BE_CRED_COMMENT_TOO_BIG(
      2217,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Be Cred Comment Too Big",
      "Be Cred Comment Too Big"),

  APP_REQ_PARAM_HTML_TOO_BIG(
      2219,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "App Req Param Html Too Big",
      "App Req Param Html Too Big"),

  APP_REQ_TYPE_VALUE_TOO_BIG(
      2220,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "App Req Type Value Too Big",
      "App Req Type Value Too Big"),

  BE_COL_NAME_EMPTY(
      2221,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Be Col Name Empty",
      "Be Col Name Empty"),

  BE_COL_NAME_TOO_BIG(
      2222,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Be Col Name Too Big",
      "Be Col Name Too Big"),

  BE_DRIVER_NULL(
      2223,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Be Driver Null",
      "Be Driver Null"),

  BE_DRIVER_TOO_BIG(
      2224,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Be Driver Too Big",
      "Be Driver Too Big"),

  BE_SERVER_TOO_BIG(
      2225,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Be Server Too Big",
      "Be Server Too Big"),

  BE_UID_TOO_BIG(
      2226,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Be Uid Too Big",
      "Be Uid Too Big"),

  BE_PASSWORD_TOO_BIG(
      2227,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Be Password Too Big",
      "Be Password Too Big"),

  BE_JOIN_RCOL_NULL(
      2228,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Be Join Rcol Null",
      "Be Join Rcol Null"),

  BE_JOIN_LCOL_NULL(
      2229,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Be Join Lcol Null",
      "Be Join Lcol Null"),

  UDFCALL_EXIT_NULL(
      2230,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Udfcall Exit Null",
      "Udfcall Exit Null"),

  JSCRIPT_EXIT_WRONG_TYPE(
      2231,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Jscript Exit Wrong Type",
      "Jscript Exit Wrong Type"),

  BE_TABLE_ALIAS_NULL(
      2232,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Be Table Alias Null",
      "Be Table Alias Null"),

  BE_TABLE_ALIAS_TOO_BIG(
      2233,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Be Table Alias Too Big",
      "Be Table Alias Too Big"),

  BE_DB_TOO_BIG(
      2234,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Be Db Too Big",
      "Be Db Too Big"),

  BE_ORIGIN_TOO_BIG(
      2235,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Be Origin Too Big",
      "Be Origin Too Big"),

  BE_TABLE_TOO_BIG(
      2236,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Be Table Too Big",
      "Be Table Too Big"),

  CUSTOM_ERROR_CODE_EMPTY(
      2237,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Custom Error Code Empty",
      "Custom Error Code Empty"),

  DATAENC_KEY_STRENGTH_REQD(
      2238,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Dataenc Key Strength Reqd",
      "Dataenc Key Strength Reqd"),

  DATAMAPPING_XML_FIELD_EMPTY(
      2239,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Datamapping Xml Field Empty",
      "Datamapping Xml Field Empty"),

  DATAMAPPING_XML_FIELD_TOO_BIG(
      2240,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Datamapping Xml Field Too Big",
      "Datamapping Xml Field Too Big"),

  DATAMAPPING_BE_COL_NULL(
      2241,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Datamapping Be Col Null",
      "Datamapping Be Col Null"),

  DATASET_NAME_NULL(
      2242,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Dataset Name Null",
      "Dataset Name Null"),

  DATASET_NAME_TOO_BIG(
      2243,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Dataset Name Too Big",
      "Dataset Name Too Big"),

  DATASET_DESC_TOO_BIG(
      2244,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Dataset Desc Too Big",
      "Dataset Desc Too Big"),

  DATASET_PAGE_TANK_NULL(
      2245,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Dataset Page Tank Null",
      "Dataset Page Tank Null"),

  DATASET_REQUESTOR_NULL(
      2246,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Dataset Requestor Null",
      "Dataset Requestor Null"),

  DATASET_RESULT_PAGES_NULL(
      2247,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Dataset Result Pages Null",
      "Dataset Result Pages Null"),

  DATASET_REQUEST_LINK_NULL(
      2248,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Dataset Request Link Null",
      "Dataset Request Link Null"),

  PAGE_TANK_SCHEMA_NULL(
      2249,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Page Tank Schema Null",
      "Page Tank Schema Null"),

  PAGE_TANK_XML_FIELD_TOO_BIG(
      2250,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Page Tank Xml Field Too Big",
      "Page Tank Xml Field Too Big"),

  PIPE_NAME_TOO_BIG(
      2252,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Pipe Name Too Big",
      "Pipe Name Too Big"),

  PIPE_DESC_TOO_BIG(
      2253,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Pipe Desc Too Big",
      "Pipe Desc Too Big"),

  PIPE_BE_TANK_NULL(
      2254,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Pipe Be Tank Null",
      "Pipe Be Tank Null"),

  UPDATEPIPE_DATA_SYNC_NULL(
      2255,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Updatepipe Data Sync Null",
      "Updatepipe Data Sync Null"),

  SIMPLE_EXIT_INVALID_TYPE(
      2256,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Simple Exit Invalid Type",
      "Simple Exit Invalid Type"),

  NOTIFIER_PROVIDER_TYPE_INVALID(
      2257,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Notifier Provider Type Invalid",
      "Notifier Provider Type Invalid"),

  NOTIFIER_SERVER_NULL(
      2258,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Notifier Server Null",
      "Notifier Server Null"),

  NOTIFIER_SERVER_TOO_BIG(
      2259,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Notifier Server Too Big",
      "Notifier Server Too Big"),

  NOTIFIER_FROM_TOO_BIG(
      2260,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Notifier From Too Big",
      "Notifier From Too Big"),

  // --- batch B (#2899): roles / recipients / exits / conditionals / data-sel / JDBC (2261–2320) ---
  ROLE_NAME_EMPTY(
      2261,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Role Name Empty",
      "Role Name Empty"),

  ROLE_NAME_TOO_BIG(
      2262,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Role Name Too Big",
      "Role Name Too Big"),

  UDFEXIT_NAME_EMPTY(
      2263,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Udfexit Name Empty",
      "Udfexit Name Empty"),

  UDFEXIT_BODY_EMPTY(
      2264,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Udfexit Body Empty",
      "Udfexit Body Empty"),

  RECIPIENT_NAME_EMPTY(
      2265,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Recipient Name Empty",
      "Recipient Name Empty"),

  RECIPIENT_NAME_TOO_BIG(
      2266,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Recipient Name Too Big",
      "Recipient Name Too Big"),

  REQUESTOR_PAGE_NAME_NULL(
      2267,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Requestor Page Name Null",
      "Requestor Page Name Null"),

  REQLINK_DATA_SET_NULL(
      2268,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Reqlink Data Set Null",
      "Reqlink Data Set Null"),

  REQLINK_XML_FIELD_TOO_BIG(
      2269,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Reqlink Xml Field Too Big",
      "Reqlink Xml Field Too Big"),

  BE_DATATANK_TABLES_EMPTY(
      2270,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Be Datatank Tables Empty",
      "Be Datatank Tables Empty"),

  BE_DATATANK_TABLES_DUP(
      2271,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Be Datatank Tables Dup",
      "Be Datatank Tables Dup"),

  JAVA_EXIT_CLASS_NULL(
      2272,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Java Exit Class Null",
      "Java Exit Class Null"),

  JAVA_EXIT_CLASS_TOO_BIG(
      2273,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Java Exit Class Too Big",
      "Java Exit Class Too Big"),

  ROLESET_PROVIDER_TYPE_INVALID(
      2274,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Roleset Provider Type Invalid",
      "Roleset Provider Type Invalid"),

  ROLESET_PROVIDER_INST_TOO_BIG(
      2275,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Roleset Provider Inst Too Big",
      "Roleset Provider Inst Too Big"),

  COND_VAR_NAME_EMPTY(
      2276,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Cond Var Name Empty",
      "Cond Var Name Empty"),

  COND_VAR_NAME_TOO_BIG(
      2277,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Cond Var Name Too Big",
      "Cond Var Name Too Big"),

  COND_OPTYPE_UNKNOWN(
      2278,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Cond Optype Unknown",
      "Cond Optype Unknown"),

  EXIT_PARAM_NAME_EMPTY(
      2279,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exit Param Name Empty",
      "Exit Param Name Empty"),

  EXIT_PARAM_NAME_TOO_BIG(
      2280,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exit Param Name Too Big",
      "Exit Param Name Too Big"),

  EXIT_PARAM_DT_TOO_BIG(
      2281,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exit Param Dt Too Big",
      "Exit Param Dt Too Big"),

  EXIT_PARAM_DESC_TOO_BIG(
      2282,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exit Param Desc Too Big",
      "Exit Param Desc Too Big"),

  SORTEDCOL_COL_NULL(
      2283,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Sortedcol Col Null",
      "Sortedcol Col Null"),

  UPDATECOL_COL_NULL(
      2284,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Updatecol Col Null",
      "Updatecol Col Null"),

  DATASEL_NATIVE_STMT_REQD(
      2285,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Datasel Native Stmt Reqd",
      "Datasel Native Stmt Reqd"),

  DATASEL_CACHE_TYPE_REQD(
      2286,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Datasel Cache Type Reqd",
      "Datasel Cache Type Reqd"),

  UDFCALL_PARAM_COUNT_MISMATCH(
      2287,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Udfcall Param Count Mismatch",
      "Udfcall Param Count Mismatch"),

  CGI_VAR_NAME_EMPTY(
      2288,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Cgi Var Name Empty",
      "Cgi Var Name Empty"),

  HTML_PARAM_NAME_EMPTY(
      2289,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Html Param Name Empty",
      "Html Param Name Empty"),

  XML_FIELD_NAME_EMPTY(
      2290,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Xml Field Name Empty",
      "Xml Field Name Empty"),

  COOKIE_NAME_EMPTY(
      2291,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Cookie Name Empty",
      "Cookie Name Empty"),

  INVALID_OBJECT_FOR_COPY(
      2292,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid Object For Copy",
      "Invalid Object For Copy"),

  COND_BOOL_UNKNOWN(
      2293,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Cond Bool Unknown",
      "Cond Bool Unknown"),

  LITERAL_DATE_INVALID(
      2294,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Literal Date Invalid",
      "Literal Date Invalid"),

  LITERAL_DATEFMT_INVALID(
      2295,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Literal Datefmt Invalid",
      "Literal Datefmt Invalid"),

  LITERAL_NUMERIC_INVALID(
      2296,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Literal Numeric Invalid",
      "Literal Numeric Invalid"),

  LITERAL_NUMERICFMT_INVALID(
      2297,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Literal Numericfmt Invalid",
      "Literal Numericfmt Invalid"),

  UDFEXIT_DESC_TOO_BIG(
      2298,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Udfexit Desc Too Big",
      "Udfexit Desc Too Big"),

  APP_ROOT_REQD(
      2299,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "App Root Reqd",
      "App Root Reqd"),

  PAGE_TANK_BAD_SCHEMA_URL(
      2300,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Page Tank Bad Schema Url",
      "Page Tank Bad Schema Url"),

  STYLE_SHEET_BAD_URL(
      2301,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Style Sheet Bad Url",
      "Style Sheet Bad Url"),

  EXITCALL_EXIT_NULL(
      2302,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exitcall Exit Null",
      "Exitcall Exit Null"),

  EXIT_INTERFACES_NOT_IMPLEMENTED(
      2303,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exit Interfaces Not Implemented",
      "Exit Interfaces Not Implemented"),

  UNKNOWN_EXIT_TYPE(
      2304,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unknown Exit Type",
      "Unknown Exit Type"),

  BE_JOIN_UNKNOWN_TYPE(
      2305,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Be Join Unknown Type",
      "Be Join Unknown Type"),

  CUSTOM_ERROR_URL_EMPTY(
      2306,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Custom Error Url Empty",
      "Custom Error Url Empty"),

  BE_TABLE_NULL(
      2307,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Be Table Null",
      "Be Table Null"),

  JDBC_DRIVER_CLASS_NULL(
      2308,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Jdbc Driver Class Null",
      "Jdbc Driver Class Null"),

  JDBC_DRIVER_CLASS_LOAD_ERROR(
      2309,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Jdbc Driver Class Load Error",
      "Jdbc Driver Class Load Error"),

  BE_CONN_MAXCONN_INVALID(
      2310,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Be Conn Maxconn Invalid",
      "Be Conn Maxconn Invalid"),

  BE_CONN_MINCONN_INVALID(
      2311,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Be Conn Minconn Invalid",
      "Be Conn Minconn Invalid"),

  BE_CONN_TIMEOUT_INVALID(
      2312,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Be Conn Timeout Invalid",
      "Be Conn Timeout Invalid"),

  EXIT_PARAM_VALUE_NULL(
      2313,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exit Param Value Null",
      "Exit Param Value Null"),

  DATASEL_SEL_TYPE_INVALID(
      2314,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Datasel Sel Type Invalid",
      "Datasel Sel Type Invalid"),

  DATASEL_CACHE_TYPE_INVALID(
      2315,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Datasel Cache Type Invalid",
      "Datasel Cache Type Invalid"),

  DATASEL_CACHE_AGE_INTERVAL_INVALID(
      2316,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Datasel Cache Age Interval Invalid",
      "Datasel Cache Age Interval Invalid"),

  LOGIN_WEBPAGE_URL_EMPTY(
      2317,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Login Webpage Url Empty",
      "Login Webpage Url Empty"),

  LOGGER_OPTIONS_INVALID(
      2318,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Logger Options Invalid",
      "Logger Options Invalid"),

  NOTIFIER_RECIPIENTS_EMPTY(
      2319,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Notifier Recipients Empty",
      "Notifier Recipients Empty"),

  UPDATEPIPE_NO_SYNC_TYPES(
      2320,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Updatepipe No Sync Types",
      "Updatepipe No Sync Types"),

  COND_VALUE_NULL(
      2321,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Cond Value Null",
      "Cond Value Null"),

  UDFCALL_EXIT_UNDEFINED(
      2322,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Udfcall Exit Undefined",
      "Udfcall Exit Undefined"),

  BE_TANK_JOINS_REQUIRED(
      2323,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Be Tank Joins Required",
      "Be Tank Joins Required"),

  DATAMAPPING_GROUP_ID_INVALID(
      2324,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Datamapping Group Id Invalid",
      "Datamapping Group Id Invalid"),

  DATASEL_CACHE_AGE_TIME_REQUIRED(
      2325,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Datasel Cache Age Time Required",
      "Datasel Cache Age Time Required"),

  SYNC_NO_UPDATE_COLUMNS(
      2326,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Sync No Update Columns",
      "Sync No Update Columns"),

  XML_PARAM_INVALID(
      2328,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Xml Param Invalid",
      "Xml Param Invalid"),

  RELATIONSHIP_PROPERTY_NAME_EMPTY(
      2329,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Relationship Property Name Empty",
      "Relationship Property Name Empty"),

  MACRO_NAME_EMPTY(
      2330,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Macro Name Empty",
      "Macro Name Empty"),

  SRV_ROOT_TOO_BIG(
      2350,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Srv Root Too Big",
      "Srv Root Too Big"),

  APP_VERSION_INVALID(
      2357,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "App Version Invalid",
      "App Version Invalid"),

  JAVA_EXIT_HANDLER_NULL(
      2358,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Java Exit Handler Null",
      "Java Exit Handler Null"),

  JAVA_EXIT_HANDLER_NULL_PARAM_DEF(
      2359,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Java Exit Handler Null Param Def",
      "Java Exit Handler Null Param Def"),

  APP_NO_DATASETS(
      2360,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "App No Datasets",
      "App No Datasets"),

  APP_REQUEST_ROOTS_DUP(
      2361,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "App Request Roots Dup",
      "App Request Roots Dup"),

  APP_DATASET_NAMES_DUP(
      2362,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "App Dataset Names Dup",
      "App Dataset Names Dup"),

  EXT_CALL_PARAM_VALUE_NULL(
      2363,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ext Call Param Value Null",
      "Ext Call Param Value Null"),

  HETERO_NATIVE_SELECT_NOT_SUPPORTED(
      2364,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Hetero Native Select Not Supported",
      "Hetero Native Select Not Supported"),

  QPIPE_DATA_SELECTOR_NULL(
      2365,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Qpipe Data Selector Null",
      "Qpipe Data Selector Null"),

  CUSTOM_ERROR_URL_INVALID(
      2366,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Custom Error Url Invalid",
      "Custom Error Url Invalid"),

  DATASET_XMLFIELD_MULTI_LINK_ERROR(
      2367,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Dataset Xmlfield Multi Link Error",
      "Dataset Xmlfield Multi Link Error"),

  SUBJECT_TYPE_INVALID(
      2368,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Subject Type Invalid",
      "Subject Type Invalid"),

  APP_ROLES_NOT_SUPPORTED(
      2369,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "App Roles Not Supported",
      "App Roles Not Supported"),

  DATABASE_FUNCTION_CALL_PARAM_COUNT_MISMATCH(
      2370,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Database Function Call Param Count Mismatch",
      "Database Function Call Param Count Mismatch"),

  DATABASE_FUNCTION_PARAM_VALUE_NULL(
      2371,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Database Function Param Value Null",
      "Database Function Param Value Null"),

  DATABASE_FUNCTION_DEFINITION_NOT_FOUND(
      2372,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Database Function Definition Not Found",
      "Database Function Definition Not Found"),

  UNSUPPORTED_DATABASE_FUNCTION_PARAMETER_TYPE(
      2373,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unsupported Database Function Parameter Type",
      "Unsupported Database Function Parameter Type"),

  DATABASE_FUNCTION_PARSE_ERROR(
      2374,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Database Function Parse Error",
      "Database Function Parse Error"),

  REQUEST_NAME_DUP(
      2375,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Request Name Dup",
      "Request Name Dup"),

  NO_JDBC_DRIVER_CONFIG(
      2376,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "No Jdbc Driver Config",
      "No Jdbc Driver Config"),

  NO_JNDI_DATASOURCE(
      2377,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "No Jndi Datasource",
      "No Jndi Datasource"),

  NO_DATASOURCE_CONNECTION(
      2378,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "No Datasource Connection",
      "No Datasource Connection"),

  APP_BACKEND_CREDS_NOT_SUPPORTED(
      2379,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "App Backend Creds Not Supported",
      "App Backend Creds Not Supported"),

  APP_LOGIN_PAGE_NOT_SUPPORTED(
      2380,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "App Login Page Not Supported",
      "App Login Page Not Supported");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  ObjectStoreErrorCodes(
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
   *
   * <p>Does not include Design-owned ACL ints (see class javadoc).
   */
  public static void ensureRegistered() {
    for (ObjectStoreErrorCodes code : values()) {
      LegacyErrorCodeRegistry.register(code.numericCode(), code);
    }
  }

  @Override
  public AuditModule module() {
    return AuditModule.DESN;
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
