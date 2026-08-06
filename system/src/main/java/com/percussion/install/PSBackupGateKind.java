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
package com.percussion.install;

/**
 * How the pre-migration backup gate (FR-018) was satisfied for #548.
 *
 * <p>Canonical values for durable migration reports (contracts/migration-observability.md).
 */
public enum PSBackupGateKind {
  /** Product-produced offline full-directory pre-migration backup succeeded. */
  PRODUCT_BACKUP,
  /**
   * Operator affirmed external backup via {@code perc.migration.externalBackupConfirmed=true}
   * (installer checkbox or CLI {@code -D}).
   */
  EXTERNAL_CONFIRM,
  /** Neither product backup nor external confirmation; migration blocked. */
  NOT_SATISFIED,
  /**
   * Backup gate was not evaluated (e.g. skip paths {@code ALREADY_MIGRATED} / {@code
   * SKIPPED_NON_DERBY}). Durable reports use this instead of leaving the field blank.
   */
  NOT_EVALUATED;

  /**
   * Parse a case-insensitive gate kind.
   *
   * @param value raw string; may be null
   * @return matching enum or null if unknown/blank
   */
  public static PSBackupGateKind fromString(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return PSBackupGateKind.valueOf(value.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
