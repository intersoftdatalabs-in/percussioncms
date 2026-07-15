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

package com.percussion.pagemanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Regression for GH-757 / v8.1.7 PRs #763 and #767: published theme {@code vspan_*} regions use
 * {@code min-height} so sidebars grow with content (footer stays below), while editor decoration CSS
 * keeps fixed {@code height !important} and resets {@code min-height} so placeholders stay sized.
 */
class VspanFooterAlignmentCssTest {

  private static final Pattern VSPAN_BLOCK =
      Pattern.compile("\\.vspan_[2468]\\s*\\{[^}]*\\}", Pattern.DOTALL);

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
    int blocks = 0;
    while (m.find()) {
      blocks++;
      String block = m.group();
      assertTrue(
          block.contains("min-height"),
          "theme vspan block must use min-height: " + block.replace('\n', ' '));
      // Fixed height would clip sidebar content into the footer on published pages.
      assertFalse(
          Pattern.compile("(?<!min-)height\\s*:").matcher(block).find(),
          "theme vspan block must not set fixed height: " + block.replace('\n', ' '));
    }
    assertTrue(blocks >= 4, "expected at least one set of vspan_2/4/6/8 rules, found " + blocks);
  }

  @Test
  void decorationCssOverridesMinHeightWithFixedHeightImportant() throws Exception {
    Path root = resolveRepoRoot();
    for (String rel : DECORATION_PATHS) {
      Path cssPath = root.resolve(rel);
      if (!Files.isRegularFile(cssPath)) {
        fail("expected decoration CSS at " + cssPath.toAbsolutePath());
      }
      String css = Files.readString(cssPath, StandardCharsets.UTF_8);
      Matcher m = VSPAN_BLOCK.matcher(css);
      int blocks = 0;
      while (m.find()) {
        blocks++;
        String block = m.group();
        assertTrue(
            block.contains("!important"),
            rel + " vspan block must use !important overrides: " + block.replace('\n', ' '));
        assertTrue(
            Pattern.compile("height\\s*:\\s*\\d+px\\s*!important").matcher(block).find(),
            rel + " vspan block must fix height with !important: " + block.replace('\n', ' '));
        assertTrue(
            Pattern.compile("min-height\\s*:\\s*0\\s*!important").matcher(block).find(),
            rel + " vspan block must reset min-height with !important: " + block.replace('\n', ' '));
      }
      assertTrue(blocks >= 4, rel + ": expected vspan_2/4/6/8 rules, found " + blocks);
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
    return cwd;
  }
}
