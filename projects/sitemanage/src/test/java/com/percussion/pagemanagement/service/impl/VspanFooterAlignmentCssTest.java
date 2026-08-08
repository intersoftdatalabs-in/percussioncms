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

package com.percussion.pagemanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Regression for GH-757 / GH-2352:
 *
 * <ul>
 *   <li>Published default theme {@code vspan_*} regions use {@code min-height} so sidebars grow
 *       with content (footer stays below) — GH-757.
 *   <li>Editor/preview decoration must <strong>not</strong> force the legacy fixed pixel grid with
 *       {@code !important} (or hard-coded px heights/widths) so responsive customer themes that set
 *       {@code height/width: auto} win in the CMS chrome — GH-2352.
 * </ul>
 */
class VspanFooterAlignmentCssTest {

  private static final Pattern VSPAN_BLOCK =
      Pattern.compile("\\.vspan_([2468])\\s*\\{([^}]*)\\}", Pattern.DOTALL);

  private static final Pattern HSPAN_BLOCK =
      Pattern.compile("\\.hspan_(2|8|10|12)\\s*\\{([^}]*)\\}", Pattern.DOTALL);

  /** Design floor heights for empty vspan regions (px), keyed by span index (default theme). */
  private static final Map<String, String> VSPAN_FLOOR_PX =
      Map.of("2", "120", "4", "240", "6", "360", "8", "480");

  private static final List<String> DECORATION_PATHS =
      List.of(
          "WebUI/war/css/perc_decoration.css",
          "WebUI/src/main/webapp/cm/css/perc_decoration.css",
          "WebUI/src/main/webapp/cm/app/css/legacy/perc_decoration.css");

  @Test
  void themeCssUsesMinHeightForVspanRegions() throws Exception {
    Path theme =
        resolveRepoRoot()
            .resolve(
                "system/cms/content/applications/rx_resources/ApplicationFiles/default_theme/theme.css");
    if (!Files.isRegularFile(theme)) {
      fail("expected default theme at " + theme.toAbsolutePath());
    }
    String css = Files.readString(theme, StandardCharsets.UTF_8);
    Matcher m = VSPAN_BLOCK.matcher(css);
    Map<String, Integer> counts = new LinkedHashMap<>();
    for (String k : VSPAN_FLOOR_PX.keySet()) {
      counts.put(k, 0);
    }
    int blocks = 0;
    while (m.find()) {
      blocks++;
      String span = m.group(1);
      String body = m.group(2);
      String floor = VSPAN_FLOOR_PX.get(span);
      assertTrue(
          Pattern.compile("min-height\\s*:\\s*" + floor + "px").matcher(body).find(),
          "theme .vspan_"
              + span
              + " must set min-height: "
              + floor
              + "px: "
              + body.replace('\n', ' '));
      // Fixed height would clip sidebar content into the footer on published pages.
      // Exclude min-height and max-height property names (lookbehind is not \w-).
      assertFalse(
          Pattern.compile("(?<!min-|max-)height\\s*:").matcher(body).find(),
          "theme .vspan_" + span + " must not set fixed height: " + body.replace('\n', ' '));
      counts.merge(span, 1, Integer::sum);
    }
    assertTrue(blocks >= 4, "expected at least one set of vspan_2/4/6/8 rules, found " + blocks);
    for (Map.Entry<String, Integer> e : counts.entrySet()) {
      assertTrue(
          e.getValue() >= 1,
          "expected at least one .vspan_" + e.getKey() + " rule, found " + e.getValue());
    }
  }

  @Test
  void decorationCssDoesNotForceFixedPixelGridWithImportant() throws Exception {
    Path root = resolveRepoRoot();
    for (String rel : DECORATION_PATHS) {
      Path cssPath = root.resolve(rel);
      if (!Files.isRegularFile(cssPath)) {
        fail("expected decoration CSS at " + cssPath.toAbsolutePath());
      }
      String css = Files.readString(cssPath, StandardCharsets.UTF_8);
      assertVspanAllowsResponsiveThemes(rel, css);
      assertHspanAllowsResponsiveThemes(rel, css);
    }
  }

