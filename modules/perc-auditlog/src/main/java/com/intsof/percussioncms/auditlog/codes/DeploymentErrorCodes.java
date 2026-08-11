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
 * Deployment / package manager error catalog bridging legacy {@code
 * com.percussion.error.IPSDeploymentErrors} package-local ints (1–85).
 *
 * <p>Every constant sets {@link #isAuditable()} explicitly: exclusive deploy-lock contention codes
 * dual-write as {@link AuditEventType#ACCESS_DENIED}; archive/repository/dependency operational
 * noise does not. Legacy aliases that share an int ({@code SERVER_VERSION_LOWER}/{@code
 * SERVER_VERSION_MISMATCH}, {@code SERVER_VERSION_HIGHER}/{@code SERVER_BUILD_MISMATCH}) are
 * represented once under the primary name.
 *
 * <p><strong>Flat registry collision:</strong> package-local ints {@code 1–73} already belong to
 * {@link WorkflowErrorCodes} (1–10), residual assembly/job (11–27), and {@link
 * WebserviceErrorCodes} (28–73). This catalog flat-registers only non-colliding ints ({@code
 * 74–85}). Prefer this enum for lock codes {@code 46}/{@code 47}/{@code 53} until a composite-key
 * registry exists. Module code is {@link AuditModule#SYS}.
 */
public enum DeploymentErrorCodes implements SystemErrorCode {

  SERVER_VERSION_INVALID(
      1,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Server version invalid: {}",
      "Deploy server version invalid version={}"),

  SERVER_RESPONSE_ELEMENT_MISSING(
      2,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Server response element missing",
      "Deploy server response element missing request={} element={}"),

  SERVER_RESPONSE_ELEMENT_INVALID(
      3,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Server response element invalid",
      "Deploy server response element invalid request={} element={} detail={}"),

  NOT_CONNECTED_ERROR(
      4,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Not connected to server: {}",
      "Deploy not connected server={}"),

  UNEXPECTED_ERROR(
      5,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unexpected deployment error: {}",
      "Deploy unexpected error detail={}"),

  SERVER_RESPONSE_EMPTY(
      6,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Server response empty",
      "Deploy server response empty status={} request={}"),

  SERVER_ERROR_RESPONSE(
      7,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Server error response",
      "Deploy server error response request={} status={} xml={}"),

  NULL_INPUT_DOC(
      8,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Null input document",
      "Deploy null input document"),

  NULL_REPOSITORY_INFO(
      9,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Null repository info",
      "Deploy null repository info"),

  INVALID_REQUEST_TYPE(
      10,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid request type: {}",
      "Deploy invalid request type type={}"),

  ARCHIVE_WRITE_ERROR(
      11,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Archive write error",
      "Deploy archive write error file={} detail={}"),

  ARCHIVE_READ_ERROR(
      12,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Archive read error",
      "Deploy archive read error file={} detail={}"),

  CATALOG_REQD_PROP_NOT_SPECIFIED(
      13,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Catalog required property not specified: {}",
      "Deploy catalog required prop not specified prop={}"),

  SERVER_REQUEST_MALFORMED(
      14,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Server request malformed",
      "Deploy server request malformed element={} detail={}"),

  SERVER_OBJECT_NOT_FOUND(
      15,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Server object not found",
      "Deploy server object not found type={} name={}"),

  SERVER_REQUEST_PARAM_INVALID(
      16,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Server request parameter invalid",
      "Deploy server request param invalid name={} value={}"),

  DEPENDENCY_HANDLER_INIT(
      17,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Dependency handler init failed",
      "Deploy dependency handler init class={} detail={}"),

  CHILD_DEPENDENCY_TYPE_NOT_FOUND(
      18,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Child dependency type not found",
      "Deploy child dependency type not found child={} parent={}"),

  DEPENDENCY_MGR_INIT(
      19,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Dependency manager init failed: {}",
      "Deploy dependency manager init detail={}"),

  DEPENDENCY_DEF_NOT_FOUND(
      20,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Dependency definition not found: {}",
      "Deploy dependency def not found type={}"),

  REPOSITORY_CONNECTION_ERROR(
      21,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Repository connection error: {}",
      "Deploy repository connection error detail={}"),

  REPOSITORY_READ_WRITE_ERROR(
      22,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Repository read/write error: {}",
      "Deploy repository read write error detail={}"),

  INVALID_REPOSITORY_COLUMN_VALUE(
      23,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid repository column value",
      "Deploy invalid repository column table={} column={} value={}"),

  MISSING_ID_MAPPING(
      24,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Missing id mapping",
      "Deploy missing id mapping type={} id={} server={}"),

  MISSING_DEPENDENCY_FILE(
      25,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Missing dependency file",
      "Deploy missing dependency file fileType={} depType={} depId={} depName={}"),

  INVALID_DEPENDENCY_FILE(
      26,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid dependency file",
      "Deploy invalid dependency file fileType={} depType={} depId={} depName={} detail={}"),

  INVALID_SAVED_ID_MAP(
      27,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid saved id map",
      "Deploy invalid saved id map repository={} sourceId={} sourceName={}"),

  UNEXPECTED_EXTRA_ROW(
      28,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unexpected extra row",
      "Deploy unexpected extra row repository={} table={}"),

  UNABLE_FIND_TABLE(
      29,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unable to find table: {}",
      "Deploy unable find table name={}"),

  ID_TYPE_MAP_LOAD(
      30,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Id type map load failed",
      "Deploy id type map load key={} detail={}"),

  SERVER_VERSION_MISMATCH(
      31,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Server version mismatch",
      "Deploy server version mismatch archive={} target={}"),

  SERVER_BUILD_MISMATCH(
      32,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Server build mismatch",
      "Deploy server build mismatch archive={} target={}"),

  ARCHIVE_REF_FOUND(
      33,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Archive ref already exists: {}",
      "Deploy archive ref found name={}"),

  INCOMPLETE_ID_TYPE_MAPPING(
      34,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Incomplete id type mapping",
      "Deploy incomplete id type mapping depType={} depId={} value={} context={}"),

  MISSING_REPOSITORY_ROW(
      35,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Missing repository row",
      "Deploy missing repository row table={} key={} value={}"),

  INCOMPLETE_ID_MAPPING(
      36,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Incomplete id mapping",
      "Deploy incomplete id mapping type={} id={} server={}"),

  INVALID_ID_MAPPING_TARGET(
      37,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid id mapping target",
      "Deploy invalid id mapping target type={} id={} server={} targetId={}"),

  VALUE_NOT_FOUND_IN_TABLE(
      38,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Value not found in table",
      "Deploy value not found in table value={} table={} column={}"),

  CHILD_DEP_NOT_FOUND(
      39,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Child dependency not found",
      "Deploy child dep not found childId={} childType={} objectId={} objectType={}"),

  NO_ROWS_TO_PROCESS(
      40,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "No rows to process",
      "Deploy no rows to process"),

  WRONG_DEPENDENCY_FILE_TYPE(
      41,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Wrong dependency file type",
      "Deploy wrong dependency file type actual={} expected={}"),

  MISSING_ID_TYPES(
      42,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Missing id types",
      "Deploy missing id types depType={} depId={}"),

  DEP_OBJECT_NOT_FOUND(
      45,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Dependency object not found",
      "Deploy dep object not found package={} id={} type={} name={}"),

  LOCK_ALREADY_HELD(
      46,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "Deployment lock already held",
      "Deploy lock already held"),

  LOCK_NOT_EXTENSIBLE_TAKEN(
      47,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "Deployment lock not extensible; taken by another",
      "Deploy lock not extensible taken"),

  CANNOT_FIND_DATA(
      49,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Cannot find data",
      "Deploy cannot find data table={} filter={}"),

  CATALOG_INVALID_DIRECTORY_SPECIFIED(
      50,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid catalog directory: {}",
      "Deploy catalog invalid directory path={}"),

  MAX_DEP_COUNT_EXCEEDED(
      51,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Max dependency count exceeded: {}",
      "Deploy max dep count exceeded max={}"),

  EMPTY_PACKAGE_LIST(
      52,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Empty package list",
      "Deploy empty package list"),

  LOCK_NOT_EXTENSIBLE_TAKEN_RELEASED(
      53,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "Deployment lock not extensible; taken and released",
      "Deploy lock not extensible taken released lastUser={}"),

  LOCK_NOT_RELEASED(
      54,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Lock not released",
      "Deploy lock not released server={} detail={}"),

  MULTISERVER_MANAGER_DISABLED(
      55,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Multi-server manager disabled",
      "Deploy multi-server manager disabled"),

  APP_FILE_LOAD(
      56,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Application file load error",
      "Deploy app file load file={} app={} detail={}"),

  SERVER_RESPONSE_PARSE_ERROR(
      57,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Server response parse error",
      "Deploy server response parse error request={} detail={}"),

  SERVER_NOT_AVAILABLE(
      58,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Server not available",
      "Deploy server not available"),

  EXTRACT_ID_FROM_KEY(
      59,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Extract id from key failed",
      "Deploy extract id from key component={} type={}"),

  ASSIGN_NEW_KEY(
      60,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Assign new key failed",
      "Deploy assign new key component={} type={} detail={}"),

  MISSING_REQUIRED_CACHE_DATA(
      61,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Missing required cache data",
      "Deploy missing required cache data contentId={} column={} table={}"),

  FAILED_GET_NUMERIC_CACHED_DATA(
      62,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Failed get numeric cached data",
      "Deploy failed get numeric cached data contentId={} table={} detail={}"),

  SLOT_DEFINITION_ALREADY_EXISTS(
      63,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Slot definition already exists: {}",
      "Deploy slot definition already exists slotId={}"),

  APP_DEFINITION_DOESNOT_EXIST(
      64,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Application definition does not exist: {}",
      "Deploy app definition does not exist name={}"),

  CANNOT_FIND_DEP_DEF(
      65,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Cannot find dependency definition: {}",
      "Deploy cannot find dep def objectType={}"),

  DEP_DEF_NOT_DEPLOYABLE(
      66,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Dependency definition not deployable: {}",
      "Deploy dep def not deployable objectType={}"),

  CANNOT_FIND_PARENT_DEP_DEF(
      67,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Cannot find parent dependency definition: {}",
      "Deploy cannot find parent dep def objectType={}"),

  PARENT_DEP_DEF_NOT_DEPLOYABLE(
      68,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Parent dependency definition not deployable: {}",
      "Deploy parent dep def not deployable objectType={}"),

  INCOMPLATE_ORDER_DEF(
      69,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Incomplete order definition",
      "Deploy incomplete order def"),

  INVALID_NUM_PARENT_DEFS(
      70,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid number of parent definitions",
      "Deploy invalid num parent defs"),

  UNEXPECTED_PARENT_TYPE(
      71,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unexpected parent type",
      "Deploy unexpected parent type actual={} expected={}"),

  INVALID_NUM_CHILD_DEFS(
      72,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid number of child definitions",
      "Deploy invalid num child defs objectType={}"),

  MISSING_PKG_GUID(
      73,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Missing package GUID",
      "Deploy missing package guid"),

  VERSION_LOWER_THAN_INSTALLED(
      74,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Package version lower than installed",
      "Deploy version lower than installed new={} installed={}"),

  PKG_DEP_VALIDATION(
      76,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Package dependency validation failed",
      "Deploy package dep validation missing={}"),

  PKG_DEP_VERSION_VALIDATION(
      77,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Package dependency version validation failed",
      "Deploy package dep version validation detail={}"),

  CONFIG_DOES_NOT_EXIST(
      78,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Configuration does not exist: {}",
      "Deploy config does not exist path={}"),

  PACKAGE_CREATED_ON_SYSTEM(
      79,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Package was created on system: {}",
      "Deploy package created on system name={}"),

  CONTROL_NOT_PACKAGEABLE(
      80,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Control not packageable: {}",
      "Deploy control not packageable name={}"),

  UNABLE_TO_CONNECT_TO_SERVER(
      81,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unable to connect to server",
      "Deploy unable to connect to server"),

  FAILED_TO_CREATE_COMPONENT_COMMUNITY_ASSNS(
      82,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Failed to create component community associations: {}",
      "Deploy failed create component community assns community={}"),

  NO_TYPE_MAPPING_FOR_GUID(
      83,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "No type mapping for GUID",
      "Deploy no type mapping for guid"),

  MISSING_VALIDATION_RESULTS(
      84,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Missing validation results",
      "Deploy missing validation results package={} depId={}"),

  WRONG_FORMAT_FOR_PAIRID_DEP_ID(
      85,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Wrong format for pair id dependency id: {}",
      "Deploy wrong format for pairid dep id pairId={}");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  DeploymentErrorCodes(
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
   * Register non-colliding deployment ints in {@link LegacyErrorCodeRegistry}. Safe to call
   * repeatedly. Skips package-local ints {@code 1–73} that collide with workflow / assembly /
   * webservice catalogs in the flat map.
   */
  public static void ensureRegistered() {
    for (DeploymentErrorCodes code : values()) {
      if (code.numericCode <= 73) {
        // Preserve WF (1–10), assembly/job (11–27), webservice (28–73) ownership.
        continue;
      }
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
