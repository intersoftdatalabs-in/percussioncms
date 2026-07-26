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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Tag("UnitTest")
class InteractiveInstallWizardTest {

  @TempDir Path tempDir;

  @Test
  void silentModeRecognizesSilentAndNoTtyFlags() {
    assertTrue(InteractiveInstallWizard.isSilentMode(Map.of("silent", "true")));
    assertTrue(InteractiveInstallWizard.isSilentMode(Map.of("silent", "yes")));
    assertTrue(InteractiveInstallWizard.isSilentMode(Map.of("no-tty", "1")));
    assertFalse(InteractiveInstallWizard.isSilentMode(Map.of()));
    assertFalse(InteractiveInstallWizard.isSilentMode(null));
  }

  @Test
  void isInteractiveRequiresConsoleAndNotSilent() {
    assertTrue(InteractiveInstallWizard.isInteractive(false, true));
    assertFalse(InteractiveInstallWizard.isInteractive(true, true));
    assertFalse(InteractiveInstallWizard.isInteractive(false, false));
    assertFalse(InteractiveInstallWizard.isInteractive(true, false));
  }

  @Test
  void nonInteractiveMissingPathReturnsUsage() {
    DbInstallConfigResolver.ParsedArgs parsed =
        new DbInstallConfigResolver.ParsedArgs(null, Map.of());
    InteractiveInstallWizard.Phase1Result result =
        InteractiveInstallWizard.runPhase1(parsed, false, null);
    assertFalse(result.proceed());
    assertEquals(InteractiveInstallWizard.EXIT_USAGE, result.exitCode());
    assertNotNull(result.message());
    assertTrue(result.message().contains("installation or upgrade folder"));
    assertTrue(result.message().toLowerCase().contains("interactive"));
  }

  @Test
  void nonInteractiveWithPathSkipsConfirmAndProceeds() {
    Path install = tempDir.resolve("cms");
    DbInstallConfigResolver.ParsedArgs parsed =
        new DbInstallConfigResolver.ParsedArgs(install, Map.of());
    InteractiveInstallWizard.Phase1Result result =
        InteractiveInstallWizard.runPhase1(parsed, false, null);
    assertTrue(result.proceed());
    assertEquals(install.toAbsolutePath().normalize(), result.installPath());
    assertEquals("h2", result.dbConfig().systemProperties().get("perc.db.type"));
  }

  @Test
  void interactivePromptsForPathAndConfirmsDefaultYesForH2() {
    Path install = tempDir.resolve("interactive-cms");
    ScriptedPrompt prompt = new ScriptedPrompt(install.toString(), ""); // empty confirm → Y for H2
    DbInstallConfigResolver.ParsedArgs parsed =
        new DbInstallConfigResolver.ParsedArgs(null, Map.of());
    InteractiveInstallWizard.Phase1Result result =
        InteractiveInstallWizard.runPhase1(parsed, true, prompt);
    assertTrue(result.proceed());
    assertEquals(install.toAbsolutePath().normalize(), result.installPath());
    assertEquals("h2", result.dbConfig().systemProperties().get("perc.db.type"));
    assertTrue(prompt.outputsAsString().contains("installation summary"));
    assertTrue(prompt.outputsAsString().contains("h2"));
  }

  @Test
  void interactiveConfirmNoAborts() {
    Path install = tempDir.resolve("abort-cms");
    ScriptedPrompt prompt = new ScriptedPrompt(install.toString(), "n");
    DbInstallConfigResolver.ParsedArgs parsed =
        new DbInstallConfigResolver.ParsedArgs(null, Map.of());
    InteractiveInstallWizard.Phase1Result result =
        InteractiveInstallWizard.runPhase1(parsed, true, prompt);
    assertFalse(result.proceed());
    assertEquals(InteractiveInstallWizard.EXIT_ABORTED, result.exitCode());
    assertTrue(result.message().toLowerCase().contains("cancelled"));
  }

  @Test
  void interactivePathAlreadySuppliedStillConfirms() {
    Path install = tempDir.resolve("cli-path");
    ScriptedPrompt prompt = new ScriptedPrompt("y");
    DbInstallConfigResolver.ParsedArgs parsed =
        new DbInstallConfigResolver.ParsedArgs(install, Map.of());
    InteractiveInstallWizard.Phase1Result result =
        InteractiveInstallWizard.runPhase1(parsed, true, prompt);
    assertTrue(result.proceed());
    assertEquals(install.toAbsolutePath().normalize(), result.installPath());
    assertTrue(prompt.outputsAsString().contains("Install path"));
  }

  @Test
  void externalDbRequiresExplicitYesOnEmptyConfirm() {
    Path install = tempDir.resolve("mysql-cms");
    Map<String, String> opts = new HashMap<>();
    opts.put("db.type", "mysql");
    opts.put("db.host", "db.example.com");
    opts.put("db.port", "3306");
    opts.put("db.name", "percussion");
    opts.put("db.user", "cms");
    opts.put("db.password", "s3cret-should-not-appear");

    // empty confirm with defaultYes=false → treated as No
    ScriptedPrompt prompt = new ScriptedPrompt("");
    DbInstallConfigResolver.ParsedArgs parsed =
        new DbInstallConfigResolver.ParsedArgs(install, opts);
    InteractiveInstallWizard.Phase1Result result =
        InteractiveInstallWizard.runPhase1(parsed, true, prompt);
    assertFalse(result.proceed());
    assertTrue(prompt.outputsAsString().contains("mysql"));
    assertTrue(prompt.outputsAsString().contains("db.example.com"));
    assertFalse(prompt.outputsAsString().contains("s3cret-should-not-appear"));
  }

