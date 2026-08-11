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
 * Error catalog bridging legacy {@code com.percussion.conn.IPSConnectionErrors} ints
 * (3001–3012, 3101–3107: client connection / socket / response).
 *
 * <p>{@link #numericCode()} preserves historical ints. Every constant sets {@link #isAuditable()}
 * explicitly. Only {@link #UNAUTHORIZED} dual-writes; port/host/socket/config noise does not. Numeric ints 3001–3005 overlap Phase-2a {@link UserManagementErrorCodes} package-local numbers; those USER codes are not flat-registered, so legacy connection ints own the flat registry keys.
 *
 * <p>All ints in this catalog are flat-registered in {@link LegacyErrorCodeRegistry} (no
 * package-local collision with already-bootstrapped catalogs). Module code is
 * {@link AuditModule#SYS}.
 */
public enum ConnectionErrorCodes implements SystemErrorCode {

  PORT_NUMBER_INVALID(
      3001,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Port number invalid",
      "Port number invalid detail={}"),
  PORT_NUMBER_REQD(
      3002,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Port number reqd",
      "Port number reqd detail={}"),
  HOST_ADDRESS_INVALID(
      3003,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Host address invalid",
      "Host address invalid detail={}"),
  HOST_ADDRESS_REQD(
      3004,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Host address reqd",
      "Host address reqd detail={}"),
  QUEUE_LIMIT_INVALID(
      3005,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Queue limit invalid",
      "Queue limit invalid detail={}"),
  CONN_ALREADY_CLOSED(
      3006,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Conn already closed",
      "Conn already closed detail={}"),
  CONN_PROPS_REQD(
      3007,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Conn props reqd",
      "Conn props reqd detail={}"),
  SERVER_NOT_RESPONDING(
      3008,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Server not responding",
      "Server not responding detail={}"),
  CONN_ALREADY_OPENED(
      3009,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Conn already opened",
      "Conn already opened detail={}"),
  CONN_PROP_MISSING(
      3010,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Conn prop missing",
      "Conn prop missing detail={}"),
  UNSUPPORTED_SSL_CIPHER(
      3011,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unsupported ssl cipher",
      "Unsupported ssl cipher detail={}"),
  SOCKET_TIMEOUT_INVALID(
      3012,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Socket timeout invalid",
      "Socket timeout invalid detail={}"),
  UNKNOWN_SERVER_EXCEPTION(
      3101,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unknown server exception",
      "Unknown server exception detail={}"),
  SERVER_GENERATED_EXCEPTION(
      3102,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Server generated exception",
      "Server generated exception detail={}"),
  RESPONSE_INVALID_MIME_TYPE(
      3103,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Response invalid mime type",
      "Response invalid mime type detail={}"),
  RESPONSE_PARSE_EXCEPTION(
      3104,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Response parse exception",
      "Response parse exception detail={}"),
  RESPONSE_PARSE_EXCEPTION_NOLINEINFO(
      3105,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Response parse exception nolineinfo",
      "Response parse exception nolineinfo detail={}"),
  NULL_SOCKET(
      3106,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Null socket",
      "Null socket detail={}"),
  UNAUTHORIZED(
      3107,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "Unauthorized",
      "Unauthorized detail={}");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  ConnectionErrorCodes(
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
    for (ConnectionErrorCodes code : values()) {
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
