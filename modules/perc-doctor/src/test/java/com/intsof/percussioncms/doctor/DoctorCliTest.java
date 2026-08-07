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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DoctorCliTest {

  @TempDir Path tempDir;

  @Test
  void dryRunViaCliDoesNotDelete() throws Exception {
    Path root = Files.createDirectories(tempDir.resolve("install"));
    Path dump = root.resolve("a.hprof");
    Files.write(dump, "abc".getBytes(StandardCharsets.UTF_8));

    ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
    ByteArrayOutputStream errBuf = new ByteArrayOutputStream();
    int code =
        DoctorCli.run(
            new String[] {
              "--install-root", root.toString(), "--dry-run", "-v", "clean-heap-dumps"
            },
            new PrintStream(outBuf, true, StandardCharsets.UTF_8),
            new PrintStream(errBuf, true, StandardCharsets.UTF_8));

    assertEquals(DoctorCli.EXIT_OK, code);
    assertTrue(Files.exists(dump));
    String out = outBuf.toString(StandardCharsets.UTF_8);
    assertTrue(out.contains("dry-run=true"));
    assertTrue(out.contains("candidates=1"));
    assertTrue(out.contains("WOULD_DELETE"));
  }

  @Test
  void applyViaCliDeletesHeapDumps() throws Exception {
    Path root = Files.createDirectories(tempDir.resolve("install2"));
    Path dump = root.resolve("b.hprof");
    Files.write(dump, "xyz".getBytes(StandardCharsets.UTF_8));

    ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
    ByteArrayOutputStream errBuf = new ByteArrayOutputStream();
    int code =
        DoctorCli.run(
            new String[] {"--install-root", root.toString(), "clean-heap-dumps"},
            new PrintStream(outBuf, true, StandardCharsets.UTF_8),
            new PrintStream(errBuf, true, StandardCharsets.UTF_8));

    assertEquals(DoctorCli.EXIT_OK, code);
    assertFalse(Files.exists(dump));
    String out = outBuf.toString(StandardCharsets.UTF_8);
    assertTrue(out.contains("deleted=1"));
  }

  @Test
  void missingCommandIsUsageError() {
    ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
    ByteArrayOutputStream errBuf = new ByteArrayOutputStream();
    int code =
        DoctorCli.run(
            new String[] {"--dry-run"},
            new PrintStream(outBuf, true, StandardCharsets.UTF_8),
            new PrintStream(errBuf, true, StandardCharsets.UTF_8));
    assertEquals(DoctorCli.EXIT_USAGE, code);
  }

  @Test
  void unknownOptionIsUsageError() {
    ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
    ByteArrayOutputStream errBuf = new ByteArrayOutputStream();
    int code =
        DoctorCli.run(
            new String[] {"--not-a-real-flag", "clean-heap-dumps"},
            new PrintStream(outBuf, true, StandardCharsets.UTF_8),
            new PrintStream(errBuf, true, StandardCharsets.UTF_8));
    assertEquals(DoctorCli.EXIT_USAGE, code);
  }

  @Test
  void unknownCommandIsUsageError() {
    ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
    ByteArrayOutputStream errBuf = new ByteArrayOutputStream();
    int code =
        DoctorCli.run(
            new String[] {"clean-everything"},
            new PrintStream(outBuf, true, StandardCharsets.UTF_8),
            new PrintStream(errBuf, true, StandardCharsets.UTF_8));
    assertEquals(DoctorCli.EXIT_USAGE, code);
  }

  @Test
  void missingInstallRootIsError() {
    Path missing = tempDir.resolve("gone");
    ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
    ByteArrayOutputStream errBuf = new ByteArrayOutputStream();
    int code =
        DoctorCli.run(
            new String[] {
              "--install-root", missing.toString(), "--dry-run", "clean-heap-dumps"
            },
            new PrintStream(outBuf, true, StandardCharsets.UTF_8),
            new PrintStream(errBuf, true, StandardCharsets.UTF_8));
    assertEquals(DoctorCli.EXIT_ERROR, code);
    assertTrue(errBuf.toString(StandardCharsets.UTF_8).toLowerCase().contains("install root"));
  }

  @Test
  void cleanInstallBackupsDryRunViaCliDoesNotDelete() throws Exception {
    Path root = Files.createDirectories(tempDir.resolve("install-bak"));
    Path bak = root.resolve("config.properties.backup");
    Path zip = root.resolve("AppServer_backup_20260101.zip");
    Files.writeString(bak, "bak");
    Files.write(zip, "zip".getBytes(StandardCharsets.UTF_8));

    ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
    ByteArrayOutputStream errBuf = new ByteArrayOutputStream();
    int code =
        DoctorCli.run(
            new String[] {
              "--install-root", root.toString(), "--dry-run", "-v", "clean-install-backups"
            },
            new PrintStream(outBuf, true, StandardCharsets.UTF_8),
            new PrintStream(errBuf, true, StandardCharsets.UTF_8));

    assertEquals(DoctorCli.EXIT_OK, code);
    assertTrue(Files.exists(bak));
    assertTrue(Files.exists(zip));
    String out = outBuf.toString(StandardCharsets.UTF_8);
    assertTrue(out.contains("command=clean-install-backups"));
    assertTrue(out.contains("dry-run=true"));
    assertTrue(out.contains("candidates=2"));
    assertTrue(out.contains("WOULD_DELETE"));
  }

  @Test
  void cleanInstallBackupsApplyViaCliDeletesAllowlistedOnly() throws Exception {
    Path root = Files.createDirectories(tempDir.resolve("install-bak-apply"));
    Path bak = root.resolve("x.bak");
    Path keep = root.resolve("server.log");
    Files.writeString(bak, "bak");
    Files.writeString(keep, "log");

    ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
    ByteArrayOutputStream errBuf = new ByteArrayOutputStream();
    int code =
        DoctorCli.run(
            new String[] {"--install-root", root.toString(), "clean-install-backups"},
            new PrintStream(outBuf, true, StandardCharsets.UTF_8),
            new PrintStream(errBuf, true, StandardCharsets.UTF_8));

    assertEquals(DoctorCli.EXIT_OK, code);
    assertFalse(Files.exists(bak));
    assertTrue(Files.exists(keep));
    String out = outBuf.toString(StandardCharsets.UTF_8);
    assertTrue(out.contains("deleted=1"));
  }

  @Test
  void cleanLogsDryRunViaCliDoesNotDelete() throws Exception {
    Path root = Files.createDirectories(tempDir.resolve("install-logs"));
    Path logs =
        Files.createDirectories(root.resolve("jetty").resolve("base").resolve("logs"));
    Path current = logs.resolve("server.log");
    Path rolled = logs.resolve("server-2020-01-01-1.log");
    Files.writeString(current, "cur");
    Files.writeString(rolled, "old");
    Files.setLastModifiedTime(
        rolled, java.nio.file.attribute.FileTime.from(java.time.Instant.now().minusSeconds(86400L * 30)));

    ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
    ByteArrayOutputStream errBuf = new ByteArrayOutputStream();
    int code =
        DoctorCli.run(
            new String[] {
              "--install-root",
              root.toString(),
              "--dry-run",
              "-v",
              "clean-logs",
              "--older-than",
              "7d"
            },
            new PrintStream(outBuf, true, StandardCharsets.UTF_8),
            new PrintStream(errBuf, true, StandardCharsets.UTF_8));

    assertEquals(DoctorCli.EXIT_OK, code);
    assertTrue(Files.exists(current));
    assertTrue(Files.exists(rolled));
    String out = outBuf.toString(StandardCharsets.UTF_8);
    assertTrue(out.contains("command=clean-logs"));
    assertTrue(out.contains("dry-run=true"));
    assertTrue(out.contains("WOULD_DELETE"));
  }

  @Test
  void cleanLogsApplyViaCliKeepsCurrentDeletesAgedRolled() throws Exception {
    Path root = Files.createDirectories(tempDir.resolve("install-logs-apply"));
    Path logs =
        Files.createDirectories(root.resolve("jetty").resolve("base").resolve("logs"));
    Path current = logs.resolve("server.log");
    Path rolled = logs.resolve("server-2020-02-02-1.log");
    Files.writeString(current, "cur");
    Files.writeString(rolled, "old");
    Files.setLastModifiedTime(
        rolled, java.nio.file.attribute.FileTime.from(java.time.Instant.now().minusSeconds(86400L * 30)));
    Files.setLastModifiedTime(
        current, java.nio.file.attribute.FileTime.from(java.time.Instant.now().minusSeconds(86400L * 30)));

    ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
    ByteArrayOutputStream errBuf = new ByteArrayOutputStream();
    int code =
        DoctorCli.run(
            new String[] {
              "--install-root", root.toString(), "clean-logs", "--older-than", "7d"
            },
            new PrintStream(outBuf, true, StandardCharsets.UTF_8),
            new PrintStream(errBuf, true, StandardCharsets.UTF_8));

    assertEquals(DoctorCli.EXIT_OK, code);
    assertTrue(Files.exists(current), "keep-current default retains server.log");
    assertFalse(Files.exists(rolled));
    String out = outBuf.toString(StandardCharsets.UTF_8);
    assertTrue(out.contains("deleted=1"));
  }

  @Test
  void cleanLogsInvalidOlderThanIsUsageError() {
    ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
    ByteArrayOutputStream errBuf = new ByteArrayOutputStream();
    int code =
        DoctorCli.run(
            new String[] {"clean-logs", "--older-than", "not-a-duration"},
            new PrintStream(outBuf, true, StandardCharsets.UTF_8),
            new PrintStream(errBuf, true, StandardCharsets.UTF_8));
    assertEquals(DoctorCli.EXIT_USAGE, code);
  }

  @Test
  void olderThanOnNonCleanLogsCommandWarnsAndContinues() throws Exception {
    Path root = Files.createDirectories(tempDir.resolve("install-older-warn"));
    Path dump = root.resolve("warn.hprof");
    Files.write(dump, "abc".getBytes(StandardCharsets.UTF_8));

    ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
    ByteArrayOutputStream errBuf = new ByteArrayOutputStream();
    int code =
        DoctorCli.run(
            new String[] {
              "--install-root",
              root.toString(),
              "--dry-run",
              "--older-than",
              "7d",
              "clean-heap-dumps"
            },
            new PrintStream(outBuf, true, StandardCharsets.UTF_8),
            new PrintStream(errBuf, true, StandardCharsets.UTF_8));

    assertEquals(DoctorCli.EXIT_OK, code);
    String err = errBuf.toString(StandardCharsets.UTF_8);
    assertTrue(
        err.contains("--older-than") && err.toLowerCase().contains("ignoring"),
        "expected ignore warning for --older-than on non-clean-logs: " + err);
    assertTrue(Files.exists(dump));
    assertTrue(outBuf.toString(StandardCharsets.UTF_8).contains("candidates=1"));
  }
}
