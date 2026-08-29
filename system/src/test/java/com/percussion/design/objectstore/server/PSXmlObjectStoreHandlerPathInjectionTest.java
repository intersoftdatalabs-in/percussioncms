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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.utils.io.PathUtils;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
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
}
