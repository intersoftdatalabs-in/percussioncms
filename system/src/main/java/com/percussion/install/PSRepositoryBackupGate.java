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

import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

/**
 * Evaluates the pre-migration backup gate (FR-018 / QC-007).
 *
 * <p>Gate is satisfied by either:
 *
 * <ul>
 *   <li><strong>FR-018a</strong> — product offline full-dir backup completed successfully, or
 *   <li><strong>FR-018b</strong> — operator affirmative external-backup confirmation via system /
 *       upgrade property {@value #EXTERNAL_BACKUP_CONFIRMED_PROPERTY}{@code =true} (installer
 *       checkbox or CLI {@code -D}). Must be affirmative and non-default; silence never opens the
 *       gate.
 * </ul>
 */
public final class PSRepositoryBackupGate {

  /**
   * Primary UX property for external-backup confirmation (contracts/migration-upgrade.md). Value
   * must be the string {@code true} (case-insensitive).
   */
  public static final String EXTERNAL_BACKUP_CONFIRMED_PROPERTY =
      "perc.migration.externalBackupConfirmed";

  private PSRepositoryBackupGate() {}

  /**
   * Evaluate the backup gate.
   *
   * @param productBackupSucceeded true if product offline backup completed successfully
   * @param systemProperties properties to read for external confirmation (typically {@link
   *     System#getProperties()} or a test map); must not be null
   * @return gate kind; never null
   */
  public static PSBackupGateKind evaluate(
      boolean productBackupSucceeded, Properties systemProperties) {
    Objects.requireNonNull(systemProperties, "systemProperties");
    if (productBackupSucceeded) {
      return PSBackupGateKind.PRODUCT_BACKUP;
    }
    if (isExternalBackupConfirmed(systemProperties)) {
      return PSBackupGateKind.EXTERNAL_CONFIRM;
    }
    return PSBackupGateKind.NOT_SATISFIED;
  }

  /**
   * Whether the gate allows migration to proceed.
   *
   * @param gate result of {@link #evaluate(boolean, Properties)}
   * @return true for PRODUCT_BACKUP or EXTERNAL_CONFIRM
   */
  public static boolean isSatisfied(PSBackupGateKind gate) {
    return gate == PSBackupGateKind.PRODUCT_BACKUP || gate == PSBackupGateKind.EXTERNAL_CONFIRM;
  }

  /**
   * Read external-backup confirmation from properties (and optionally env-style duplicates).
   *
   * @param properties non-null property set
   * @return true only when property is explicitly {@code true}
   */
  public static boolean isExternalBackupConfirmed(Properties properties) {
    Objects.requireNonNull(properties, "properties");
    // Use only the provided Properties (typically System.getProperties() in production).
    // Do not also call System.getProperty here — that would make unit tests non-deterministic.
    String value = properties.getProperty(EXTERNAL_BACKUP_CONFIRMED_PROPERTY);
    if (value == null) {
      return false;
    }
    return "true".equals(value.trim().toLowerCase(Locale.ROOT));
  }
}
