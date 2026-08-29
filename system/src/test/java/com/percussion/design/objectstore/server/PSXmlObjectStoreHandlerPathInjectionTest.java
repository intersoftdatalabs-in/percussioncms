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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.error.PSNotFoundException;
import com.percussion.utils.io.PathUtils;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Path-injection barriers for object-store application files and lock streams (CodeQL {@code
 * java/path-injection} #2002 / #1920 / #1921).
 */
@DisplayName("PSXmlObjectStoreHandler path-injection barrier (CWE-22, #2002)")
class PSXmlObjectStoreHandlerPathInjectionTest {

  @TempDir Path tmp;

  @AfterEach
  void clearThreadRxDir() {
    PathUtils.unsetThreadOnlyRxDir(tmp.toFile());
  }

  @Test
  void resolveApplicationXmlFile_rejectsParentTraversal() {
    File objectDir = tmp.toFile();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            PSXmlObjectStoreHandler.resolveApplicationXmlFile(
                objectDir, ".." + File.separator + "escape"));
  }

  @Test
  void resolveApplicationXmlFile_placesXmlUnderObjectDir() throws Exception {
    File objectDir = tmp.toFile();
    File resolved = PSXmlObjectStoreHandler.resolveApplicationXmlFile(objectDir, "MyApp");
    assertEquals(
        new File(objectDir, "MyApp.xml").getCanonicalFile(), resolved.getCanonicalFile());
    assertTrue(
        resolved.getCanonicalFile().toPath().startsWith(objectDir.getCanonicalFile().toPath()));
  }

  @Test
  void requireUnderRxDir_rejectsEscapeOutsideRxRoot() {
    File rx = tmp.toFile();
    PathUtils.setThreadOnlyRxDir(rx);
    File outside = tmp.getParent().resolve("escape.txt").toFile();
    assertThrows(
        IllegalArgumentException.class, () -> PSXmlObjectStoreHandler.requireUnderRxDir(outside));
  }

  @Test
  void requireUnderRxDir_allowsFileUnderRxRoot() throws Exception {
    File rx = tmp.toFile();
    PathUtils.setThreadOnlyRxDir(rx);
    File child = tmp.resolve("ObjectStore").resolve("app.xml").toFile();
    Files.createDirectories(child.getParentFile().toPath());
    Files.writeString(child.toPath(), "<app/>");
    File safe = PSXmlObjectStoreHandler.requireUnderRxDir(child);
    assertEquals(child.getCanonicalFile(), safe.getCanonicalFile());
  }

  @Test
  void getApplicationFile_rejectsParentTraversal() throws Exception {
    PSXmlObjectStoreHandler handler = newHandler();
    assertThrows(
        IllegalArgumentException.class,
        () -> handler.getApplicationFile(".." + File.separator + "escape"));
  }

  @Test
  void loadApplication_translatesTraversalToNotFound() throws Exception {
    PSXmlObjectStoreHandler handler = newHandler();
    assertThrows(
        PSNotFoundException.class,
        () -> handler.loadApplication(".." + File.separator + "escape"));
  }

  @Test
  void lockOutputStream_writesUnderRxDirAndRejectsEscape() throws Exception {
    PSXmlObjectStoreHandler handler = newHandler();
    File target = tmp.resolve("ObjectStore").resolve("locked.xml").toFile();
    OutputStream out = handler.lockOutputStream(target);
    try {
      out.write(new byte[] {1, 2, 3});
    } finally {
      handler.releaseOutputStream(out, target);
    }
    assertArrayEquals(new byte[] {1, 2, 3}, Files.readAllBytes(target.toPath()));

    File outside = tmp.getParent().resolve("escape.bin").toFile();
    assertThrows(IllegalArgumentException.class, () -> handler.lockOutputStream(outside));
  }

  @Test
  void lockInputStream_readsUnderRxDirAndRejectsEscape() throws Exception {
    PSXmlObjectStoreHandler handler = newHandler();
    File target = tmp.resolve("ObjectStore").resolve("locked-in.xml").toFile();
    Files.write(target.toPath(), new byte[] {9, 8, 7});
    InputStream in = handler.lockInputStream(target);
    try {
      assertArrayEquals(new byte[] {9, 8, 7}, in.readAllBytes());
    } finally {
      handler.releaseInputStream(in, target);
    }

    File outside = tmp.getParent().resolve("escape-in.bin").toFile();
    Files.write(outside.toPath(), new byte[] {1});
    assertThrows(IllegalArgumentException.class, () -> handler.lockInputStream(outside));
  }

  private PSXmlObjectStoreHandler newHandler() throws Exception {
    Path objectStore = tmp.resolve("ObjectStore");
    Files.createDirectories(objectStore);
    PathUtils.setThreadOnlyRxDir(tmp.toFile());
    Properties props = new Properties();
    props.put("objectDirectory", "ObjectStore");
    return new PSXmlObjectStoreHandler(props);
  }
}
