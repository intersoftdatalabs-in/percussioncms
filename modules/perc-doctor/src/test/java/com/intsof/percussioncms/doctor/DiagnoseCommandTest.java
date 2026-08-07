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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DiagnoseCommandTest {

  @TempDir Path tempDir;

  @Test
  void isDiagnoseCommandRecognizesTokens() {
    assertTrue(DiagnoseCommand.isDiagnoseCommand("diagnose"));
    assertTrue(DiagnoseCommand.isDiagnoseCommand("health"));
    assertFalse(DiagnoseCommand.isDiagnoseCommand("clean-logs"));
    assertFalse(DiagnoseCommand.isDiagnoseCommand("Diagnose"));
    assertFalse(DiagnoseCommand.isDiagnoseCommand(null));
  }

  @Test
  void reportShapeHasStableIdsAndNoMutations() throws Exception {
    Path root = Files.createDirectories(tempDir.resolve("empty-install"));
    Path marker = root.resolve("sentinel.txt");
    Files.writeString(marker, "keep-me", StandardCharsets.UTF_8);

    DiagnoseReport report = DiagnoseCommand.execute(root, true);

    assertEquals(DiagnoseCommand.COMMAND_NAME, report.getCommand());
    assertEquals(root.toAbsolutePath().normalize(), report.getInstallRoot());
    assertTrue(report.isDryRun());
    assertTrue(report.getCheckCount() >= 5, "expected multiple checklist rows");
    assertTrue(Files.exists(marker), "diagnose must never delete files");
    assertTrue(Files.readString(marker).equals("keep-me"));

    Set<String> ids = new HashSet<>();
    for (DiagnoseReport.Check c : report.getChecks()) {
      assertNotNull(c.getId());
      assertFalse(c.getId().isEmpty());
      assertNotNull(c.getStatus());
      assertNotNull(c.getMessage());
      assertFalse(c.getMessage().isEmpty());
      ids.add(c.getId());
    }
    assertTrue(ids.contains("install-root"));
    assertTrue(ids.contains("disk.free"));
    assertTrue(ids.contains("java.version"));
    assertTrue(ids.contains("java.major"));
    assertTrue(ids.contains("layout.jetty.base") || ids.contains("layout.jetty"));
    assertTrue(ids.stream().anyMatch(id -> id.startsWith("config.")));
    assertTrue(ids.stream().anyMatch(id -> id.startsWith("logs.")));
  }

  @Test
  void emptyTreeFailsCriticalLayoutAndWarnsConfigs() throws Exception {
    Path root = Files.createDirectories(tempDir.resolve("bare"));
    DiagnoseReport report = DiagnoseCommand.execute(root, false, "health");

    assertEquals("health", report.getCommand());
    assertFalse(report.isDryRun());
    assertFalse(report.isHealthy(), "missing jetty/rxconfig should fail health");
    assertTrue(report.getFailCount() >= 1);

    DiagnoseReport.Check jettyBase =
        findCheck(report, "layout.jetty.base");
    assertNotNull(jettyBase);
    assertEquals(DiagnoseReport.CheckStatus.FAIL, jettyBase.getStatus());

    DiagnoseReport.Check serverProps =
        findCheck(report, "config.rxconfig.Server.server.properties");
    assertNotNull(serverProps);
    assertEquals(DiagnoseReport.CheckStatus.WARN, serverProps.getStatus());
  }

  @Test
  void fullLayoutPassesCriticalChecks() throws Exception {
    Path root = Files.createDirectories(tempDir.resolve("full"));
    Files.createDirectories(root.resolve("jetty").resolve("base").resolve("logs"));
    Files.createDirectories(
        root.resolve("jetty")
            .resolve("base")
            .resolve("modules")
            .resolve("perc-logging")
            .resolve("logs"));
    Files.createDirectories(root.resolve("Deployment").resolve("Server").resolve("logs"));
    Files.createDirectories(root.resolve("bin"));
    Files.createDirectories(root.resolve("rxconfig").resolve("Server"));
    Files.createDirectories(root.resolve("rxconfig").resolve("Installer"));
    Files.writeString(
        root.resolve("rxconfig").resolve("Server").resolve("server.properties"), "x=1");
    Files.writeString(
        root.resolve("rxconfig").resolve("Installer").resolve("rxrepository.properties"), "y=2");

    DiagnoseReport report = DiagnoseCommand.execute(root, true);

    assertTrue(report.isHealthy(), "full synthetic tree should have zero FAIL: " + summarize(report));
    assertEquals(DiagnoseReport.CheckStatus.PASS, findCheck(report, "layout.jetty.base").getStatus());
    assertEquals(DiagnoseReport.CheckStatus.PASS, findCheck(report, "layout.rxconfig").getStatus());
    assertEquals(
        DiagnoseReport.CheckStatus.PASS,
        findCheck(report, "config.rxconfig.Server.server.properties").getStatus());
    assertEquals(
        DiagnoseReport.CheckStatus.PASS, findCheck(report, "logs.jetty.base.logs").getStatus());
    // Never delete configs either
    assertTrue(
        Files.exists(root.resolve("rxconfig").resolve("Server").resolve("server.properties")));
  }

  @Test
  void pathGuardsRejectDotDotRelative() {
    Path root = tempDir.resolve("guard-root");
    // resolveRelativeUnderRoot must reject ".."
    assertEquals(null, DiagnoseCommand.resolveChecked(root, "../escape"));
    assertEquals(null, InstallRootGuard.resolveRelativeUnderRoot(root, "jetty/../../etc"));
  }

  @Test
  void checkPathsStayUnderInstallRoot() throws Exception {
    Path root = Files.createDirectories(tempDir.resolve("contained"));
    Files.createDirectories(root.resolve("jetty").resolve("base"));
    Files.createDirectories(root.resolve("rxconfig"));

    DiagnoseReport report = DiagnoseCommand.execute(root, true);
    for (DiagnoseReport.Check c : report.getChecks()) {
      if (c.getPath() == null) {
        continue;
      }
      assertTrue(
          InstallRootGuard.isUnderInstallRoot(root, c.getPath()),
          "check path must stay under install root: " + c.getId() + " -> " + c.getPath());
    }
  }

  @Test
  void missingInstallRootThrows() {
    Path missing = tempDir.resolve("nope");
    assertThrows(IllegalArgumentException.class, () -> DiagnoseCommand.execute(missing, true));
  }

  @Test
  void parseJavaMajorHandlesLegacyAndModern() {
    assertEquals(8, DiagnoseCommand.parseJavaMajor("1.8.0_402"));
    assertEquals(11, DiagnoseCommand.parseJavaMajor("11.0.22"));
    assertEquals(21, DiagnoseCommand.parseJavaMajor("21.0.2"));
    assertEquals(21, DiagnoseCommand.parseJavaMajor("21"));
    assertEquals(0, DiagnoseCommand.parseJavaMajor(""));
    assertEquals(0, DiagnoseCommand.parseJavaMajor(null));
  }

  @Test
  void formatBytesUsesBinaryUnits() {
    assertEquals("0 B", DiagnoseCommand.formatBytes(0));
    assertTrue(DiagnoseCommand.formatBytes(1536).contains("KiB"));
    assertTrue(DiagnoseCommand.formatBytes(2L * 1024 * 1024 * 1024).contains("GiB"));
  }

  private static DiagnoseReport.Check findCheck(DiagnoseReport report, String id) {
    for (DiagnoseReport.Check c : report.getChecks()) {
      if (id.equals(c.getId())) {
        return c;
      }
    }
    return null;
  }

  private static String summarize(DiagnoseReport report) {
    StringBuilder sb = new StringBuilder();
    for (DiagnoseReport.Check c : report.getChecks()) {
      sb.append(c.getStatus()).append(' ').append(c.getId()).append(' ').append(c.getMessage())
          .append("; ");
    }
    return sb.toString();
  }
}
