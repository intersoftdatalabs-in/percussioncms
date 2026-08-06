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

import com.percussion.utils.jdbc.PSJdbcUtils;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

/**
 * Classifies product-managed CMS repository configuration for Derby → H2 migration (#548).
 *
 * <p>Detection rules follow contracts/repository-config.md:
 *
 * <ul>
 *   <li>Derby backend or Derby driver/class → migration candidate
 *   <li>H2 backend → already migrated / new install
 *   <li>Other backends (MySQL, MSSQL, …) → skip migration (FR-009)
 * </ul>
 */
public final class PSEmbeddedRepositoryDetector {

  public static final String KEY_DB_BACKEND = "DB_BACKEND";
  public static final String KEY_DB_DRIVER_NAME = "DB_DRIVER_NAME";
  public static final String KEY_DB_DRIVER_CLASS = "DB_DRIVER_CLASS_NAME";

  private static final String DERBY_EMBEDDED_CLASS = "org.apache.derby.jdbc.EmbeddedDriver";
  private static final String DERBY_CLIENT_CLASS = "org.apache.derby.jdbc.ClientDriver";

  private PSEmbeddedRepositoryDetector() {}

  /** Classification of a repository properties set. */
  public enum Classification {
    /** Product-managed Derby (embedded or networked ClientDriver). */
    PRODUCT_MANAGED_DERBY,
    /** Already on H2 new default. */
    ALREADY_H2,
    /** External or non-Derby backend (MySQL, MSSQL, Oracle, …). */
    NON_DERBY
  }

  /**
   * Classify {@code rxrepository.properties} (or equivalent) content.
   *
   * @param repositoryProperties loaded properties; must not be null
   * @return classification; never null
   */
  public static Classification classify(Properties repositoryProperties) {
    Objects.requireNonNull(repositoryProperties, "repositoryProperties");

    String backend = trimLower(repositoryProperties.getProperty(KEY_DB_BACKEND));
    String driverName = trimLower(repositoryProperties.getProperty(KEY_DB_DRIVER_NAME));
    String driverClass = trim(repositoryProperties.getProperty(KEY_DB_DRIVER_CLASS));

    if (isH2(backend, driverName, driverClass)) {
      return Classification.ALREADY_H2;
    }
    if (isDerby(backend, driverName, driverClass)) {
      return Classification.PRODUCT_MANAGED_DERBY;
    }
    return Classification.NON_DERBY;
  }

  /**
   * Map classification to a migration outcome when no further work is needed (skip paths).
   *
   * @param classification detector result
   * @return {@link PSMigrationOutcome#ALREADY_MIGRATED}, {@link
   *     PSMigrationOutcome#SKIPPED_NON_DERBY}, or null if migration should proceed
   */
  public static PSMigrationOutcome toSkipOutcome(Classification classification) {
    return switch (classification) {
      case ALREADY_H2 -> PSMigrationOutcome.ALREADY_MIGRATED;
      case NON_DERBY -> PSMigrationOutcome.SKIPPED_NON_DERBY;
      case PRODUCT_MANAGED_DERBY -> null;
    };
  }

  private static boolean isH2(String backend, String driverName, String driverClass) {
    if ("h2".equals(backend) || PSJdbcUtils.H2_DB_BACKEND.equalsIgnoreCase(backend)) {
      return true;
    }
    if (PSJdbcUtils.H2_DRIVER.equalsIgnoreCase(driverName)) {
      return true;
    }
    return driverClass != null && driverClass.equalsIgnoreCase(PSJdbcUtils.H2_DRIVER_CLASS);
  }

  private static boolean isDerby(String backend, String driverName, String driverClass) {
    if ("derby".equals(backend) || PSJdbcUtils.DERBY_DB_BACKEND.equalsIgnoreCase(backend)) {
      return true;
    }
    if (PSJdbcUtils.DERBY_DRIVER.equalsIgnoreCase(driverName)) {
      return true;
    }
    if (driverClass == null) {
      return false;
    }
    return DERBY_EMBEDDED_CLASS.equalsIgnoreCase(driverClass)
        || DERBY_CLIENT_CLASS.equalsIgnoreCase(driverClass)
        || driverClass.toLowerCase(Locale.ROOT).contains("derby.jdbc");
  }

  private static String trim(String value) {
    return value == null ? null : value.trim();
  }

  private static String trimLower(String value) {
    String t = trim(value);
    return t == null ? null : t.toLowerCase(Locale.ROOT);
  }
}
