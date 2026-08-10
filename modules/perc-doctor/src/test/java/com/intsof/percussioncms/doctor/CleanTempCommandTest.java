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

class CleanTempCommandTest {

  @TempDir Path tempDir;

  private Path installRoot;
  private Path cmsTemp;
  private Path jettyWork;
  private Path dtsTemp;
  private Path nestedTempFile;
  private Path jettyWorkFile;
  private Path dtsTempFile;
  private Path outsideTempFile;
  private Path nonTempFile;
  private Path logsDirFile;

  @BeforeEach
  void setUp() throws Exception {
    installRoot = Files.createDirectories(tempDir.resolve("cms-install"));
    cmsTemp = Files.createDirectories(installRoot.resolve("temp"));
    jettyWork =
        Files.createDirectories(installRoot.resolve("jetty").resolve("base").resolve("work"));
    dtsTemp =
        Files.createDirectories(
            installRoot.resolve("Deployment").resolve("Server").resolve("temp"));

    Path nested = Files.createDirectories(cmsTemp.resolve("session").resolve("abc"));
    nestedTempFile = nested.resolve("upload.tmp");
    jettyWorkFile = jettyWork.resolve("jstl_1");
    dtsTempFile = dtsTemp.resolve("safeToDelete.tmp");
    nonTempFile = installRoot.resolve("rxconfig").resolve("Server").resolve("server.properties");
    Files.createDirectories(nonTempFile.getParent());
    Files.writeString(nestedTempFile, "nested-temp");
    Files.write(jettyWorkFile, "work-bytes".getBytes(StandardCharsets.UTF_8));
    Files.writeString(dtsTempFile, "dts-temp");
    Files.writeString(nonTempFile, "keep-me");

    Path logs =
        Files.createDirectories(installRoot.resolve("jetty").resolve("base").resolve("logs"));
    logsDirFile = logs.resolve("server.log");
    Files.writeString(logsDirFile, "not-temp");

    Path outside = Files.createDirectories(tempDir.resolve("not-install").resolve("temp"));
    outsideTempFile = outside.resolve("escape.tmp");
    Files.writeString(outsideTempFile, "outside");
  }

  @Test
  void dryRunInventoriesOnlyAllowlistedTempDirsAndDoesNotDelete() throws Exception {
    CleanReport report = CleanTempCommand.execute(installRoot, true);

    assertTrue(report.isDryRun());
    assertEquals(0, report.getDeletedCount());
    assertEquals(3, report.getCandidateCount());
    assertTrue(Files.exists(nestedTempFile));
    assertTrue(Files.exists(jettyWorkFile));
    assertTrue(Files.exists(dtsTempFile));
    assertTrue(Files.exists(outsideTempFile));
    assertTrue(Files.exists(nonTempFile));
    assertTrue(Files.exists(logsDirFile));
    assertTrue(Files.isDirectory(cmsTemp), "allowlisted root dirs must remain");
    assertTrue(Files.isDirectory(jettyWork));
    assertTrue(Files.isDirectory(dtsTemp));

    for (CleanReport.Entry e : report.getEntries()) {
      assertEquals(CleanReport.EntryStatus.WOULD_DELETE, e.getStatus());
      assertTrue(InstallRootGuard.isUnderInstallRoot(installRoot, e.getPath()));
      assertTrue(
          CleanTempCommand.isUnderAnyTempRoot(
              InstallRootGuard.existingTempDirs(installRoot), e.getPath()));
    }

    long expectedBytes =
        Files.size(nestedTempFile) + Files.size(jettyWorkFile) + Files.size(dtsTempFile);
    assertEquals(expectedBytes, report.getTotalBytes());
  }

