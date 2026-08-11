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
 * Assembly error catalog bridging legacy {@code com.percussion.services.assembly.IPSAssemblyErrors}
 * package-local ints (1–27: template, assembler, slot, finder, binary hash, inline).
 *
 * <p>Every constant sets {@link #isAuditable()} to {@code false}: assembly failures are
 * operational / rendering noise, not security dual-write events.
 *
 * <p><strong>Flat registry collision:</strong> package-local ints {@code 1–10} already belong to
 * {@link WorkflowErrorCodes} in {@link LegacyErrorCodeRegistry}. This catalog therefore registers
 * only non-colliding ints ({@code 11–27}). Call sites should prefer this enum directly for ints
 * {@code 1–10} until a composite-key registry exists. Module code is {@link AuditModule#PUB}.
 */
public enum AssemblyErrorCodes implements SystemErrorCode {

  TEMPLATE_MISSING(
      1,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Template missing",
      "Template missing detail={}"),

  ASSEMBLER_MISSING(
      2,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Assembler missing",
      "Assembler missing detail={}"),

  ASSEMBLER_INST(
      3,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Assembler inst",
      "Assembler inst detail={}"),

  PARAMS_VARIANT_OR_TEMPLATE(
      4,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Params variant or template",
      "Params variant or template detail={}"),

  UNKNOWN_ERROR(
      5,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unknown error",
      "Unknown error detail={}"),

  PARAMS_AUTHTYPE_OR_FILTER(
      6,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Params authtype or filter",
      "Params authtype or filter detail={}"),

  PARAMS_ITEM_SPEC(
      7,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Params item spec",
      "Params item spec detail={}"),

  INVALID_PATH(
      8,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid path",
      "Invalid path detail={}"),

  MISSING_PATH(
      9,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Missing path",
      "Missing path detail={}"),

  UNKNOWN_CRUD_ERROR(
      10,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unknown crud error",
      "Unknown crud error detail={}"),

  MISSING_SLOT(
      11,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Missing slot",
      "Missing slot detail={}"),

  MISSING_FINDER(
      12,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Missing finder",
      "Missing finder detail={}"),

  ITEM_CREATION(
      13,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Item creation",
      "Item creation detail={}"),

  LANDING_PAGE_URL_1(
      14,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Landing page url 1",
      "Landing page url 1 detail={}"),

  MISSING_PAGELINK(
      15,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Missing pagelink",
      "Missing pagelink detail={}"),

  TEMPLATE_BY_ID_MISSING(
      16,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Template by id missing",
      "Template by id missing detail={}"),

  NO_DEFAULT_TEMPLATE(
      17,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "No default template",
      "No default template detail={}"),

  NAME_NOT_UNIQUE(
      18,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Name not unique",
      "Name not unique detail={}"),

  FINDER_ERROR(
      19,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Finder error",
      "Finder error detail={}"),

  PARAMS_ITEM_ID_MISMATCH(
      20,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Params item id mismatch",
      "Params item id mismatch detail={}"),

  PARAMS_ITEM_FOLDER_MISMATCH(
      21,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Params item folder mismatch",
      "Params item folder mismatch detail={}"),

  HASHED_BINARY_NOT_FOUND(
      22,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Hashed binary not found",
      "Hashed binary not found detail={}"),

  HASHED_BINARY_NO_HASH(
      23,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Hashed binary no hash",
      "Hashed binary no hash detail={}"),

  HASHED_BINARY_ERROR(
      24,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Hashed binary error",
      "Hashed binary error detail={}"),

  INLINE_TEMPLATE_NON_HTML(
      25,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Inline template non html",
      "Inline template non html detail={}"),

  INLINE_TEMPLATE_ERROR(
      26,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Inline template error",
      "Inline template error detail={}"),

  INLINE_LINK_ERROR(
      27,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Inline link error",
      "Inline link error detail={}");


  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  AssemblyErrorCodes(
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
   * Register non-colliding assembly ints in {@link LegacyErrorCodeRegistry}. Safe to call repeatedly.
   * Skips package-local ints {@code 1–10} that collide with {@link WorkflowErrorCodes}.
   */
  public static void ensureRegistered() {
    for (AssemblyErrorCodes code : values()) {
      if (code.numericCode <= 10) {
        // Preserve WorkflowErrorCodes ownership of bare ints 1–10 in the flat registry.
        continue;
      }
      LegacyErrorCodeRegistry.register(code.numericCode(), code);
    }
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
