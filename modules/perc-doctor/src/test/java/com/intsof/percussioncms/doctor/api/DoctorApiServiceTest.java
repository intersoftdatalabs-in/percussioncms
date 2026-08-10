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
package com.intsof.percussioncms.doctor.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intsof.percussioncms.doctor.CleanHeapDumpsCommand;
import com.intsof.percussioncms.doctor.CleanReport;
import com.intsof.percussioncms.doctor.CleanTempCommand;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Behavioral tests for doctor HTTP command runner (dry-run default + inventory). */
class DoctorApiServiceTest {

  @TempDir Path tempDir;

  private Path installRoot;
  private Path heapDump;
  private DoctorApiService service;

  @BeforeEach
  void setUp() throws Exception {
    installRoot = Files.createDirectories(tempDir.resolve("cms-install"));
    heapDump = installRoot.resolve("java_pid9.hprof");
    Files.write(heapDump, "HEAP".getBytes(StandardCharsets.UTF_8));
    service = new DoctorApiService(() -> installRoot);
  }

  @Test
  void dryRunDefaultWhenBodyNullInventoriesAndDoesNotDelete() throws Exception {
    DoctorReportView view = service.execute(CleanHeapDumpsCommand.COMMAND_NAME, null);

    assertTrue(view.isDryRun());
    assertEquals(1, view.getCandidateCount());
    assertEquals(0, view.getDeletedCount());
    assertEquals(CleanReport.EntryStatus.WOULD_DELETE.name(), view.getEntries().get(0).getStatus());
    assertTrue(Files.exists(heapDump), "dry-run must not delete");
  }

  @Test
  void dryRunDefaultWhenDryRunFieldOmitted() throws Exception {
    DoctorRequest req = new DoctorRequest();
    // dryRun left null → effective dry-run
    DoctorReportView view = service.execute(CleanHeapDumpsCommand.COMMAND_NAME, req);

    assertTrue(view.isDryRun());
    assertTrue(Files.exists(heapDump));
    assertEquals(0, view.getDeletedCount());
  }

  @Test
  void explicitDryRunTrueDoesNotDelete() throws Exception {
    DoctorRequest req = new DoctorRequest();
    req.setDryRun(true);
    DoctorReportView view = service.execute(CleanHeapDumpsCommand.COMMAND_NAME, req);

    assertTrue(view.isDryRun());
    assertTrue(Files.exists(heapDump));
    assertEquals(Files.size(heapDump), view.getTotalBytes());
  }

  @Test
  void dryRunFalseAppliesDeletes() throws Exception {
    DoctorRequest req = new DoctorRequest();
    req.setDryRun(false);
    DoctorReportView view = service.execute(CleanHeapDumpsCommand.COMMAND_NAME, req);

    assertFalse(view.isDryRun());
    assertEquals(1, view.getDeletedCount());
    assertFalse(Files.exists(heapDump));
  }

  @Test
  void explicitInstallRootMatchingHostIsAcceptedAndUsesHostPath() throws Exception {
    DoctorRequest req = new DoctorRequest();
    req.setDryRun(true);
    // Same tree as provider — allowed, but I/O still uses host Path.
    req.setInstallRoot(installRoot.toAbsolutePath().normalize().toString());

    DoctorReportView view = service.execute(CleanHeapDumpsCommand.COMMAND_NAME, req);
    assertEquals(1, view.getCandidateCount());
    assertTrue(Files.exists(heapDump), "dry-run");
    assertEquals(
        installRoot.toAbsolutePath().normalize().toString(),
        Path.of(view.getInstallRoot()).toAbsolutePath().normalize().toString());
  }

  @Test
  void explicitInstallRootOutsideHostIsRejected() throws Exception {
    Path otherRoot = Files.createDirectories(tempDir.resolve("other-install"));
    Path otherDump = otherRoot.resolve("x.hprof");
    Files.write(otherDump, "X".getBytes(StandardCharsets.UTF_8));

    DoctorRequest req = new DoctorRequest();
    req.setDryRun(true);
    req.setInstallRoot(otherRoot.toString());

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> service.execute(CleanHeapDumpsCommand.COMMAND_NAME, req));
    assertTrue(ex.getMessage().toLowerCase().contains("installroot"));
    assertTrue(Files.exists(heapDump), "rejected override must not touch host root");
    assertTrue(Files.exists(otherDump), "rejected override must not walk outside root");
  }

  @Test
  void unknownCommandThrows() {
    assertThrows(
        DoctorUnknownCommandException.class,
        () -> service.execute("not-a-real-command", new DoctorRequest()));
  }

  @Test
  void cleanTempDryRunDefaultDoesNotDelete() throws Exception {
    Path cmsTemp = Files.createDirectories(installRoot.resolve("temp"));
    Path junk = cmsTemp.resolve("api-scratch.tmp");
    Files.writeString(junk, "tmp");

    DoctorReportView view = service.execute(CleanTempCommand.COMMAND_NAME, null);

    assertTrue(view.isDryRun());
    assertEquals(1, view.getCandidateCount());
    assertEquals(0, view.getDeletedCount());
    assertEquals(CleanReport.EntryStatus.WOULD_DELETE.name(), view.getEntries().get(0).getStatus());
    assertTrue(Files.exists(junk), "dry-run must not delete temp files");
    assertTrue(Files.isDirectory(cmsTemp));
  }

  @Test
  void cleanTempApplyDeletesUnderAllowlistedDirs() throws Exception {
    Path cmsTemp = Files.createDirectories(installRoot.resolve("temp"));
    Path junk = cmsTemp.resolve("api-apply.tmp");
    Files.writeString(junk, "tmp");

    DoctorRequest req = new DoctorRequest();
    req.setDryRun(false);
    DoctorReportView view = service.execute(CleanTempCommand.COMMAND_NAME, req);

    assertFalse(view.isDryRun());
    assertEquals(1, view.getDeletedCount());
    assertFalse(Files.exists(junk));
    assertTrue(Files.isDirectory(cmsTemp));
  }
}
