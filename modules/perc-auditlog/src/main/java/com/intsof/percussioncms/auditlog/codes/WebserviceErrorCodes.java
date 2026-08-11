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
 * Modern SOAP / design webservices error catalog bridging legacy {@code
 * com.percussion.webservices.IPSWebserviceErrors} package-local ints (1–73).
 *
 * <p>Every constant sets {@link #isAuditable()} explicitly: session and ACL / authorization
 * failures dual-write; design CRUD and lookup operational noise does not.
 *
 * <p><strong>Flat registry collision:</strong> package-local ints {@code 1–10} already belong to
 * {@link WorkflowErrorCodes}; ints {@code 11–27} are claimed by residual assembly / job catalogs
 * (PR #2867 / #2866). This catalog therefore flat-registers only non-colliding ints ({@code
 * 28–73}), which includes the security-relevant {@code ACCESS_CONTROL_ERROR} and {@code
 * NOT_AUTHORIZED} codes. Prefer this enum directly for ints {@code 1–27} until a composite-key
 * registry exists. Module code is {@link AuditModule#SYS}.
 */
public enum WebserviceErrorCodes implements SystemErrorCode {

  INVALID_CONTRACT(
      1,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid contract",
      "Invalid contract detail={}"),

  OBJECT_NOT_FOUND(
      2,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Object not found",
      "Object not found detail={}"),

  INVALID_SESSION(
      3,
      true,
      AuditEventType.AUTH_FAILURE,
      AuditOutcome.FAILURE,
      "Invalid session",
      "Invalid session detail={}"),

  MISSING_SESSION(
      4,
      true,
      AuditEventType.AUTH_FAILURE,
      AuditOutcome.FAILURE,
      "Missing session",
      "Missing session detail={}"),

  CREATE_LOCK_FAILED(
      5,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Create lock failed",
      "Create lock failed detail={}"),

  SAVE_FAILED(
      6,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Save failed",
      "Save failed detail={}"),

  DELETE_FAILED(
      7,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Delete failed",
      "Delete failed detail={}"),

  OBJECT_NOT_LOCKED(
      8,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Object not locked",
      "Object not locked detail={}"),

  OBJECT_NOT_LOCKED_FOR_REQUESTOR(
      9,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Object not locked for requestor",
      "Object not locked for requestor detail={}"),

  CREATE_EXTEND_LOCK_FAILED(
      10,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Create extend lock failed",
      "Create extend lock failed detail={}"),

  OBJECT_ALREADY_EXISTS(
      11,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Object already exists",
      "Object already exists detail={}"),

  MISSING_HIERARCHY_NODE_FOR_PARENT(
      12,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Missing hierarchy node for parent",
      "Missing hierarchy node for parent detail={}"),

  DUPLICATE_HIERARCHY_NODE_FOR_PARENT(
      13,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Duplicate hierarchy node for parent",
      "Duplicate hierarchy node for parent detail={}"),

  DELETE_FAILED_DEPENDENTS(
      14,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Delete failed dependents",
      "Delete failed dependents detail={}"),

  LOAD_FAILED(
      15,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Load failed",
      "Load failed detail={}"),

  FIND_FAILED(
      16,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Find failed",
      "Find failed detail={}"),

  FAILED_LOAD_REL_CONFIGS(
      17,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Failed load rel configs",
      "Failed load rel configs detail={}"),

  FAILED_COMMUNITY_VISIBILITY_LOOKUP(
      18,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Failed community visibility lookup",
      "Failed community visibility lookup detail={}"),

  UNSUPPORTD_COMMUNITY_VISIBILITY_LOOKUP_TYPE(
      19,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unsupportd community visibility lookup type",
      "Unsupportd community visibility lookup type detail={}"),

  FAILED_LOAD_WORKFLOW(
      20,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Failed load workflow",
      "Failed load workflow detail={}"),

  CANNOT_FIND_WORKFLOW_STATE_ID(
      21,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Cannot find workflow state id",
      "Cannot find workflow state id detail={}"),

  CURR_STATE_NOT_MATCH(
      22,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Curr state not match",
      "Curr state not match detail={}"),

  CANNOT_FIND_TRANS_TO_QE_STATE(
      23,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Cannot find trans to qe state",
      "Cannot find trans to qe state detail={}"),

  CANNOT_FIND_TRANS_4_STATE_2_STATE(
      24,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Cannot find trans 4 state 2 state",
      "Cannot find trans 4 state 2 state detail={}"),

  FAILED_TRANSITION_ITEM(
      25,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Failed transition item",
      "Failed transition item detail={}"),

  FAILED_CHECK_OUT_ITEM(
      26,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Failed check out item",
      "Failed check out item detail={}"),

  FAILED_CHECK_IN_ITEM(
      27,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Failed check in item",
      "Failed check in item detail={}"),

  FAILED_SAVE_RELATIONSHIPS(
      28,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Failed save relationships",
      "Failed save relationships detail={}"),

  FAILED_LOAD_RELATIONSHIP(
      29,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Failed load relationship",
      "Failed load relationship detail={}"),

  CANNOT_FIND_RELATIONSHIP(
      30,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Cannot find relationship",
      "Cannot find relationship detail={}"),

  FAILED_DELETE_RELATIONSHIPS(
      31,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Failed delete relationships",
      "Failed delete relationships detail={}"),

  ACCESS_CONTROL_ERROR(
      32,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "Access control error",
      "Access control error detail={}"),

  LOAD_OBJECTS_ERROR(
      33,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Load objects error",
      "Load objects error detail={}"),

  OBJECT_NOT_FOUND_BY_NAME(
      34,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Object not found by name",
      "Object not found by name detail={}"),

  NO_FOLDER_PATH_FOR_FOLDERID(
      35,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "No folder path for folderid",
      "No folder path for folderid detail={}"),

  FAILED_LOAD_FOLDER_PATH(
      36,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Failed load folder path",
      "Failed load folder path detail={}"),

  ITEM_NOT_CHECKED_OUT(
      37,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "Item not checked out",
      "Item not checked out detail={}"),

  INVALID_CHILD_ID(
      38,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid child id",
      "Invalid child id detail={}"),

  CHILD_ENTRY_NOT_FOUND(
      39,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Child entry not found",
      "Child entry not found detail={}"),

  CHILD_ENTRY_ALREADY_EXISTS(
      40,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Child entry already exists",
      "Child entry already exists detail={}"),

  UNKNOWN_CONTENT_TYPE(
      41,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unknown content type",
      "Unknown content type detail={}"),

  FAILED_FIND_ID_FROM_PATH(
      42,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Failed find id from path",
      "Failed find id from path detail={}"),

  PATH_NOT_EXIST(
      43,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Path not exist",
      "Path not exist detail={}"),

  UNKNOWN_COMMUNITY_ID(
      44,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unknown community id",
      "Unknown community id detail={}"),

  FAILED_ADD_FOLDER_CHILDREN(
      45,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Failed add folder children",
      "Failed add folder children detail={}"),

  FAILED_FIND_FOLDER_CHILDREN(
      46,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Failed find folder children",
      "Failed find folder children detail={}"),

  FAILED_FIND_CHILD_ITEMS(
      47,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Failed find child items",
      "Failed find child items detail={}"),

  FAILED_FIND_PARENT_ITEMS(
      48,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Failed find parent items",
      "Failed find parent items detail={}"),

  ITEM_NOT_CHECKOUT_BY_USER(
      49,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "Item not checkout by user",
      "Item not checkout by user detail={}"),

  INVALID_EDIT_REVISION(
      50,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid edit revision",
      "Invalid edit revision detail={}"),

  INVALID_CURRENT_REVISION(
      51,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid current revision",
      "Invalid current revision detail={}"),

  USER_NOT_MEMBER_COMMUNITY(
      52,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "User not member community",
      "User not member community detail={}"),

  INVALID_LOCALE(
      53,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid locale",
      "Invalid locale detail={}"),

  INVALID_FOLDER_CHILD(
      54,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid folder child",
      "Invalid folder child detail={}"),

  FAILED_FIND_PATH_FROM_ID(
      55,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Failed find path from id",
      "Failed find path from id detail={}"),

  FAILED_SAVE_ITEM(
      56,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Failed save item",
      "Failed save item detail={}"),

  FAILED_LOAD_ITEM(
      57,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Failed load item",
      "Failed load item detail={}"),

  ITEM_NOT_CHECKED_IN(
      58,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "Item not checked in",
      "Item not checked in detail={}"),

  INAVLID_ACTION_FOR_STATE(
      59,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Inavlid action for state",
      "Inavlid action for state detail={}"),

  NEWCOPY_FAILED(
      60,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Newcopy failed",
      "Newcopy failed detail={}"),

  NEWPROMOTABLEVERSION_FAILED(
      61,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Newpromotableversion failed",
      "Newpromotableversion failed detail={}"),

  NEWTRANSLATION_FAILED(
      62,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Newtranslation failed",
      "Newtranslation failed detail={}"),

  FAILED_VIEW_ITEM(
      63,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Failed view item",
      "Failed view item detail={}"),

  UNKNOWN_FIELD_NAME(
      64,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unknown field name",
      "Unknown field name detail={}"),

  FAILED_SYS_SHARED_DEF_VALIDATION(
      65,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Failed sys shared def validation",
      "Failed sys shared def validation detail={}"),

  DELETE_ASSOCIATION_FAILED_DEPENDENTS(
      66,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Delete association failed dependents",
      "Delete association failed dependents detail={}"),

  FAILED_LOAD_FOLDER(
      67,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Failed load folder",
      "Failed load folder detail={}"),

  OPERATION_FAILED_ERROR(
      68,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Operation failed error",
      "Operation failed error detail={}"),

  UNEXPECTED_ERROR(
      69,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unexpected error",
      "Unexpected error detail={}"),

  UNABLE_SAVE_SHARED_DEF_VALIDATION(
      70,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unable save shared def validation",
      "Unable save shared def validation detail={}"),

  FAILED_RENAMING_ACLS(
      71,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Failed renaming acls",
      "Failed renaming acls detail={}"),

  NOT_AUTHORIZED(
      72,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "Not authorized",
      "Not authorized detail={}"),

  FAILED_TO_OBTAIN_PATH_FROM_OBJECT_ID(
      73,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Failed to obtain path from object id",
      "Failed to obtain path from object id detail={}");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  WebserviceErrorCodes(
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
   * Register non-colliding webservice ints in {@link LegacyErrorCodeRegistry}. Safe to call
   * repeatedly. Skips package-local ints {@code 1–27} that collide with workflow / assembly / job
   * catalogs in the flat map.
   */
  public static void ensureRegistered() {
    for (WebserviceErrorCodes code : values()) {
      if (code.numericCode <= 27) {
        // Preserve WorkflowErrorCodes (1–10) and residual assembly/job ownership of 11–27.
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
