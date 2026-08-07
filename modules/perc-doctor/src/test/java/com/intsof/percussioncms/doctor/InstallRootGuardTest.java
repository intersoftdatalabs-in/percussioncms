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
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
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

  @Test
  void installBackupAllowlistAcceptsDocumentedPatternsOnly() {
    assertTrue(InstallRootGuard.isInstallBackupFileName("AppServer_backup_20240115_120000.zip"));
    assertTrue(InstallRootGuard.isInstallBackupFileName("appserver_backup_1.zip"));
    assertTrue(InstallRootGuard.isInstallBackupFileName("ResourceBundle.tmx.bak"));
    assertTrue(InstallRootGuard.isInstallBackupFileName("file.BAK"));
    assertTrue(InstallRootGuard.isInstallBackupFileName("misc.config.backup"));
    assertTrue(InstallRootGuard.isInstallBackupFileName("Navigation.properties.backup"));
    assertTrue(InstallRootGuard.isInstallBackupFileName("FOO.PROPERTIES.BACKUP"));

    assertFalse(InstallRootGuard.isInstallBackupFileName("AppServer_backup_.zip"));
    assertFalse(InstallRootGuard.isInstallBackupFileName("AppServer_backup.zip"));
    assertFalse(InstallRootGuard.isInstallBackupFileName("OtherServer_backup_1.zip"));
    assertFalse(InstallRootGuard.isInstallBackupFileName("java_pid123.hprof"));
    assertFalse(InstallRootGuard.isInstallBackupFileName("server.log"));
    assertFalse(InstallRootGuard.isInstallBackupFileName("notes.txt"));
    assertFalse(InstallRootGuard.isInstallBackupFileName(""));
    assertFalse(InstallRootGuard.isInstallBackupFileName(null));
    assertFalse(InstallRootGuard.isInstallBackupFileName("../evil.bak"));
    assertFalse(InstallRootGuard.isInstallBackupFileName("dir/file.bak"));
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  void isUnderInstallRootIsCaseInsensitiveOnWindows() {
    // Path.startsWith is case-sensitive; Windows FS is not — guard must fold.
    Path root = Path.of("C:\\Percussion\\Install");
    Path child = Path.of("c:\\percussion\\install\\jetty\\base\\java_pid1.hprof");
    Path siblingPrefix = Path.of("C:\\Percussion\\InstallExtra\\x.hprof");
    assertTrue(InstallRootGuard.isUnderInstallRoot(root, child));
    assertTrue(InstallRootGuard.isUnderInstallRoot(root, Path.of("C:\\PERCUSSION\\INSTALL")));
    assertFalse(
        InstallRootGuard.isUnderInstallRoot(root, siblingPrefix),
        "must not treat InstallExtra as under Install");
  }
}