  @Test
  void applyDeletesFilesUnderTempRootsOnlyLeavesRootsAndOtherTrees() throws Exception {
    CleanReport report = CleanTempCommand.execute(installRoot, false);

    assertFalse(report.isDryRun());
    assertEquals(3, report.getDeletedCount());
    assertEquals(0, report.getFailedCount());
    assertFalse(Files.exists(nestedTempFile));
    assertFalse(Files.exists(jettyWorkFile));
    assertFalse(Files.exists(dtsTempFile));
    assertTrue(Files.exists(outsideTempFile), "outside install root must not be deleted");
    assertTrue(Files.exists(nonTempFile), "files outside allowlisted temp dirs must remain");
    assertTrue(Files.exists(logsDirFile), "log tree is not a clean-temp target");
    assertTrue(Files.isDirectory(cmsTemp), "allowlisted temp root must be retained");
    assertTrue(Files.isDirectory(jettyWork), "allowlisted work root must be retained");
    assertTrue(Files.isDirectory(dtsTemp), "allowlisted DTS temp root must be retained");
    // Nested empty dirs under temp may be removed best-effort
    assertFalse(Files.exists(nestedTempFile.getParent().resolve("upload.tmp")));
  }

  @Test
  void findTempFilesDoesNotIncludeOutsideInstallRootOrNonTempTrees() throws Exception {
    List<Path> found = CleanTempCommand.findTempFiles(installRoot);
    assertEquals(3, found.size());
    for (Path p : found) {
      assertTrue(InstallRootGuard.isUnderInstallRoot(installRoot, p));
      assertFalse(p.equals(outsideTempFile));
      assertFalse(p.equals(nonTempFile));
      assertFalse(p.equals(logsDirFile));
    }
  }

  @Test
  void executeRejectsMissingInstallRoot() {
    Path missing = tempDir.resolve("missing-root");
    assertThrows(IllegalArgumentException.class, () -> CleanTempCommand.execute(missing, true));
  }

  @Test
  void missingTempDirsAreSkippedNotError() throws Exception {
    Path emptyRoot = Files.createDirectories(tempDir.resolve("empty-install"));
    CleanReport report = CleanTempCommand.execute(emptyRoot, true);
    assertEquals(0, report.getCandidateCount());
    assertEquals(0, report.getFailedCount());
    assertTrue(report.isDryRun());
  }

  @Test
  void onlyExistingTempDirsAreWalked() throws Exception {
    // Remove jetty work; keep cms temp + dts temp
    Files.deleteIfExists(jettyWorkFile);
    Files.delete(jettyWork);

    CleanReport report = CleanTempCommand.execute(installRoot, true);
    assertEquals(2, report.getCandidateCount());
    for (CleanReport.Entry e : report.getEntries()) {
      assertFalse(e.getPath().startsWith(jettyWork));
    }
  }

  @Test
  void visitFileFailedRecordsFailedEntryOnReport() {
    CleanReport report = new CleanReport(CleanTempCommand.COMMAND_NAME, installRoot, true);
    Path failed = cmsTemp.resolve("locked.tmp");
    CleanTempCommand.recordVisitFailure(report, failed, new java.io.IOException("Access denied"));

    assertEquals(1, report.getFailedCount());
    CleanReport.Entry e = report.getEntries().get(0);
    assertEquals(CleanReport.EntryStatus.FAILED, e.getStatus());
    assertEquals(failed, e.getPath());
    assertTrue(e.getDetail().contains("walk:"));
    assertTrue(e.getDetail().toLowerCase().contains("access denied"));
  }

  @Test
  void recordVisitFailureNoopsWhenReportNull() {
    CleanTempCommand.recordVisitFailure(
        null, installRoot.resolve("temp").resolve("x.tmp"), new java.io.IOException("x"));
  }

  @Test
  void isUnderAnyTempRootRejectsNullsAndOutside() throws Exception {
    List<Path> roots = InstallRootGuard.existingTempDirs(installRoot);
    assertTrue(CleanTempCommand.isUnderAnyTempRoot(roots, nestedTempFile));
    assertFalse(CleanTempCommand.isUnderAnyTempRoot(roots, outsideTempFile));
    assertFalse(CleanTempCommand.isUnderAnyTempRoot(roots, nonTempFile));
    assertFalse(CleanTempCommand.isUnderAnyTempRoot(null, nestedTempFile));
    assertFalse(CleanTempCommand.isUnderAnyTempRoot(roots, null));
  }
}
