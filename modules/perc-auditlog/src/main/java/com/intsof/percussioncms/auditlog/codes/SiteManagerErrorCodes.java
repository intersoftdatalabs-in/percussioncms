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
 * Site manager error catalog bridging legacy {@code
 * com.percussion.services.sitemgr.IPSSiteManagerErrors} package-local ints (1–9).
 *
 * <p>Every constant sets {@link #isAuditable()} to {@code false}: site/scheme/context lookup
 * failures are operational noise, not security dual-write events.
 *
 * <p><strong>Flat registry collision:</strong> package-local ints {@code 1–9} already belong to
 * {@link WorkflowErrorCodes}. This catalog does <strong>not</strong> flat-register any ints.
 * Prefer this enum directly. Module code is {@link AuditModule#PUB}.
 */
public enum SiteManagerErrorCodes implements SystemErrorCode {

  SITE_ID_NOT_EXIST(
      1,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Site id not found: {}",
      "Site id not found siteId={}"),

  FAILED_FIND_ROOT_FOLDER_ID(
      2,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Failed to find root folder id for site",
      "Failed find root folder id siteId={} path={} detail={}"),

  CANNOT_FIND_ROOT_FOLDER_ID(
      3,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Cannot find root folder id for site",
      "Cannot find root folder id siteId={} path={}"),

  FAILED_GET_FOLDER_PATH(
      4,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Failed to get folder path",
      "Failed get folder path folderId={} detail={}"),

  NOT_SITE_FOLDER(
      5,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Folder is not under site",
      "Not site folder folderId={} siteId={} rootPath={}"),

  UNEXPECTED_ERROR(
      6,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unexpected site manager error: {}",
      "Site manager unexpected error detail={}"),

  SITE_NAME_NOT_EXIST(
      7,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Site name not found: {}",
      "Site name not found name={}"),

  SCHEME_NOT_EXIST(
      8,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Scheme not found: {}",
      "Scheme not found schemeId={}"),

  NO_SUCH_CONTEXT(
      9,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Publishing context not found",
      "No such context kind={} data={}");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  SiteManagerErrorCodes(
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
   * No-op for the flat registry: package-local ints collide with earlier Phase 2b catalogs. Prefer
   * this enum directly. Safe to call repeatedly for bootstrap symmetry.
   */
  public static void ensureRegistered() {
    // Intentionally empty — package-local ints are not flat-registered.
  }

  @Override
  public AuditModule module() {
    return AuditModule.PUB;
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
