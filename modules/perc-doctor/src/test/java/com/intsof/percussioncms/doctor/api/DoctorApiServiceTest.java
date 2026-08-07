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
    DoctorReportView view =
        service.execute(CleanHeapDumpsCommand.COMMAND_NAME, null);

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
  void explicitInstallRootOverridesProvider() throws Exception {
    Path otherRoot = Files.createDirectories(tempDir.resolve("other-install"));
    Path otherDump = otherRoot.resolve("x.hprof");
    Files.write(otherDump, "X".getBytes(StandardCharsets.UTF_8));

    DoctorRequest req = new DoctorRequest();
    req.setDryRun(true);
    req.setInstallRoot(otherRoot.toString());

    DoctorReportView view = service.execute(CleanHeapDumpsCommand.COMMAND_NAME, req);
    assertEquals(1, view.getCandidateCount());
    assertTrue(view.getInstallRoot().contains("other-install") || view.getInstallRoot().equals(otherRoot.toString()));
    assertTrue(Files.exists(heapDump), "default install root dump untouched");
    assertTrue(Files.exists(otherDump), "dry-run");
  }

  @Test
  void unknownCommandThrows() {
    assertThrows(
        DoctorUnknownCommandException.class,
        () -> service.execute("not-a-real-command", new DoctorRequest()));
  }
}
