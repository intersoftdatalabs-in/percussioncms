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

package com.percussion.distribution.install;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Issue #2348: CMS distribution ships default Linux logrotate samples + Windows clean-logs guidance
 * under {@code rxconfig/Installer/logrotate/}.
 *
 * <p>Structural source contracts only (no live logrotate, no root). Samples must stay portable (no
 * developer home paths), cover CMS Jetty + DTS Tomcat roots, prefer {@code copytruncate}, and
 * remain opt-in (not auto-enabled).
 */
class LogrotateSamplePackagingTest {

  private static final Path LOGROTATE_DIR =
      Path.of("src", "main", "resources", "distribution", "rxconfig", "Installer", "logrotate");

  private static final Path PERCUSSION_CMS = LOGROTATE_DIR.resolve("percussion-cms");
  private static final Path PERCUSSION_DTS = LOGROTATE_DIR.resolve("percussion-dts");
  private static final Path WINDOWS_SAMPLE = LOGROTATE_DIR.resolve("schedule-clean-logs.ps1");
  private static final Path README = LOGROTATE_DIR.resolve("README.md");
  private static final Path INSTALL_XML =
      Path.of("src", "main", "resources", "distribution", "rxconfig", "Installer", "install.xml");

  private static final Pattern HARDCODED_USER_HOME =
      Pattern.compile(
          "(?i)(C:\\\\Users\\\\[A-Za-z]|/home/[a-zA-Z0-9._-]+|/Users/[A-Za-z]|\\\\Users\\\\Nate)");

  @Test
  @DisplayName("logrotate sample tree ships CMS, DTS, Windows script, and README")
  void sampleTreeExists() {
    assertTrue(Files.isDirectory(LOGROTATE_DIR), () -> "missing " + LOGROTATE_DIR.toAbsolutePath());
    assertTrue(Files.isRegularFile(PERCUSSION_CMS), () -> "missing " + PERCUSSION_CMS);
    assertTrue(Files.isRegularFile(PERCUSSION_DTS), () -> "missing " + PERCUSSION_DTS);
    assertTrue(Files.isRegularFile(WINDOWS_SAMPLE), () -> "missing " + WINDOWS_SAMPLE);
    assertTrue(Files.isRegularFile(README), () -> "missing " + README);
  }

  @Test
  @DisplayName("CMS policy covers Jetty + perc-logging + audit globs with copytruncate")
  void cmsPolicyCoversJettyRoots() throws IOException {
    String text = Files.readString(PERCUSSION_CMS, StandardCharsets.UTF_8);
    assertFalse(HARDCODED_USER_HOME.matcher(text).find(), "CMS policy must not hardcode user home");
    assertTrue(text.contains("jetty/base/logs"), "CMS policy must include jetty/base/logs");
    assertTrue(
        text.contains("jetty/base/modules/perc-logging/logs"),
        "CMS policy must include perc-logging logs");
    assertTrue(text.contains("logs/audit"), "CMS policy must include audit log dir");
    assertTrue(text.contains("*.log"), "CMS policy must rotate *.log");
    assertTrue(text.contains("*.out"), "CMS policy must rotate *.out");
    assertTrue(text.contains("copytruncate"), "CMS policy must prefer copytruncate");
    assertTrue(text.contains("rotate 14"), "CMS policy default rotate 14");
    assertTrue(
        text.toLowerCase().contains("sample") || text.contains("NOT installed"),
        "CMS policy must state it is a sample / not auto-installed");
  }

  @Test
  @DisplayName("DTS policy covers Deployment/Server/logs and catalina.out via *.out")
  void dtsPolicyCoversTomcatLogs() throws IOException {
    String text = Files.readString(PERCUSSION_DTS, StandardCharsets.UTF_8);
    assertFalse(HARDCODED_USER_HOME.matcher(text).find(), "DTS policy must not hardcode user home");
    assertTrue(
        text.contains("Deployment/Server/logs"), "DTS policy must include Deployment/Server/logs");
    assertTrue(text.contains("*.log") && text.contains("*.out"), "DTS globs for log and out");
    assertTrue(
        text.contains("catalina.out") || text.contains("*.out"),
        "DTS policy must cover catalina.out");
    assertTrue(text.contains("copytruncate"), "DTS policy must prefer copytruncate");
    assertTrue(text.contains("rotate 14"), "DTS policy default rotate 14");
  }

