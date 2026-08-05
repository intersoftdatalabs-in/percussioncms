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
package com.percussion.distribution.install;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Regression guard for GH-1547: the installer ships {@code migration_i18n_locales.xml} and both
 * install/upgrade chains invoke it. Uses relative {@link Path} only (cross-platform).
 */
@Tag("UnitTest")
class I18nLocaleMigrationWiringTest {

  private static final Path INSTALLER =
      Path.of("src", "main", "resources", "distribution", "rxconfig", "Installer");

  private static final Path MIGRATION_XML = INSTALLER.resolve("migration_i18n_locales.xml");
  private static final Path INSTALL_XML = INSTALLER.resolve("install.xml");

  @Test
  void migrationScriptIsShipped() throws IOException {
    assertTrue(Files.isRegularFile(MIGRATION_XML), "missing " + MIGRATION_XML);
    String body = Files.readString(MIGRATION_XML, StandardCharsets.UTF_8);
    assertTrue(body.contains("PSMigrateI18nLocaleCodes"), "migration must use Ant task");
    assertTrue(body.contains("migrateI18nLocales"), "default target name");
    assertTrue(body.contains("i18n.locale.migration.dryRun"), "dry-run property");
  }

  @Test
  void installAndUpgradeChainsInvokeMigration() throws IOException {
    assertTrue(Files.isRegularFile(INSTALL_XML), "missing " + INSTALL_XML);
    String body = Files.readString(INSTALL_XML, StandardCharsets.UTF_8);
    assertTrue(
        body.contains("migration_i18n_locales.xml"),
        "install.xml must reference migration_i18n_locales.xml");
    // Both chains should mention the antfile (count >= 2).
    int idx = 0;
    int hits = 0;
    while ((idx = body.indexOf("migration_i18n_locales.xml", idx)) >= 0) {
      hits++;
      idx += "migration_i18n_locales.xml".length();
    }
    assertTrue(hits >= 2, "expected install.chain + upgrade.chain wiring, hits=" + hits);
  }
}
