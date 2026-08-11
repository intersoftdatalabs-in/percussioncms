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
 * Object-store error catalog (batch A) bridging legacy {@code
 * com.percussion.design.objectstore.IPSObjectStoreErrors} general + PSObjectStore + early
 * object-store object validation ints.
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
 * <p><strong>Collision / ownership notes:</strong>
 *
 * <ul>
 *   <li>{@link DesignErrorCodes} already flat-registers objectstore ACL / server-ACL / SP-instance
 *       ints ({@code 2201–2208}, {@code 2213–2214}, {@code 2218}, {@code 2327}, {@code 2351–2356})
 *       including the auditable ACL failures. This enum <strong>does not</strong> re-register those
 *       ints so Design keeps flat-registry ownership.
 *   <li>{@link ContentErrorCodes} lifecycle ints {@code 2001–2006} are intentionally outside
 *       objectstore general range (objectstore starts at {@code 2011}).
 *   <li>Remaining objectstore ranges ({@code 2261+} object objects, content-editor 2401–2500,
 *       handlers 2801–3000) are residual batch B+ (#2899).
 * </ul>
 *
 * <p>Every constant in this batch sets {@link #isAuditable()} to {@code false}: XML parse /
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
      "Notifier From Too Big");

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
