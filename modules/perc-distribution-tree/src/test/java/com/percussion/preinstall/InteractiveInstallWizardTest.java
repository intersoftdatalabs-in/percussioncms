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

  /** Use the running JVM home so selection does not depend on host discovery. */
  private Path runningJavaHome() {
    return Path.of(System.getProperty("java.home")).toAbsolutePath().normalize();
  }

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
        InteractiveInstallWizard.runPhase1(parsed, false, null, runningJavaHome());
    assertTrue(result.proceed());
    assertEquals(install.toAbsolutePath().normalize(), result.installPath());
    assertEquals("h2", result.dbConfig().systemProperties().get("perc.db.type"));
    assertNotNull(result.javaOutcome());
    assertTrue(Files.exists(install.resolve("java.properties")));
  }

  @Test
  void interactivePromptsForPathAndConfirmsDefaultYesForH2() {
    Path install = tempDir.resolve("interactive-cms");
    // path → DB menu default H2 → CMS DB password + confirm → confirm default Y
    ScriptedPrompt prompt =
        new ScriptedPrompt(install.toString(), "", "operator-pwd", "operator-pwd", "");
    DbInstallConfigResolver.ParsedArgs parsed =
        new DbInstallConfigResolver.ParsedArgs(null, Map.of());
    InteractiveInstallWizard.Phase1Result result =
        InteractiveInstallWizard.runPhase1(parsed, true, prompt, runningJavaHome());
    assertTrue(result.proceed());
    assertEquals(install.toAbsolutePath().normalize(), result.installPath());
    assertEquals("h2", result.dbConfig().systemProperties().get("perc.db.type"));
    assertEquals("operator-pwd", result.dbConfig().systemProperties().get("cmdb.password"));
    assertTrue(prompt.outputsAsString().contains("installation summary"));
    assertTrue(prompt.outputsAsString().contains("h2"));
    assertTrue(prompt.outputsAsString().contains("Java home"));
    assertTrue(
        prompt.outputsAsString().contains("rxrepository.properties"),
        "interactive H2 summary must reference rxrepository.properties; was:\n"
            + prompt.outputsAsString());
  }

  @Test
  void interactiveConfirmNoAborts() {
    Path install = tempDir.resolve("abort-cms");
    // path → H2 menu → CMS DB password + confirm → confirm n
    ScriptedPrompt prompt = new ScriptedPrompt(install.toString(), "1", "secret", "secret", "n");
    DbInstallConfigResolver.ParsedArgs parsed =
        new DbInstallConfigResolver.ParsedArgs(null, Map.of());
    InteractiveInstallWizard.Phase1Result result =
        InteractiveInstallWizard.runPhase1(parsed, true, prompt, runningJavaHome());
    assertFalse(result.proceed());
    assertEquals(InteractiveInstallWizard.EXIT_ABORTED, result.exitCode());
    assertTrue(result.message().toLowerCase().contains("cancelled"));
  }

  @Test
  void interactivePathAlreadySuppliedStillConfirms() {
    Path install = tempDir.resolve("cli-path");
    // H2 menu → CMS DB password + confirm → confirm y
    ScriptedPrompt prompt = new ScriptedPrompt("1", "secret", "secret", "y");
    DbInstallConfigResolver.ParsedArgs parsed =
        new DbInstallConfigResolver.ParsedArgs(install, Map.of());
    InteractiveInstallWizard.Phase1Result result =
        InteractiveInstallWizard.runPhase1(parsed, true, prompt, runningJavaHome());
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

    // skip field prompts (CLI override) → test connection n → confirm empty → No
    ScriptedPrompt prompt = new ScriptedPrompt("n", "");
    DbInstallConfigResolver.ParsedArgs parsed =
        new DbInstallConfigResolver.ParsedArgs(install, opts);
    InteractiveInstallWizard.Phase1Result result =
        InteractiveInstallWizard.runPhase1(parsed, true, prompt, runningJavaHome());
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

    // test connection n → confirm yes
    ScriptedPrompt prompt = new ScriptedPrompt("n", "yes");
    DbInstallConfigResolver.ParsedArgs parsed =
        new DbInstallConfigResolver.ParsedArgs(install, opts);
    InteractiveInstallWizard.Phase1Result result =
        InteractiveInstallWizard.runPhase1(parsed, true, prompt, runningJavaHome());
    assertTrue(result.proceed());
    assertEquals("mysql", result.dbConfig().systemProperties().get("perc.db.type"));
    assertFalse(prompt.outputsAsString().contains("s3cret-should-not-appear"));
  }

  @Test
  void interactiveSqlServerExpressPathCollectsStructuredFields() {
    Path install = tempDir.resolve("express-cms");
    // menu 2 (SQL Server) → host/port/name/schema/user/password/ssl/sslVerify → skip test → confirm
    ScriptedPrompt prompt =
        new ScriptedPrompt(
            "2",
            "localhost",
            "1433",
            "percussion",
            "dbo",
            "sa",
            "pw-secret",
            "true",
            "true",
            "n",
            "y");
    DbInstallConfigResolver.ParsedArgs parsed =
        new DbInstallConfigResolver.ParsedArgs(install, Map.of());
    InteractiveInstallWizard.Phase1Result result =
        InteractiveInstallWizard.runPhase1(parsed, true, prompt, runningJavaHome());
    assertTrue(result.proceed());
    assertEquals("sqlserver", result.dbConfig().systemProperties().get("perc.db.type"));
    assertEquals("sa", result.dbConfig().systemProperties().get("perc.db.user"));
    assertTrue(prompt.outputsAsString().toLowerCase().contains("express"));
    assertFalse(prompt.outputsAsString().contains("pw-secret"));
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
        InteractiveInstallWizard.runPhase1(parsed, false, null, runningJavaHome());
    assertFalse(result.proceed());
    assertEquals(InteractiveInstallWizard.EXIT_DB_CONFIG, result.exitCode());
    assertTrue(result.message().toLowerCase().contains("database"));
    assertFalse(result.message().toLowerCase().contains("password"));
  }

  @Test
  void invalidJavaHomeAbortsWithJavaExitCode() {
    Path install = tempDir.resolve("bad-java");
    Path invalid = tempDir.resolve("not-a-jdk");
    DbInstallConfigResolver.ParsedArgs parsed =
        new DbInstallConfigResolver.ParsedArgs(install, Map.of());
    InteractiveInstallWizard.Phase1Result result =
        InteractiveInstallWizard.runPhase1(parsed, false, null, invalid);
    assertFalse(result.proceed());
    assertEquals(InteractiveInstallWizard.EXIT_JAVA, result.exitCode());
    assertTrue(result.message().toLowerCase().contains("java"));
  }

  @Test
  void summaryDetectsUpgradeWhenVersionPropertiesPresent() throws Exception {
    Path install = tempDir.resolve("upgrade-root");
    Files.createDirectories(install);
    Files.writeString(
        install.resolve(Main.VERSION_PROPERTIES),
        "majorVersion=8\nminorVersion=2\n",
        StandardCharsets.UTF_8);
    DbInstallConfigResolver.ResolvedDbConfig db = DbInstallConfigResolver.resolveDbConfig(Map.of());
    String summary = InteractiveInstallWizard.buildSummary(install, db);
    assertTrue(summary.contains("Upgrade"));
    assertFalse(summary.contains("New install"));
  }

  @Test
  void interactiveH2SummaryReferencesRxrepositoryProperties() {
    // Operator-supplied H2 passwords live only in rxrepository.properties and
    // perc-ds.properties; the system-generated passwords file is reserved for
    // credentials the system auto-generates, not operator-chosen secrets.
    Map<String, String> props = new HashMap<>();
    props.put("perc.db.type", "h2");
    props.put("cmdb.password", "operator-chosen-pwd");
    DbInstallConfigResolver.ResolvedDbConfig db =
        new DbInstallConfigResolver.ResolvedDbConfig(props, "structured");
    Path install = tempDir.resolve("new-install-root");
    String summary = InteractiveInstallWizard.buildSummary(install, db);
    assertTrue(
        summary.contains("rxrepository.properties"),
        "interactive H2 summary must point the operator to rxrepository.properties; was:\n"
            + summary);
    assertFalse(
        summary.contains("var/config/generated/passwords"),
        "operator-chosen H2 passwords must NOT be advertised as living in"
            + " var/config/generated/passwords; was:\n"
            + summary);
    assertFalse(
        summary.contains("operator-chosen-pwd"),
        "summary must never echo the password value; was:\n" + summary);
  }

  @Test
  void silentH2SummaryReferencesGeneratedPasswordsFile() {
    // Silent installs (no operator cmdb.password in the resolved config) get a
    // random value persisted to var/config/generated/passwords by ANT's
    // PSGenerateRepositoryPassword; the summary must point operators there.
    Map<String, String> props = new HashMap<>();
    props.put("perc.db.type", "h2");
    DbInstallConfigResolver.ResolvedDbConfig db =
        new DbInstallConfigResolver.ResolvedDbConfig(props, "default");
    Path install = tempDir.resolve("new-install-root");
    String summary = InteractiveInstallWizard.buildSummary(install, db);
    assertTrue(
        summary.contains("var/config/generated/passwords"),
        "silent H2 summary must point operators to var/config/generated/passwords; was:\n"
            + summary);
    assertTrue(summary.contains("cmdb"), summary);
  }

  @Test
  void externalDbSummaryDoesNotMentionGeneratedPasswords() {
    Map<String, String> props = new HashMap<>();
    props.put("perc.db.type", "postgresql");
    props.put("perc.db.host", "localhost");
    DbInstallConfigResolver.ResolvedDbConfig db =
        new DbInstallConfigResolver.ResolvedDbConfig(props, "structured");
    Path install = tempDir.resolve("new-install-root");
    String summary = InteractiveInstallWizard.buildSummary(install, db);
    assertFalse(
        summary.contains("var/config/generated/passwords"),
        "external backends manage their own credentials; was:\n" + summary);
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
    public char[] readPassword(String prompt) {
      String s = readLine(prompt);
      return s == null ? null : s.toCharArray();
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
