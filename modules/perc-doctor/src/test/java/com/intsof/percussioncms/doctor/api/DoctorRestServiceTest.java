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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intsof.percussioncms.doctor.CleanHeapDumpsCommand;
import jakarta.ws.rs.WebApplicationException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Auth rejection + happy-path dry-run inventory for doctor REST resource. */
class DoctorRestServiceTest {

  @TempDir Path tempDir;

  private Path installRoot;
  private Path heapDump;
  private AtomicBoolean admin;
  private DoctorRestService rest;

  @BeforeEach
  void setUp() throws Exception {
    installRoot = Files.createDirectories(tempDir.resolve("cms-install"));
    heapDump = installRoot.resolve("crash.hprof");
    Files.write(heapDump, "HEAP".getBytes(StandardCharsets.UTF_8));
    admin = new AtomicBoolean(true);
    rest = new DoctorRestService(admin::get, () -> installRoot);
  }

  @Test
  void adminDryRunInventoryHappyPath() {
    DoctorRequest req = new DoctorRequest();
    req.setDryRun(true);

    DoctorReportView view = rest.runCommand(CleanHeapDumpsCommand.COMMAND_NAME, req);

    assertTrue(view.isDryRun());
    assertEquals(1, view.getCandidateCount());
    assertEquals(0, view.getDeletedCount());
    assertTrue(Files.exists(heapDump));
    assertEquals(CleanHeapDumpsCommand.COMMAND_NAME, view.getCommand());
  }

  @Test
  void omittedBodyDefaultsToDryRun() {
    DoctorReportView view = rest.runCommand(CleanHeapDumpsCommand.COMMAND_NAME, null);
    assertTrue(view.isDryRun());
    assertTrue(Files.exists(heapDump));
  }

  @Test
  void nonAdminRejectedWith403() {
    admin.set(false);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> rest.runCommand(CleanHeapDumpsCommand.COMMAND_NAME, new DoctorRequest()));
    assertEquals(403, ex.getResponse().getStatus());
    String body = String.valueOf(ex.getResponse().getEntity());
    assertTrue(body.contains("Admin"));
    assertTrue(Files.exists(heapDump), "rejected request must not delete");
  }

  @Test
  void anonymousStyleGateFalseIs403() {
    // Admin checker returns false when no current user / anonymous.
    rest = new DoctorRestService(() -> false, () -> installRoot);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> rest.runCommand(CleanHeapDumpsCommand.COMMAND_NAME, null));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void unknownCommandIs404() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> rest.runCommand("wipe-everything", new DoctorRequest()));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  void adminCheckerExceptionIs403WithoutLeak() {
    rest =
        new DoctorRestService(
            () -> {
              throw new IllegalStateException("secret-db-host=internal");
            },
            () -> installRoot);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> rest.runCommand(CleanHeapDumpsCommand.COMMAND_NAME, null));
    assertEquals(403, ex.getResponse().getStatus());
    String body = String.valueOf(ex.getResponse().getEntity());
    assertTrue(!body.contains("secret-db-host"));
  }
}
