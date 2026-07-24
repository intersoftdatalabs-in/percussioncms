/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
 * Canonical migration outcomes for Derby → H2 upgrade (GitHub #548, FR-017).
 *
 * <p>Values must match contracts/migration-observability.md and data-model.md.
 */
public enum PSMigrationOutcome {
  /** Migration completed; live config points at H2; Derby residue retained. */
  SUCCESS,
  /** Migration aborted; Derby config and data remain live; no partial cutover. */
  FAILED,
  /** Backend is not product-managed Derby (e.g. MySQL/MSSQL); no rewrite. */
  SKIPPED_NON_DERBY,
  /** Backup gate not satisfied; migration did not start. */
  BLOCKED_BACKUP_GATE,
  /** Already on H2 (or new default); idempotent no-op. */
  ALREADY_MIGRATED;

  /**
   * Parse a case-insensitive outcome name.
   *
   * @param value raw string; may be null
   * @return matching enum or null if unknown/blank
   */
  public static PSMigrationOutcome fromString(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return PSMigrationOutcome.valueOf(value.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
