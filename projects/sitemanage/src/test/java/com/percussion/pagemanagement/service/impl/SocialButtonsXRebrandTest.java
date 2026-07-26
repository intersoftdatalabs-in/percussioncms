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
import org.junit.jupiter.api.Test;

/**
 * Regression for GH-809 / v8.1.7 PRs #812 and #821: Social Buttons widget rebrands Twitter to X
 * (color, SVG mask icon, share URL, labels, version).
 */
class SocialButtonsXRebrandTest {

  private static final Path PKG =
      Path.of("modules/perc-packages/src/main/resources/Packages/perc.widget.socialButtons");

  @Test
  void socialButtonsUsesXBranding() throws Exception {
    Path root = resolveRepoRoot();
    Path css =
        root.resolve(
            PKG.resolve(
                "sys__UserDependency--rx_resources/widgets/percSocialButtons/css/percSocialButtons.css"));
    Path supportCss =
        root.resolve(
            PKG.resolve(
                "SupportFile-rx_resources/widgets/percSocialButtons/css/percSocialButtons.css"));
    Path widget =
        root.resolve(PKG.resolve("sys__UserDependency--rxconfig/Widgets/percSocialButtons.xml"));
    Path xsl =
        root.resolve(
            PKG.resolve(
                "SupportFile-rx_resources/stylesheets/controls/percSocialButtonsControl.xsl"));
    Path archive = root.resolve(PKG.resolve("psx_archiveInfo.xml"));

    for (Path p : new Path[] {css, supportCss, widget, xsl, archive}) {
      if (!Files.isRegularFile(p)) {
        fail("expected " + p.toAbsolutePath());
      }
    }

    String cssText = Files.readString(css, StandardCharsets.UTF_8);
    assertTrue(cssText.contains("#111111"), "twitter brand color must be X black");
    assertFalse(cssText.contains("#00a9f1"), "legacy Twitter blue must be gone");
    assertTrue(cssText.contains("mask-size: contain"), "X icon sizing from #821");
    assertTrue(cssText.contains("vertical-align: -0.125em"), "X icon alignment from #821");

    String support = Files.readString(supportCss, StandardCharsets.UTF_8);
    assertTrue(support.contains("#111111") && support.contains("mask-size: contain"));

    String widgetXml = Files.readString(widget, StandardCharsets.UTF_8);
    assertTrue(widgetXml.contains("https://x.com/intent/tweet"), "share link must use x.com");
    assertFalse(
        widgetXml.contains("https://twitter.com/intent/tweet"), "legacy twitter.com share gone");
    assertTrue(widgetXml.contains("put('twitter', '111111')"), "velocity standard color");
    assertTrue(widgetXml.contains(".perc-twitter-social-button i::before"));

    String xslText = Files.readString(xsl, StandardCharsets.UTF_8);
    assertTrue(xslText.contains("aria-label=\"X (Twitter)\""));
    // Rebrand label must be exposed: fa-twitter must not carry aria-hidden
    assertFalse(
        xslText.matches("(?s).*fa-twitter\"[^>]*aria-hidden=\"true\".*"),
        "fa-twitter must not use aria-hidden so aria-label is announced");
    assertTrue(xslText.contains("placeholder=\"https://x.com/percussion\""));

    String arch = Files.readString(archive, StandardCharsets.UTF_8);
    assertTrue(arch.contains("<Version>1.2.4</Version>"), "package version bump");
  }

  /**
   * Walk parent directories from the process working directory until {@code modules/perc-packages}
   * is found. Works whether Surefire runs from the monorepo root or from {@code
   * projects/sitemanage}.
   */
  private static Path resolveRepoRoot() {
    Path dir = Path.of("").toAbsolutePath().normalize();
    while (dir != null) {
      if (Files.isDirectory(dir.resolve("modules/perc-packages"))) {
        return dir;
      }
      dir = dir.getParent();
    }
    fail("could not resolve monorepo root (no modules/perc-packages ancestor)");
    return Path.of("").toAbsolutePath();
  }
}
