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
 * Managed navigation error catalog bridging legacy {@code
 * com.percussion.fastforward.managednav.IPSNavigationErrors} ints (18001–18009; category base
 * 18000).
 *
 * <p>Every constant sets {@link #isAuditable()} to {@code false}: navon/navtree structural
 * failures are operational content-structure noise.
 *
 * <p>Numbering is globally unique in the flat {@link LegacyErrorCodeRegistry}, so all constants
 * are registered. Module code is {@link AuditModule#CONT}.
 */
public enum NavigationErrorCodes implements SystemErrorCode {

  NAVIGATION_SERVICE_FOLDER_ID_NOT_FOUND_FOR_PATH(
      18001,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Folder id not found for path",
      "Nav folder id not found for path path={}"),

  NAVIGATION_SERVICE_CANT_FIND_RELATED_FOLDER_FOR_NAVON(
      18002,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Cannot find related folder for navon",
      "Nav cannot find related folder for navon"),

  NAVIGATION_SERVICE_FAILED_TO_MOVE_SOURCE_NAVON_TO_TARGET(
      18003,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Failed to move source navon to target",
      "Nav failed to move source navon to target"),

  NAVIGATION_SERVICE_FAILED_TO_MOVE_SECTION_BECAUSE_TARGET_ALREADY_HAS_ITEM(
      18004,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Failed to move section; target already has item",
      "Nav failed move section target has item"),

  NAVIGATION_SERVICE_NAVTREE_CANNOT_BE_ADDED_TO_FOLDER_WITH_NAVON(
      18005,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Navtree cannot be added to folder with navon",
      "Nav navtree cannot add to folder with navon"),

  NAVIGATION_SERVICE_NAVTREE_CANNOT_BE_ADDED_TO_FOLDER_WITH_NAVTREE(
      18006,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Navtree cannot be added to folder with navtree",
      "Nav navtree cannot add to folder with navtree"),

  NAVIGATION_SERVICE_ERROR_ADDING_NAVTREE_TO_FOLDER(
      18007,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Error adding navtree to folder",
      "Nav error adding navtree to folder"),

  NAVIGATION_SERVICE_CANNOT_FIND_ANY_NAVONS(
      18008,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Cannot find any navons",
      "Nav cannot find any navons"),

  NAVIGATION_SERVICE_CANNOT_FIND_NAVTREE_FOR_SITE(
      18009,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Cannot find navtree for site",
      "Nav cannot find navtree for site");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  NavigationErrorCodes(
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
    for (NavigationErrorCodes code : values()) {
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
