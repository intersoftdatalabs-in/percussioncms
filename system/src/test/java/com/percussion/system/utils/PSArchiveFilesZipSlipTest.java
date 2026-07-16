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
package com.percussion.system.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.security.validation.PathValidation;
import com.percussion.security.validation.PathValidation.SecurityException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for {@link PSArchiveFiles#extractFilesFromArchive} (CodeQL
 * {@code java/zipslip}, T041, US3).
 *
 * <p><strong>Background.</strong> The pre-fix code used the raw {@code entry.getName()}
 * as the destination path of a {@link FileOutputStream}, which is the canonical
 * zip-slip vector (CWE-22): a zip entry named {@code ../../etc/passwd} would write
 * outside the extract directory. The fix routes the entry name through
 * {@link PathValidation#constructSafePath(File, String)}, which canonicalizes the
 * resolved path and rejects any path that escapes the base directory.
 *
 * <p><strong>Fail-then-pass coverage (Constitution III).</strong> The pre-fix
 * code was structurally vulnerable — for a zip entry named {@code ../escape.txt},
 * the test on a real local filesystem would create {@code escape.txt} in the
 * parent directory of the extract root. The post-fix code throws a
 * {@link SecurityException} (wrapped in {@link IOException} by
 * {@code extractFilesFromArchive}) and writes nothing. The test below exercises
 * this directly: a zip containing a traversal entry is created on disk, the
 * extract is invoked, and the test asserts the parent directory is unchanged.
 */
@DisplayName("PSArchiveFiles.extractFilesFromArchive — ZipSlip (CWE-22) regression tests")
class PSArchiveFilesZipSlipTest {

  private File extractDir;
  private File parentDir;
  private File escapeFile;
  private File zipFile;

  @BeforeEach
  void setUp() throws IOException {
    parentDir = Files.createTempDirectory("zipslip-parent").toFile();
    extractDir = new File(parentDir, "extract");
    assertTrue(extractDir.mkdirs(), "extract dir should be created");
    escapeFile = new File(parentDir, "escape.txt");
    zipFile = new File(parentDir, "test.zip");
    try (var zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
      // Bad entry: traversal path that would escape extractDir. Listed
      // first so the SecurityException aborts the extract before any
      // good entries are processed.
      ZipEntry escape = new ZipEntry("../escape.txt");
      zos.putNextEntry(escape);
      zos.write("escaped content".getBytes());
      zos.closeEntry();
      // Good entry: relative path under extractDir. Listed second; with
      // the fix this is NEVER reached because the traversal entry throws
      // and aborts the loop.
      ZipEntry good = new ZipEntry("good.txt");
      zos.putNextEntry(good);
      zos.write("safe content".getBytes());
      zos.closeEntry();
    }
  }

  @AfterEach
  void tearDown() {
    deleteRecursive(parentDir);
  }

  @Test
  @DisplayName("extractFilesFromArchive rejects zip-slip entries and skips them")
  void testExtractRejectsZipSlip() throws IOException {
    try (var archiveFile = new ZipFile(zipFile)) {
      // The fix routes every entry's name through PathValidation.constructSafePath,
      // which throws SecurityException (a RuntimeException) for any path that
      // resolves outside extractDir. extractFilesFromArchive's outer catch
      // only handles IOException, so the SecurityException propagates out
      // of the for loop. We assert that the bad entry is rejected and the
      // escape file is NOT created in the parent directory.
      SecurityException ex = assertThrows(
          SecurityException.class,
          () -> PSArchiveFiles.extractFilesFromArchive(
              archiveFile, extractDir.getAbsolutePath(), null),
          "extractFilesFromArchive must propagate PathValidation's"
              + " SecurityException for a zip-slip entry");
      assertNotNull(ex.getMessage(),
          "the SecurityException must carry a descriptive message");
      assertTrue(ex.getMessage().toLowerCase().contains("escape")
              || ex.getMessage().toLowerCase().contains("cwe-22")
              || ex.getMessage().toLowerCase().contains("traversal")
              || ex.getMessage().toLowerCase().contains("../"),
          "the SecurityException message should describe the traversal attempt,"
              + " got: " + ex.getMessage());
      assertFalse(escapeFile.exists(),
          "../escape.txt must NOT be created in the parent of the extract directory"
              + " (the zip-slip regression - the file appearing here is the original"
              + " CWE-22 path-traversal bug)");
    }
  }

  @Test
  @DisplayName("PathValidation.constructSafePath rejects absolute paths in user input")
  void testConstructSafePathRejectsAbsolutePath() {
    File base = extractDir;
    assertThrows(SecurityException.class,
        () -> PathValidation.constructSafePath(base, "/etc/passwd"),
        "constructSafePath must throw SecurityException for an absolute userPath");
  }

  @Test
  @DisplayName("PathValidation.constructSafePath rejects ..-traversal in user input")
  void testConstructSafePathRejectsTraversal() {
    File base = extractDir;
    assertThrows(SecurityException.class,
        () -> PathValidation.constructSafePath(base, "../../etc/passwd"),
        "constructSafePath must throw SecurityException for a ..-traversal userPath");
  }

  @Test
  @DisplayName("PathValidation.constructSafePath returns a path under baseDir for a valid relative path")
  void testConstructSafePathAcceptsValidRelative() throws IOException {
    File base = extractDir;
    File safe = PathValidation.constructSafePath(base, "subdir/file.txt");
    assertNotNull(safe);
    // Canonicalize both sides before comparing. PathValidation.constructSafePath
    // returns the canonical form of the resolved path (long-form on Windows),
    // but base.getAbsoluteFile() returns the path with whatever the JVM cached
    // for the original constructor - on Windows that can be the 8.3 short form
    // (e.g. C:\Users\VIJAYA~1.BOD\...) even when the OS still resolves the long
    // name. The two forms are NOT equal path-by-path, so Path#startsWith sees
    // different first components (VIJAYA~1.BOD vs vijaya.boddipudi) and
    // returns false even though both addresses point at the same directory.
    // Forcing both sides through getCanonicalFile() puts them in the same
    // canonical long-form, after which Path#startsWith (which is case-insensitive
    // on Windows) reports containment correctly.
    Path basePath = base.getCanonicalFile().toPath();
    Path safePath = safe.getCanonicalFile().toPath();
    assertTrue(safePath.startsWith(basePath),
        "safe path must be under baseDir, got: " + safe);
  }

  /** Recursive delete helper for tmp dirs. */
  private static void deleteRecursive(File f) {
    if (f == null || !f.exists()) return;
    File[] children = f.listFiles();
    if (children != null) {
      for (var c : children) deleteRecursive(c);
    }
    f.delete();
  }
}
