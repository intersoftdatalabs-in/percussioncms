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
 * Content lifecycle audit codes (create / update / delete / recycle / schedule). Outcome is set at
 * the call site when success and failure share the same code.
 */
public enum ContentErrorCodes implements SystemErrorCode {
  CREATE(
      2001,
      true,
      AuditEventType.CONTENT_CREATE,
      AuditOutcome.SUCCESS,
      "Content item {} created",
      "Content create guid={} contentId={} path={}"),

  UPDATE(
      2002,
      true,
      AuditEventType.CONTENT_UPDATE,
      AuditOutcome.SUCCESS,
      "Content item {} updated",
      "Content update guid={} contentId={} path={}"),

  DELETE(
      2003,
      true,
      AuditEventType.CONTENT_DELETE,
      AuditOutcome.SUCCESS,
      "Content item {} deleted",
      "Content delete guid={} contentId={} path={}"),

  RECYCLE(
      2004,
      true,
      AuditEventType.CONTENT_RECYCLE,
      AuditOutcome.SUCCESS,
      "Content item {} recycled",
      "Content recycle guid={} contentId={} path={}"),

  PAGE_PUBLISH_SCHEDULE(
      2005,
      true,
      AuditEventType.CONTENT_PUBLISH,
      AuditOutcome.SUCCESS,
      "Page publish scheduled for {}",
      "Page publish schedule guid={} contentId={} path={}"),

  PAGE_REMOVAL_SCHEDULE(
      2006,
      true,
      AuditEventType.CONTENT_UPDATE,
      AuditOutcome.SUCCESS,
      "Page removal scheduled for {}",
      "Page removal schedule guid={} contentId={} path={}");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  ContentErrorCodes(
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
