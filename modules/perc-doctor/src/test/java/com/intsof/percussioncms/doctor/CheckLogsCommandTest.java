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
package com.intsof.percussioncms.doctor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CheckLogsCommandTest {

  @TempDir Path tempDir;

  private Path installRoot;

  @BeforeEach
  void setUp() throws Exception {
    installRoot = Files.createDirectories(tempDir.resolve("cms-install"));
  }

  @Test
  void missingLogsSkipAndStayHealthyWithoutRequireFlags() throws Exception {
    CheckLogsReport report = CheckLogsCommand.execute(installRoot, true);
    assertEquals(CheckLogsCommand.COMMAND_NAME, report.getCommand());
    assertTrue(report.isDryRun());
    assertTrue(report.isHealthy());
    assertTrue(report.getSkipCount() >= 1);
  }

  @Test
  void cleanServerLogPasses() throws Exception {
    Path log =
        Files.createDirectories(installRoot.resolve("jetty").resolve("base").resolve("logs"))
            .resolve("server.log");
    Files.writeString(
        log,
        "2026-08-09 INFO [Server] Started\nINFO [PSServer] ready\n",
        StandardCharsets.UTF_8);

    CheckLogsReport report =
        CheckLogsCommand.execute(
            installRoot,
            false,
            new CheckLogsCommand.Options(CheckLogsCommand.PHASE_STARTUP, 1000, true, false));
    assertTrue(report.isHealthy());
    assertTrue(report.getPassCount() >= 1);
    assertTrue(hasStatus(report, CheckLogsReport.CheckStatus.PASS, "log.server.content"));
  }

  @Test
  void errorInServerLogFails() throws Exception {
    Path log =
        Files.createDirectories(installRoot.resolve("jetty").resolve("base").resolve("logs"))
            .resolve("server.log");
    Files.writeString(
        log,
        "INFO start\nERROR [PSX] bean wiring failed\n",
        StandardCharsets.UTF_8);

    CheckLogsReport report =
        CheckLogsCommand.execute(
            installRoot,
            false,
            new CheckLogsCommand.Options(CheckLogsCommand.PHASE_STARTUP, 1000, false, false));
    assertFalse(report.isHealthy());
    assertEquals(1, report.getFailCount());
    assertTrue(report.firstFailMatch().contains("ERROR"));
  }

  @Test
  void installPhaseScansInstallPackagesAndTableFactory() throws Exception {
    Path installer = Files.createDirectories(installRoot.resolve("rxconfig").resolve("Installer"));
    Files.writeString(
        installer.resolve("InstallPackages.log"),
        "INFO package ok\n",
        StandardCharsets.UTF_8);
    Files.writeString(
        installer.resolve("install.log"), "INFO install complete\n", StandardCharsets.UTF_8);
    Files.writeString(
        installer.resolve("tablefactory.log"),
        "ERROR tablefactory failed\n",
        StandardCharsets.UTF_8);

    CheckLogsReport report =
        CheckLogsCommand.execute(
            installRoot,
            false,
            new CheckLogsCommand.Options(CheckLogsCommand.PHASE_INSTALL, 500, false, true));
    assertFalse(report.isHealthy());
    assertTrue(hasStatus(report, CheckLogsReport.CheckStatus.FAIL, "log.tablefactory.content"));
  }

  @Test
  void requireStartupFailsWhenServerLogMissing() throws Exception {
    CheckLogsReport report =
        CheckLogsCommand.execute(
            installRoot,
            false,
            new CheckLogsCommand.Options(CheckLogsCommand.PHASE_STARTUP, 100, true, false));
    assertFalse(report.isHealthy());
    assertTrue(hasStatus(report, CheckLogsReport.CheckStatus.FAIL, "require.startup"));
  }

  @Test
  void parsePhaseRejectsUnknown() {
    assertThrows(IllegalArgumentException.class, () -> CheckLogsCommand.parsePhase("nope"));
    assertEquals(CheckLogsCommand.PHASE_ALL, CheckLogsCommand.parsePhase("ALL"));
  }

  @Test
  void lastNLinesHelper() {
    // split("\n", -1) keeps a trailing empty segment after a final newline
    assertEquals("c\nd", CheckLogsCommand.lastNLines("a\nb\nc\nd", 2));
    assertEquals("d\n", CheckLogsCommand.lastNLines("a\nb\nc\nd\n", 2));
  }

  private static boolean hasStatus(
      CheckLogsReport report, CheckLogsReport.CheckStatus status, String id) {
    return report.getChecks().stream()
        .anyMatch(c -> c.getStatus() == status && id.equals(c.getId()));
  }
}
