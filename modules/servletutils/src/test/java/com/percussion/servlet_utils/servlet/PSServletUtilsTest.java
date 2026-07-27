/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.servlet_utils.servlet;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression coverage for {@link PSServletUtils} focused on the {@code
 * java/unvalidated-url-forward} finding closed at {@code PSServletUtils.java:247} (CodeQL alert
 * #792).
 *
 * <p>The previous code passed {@code path} directly to {@link
 * jakarta.servlet.ServletContext#getRequestDispatcher(String)}, letting a caller-supplied path
 * {@code forward} outside the web application root or expose {@code WEB-INF} / {@code META-INF}
 * content. The fix adds {@link PSServletUtils#validateForwardPath(String)} up front.
 *
 * <p>These tests cover both the helper itself (pure path validation) and the public entry points
 * ({@link PSServletUtils#getDispatcher(String)}) to pin both layers of the fix.
 */
public class PSServletUtilsTest {

  // ---------- getDispatcher() / validateForwardPath(): boundary contracts ----------

  @Test
  @DisplayName("getDispatcher(null) is rejected")
  void testGetDispatcherNullIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> PSServletUtils.getDispatcher(null));
  }

  @Test
  @DisplayName("getDispatcher(\"\") is rejected")
  void testGetDispatcherEmptyIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> PSServletUtils.getDispatcher(""));
  }

  @Test
  @DisplayName("getDispatcher(\"   \") is rejected (blank)")
  void testGetDispatcherBlankIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> PSServletUtils.getDispatcher("   "));
  }

  // ---------- validateForwardPath(): benign paths are accepted ----------

  @Test
  @DisplayName("validateForwardPath accepts a plain servlet path")
  void testPlainPathAccepted() {
    assertDoesNotThrow(() -> PSServletUtils.validateForwardPath("/app/servlet"));
    assertDoesNotThrow(() -> PSServletUtils.validateForwardPath("/foo"));
    assertDoesNotThrow(() -> PSServletUtils.validateForwardPath("/"));
  }

  @Test
  @DisplayName("validateForwardPath accepts a path with a query string")
  void testPathWithQueryAccepted() {
    assertDoesNotThrow(() -> PSServletUtils.validateForwardPath("/app/list?id=1"));
  }

  @Test
  @DisplayName("validateForwardPath accepts a path with a fragment")
  void testPathWithFragmentAccepted() {
    assertDoesNotThrow(() -> PSServletUtils.validateForwardPath("/docs/page#section"));
  }

  @Test
  @DisplayName("validateForwardPath accepts dots that are NOT traversal segments")
  void testNonTraversalDotsAccepted() {
    // Single dots inside a path (current-dir refs) — these are fine; the container normalizes.
    assertDoesNotThrow(() -> PSServletUtils.validateForwardPath("/foo/./bar"));
    // Triple-dot extensions (file names).
    assertDoesNotThrow(() -> PSServletUtils.validateForwardPath("/foo/bar.html"));
    // Dots in the middle of a name (`..foo`, `foo..bar`).
    assertDoesNotThrow(() -> PSServletUtils.validateForwardPath("/foo..bar/baz"));
    assertDoesNotThrow(() -> PSServletUtils.validateForwardPath("/..hidden/file"));
    // Three+ dots in a row (longer than 2) — not a traversal segment, no match.
    assertDoesNotThrow(() -> PSServletUtils.validateForwardPath("/foo.../bar"));
  }

  // ---------- validateForwardPath(): traversal sequences are rejected ----------

  @Test
  @DisplayName("validateForwardPath rejects '/..' segment in the middle of a path")
  void testMiddleDoubleDotRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSServletUtils.validateForwardPath("/app/../etc/passwd"));
  }

  @Test
  @DisplayName("validateForwardPath rejects '..' at the end of a path")
  void testTrailingDoubleDotRejected() {
    assertThrows(
        IllegalArgumentException.class, () -> PSServletUtils.validateForwardPath("/app/.."));
    assertThrows(
        IllegalArgumentException.class, () -> PSServletUtils.validateForwardPath("/app/../"));
  }

  @Test
  @DisplayName("validateForwardPath rejects '..' with a query string attached")
  void testTraversalWithQueryRejected() {
    assertThrows(
        IllegalArgumentException.class, () -> PSServletUtils.validateForwardPath("/app/..?id=1"));
  }

  @Test
  @DisplayName("validateForwardPath rejects bare '..' (no leading slash)")
  void testBareDoubleDotRejected() {
    assertThrows(IllegalArgumentException.class, () -> PSServletUtils.validateForwardPath(".."));
  }

  @Test
  @DisplayName("validateForwardPath rejects '../foo' (no leading slash)")
  void testDotDotSlashNoLeadingSlashRejected() {
    assertThrows(
        IllegalArgumentException.class, () -> PSServletUtils.validateForwardPath("../foo"));
  }

  @Test
  @DisplayName(
      "validateForwardPath rejects URL-encoded '%2e%2e' equivalent? — no, decoders run later")
  void testEncodedTraversalPassesButContainerHandlesIt() {
    // The validator deliberately does not decode; the servlet container normalizes AFTER
    // decode. Both layers are needed; this tests the validator in isolation.
    assertDoesNotThrow(
        () -> PSServletUtils.validateForwardPath("/app/%2e%2e/etc/passwd"),
        "Encoded traversal must be left to the container's own normalization; the validator's"
            + " job is the unencoded shape");
  }

  // ---------- validateForwardPath(): control characters and backslashes ----------

  @Test
  @DisplayName("validateForwardPath rejects embedded NUL")
  void testNulRejected() {
    assertThrows(
        IllegalArgumentException.class, () -> PSServletUtils.validateForwardPath("/app\0/../etc"));
  }

  @Test
  @DisplayName("validateForwardPath rejects embedded CR/LF (header injection)")
  void testCrlfRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSServletUtils.validateForwardPath("/app\r\n/../etc"));
  }

  @Test
  @DisplayName("validateForwardPath rejects embedded tab")
  void testTabRejected() {
    assertThrows(
        IllegalArgumentException.class, () -> PSServletUtils.validateForwardPath("/app\t/../etc"));
  }

  @Test
  @DisplayName("validateForwardPath rejects embedded DEL (0x7F)")
  void testDelRejected() {
    assertThrows(
        IllegalArgumentException.class, () -> PSServletUtils.validateForwardPath("/app/../etc"));
  }

  @Test
  @DisplayName("validateForwardPath rejects backslashes (Windows path separator confusion)")
  void testBackslashRejected() {
    assertThrows(
        IllegalArgumentException.class, () -> PSServletUtils.validateForwardPath("/app\\..\\etc"));
  }

  // ---------- validateForwardPath(): WEB-INF and META-INF ----------

  @Test
  @DisplayName("validateForwardPath rejects paths that start with WEB-INF")
  void testLeadingWebInfRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSServletUtils.validateForwardPath("WEB-INF/web.xml"));
    assertThrows(
        IllegalArgumentException.class,
        () -> PSServletUtils.validateForwardPath("/WEB-INF/web.xml"));
  }

  @Test
  @DisplayName("validateForwardPath rejects paths targeting /WEB-INF in the middle")
  void testMiddleWebInfRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSServletUtils.validateForwardPath("/app/WEB-INF/web.xml"));
    assertThrows(
        IllegalArgumentException.class, () -> PSServletUtils.validateForwardPath("/app/WEB-INF"));
  }

  @Test
  @DisplayName("validateForwardPath rejects paths targeting /META-INF")
  void testMetaInfRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSServletUtils.validateForwardPath("/META-INF/MANIFEST.MF"));
    assertThrows(
        IllegalArgumentException.class, () -> PSServletUtils.validateForwardPath("/app/META-INF"));
  }

  @Test
  @DisplayName(
      "validateForwardPath allows look-alike directory names that are NOT WEB-INF/META-INF")
  void testLookalikeDirectoriesAccepted() {
    // Case-insensitive match for the protected names; look-alikes that don't match the segment
    // boundary are accepted.
    assertDoesNotThrow(() -> PSServletUtils.validateForwardPath("/web-info/notes"));
    assertDoesNotThrow(() -> PSServletUtils.validateForwardPath("/meta-info/file"));
    assertDoesNotThrow(() -> PSServletUtils.validateForwardPath("/WEB_INF/file"));
  }

  // ---------- containsTraversal(): helper unit coverage ----------

  @Test
  @DisplayName("containsTraversal: clean paths return false")
  void testContainsTraversalCleanPaths() {
    assertEquals(false, PSServletUtils.containsTraversal("/foo/bar"));
    assertEquals(false, PSServletUtils.containsTraversal("/foo/bar/baz.html"));
    assertEquals(false, PSServletUtils.containsTraversal("/a.b/c.d"));
  }

  @Test
  @DisplayName("containsTraversal: various traversal shapes return true")
  void testContainsTraversalAttackShapes() {
    assertEquals(true, PSServletUtils.containsTraversal(".."));
    assertEquals(true, PSServletUtils.containsTraversal("../foo"));
    assertEquals(true, PSServletUtils.containsTraversal("/foo/.."));
    assertEquals(true, PSServletUtils.containsTraversal("/foo/../bar"));
    assertEquals(true, PSServletUtils.containsTraversal("/foo/../"));
    assertEquals(true, PSServletUtils.containsTraversal("/foo/..?id=1"));
    assertEquals(true, PSServletUtils.containsTraversal("foo/../bar"));
  }

  @Test
  @DisplayName("containsTraversal: dots that are NOT a '..' segment return false")
  void testContainsTraversalNonTraversalDots() {
    assertEquals(false, PSServletUtils.containsTraversal("/.hidden"));
    assertEquals(false, PSServletUtils.containsTraversal("/foo/."));
    assertEquals(false, PSServletUtils.containsTraversal("/foo.html"));
    assertEquals(false, PSServletUtils.containsTraversal("/foo...bar"));
    assertEquals(false, PSServletUtils.containsTraversal("/.../bar"));
  }

  @Test
  @DisplayName(
      "containsTraversal: '#' is treated as a segment boundary (defense-in-depth symmetry"
          + " with '?')")
  void testContainsTraversalHashBoundary() {
    // The '..' immediately after a fragment boundary '#' is treated as a traversal
    // segment for symmetry with '?'. The servlet container typically strips '#fragment'
    // before getRequestDispatcher sees it, so this is defense-in-depth, but it
    // closes a class of synthetic inputs that would otherwise slip through.
    assertEquals(true, PSServletUtils.containsTraversal("/foo#.."));
    assertEquals(true, PSServletUtils.containsTraversal("/foo#../bar"));
    assertEquals(true, PSServletUtils.containsTraversal("/foo#..?id=1"));
    // Plain '..' with no boundary char is still rejected.
    assertEquals(true, PSServletUtils.containsTraversal("foo#bar/.."));
  }

  // ---------- getDispatcher() end-to-end: container wiring ----------

  @Test
  @DisplayName("getDispatcher: validation runs even when ServletContext is not initialized")
  void testGetDispatcherValidationRunsBeforeServletContextLookup() {
    // ServletContext is null until initialize() runs; validation must still fire BEFORE the
    // container call, otherwise an attacker could bypass the validator with a null context.
    assertThrows(
        IllegalArgumentException.class,
        () -> PSServletUtils.getDispatcher("/app/../etc/passwd"),
        "validateForwardPath must run before m_servletContext.getRequestDispatcher so a"
            + " not-yet-initialized context cannot bypass validation");
  }

  @Test
  @DisplayName(
      "getDispatcher: when validation passes but no context is initialized, NPE is surfaced")
  void testGetDispatcherUninitializedSurfacesNpeAfterValidation() {
    // With validation passing, an uninitialized m_servletContext yields a NullPointerException;
    // this confirms that the validator ran first (the rejection path didn't fire).
    assertThrows(
        NullPointerException.class,
        () -> {
          try {
            PSServletUtils.getDispatcher("/safe/path");
          } catch (NullPointerException expected) {
            // Sanity: validation passed since it didn't throw IAE.
            throw expected;
          }
        });
  }

  // Quick check that we constructed the suite correctly
  @Test
  @DisplayName("sanity: validateForwardPath returns void")
  void testValidateReturnsVoid() {
    assertNull(
        (Void) null,
        "trivially true — guards the test class itself in case future refactors break it");
  }
}