  @Test
  @DisplayName("README documents install dry-run, coexistence, and Windows clean-logs")
  void readmeOperatorGuidance() throws IOException {
    String text = Files.readString(README, StandardCharsets.UTF_8);
    assertFalse(HARDCODED_USER_HOME.matcher(text).find(), "README must not hardcode user home");
    assertTrue(text.contains("logrotate -d"), "README must document logrotate dry-run");
    assertTrue(text.contains("/etc/logrotate.d"), "README must document logrotate.d install path");
    assertTrue(text.toLowerCase().contains("copytruncate"), "README must explain copytruncate");
    assertTrue(
        text.contains("Log4j") || text.contains("Log4j2"),
        "README must document Log4j coexistence");
    assertTrue(
        text.contains("clean-logs") && text.contains("perc-doctor"),
        "README must document perc-doctor clean-logs coexistence");
    assertTrue(
        text.contains("Task Scheduler") || text.contains("schedule-clean-logs"),
        "README must document Windows scheduled clean-logs");
    assertTrue(
        text.toLowerCase().contains("not")
            && (text.toLowerCase().contains("auto-enable")
                || text.toLowerCase().contains("not auto")
                || text.contains("operator consent")),
        "README must state samples are not auto-enabled");
  }

  @Test
  @DisplayName("Windows sample invokes perc-doctor clean-logs with portable install-root")
  void windowsSampleUsesPercDoctor() throws IOException {
    String text = Files.readString(WINDOWS_SAMPLE, StandardCharsets.UTF_8);
    assertFalse(
        HARDCODED_USER_HOME.matcher(text).find(), "Windows sample must not hardcode user home");
    assertTrue(text.contains("clean-logs"), "Windows sample must call clean-logs");
    assertTrue(
        text.contains("perc-doctor") || text.contains("perc-doctor.bat"),
        "Windows sample must invoke perc-doctor");
    assertTrue(
        text.contains("OlderThan") || text.contains("14d"),
        "Windows sample must expose retention (default 14d)");
    assertTrue(
        text.contains("DryRun") || text.contains("dry-run"),
        "Windows sample must default to / support dry-run");
    // Resolve-DefaultInstallRoot: sample is at <install-root>/rxconfig/Installer/logrotate/
    // so climb exactly three parents (not four, which would leave install-root entirely).
    assertTrue(
        text.contains("..\\..\\..") || text.contains("../.."),
        "Resolve-DefaultInstallRoot must climb three levels from logrotate/ to install root");
    assertFalse(
        text.contains("..\\..\\..\\..") || text.contains("../../../.."),
        "must not climb four levels (parent of install root)");
    assertTrue(
        text.toLowerCase().contains("three levels") || text.contains("three levels up"),
        "doc comment must describe three-level climb");
    // Behavioral path math: three parents of .../rxconfig/Installer/logrotate == install root
    Path installRoot = Path.of("install-root").toAbsolutePath().normalize();
    Path scriptDir = installRoot.resolve("rxconfig").resolve("Installer").resolve("logrotate");
    Path resolved = scriptDir.resolve("..").resolve("..").resolve("..").normalize();
    assertEquals(
        installRoot,
        resolved,
        "three-level climb from rxconfig/Installer/logrotate must yield install root");
    Path overshoot = scriptDir.resolve("..").resolve("..").resolve("..").resolve("..").normalize();
    assertFalse(
        installRoot.equals(overshoot),
        "four-level climb must leave the install root (regression for 4-parent bug)");
  }

  @Test
  @DisplayName("install.xml upgrade.overwrite includes rxconfig/Installer/** (logrotate refresh)")
  void installXmlUpgradeIncludesInstallerTree() throws IOException {
    assertTrue(Files.isRegularFile(INSTALL_XML), () -> "missing " + INSTALL_XML.toAbsolutePath());
    String xml = Files.readString(INSTALL_XML, StandardCharsets.UTF_8);
    assertTrue(
        xml.contains("upgrade.overwrite"), "install.xml must define upgrade.overwrite patternset");
    assertTrue(
        xml.contains("rxconfig/Installer/**") || xml.contains("name=\"rxconfig/Installer/**\""),
        "upgrade.overwrite must include rxconfig/Installer/** so logrotate samples refresh");
    assertFalse(
        xml.contains("<exclude name=\"rxconfig/Installer/logrotate")
            || xml.contains("<exclude name=\"**/logrotate/**\""),
        "must not exclude logrotate samples from install packaging");
  }
}