  private static void assertVspanAllowsResponsiveThemes(String rel, String css) {
    Matcher m = VSPAN_BLOCK.matcher(css);
    Map<String, Integer> counts = new LinkedHashMap<>();
    for (String k : VSPAN_FLOOR_PX.keySet()) {
      counts.put(k, 0);
    }
    int blocks = 0;
    while (m.find()) {
      blocks++;
      String span = m.group(1);
      String body = m.group(2);
      // GH-2352: never lock height with !important over customer auto rules.
      assertFalse(
          Pattern.compile("(?<!min-|max-)height\\s*:[^;!]*!important").matcher(body).find(),
          rel
              + " .vspan_"
              + span
              + " must not force height with !important: "
              + body.replace('\n', ' '));
      assertFalse(
          Pattern.compile("min-height\\s*:\\s*0\\s*!important").matcher(body).find(),
          rel
              + " .vspan_"
              + span
              + " must not reset min-height: 0 !important (kills theme floors): "
              + body.replace('\n', ' '));
      // Prefer auto so decoration does not re-impose the legacy 120/240/360/480 grid.
      assertTrue(
          Pattern.compile("(?<!min-|max-)height\\s*:\\s*auto").matcher(body).find(),
          rel + " .vspan_" + span + " must use height: auto: " + body.replace('\n', ' '));
      // No fixed design-floor px height on decoration (theme owns min-height floors).
      String floor = VSPAN_FLOOR_PX.get(span);
      assertFalse(
          Pattern.compile("(?<!min-|max-)height\\s*:\\s*" + floor + "px").matcher(body).find(),
          rel
              + " .vspan_"
              + span
              + " must not set fixed height: "
              + floor
              + "px: "
              + body.replace('\n', ' '));
      counts.merge(span, 1, Integer::sum);
    }
    assertTrue(blocks >= 4, rel + ": expected at least vspan_2/4/6/8 rules, found " + blocks);
    for (Map.Entry<String, Integer> e : counts.entrySet()) {
      assertTrue(e.getValue() >= 1, rel + " .vspan_" + e.getKey() + " count=" + e.getValue());
    }
  }

  private static void assertHspanAllowsResponsiveThemes(String rel, String css) {
    Matcher m = HSPAN_BLOCK.matcher(css);
    Map<String, Integer> counts = new LinkedHashMap<>();
    for (String k : List.of("2", "8", "10", "12")) {
      counts.put(k, 0);
    }
    int blocks = 0;
    while (m.find()) {
      blocks++;
      String span = m.group(1);
      String body = m.group(2);
      assertFalse(
          Pattern.compile("width\\s*:[^;!]*!important").matcher(body).find(),
          rel
              + " .hspan_"
              + span
              + " must not force width with !important: "
              + body.replace('\n', ' '));
      assertTrue(
          Pattern.compile("width\\s*:\\s*auto").matcher(body).find(),
          rel + " .hspan_" + span + " must use width: auto: " + body.replace('\n', ' '));
      // Legacy fixed grid must not be reimposed (160/640/800/960).
      assertFalse(
          Pattern.compile("width\\s*:\\s*(160|640|800|960)px").matcher(body).find(),
          rel
              + " .hspan_"
              + span
              + " must not set fixed legacy grid width: "
              + body.replace('\n', ' '));
      counts.merge(span, 1, Integer::sum);
    }
    assertTrue(blocks >= 4, rel + ": expected at least hspan_2/8/10/12 rules, found " + blocks);
    for (Map.Entry<String, Integer> e : counts.entrySet()) {
      assertTrue(e.getValue() >= 1, rel + " .hspan_" + e.getKey() + " count=" + e.getValue());
    }
  }

  private static Path resolveRepoRoot() {
    Path cwd = Path.of("").toAbsolutePath().normalize();
    Path candidate = cwd.resolve("../..").normalize();
    if (Files.isDirectory(candidate.resolve("system"))
        && Files.isDirectory(candidate.resolve("WebUI"))) {
      return candidate;
    }
    if (Files.isDirectory(cwd.resolve("system")) && Files.isDirectory(cwd.resolve("WebUI"))) {
      return cwd;
    }
    fail(
        "could not resolve monorepo root (need system/ + WebUI/). Tried: "
            + candidate
            + " and "
            + cwd);
    return cwd; // unreachable
  }
}
