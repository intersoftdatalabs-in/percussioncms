/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.ant.install;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for {@link PSExtractJarFiles} ZipSlip defense (CodeQL {@code java/zipslip} alert
 * #720, T041).
 *
 * <p>Pre-fix code concatenated {@code destinationDir + File.separator + entryName} and wrote the
 * result with {@link FileOutputStream}, which is the classic ZipSlip vector (CWE-22). The fix
 * routes every entry name through {@code PathValidation.constructSafePath} and skips unsafe
 * entries.
 */
@Tag("UnitTest")
@DisplayName("PSExtractJarFiles — ZipSlip (CWE-22) regression tests")
class PSExtractJarFilesZipSlipTest {

  private File parentDir;
  private File destDir;
  private File escapeFile;
  private File jarFile;

  @BeforeEach
  void setUp() throws IOException {
    parentDir = Files.createTempDirectory("extract-jar-zipslip-parent").toFile();
    destDir = new File(parentDir, "dest");
    assertTrue(destDir.mkdirs());
    escapeFile = new File(parentDir, "escape.txt");
    jarFile = new File(parentDir, "payload.jar");

    try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile))) {
      // Traversal entry that would escape destDir if not validated.
      jos.putNextEntry(new JarEntry("../escape.txt"));
      jos.write("escaped".getBytes(StandardCharsets.UTF_8));
      jos.closeEntry();
      // Safe entry that must still extract after the bad entry is skipped.
      jos.putNextEntry(new JarEntry("safe/ok.txt"));
      jos.write("ok".getBytes(StandardCharsets.UTF_8));
      jos.closeEntry();
    }
  }

  @AfterEach
  void tearDown() {
    deleteRecursive(parentDir);
  }

  @Test
  @DisplayName("execute skips zip-slip entries and still extracts safe ones")
  void executeRejectsZipSlipAndExtractsSafeEntry() {
    PSExtractJarFiles task = new PSExtractJarFiles();
    task.setJarFile(jarFile.getAbsolutePath());
    task.setDestinationDir(destDir.getAbsolutePath());
    task.execute();

    assertFalse(
        escapeFile.exists(),
        "ZipSlip entry ../escape.txt must not create a file outside destinationDir");
    File safe = new File(destDir, "safe/ok.txt".replace('/', File.separatorChar));
    assertTrue(safe.isFile(), "safe/ok.txt must still be extracted under destinationDir");
  }

  private static void deleteRecursive(File f) {
    if (f == null || !f.exists()) {
      return;
    }
    File[] children = f.listFiles();
    if (children != null) {
      for (File c : children) {
        deleteRecursive(c);
      }
    }
    f.delete();
  }
}
