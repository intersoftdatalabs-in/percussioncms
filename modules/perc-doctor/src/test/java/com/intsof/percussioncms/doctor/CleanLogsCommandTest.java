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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CleanLogsCommandTest {

  @TempDir Path tempDir;

  private Path installRoot;
  private Path jettyLogs;
  private Path dtsLogs;
  private Path serverLog;
  private Path rolledServerLog;
  private Path oldRolledLog;
  private Path catalinaGz;
  private Path outsideLog;
  private Path nonLogInLogDir;

  @BeforeEach
  void setUp() throws Exception {
    installRoot = Files.createDirectories(tempDir.resolve("cms-install"));
    jettyLogs =
        Files.createDirectories(
            installRoot.resolve("jetty").resolve("base").resolve("logs"));
    dtsLogs =
        Files.createDirectories(
            installRoot.resolve("Deployment").resolve("Server").resolve("logs"));

    serverLog = jettyLogs.resolve("server.log");
    rolledServerLog = jettyLogs.resolve("server-2024-01-15-1.log");
    oldRolledLog = jettyLogs.resolve("server-2023-06-01-1.log");
    catalinaGz = dtsLogs.resolve("catalina.2024-01-10.log.gz");
    nonLogInLogDir = jettyLogs.resolve("readme.txt");

    Files.writeString(serverLog, "active-server");
    Files.writeString(rolledServerLog, "rolled-recent");
    Files.writeString(oldRolledLog, "rolled-old");
    Files.write(catalinaGz, new byte[] {(byte) 0x1f, (byte) 0x8b});
    Files.writeString(nonLogInLogDir, "not-a-log");

    // Make old rolled file aged beyond 7d; leave rolledServerLog "recent"
    Instant now = Instant.now();
    Files.setLastModifiedTime(oldRolledLog, FileTime.from(now.minus(Duration.ofDays(30))));
    Files.setLastModifiedTime(rolledServerLog, FileTime.from(now.minus(Duration.ofHours(2))));
    Files.setLastModifiedTime(catalinaGz, FileTime.from(now.minus(Duration.ofDays(20))));
    Files.setLastModifiedTime(serverLog, FileTime.from(now.minus(Duration.ofDays(40))));

    Path outside = Files.createDirectories(tempDir.resolve("not-install").resolve("logs"));
    outsideLog = outside.resolve("escape.log");
    Files.writeString(outsideLog, "outside");
    Files.setLastModifiedTime(outsideLog, FileTime.from(now.minus(Duration.ofDays(40))));
  }

  @Test
  void dryRunNeverDeletesWithAgeAndKeepCurrent() throws Exception {
    CleanLogsCommand.Options opts =
        new CleanLogsCommand.Options(Duration.ofDays(7), true);
    CleanReport report = CleanLogsCommand.execute(installRoot, true, opts);

    assertTrue(report.isDryRun());
    assertEquals(0, report.getDeletedCount());
    assertTrue(Files.exists(serverLog));
    assertTrue(Files.exists(rolledServerLog));
    assertTrue(Files.exists(oldRolledLog));
    assertTrue(Files.exists(catalinaGz));
    assertTrue(Files.exists(outsideLog));
    assertTrue(Files.exists(nonLogInLogDir));

    // keep-current skips server.log; age skips recent rolled; old rolled + catalina gz would delete
    int wouldDelete = 0;
    int skipped = 0;
    for (CleanReport.Entry e : report.getEntries()) {
      assertTrue(InstallRootGuard.isUnderInstallRoot(installRoot, e.getPath()));
      if (e.getStatus() == CleanReport.EntryStatus.WOULD_DELETE) {
        wouldDelete++;
      } else if (e.getStatus() == CleanReport.EntryStatus.SKIPPED) {
        skipped++;
      }
    }
    assertEquals(2, wouldDelete, "old rolled + catalina.gz");
    assertTrue(skipped >= 2, "keep-current + age skip at least current + recent rolled");
  }

  @Test
  void applyDeletesOnlyAgedNonCurrentUnderRoot() throws Exception {
    CleanLogsCommand.Options opts =
        new CleanLogsCommand.Options(Duration.ofDays(7), true);
    CleanReport report = CleanLogsCommand.execute(installRoot, false, opts);

    assertFalse(report.isDryRun());
    assertEquals(2, report.getDeletedCount());
    assertEquals(0, report.getFailedCount());
    assertTrue(Files.exists(serverLog), "keep-current must retain active server.log");
    assertTrue(Files.exists(rolledServerLog), "recent rolled must be retained by age filter");
    assertFalse(Files.exists(oldRolledLog), "old rolled log should be deleted");
    assertFalse(Files.exists(catalinaGz), "old catalina.gz should be deleted");
    assertTrue(Files.exists(outsideLog), "outside install root must not be deleted");
    assertTrue(Files.exists(nonLogInLogDir), "non-log files in log dir must not be deleted");
  }

  @Test
  void keepCurrentFalseAllowsDeletingActiveLogWhenAged() throws Exception {
    CleanLogsCommand.Options opts =
        new CleanLogsCommand.Options(Duration.ofDays(7), false);
    CleanReport report = CleanLogsCommand.execute(installRoot, false, opts);

    assertFalse(Files.exists(serverLog), "active log deletable when keep-current off and aged");
    assertFalse(Files.exists(oldRolledLog));
    assertFalse(Files.exists(catalinaGz));
    assertTrue(Files.exists(rolledServerLog), "recent rolled still protected by age");
    assertTrue(report.getDeletedCount() >= 3);
  }

  @Test
  void noAgeFilterWithKeepCurrentDeletesRolledOnly() throws Exception {
    CleanLogsCommand.Options opts = CleanLogsCommand.Options.defaults();
    CleanReport report = CleanLogsCommand.execute(installRoot, false, opts);

    assertTrue(Files.exists(serverLog), "current retained");
    assertFalse(Files.exists(rolledServerLog));
    assertFalse(Files.exists(oldRolledLog));
    assertFalse(Files.exists(catalinaGz));
    assertEquals(3, report.getDeletedCount());
  }

  @Test
  void findLogFilesOnlyUnderKnownDirsAndRoot() throws Exception {
    List<Path> found = CleanLogsCommand.findLogFiles(installRoot);
    assertEquals(4, found.size()); // server + 2 rolled + catalina.gz; not readme.txt
    for (Path p : found) {
      assertTrue(InstallRootGuard.isUnderInstallRoot(installRoot, p));
      assertFalse(p.equals(outsideLog));
      assertTrue(InstallRootGuard.isLogFileName(p.getFileName().toString()));
    }
  }

  @Test
  void executeRejectsMissingInstallRoot() {
    Path missing = tempDir.resolve("missing-root");
    assertThrows(
        IllegalArgumentException.class,
        () ->
            CleanLogsCommand.execute(
                missing, true, new CleanLogsCommand.Options(Duration.ofDays(1), true)));
  }

  @Test
  void missingLogDirsIsEmptyReportNotError() throws Exception {
    Path emptyRoot = Files.createDirectories(tempDir.resolve("empty-install"));
    CleanReport report =
        CleanLogsCommand.execute(emptyRoot, true, CleanLogsCommand.Options.defaults());
    assertEquals(0, report.getCandidateCount());
    assertEquals(0, report.getFailedCount());
  }

  @Test
  void parseOlderThanAcceptsUnits() {
    assertEquals(Duration.ofDays(7), CleanLogsCommand.parseOlderThan("7d"));
    assertEquals(Duration.ofHours(24), CleanLogsCommand.parseOlderThan("24h"));
    assertEquals(Duration.ofMinutes(30), CleanLogsCommand.parseOlderThan("30m"));
    assertEquals(Duration.ofSeconds(90), CleanLogsCommand.parseOlderThan("90s"));
    assertEquals(Duration.ofDays(14), CleanLogsCommand.parseOlderThan("2w"));
    assertEquals(Duration.ofDays(7), CleanLogsCommand.parseOlderThan("7D"));
  }

  @Test
  void parseOlderThanRejectsInvalid() {
    assertThrows(IllegalArgumentException.class, () -> CleanLogsCommand.parseOlderThan(""));
    assertThrows(IllegalArgumentException.class, () -> CleanLogsCommand.parseOlderThan("7"));
    assertThrows(IllegalArgumentException.class, () -> CleanLogsCommand.parseOlderThan("d7"));
    assertThrows(IllegalArgumentException.class, () -> CleanLogsCommand.parseOlderThan("0d"));
    assertThrows(IllegalArgumentException.class, () -> CleanLogsCommand.parseOlderThan("-1d"));
    assertThrows(IllegalArgumentException.class, () -> CleanLogsCommand.parseOlderThan("1x"));
  }

  @Test
  void logFileNameAndCurrentHelpers() {
    assertTrue(InstallRootGuard.isLogFileName("server.log"));
    assertTrue(InstallRootGuard.isLogFileName("catalina.2024-01-01.log.gz"));
    assertTrue(InstallRootGuard.isLogFileName("catalina.out"));
    assertFalse(InstallRootGuard.isLogFileName("readme.txt"));
    assertFalse(InstallRootGuard.isLogFileName("a/b.log"));

    assertTrue(InstallRootGuard.isCurrentLogFileName("server.log"));
    assertTrue(InstallRootGuard.isCurrentLogFileName("catalina.out"));
    assertFalse(InstallRootGuard.isCurrentLogFileName("server-2024-01-15-1.log"));
    assertFalse(InstallRootGuard.isCurrentLogFileName("catalina.2024-01-15.log.gz"));
    assertFalse(InstallRootGuard.isCurrentLogFileName("server.log.1"));
  }

  @Test
  void visitFileFailedRecordsFailedEntryOnReport() {
    CleanReport report = new CleanReport(CleanLogsCommand.COMMAND_NAME, installRoot, true);
    Path failed = jettyLogs.resolve("unreadable.log");
    CleanLogsCommand.recordVisitFailure(report, failed, new java.io.IOException("Access denied"));

    assertEquals(1, report.getFailedCount());
    CleanReport.Entry e = report.getEntries().get(0);
    assertEquals(CleanReport.EntryStatus.FAILED, e.getStatus());
    assertTrue(e.getDetail().contains("walk:"));
  }

  @Test
  void optionsRejectNonPositiveOlderThan() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new CleanLogsCommand.Options(Duration.ZERO, true));
    assertThrows(
        IllegalArgumentException.class,
        () -> new CleanLogsCommand.Options(Duration.ofSeconds(-1), true));
  }
}
