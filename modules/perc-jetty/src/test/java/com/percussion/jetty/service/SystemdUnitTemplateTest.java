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

package com.percussion.jetty.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * GH-962 / specs/988: structural contract for the shipped systemd unit template.
 *
 * @see specs/988-linux-systemd-services/contracts/systemd-unit-contract.md
 */
class SystemdUnitTemplateTest {

  private static final Path UNIT_TEMPLATE =
      Path.of("src", "main", "jetty", "service", "percussion-cms.service.in");

  private static String unitText;

  @BeforeAll
  static void load() throws Exception {
    assertTrue(
        Files.isRegularFile(UNIT_TEMPLATE), () -> "missing " + UNIT_TEMPLATE.toAbsolutePath());
    unitText = Files.readString(UNIT_TEMPLATE, StandardCharsets.UTF_8);
  }

  @Test
  void unit_isTypeForking_withPidAndEnvFile() {
    assertTrue(unitText.contains("Type=forking"), "Type=forking");
    assertTrue(
        unitText.contains("start-stop-daemon") || unitText.contains("fork+PID"),
        "documents forking dependency on start helper");
    assertTrue(unitText.contains("PIDFile=@PID_FILE@"), "PIDFile placeholder");
    assertTrue(
        unitText.contains("EnvironmentFile=-@ENV_FILE@")
            || unitText.contains("EnvironmentFile=@ENV_FILE@"),
        "EnvironmentFile");
    assertTrue(unitText.contains("ExecStart=@INIT_SCRIPT@ start"), "ExecStart");
    assertTrue(unitText.contains("ExecStop=@INIT_SCRIPT@ stop"), "ExecStop");
    assertTrue(
        !unitText.contains("ExecReload="),
        "no ExecReload (restart is systemctl restart, not reload)");
  }

  @Test
  void unit_hasLongStartTimeoutAndJournal() {
    assertTrue(unitText.contains("TimeoutStartSec=1800"), "TimeoutStartSec default 30m (FR-005)");
    // Parse numeric value for contract min 900
    int timeout = extractTimeoutStartSec(unitText);
    assertTrue(timeout >= 900, "TimeoutStartSec must be >= 900, was " + timeout);
    assertTrue(unitText.contains("StandardOutput=journal"), "journal stdout");
    assertTrue(unitText.contains("StandardError=journal"), "journal stderr");
  }

  @Test
  void unit_hasInstallWantedByMultiUser() {
    assertTrue(unitText.contains("[Install]"), "[Install]");
    assertTrue(unitText.contains("WantedBy=multi-user.target"), "WantedBy");
    assertTrue(unitText.contains("After=network.target"), "After=network");
  }

  private static int extractTimeoutStartSec(String text) {
    for (String line : text.split("\n")) {
      String trimmed = line.trim();
      if (trimmed.startsWith("TimeoutStartSec=")) {
        String v = trimmed.substring("TimeoutStartSec=".length()).trim();
        return Integer.parseInt(v);
      }
    }
    return -1;
  }
}
