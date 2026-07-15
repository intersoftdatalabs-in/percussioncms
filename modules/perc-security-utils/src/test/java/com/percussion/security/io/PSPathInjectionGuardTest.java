/*
 * Copyright 1999-2025 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.percussion.security.io;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for {@link PSPathInjectionGuard} (CodeQL {@code java/path-injection}, T043,
 * US3).
 *
 * <p>The pre-fix pattern across the 58 T043 sites in projects/sitemanage and system/ was to pass
 * user-supplied strings directly to {@code new File(baseDir, userInput)} without any traversal
 * check. The post-fix pattern is to call {@link PSPathInjectionGuard#requireSafeFileName} on the
 * user-supplied string and/or {@link PSPathInjectionGuard#requireUnderBase} to verify the resolved
 * path is still within the base directory.
 *
 * <p><strong>Fail-then-pass coverage (Constitution III).</strong> The pre-fix code accepted every
 * payload below; the post-fix code rejects each one with an {@link IllegalArgumentException} (or,
 * for the canonical-path test, refuses to return a path outside the base directory).
 */
@DisplayName("PSPathInjectionGuard — path-traversal (CWE-22/CWE-23) regression tests")
class PSPathInjectionGuardTest {

  @Nested
  @DisplayName("requireSafeFileName accepts safe inputs")
  class SafeInputs {

    @Test
    @DisplayName("alphanumeric filename is accepted")
    void testAlphanumeric() {
      assertEquals("abc123", PSPathInjectionGuard.requireSafeFileName("abc123"));
    }

    @Test
    @DisplayName("filename with dot is accepted (e.g. file.txt)")
    void testDotInFilename() {
      assertEquals("file.txt", PSPathInjectionGuard.requireSafeFileName("file.txt"));
    }

    @Test
    @DisplayName("filename with multiple dots is accepted (e.g. archive.tar.gz)")
    void testMultipleDots() {
      assertEquals("archive.tar.gz", PSPathInjectionGuard.requireSafeFileName("archive.tar.gz"));
    }

    @Test
    @DisplayName("filename with hyphen, underscore, space is accepted")
    void testSpecialCharsInFilename() {
      assertEquals(
          "my file-name_v2.txt", PSPathInjectionGuard.requireSafeFileName("my file-name_v2.txt"));
    }
  }

  @Nested
  @DisplayName("requireSafeFileName rejects traversal payloads")
  class RejectedInputs {

