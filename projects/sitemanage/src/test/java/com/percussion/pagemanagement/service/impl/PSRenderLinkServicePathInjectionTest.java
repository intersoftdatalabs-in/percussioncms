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
package com.percussion.pagemanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.security.io.PSPathInjectionGuard;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Behavioral path-injection tests for the region-CSS containment check used by {@link
 * PSRenderLinkService#renderLinkThemeRegionCSS}: after resolving {@code regionCssPath}, the service
 * calls {@link PSPathInjectionGuard#requireUnderBase}(themesRoot, regionCssPath) before treating
 * the file as present.
 *
 * <p>These tests exercise that exact call-site contract with a controlled themes root (no full
 * Spring / resource-definition stack).
 */
public class PSRenderLinkServicePathInjectionTest {

  @Test
  @DisplayName("region CSS path under themes root is accepted")
  void legitimateRegionCssPathAccepted(@TempDir Path temp) throws Exception {
    Path themesRoot = temp.resolve("themes");
    Path css = themesRoot.resolve("mytheme").resolve("perc").resolve("perc_region.css");
    Files.createDirectories(css.getParent());
    Files.writeString(css, "/* css */");

    File resolved =
        assertDoesNotThrow(
            () ->
                PSPathInjectionGuard.requireUnderBase(
                    themesRoot.toFile(), "mytheme/perc/perc_region.css"));
    assertTrue(Files.isSameFile(css, resolved.toPath()));
    assertTrue(resolved.exists() && resolved.length() > 0);
  }

  @Test
  @DisplayName("region CSS path with parent traversal is rejected")
  void traversalRegionCssPathRejected(@TempDir Path temp) throws Exception {
    Path themesRoot = temp.resolve("themes");
    Files.createDirectories(themesRoot);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            PSPathInjectionGuard.requireUnderBase(
                themesRoot.toFile(), "mytheme/../../../etc/passwd"),
        "requireUnderBase must reject regionCssPath that escapes themes root");
  }

  @Test
  @DisplayName("absolute path outside themes root is rejected")
  void absoluteOutsideThemesRootRejected(@TempDir Path temp) throws Exception {
    Path themesRoot = temp.resolve("themes");
    Files.createDirectories(themesRoot);
    Path outside = temp.resolve("outside.txt");
    Files.writeString(outside, "x");

    assertThrows(
        IllegalArgumentException.class,
        () ->
            PSPathInjectionGuard.requireUnderBase(
                themesRoot.toFile(), outside.toAbsolutePath().toString()),
        "absolute path outside themes root must be rejected");
  }

  @Test
  @DisplayName("empty-length region CSS file is still contained (exists check is separate)")
  void emptyRegionCssStillContained(@TempDir Path temp) throws Exception {
    Path themesRoot = temp.resolve("themes");
    Path css = themesRoot.resolve("t").resolve("empty.css");
    Files.createDirectories(css.getParent());
    Files.writeString(css, "");

    File resolved = PSPathInjectionGuard.requireUnderBase(themesRoot.toFile(), "t/empty.css");
    assertTrue(resolved.exists());
    assertTrue(resolved.length() == 0);
    // Call-site then returns empty PSRenderLink when length == 0; containment already succeeded.
  }
}
