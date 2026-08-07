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
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CheckConfigCommandTest {

  @TempDir Path tempDir;

  private Path installRoot;

  @BeforeEach
  void setUp() throws Exception {
    installRoot = Files.createDirectories(tempDir.resolve("cms-install"));
  }

  @Test
  void bareInstallWarnsOnMissingConfigsAndStaysHealthyWithoutFail() throws Exception {
    CheckConfigReport report = CheckConfigCommand.execute(installRoot, true);
    assertEquals(CheckConfigCommand.COMMAND_NAME, report.getCommand());
    assertTrue(report.isDryRun());
    assertTrue(report.getCheckCount() >= 2);
    // Missing configs are WARN, not FAIL — install may be partial.
    assertTrue(report.isHealthy(), "missing optional configs should be WARN not FAIL");
    assertTrue(report.getWarnCount() >= 2);
    assertTrue(hasStatusId(report, CheckConfigReport.CheckStatus.WARN, "server.properties.present"));
    assertTrue(
        hasStatusId(report, CheckConfigReport.CheckStatus.WARN, "rxrepository.properties.present"));
  }

  @Test
  void healthyServerAndH2RepoProducesPassAndInfo() throws Exception {
    writeServerProps(
        "enableDebugTools=false\n"
            + "disableCrossSiteRequestForgeryCheck=false\n"
            + "requireHTTPS=true\n"
            + "bindPort=9992\n");
    writeRepoProps(
        "DB_BACKEND=H2\n"
            + "DB_DRIVER_NAME=h2\n"
            + "DB_DRIVER_CLASS_NAME=org.h2.Driver\n"
            + "DB_SERVER=file:../../Repository/CMDB\n"
            + "UID=sa\n"
            + "PWD=\n");

    CheckConfigReport report = CheckConfigCommand.execute(installRoot, false);
    assertFalse(report.isDryRun());
    assertTrue(report.isHealthy());
    assertTrue(hasStatusId(report, CheckConfigReport.CheckStatus.PASS, "server.enableDebugTools"));
    assertTrue(hasStatusId(report, CheckConfigReport.CheckStatus.PASS, "server.bindPort"));
    assertTrue(hasStatusId(report, CheckConfigReport.CheckStatus.PASS, "repo.DB_BACKEND"));
    assertTrue(hasStatusId(report, CheckConfigReport.CheckStatus.INFO, "repo.PWD"));
  }

  @Test
  void debugToolsAndCsrfDisableWarn() throws Exception {
    writeServerProps(
        "enableDebugTools=true\n"
            + "disableCrossSiteRequestForgeryCheck=true\n"
            + "bindPort=9992\n");
    writeRepoProps(h2RepoBody());

    CheckConfigReport report = CheckConfigCommand.execute(installRoot, true);
    assertTrue(report.isHealthy());
    assertTrue(hasStatusId(report, CheckConfigReport.CheckStatus.WARN, "server.enableDebugTools"));
    assertTrue(
        hasStatusId(
            report,
            CheckConfigReport.CheckStatus.WARN,
            "server.disableCrossSiteRequestForgeryCheck"));
  }

  @Test
  void invalidBindPortFails() throws Exception {
    writeServerProps("bindPort=not-a-port\n");
    writeRepoProps(h2RepoBody());

    CheckConfigReport report = CheckConfigCommand.execute(installRoot, true);
    assertFalse(report.isHealthy());
    assertTrue(hasStatusId(report, CheckConfigReport.CheckStatus.FAIL, "server.bindPort"));
  }

  @Test
  void unresolvedPlaceholderInBindPortFails() throws Exception {
    writeServerProps("bindPort=${cms.port}\n");
    writeRepoProps(h2RepoBody());

    CheckConfigReport report = CheckConfigCommand.execute(installRoot, true);
    assertFalse(report.isHealthy());
    assertTrue(hasStatusId(report, CheckConfigReport.CheckStatus.FAIL, "server.bindPort"));
  }

  @Test
  void missingRequiredRepoKeyFails() throws Exception {
    writeServerProps("bindPort=9992\nenableDebugTools=false\n");
    writeRepoProps(
        "DB_BACKEND=MSSQL\n"
            + "DB_DRIVER_NAME=sqlserver\n"
            + "DB_DRIVER_CLASS_NAME=com.microsoft.sqlserver.jdbc.SQLServerDriver\n"
            + "DB_SERVER=\n"
            + "UID=cms\n"
            + "PWD=demo\n"
            + "PWD_ENCRYPTED=N\n");

    CheckConfigReport report = CheckConfigCommand.execute(installRoot, true);
    assertFalse(report.isHealthy());
    assertTrue(hasStatusId(report, CheckConfigReport.CheckStatus.FAIL, "repo.DB_SERVER"));
    assertTrue(hasStatusId(report, CheckConfigReport.CheckStatus.WARN, "repo.PWD"));
    assertTrue(hasStatusId(report, CheckConfigReport.CheckStatus.WARN, "repo.PWD_ENCRYPTED"));
  }

  @Test
  void driverBackendMismatchWarns() throws Exception {
    writeServerProps("bindPort=9992\n");
    writeRepoProps(
        "DB_BACKEND=MSSQL\n"
            + "DB_DRIVER_NAME=h2\n"
            + "DB_DRIVER_CLASS_NAME=org.h2.Driver\n"
            + "DB_SERVER=//sql.example.com:1433\n"
            + "UID=cms\n"
            + "PWD=s3cureValue\n"
            + "PWD_ENCRYPTED=Y\n");

    CheckConfigReport report = CheckConfigCommand.execute(installRoot, true);
    assertTrue(report.isHealthy());
    assertTrue(
        hasStatusId(report, CheckConfigReport.CheckStatus.WARN, "repo.driver-backend-consistency"));
  }

  @Test
  void neverWritesUnderInstallRoot() throws Exception {
    writeServerProps("bindPort=9992\nenableDebugTools=true\n");
    writeRepoProps(h2RepoBody());
    Path marker = installRoot.resolve("marker.txt");
    Files.writeString(marker, "keep");
    long before = Files.walk(installRoot).count();

    CheckConfigCommand.execute(installRoot, false);

    assertTrue(Files.exists(marker));
    assertEquals(before, Files.walk(installRoot).count());
    assertEquals(
        "keep", Files.readString(marker, StandardCharsets.UTF_8));
  }

  @Test
  void missingInstallRootRejected() {
    Path missing = tempDir.resolve("gone");
    assertThrows(IllegalArgumentException.class, () -> CheckConfigCommand.execute(missing, true));
  }

  @Test
  void allResolvedPathsStayUnderInstallRoot() throws Exception {
    writeServerProps("bindPort=9992\n");
    writeRepoProps(h2RepoBody());
    CheckConfigReport report = CheckConfigCommand.execute(installRoot, true);
    for (CheckConfigReport.Check c : report.getChecks()) {
      if (c.getPath() == null) {
        continue;
      }
      assertTrue(
          InstallRootGuard.isUnderInstallRoot(installRoot, c.getPath()),
          "path escaped root: " + c.getPath());
    }
  }

  @Test
  void weakPasswordHelperRecognizesKnownTokens() {
    assertTrue(CheckConfigCommand.isWeakPassword("demo"));
    assertTrue(CheckConfigCommand.isWeakPassword("Password"));
    assertFalse(CheckConfigCommand.isWeakPassword("s3cureValue!"));
  }

  private void writeServerProps(String body) throws Exception {
    Path dir = Files.createDirectories(installRoot.resolve("rxconfig").resolve("Server"));
    Files.writeString(dir.resolve("server.properties"), body, StandardCharsets.UTF_8);
  }

  private void writeRepoProps(String body) throws Exception {
    Path dir = Files.createDirectories(installRoot.resolve("rxconfig").resolve("Installer"));
    Files.writeString(dir.resolve("rxrepository.properties"), body, StandardCharsets.UTF_8);
  }

  private static String h2RepoBody() {
    return "DB_BACKEND=H2\n"
        + "DB_DRIVER_NAME=h2\n"
        + "DB_DRIVER_CLASS_NAME=org.h2.Driver\n"
        + "DB_SERVER=file:../../Repository/CMDB\n"
        + "UID=sa\n"
        + "PWD=\n";
  }

  private static boolean hasStatusId(
      CheckConfigReport report, CheckConfigReport.CheckStatus status, String id) {
    List<String> matches =
        report.getChecks().stream()
            .filter(c -> c.getStatus() == status && id.equals(c.getId()))
            .map(CheckConfigReport.Check::getId)
            .collect(Collectors.toList());
    return !matches.isEmpty();
  }
}
