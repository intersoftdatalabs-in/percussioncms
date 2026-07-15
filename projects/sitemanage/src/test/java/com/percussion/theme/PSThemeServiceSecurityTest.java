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

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.percussion.share.service.IPSDataService.DataServiceLoadException;
import com.percussion.share.service.IPSDataService.DataServiceNotFoundException;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.theme.service.impl.PSThemeService;
import org.junit.jupiter.api.Test;

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
}
