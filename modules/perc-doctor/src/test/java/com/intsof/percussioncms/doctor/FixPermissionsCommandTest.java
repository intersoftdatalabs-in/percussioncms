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
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class FixPermissionsCommandTest {

  @TempDir Path tempDir;

  private Path installRoot;
  private Path binDir;
  private Path unixLauncher;
  private Path jettyLogs;

  @BeforeEach
  void setUp() throws Exception {
    installRoot = Files.createDirectories(tempDir.resolve("cms-install"));
    binDir = Files.createDirectories(installRoot.resolve("bin"));
    unixLauncher = binDir.resolve("perc-doctor");
    Files.writeString(unixLauncher, "#!/bin/sh\necho doctor\n", StandardCharsets.UTF_8);
    Files.writeString(binDir.resolve("perc-doctor.bat"), "@echo off\r\n", StandardCharsets.UTF_8);
    Files.write(binDir.resolve("perc-doctor.jar"), new byte[] {0x50, 0x4b});
    jettyLogs =
        Files.createDirectories(
            installRoot.resolve("jetty").resolve("base").resolve("logs"));
    Path serverDir = Files.createDirectories(installRoot.resolve("rxconfig").resolve("Server"));
    Files.writeString(serverDir.resolve("server.properties"), "bindPort=9992\n");
    Path installerDir =
        Files.createDirectories(installRoot.resolve("rxconfig").resolve("Installer"));
    Files.writeString(
        installerDir.resolve("rxrepository.properties"),
        "DB_BACKEND=H2\nDB_DRIVER_NAME=h2\nDB_DRIVER_CLASS_NAME=org.h2.Driver\n"
            + "DB_SERVER=file:x\nUID=sa\n");
  }

  @Test
  void dryRunNeverMutatesAndReportsCandidates() throws Exception {
    FixPermissionsReport report = FixPermissionsCommand.execute(installRoot, true);
    assertEquals(FixPermissionsCommand.COMMAND_NAME, report.getCommand());
    assertTrue(report.isDryRun());
    assertTrue(report.getCandidateCount() > 0);
    assertEquals(0, report.getFixedCount());
    // Launcher file content unchanged
    assertTrue(Files.readString(unixLauncher, StandardCharsets.UTF_8).contains("#!/bin/sh"));
  }

  @Test
  void missingPathsAreSkippedNotFailed() throws Exception {
    Path emptyRoot = Files.createDirectories(tempDir.resolve("empty-install"));
    FixPermissionsReport report = FixPermissionsCommand.execute(emptyRoot, true);
    assertEquals(0, report.getFailedCount());
    assertTrue(
        report.getEntries().stream()
            .anyMatch(e -> e.getStatus() == FixPermissionsReport.EntryStatus.SKIPPED));
  }

  @Test
  void missingInstallRootRejected() {
    Path missing = tempDir.resolve("gone");
    assertThrows(
        IllegalArgumentException.class, () -> FixPermissionsCommand.execute(missing, true));
  }

  @Test
  void allEntryPathsStayUnderInstallRoot() throws Exception {
    FixPermissionsReport report = FixPermissionsCommand.execute(installRoot, true);
    for (FixPermissionsReport.Entry e : report.getEntries()) {
      assertTrue(
          InstallRootGuard.isUnderInstallRoot(installRoot, e.getPath()),
          "path escaped root: " + e.getPath());
    }
  }

  @Test
  void isUnixScriptLauncherHeuristic() {
    assertTrue(FixPermissionsCommand.isUnixScriptLauncher("perc-doctor"));
    assertFalse(FixPermissionsCommand.isUnixScriptLauncher("perc-doctor.bat"));
    assertFalse(FixPermissionsCommand.isUnixScriptLauncher("perc-doctor.jar"));
    assertFalse(FixPermissionsCommand.isUnixScriptLauncher("perc-doctor.cmd"));
  }

  @Test
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void posixDryRunWouldFixMissingOwnerExecute() throws Exception {
    // Strip owner execute if present
    Set<PosixFilePermission> perms = Files.getPosixFilePermissions(unixLauncher);
    EnumSet<PosixFilePermission> stripped = EnumSet.copyOf(perms);
    stripped.remove(PosixFilePermission.OWNER_EXECUTE);
    stripped.remove(PosixFilePermission.GROUP_EXECUTE);
    stripped.remove(PosixFilePermission.OTHERS_EXECUTE);
    Files.setPosixFilePermissions(unixLauncher, stripped);

    FixPermissionsReport dry = FixPermissionsCommand.execute(installRoot, true);
    assertTrue(dry.getWouldFixCount() >= 1);
    assertFalse(Files.getPosixFilePermissions(unixLauncher).contains(PosixFilePermission.OWNER_EXECUTE));

    FixPermissionsReport apply = FixPermissionsCommand.execute(installRoot, false);
    assertTrue(apply.getFixedCount() >= 1);
    assertTrue(
        Files.getPosixFilePermissions(unixLauncher).contains(PosixFilePermission.OWNER_EXECUTE));
  }

  @Test
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void posixLogDirOwnerRwxFix() throws Exception {
    Set<PosixFilePermission> perms = Files.getPosixFilePermissions(jettyLogs);
    EnumSet<PosixFilePermission> stripped = EnumSet.copyOf(perms);
    stripped.remove(PosixFilePermission.OWNER_WRITE);
    Files.setPosixFilePermissions(jettyLogs, stripped);

    FixPermissionsReport dry = FixPermissionsCommand.execute(installRoot, true);
    assertTrue(
        dry.getEntries().stream()
            .anyMatch(
                e ->
                    e.getStatus() == FixPermissionsReport.EntryStatus.WOULD_FIX
                        && e.getPath().equals(jettyLogs.toAbsolutePath().normalize())));

    FixPermissionsReport apply = FixPermissionsCommand.execute(installRoot, false);
    assertTrue(
        Files.getPosixFilePermissions(jettyLogs).contains(PosixFilePermission.OWNER_WRITE));
    assertTrue(apply.getFixedCount() >= 1);
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  void windowsReportsAccessWithoutModeMutation() throws Exception {
    FixPermissionsReport report = FixPermissionsCommand.execute(installRoot, false);
    assertEquals(0, report.getFixedCount());
    assertTrue(
        report.getEntries().stream()
            .anyMatch(
                e ->
                    e.getStatus() == FixPermissionsReport.EntryStatus.SKIPPED
                        && e.getDetail() != null
                        && e.getDetail().toLowerCase().contains("posix")));
    // Launchers still OK/readable
    assertTrue(
        report.getEntries().stream()
            .anyMatch(
                e ->
                    e.getPath().getFileName().toString().equals("perc-doctor.bat")
                        && (e.getStatus() == FixPermissionsReport.EntryStatus.OK
                            || e.getStatus() == FixPermissionsReport.EntryStatus.SKIPPED)));
  }
}
