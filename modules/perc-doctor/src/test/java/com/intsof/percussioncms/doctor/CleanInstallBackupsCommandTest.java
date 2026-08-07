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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CleanInstallBackupsCommandTest {

  @TempDir Path tempDir;

  private Path installRoot;
  private Path appServerZip;
  private Path bakFile;
  private Path backupFile;
  private Path propertiesBackup;
  private Path keepLog;
  private Path outsideBak;

  @BeforeEach
  void setUp() throws Exception {
    installRoot = Files.createDirectories(tempDir.resolve("cms-install"));
    Path rxconfig = Files.createDirectories(installRoot.resolve("rxconfig").resolve("Server"));

    appServerZip = installRoot.resolve("AppServer_backup_20240115_120000.zip");
    bakFile = installRoot.resolve("ResourceBundle.tmx.bak");
    backupFile = installRoot.resolve("misc.config.backup");
    propertiesBackup = rxconfig.resolve("Navigation.properties.backup");
    keepLog = installRoot.resolve("server.log");

    Files.write(appServerZip, "ZIP".getBytes(StandardCharsets.UTF_8));
    Files.writeString(bakFile, "bak");
    Files.writeString(backupFile, "backup");
    Files.writeString(propertiesBackup, "nav-backup");
    Files.writeString(keepLog, "active log");

    Path outside = Files.createDirectories(tempDir.resolve("not-install"));
    outsideBak = outside.resolve("escape.bak");
    Files.writeString(outsideBak, "outside");
  }

  @Test
  void dryRunInventoriesAllowlistedBackupsOnlyAndDoesNotDelete() throws Exception {
    CleanReport report = CleanInstallBackupsCommand.execute(installRoot, true);

    assertTrue(report.isDryRun());
    assertEquals(4, report.getCandidateCount());
    assertEquals(0, report.getDeletedCount());
    assertTrue(Files.exists(appServerZip));
    assertTrue(Files.exists(bakFile));
    assertTrue(Files.exists(backupFile));
    assertTrue(Files.exists(propertiesBackup));
    assertTrue(Files.exists(keepLog));
    assertTrue(Files.exists(outsideBak));

    for (CleanReport.Entry e : report.getEntries()) {
      assertEquals(CleanReport.EntryStatus.WOULD_DELETE, e.getStatus());
      assertTrue(InstallRootGuard.isUnderInstallRoot(installRoot, e.getPath()));
      assertTrue(
          InstallRootGuard.isInstallBackupFileName(e.getPath().getFileName().toString()));
    }

    long expectedBytes =
        Files.size(appServerZip)
            + Files.size(bakFile)
            + Files.size(backupFile)
            + Files.size(propertiesBackup);
    assertEquals(expectedBytes, report.getTotalBytes());
  }

  @Test
  void applyDeletesAllowlistedBackupsUnderRootOnlyLeavesOtherFiles() throws Exception {
    CleanReport report = CleanInstallBackupsCommand.execute(installRoot, false);

    assertFalse(report.isDryRun());
    assertEquals(4, report.getDeletedCount());
    assertEquals(0, report.getFailedCount());
    assertFalse(Files.exists(appServerZip));
    assertFalse(Files.exists(bakFile));
    assertFalse(Files.exists(backupFile));
    assertFalse(Files.exists(propertiesBackup));
    assertTrue(Files.exists(keepLog), "non-backup files must not be deleted");
    assertTrue(Files.exists(outsideBak), "files outside install root must not be deleted");
  }

  @Test
  void findInstallBackupsDoesNotIncludeOutsideInstallRoot() throws Exception {
    List<Path> found = CleanInstallBackupsCommand.findInstallBackups(installRoot);
    assertEquals(4, found.size());
    for (Path p : found) {
      assertTrue(InstallRootGuard.isUnderInstallRoot(installRoot, p));
      assertFalse(p.equals(outsideBak));
    }
  }

  @Test
  void executeRejectsMissingInstallRoot() {
    Path missing = tempDir.resolve("missing-root");
    assertThrows(
        IllegalArgumentException.class,
        () -> CleanInstallBackupsCommand.execute(missing, true));
  }

  @Test
  void allowlistRejectsNonBackupNamesAndHeapDumps() throws Exception {
    Path hprof = installRoot.resolve("java_pid1.hprof");
    Path txt = installRoot.resolve("notes.txt");
    Path emptyZip = installRoot.resolve("AppServer_backup_.zip");
    Path wrongPrefix = installRoot.resolve("OtherServer_backup_1.zip");
    Files.writeString(hprof, "heap");
    Files.writeString(txt, "txt");
    Files.writeString(emptyZip, "bad");
    Files.writeString(wrongPrefix, "bad");

    CleanReport dry = CleanInstallBackupsCommand.execute(installRoot, true);
    // still only the 4 allowlisted fixtures from setUp
    assertEquals(4, dry.getCandidateCount());

    CleanInstallBackupsCommand.execute(installRoot, false);
    assertTrue(Files.exists(hprof));
    assertTrue(Files.exists(txt));
    assertTrue(Files.exists(emptyZip));
    assertTrue(Files.exists(wrongPrefix));
  }

  @Test
  void visitFileFailedRecordsFailedEntryOnReport() {
    CleanReport report =
        new CleanReport(CleanInstallBackupsCommand.COMMAND_NAME, installRoot, true);
    Path failed = installRoot.resolve("unreadable.bak");
    CleanInstallBackupsCommand.recordVisitFailure(
        report, failed, new java.io.IOException("Access denied"));

    assertEquals(1, report.getFailedCount());
    CleanReport.Entry e = report.getEntries().get(0);
    assertEquals(CleanReport.EntryStatus.FAILED, e.getStatus());
    assertEquals(failed, e.getPath());
    assertTrue(e.getDetail().contains("walk:"));
    assertTrue(e.getDetail().toLowerCase().contains("access denied"));
  }

  @Test
  void recordVisitFailureNoopsWhenReportNull() {
    CleanInstallBackupsCommand.recordVisitFailure(
        null, installRoot.resolve("x.bak"), new java.io.IOException("x"));
  }
}
