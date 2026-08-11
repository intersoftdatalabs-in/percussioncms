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
 * structure / validation / handler ints (batches A–E; complete residual catalog).
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
 * <p><strong>Batch D scope</strong> (issue #2917):
 *
 * <ul>
 *   <li>{@code 2401–2475} — content-editor structure cluster: field/fieldset uniqueness and
 *       types, choices/sort, shared def, application flow / command-handler stylesheets and
 *       redirects, mapper/pipe/control refs, display mapping, table locator/set, UI definition,
 *       form action links, CE merge/override/shared-group validation, choice-filter dependents
 *       (75 codes; no Design collisions)
 * </ul>
 *
 * <p><strong>Batch E scope</strong> (issue #2918):
 *
 * <ul>
 *   <li>{@code 2801–2848} — object-store handler residual: app load/find, server/user config,
 *       app files/dirs/streams/IO, exclusive locks, request handler IO/properties, validation
 *       unexpected, character/feature-set load, role cfg, DB components, CE choice validation
 *       warnings, lookup table info (48 codes; max legacy {@code IPSObjectStoreErrors} constant)
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
 *   <li>After batch E there is <strong>zero</strong> remaining {@code IPSObjectStoreErrors}
 *       residual: 280 ObjectStore + 18 Design-owned ACL = 298 legacy constants.
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
      "App Login Page Not Supported"),

  FIELD_NAME_NOT_UNIQUE(
      2401,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Field Name Not Unique",
      "Field Name Not Unique"),

  UNSUPPORTED_FIELD_TYPE(
      2402,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unsupported Field Type",
      "Unsupported Field Type"),

  UNSUPPORTED_OCCURRENCE_DIMENSION(
      2403,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unsupported Occurrence Dimension",
      "Unsupported Occurrence Dimension"),

  UNSUPPORTED_OCCURRENCE_MULTI_VALUED_TYPE(
      2404,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unsupported Occurrence Multi Valued Type",
      "Unsupported Occurrence Multi Valued Type"),

  INVALID_CONTENT_TYPE(
      2405,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid Content Type",
      "Invalid Content Type"),

  INVALID_WORKFLOW_ID(
      2406,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid Workflow Id",
      "Invalid Workflow Id"),

  INVALID_CONTENT_EDITOR_SHARED_DEF(
      2407,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid Content Editor Shared Def",
      "Invalid Content Editor Shared Def"),

  UNSUPPORTED_CHOICE_TYPE(
      2408,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unsupported Choice Type",
      "Unsupported Choice Type"),

  UNSUPPORTED_SORT_ORDER(
      2409,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unsupported Sort Order",
      "Unsupported Sort Order"),

  INVALID_GLOBAL_TABLE_ID(
      2410,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid Global Table Id",
      "Invalid Global Table Id"),

  LOCAL_CHOICES_NULL_OR_EMPTY(
      2411,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Local Choices Null Or Empty",
      "Local Choices Null Or Empty"),

  LOOKUP_CHOICES_NULL(
      2412,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Lookup Choices Null",
      "Lookup Choices Null"),

  INVALID_APPLICATION_FLOW(
      2413,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid Application Flow",
      "Invalid Application Flow"),

  INVALID_COMMAND_HANDLER_STYLESHEETS(
      2414,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid Command Handler Stylesheets",
      "Invalid Command Handler Stylesheets"),

  INVALID_CONDITIONAL_EXIT(
      2415,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid Conditional Exit",
      "Invalid Conditional Exit"),

  INVALID_CONDITIONAL_REQUEST(
      2416,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid Conditional Request",
      "Invalid Conditional Request"),

  INVALID_CONDITIONAL_STYLESHEET(
      2417,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid Conditional Stylesheet",
      "Invalid Conditional Stylesheet"),

  INVALID_CONTAINER_LOCATOR(
      2418,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid Container Locator",
      "Invalid Container Locator"),

  INVALD_COMMAND_HANDLER_REFERENCE(
      2419,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invald Command Handler Reference",
      "Invald Command Handler Reference"),

  INVALD_REDIRECT(
      2420,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invald Redirect",
      "Invald Redirect"),

  INVALID_COMMAND_HANDLER_REDIRECTS(
      2421,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid Command Handler Redirects",
      "Invalid Command Handler Redirects"),

  INVALID_URL_REQUEST(
      2422,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid Url Request",
      "Invalid Url Request"),

  INVALID_CONTENT_EDITOR_MAPPER(
      2423,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid Content Editor Mapper",
      "Invalid Content Editor Mapper"),

  INVALID_CONTENT_EDITOR_PIPE(
      2424,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid Content Editor Pipe",
      "Invalid Content Editor Pipe"),

  INVALID_CONTROL_REF(
      2425,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid Control Ref",
      "Invalid Control Ref"),

  UNSUPPORTED_DEFAULT_SELECTED_TYPE(
      2426,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unsupported Default Selected Type",
      "Unsupported Default Selected Type"),

  INVALID_DEFAULT_SELECTED(
      2427,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid Default Selected",
      "Invalid Default Selected"),

  UNSUPPORTED_FIELD_SET_TYPE(
      2428,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unsupported Field Set Type",
      "Unsupported Field Set Type"),

  UNSUPPORTED_FIELD_SET_REPEATABILITY(
      2429,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unsupported Field Set Repeatability",
      "Unsupported Field Set Repeatability"),

  INVALID_FIELD_SET_NAME(
      2430,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid Field Set Name",
      "Invalid Field Set Name"),

  INVALID_DISPLAY_MAPPER(
      2431,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid Display Mapper",
      "Invalid Display Mapper"),

  INVALID_DISPLAY_MAPPING(
      2432,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid Display Mapping",
      "Invalid Display Mapping"),

  INVALID_DISPLAY_TEXT(
      2433,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid Display Text",
      "Invalid Display Text"),

  INVALID_ENTRY(
      2434,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid Entry",
      "Invalid Entry"),

  INVALID_FIELD(
      2435,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid Field",
      "Invalid Field"),

  INVALID_FIELD_TRANSLATION(
      2436,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid Field Translation",
      "Invalid Field Translation"),

  UNSUPPORTED_INCLUDE_WHEN(
      2437,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unsupported Include When",
      "Unsupported Include When"),

  INVALID_PARAM(
      2438,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid Param",
      "Invalid Param"),

  INVALID_SHARED_FIELD_GROUP(
      2439,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid Shared Field Group",
      "Invalid Shared Field Group"),

  INVALID_STYLESHEET(
      2440,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid Stylesheet",
      "Invalid Stylesheet"),

  INVALID_TABLE_LOCATOR(
      2441,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid Table Locator",
      "Invalid Table Locator"),

  INVALID_TABLE_REF(
      2442,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid Table Ref",
      "Invalid Table Ref"),

  INVALID_TABLE_SET(
      2443,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid Table Set",
      "Invalid Table Set"),

  INVALID_UI_DEFINITION(
      2444,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid Ui Definition",
      "Invalid Ui Definition"),

  UNSUPPORTED_DATA_HIDING(
      2445,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unsupported Data Hiding",
      "Unsupported Data Hiding"),

  INVALID_FORM_ACTION(
      2446,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid Form Action",
      "Invalid Form Action"),

  INVALID_ACTION_LINK(
      2447,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid Action Link",
      "Invalid Action Link"),

  INVALID_LOCATION(
      2448,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid Location",
      "Invalid Location"),

  INVALID_CUSTOM_ACTION_GROUP(
      2449,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid Custom Action Group",
      "Invalid Custom Action Group"),

  SYSTEM_TABLE_NOT_FOUND(
      2450,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "System Table Not Found",
      "System Table Not Found"),

  CE_INCORRECT_FIELD_COUNT(
      2451,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ce Incorrect Field Count",
      "Ce Incorrect Field Count"),

  CE_CANNOT_HAVE_FIELDSETS(
      2452,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ce Cannot Have Fieldsets",
      "Ce Cannot Have Fieldsets"),

  CE_NOT_EXIST_TABLES(
      2453,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ce Not Exist Tables",
      "Ce Not Exist Tables"),

  CREATE_TABLE_EXISTS(
      2454,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Create Table Exists",
      "Create Table Exists"),

  CE_SYSTEM_DEF_NOT_FOUND(
      2455,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ce System Def Not Found",
      "Ce System Def Not Found"),

  CE_SHARED_DEF_NOT_FOUND(
      2456,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ce Shared Def Not Found",
      "Ce Shared Def Not Found"),

  CE_MISSING_FIELD_ELEMENT(
      2457,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ce Missing Field Element",
      "Ce Missing Field Element"),

  CE_INCLUDED_GROUP_INVALID(
      2458,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ce Included Group Invalid",
      "Ce Included Group Invalid"),

  CE_SHARED_GROUP_NO_DEF(
      2459,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ce Shared Group No Def",
      "Ce Shared Group No Def"),

  CE_SHARED_EXCLUDE_INVALID(
      2460,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ce Shared Exclude Invalid",
      "Ce Shared Exclude Invalid"),

  CE_SYSTEM_EXCLUDE_INVALID(
      2461,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ce System Exclude Invalid",
      "Ce System Exclude Invalid"),

  CE_EXCLUDED_FIELD_MISSING(
      2462,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ce Excluded Field Missing",
      "Ce Excluded Field Missing"),

  CE_MAPPING_INVALID_CHILD(
      2463,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ce Mapping Invalid Child",
      "Ce Mapping Invalid Child"),

  CE_MAPPING_INVALID_CHILD_FIELDS(
      2464,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ce Mapping Invalid Child Fields",
      "Ce Mapping Invalid Child Fields"),

  CE_MAPPING_INVALID_DEFAULT_UISET(
      2465,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ce Mapping Invalid Default Uiset",
      "Ce Mapping Invalid Default Uiset"),

  CE_DUPLICATE_MERGED_FIELD_NAME(
      2466,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ce Duplicate Merged Field Name",
      "Ce Duplicate Merged Field Name"),

  CE_INVALID_FIELD_OVERRIDE(
      2467,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ce Invalid Field Override",
      "Ce Invalid Field Override"),

  CE_UNUSED_MAPPER(
      2468,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ce Unused Mapper",
      "Ce Unused Mapper"),

  CE_INVALID_SHARED_FIELDSET_TYPE(
      2469,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ce Invalid Shared Fieldset Type",
      "Ce Invalid Shared Fieldset Type"),

  CE_MISSING_OR_INVALID_CHILD_DISPLAY_MAPPING(
      2470,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ce Missing Or Invalid Child Display Mapping",
      "Ce Missing Or Invalid Child Display Mapping"),

  CE_GROUPNAME_AND_FIELDSETNAME_MUST_MATCH(
      2471,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ce Groupname And Fieldsetname Must Match",
      "Ce Groupname And Fieldsetname Must Match"),

  CE_FIELDSETNAME_AND_FIELDSETREF_MUST_MATCH(
      2472,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ce Fieldsetname And Fieldsetref Must Match",
      "Ce Fieldsetname And Fieldsetref Must Match"),

  CE_MISSING_CHILD_DISPLAY_MAPPING(
      2473,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ce Missing Child Display Mapping",
      "Ce Missing Child Display Mapping"),

  CHOICE_FILTER_MISSING_REQUIRED_CHILD(
      2474,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Choice Filter Missing Required Child",
      "Choice Filter Missing Required Child"),

  CHOICE_FILTER_DEPENDENT_FIELD_MISSING_ATTR(
      2475,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Choice Filter Dependent Field Missing Attr",
      "Choice Filter Dependent Field Missing Attr"),

  METHOD_NOT_SUPPORTED(
      2801,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Method Not Supported",
      "Method Not Supported"),

  APP_NOT_FOUND(
      2802,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "App Not Found",
      "App Not Found"),

  APP_NAME_ALREADY_EXISTS(
      2803,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "App Name Already Exists",
      "App Name Already Exists"),

  APP_LOAD_EXCEPTION(
      2804,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "App Load Exception",
      "App Load Exception"),

  SERVER_CFG_NOT_FOUND(
      2805,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Server Cfg Not Found",
      "Server Cfg Not Found"),

  SERVER_CFG_LOAD_EXCEPTION(
      2806,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Server Cfg Load Exception",
      "Server Cfg Load Exception"),

  USER_CFG_NOT_FOUND(
      2807,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "User Cfg Not Found",
      "User Cfg Not Found"),

  USER_CFG_LOAD_EXCEPTION(
      2808,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "User Cfg Load Exception",
      "User Cfg Load Exception"),

  APP_FILE_NOT_FOUND(
      2809,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "App File Not Found",
      "App File Not Found"),

  APP_DIR_NOT_FOUND(
      2810,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "App Dir Not Found",
      "App Dir Not Found"),

  APP_FILE_MKSUBDIR_ERROR(
      2811,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "App File Mksubdir Error",
      "App File Mksubdir Error"),

  LOCK_ALREADY_HELD(
      2812,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Lock Already Held",
      "Lock Already Held"),

  LOCK_WAIT_INTERRUPTED(
      2813,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Lock Wait Interrupted",
      "Lock Wait Interrupted"),

  LOCK_BAD_KEY(
      2814,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Lock Bad Key",
      "Lock Bad Key"),

  LOCK_BAD_LOCKER_ID(
      2815,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Lock Bad Locker Id",
      "Lock Bad Locker Id"),

  LOCK_BAD_EXPIRATION(
      2816,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Lock Bad Expiration",
      "Lock Bad Expiration"),

  LOCK_BAD_OBJECT(
      2817,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Lock Bad Object",
      "Lock Bad Object"),

  LOCK_BAD_TYPE(
      2818,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Lock Bad Type",
      "Lock Bad Type"),

  APP_FILE_NAME_NULL(
      2819,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "App File Name Null",
      "App File Name Null"),

  APP_FILE_STREAM_NULL(
      2820,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "App File Stream Null",
      "App File Stream Null"),

  APP_FILE_STREAM_EXHAUSTED(
      2821,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "App File Stream Exhausted",
      "App File Stream Exhausted"),

  APP_FILE_IO_ERROR(
      2822,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "App File Io Error",
      "App File Io Error"),

  REQ_UNKNOWN_TYPE(
      2823,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Req Unknown Type",
      "Req Unknown Type"),

  REQ_DOCUMENT_NULL(
      2824,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Req Document Null",
      "Req Document Null"),

  APP_ROOT_RENAME_FAILED(
      2825,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "App Root Rename Failed",
      "App Root Rename Failed"),

  HANDLER_IO_ERROR(
      2826,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Handler Io Error",
      "Handler Io Error"),

  APP_FILE_ROOT_MISMATCH(
      2827,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "App File Root Mismatch",
      "App File Root Mismatch"),

  HANDLER_PROPERTIES_NULL(
      2828,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Handler Properties Null",
      "Handler Properties Null"),

  HANDLER_OBJECTDIR_INVALID(
      2829,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Handler Objectdir Invalid",
      "Handler Objectdir Invalid"),

  HANDLER_UNEXPECTED_EXCEPTION(
      2830,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Handler Unexpected Exception",
      "Handler Unexpected Exception"),

  VALIDATION_UNEXPECTED_EXCEPTION(
      2831,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Validation Unexpected Exception",
      "Validation Unexpected Exception"),

  LOCK_CORRUPT_LOCKFILE(
      2832,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Lock Corrupt Lockfile",
      "Lock Corrupt Lockfile"),

  LOCK_IO_EXCEPTION(
      2833,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Lock Io Exception",
      "Lock Io Exception"),

  LOCK_ALREADY_HELD_SAME_USER(
      2834,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Lock Already Held Same User",
      "Lock Already Held Same User"),

  DOC_CONVERSION_FAILED(
      2835,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Doc Conversion Failed",
      "Doc Conversion Failed"),

  APP_MAPPER_EMPTY(
      2836,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "App Mapper Empty",
      "App Mapper Empty"),

  CHARACTER_SET_MAP_NOT_FOUND(
      2837,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Character Set Map Not Found",
      "Character Set Map Not Found"),

  CHARACTER_SET_LOAD_EXCEPTION(
      2838,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Character Set Load Exception",
      "Character Set Load Exception"),

  FEATURE_SET_LOAD_EXCEPTION(
      2839,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Feature Set Load Exception",
      "Feature Set Load Exception"),

  LOCK_NOT_HELD(
      2840,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Lock Not Held",
      "Lock Not Held"),

  ROLE_CFG_LOAD_EXCEPTION(
      2841,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Role Cfg Load Exception",
      "Role Cfg Load Exception"),

  DB_COMPONENT_NEW_ID(
      2842,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Db Component New Id",
      "Db Component New Id"),

  DB_COMPONENT_LOAD_EXCEPTION(
      2843,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Db Component Load Exception",
      "Db Component Load Exception"),

  RELATED_DB_COMPONENT_LOAD_EXCEPTION(
      2844,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Related Db Component Load Exception",
      "Related Db Component Load Exception"),

  DUPLICATE_SHARED_FIELD_VALIDATION_WARNING(
      2845,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Duplicate Shared Field Validation Warning",
      "Duplicate Shared Field Validation Warning"),

  INVALID_CE_FIELD_CHOICES_ERROR(
      2846,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid Ce Field Choices Error",
      "Invalid Ce Field Choices Error"),

  APP_FILE_EXISTS_RENAME_ERROR(
      2847,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "App File Exists Rename Error",
      "App File Exists Rename Error"),

  LOOKUP_TABLE_INFO_NULL(
      2848,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Lookup Table Info Null",
      "Lookup Table Info Null");

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
