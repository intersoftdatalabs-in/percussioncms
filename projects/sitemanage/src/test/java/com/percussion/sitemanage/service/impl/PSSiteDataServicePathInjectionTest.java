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
package com.percussion.sitemanage.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.security.io.PSPathInjectionGuard;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

/**
 * Regression tests for {@link PSSiteDataService} focused on the {@code java/path-injection} finding
 * closed at {@code PSSiteDataService.java:503} (CodeQL alert #1058, T043).
 *
 * <p>The pre-fix code built File paths from {@code oldSiteName} and {@code newSiteName} (both
 * user-supplied via {@code updateSiteProperties(PSSiteProperties)}) without validating that the
 * names were safe single path segments. A site name like {@code ../../../etc/passwd} would escape
 * the cache directory and reach File construction with attacker-controlled traversal sequences.
 *
 * <p>The fix adds {@link PSPathInjectionGuard#requireSafeFileName} on both names BEFORE any File
 * construction. To invoke the private {@code updateThumbnailCache} without the 21-arg public
 * constructor we use {@link Unsafe#allocateInstance} (which skips constructors and field
 * initializers), then reflectively call the method. Because the validator runs FIRST in the
 * post-fix code, malicious payloads surface as {@link IllegalArgumentException} from the validator,
 * not as a later {@link NullPointerException} from the un-initialized {@code PSServer.getRxDir()}
 * call.
 */
public class PSSiteDataServicePathInjectionTest {

  private static final String METHOD_NAME = "updateThumbnailCache";

  private static final Unsafe UNSAFE;
  private static final Method UPDATE_METHOD;

  static {
    try {
      Field f = Unsafe.class.getDeclaredField("theUnsafe");
      f.setAccessible(true);
      UNSAFE = (Unsafe) f.get(null);
      UPDATE_METHOD =
          PSSiteDataService.class.getDeclaredMethod(METHOD_NAME, String.class, String.class);
      UPDATE_METHOD.setAccessible(true);
    } catch (ReflectiveOperationException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  /**
   * Reflectively invokes the private {@code updateThumbnailCache} on an uninitialized instance
   * (skipping the 21-arg public constructor). The validator must run BEFORE any PSServer.getRxDir()
   * call, so traversal payloads must surface as IllegalArgumentException.
   */
  private static void invokeUpdateThumbnailCache(String oldSiteName, String newSiteName)
      throws Exception {
    Object instance = UNSAFE.allocateInstance(PSSiteDataService.class);
    try {
      UPDATE_METHOD.invoke(instance, oldSiteName, newSiteName);
    } catch (InvocationTargetException ite) {
      Throwable cause = ite.getCause();
      if (cause instanceof Exception) throw (Exception) cause;
      if (cause instanceof Error) throw (Error) cause;
      throw ite;
    }
  }

  // ---------- Malicious inputs that MUST be rejected by the validator ----------

  @Test
  @DisplayName(
      "updateThumbnailCache rejects a '..' traversal payload (oldSiteName) "
          + "before any file I/O — CodeQL java/path-injection #1058")
  void testRejectsTraversalInOldSiteName() throws Exception {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> invokeUpdateThumbnailCache("../../../etc/passwd", "goodSite"),
            "A '..' traversal payload must be rejected by PSPathInjectionGuard before any"
                + " File construction. Pre-fix: NPE from PSServer.getRxDir(). Post-fix:"
                + " IllegalArgumentException from the validator (run before PSServer is"
                + " consulted).");
    assertTrue(
        ex.getMessage() != null && ex.getMessage().toLowerCase().contains("path"),
        "Validator message should mention 'path'; got: " + ex.getMessage());
  }

  @Test
  @DisplayName(
      "updateThumbnailCache rejects a '..' traversal payload (newSiteName) before"
          + " any file I/O")
  void testRejectsTraversalInNewSiteName() throws Exception {
    assertThrows(
        IllegalArgumentException.class, () -> invokeUpdateThumbnailCache("goodSite", "../escape"));
  }

  @Test
  @DisplayName("updateThumbnailCache rejects a forward-slash embedded name")
  void testRejectsForwardSlashInSiteName() throws Exception {
    assertThrows(
        IllegalArgumentException.class, () -> invokeUpdateThumbnailCache("goodSite", "foo/bar"));
    assertThrows(
        IllegalArgumentException.class, () -> invokeUpdateThumbnailCache("foo/bar", "goodSite"));
  }

  @Test
  @DisplayName("updateThumbnailCache rejects a backslash embedded name")
  void testRejectsBackslashInSiteName() throws Exception {
    assertThrows(
        IllegalArgumentException.class, () -> invokeUpdateThumbnailCache("goodSite", "foo\\bar"));
  }

  @Test
  @DisplayName("updateThumbnailCache rejects an absolute path payload")
  void testRejectsAbsolutePathInSiteName() throws Exception {
    assertThrows(
        IllegalArgumentException.class,
        () -> invokeUpdateThumbnailCache("/etc/passwd", "goodSite"));
  }

  @Test
  @DisplayName("updateThumbnailCache rejects empty / null site names")
  void testRejectsNullAndEmptySiteName() throws Exception {
    assertThrows(
        IllegalArgumentException.class, () -> invokeUpdateThumbnailCache(null, "goodSite"));
    assertThrows(
        IllegalArgumentException.class, () -> invokeUpdateThumbnailCache("goodSite", null));
    assertThrows(IllegalArgumentException.class, () -> invokeUpdateThumbnailCache("", "goodSite"));
    assertThrows(IllegalArgumentException.class, () -> invokeUpdateThumbnailCache("goodSite", ""));
  }

  @Test
  @DisplayName("updateThumbnailCache rejects a NUL byte in a site name")
  void testRejectsNulInSiteName() throws Exception {
    assertThrows(
        IllegalArgumentException.class, () -> invokeUpdateThumbnailCache("good\0site", "x"));
    assertThrows(
        IllegalArgumentException.class, () -> invokeUpdateThumbnailCache("x", "good\0site"));
  }

  @Test
  @DisplayName("updateThumbnailCache rejects a single-segment '.' or '..'")
  void testRejectsSingleDotOrDotDot() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> invokeUpdateThumbnailCache(".", "goodSite"));
    assertThrows(
        IllegalArgumentException.class, () -> invokeUpdateThumbnailCache("..", "goodSite"));
    assertThrows(IllegalArgumentException.class, () -> invokeUpdateThumbnailCache("goodSite", "."));
    assertThrows(
        IllegalArgumentException.class, () -> invokeUpdateThumbnailCache("goodSite", ".."));
  }

  // ---------- Benign inputs that MUST NOT throw at the validator ----------

  @Test
  @DisplayName("Sanity: PSPathInjectionGuard.requireSafeFileName accepts well-formed site names")
  void testAcceptsSiteNameWithPunctuation() {
    // Direct helper test — the validator accepts legitimate single-segment
    // site names with allowed punctuation. No reflection needed.
    org.junit.jupiter.api.Assertions.assertDoesNotThrow(
        () -> PSPathInjectionGuard.requireSafeFileName("MySite"));
    org.junit.jupiter.api.Assertions.assertDoesNotThrow(
        () -> PSPathInjectionGuard.requireSafeFileName("my-site_v1.0"));
  }
}
