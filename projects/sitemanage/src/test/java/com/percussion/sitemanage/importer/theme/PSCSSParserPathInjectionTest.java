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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.sitemanage.importer.theme;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sun.misc.Unsafe;

/**
 * Regression tests for {@link PSCSSParser} focused on the three {@code java/path-injection}
 * findings closed at {@code PSCSSParser.java:236, 264, 275} (CodeQL alerts #1055, #1056, #1057,
 * T043).
 *
 * <p>The pre-fix code called {@code new File(path)} / {@code new FileWriter(path)} / {@code new
 * FileInputStream(new File(path))} with paths derived from CSS {@code @import} and {@code url(...)}
 * values extracted via regex from user-supplied CSS, without verifying the resolved path was
 * contained within the theme root. A CSS payload containing {@code @import
 * url('../../../etc/passwd')} could escape the theme directory and reach the filesystem with
 * attacker-controlled traversal sequences.
 *
 * <p>The fix calls {@link com.percussion.security.io.PSPathInjectionGuard#requireUnderBase} on
 * every path BEFORE any File / FileWriter / FileInputStream construction, with the theme root as
 * the canonical base.
 *
 * <p>Tests use {@link sun.misc.Unsafe#allocateInstance} to bypass the PSCSSParser constructor
 * (which has a {@code @notNull} precondition and a real logger dependency) and reflectively invoke
 * the three private sink methods. The same pattern is used by {@code
 * PSSiteDataServicePathInjectionTest} (T043).
 */
public class PSCSSParserPathInjectionTest {

  @TempDir File themeRoot;

  private static final Unsafe UNSAFE;
  private static final Method FILE_EXISTS_METHOD;
  private static final Method SAVE_FILE_METHOD;
  private static final Method LOAD_FILE_METHOD;

