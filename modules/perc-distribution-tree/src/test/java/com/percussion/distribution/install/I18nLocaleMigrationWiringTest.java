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
import java.util.regex.Pattern;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Regression guard for GH-1547 / GH-2011: the installer ships {@code migration_i18n_locales.xml}
 * with a self-contained {@code ant.deps} path (nested {@code <ant>} does not apply {@code
 * inheritRefs} until after the child project is configured), and both install/upgrade chains invoke
 * it. Uses relative {@link Path} only (cross-platform).
 */
@Tag("UnitTest")
class I18nLocaleMigrationWiringTest {

  private static final Path INSTALLER =
      Path.of("src", "main", "resources", "distribution", "rxconfig", "Installer");

  private static final Path MIGRATION_XML = INSTALLER.resolve("migration_i18n_locales.xml");
  private static final Path INSTALL_XML = INSTALLER.resolve("install.xml");

  /** Local path id in the nested migration project (not a parent-only inheritRefs dependency). */
  private static final Pattern LOCAL_ANT_DEPS_PATH =
      Pattern.compile("<path\\s+id\\s*=\\s*[\"']ant\\.deps[\"']", Pattern.CASE_INSENSITIVE);

  private static final Pattern CLASSPATHREF_ANT_DEPS =
      Pattern.compile("classpathref\\s*=\\s*[\"']ant\\.deps[\"']", Pattern.CASE_INSENSITIVE);

  @Test
  void migrationScriptIsShipped() throws IOException {
    assertTrue(Files.isRegularFile(MIGRATION_XML), "missing " + MIGRATION_XML);
    String body = Files.readString(MIGRATION_XML, StandardCharsets.UTF_8);
    assertTrue(body.contains("PSMigrateI18nLocaleCodes"), "migration must use Ant task");
    assertTrue(body.contains("migrateI18nLocales"), "default target name");
    assertTrue(body.contains("i18n.locale.migration.dryRun"), "dry-run property");
  }

  /**
   * GH-2011: project-level {@code classpathref="ant.deps"} must resolve against a path id defined
   * in this same file. Parent {@code inheritRefs} is applied too late for nested project configure.
   */
  @Test
  void migrationScriptDefinesLocalAntDepsPath() throws IOException {
    assertTrue(Files.isRegularFile(MIGRATION_XML), "missing " + MIGRATION_XML);
    String body = Files.readString(MIGRATION_XML, StandardCharsets.UTF_8);

    assertTrue(
        LOCAL_ANT_DEPS_PATH.matcher(body).find(),
        "migration_i18n_locales.xml must define <path id=\"ant.deps\"> locally (GH-2011)");
    // taskdef classpathref must point at that local path (no dangling parent-only ref).
    assertTrue(
        CLASSPATHREF_ANT_DEPS.matcher(body).find(),
        "migration must use classpathref=\"ant.deps\" for PSMigrateI18nLocaleCodes taskdef");

    // Partial installer layouts must configure: every local ant.deps fileset uses
    // erroronmissingdir="false".
    assertTrue(
        body.contains("erroronmissingdir=\"false\"") || body.contains("erroronmissingdir='false'"),
        "local ant.deps filesets must set erroronmissingdir=\"false\" for partial layouts");
    // Count fileset opens vs erroronmissingdir so a fileset without the attribute fails.
    int filesetCount = 0;
    int missingDirFalseCount = 0;
    int searchFrom = 0;
    while ((searchFrom = body.indexOf("<fileset", searchFrom)) >= 0) {
      filesetCount++;
      searchFrom += "<fileset".length();
    }
    searchFrom = 0;
    while (true) {
      int dqi = body.indexOf("erroronmissingdir=\"false\"", searchFrom);
      int sqi = body.indexOf("erroronmissingdir='false'", searchFrom);
      int next;
      if (dqi < 0 && sqi < 0) {
        break;
      }
      if (dqi < 0) {
        next = sqi;
      } else if (sqi < 0) {
        next = dqi;
      } else {
        next = Math.min(dqi, sqi);
      }
      missingDirFalseCount++;
      searchFrom = next + "erroronmissingdir=".length();
    }
    assertTrue(
        filesetCount > 0 && missingDirFalseCount >= filesetCount,
        "every ant.deps fileset needs erroronmissingdir=\"false\" (filesets="
            + filesetCount
            + ", attrs="
            + missingDirFalseCount
            + ")");

    // Properties used to build the local path must come from the parent via inheritAll.
    assertTrue(
        body.contains("${install.dir}") || body.contains("${install.src}"),
        "local ant.deps should resolve jars under install.dir and/or install.src");
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

    // Parent still passes inheritAll/inheritRefs for properties and any other refs; classpath for
    // taskdef is self-contained in the child (asserted above).
    assertTrue(body.contains("install.chain"), "install.chain target must remain");
    assertTrue(body.contains("upgrade.chain"), "upgrade.chain target must remain");
  }
}
