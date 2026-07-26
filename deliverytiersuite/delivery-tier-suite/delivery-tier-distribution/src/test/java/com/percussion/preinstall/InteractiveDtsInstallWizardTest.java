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
import static org.junit.jupiter.api.Assertions.assertTrue;

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
class InteractiveDtsInstallWizardTest {

  @TempDir Path tempDir;

  private Path runningJavaHome() {
    return Path.of(System.getProperty("java.home")).toAbsolutePath().normalize();
  }

  @Test
  void nonInteractiveMissingPathReturnsUsage() {
    MainDTSPreInstall.ParsedArgs parsed = new MainDTSPreInstall.ParsedArgs(null, Map.of());
    InteractiveDtsInstallWizard.WizardResult result =
        InteractiveDtsInstallWizard.run(parsed, false, null, runningJavaHome(), null);
    assertFalse(result.proceed());
    assertEquals(InteractiveDtsInstallWizard.EXIT_USAGE, result.exitCode());
    assertTrue(result.message().contains("installation or upgrade folder"));
  }

  @Test
  void nonInteractiveWithPathProceedsDefaultH2Production() {
    Path install = tempDir.resolve("dts");
    MainDTSPreInstall.ParsedArgs parsed =
        new MainDTSPreInstall.ParsedArgs(install, Map.of());
    InteractiveDtsInstallWizard.WizardResult result =
        InteractiveDtsInstallWizard.run(parsed, false, null, runningJavaHome(), null);
    assertTrue(result.proceed());
    assertEquals(install.toAbsolutePath().normalize(), result.installPath());
    assertEquals("h2", result.dbConfig().systemProperties().get("perc.db.type"));
    assertEquals("true", result.isProduction());
    assertNotNull(result.javaOutcome());
    assertTrue(Files.exists(install.resolve("java.properties")));
  }

  @Test
  void interactiveFullFlowH2Production() {
    Path install = tempDir.resolve("dts-i");
    // path, server type default, DB H2, confirm Y
    ScriptedPrompt prompt = new ScriptedPrompt(install.toString(), "", "", "");
    MainDTSPreInstall.ParsedArgs parsed = new MainDTSPreInstall.ParsedArgs(null, Map.of());
    InteractiveDtsInstallWizard.WizardResult result =
        InteractiveDtsInstallWizard.run(parsed, true, prompt, runningJavaHome(), null);
    assertTrue(result.proceed());
    assertEquals("true", result.isProduction());
    assertEquals("h2", result.dbConfig().systemProperties().get("perc.db.type"));
    assertTrue(prompt.outputsAsString().contains("DTS installation summary"));
  }

  @Test
  void interactiveStagingAndSqlServer() {
    Path install = tempDir.resolve("dts-ss");
    // server type 2 staging, DB menu 2 SQL Server, fields, skip test, confirm y
    ScriptedPrompt prompt =
        new ScriptedPrompt(
            "2",
            "2",
            "localhost",
            "1433",
            "percussion_dts",
            "DBO",
            "sa",
            "secret-pw",
            "true",
            "true",
            "n",
            "y");
    MainDTSPreInstall.ParsedArgs parsed =
        new MainDTSPreInstall.ParsedArgs(install, Map.of());
    InteractiveDtsInstallWizard.WizardResult result =
        InteractiveDtsInstallWizard.run(parsed, true, prompt, runningJavaHome(), null);
    assertTrue(result.proceed());
    assertEquals("false", result.isProduction());
    assertEquals("sqlserver", result.dbConfig().systemProperties().get("perc.db.type"));
    assertTrue(prompt.outputsAsString().toLowerCase().contains("express"));
    assertFalse(prompt.outputsAsString().contains("secret-pw"));
    assertTrue(prompt.outputsAsString().contains("Staging"));
  }

  @Test
  void interactiveConfirmNoAborts() {
    Path install = tempDir.resolve("dts-abort");
    ScriptedPrompt prompt = new ScriptedPrompt("1", "1", "n");
    MainDTSPreInstall.ParsedArgs parsed =
        new MainDTSPreInstall.ParsedArgs(install, Map.of());
    InteractiveDtsInstallWizard.WizardResult result =
        InteractiveDtsInstallWizard.run(parsed, true, prompt, runningJavaHome(), null);
    assertFalse(result.proceed());
    assertTrue(result.message().toLowerCase().contains("cancelled"));
  }

  @Test
  void upgradeDetectsDeploymentTreeAndSkipsDbMenu() throws Exception {
    Path install = tempDir.resolve("dts-upg");
    Files.createDirectories(install.resolve("Deployment"));
    ScriptedPrompt prompt = new ScriptedPrompt("y");
    MainDTSPreInstall.ParsedArgs parsed =
        new MainDTSPreInstall.ParsedArgs(install, Map.of());
    InteractiveDtsInstallWizard.WizardResult result =
        InteractiveDtsInstallWizard.run(parsed, true, prompt, runningJavaHome(), "true");
    assertTrue(result.proceed());
    assertTrue(prompt.outputsAsString().toLowerCase().contains("upgrade"));
    assertTrue(prompt.outputsAsString().contains("Upgrade"));
  }

  @Test
  void invalidJavaAborts() {
    Path install = tempDir.resolve("dts-bad-java");
    Path invalid = tempDir.resolve("no-jdk");
    MainDTSPreInstall.ParsedArgs parsed =
        new MainDTSPreInstall.ParsedArgs(install, Map.of());
    InteractiveDtsInstallWizard.WizardResult result =
        InteractiveDtsInstallWizard.run(parsed, false, null, invalid, null);
    assertFalse(result.proceed());
    assertEquals(InteractiveDtsInstallWizard.EXIT_JAVA, result.exitCode());
  }

  @Test
  void externalDbCliOverrideSkipsMenu() {
    Path install = tempDir.resolve("dts-cli-db");
    Map<String, String> opts = new HashMap<>();
    opts.put("db.type", "mysql");
    opts.put("db.host", "db.example.com");
    opts.put("db.port", "3306");
    opts.put("db.name", "dts");
    opts.put("db.user", "u");
    opts.put("db.password", "p-secret");
    // test connection n, confirm yes
    ScriptedPrompt prompt = new ScriptedPrompt("n", "yes");
    MainDTSPreInstall.ParsedArgs parsed =
        new MainDTSPreInstall.ParsedArgs(install, opts);
    InteractiveDtsInstallWizard.WizardResult result =
        InteractiveDtsInstallWizard.run(parsed, true, prompt, runningJavaHome(), "true");
    assertTrue(result.proceed());
    assertEquals("mysql", result.dbConfig().systemProperties().get("perc.db.type"));
    assertFalse(prompt.outputsAsString().contains("p-secret"));
  }

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
