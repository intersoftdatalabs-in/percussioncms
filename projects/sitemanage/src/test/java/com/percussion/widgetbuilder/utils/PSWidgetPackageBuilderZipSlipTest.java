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
package com.percussion.widgetbuilder.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for {@link PSWidgetPackageBuilder} ZipSlip defense (CodeQL {@code java/zipslip}
 * alert #722, T041).
 *
 * <p>Pre-fix code used {@code new File(rootDir, resolvePath)} with the raw zip entry name, which is
 * the classic ZipSlip vector (CWE-22). The fix routes the resolved path through {@code
 * PathValidation.constructSafePath} before any {@code FileOutputStream} / {@code mkdirs}.
 */
@DisplayName("PSWidgetPackageBuilder — ZipSlip (CWE-22) regression tests")
class PSWidgetPackageBuilderZipSlipTest {

  private File parentDir;
  private File tmpDir;
  private File tgtDir;
  private File escapeFile;
  private File srcZip;

  @BeforeEach
  void setUp() throws IOException {
    parentDir = Files.createTempDirectory("widget-pkg-zipslip-parent").toFile();
    tmpDir = new File(parentDir, "tmp");
    tgtDir = new File(parentDir, "tgt");
    assertTrue(tmpDir.mkdirs());
    assertTrue(tgtDir.mkdirs());
    escapeFile = new File(parentDir, "escape.txt");
    srcZip = new File(parentDir, "template.zip");

    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(srcZip))) {
      zos.putNextEntry(new ZipEntry("../escape.txt"));
      zos.write("escaped".getBytes(StandardCharsets.UTF_8));
      zos.closeEntry();
    }
  }

  @AfterEach
  void tearDown() {
    deleteRecursive(parentDir);
  }

  @Test
  @DisplayName("generatePackage rejects zip-slip entries and does not write outside tmpDir")
  void generatePackageRejectsZipSlip() {
    PSWidgetPackageBuilder builder = new PSWidgetPackageBuilder(srcZip, tmpDir);
    // No transformers — keep the path as resolved from the zip entry only.
    builder.setFileTransformers(Collections.emptyList());

    PSWidgetPackageSpec spec =
        new PSWidgetPackageSpec(
            "test", "www.test.com", "ZipSlip Widget", "zipslip test", "1.0.0", "8.2.0");

    assertThrows(
        PSWidgetPackageBuilderException.class,
        () -> builder.generatePackage(tgtDir, spec),
        "generatePackage must fail closed when the template zip contains a traversal entry");

    assertFalse(
        escapeFile.exists(),
        "../escape.txt must not be created outside the package extract directory");
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
