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
 * High-level authentication <em>audit events</em> (login success/failure/logout).
 *
 * <p>Exception catalog codes that bridge legacy {@code IPSSecurityErrors} ints live in {@link
 * SecurityErrorCodes} (Phase 2b). Prefer this enum for intentional audit emits from login
 * servlets; prefer {@link SecurityErrorCodes} / {@code LegacyErrorCodeRegistry} when handling
 * {@code PSException} error codes.
 *
 * <p>Every constant sets {@link #isAuditable()} explicitly.
 */
public enum AuthenticationErrorCodes implements SystemErrorCode {
  LOGIN_SUCCESS(
      1001,
      true,
      AuditEventType.AUTH_LOGIN,
      AuditOutcome.SUCCESS,
      "User {} logged in successfully",
      "Login success actor={} sourceIp={}"),

  LOGIN_FAILURE(
      1002,
      true,
      AuditEventType.AUTH_FAILURE,
      AuditOutcome.FAILURE,
      "Login failed for user {}",
      "Login failure actor={} reason={} sourceIp={}"),

  LOGOUT(
      1003,
      true,
      AuditEventType.AUTH_LOGOUT,
      AuditOutcome.SUCCESS,
      "User {} logged out",
      "Logout actor={} sourceIp={}"),

  /** Session nearing timeout was revoked / released. */
  SESSION_REVOKE(
      1004,
      true,
      AuditEventType.AUTH_SESSION_TIMEOUT,
      AuditOutcome.SUCCESS,
      "Session revoked for user {}",
      "Session revoke actor={} sourceIp={}"),

  /** Non-auditable operational noise example. */
  SESSION_CACHE_MISS(
      1099,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Session cache miss for key {}",
      "Session cache miss key={} detail={}");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  AuthenticationErrorCodes(
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
    return AuditModule.AUTH;
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