    @Test
    @DisplayName("parent-traversal '..' is rejected")
    void testDoubleDot() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PSPathInjectionGuard.requireSafeFileName(".."),
          "name '..' must be rejected");
    }

    @Test
    @DisplayName("'.\\etc\\passwd' is rejected (backslash)")
    void testParentTraversal() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PSPathInjectionGuard.requireSafeFileName("..\\etc\\passwd"),
          "'..\\etc\\passwd' must be rejected");
    }

    @Test
    @DisplayName("'/etc/passwd' absolute path is rejected")
    void testAbsolutePath() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PSPathInjectionGuard.requireSafeFileName("/etc/passwd"),
          "absolute paths must be rejected");
    }

    @Test
    @DisplayName("'a/b/c' multi-segment is rejected (requireSafeFileName is single-segment)")
    void testMultiSegment() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PSPathInjectionGuard.requireSafeFileName("a/b/c"),
          "multi-segment paths must be rejected (callers wanting "
              + "multi-segment need requireUnderBase instead)");
    }

    @Test
    @DisplayName("'file..txt' is rejected (literal '..' substring is forbidden by contract)")
    void testLiteralDoubleDotInFilename() {
      // Per the round-3 review fix: requireSafeFileName explicitly
      // rejects ".." and "." (the only reserved path-segment names). A
      // filename like "file..txt" is rejected because the literal ".."
      // is treated as a traversal marker by contract (callers wanting
      // a literal ".." in a filename should use requireUnderBase with
      // an explicit base directory).
      assertThrows(
          IllegalArgumentException.class,
          () -> PSPathInjectionGuard.requireSafeFileName("file..txt"),
          "literal '..' substring in filename must be rejected per contract");
    }

    @Test
    @DisplayName("NUL byte in name is rejected")
    void testNulByte() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PSPathInjectionGuard.requireSafeFileName("safe\u0000.txt"),
          "NUL bytes must be rejected");
    }

    @Test
    @DisplayName("null name is rejected")
    void testNull() {
      assertThrows(
          IllegalArgumentException.class, () -> PSPathInjectionGuard.requireSafeFileName(null));
    }

    @Test
    @DisplayName("empty name is rejected")
    void testEmpty() {
      assertThrows(
          IllegalArgumentException.class, () -> PSPathInjectionGuard.requireSafeFileName(""));
    }

    @Test
    @DisplayName("'.' (current-dir marker) is rejected")
    void testDot() {
      assertThrows(
          IllegalArgumentException.class, () -> PSPathInjectionGuard.requireSafeFileName("."));
    }
  }

  @Nested
  @DisplayName("requireUnderBase resolves safely under a real base directory")
  class RequireUnderBaseTests {

    private File m_tmpRoot;

    @org.junit.jupiter.api.BeforeEach
    void setUp() throws IOException {
      m_tmpRoot = Files.createTempDirectory("pspathinj-").toFile();
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
      if (m_tmpRoot != null && m_tmpRoot.exists()) {
        for (File f : m_tmpRoot.listFiles()) {
          f.delete();
        }
        m_tmpRoot.delete();
      }
    }

    @Test
    @DisplayName("a multi-segment relative path resolves under the base")
    void testMultiSegmentPath() throws IOException {
      // The canonical-path check is the authoritative test, so any
      // subdir/file.txt that resolves under the base is allowed. The
      // previous round used requireSafeFileName here which rejected
      // the "/" separator; this test verifies the more permissive
      // contract: requireUnderBase accepts multi-segment paths and
      // relies on the canonical-path check to reject traversal.
      File result = PSPathInjectionGuard.requireUnderBase(m_tmpRoot, "subdir/inner/file.txt");
      assertNotNull(result);
      String resolvedCanonical = result.getCanonicalPath();
      String baseCanonical = m_tmpRoot.getCanonicalPath();
      String baseWithSep =
          baseCanonical.endsWith(File.separator) ? baseCanonical : baseCanonical + File.separator;
      assertTrue(
          resolvedCanonical.equals(baseCanonical) || resolvedCanonical.startsWith(baseWithSep),
          "resolved canonical path '"
              + resolvedCanonical
              + "' must be under baseDir '"
              + baseCanonical
              + "'");
    }

    @Test
    @DisplayName("'subdir/../../../escape' is rejected (canonical-path check catches it)")
    void testSubdirEscape() {
      // 'subdir/../escape.txt' is actually under the baseDir (the ..
      // cancels subdir) so the canonical-path check accepts it. The
      // path-traversal intent is to escape the base, which requires
      // enough .. segments to go ABOVE the base. Three .. segments
      // are needed: subdir/../../../escape.txt resolves to
      // ../../escape.txt which is above the base.
      assertThrows(
          IllegalArgumentException.class,
          () -> PSPathInjectionGuard.requireUnderBase(m_tmpRoot, "subdir/../../../escape.txt"),
          "subdir/../../../escape.txt must be rejected (resolves outside baseDir)");
    }

    @Test
    @DisplayName("'../escape' is rejected (would resolve outside base)")
    void testParentTraversal() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PSPathInjectionGuard.requireUnderBase(m_tmpRoot, "../escape.txt"),
          "parent-traversal must be rejected");
    }

    @Test
    @DisplayName("nested traversal 'a/../../escape' is rejected")
    void testNestedTraversal() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PSPathInjectionGuard.requireUnderBase(m_tmpRoot, "a/../../escape.txt"));
    }

    @Test
    @DisplayName("absolute path is rejected")
    void testAbsoluteEscape() {
      // On Unix, "/etc/passwd" is a traversal target. On Windows,
      // "C:\\Windows\\System32\\config\\SAM" is the classic equivalent.
      // We test with the actual platform's "forbidden outside the
      // tempdir" path so the canonical-path check is meaningful on
      // every OS.
      String os = System.getProperty("os.name").toLowerCase();
      String traversal = os.contains("win") ? "C:\\Windows\\System32\\config\\SAM" : "/etc/passwd";
      assertThrows(
          IllegalArgumentException.class,
          () -> PSPathInjectionGuard.requireUnderBase(m_tmpRoot, traversal),
          "absolute path '" + traversal + "' must be rejected");
    }

    @Test
    @DisplayName("baseDir must exist")
    void testNonExistentBase() {
      File nonExistent = new File(m_tmpRoot, "does-not-exist");
      assertThrows(
          IllegalArgumentException.class,
          () -> PSPathInjectionGuard.requireUnderBase(nonExistent, "file.txt"));
    }

    @Test
    @DisplayName("null baseDir is rejected")
    void testNullBase() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PSPathInjectionGuard.requireUnderBase((File) null, "file.txt"));
    }
  }

  @Nested
  @DisplayName("containsForbiddenCharacters boolean helper")
  class BooleanHelper {

    @Test
    @DisplayName("safe inputs return false")
    void testSafeInputs() {
      assertFalse(PSPathInjectionGuard.containsForbiddenCharacters("file.txt"));
      assertFalse(PSPathInjectionGuard.containsForbiddenCharacters(""));
      assertFalse(PSPathInjectionGuard.containsForbiddenCharacters(null));
      assertFalse(PSPathInjectionGuard.containsForbiddenCharacters("a.b.c"));
      // Literal ".." in a filename (e.g. "file..txt") is no longer
      // flagged by the boolean helper per the round-3 review. Only
      // separator and NUL-byte checks are done at the boolean level;
      // the segment-aware check is in requireSafeFileName.
      assertFalse(PSPathInjectionGuard.containsForbiddenCharacters("file..txt"));
      assertFalse(PSPathInjectionGuard.containsForbiddenCharacters("archive..tar.gz"));
    }

    @Test
    @DisplayName("traversal payloads return true")
    void testForbiddenInputs() {
      assertTrue(PSPathInjectionGuard.containsForbiddenCharacters("/etc/passwd"));
      assertTrue(PSPathInjectionGuard.containsForbiddenCharacters("../etc/passwd"));
      assertTrue(PSPathInjectionGuard.containsForbiddenCharacters("a\\b"));
      assertTrue(PSPathInjectionGuard.containsForbiddenCharacters("a/b"));
      assertTrue(PSPathInjectionGuard.containsForbiddenCharacters("safe\u0000.txt"));
    }
  }

  @Test
  @DisplayName("Smoke: requireSafeFileName is a no-op on a clean input (no throw)")
  void testSmokeNoThrow() {
    assertDoesNotThrow(() -> PSPathInjectionGuard.requireSafeFileName("clean-input.txt"));
  }
}
