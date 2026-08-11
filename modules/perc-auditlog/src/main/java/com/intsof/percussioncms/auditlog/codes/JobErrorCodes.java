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
 * Server job error catalog bridging legacy {@code com.percussion.server.job.IPSJobErrors} ints
 * (1–11: job definition, factory, request, descriptor, config).
 *
 * <p>Every constant sets {@link #isAuditable()} to {@code false}: job-handler failures are
 * operational / deployment noise, not security dual-write events.
 *
 * <p><strong>Flat registry collision:</strong> package-local ints {@code 1–10} already belong to
 * {@link WorkflowErrorCodes} in {@link LegacyErrorCodeRegistry}. This catalog therefore registers
 * only non-colliding ints ({@code CONFIG_FILE_NOT_FOUND = 11}). Call sites should prefer this enum
 * directly until a composite-key registry exists. Module code is {@link AuditModule#SYS}.
 */
public enum JobErrorCodes implements SystemErrorCode {
  JOB_DEFINITION_NOT_FOUND(
      1,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Job definition not found",
      "Job definition not found detail={}"),
  FACTORY_GET_RUNNER(
      2,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Factory get runner",
      "Factory get runner detail={}"),
  INVALID_REQUEST_TYPE(
      3,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid request type",
      "Invalid request type detail={}"),
  UNEXPECTED_ERROR(
      4,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unexpected error",
      "Unexpected error detail={}"),
  NULL_INPUT_DOC(
      5,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Null input doc",
      "Null input doc detail={}"),
  SERVER_REQUEST_PARAM_INVALID(
      6,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Server request param invalid",
      "Server request param invalid detail={}"),
  JOB_ALREADY_RUNNING(
      7,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Job already running",
      "Job already running detail={}"),
  SERVER_REQUEST_MALFORMED(
      8,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Server request malformed",
      "Server request malformed detail={}"),
  INVALID_JOB_ID(
      9,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid job id",
      "Invalid job id detail={}"),
  INVALID_JOB_DESCRIPTOR(
      10,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid job descriptor",
      "Invalid job descriptor detail={}"),
  CONFIG_FILE_NOT_FOUND(
      11,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Config file not found",
      "Config file not found detail={}");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  JobErrorCodes(
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
   * Register non-colliding job ints in {@link LegacyErrorCodeRegistry}. Safe to call repeatedly.
   * Skips package-local ints {@code 1–10} that collide with {@link WorkflowErrorCodes}.
   */
  public static void ensureRegistered() {
    for (JobErrorCodes code : values()) {
      if (code.numericCode <= 10) {
        // Preserve WorkflowErrorCodes ownership of bare ints 1–10 in the flat registry.
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
