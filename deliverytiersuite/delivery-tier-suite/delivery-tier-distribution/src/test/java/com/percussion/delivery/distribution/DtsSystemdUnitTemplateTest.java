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

package com.percussion.delivery.distribution;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * GH-962 / GH-1977: DTS Tomcat systemd unit template contract (Production + Staging share one
 * template). Aligns with {@code
 * specs/988-linux-systemd-services/contracts/systemd-unit-contract.md} and CMS {@code
 * SystemdUnitTemplateTest} parity.
 */
class DtsSystemdUnitTemplateTest {

  private static final Path UNIT_TEMPLATE =
      Path.of("src", "main", "rootFiles", "dts-tomcat.service.in");

  private static final Path README = Path.of("src", "main", "rootFiles", "README-systemd.md");

  private static String unitText;
  private static String readmeText;

  @BeforeAll
  static void load() throws Exception {
    assertTrue(
        Files.isRegularFile(UNIT_TEMPLATE), () -> "missing " + UNIT_TEMPLATE.toAbsolutePath());
    assertTrue(Files.isRegularFile(README), () -> "missing " + README.toAbsolutePath());
    unitText = Files.readString(UNIT_TEMPLATE, StandardCharsets.UTF_8);
    readmeText = Files.readString(README, StandardCharsets.UTF_8);
  }

  @Test
  void unit_isTypeForking_withPidEnvAndExec() {
    assertTrue(unitText.contains("Type=forking"));
    assertTrue(
        unitText.contains("fork+PID")
            || unitText.contains("CATALINA_PID")
            || unitText.contains("fork"),
        "documents forking / PID behavior");
    assertTrue(unitText.contains("PIDFile=@PID_FILE@"));
    assertTrue(
        unitText.contains("EnvironmentFile=-@ENV_FILE@")
            || unitText.contains("EnvironmentFile=@ENV_FILE@"));
    assertTrue(unitText.contains("ExecStart=@INIT_SCRIPT@ start"));
    assertTrue(unitText.contains("ExecStop=@INIT_SCRIPT@ stop"));
    assertTrue(
        !unitText.contains("ExecReload="),
        "no ExecReload (restart is systemctl restart, not reload)");
  }

  @Test
  void unit_hasLongStartTimeoutAndJournal() {
    assertTrue(unitText.contains("TimeoutStartSec=1800"));
    int timeout = -1;
    for (String line : unitText.split("\n")) {
      String t = line.trim();
      if (t.startsWith("TimeoutStartSec=")) {
        timeout = Integer.parseInt(t.substring("TimeoutStartSec=".length()).trim());
      }
    }
    assertTrue(timeout >= 900, "TimeoutStartSec >= 900, was " + timeout);
    assertTrue(unitText.contains("StandardOutput=journal"));
    assertTrue(unitText.contains("StandardError=journal"));
    assertTrue(unitText.contains("WantedBy=multi-user.target"));
    assertTrue(unitText.contains("After=network.target"), "After=network");
  }

  @Test
  void unit_hasDescriptionAndPrivilegeModelDocumented() {
    assertTrue(unitText.contains("Description=@DESCRIPTION@"), "Description placeholder");
    boolean hasUserDirective = unitText.lines().anyMatch(l -> l.trim().startsWith("User="));
    boolean documentsPrivilege =
        unitText.contains("User=")
            || unitText.toLowerCase().contains("privilege")
            || unitText.contains("init.d");
    assertTrue(
        hasUserDirective || documentsPrivilege,
        "User= key or privilege model documentation required by contract");
  }

  @Test
  void readme_documentsFlagsDryRunAndMigration() {
    assertTrue(readmeText.contains("--systemd"), "documents --systemd");
    assertTrue(readmeText.contains("--initd"), "documents --initd");
    assertTrue(
        readmeText.toLowerCase().contains("dry-run") || readmeText.contains("non-root"),
        "documents dry-run / non-root limitations");
    assertTrue(
        readmeText.contains("must be run")
            || readmeText.toLowerCase().contains("requires root")
            || readmeText.toLowerCase().contains("require root"),
        "documents root requirement");
    assertTrue(readmeText.contains("uninstall"), "documents uninstall");
    assertTrue(
        readmeText.toLowerCase().contains("migration")
            || readmeText.contains("uninstall then install"),
        "documents migration uninstall→install");
    assertTrue(
        readmeText.contains("init.d") || readmeText.contains("SysV"),
        "documents init.d helper/fallback retained");
    assertTrue(
        readmeText.contains("not removed")
            || readmeText.contains("Start helper")
            || readmeText.contains("start helper"),
        "documents init.d remains start helper + fallback");
  }
}
