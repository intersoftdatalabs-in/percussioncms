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
 * Table factory error catalog bridging legacy {@code
 * com.percussion.tablefactory.IPSTableFactoryErrors} ints (1001–1310: XML schema, object store,
 * SQL processing, schema/data process).
 *
 * <p>Every constant sets {@link #isAuditable()} to {@code false}: schema/SQL factory failures are
 * operational install/upgrade noise, not security dual-write events.
 *
 * <p><strong>Flat registry collision:</strong> the entire package-local range overlaps {@link
 * ServerErrorCodes} (1001–1709). This catalog therefore does <strong>not</strong> flat-register
 * any ints. Prefer this enum directly until a composite-key registry exists. Module code is {@link
 * AuditModule#CFG}.
 */
public enum TableFactoryErrorCodes implements SystemErrorCode {

  XML_ELEMENT_NULL(
      1001,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table factory XML element null",
      "Table factory XML element null expected={}"),

  XML_ELEMENT_WRONG_TYPE(
      1002,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table factory XML element wrong type",
      "Table factory XML element wrong type expected={} actual={}"),

  XML_ELEMENT_INVALID_ATTR(
      1003,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table factory XML element invalid attr",
      "Table factory XML element invalid attr tag={} attr={} value={}"),

  XML_ELEMENT_INVALID_CHILD(
      1004,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table factory XML element invalid child",
      "Table factory XML element invalid child tag={} child={} value={}"),

  STYLESHEET_NOT_FOUND(
      1005,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table factory stylesheet not found",
      "Table factory stylesheet not found name={}"),

  TRANSFORMATION_ERROR(
      1006,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table factory transform error",
      "Table factory transform error stylesheet={} detail={}"),

  LOG_FILE_CONF_ERROR(
      1007,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table factory log file config error",
      "Table factory log file config error"),

  LOG_FILE_WRITE_ERROR(
      1008,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table factory log file write error",
      "Table factory log file write error"),

  DATA_TYPE_MAP_NOT_FOUND(
      1101,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table factory data type map not found",
      "Table factory data type map not found db={} driver={} os={}"),

  INVALID_DATA_TYPE_MAPPING(
      1102,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table factory invalid data type mapping",
      "Table factory invalid data type mapping jdbc={} native={}"),

  JDBC_INT_DATA_TYPE_CONVERSION(
      1103,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table factory JDBC int data type conversion",
      "Table factory JDBC int data type conversion jdbc={}"),

  JDBC_STRING_DATA_TYPE_CONVERSION(
      1104,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table factory JDBC string data type conversion",
      "Table factory JDBC string data type conversion jdbc={}"),

  PW_DECRYPTION_ERROR(
      1105,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table factory password decryption error",
      "Table factory password decryption error password={} detail={}"),

  DUPLICATE_COLUMN(
      1106,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table factory duplicate column",
      "Table factory duplicate column container={} column={}"),

  INVALID_COLUMN_NAME(
      1107,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table factory invalid column name",
      "Table factory invalid column name container={}"),

  REMOVE_LAST_COLUMN(
      1108,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table factory remove last column",
      "Table factory remove last column"),

  TABLE_SCHEMA_NOT_FOUND(
      1109,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table factory table schema not found",
      "Table factory table schema not found table={}"),

  COLUMN_NOT_FOUND(
      1110,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table factory column not found",
      "Table factory column not found table={} column={}"),

  ALTER_TABLE_SET_DATA(
      1111,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table factory alter table set data",
      "Table factory alter table set data table={}"),

  MISSING_COLUMN(
      1112,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table factory missing column",
      "Table factory missing column table={} container={} columns={}"),

  LOAD_DEFAULT_DATATYPE_MAP(
      1113,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table factory load default datatype map",
      "Table factory load default datatype map detail={}"),

  INVALID_ENCODING(
      1114,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table factory invalid encoding",
      "Table factory invalid encoding container={} encoding={}"),

  SQL_TABLE_META_DATA(
      1201,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table factory SQL table meta data",
      "Table factory SQL table meta data table={} detail={}"),

  SQL_CONNECTION_FAILED(
      1202,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table factory SQL connection failed",
      "Table factory SQL connection failed detail={}"),

  SQL_CATALOG_TABLE_FAILED(
      1203,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table factory SQL catalog table failed",
      "Table factory SQL catalog table failed table={} detail={}"),

  SQL_BIND_PARAMETER(
      1204,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table factory SQL bind parameter",
      "Table factory SQL bind parameter value={} type={} detail={}"),

  SQL_CATALOG_DATA(
      1205,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table factory SQL catalog data",
      "Table factory SQL catalog data table={} detail={}"),

  SCHEMA_PROCESS_ERROR(
      1301,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table factory schema process error",
      "Table factory schema process error table={} detail={}"),

  CHECK_EXISTING_DATA(
      1302,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table factory check existing data",
      "Table factory check existing data table={} detail={}"),

  SCHEMA_COLL_PROCESS_ERROR(
      1303,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table factory schema collection process error",
      "Table factory schema collection process error detail={}"),

  ALTER_NO_TABLE(
      1304,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table factory alter no table",
      "Table factory alter no table table={}"),

  UPDATE_DATA_NO_KEYS(
      1305,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table factory update data no keys",
      "Table factory update data no keys table={}"),

  UPDATE_DATA_NO_KEY_VALUE(
      1306,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table factory update data no key value",
      "Table factory update data no key value table={} column={}"),

  DATA_PROCESS_ERROR(
      1307,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table factory data process error",
      "Table factory data process error table={} detail={}"),

  UPDATE_DATA_NO_KEY_VALUE_IN_DB(
      1308,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table factory update data no key value in db",
      "Table factory update data no key value in db table={} column={}"),

  ALTER_VIEW_NOT_SUPPORTED(
      1309,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table factory alter view not supported",
      "Table factory alter view not supported view={}"),

  DATA_HANDLER_CLASS_NOT_FOUND(
      1310,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table factory data handler class not found",
      "Table factory data handler class not found class={}");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  TableFactoryErrorCodes(
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
   * No-op for the flat {@link com.intsof.percussioncms.auditlog.LegacyErrorCodeRegistry}: the full
   * range collides with {@link ServerErrorCodes}. Prefer this enum directly. Safe to call
   * repeatedly for bootstrap symmetry.
   */
  public static void ensureRegistered() {
    // Intentionally empty — package-local ints collide with ServerErrorCodes.
  }

  @Override
  public AuditModule module() {
    return AuditModule.CFG;
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
