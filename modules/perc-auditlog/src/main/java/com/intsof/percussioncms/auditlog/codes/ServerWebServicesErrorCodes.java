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
 * Legacy server webservices error catalog bridging {@code
 * com.percussion.server.webservices.IPSWebServicesErrors} ints (14001–14026).
 *
 * <p>Every constant sets {@link #isAuditable()} explicitly: login failure and unauthorized client /
 * checkout-user denials dual-write; content/search operational noise does not.
 *
 * <p>All ints are globally unique in the historical error map and are fully flat-registered in
 * {@link LegacyErrorCodeRegistry}. Module code is {@link AuditModule#SYS}.
 */
public enum ServerWebServicesErrorCodes implements SystemErrorCode {

  WEB_SERVICE_CONTENT_ITEM_NOT_FOUND(
      14001,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Web service content item not found",
      "Web service content item not found detail={}"),

  WEB_SERVICE_CHECKOUT_FAILURE(
      14002,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Web service checkout failure",
      "Web service checkout failure detail={}"),

  WEB_SERVICE_CONTENT_TYPE_NOT_FOUND(
      14003,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Web service content type not found",
      "Web service content type not found detail={}"),

  WEB_SERVICE_INSERT_FAILURE(
      14004,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Web service insert failure",
      "Web service insert failure detail={}"),

  WEB_SERVICE_CHECKOUT_USER_FAILURE(
      14005,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "Web service checkout user failure",
      "Web service checkout user failure detail={}"),

  WEB_SERVICE_TRANSITION_NOT_FOUND(
      14006,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Web service transition not found",
      "Web service transition not found detail={}"),

  WEB_SERVICE_TRANSITION_COMMENT_REQUIRED(
      14007,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Web service transition comment required",
      "Web service transition comment required detail={}"),

  WEB_SERVICE_VALIDATION_FAILURE(
      14008,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Web service validation failure",
      "Web service validation failure detail={}"),

  WEB_SERVICE_INTERNAL_SEARCH_NOT_FOUND(
      14009,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Web service internal search not found",
      "Web service internal search not found detail={}"),

  WEB_SERVICE_LOGIN_FAILURE(
      14010,
      true,
      AuditEventType.AUTH_FAILURE,
      AuditOutcome.FAILURE,
      "Web service login failure",
      "Web service login failure detail={}"),

  WEB_SERVICE_INVALID_CLIENT_ACESS(
      14011,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "Web service invalid client acess",
      "Web service invalid client acess detail={}"),

  WEB_SERVICE_INVALID_SEARCH_PARAMS(
      14012,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Web service invalid search params",
      "Web service invalid search params detail={}"),

  WEB_SERVICE_INVALID_SEARCH_CONTENTTYPE(
      14013,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Web service invalid search contenttype",
      "Web service invalid search contenttype detail={}"),

  WEB_SERVICE_INTERNAL_REQUEST_FAILED(
      14014,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Web service internal request failed",
      "Web service internal request failed detail={}"),

  WEB_SERVICE_ACTION_NOT_FOUND(
      14015,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Web service action not found",
      "Web service action not found detail={}"),

  WEB_SERVICE_ITEM_CHILD_NOT_FOUND(
      14016,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Web service item child not found",
      "Web service item child not found detail={}"),

  WEB_SERVICE_MISSING_ELEMENT(
      14017,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Web service missing element",
      "Web service missing element detail={}"),

  WEB_SERVICE_MISSING_PARAMETER(
      14018,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Web service missing parameter",
      "Web service missing parameter detail={}"),

  WEB_SERVICE_DISPATCH_ERROR(
      14019,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Web service dispatch error",
      "Web service dispatch error detail={}"),

  WEB_SERVICE_MISSING_ID(
      14020,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Web service missing id",
      "Web service missing id detail={}"),

  INVALID_MIXED_CHILD_IDS(
      14021,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid mixed child ids",
      "Invalid mixed child ids detail={}"),

  WEB_SERVICE_INVALID_FOLDER(
      14022,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Web service invalid folder",
      "Web service invalid folder detail={}"),

  WEB_SERVICE_PROMOTE_FAILED_CHECKOUT(
      14023,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Web service promote failed checkout",
      "Web service promote failed checkout detail={}"),

  WEB_SERVICE_PROMOTE_FAILED_CHECKIN(
      14024,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Web service promote failed checkin",
      "Web service promote failed checkin detail={}"),

  WEB_SERVICE_INTERNAL_REQUEST_NOT_FOUND(
      14025,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Web service internal request not found",
      "Web service internal request not found detail={}"),

  WEB_SERVICE_SEARCH_RESOURCE_NOT_FOUND(
      14026,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Web service search resource not found",
      "Web service search resource not found detail={}");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  ServerWebServicesErrorCodes(
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
    for (ServerWebServicesErrorCodes code : values()) {
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
