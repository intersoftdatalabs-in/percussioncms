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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InstallRootGuardTest {

  @TempDir Path tempDir;

  @Test
  void requireInstallRootAcceptsExistingDirectory() throws Exception {
    Path root = Files.createDirectories(tempDir.resolve("install"));
    Path resolved = InstallRootGuard.requireInstallRoot(root);
    assertEquals(root.toAbsolutePath().normalize(), resolved);
  }

  @Test
  void requireInstallRootRejectsMissing() {
    Path missing = tempDir.resolve("no-such-dir");
    assertThrows(IllegalArgumentException.class, () -> InstallRootGuard.requireInstallRoot(missing));
  }

  @Test
  void isUnderInstallRootAllowsDescendantAndRoot() throws Exception {
    Path root = Files.createDirectories(tempDir.resolve("cms"));
    Path child = Files.createDirectories(root.resolve("logs"));
    assertTrue(InstallRootGuard.isUnderInstallRoot(root, root));
    assertTrue(InstallRootGuard.isUnderInstallRoot(root, child));
    assertTrue(InstallRootGuard.isUnderInstallRoot(root, child.resolve("java_pid1.hprof")));
  }

  @Test
  void requireUnderInstallRootRejectsSiblingOutsideRoot() throws Exception {
    Path root = Files.createDirectories(tempDir.resolve("cms"));
    Path outside = Files.createDirectories(tempDir.resolve("outside"));
    Path dump = outside.resolve("escape.hprof");
    Files.writeString(dump, "x");

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> InstallRootGuard.requireUnderInstallRoot(root, dump));
    assertTrue(ex.getMessage().toLowerCase().contains("outside"));
    assertFalse(InstallRootGuard.isUnderInstallRoot(root, dump));
  }

  @Test
  void requireUnderInstallRootRejectsParentEscapeViaDotDot() throws Exception {
    Path root = Files.createDirectories(tempDir.resolve("cms"));
    Path escape = root.resolve("..").resolve("escape.hprof").normalize();
    assertThrows(
        IllegalArgumentException.class,
        () -> InstallRootGuard.requireUnderInstallRoot(root, escape));
  }

  @Test
  void heapDumpAllowlistAcceptsHprofOnly() {
    assertTrue(InstallRootGuard.isHeapDumpFileName("java_pid123.hprof"));
    assertTrue(InstallRootGuard.isHeapDumpFileName("HEAP.HPROF"));
    assertFalse(InstallRootGuard.isHeapDumpFileName("app.log"));
    assertFalse(InstallRootGuard.isHeapDumpFileName("java_pid123.hprof.bak"));
    assertFalse(InstallRootGuard.isHeapDumpFileName("hprof"));
    assertFalse(InstallRootGuard.isHeapDumpFileName(""));
    assertFalse(InstallRootGuard.isHeapDumpFileName(null));
    assertFalse(InstallRootGuard.isHeapDumpFileName("../evil.hprof"));
  }
}
