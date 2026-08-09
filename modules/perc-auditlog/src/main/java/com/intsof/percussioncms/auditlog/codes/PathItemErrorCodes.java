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
 * CMS path / folder / item error catalog bridging legacy {@code IPSCmsErrors} ints (13001–14000)
 * used for share/path/item operations.
 *
 * <p>{@link #numericCode()} preserves historical ints. Every constant sets {@link #isAuditable()}
 * explicitly: folder permission / community visibility denials dual-write; operational path and
 * item lookup noise does not.
 *
 * <p>Module code is {@link AuditModule#CONT} so path/item access denials appear under the content
 * audit stream alongside lifecycle events from {@link ContentErrorCodes}.
 */
public enum PathItemErrorCodes implements SystemErrorCode {

  // --- folder / path (permission + operational) ---

  FOLDER_OPERATION_FAILED(
      13005,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Folder operation failed",
      "Folder operation failed operation={}"),

  FOLDER_ERROR_MSG(
      13006,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Folder error: {}",
      "Folder error message={}"),

  FOLDER_PERMISSION_DENIED(
      13007,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "Folder permission denied",
      "Folder permission denied"),

  FOLDER_CREATE_ERROR(
      13008,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "Insufficient privileges to create folder",
      "Folder create privilege error"),

  SITE_LOOKUP_FAILED(
      13009,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Site lookup failed",
      "Site lookup failed resource={} siteId={}"),

  CONTENT_TYPE_NOT_VISIBLE_BY_COMMUNITY(
      13010,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "Content type not visible to community",
      "Content type not visible by community itemId={} revision={} contentType={} community={}"),

  // --- item / path lookup ---

  CONTENT_ITEM_CANNOT_BE_LOCATED(
      13104,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Content item cannot be located",
      "Content item cannot be located contentId={} revision={}"),

  FAIL_GET_PARENT_FOLDER(
      13137,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Failed to get parent folder",
      "Failed to get parent folder"),

  FAIL_OPEN_FOLDER(
      13138,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "Failed to open folder",
      "Failed to open folder"),

  INVALID_FOLDER_ID(
      13139,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid folder id",
      "Invalid folder id={}"),

  CIRCULAR_FOLDER_REFERENCE(
      13141,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Circular folder reference",
      "Circular folder reference"),

  CANNOT_MOVE_FOLDER_TO_ITS_DESCENDENT(
      13142,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Cannot move folder to its descendent",
      "Cannot move folder to its descendent"),

  CANNOT_COPY_FOLDER_TO_ITS_DESCENDENT(
      13143,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Cannot copy folder to its descendent",
      "Cannot copy folder to its descendent"),

  DUPLICATE_ITEM_NAME(
      13212,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Duplicate item name",
      "Duplicate item name={}"),

  INVALID_FOLDER_VALUE(
      13216,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid folder value",
      "Invalid folder value"),

  FOLDER_REL_ERROR_DUPLICATED_CHILDNAME(
      13222,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Duplicate child name in folder relationship",
      "Folder relationship duplicated child name"),

  DUPLICATE_ITEM_NAME_COPY_CREATED(
      13224,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Duplicate item name; copy created",
      "Duplicate item name copy created"),

  FOLDER_REL_INSERT_ERROR_DUPLICATED_CHILDNAME(
      13228,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Duplicate child name on folder relationship insert",
      "Folder relationship insert duplicated child name"),

  INVALID_FOLDER_NAME(
      13235,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid folder name",
      "Invalid folder name={}");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  PathItemErrorCodes(
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
    for (PathItemErrorCodes code : values()) {
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
