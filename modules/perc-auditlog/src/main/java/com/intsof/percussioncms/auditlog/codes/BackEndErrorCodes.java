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
 * Error catalog bridging legacy {@code com.percussion.data.IPSBackEndErrors} ints
 * (5001–5057, 5401–5402, 5999: connectivity, SQL builder, exec plan, activity log).
 *
 * <p>{@link #numericCode()} preserves historical ints. Every constant sets {@link #isAuditable()}
 * explicitly. Only {@link #AUTHORIZATION_ERROR} dual-writes; JDBC/SQL/pool failures do not.
 *
 * <p>All ints in this catalog are flat-registered in {@link LegacyErrorCodeRegistry} (no
 * package-local collision with already-bootstrapped catalogs). Module code is
 * {@link AuditModule#SYS}.
 */
public enum BackEndErrorCodes implements SystemErrorCode {

  AUTHORIZATION_ERROR(
      5001,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "Authorization error",
      "Authorization error detail={}"),
  REQUEST_QUEUE_FULL(
      5002,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Request queue full",
      "Request queue full detail={}"),
  SERVER_DOWN_ERROR(
      5003,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Server down error",
      "Server down error detail={}"),
  SET_CATALOG_RETRY(
      5004,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Set catalog retry",
      "Set catalog retry detail={}"),
  SET_CATALOG_FAILED(
      5005,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Set catalog failed",
      "Set catalog failed detail={}"),
  CONNECT_INTERRUPTED(
      5006,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Connect interrupted",
      "Connect interrupted detail={}"),
  LOGIN_TIMEOUT_INVALID_USING_DEFAULT(
      5007,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Login timeout invalid using default",
      "Login timeout invalid using default detail={}"),
  JDBC_DRIVER_LOAD_FAILED(
      5008,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Jdbc driver load failed",
      "Jdbc driver load failed detail={}"),
  JDBC_CLASS_NOT_FOUND(
      5009,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Jdbc class not found",
      "Jdbc class not found detail={}"),
  CONN_RELEASE_MONITOR_LOST(
      5010,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Conn release monitor lost",
      "Conn release monitor lost detail={}"),
  SQL_BUILDER_NO_BACK_ENDS(
      5011,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Sql builder no back ends",
      "Sql builder no back ends detail={}"),
  SQL_BUILDER_NO_BACK_END_TABLES(
      5012,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Sql builder no back end tables",
      "Sql builder no back end tables detail={}"),
  SQL_BUILDER_NO_CONN_DEFINED(
      5013,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Sql builder no conn defined",
      "Sql builder no conn defined detail={}"),
  SQL_BUILDER_GET_DATATYPE_EXCEPTION(
      5014,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Sql builder get datatype exception",
      "Sql builder get datatype exception detail={}"),
  SQL_BUILDER_VAR_NOT_TERMINATED(
      5015,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Sql builder var not terminated",
      "Sql builder var not terminated detail={}"),
  SQL_BUILDER_NO_BECOL_IN_MAP(
      5016,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Sql builder no becol in map",
      "Sql builder no becol in map detail={}"),
  SQL_BUILDER_NO_SELECT_COLS_IN_BECOL(
      5017,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Sql builder no select cols in becol",
      "Sql builder no select cols in becol detail={}"),
  SQL_BUILDER_ORDER_BY_COL_NULL(
      5018,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Sql builder order by col null",
      "Sql builder order by col null detail={}"),
  EXEC_PLAN_APP_HANDLER_NULL(
      5019,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exec plan app handler null",
      "Exec plan app handler null detail={}"),
  EXEC_PLAN_DATA_SET_NULL(
      5020,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exec plan data set null",
      "Exec plan data set null detail={}"),
  EXEC_PLAN_PIPES_NULL(
      5021,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exec plan pipes null",
      "Exec plan pipes null detail={}"),
  EXEC_PLAN_NO_QUERY_PIPES(
      5022,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exec plan no query pipes",
      "Exec plan no query pipes detail={}"),
  EXEC_PLAN_MULTIPLE_QUERY_PIPES(
      5023,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exec plan multiple query pipes",
      "Exec plan multiple query pipes detail={}"),
  EXEC_PLAN_NO_BETABLES_IN_PIPE(
      5024,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exec plan no betables in pipe",
      "Exec plan no betables in pipe detail={}"),
  EXEC_DATA_CLOSE_RESULT_SET(
      5025,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exec data close result set",
      "Exec data close result set detail={}"),
  EXEC_DATA_CLOSE_PREP_STMT(
      5026,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exec data close prep stmt",
      "Exec data close prep stmt detail={}"),
  EXEC_DATA_NO_CONNECTIONS(
      5027,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exec data no connections",
      "Exec data no connections detail={}"),
  EXEC_DATA_BAD_CONN_KEY(
      5028,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exec data bad conn key",
      "Exec data bad conn key detail={}"),
  LOAD_DEF_CREDENTIALS_EXCEPTION(
      5029,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Load def credentials exception",
      "Load def credentials exception detail={}"),
  DBPOOL_CONN_INIT_EXCEEDS_MAX(
      5030,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Dbpool conn init exceeds max",
      "Dbpool conn init exceeds max detail={}"),
  DBPOOL_CONN_INIT_EXCEPTION(
      5031,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Dbpool conn init exception",
      "Dbpool conn init exception detail={}"),
  SQL_BUILDER_NO_JOINS(
      5032,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Sql builder no joins",
      "Sql builder no joins detail={}"),
  EXEC_PLAN_MULTIPLE_BETABLES_IN_PIPE(
      5033,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exec plan multiple betables in pipe",
      "Exec plan multiple betables in pipe detail={}"),
  SQL_BUILDER_MOD_SINGLE_TAB_ONLY(
      5034,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Sql builder mod single tab only",
      "Sql builder mod single tab only detail={}"),
  SQL_BUILDER_UDF_NOT_SUPPORTED_IN_MOD(
      5035,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Sql builder udf not supported in mod",
      "Sql builder udf not supported in mod detail={}"),
  SQL_BUILDER_MOD_TABLE_REQD(
      5036,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Sql builder mod table reqd",
      "Sql builder mod table reqd detail={}"),
  SQL_BUILDER_UPDATABLE_COL_REQD(
      5037,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Sql builder updatable col reqd",
      "Sql builder updatable col reqd detail={}"),
  SQL_BUILDER_UPD_OR_DEL_NO_WHERE(
      5038,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Sql builder upd or del no where",
      "Sql builder upd or del no where detail={}"),
  SQL_BUILDER_MOD_MAP_REQD(
      5039,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Sql builder mod map reqd",
      "Sql builder mod map reqd detail={}"),
  EXEC_PLAN_NO_UPDATE_PIPES(
      5040,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exec plan no update pipes",
      "Exec plan no update pipes detail={}"),
  EXEC_PLAN_MULTIPLE_UPDATE_PIPES(
      5041,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exec plan multiple update pipes",
      "Exec plan multiple update pipes detail={}"),
  EXEC_PLAN_COL_UPD_AND_KEY_NOT_SUPPORTED(
      5042,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exec plan col upd and key not supported",
      "Exec plan col upd and key not supported detail={}"),
  LOAD_META_DATA_EXCEPTION(
      5043,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Load meta data exception",
      "Load meta data exception detail={}"),
  NO_LOOKUP_INDEX_DEFINED(
      5044,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "No lookup index defined",
      "No lookup index defined detail={}"),
  UPDATE_MAP_NOT_TO_BECOL(
      5045,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Update map not to becol",
      "Update map not to becol detail={}"),
  DBPOOL_CONN_RELEASE_EXCEPTION(
      5046,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Dbpool conn release exception",
      "Dbpool conn release exception detail={}"),
  DATA_MOD_UNSUPPORTED_FOR_XDEPEND(
      5047,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Data mod unsupported for xdepend",
      "Data mod unsupported for xdepend detail={}"),
  EXEC_PLAN_UPD_COL_NOT_MAPPED(
      5048,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exec plan upd col not mapped",
      "Exec plan upd col not mapped detail={}"),
  EXEC_PLAN_KEY_COL_NOT_MAPPED(
      5049,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exec plan key col not mapped",
      "Exec plan key col not mapped detail={}"),
  DATABASE_ACCESS_ERROR(
      5050,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Database access error",
      "Database access error detail={}"),
  BE_CONN_EXCEPTION(
      5051,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Be conn exception",
      "Be conn exception detail={}"),
  NO_AVAILABLE_BE_CONNS(
      5052,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "No available be conns",
      "No available be conns detail={}"),
  SQL_BUILDER_LITSET_REQD_FOR_OP(
      5053,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Sql builder litset reqd for op",
      "Sql builder litset reqd for op detail={}"),
  SQL_BUILDER_LITSET_INVALID_FOR_OP(
      5054,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Sql builder litset invalid for op",
      "Sql builder litset invalid for op detail={}"),
  SQL_BUILDER_LITSET_WRONG_SIZE(
      5055,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Sql builder litset wrong size",
      "Sql builder litset wrong size detail={}"),
  SQL_BUILDER_LITSET_EMPTY(
      5056,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Sql builder litset empty",
      "Sql builder litset empty detail={}"),
  SQL_BUILDER_ALIAS_UNSUPPORTED(
      5057,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Sql builder alias unsupported",
      "Sql builder alias unsupported detail={}"),
  LOG_PREPARED_STMT(
      5401,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Log prepared stmt",
      "Log prepared stmt detail={}"),
  LOG_BOUND_COL_DATA(
      5402,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Log bound col data",
      "Log bound col data detail={}"),
  NOT_YET_SUPPORTED(
      5999,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Not yet supported",
      "Not yet supported detail={}");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  BackEndErrorCodes(
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
    for (BackEndErrorCodes code : values()) {
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
