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
package com.percussion.design.objectstore.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.percussion.design.objectstore.server.PSServerXmlObjectStore.RecoverableFile;
import com.percussion.utils.io.PathUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Path-injection barrier for application request-root resolution (CodeQL {@code
 * java/path-injection} #2001 / #1988–#2000).
 */
@DisplayName("PSServerXmlObjectStore.getAppRootDir path-injection barrier (CWE-22, #2001)")
class PSServerXmlObjectStorePathInjectionTest {

  @TempDir Path tmp;

  @AfterEach
  void clearThreadRxDir() {
    PathUtils.unsetThreadOnlyRxDir(tmp.toFile());
  }

  @Test
  void getAppRootDir_rejectsParentTraversal() {
    PathUtils.setThreadOnlyRxDir(tmp.toFile());
    assertThrows(
        IllegalArgumentException.class,
        () -> PSServerXmlObjectStore.getAppRootDir(".." + File.separator + "escape"));
  }

  @Test
  void getAppRootDir_resolvesChildUnderRxDir() throws Exception {
    File rx = tmp.toFile();
    PathUtils.setThreadOnlyRxDir(rx);
    File resolved = PSServerXmlObjectStore.getAppRootDir("MyApp");
    assertEquals(new File(rx, "MyApp").getCanonicalFile(), resolved.getCanonicalFile());
    assertTrue(resolved.getCanonicalFile().toPath().startsWith(rx.getCanonicalFile().toPath()));
  }

  @Test
  void getAppRootDir_emptyUsesRxDir_nullRejected() throws Exception {
    File rx = tmp.toFile();
    PathUtils.setThreadOnlyRxDir(rx);
    assertEquals(rx.getCanonicalFile(), PSServerXmlObjectStore.getAppRootDir("").getCanonicalFile());
    assertThrows(IllegalArgumentException.class, () -> PSServerXmlObjectStore.getAppRootDir(null));
  }

  @Test
  void deleteFile_removesFileUnderRxDirAndRejectsEscape() throws Exception {
    PathUtils.setThreadOnlyRxDir(tmp.toFile());
    File child = tmp.resolve("gone.txt").toFile();
    Files.writeString(child.toPath(), "x");
    assertTrue(PSServerXmlObjectStore.deleteFile(child));
    assertFalse(child.exists());

    File outside = tmp.getParent().resolve("escape-delete.txt").toFile();
    Files.writeString(outside.toPath(), "y");
    assertThrows(
        IllegalArgumentException.class, () -> PSServerXmlObjectStore.deleteFile(outside));
    assertTrue(outside.exists());
  }

  @Test
  void deleteDirectory_removesTreeUnderRxDir() throws Exception {
    PathUtils.setThreadOnlyRxDir(tmp.toFile());
    Path dir = tmp.resolve("appdir");
    Files.createDirectories(dir.resolve("sub"));
    Files.writeString(dir.resolve("sub").resolve("f.txt"), "z");
    PSServerXmlObjectStore.deleteDirectory(dir.toFile());
    assertTrue(Files.notExists(dir));
  }

  @Test
  void deleteFile_deletesSymlinkNotTarget() throws Exception {
    PathUtils.setThreadOnlyRxDir(tmp.toFile());
    Path target = tmp.resolve("target-dir");
    Files.createDirectories(target);
    Path marker = target.resolve("keep.txt");
    Files.writeString(marker, "keep");
    Path link = tmp.resolve("link-dir");
    try {
      Files.createSymbolicLink(link, target);
    } catch (IOException | UnsupportedOperationException e) {
      assumeTrue(false, "symlinks not available: " + e.getMessage());
    }
    assertTrue(PSServerXmlObjectStore.deleteFile(link.toFile()));
    assertTrue(Files.notExists(link));
    assertTrue(Files.exists(marker));
  }

  @Test
  void requireFileUnderRxDir_rejectsEscapeAndAcceptsChild() throws Exception {
    PathUtils.setThreadOnlyRxDir(tmp.toFile());
    File outside = tmp.getParent().resolve("escape-save.txt").toFile();
    Files.writeString(outside.toPath(), "no");
    assertThrows(
        IllegalArgumentException.class,
        () -> PSServerXmlObjectStore.requireFileUnderRxDir(outside));

    File child = tmp.resolve("app").resolve("file.txt").toFile();
    Files.createDirectories(child.getParentFile().toPath());
    Files.writeString(child.toPath(), "ok");
    File safe = PSServerXmlObjectStore.requireFileUnderRxDir(child);
    assertEquals(child.getCanonicalFile(), safe.getCanonicalFile());
  }

  @Test
  void recoverableFile_rejectsEscapeAndAcceptsUnderRxDir() throws Exception {
    PathUtils.setThreadOnlyRxDir(tmp.toFile());
    File outside = tmp.getParent().resolve("escape-recover.txt").toFile();
    Files.writeString(outside.toPath(), "no");
    assertThrows(
        IllegalArgumentException.class, () -> new RecoverableFile(outside));

    File child = tmp.resolve("app.xml").toFile();
    Files.writeString(child.toPath(), "<app/>");
    RecoverableFile rf = new RecoverableFile(child);
    assertFalse(child.exists());
    assertTrue(rf.recover());
    assertTrue(child.exists());
    assertEquals("<app/>", Files.readString(child.toPath()));
  }
}
