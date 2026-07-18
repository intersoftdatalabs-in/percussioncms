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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.share.service.IPSDataService.DataServiceLoadException;
import com.percussion.share.service.IPSDataService.DataServiceNotFoundException;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.theme.service.impl.PSThemeService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Regression tests for the CWE-22 / CWE-23 path-injection alert on {@link PSThemeService}. Covers
 * GitHub code-scanning alert #1713 (java/path-injection, error severity, projects/sitemanage/).
 *
 * <p>Pre-fix behavior: {@code getThemeFolder(String themeName)} accepted any string and built
 * {@code new File(themesRoot, themeName)} without validation. A payload like {@code ../../etc}
 * would have resolved to a path outside the themes root.
 *
 * <p>Post-fix behavior: the theme name is validated via {@code
 * PSPathInjectionGuard.requireSafeFileName} at the trust boundary, rejecting null/empty, NUL bytes,
 * path separators, and {@code ..} segment markers.
 */
public class PSThemeServiceSecurityTest {

  /**
   * Closes alert #1713: a theme name with a parent-traversal payload must be rejected before it
   * reaches {@code new File(themesRoot, themeName)}.
   */
  @Test
  public void getThemeFolder_rejectsParentTraversalInThemeName() {
    PSThemeService svc = new PSThemeService();
    String payload = "../escape";
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          try {
            svc.find(payload);
          } catch (DataServiceLoadException
              | DataServiceNotFoundException
              | PSValidationException e) {
            // Expected: the lookup itself may fail with a
            // service-level exception (folder does not exist).
            // The point of this test is that the validation
            // layer rejects the name FIRST, before the lookup.
            throw new AssertionError(
                "find() must reject traversal payload at validation, not at lookup: " + e, e);
          }
        },
        "find() must reject a theme name that escapes themesRoot via parent traversal");
  }

  /** Sanity: a single-segment, safe theme name is accepted (does NOT throw IAE). */
  @Test
  public void getThemeFolder_acceptsSafeSingleSegmentName() {
    PSThemeService svc = new PSThemeService();
    String safe = "mytheme";
    try {
      svc.find(safe);
    } catch (IllegalArgumentException e) {
      throw new AssertionError("Safe single-segment name must not be rejected", e);
    } catch (Exception expected) {
      // DataServiceNotFoundException or similar for a non-existent
      // theme is fine - what matters is that the validation layer
      // does NOT throw IAE on a safe name.
    }
  }

  /** A theme name containing a forward slash is rejected (path separator). */
  @Test
  public void find_rejectsThemeNameWithForwardSlash() {
    PSThemeService svc = new PSThemeService();
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          try {
            svc.find("foo/bar");
          } catch (DataServiceLoadException
              | DataServiceNotFoundException
              | PSValidationException e) {
            throw new AssertionError(
                "find() must reject forward slash at validation, not at lookup: " + e, e);
          }
        },
        "find() must reject a theme name containing '/'");
  }

  /** A theme name containing a backslash is rejected (Windows separator). */
  @Test
  public void find_rejectsThemeNameWithBackslash() {
    PSThemeService svc = new PSThemeService();
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          try {
            svc.find("foo\\bar");
          } catch (DataServiceLoadException
              | DataServiceNotFoundException
              | PSValidationException e) {
            throw new AssertionError(
                "find() must reject backslash at validation, not at lookup: " + e, e);
          }
        },
        "find() must reject a theme name containing '\\'");
  }

  /** A theme name containing a NUL byte is rejected. */
  @Test
  public void find_rejectsThemeNameWithNulByte() {
    PSThemeService svc = new PSThemeService();
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          try {
            svc.find("foo\0bar");
          } catch (DataServiceLoadException
              | DataServiceNotFoundException
              | PSValidationException e) {
            throw new AssertionError(
                "find() must reject NUL byte at validation, not at lookup: " + e, e);
          }
        },
        "find() must reject a theme name containing a NUL byte");
  }

  /** create() must reject traversal in the new theme name (raw new File path closed). */
  @Test
  public void create_rejectsTraversalInNewThemeName() {
    PSThemeService svc = new PSThemeService();
    assertThrows(
        IllegalArgumentException.class,
        () -> svc.create("../evil", "existing"),
        "create() must reject parent traversal in newTheme");
  }

  /** create() must reject separators in the new theme name. */
  @Test
  public void create_rejectsSlashInNewThemeName() {
    PSThemeService svc = new PSThemeService();
    assertThrows(
        IllegalArgumentException.class,
        () -> svc.create("a/b", "existing"),
        "create() must reject '/' in newTheme");
  }

  /** Session segment sanitizer is the single source of truth for cache path + URL. */
  @Test
  public void safeSessionSegment_nullBlankAndSpecialChars() {
    assertEquals("pssession", PSThemeService.safeSessionSegment(null));
    assertEquals("pssession", PSThemeService.safeSessionSegment(""));
    assertEquals("pssession", PSThemeService.safeSessionSegment("   "));
    assertEquals("abc-123_X.y", PSThemeService.safeSessionSegment("abc-123_X.y"));
    assertEquals("a_b_c", PSThemeService.safeSessionSegment("a/b\\c"));
    assertEquals("sess__id", PSThemeService.safeSessionSegment("sess::id"));
  }

  /**
   * clearCacheRegionCSS must reject theme-name traversal before any delete under the temp root.
   */
  @Test
  public void clearCacheRegionCSS_rejectsTraversalThemeName() {
    PSThemeService svc = new PSThemeService();
    assertThrows(
        IllegalArgumentException.class,
        () -> svc.clearCacheRegionCSS("../evil", "template"),
        "clearCacheRegionCSS must reject parent traversal in theme");
  }

  /** clearCacheRegionCSS must reject separators in the theme name. */
  @Test
  public void clearCacheRegionCSS_rejectsSlashInThemeName() {
    PSThemeService svc = new PSThemeService();
    assertThrows(
        IllegalArgumentException.class,
        () -> svc.clearCacheRegionCSS("a/b", "template"),
        "clearCacheRegionCSS must reject '/' in theme");
  }

  /**
   * When temp root is missing, clearCache is a no-op (no exception). Ensures early-return path
   * does not NPE after the session-segment rewrite.
   */
  @Test
  public void clearCacheRegionCSS_missingTempRootIsNoOp(@TempDir Path tempDir) {
    PSThemeService svc = new PSThemeService();
    Path missing = tempDir.resolve("no-such-temp-root");
    svc.setThemesTempRootDirectory(missing.toAbsolutePath().toString());
    svc.clearCacheRegionCSS("mytheme", "template");
    assertFalse(Files.exists(missing));
  }

  /**
   * clearCache deletes only the sanitized session directory under the configured temp root
   * (behavioral containment: sibling dirs outside session are preserved).
   */
  @Test
  public void clearCacheRegionCSS_deletesOnlySessionDir(@TempDir Path tempDir) throws Exception {
    PSThemeService svc = new PSThemeService();
    Path tempRoot = tempDir.resolve("themes-temp");
    Files.createDirectories(tempRoot);
    // No request context → session segment is "pssession"
    Path sessionDir = tempRoot.resolve("pssession");
    Path sibling = tempRoot.resolve("other-session");
    Files.createDirectories(sessionDir.resolve("mytheme"));
    Files.createDirectories(sibling);
    svc.setThemesTempRootDirectory(tempRoot.toAbsolutePath().toString());

    svc.clearCacheRegionCSS("mytheme", "template");

    assertFalse(Files.exists(sessionDir), "session dir must be removed");
    assertTrue(Files.exists(sibling), "sibling session dirs must not be deleted");
  }
}
