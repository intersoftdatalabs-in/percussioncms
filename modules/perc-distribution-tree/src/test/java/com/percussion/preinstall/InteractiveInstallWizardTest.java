/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

  /**
   * Isolated {@code user.home} for {@link InstallerUserSettings} so tests never read the
   * developer's real {@code ~/.intsof/percussion/last-install.properties}.
   */
  private Path isolatedUserHome() {
    return tempDir.resolve("fake-user-home");
  }

  private InteractiveInstallWizard.Phase1Result runPhase1Isolated(
      DbInstallConfigResolver.ParsedArgs parsed,
      boolean interactive,
      InstallPrompt prompt,
      Path unattendedJavaHome) {
    return InteractiveInstallWizard.runPhase1(
        parsed, interactive, prompt, unattendedJavaHome, isolatedUserHome());
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
        runPhase1Isolated(parsed, false, null, runningJavaHome());
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
        runPhase1Isolated(parsed, false, null, runningJavaHome());
    assertTrue(result.proceed());
    assertEquals(install.toAbsolutePath().normalize(), result.installPath());
    assertEquals("h2", result.dbConfig().systemProperties().get("perc.db.type"));
    assertNotNull(result.javaOutcome());
    assertTrue(Files.exists(install.resolve("java.properties")));
  }

  @Test
  void interactivePromptsForPathAndConfirmsDefaultYesForH2() {
    Path install = tempDir.resolve("interactive-cms");
    // path → DB menu default H2 → CMS DB password + confirm → demo-sites (default No) → confirm Y
    ScriptedPrompt prompt =
        new ScriptedPrompt(install.toString(), "", "operator-pwd", "operator-pwd", "", "");
    DbInstallConfigResolver.ParsedArgs parsed =
        new DbInstallConfigResolver.ParsedArgs(null, Map.of());
    InteractiveInstallWizard.Phase1Result result =
        runPhase1Isolated(parsed, true, prompt, runningJavaHome());
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
    assertTrue(
        prompt.outputsAsString().contains("Sample sites"),
        "summary must include the demo-sites line; was:\n" + prompt.outputsAsString());
  }

  @Test
  void interactiveConfirmNoAborts() {
    Path install = tempDir.resolve("abort-cms");
    // path → H2 menu → CMS DB password + confirm → demo-sites (default No) → confirm n
    ScriptedPrompt prompt =
        new ScriptedPrompt(install.toString(), "1", "secret", "secret", "", "n");
    DbInstallConfigResolver.ParsedArgs parsed =
        new DbInstallConfigResolver.ParsedArgs(null, Map.of());
    InteractiveInstallWizard.Phase1Result result =
        runPhase1Isolated(parsed, true, prompt, runningJavaHome());
    assertFalse(result.proceed());
    assertEquals(InteractiveInstallWizard.EXIT_ABORTED, result.exitCode());
    assertTrue(result.message().toLowerCase().contains("cancelled"));
  }

  @Test
  void interactivePathAlreadySuppliedStillConfirms() {
    Path install = tempDir.resolve("cli-path");
    // H2 menu → CMS DB password + confirm → demo-sites (default No) → confirm y
    ScriptedPrompt prompt = new ScriptedPrompt("1", "secret", "secret", "", "y");
    DbInstallConfigResolver.ParsedArgs parsed =
        new DbInstallConfigResolver.ParsedArgs(install, Map.of());
    InteractiveInstallWizard.Phase1Result result =
        runPhase1Isolated(parsed, true, prompt, runningJavaHome());
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

    // skip field prompts (CLI override) → test connection n → demo-sites (default No) → confirm
    // empty → No
    ScriptedPrompt prompt = new ScriptedPrompt("n", "", "");
    DbInstallConfigResolver.ParsedArgs parsed =
        new DbInstallConfigResolver.ParsedArgs(install, opts);
    InteractiveInstallWizard.Phase1Result result =
        runPhase1Isolated(parsed, true, prompt, runningJavaHome());
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

    // test connection n → demo-sites (default No) → confirm yes
    ScriptedPrompt prompt = new ScriptedPrompt("n", "", "yes");
    DbInstallConfigResolver.ParsedArgs parsed =
        new DbInstallConfigResolver.ParsedArgs(install, opts);
    InteractiveInstallWizard.Phase1Result result =
        runPhase1Isolated(parsed, true, prompt, runningJavaHome());
    assertTrue(result.proceed());
    assertEquals("mysql", result.dbConfig().systemProperties().get("perc.db.type"));
    assertFalse(prompt.outputsAsString().contains("s3cret-should-not-appear"));
  }

  @Test
  void interactiveSqlServerExpressPathCollectsStructuredFields() {
    Path install = tempDir.resolve("express-cms");
    // menu 2 (SQL Server) → host/port/name/schema/user/password/ssl/sslVerify → skip test
    //   → demo-sites (default No) → confirm y
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
            "",
            "y");
    DbInstallConfigResolver.ParsedArgs parsed =
        new DbInstallConfigResolver.ParsedArgs(install, Map.of());
    InteractiveInstallWizard.Phase1Result result =
        runPhase1Isolated(parsed, true, prompt, runningJavaHome());
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
        runPhase1Isolated(parsed, false, null, runningJavaHome());
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
    InteractiveInstallWizard.Phase1Result result = runPhase1Isolated(parsed, false, null, invalid);
    assertFalse(result.proceed());
    assertEquals(InteractiveInstallWizard.EXIT_JAVA, result.exitCode());
    assertTrue(result.message().toLowerCase().contains("java"));
  }

  @Test
  void savedInstallDirectoryIsAppliedWhenCliPathMissing() throws Exception {
    Path savedInstall = tempDir.resolve("from-settings");
    Files.createDirectories(savedInstall);
    InstallerUserSettings settings =
        new InstallerUserSettings(isolatedUserHome(), InstallerUserSettings.PREFIX_CMS);
    settings.save(savedInstall, "8.2.0", Map.of(), runningJavaHome().toString());

    DbInstallConfigResolver.ParsedArgs parsed =
        new DbInstallConfigResolver.ParsedArgs(null, Map.of());
    InteractiveInstallWizard.Phase1Result result =
        runPhase1Isolated(parsed, false, null, runningJavaHome());
    assertTrue(result.proceed());
    assertEquals(savedInstall.toAbsolutePath().normalize(), result.installPath());
  }

  /**
   * Regression: last-install path must be an editable default in interactive mode, not a forced
   * destination that skips the directory prompt (operator cannot change install root).
   */
  @Test
  void interactivePromptsWithSavedPathAsDefaultAndAcceptsEmpty() throws Exception {
    Path savedInstall = tempDir.resolve("prior-install");
    Files.createDirectories(savedInstall);
    InstallerUserSettings settings =
        new InstallerUserSettings(isolatedUserHome(), InstallerUserSettings.PREFIX_CMS);
    settings.save(savedInstall, "8.2.0", Map.of(), runningJavaHome().toString());

    // empty path → accept default; H2 menu; CMS DB password + confirm; demo-sites; confirm Y
    ScriptedPrompt prompt = new ScriptedPrompt("", "1", "secret", "secret", "", "y");
    DbInstallConfigResolver.ParsedArgs parsed =
        new DbInstallConfigResolver.ParsedArgs(null, Map.of());
    InteractiveInstallWizard.Phase1Result result =
        runPhase1Isolated(parsed, true, prompt, runningJavaHome());
    assertTrue(result.proceed());
    assertEquals(savedInstall.toAbsolutePath().normalize(), result.installPath());
    String out = prompt.outputsAsString();
    assertTrue(
        out.contains("Installation directory"),
        "interactive mode must prompt for install directory even when last-install has a path;"
            + " was:\n"
            + out);
    assertTrue(
        out.contains(savedInstall.toAbsolutePath().normalize().toString())
            || out.contains(savedInstall.toString()),
        "path prompt must show saved install path as default; was:\n" + out);
  }

  /**
   * Regression: operator can override last-install directory by typing a different path at the
   * prompt.
   */
  @Test
  void interactiveAllowsChangingSavedInstallDirectory() throws Exception {
    Path savedInstall = tempDir.resolve("old-install");
    Path newInstall = tempDir.resolve("new-install");
    Files.createDirectories(savedInstall);
    InstallerUserSettings settings =
        new InstallerUserSettings(isolatedUserHome(), InstallerUserSettings.PREFIX_CMS);
    settings.save(savedInstall, "8.2.0", Map.of(), runningJavaHome().toString());

    // different path → H2; password + confirm; demo-sites; confirm Y
    ScriptedPrompt prompt =
        new ScriptedPrompt(newInstall.toString(), "1", "secret", "secret", "", "y");
    DbInstallConfigResolver.ParsedArgs parsed =
        new DbInstallConfigResolver.ParsedArgs(null, Map.of());
    InteractiveInstallWizard.Phase1Result result =
        runPhase1Isolated(parsed, true, prompt, runningJavaHome());
    assertTrue(result.proceed());
    assertEquals(newInstall.toAbsolutePath().normalize(), result.installPath());
    assertTrue(
        prompt.outputsAsString().contains("Installation directory"),
        "must still show the install-directory prompt when last-install path exists");
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

  @Test
  void demoSitesFlagParsesCliAndSystemProperty() {
    String prevSys = System.getProperty(DbInstallConfigResolver.DEMO_SITES_SYSTEM_PROPERTY);
    try {
      System.clearProperty(DbInstallConfigResolver.DEMO_SITES_SYSTEM_PROPERTY);
      assertFalse(DbInstallConfigResolver.parseDemoSitesFlag(Map.of()));
      assertFalse(DbInstallConfigResolver.parseDemoSitesFlag(null));
      assertTrue(
          DbInstallConfigResolver.parseDemoSitesFlag(
              Map.of(DbInstallConfigResolver.DEMO_SITES_KEY, "true")));
      assertTrue(
          DbInstallConfigResolver.parseDemoSitesFlag(
              Map.of(DbInstallConfigResolver.DEMO_SITES_KEY, "yes")));
      assertTrue(
          DbInstallConfigResolver.parseDemoSitesFlag(
              Map.of(DbInstallConfigResolver.DEMO_SITES_KEY, "1")));
      assertTrue(DbInstallConfigResolver.parseDemoSitesFlag(Map.of("install.demo.sites", "true")));
      assertFalse(
          DbInstallConfigResolver.parseDemoSitesFlag(
              Map.of(DbInstallConfigResolver.DEMO_SITES_KEY, "false")));

      // CLI flag wins when both are set; system property is the fallback when CLI is blank.
      // Mirrors ObsoleteInstallDirCleaner.parseCleanInstallDirFlag.
      System.setProperty(DbInstallConfigResolver.DEMO_SITES_SYSTEM_PROPERTY, "true");
      assertFalse(
          DbInstallConfigResolver.parseDemoSitesFlag(
              Map.of(DbInstallConfigResolver.DEMO_SITES_KEY, "false")));
      System.setProperty(DbInstallConfigResolver.DEMO_SITES_SYSTEM_PROPERTY, "true");
      assertTrue(DbInstallConfigResolver.parseDemoSitesFlag(Map.of()));
      System.setProperty(DbInstallConfigResolver.DEMO_SITES_SYSTEM_PROPERTY, "no");
      assertFalse(DbInstallConfigResolver.parseDemoSitesFlag(Map.of()));
    } finally {
      if (prevSys == null) {
        System.clearProperty(DbInstallConfigResolver.DEMO_SITES_SYSTEM_PROPERTY);
      } else {
        System.setProperty(DbInstallConfigResolver.DEMO_SITES_SYSTEM_PROPERTY, prevSys);
      }
    }
  }

  @Test
  void resolveDemoSitesInteractivePromptsOnUpgradeToo() {
    // Upgrades must surface the demo-sites prompt so operators can opt in (the strip
    // step in installRepository.xml is the RXLOCALE/RXLOCALEFORMAT protection, not
    // install-type gating).
    Map<String, String> options = new HashMap<>();
    ScriptedPrompt prompt = new ScriptedPrompt("y");
    boolean resolved = InteractiveInstallWizard.resolveDemoSites(true, options, prompt);
    assertTrue(resolved);
    assertEquals("true", options.get(DbInstallConfigResolver.DEMO_SITES_KEY));
    assertTrue(
        prompt.outputsAsString().contains("Install sample sites"),
        "prompt must show on upgrade; was:\n" + prompt.outputsAsString());
  }

  @Test
  void resolveDemoSitesInteractiveHonorsEmptyAnswerAgainstCliDefault() {
    Map<String, String> options = new HashMap<>();
    options.put(DbInstallConfigResolver.DEMO_SITES_KEY, "true");
    ScriptedPrompt prompt = new ScriptedPrompt("");
    boolean resolved = InteractiveInstallWizard.resolveDemoSites(true, options, prompt);
    assertTrue(resolved);
    assertEquals("true", options.get(DbInstallConfigResolver.DEMO_SITES_KEY));
  }

  @Test
  void resolveDemoSitesSilentHonorsCliAndDefaultsToFalse() {
    Map<String, String> options = new HashMap<>();
    assertFalse(InteractiveInstallWizard.resolveDemoSites(false, options, new ScriptedPrompt()));
    assertEquals("false", options.get(DbInstallConfigResolver.DEMO_SITES_KEY));

    options.clear();
    options.put(DbInstallConfigResolver.DEMO_SITES_KEY, "yes");
    assertTrue(InteractiveInstallWizard.resolveDemoSites(false, options, new ScriptedPrompt()));
    assertEquals("true", options.get(DbInstallConfigResolver.DEMO_SITES_KEY));
  }

  @Test
  void resolveDemoSitesInteractiveDefaultsNoAndPrefersPreFlag() {
    // No CLI pre-population: empty input defaults to No.
    Map<String, String> options = new HashMap<>();
    ScriptedPrompt prompt = new ScriptedPrompt("");
    boolean resolved = InteractiveInstallWizard.resolveDemoSites(true, options, prompt);
    assertFalse(resolved);
    assertEquals("false", options.get(DbInstallConfigResolver.DEMO_SITES_KEY));
    assertTrue(
        prompt.outputsAsString().contains("Install sample sites"),
        "must show the demo-sites prompt; was:\n" + prompt.outputsAsString());

    // With --demo-sites on CLI: empty input honors the pre-population.
    options.clear();
    prompt = new ScriptedPrompt("");
    options.put(DbInstallConfigResolver.DEMO_SITES_KEY, "true");
    resolved = InteractiveInstallWizard.resolveDemoSites(true, options, prompt);
    assertTrue(resolved);

    // Explicit operator answer y.
    options.clear();
    prompt = new ScriptedPrompt("y");
    resolved = InteractiveInstallWizard.resolveDemoSites(true, options, prompt);
    assertTrue(resolved);
    assertEquals("true", options.get(DbInstallConfigResolver.DEMO_SITES_KEY));
  }

  @Test
  void summaryMentionsSampleSitesState() {
    DbInstallConfigResolver.ResolvedDbConfig db = DbInstallConfigResolver.resolveDbConfig(Map.of());
    Path install = tempDir.resolve("any-root");
    String enabled = InteractiveInstallWizard.buildSummary(install, db, null, true, false);
    assertTrue(enabled.contains("Sample sites : enabled"));
    String disabled = InteractiveInstallWizard.buildSummary(install, db, null, false, false);
    assertTrue(disabled.contains("Sample sites : disabled"));
    // Upgrades also surface the enabled/disabled state, not a "skipped" sentinel.
    String upgradeEnabled = InteractiveInstallWizard.buildSummary(install, db, null, true, true);
    assertTrue(upgradeEnabled.contains("Sample sites : enabled"));
  }

  @Test
  void usageMessageMentionsDemoSitesFlag() {
    String msg = InteractiveInstallWizard.usageMessage();
    assertTrue(
        msg.contains("--demo-sites"), "usage message must mention --demo-sites; was:\n" + msg);
    assertTrue(msg.contains("--no-demo-sites"));
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