  @Test
  void externalDbExplicitYesProceedsAndRedactsPassword() {
    Path install = tempDir.resolve("mysql-cms-ok");
    Map<String, String> opts = new HashMap<>();
    opts.put("db.type", "mysql");
    opts.put("db.host", "db.example.com");
    opts.put("db.port", "3306");
    opts.put("db.name", "percussion");
    opts.put("db.user", "cms");
    opts.put("db.password", "s3cret-should-not-appear");

    ScriptedPrompt prompt = new ScriptedPrompt("yes");
    DbInstallConfigResolver.ParsedArgs parsed =
        new DbInstallConfigResolver.ParsedArgs(install, opts);
    InteractiveInstallWizard.Phase1Result result =
        InteractiveInstallWizard.runPhase1(parsed, true, prompt);
    assertTrue(result.proceed());
    assertEquals("mysql", result.dbConfig().systemProperties().get("perc.db.type"));
    assertFalse(prompt.outputsAsString().contains("s3cret-should-not-appear"));
  }

  @Test
  void badDbConfigAbortsWithDbExitCode() {
    Path install = tempDir.resolve("bad-db");
    // Unreadable dbprops path fails fast without depending on process env filling --db.* fields.
    Map<String, String> opts =
        Map.of("dbprops", tempDir.resolve("missing-rxrepository.properties").toString());
    DbInstallConfigResolver.ParsedArgs parsed =
        new DbInstallConfigResolver.ParsedArgs(install, opts);
    InteractiveInstallWizard.Phase1Result result =
        InteractiveInstallWizard.runPhase1(parsed, false, null);
    assertFalse(result.proceed());
    assertEquals(InteractiveInstallWizard.EXIT_DB_CONFIG, result.exitCode());
    assertTrue(result.message().toLowerCase().contains("database"));
    assertFalse(result.message().toLowerCase().contains("password"));
  }

  @Test
  void summaryDetectsUpgradeWhenVersionPropertiesPresent() throws Exception {
    Path install = tempDir.resolve("upgrade-root");
    Files.createDirectories(install);
    Files.writeString(
        install.resolve(Main.VERSION_PROPERTIES),
        "majorVersion=8\nminorVersion=2\n",
        StandardCharsets.UTF_8);
    DbInstallConfigResolver.ResolvedDbConfig db =
        DbInstallConfigResolver.resolveDbConfig(Map.of());
    String summary = InteractiveInstallWizard.buildSummary(install, db);
    assertTrue(summary.contains("Upgrade"));
    assertFalse(summary.contains("New install"));
  }

  @Test
  void parseYesNoDefaultsAndValues() {
    assertEquals(Boolean.TRUE, InteractiveInstallWizard.parseYesNo("", true));
    assertEquals(Boolean.FALSE, InteractiveInstallWizard.parseYesNo("", false));
    assertEquals(Boolean.TRUE, InteractiveInstallWizard.parseYesNo("Y", false));
    assertEquals(Boolean.FALSE, InteractiveInstallWizard.parseYesNo("no", true));
    assertNull(InteractiveInstallWizard.parseYesNo("maybe", true));
  }

  @Test
  void isDefaultYesConfirmOnlyForEmbedded() {
    assertTrue(
        InteractiveInstallWizard.isDefaultYesConfirm(
            DbInstallConfigResolver.resolveDbConfig(Map.of())));
    Map<String, String> mysql = new HashMap<>();
    mysql.put("db.type", "mysql");
    mysql.put("db.host", "h");
    mysql.put("db.port", "3306");
    mysql.put("db.name", "n");
    mysql.put("db.user", "u");
    mysql.put("db.password", "p");
    assertFalse(
        InteractiveInstallWizard.isDefaultYesConfirm(
            DbInstallConfigResolver.resolveDbConfig(mysql)));
  }

  /** Scripted prompt: answers consumed in order for readLine/readPassword. */
  private static final class ScriptedPrompt implements InstallPrompt {
    private final Deque<String> answers;
    private final List<String> outputs = new ArrayList<>();

    ScriptedPrompt(String... answers) {
      this.answers = new ArrayDeque<>();
      for (String a : answers) {
        this.answers.addLast(a);
      }
    }

    @Override
    public void print(String message) {
      outputs.add(message == null ? "" : message);
    }

    @Override
    public void println(String message) {
      outputs.add((message == null ? "" : message) + "\n");
    }

    @Override
    public String readLine(String prompt) {
      print(prompt);
      return answers.isEmpty() ? "" : answers.removeFirst();
    }

    @Override
    public String readPassword(String prompt) {
      return readLine(prompt);
    }

    String outputsAsString() {
      StringBuilder sb = new StringBuilder();
      for (String o : outputs) {
        sb.append(o);
      }
      return sb.toString();
    }
  }
}
