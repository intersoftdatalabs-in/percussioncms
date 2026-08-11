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
 * Delivery error catalog bridging legacy {@code com.percussion.rx.delivery.IPSDeliveryErrors}
 * package-local ints (1–12: abort, directory, copy, temp write, credentials, Amazon, Solr).
 *
 * <p>Every constant sets {@link #isAuditable()} explicitly: credential decrypt failures are
 * security-relevant when observed via this enum; other delivery I/O noise is not.
 *
 * <p><strong>Flat registry collision:</strong> package-local ints {@code 1–10} already belong to
 * {@link WorkflowErrorCodes}; ints {@code 11–12} are claimed by {@link AssemblyErrorCodes} (and
 * residual job catalogs). This catalog therefore does <strong>not</strong> flat-register any
 * ints. Call sites should prefer this enum directly (including {@link
 * #COULD_NOT_DECRYPT_CREDENTIALS}) until a composite-key registry exists. Module code is {@link
 * AuditModule#PUB}.
 */
public enum DeliveryErrorCodes implements SystemErrorCode {

  ABORT_FAILURE(
      1,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Abort failure",
      "Abort failure detail={}"),

  DIR_CANT_CREATE(
      2,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Dir cant create",
      "Dir cant create detail={}"),

  CREATE_DIR_W_EXCEPTION(
      3,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Create dir w exception",
      "Create dir w exception detail={}"),

  COPY_FILE_FAILED(
      4,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Copy file failed",
      "Copy file failed detail={}"),

  UNEXPECTED_ERROR(
      5,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unexpected error",
      "Unexpected error detail={}"),

  COULD_NOT_WRITE_TEMP(
      6,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Could not write temp",
      "Could not write temp detail={}"),

  COULD_NOT_DECRYPT_CREDENTIALS(
      7,
      true,
      AuditEventType.OTHER,
      AuditOutcome.FAILURE,
      "Could not decrypt credentials",
      "Could not decrypt credentials detail={}"),

  COULD_NOT_COPY_TO_AMAMZON(
      8,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Could not copy to amamzon",
      "Could not copy to amamzon detail={}"),

  COULD_NOT_DELETE_FROM_AMAZON(
      9,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Could not delete from amazon",
      "Could not delete from amazon detail={}"),

  BAD_DELIVERY_SERVER_CONFIGURATION(
      10,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Bad delivery server configuration",
      "Bad delivery server configuration detail={}"),

  SOLR_COMMUNICATION_EXCEPTION(
      11,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Solr communication exception",
      "Solr communication exception detail={}"),

  CANNOT_DELIVER_NO_DELIVERYTYPE(
      12,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Cannot deliver no deliverytype",
      "Cannot deliver no deliverytype detail={}");


  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  DeliveryErrorCodes(
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
   * No-op for the flat {@link com.intsof.percussioncms.auditlog.LegacyErrorCodeRegistry}: every
   * package-local delivery int collides with {@link WorkflowErrorCodes} (1–10) or {@link
   * AssemblyErrorCodes} (11–12). Prefer this enum directly for dual-write decisions. Safe to call
   * repeatedly for bootstrap symmetry.
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
