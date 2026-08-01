/*
 * Copyright 1999-2026 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */
package com.percussion.preinstall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Tag("UnitTest")
class InstallerUserSettingsTest {

  @TempDir Path tempHome;

  @Test
  void saveAndLoadRoundTripCms() throws Exception {
    Path installDir = tempHome.resolve("cms-install");
    Files.createDirectories(installDir);
    InstallerUserSettings settings =
        new InstallerUserSettings(tempHome, InstallerUserSettings.PREFIX_CMS);

    Map<String, String> options = new LinkedHashMap<>();
    options.put("db.type", "mysql");
    options.put("db.host", "db.example.com");
    options.put("db.port", "3306");
    options.put("db.name", "rx");
    options.put("db.user", "rxuser");
    options.put("db.password", "SECRET-MUST-NOT-PERSIST");
    options.put("demo-sites", "true");

    settings.save(
        installDir, "8.2.0", options, Path.of(System.getProperty("java.home")).toString());

    assertEquals(
        installDir.toAbsolutePath().normalize(), settings.loadInstallDirectory().orElseThrow());
    assertEquals(Optional.of("8.2.0"), settings.loadVersion());
    Map<String, String> loaded = settings.loadOptionDefaults();
    assertEquals("mysql", loaded.get("db.type"));
    assertEquals("db.example.com", loaded.get("db.host"));
    assertEquals("true", loaded.get("demo-sites"));
    assertFalse(loaded.containsKey("db.password"));

    // Raw file must not contain password material
    Path propsFile =
        tempHome
            .resolve(".intsof")
            .resolve(InstallerUserSettings.APPLICATION_NAME)
            .resolve(InstallerUserSettings.SETTINGS_FILE);
    String raw = Files.readString(propsFile, StandardCharsets.ISO_8859_1);
    assertFalse(raw.toLowerCase().contains("password"));
    assertFalse(raw.contains("SECRET"));
    assertTrue(raw.contains("cms.version=8.2.0") || raw.contains("cms.version = 8.2.0"));
  }

  @Test
  void saveMergesWithoutWipingSiblingPrefixes() throws Exception {
    Path cmsDir = tempHome.resolve("cms");
    Path dtsDir = tempHome.resolve("dts");
    Files.createDirectories(cmsDir);
    Files.createDirectories(dtsDir);

    InstallerUserSettings cms =
        new InstallerUserSettings(tempHome, InstallerUserSettings.PREFIX_CMS);
    InstallerUserSettings dtsProd =
        new InstallerUserSettings(tempHome, InstallerUserSettings.PREFIX_DTS_PROD);

    cms.save(cmsDir, "8.2.0", Map.of("db.type", "h2"), null);
    dtsProd.save(dtsDir, "8.2.1", Map.of("db.type", "postgres"), null);

    // CMS save again must keep DTS keys
    cms.save(cmsDir, "8.2.2", Map.of("db.type", "h2", "db.user", "cms"), null);

    assertEquals(Optional.of("8.2.2"), cms.loadVersion());
    assertEquals(Optional.of("8.2.1"), dtsProd.loadVersion());
    assertEquals(dtsDir.toAbsolutePath().normalize(), dtsProd.loadInstallDirectory().orElseThrow());

    Properties all = new Properties();
    try (var in =
        Files.newInputStream(
            tempHome.resolve(".intsof").resolve("percussion").resolve("last-install.properties"))) {
      all.load(in);
    }
    assertTrue(
        all.getProperty("dts.prod.db.type").contains("postgres")
            || "postgres".equals(all.getProperty("dts.prod.db.type")));
    assertEquals("cms", all.getProperty("cms.db.user"));
  }