  static {
    try {
      Field f = Unsafe.class.getDeclaredField("theUnsafe");
      f.setAccessible(true);
      UNSAFE = (Unsafe) f.get(null);
      FILE_EXISTS_METHOD = PSCSSParser.class.getDeclaredMethod("fileExists", String.class);
      FILE_EXISTS_METHOD.setAccessible(true);
      SAVE_FILE_METHOD =
          PSCSSParser.class.getDeclaredMethod("saveFile", StringBuffer.class, String.class);
      SAVE_FILE_METHOD.setAccessible(true);
      LOAD_FILE_METHOD = PSCSSParser.class.getDeclaredMethod("loadFileFromDisk", String.class);
      LOAD_FILE_METHOD.setAccessible(true);
    } catch (ReflectiveOperationException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  /**
   * Constructs an uninitialized PSCSSParser instance and sets the {@code themeRootDirectory} field
   * via reflection so the validator has a real base directory to check against.
   */
  private PSCSSParser parser() throws Exception {
    PSCSSParser p = (PSCSSParser) UNSAFE.allocateInstance(PSCSSParser.class);
    setField(p, "themeRootDirectory", themeRoot.getAbsolutePath());
    // logger is unused by the three sink methods being tested (they do
    // not log on success), so leave it null. setFileDownloader would
    // require a real downloader; not needed for these tests.
    setField(p, "logger", null);
    return p;
  }

  private static void setField(Object target, String name, Object value)
      throws ReflectiveOperationException {
    Field f = PSCSSParser.class.getDeclaredField(name);
    f.setAccessible(true);
    f.set(target, value);
  }

  private static Object invokeFileExists(PSCSSParser p, String path) throws Throwable {
    try {
      return FILE_EXISTS_METHOD.invoke(p, path);
    } catch (InvocationTargetException ite) {
      throw ite.getCause();
    }
  }

  private static Object invokeSaveFile(PSCSSParser p, StringBuffer sb, String path)
      throws Throwable {
    try {
      return SAVE_FILE_METHOD.invoke(p, sb, path);
    } catch (InvocationTargetException ite) {
      throw ite.getCause();
    }
  }

  private static Object invokeLoadFile(PSCSSParser p, String path) throws Throwable {
    try {
      return LOAD_FILE_METHOD.invoke(p, path);
    } catch (InvocationTargetException ite) {
      throw ite.getCause();
    }
  }

  // ====================================================================
  // fileExists sink (#1055)
  // ====================================================================

  @Test
  @DisplayName(
      "fileExists: rejects a '..' traversal payload before any File construction —"
          + " CodeQL java/path-injection #1055")
  void testFileExistsRejectsTraversal() throws Throwable {
    PSCSSParser p = parser();
    String traversal = themeRoot.getAbsolutePath() + "/../../../etc/passwd";
    assertThrows(
        IllegalArgumentException.class,
        () -> invokeFileExists(p, traversal),
        "A '..' traversal payload must be rejected by PSPathInjectionGuard.requireUnderBase"
            + " before new File(path).exists() runs. Pre-fix: silently returns false (or"
            + " reads /etc/passwd if the path resolves). Post-fix: IllegalArgumentException.");
  }

  @Test
  @DisplayName("fileExists: rejects a relative traversal payload (cross-platform)")
  void testFileExistsRejectsRelativeTraversal() throws Throwable {
    PSCSSParser p = parser();
    // Use a relative '..' traversal that escapes the theme root. This form
    // is platform-portable: on Unix it resolves against the cwd, on
    // Windows requireUnderBase still detects the prefix mismatch because
    // the resolved canonical path will sit outside themeRoot.
    String traversal = themeRoot.getAbsolutePath() + "/../escape.css";
    assertThrows(
        IllegalArgumentException.class,
        () -> invokeFileExists(p, traversal),
        "A relative traversal payload that escapes the theme root must be"
            + " rejected by PSPathInjectionGuard.requireUnderBase.");
  }

  @Test
  @DisplayName("fileExists: rejects a relative traversal payload that reaches /etc/passwd")
  void testFileExistsRejectsEtcPasswdTraversal() throws Throwable {
    PSCSSParser p = parser();
    // Platform-portable: this is a path like
    //   <themeRoot>/sub1/../../../../etc/passwd
    // which canonicalizes to a path under /etc/passwd on Unix, and to a
    // path under C:\etc\passwd on Windows — both OUTSIDE the theme root.
    String traversal = themeRoot.getAbsolutePath() + "/sub1/../../../../etc/passwd";
    assertThrows(
        IllegalArgumentException.class,
        () -> invokeFileExists(p, traversal),
        "A traversal payload reaching /etc/passwd must be rejected.");
  }

  @Test
  @DisplayName("fileExists: rejects a NUL byte payload")
  void testFileExistsRejectsNul() throws Throwable {
    PSCSSParser p = parser();
    assertThrows(
        IllegalArgumentException.class, () -> invokeFileExists(p, "good.css\0../../etc/passwd"));
  }

  @Test
  @DisplayName("fileExists: rejects null")
  void testFileExistsRejectsNull() throws Throwable {
    PSCSSParser p = parser();
    assertThrows(IllegalArgumentException.class, () -> invokeFileExists(p, null));
  }

  @Test
  @DisplayName("fileExists: accepts a path inside the theme root and returns false for missing")
  void testFileExistsAcceptsPathInsideRoot() throws Throwable {
    PSCSSParser p = parser();
    // The file does not exist (we did not create it), so fileExists must
    // return false — proving the validator passed and the file construction
    // ran without throwing.
    Object result =
        assertDoesNotThrow(
            () -> invokeFileExists(p, new File(themeRoot, "missing.css").getAbsolutePath()));
    assertEquals(false, result, "Missing file in theme root must return false");
  }

  // ====================================================================
  // saveFile sink (#1056)
  // ====================================================================

  @Test
  @DisplayName(
      "saveFile: rejects a '..' traversal payload before any FileWriter construction —"
          + " CodeQL java/path-injection #1056")
  void testSaveFileRejectsTraversal() throws Throwable {
    PSCSSParser p = parser();
    String traversal = themeRoot.getAbsolutePath() + "/../../etc/evil.css";
    assertThrows(
        IllegalArgumentException.class,
        () -> invokeSaveFile(p, new StringBuffer("body { color: red; }"), traversal));
  }

  @Test
  @DisplayName("saveFile: accepts a path inside the theme root and writes the file")
  void testSaveFileAcceptsPathInsideRoot() throws Throwable {
    PSCSSParser p = parser();
    File target = new File(themeRoot, "out.css");
    assertDoesNotThrow(
        () ->
            invokeSaveFile(p, new StringBuffer("body { color: red; }"), target.getAbsolutePath()));
    assertTrue(target.exists(), "saveFile must write the file when path is inside theme root");
  }

  // ====================================================================
  // loadFileFromDisk sink (#1057)
  // ====================================================================

  @Test
  @DisplayName(
      "loadFileFromDisk: rejects a '..' traversal payload before any FileInputStream"
          + " construction — CodeQL java/path-injection #1057")
  void testLoadFileFromDiskRejectsTraversal() throws Throwable {
    PSCSSParser p = parser();
    String traversal = themeRoot.getAbsolutePath() + "/../../etc/passwd";
    assertThrows(IllegalArgumentException.class, () -> invokeLoadFile(p, traversal));
  }

  @Test
  @DisplayName("loadFileFromDisk: accepts a path under the theme root and reports missing file")
  void testLoadFileFromDiskAcceptsPathInsideRoot() throws Throwable {
    PSCSSParser p = parser();
    // Path is under themeRoot, so validation passes. The file does not
    // exist, so FileInputStream throws FileNotFoundException — which proves
    // the validator ran FIRST (no IllegalArgumentException).
    String insideRoot = new File(themeRoot, "missing.css").getAbsolutePath();
    assertThrows(
        java.io.FileNotFoundException.class,
        () -> invokeLoadFile(p, insideRoot),
        "Validation must pass (no IllegalArgumentException); the FileInputStream"
            + " then throws FileNotFoundException because the file is absent.");
  }

  // ====================================================================
  // End-to-end: CSS payload containing @import with traversal
  // ====================================================================

  @Test
  @DisplayName(
      "End-to-end: a CSS @import('../../../etc/passwd') payload is rejected before any"
          + " File construction (the actual alert scenario)")
  void testCssImportTraversalPayloadIsRejected() throws Exception {
    // The realistic attack vector: a user-supplied CSS file contains
    //   @import url('../../../etc/passwd');
    // PSURLConverter.getFileSystemPathForCss converts that to a filesystem
    // path under themeRoot. The parser's updateImports flow then calls
    // fileExists(importPath) and loadFileFromDisk(importPath). Both must
    // throw IllegalArgumentException BEFORE the resolver runs.
    //
    // We simulate the post-conversion path here (the converter's behavior
    // is out of scope for this test) by passing a path that resolves
    // outside the theme root.
    PSCSSParser p = parser();
    String resolved = new File(themeRoot, "../../../etc/passwd").getAbsolutePath();
    assertThrows(IllegalArgumentException.class, () -> invokeFileExists(p, resolved));
    assertThrows(IllegalArgumentException.class, () -> invokeLoadFile(p, resolved));
    assertThrows(
        IllegalArgumentException.class,
        () -> invokeSaveFile(p, new StringBuffer("/* should never write */"), resolved));
  }
}
