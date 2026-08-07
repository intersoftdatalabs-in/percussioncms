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

package com.percussion.delivery.distribution;

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
 * Issue #2348: standalone DTS ships sample logrotate under {@code rootFiles/logrotate/} and {@code
 * installDts.xml} copies that tree to the install root (bulk {@code *} copy is files-only).
 *
 * <p>Structural only — no live logrotate, no root.
 */
class DtsLogrotateSamplePackagingTest {

  private static final Path ROOT_FILES = Path.of("src", "main", "rootFiles");
  private static final Path LOGROTATE_DIR = ROOT_FILES.resolve("logrotate");
  private static final Path POLICY = LOGROTATE_DIR.resolve("percussion-dts");
  private static final Path README = LOGROTATE_DIR.resolve("README.md");
  private static final Path INSTALL_DTS =
      ROOT_FILES.resolve(Path.of("rxconfig", "Installer", "installDts.xml"));

  private static final Pattern HARDCODED_USER_HOME =
      Pattern.compile(
          "(?i)(C:\\\\Users\\\\[A-Za-z]|/home/[a-zA-Z0-9._-]+|/Users/[A-Za-z]|\\\\Users\\\\Nate)");

  @Test
  @DisplayName("rootFiles ship DTS logrotate sample + README")
  void rootFilesShipLogrotateSample() {
    assertTrue(Files.isDirectory(LOGROTATE_DIR), () -> "missing " + LOGROTATE_DIR.toAbsolutePath());
    assertTrue(Files.isRegularFile(POLICY), () -> "missing " + POLICY.toAbsolutePath());
    assertTrue(Files.isRegularFile(README), () -> "missing " + README.toAbsolutePath());
  }

  @Test
  @DisplayName("DTS logrotate sample covers Deployment/Server/logs with copytruncate")
  void policyContent() throws IOException {
    String text = Files.readString(POLICY, StandardCharsets.UTF_8);
    assertFalse(HARDCODED_USER_HOME.matcher(text).find(), "policy must not hardcode user home");
    assertTrue(text.contains("Deployment/Server/logs"), "must target Tomcat logs dir");
    assertTrue(text.contains("*.out"), "must cover catalina.out via *.out");
    assertTrue(text.contains("copytruncate"), "must prefer copytruncate");
    assertTrue(text.contains("rotate 14"), "default rotate 14");
    assertTrue(
        text.toLowerCase().contains("sample") || text.contains("NOT installed"),
        "must state sample / not auto-installed");
  }

  @Test
  @DisplayName("installDts.xml copies logrotate tree on fresh install and upgrade")
  void installDtsCopiesLogrotateTree() throws IOException {
    assertTrue(Files.isRegularFile(INSTALL_DTS), () -> "missing " + INSTALL_DTS.toAbsolutePath());
    String xml = Files.readString(INSTALL_DTS, StandardCharsets.UTF_8);
    assertTrue(
        xml.contains("logrotate"),
        "installDts.xml must reference logrotate sample packaging (GH-2348)");
    assertTrue(
        xml.contains("install.src}/logrotate") || xml.contains("${install.src}/logrotate"),
        "installDts must copy from install.src/logrotate");
    assertTrue(
        xml.contains("/logrotate"),
        "installDts must land samples under install-root logrotate/");
    // Expect at least two copy blocks (fresh + upgrade) mentioning logrotate
    int idx = 0;
    int count = 0;
    while ((idx = xml.indexOf("logrotate", idx)) >= 0) {
      count++;
      idx += "logrotate".length();
    }
    assertTrue(count >= 4, "expect multiple logrotate references (comment + fresh + upgrade)");
  }

  @Test
  @DisplayName("DTS logrotate README documents dry-run and opt-in install")
  void readmeDocumentsOptIn() throws IOException {
    String text = Files.readString(README, StandardCharsets.UTF_8);
    assertFalse(HARDCODED_USER_HOME.matcher(text).find(), "README must not hardcode user home");
    assertTrue(text.contains("logrotate -d"), "README must document dry-run");
    assertTrue(text.contains("/etc/logrotate.d"), "README must document logrotate.d path");
    assertTrue(
        text.toLowerCase().contains("not auto")
            || text.toLowerCase().contains("operator consent")
            || text.contains("Not auto-enabled"),
        "README must state not auto-enabled");
  }
}
