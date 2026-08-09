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
package com.intsof.percussioncms.auditlog;

import com.intsof.percussioncms.auditlog.codes.SecurityErrorCodes;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Maps legacy {@code IPS*Errors} integer codes to {@link SystemErrorCode} until catalogs are fully
 * migrated. Unregistered codes are treated as <strong>not auditable</strong> (no dual-write).
 *
 * <p>Phase 2b first slice registers {@link SecurityErrorCodes} (auth/security). Later slices
 * register content/workflow/design catalogs via {@link #register(int, SystemErrorCode)}.
 */
public final class LegacyErrorCodeRegistry {

  /** Same sentinel used by {@link DefaultAuditLogService} when dual-write is skipped. */
  public static final AuditLogId SKIPPED = AuditLogId.of("00000000-0000-0000-0000-000000000000");

  private static final Map<Integer, SystemErrorCode> BY_LEGACY = new ConcurrentHashMap<>();
  private static final AtomicBoolean BOOTSTRAPPED = new AtomicBoolean(false);

  private LegacyErrorCodeRegistry() {}

  /**
   * Ensure Phase 2b auth/security catalog is loaded. Safe to call repeatedly; catalogs register
   * themselves in their own static initializers.
   */
  public static void bootstrap() {
    if (BOOTSTRAPPED.compareAndSet(false, true)) {
      SecurityErrorCodes.ensureRegistered();
    }
  }

  /**
   * Register a legacy int → {@link SystemErrorCode} mapping. Later registration for the same int
   * replaces the previous mapping (last writer wins) so residual slices can refine catalogs.
   *
   * @param legacyCode historical {@code IPS*Errors} constant
   * @param code modern code with explicit {@link SystemErrorCode#isAuditable()}
   */
  public static void register(int legacyCode, SystemErrorCode code) {
    Objects.requireNonNull(code, "code");
    BY_LEGACY.put(legacyCode, code);
  }

  /** Resolve a legacy int to a modern code when this phase (or a later slice) has cataloged it. */
  public static Optional<SystemErrorCode> find(int legacyCode) {
    bootstrap();
    return Optional.ofNullable(BY_LEGACY.get(legacyCode));
  }

  /**
   * Whether dual-write should run for this legacy int. Unregistered codes return {@code false}
   * (safe default — operational only until cataloged).
   */
  public static boolean isAuditable(int legacyCode) {
    bootstrap();
    SystemErrorCode code = BY_LEGACY.get(legacyCode);
    return code != null && code.isAuditable();
  }

  /**
   * Dual-write when the legacy code is registered and {@link SystemErrorCode#isAuditable()};
   * otherwise returns {@link #SKIPPED} without writing to sinks.
   */
  public static AuditLogId logIfAuditable(
      AuditLogService service,
      int legacyCode,
      AuditContext context,
      AuditOutcome outcome,
      Object... params) {
    Objects.requireNonNull(service, "service");
    bootstrap();
    SystemErrorCode code = BY_LEGACY.get(legacyCode);
    if (code == null || !code.isAuditable()) {
      return SKIPPED;
    }
    return service.log(
        code,
        context == null ? AuditContext.empty() : context,
        outcome == null ? code.defaultOutcome() : outcome,
        params);
  }

  /** Convenience when outcome comes from the code default. */
  public static AuditLogId logIfAuditable(
      AuditLogService service, int legacyCode, AuditContext context, Object... params) {
    bootstrap();
    SystemErrorCode code = BY_LEGACY.get(legacyCode);
    AuditOutcome outcome = code != null ? code.defaultOutcome() : AuditOutcome.UNKNOWN;
    return logIfAuditable(service, legacyCode, context, outcome, params);
  }

  /** Test support: clear all mappings and bootstrap flag (re-register catalogs after). */
  static void clearForTests() {
    BY_LEGACY.clear();
    BOOTSTRAPPED.set(false);
  }

  /** Number of registered legacy codes (diagnostics / tests). */
  public static int size() {
    bootstrap();
    return BY_LEGACY.size();
  }
}
