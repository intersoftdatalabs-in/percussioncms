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

class CleanHeapDumpsCommandTest {

  @TempDir Path tempDir;

  private Path installRoot;
  private Path nestedDump;
  private Path rootDump;
  private Path logFile;
  private Path outsideDump;

  @BeforeEach
  void setUp() throws Exception {
    installRoot = Files.createDirectories(tempDir.resolve("cms-install"));
    Path jetty = Files.createDirectories(installRoot.resolve("jetty").resolve("base"));
    nestedDump = jetty.resolve("java_pid4242.hprof");
    rootDump = installRoot.resolve("crash.hprof");
    logFile = installRoot.resolve("server.log");
    Files.write(nestedDump, "HEAPNEST".getBytes(StandardCharsets.UTF_8));
    Files.write(rootDump, "HEAPROOT".getBytes(StandardCharsets.UTF_8));
    Files.writeString(logFile, "not a dump");

    Path outside = Files.createDirectories(tempDir.resolve("not-install"));
    outsideDump = outside.resolve("outside.hprof");
    Files.write(outsideDump, "OUTSIDE".getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void dryRunInventoriesOnlyAllowlistedHprofUnderRootAndDoesNotDelete() throws Exception {
    CleanReport report = CleanHeapDumpsCommand.execute(installRoot, true);

    assertTrue(report.isDryRun());
    assertEquals(2, report.getCandidateCount());
    assertEquals(0, report.getDeletedCount());
    assertTrue(Files.exists(nestedDump));
    assertTrue(Files.exists(rootDump));
    assertTrue(Files.exists(logFile));
    assertTrue(Files.exists(outsideDump));

    for (CleanReport.Entry e : report.getEntries()) {
      assertEquals(CleanReport.EntryStatus.WOULD_DELETE, e.getStatus());
      assertTrue(InstallRootGuard.isUnderInstallRoot(installRoot, e.getPath()));
      assertTrue(InstallRootGuard.isHeapDumpFileName(e.getPath().getFileName().toString()));
    }

    long expectedBytes =
        Files.size(nestedDump) + Files.size(rootDump);
    assertEquals(expectedBytes, report.getTotalBytes());
  }

  @Test
  void applyDeletesHprofUnderRootOnlyLeavesOtherFiles() throws Exception {
    CleanReport report = CleanHeapDumpsCommand.execute(installRoot, false);

    assertFalse(report.isDryRun());
    assertEquals(2, report.getDeletedCount());
    assertEquals(0, report.getFailedCount());
    assertFalse(Files.exists(nestedDump));
    assertFalse(Files.exists(rootDump));
    assertTrue(Files.exists(logFile), "non-hprof must not be deleted");
    assertTrue(Files.exists(outsideDump), "files outside install root must not be deleted");
  }

  @Test
  void findHeapDumpsDoesNotIncludeOutsideInstallRoot() throws Exception {
    List<Path> found = CleanHeapDumpsCommand.findHeapDumps(installRoot);
    assertEquals(2, found.size());
    for (Path p : found) {
      assertTrue(InstallRootGuard.isUnderInstallRoot(installRoot, p));
      assertFalse(p.equals(outsideDump));
    }
  }

  @Test
  void executeRejectsMissingInstallRoot() {
    Path missing = tempDir.resolve("missing-root");
    assertThrows(IllegalArgumentException.class, () -> CleanHeapDumpsCommand.execute(missing, true));
  }

  @Test
  void allowlistSkipsNonHprofExtensions() throws Exception {
    Path bak = installRoot.resolve("java_pid1.hprof.bak");
    Path txt = installRoot.resolve("notes.txt");
    Files.writeString(bak, "bak");
    Files.writeString(txt, "txt");

    CleanReport dry = CleanHeapDumpsCommand.execute(installRoot, true);
    assertEquals(2, dry.getCandidateCount());

    CleanHeapDumpsCommand.execute(installRoot, false);
    assertTrue(Files.exists(bak));
    assertTrue(Files.exists(txt));
  }

  @Test
  void visitFileFailedRecordsFailedEntryOnReport() {
    CleanReport report = new CleanReport(CleanHeapDumpsCommand.COMMAND_NAME, installRoot, true);
    Path failed = installRoot.resolve("unreadable.hprof");
    CleanHeapDumpsCommand.recordVisitFailure(
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
    // Should not throw when inventory-only walk has no report
    CleanHeapDumpsCommand.recordVisitFailure(
        null, installRoot.resolve("x.hprof"), new java.io.IOException("x"));
  }
}