  @Test
  void applyDefaultsFillsMissingPathAndOptionsOnly() {
    Path installDir = tempHome.resolve("prior-cms");
    InstallerUserSettings settings =
        new InstallerUserSettings(tempHome, InstallerUserSettings.PREFIX_CMS);
    settings.save(installDir, "8.2.0", Map.of("db.type", "mysql", "db.host", "saved-host"), null);

    DbInstallConfigResolver.ParsedArgs empty =
        new DbInstallConfigResolver.ParsedArgs(null, Map.of());
    DbInstallConfigResolver.ParsedArgs filled = settings.applyDefaults(empty);
    assertEquals(installDir.toAbsolutePath().normalize(), filled.installPath());
    assertEquals("mysql", filled.options().get("db.type"));
    assertEquals("saved-host", filled.options().get("db.host"));

    Path cliPath = tempHome.resolve("cli-path");
    DbInstallConfigResolver.ParsedArgs cliWins =
        settings.applyDefaults(
            new DbInstallConfigResolver.ParsedArgs(
                cliPath, Map.of("db.type", "h2", "db.host", "from-cli")));
    assertEquals(
        cliPath.toAbsolutePath().normalize(), cliWins.installPath().toAbsolutePath().normalize());
    assertEquals("h2", cliWins.options().get("db.type"));
    assertEquals("from-cli", cliWins.options().get("db.host"));
  }

  @Test
  void dtsPrefixSelection() {
    assertEquals(InstallerUserSettings.PREFIX_DTS_PROD, InstallerUserSettings.dtsPrefix("true"));
    assertEquals(InstallerUserSettings.PREFIX_DTS_PROD, InstallerUserSettings.dtsPrefix("TRUE"));
    assertEquals(InstallerUserSettings.PREFIX_DTS_STAGE, InstallerUserSettings.dtsPrefix("false"));
    assertEquals(InstallerUserSettings.PREFIX_DTS_STAGE, InstallerUserSettings.dtsPrefix(null));
  }

  @Test
  void mergePercDbIntoOptionsDoesNotCopyPassword() {
    Map<String, String> options = new LinkedHashMap<>();
    Map<String, String> perc =
        Map.of(
            "perc.db.type", "oracle",
            "perc.db.host", "ora-host",
            "perc.db.password", "nope");
    InstallerUserSettings.mergePercDbIntoOptions(options, perc);
    assertEquals("oracle", options.get("db.type"));
    assertEquals("ora-host", options.get("db.host"));
    assertFalse(options.containsKey("db.password"));
  }

  @Test
  void interactiveWizardUsesSavedPathDefault() throws Exception {
    Path prior = tempHome.resolve("saved-install");
    Files.createDirectories(prior);
    new InstallerUserSettings(tempHome, InstallerUserSettings.PREFIX_CMS)
        .save(prior, "8.2.0", Map.of("db.type", "h2"), null);

    // Empty path answer accepts default; then H2 password prompts + demo-sites + confirm
    ScriptedPrompt prompt = new ScriptedPrompt("", "", "operator-pwd", "operator-pwd", "", "");
    Path javaHome = Path.of(System.getProperty("java.home")).toAbsolutePath().normalize();
    DbInstallConfigResolver.ParsedArgs parsed =
        new DbInstallConfigResolver.ParsedArgs(null, Map.of());
    InteractiveInstallWizard.Phase1Result result =
        InteractiveInstallWizard.runPhase1(parsed, true, prompt, javaHome, tempHome);
    assertTrue(result.proceed());
    assertEquals(prior.toAbsolutePath().normalize(), result.installPath());
  }

  /** Minimal scripted console for wizard tests. */
  private static final class ScriptedPrompt implements InstallPrompt {
    private final java.util.ArrayDeque<String> answers = new java.util.ArrayDeque<>();

    ScriptedPrompt(String... answers) {
      for (String a : answers) {
        this.answers.addLast(a);
      }
    }

    @Override
    public void print(String message) {}

    @Override
    public void println(String message) {}

    @Override
    public String readLine(String prompt) {
      return answers.isEmpty() ? "" : answers.removeFirst();
    }

    @Override
    public char[] readPassword(String prompt) {
      String s = readLine(prompt);
      return s == null ? null : s.toCharArray();
    }
  }
}
