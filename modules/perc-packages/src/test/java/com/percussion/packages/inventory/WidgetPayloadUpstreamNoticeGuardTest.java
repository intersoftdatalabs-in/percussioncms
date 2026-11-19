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
package com.percussion.packages.inventory;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Guards upstream copyright/license notices on vendored widget-payload
 * JavaScript (issue #6734, parent audit #5627).
 *
 * <p>The MIT license requires keeping the original copyright and permission
 * notice in copies and substantial portions. Two payload rows carried a
 * Percussion/Intersoft header in place of the upstream notice: the
 * nav-widget hoverIntent pair (upstream MIT banner restored and kept by the
 * merged #6725 shim, issue #6689) and the MiniColors twins (banner
 * restoration owned by open PR #6724, issue #6686 — excluded below so this
 * guard stays green across that rebase; #6724 pins the restored banner in
 * {@code WidgetPayloadVendoredJsAuditTest}).
 *
 * <p>The generic sweep fails when any payload {@code .js} carries
 * identifiable upstream code (author/origin markers) while its header holds
 * no upstream copyright/license notice, so a future vendored file cannot land
 * with only a first-party header the way MiniColors did.
 *
 * <p>Cross-platform: all path construction uses {@link Path#resolve(String)}
 * / {@link Files}; relative keys always use {@code '/'} separators.
 */
class WidgetPayloadUpstreamNoticeGuardTest {

  /** MiniColors twins — banner restoration owned by open PR #6724 (#6686). */
  private static final List<String> MINICOLORS_EXCLUSION =
      List.of(
          "perc.widget.calendar/SupportFile-rx_resources/widgets/percCalendarTwo/js/jquery.minicolors.js",
          "perc.widget.calendar/sys__UserDependency--rx_resources/widgets/percCalendarTwo/js/jquery.minicolors.js");

  /**
   * Markers that identify an upstream body. Each fires only on genuine
   * third-party payload files (verified November 2026): author names never
   * appear in first-party payload code, {@code .superfish(} call sites such
   * as {@code navbar.js} carry no author marker, and the two origin URLs
   * appear only in the gcal/qTip payloads.
   */
  private static final List<String> UPSTREAM_BODY_MARKERS =
      List.of(
          "Brian Cherne",
          "Joel Birch",
          "Adam Shaw",
          "Lokesh Dhakar",
          "qtip2.com",
          "www.googleapis.com/calendar");

  /**
   * Evidence of an upstream copyright/license notice in a file header
   * (first 40 lines). Covers the banner shapes currently shipped:
   * full MIT permission text (hoverIntent non-minified), MIT shorthand
   * (hoverIntent minified twins), dual-license lines (superfish),
   * {@code Released under the MIT} (qTip2, Lightbox), and the
   * {@code Docs & License} form (FullCalendar pair, gcal stock banner).
   */
  private static final List<String> UPSTREAM_NOTICE_MARKERS =
      List.of(
          "Permission is hereby granted",
          "opensource.org/licenses",
          "Dual licensed",
          "Released under the MIT",
          "Docs & License:",
          "MIT license");

  /** Nav-widget hoverIntent files whose upstream MIT notice must survive. */
  private static final List<String> HOVER_INTENT_FILES =
      List.of(
          "perc.widgets.nav/sys__UserDependency--web_resources/widgets/navBar/js/hoverIntent.js",
          "perc.widgets.nav/sys__UserDependency--web_resources/widgets/navBar/js/hoverIntent.min.js",
          "perc.widgets.nav/sys__UserDependency--web_resources/widgets/navBar/js/perc-hover-intent.js",
          "perc.widgets.nav/sys__UserDependency--web_resources/widgets/navBar/js/perc-hover-intent.min.js");

  private static Path packagesRoot() {
    List<Path> candidates =
        List.of(
            Path.of(System.getProperty("user.dir"), "src", "main", "resources", "Packages"),
            Path.of(
                System.getProperty("user.dir"),
                "modules",
                "perc-packages",
                "src",
                "main",
                "resources",
                "Packages"));
    for (Path candidate : candidates) {
      if (Files.isDirectory(candidate)) {
        return candidate;
      }
    }
    fail("Packages payload root not found under user.dir=" + System.getProperty("user.dir"));
    throw new IllegalStateException("unreachable");
  }

  private static String headerOf(String content) {
    String[] lines = content.split("\n", -1);
    StringBuilder header = new StringBuilder();
    for (int i = 0; i < Math.min(40, lines.length); i++) {
      header.append(lines[i]).append('\n');
    }
    return header.toString();
  }

  @Test
  void navHoverIntentKeepsUpstreamMitNotice() throws IOException {
    Path root = packagesRoot();
    Map<String, String> missing = new LinkedHashMap<>();
    for (String rel : HOVER_INTENT_FILES) {
      String content = Files.readString(root.resolve(rel), StandardCharsets.UTF_8);
      if (!content.contains("Copyright 2007, 2013 Brian Cherne")) {
        missing.put(rel, "upstream copyright line");
      } else if (!content.contains("Permission is hereby granted")
          && !content.contains("MIT license")) {
        missing.put(rel, "MIT permission/license notice");
      }
    }
    assertTrue(
        missing.isEmpty(),
        "Nav hoverIntent files lost the upstream MIT notice (issue #6734) — "
            + "restore Copyright 2007, 2013 Brian Cherne + the permission notice: "
            + missing);
  }

  @Test
  void noFirstPartyHeaderOverUpstreamBody() throws IOException {
    Path root = packagesRoot();
    List<String> payloadJs = new ArrayList<>();
    try (Stream<Path> walk = Files.walk(root)) {
      walk.filter(Files::isRegularFile)
          .filter(p -> p.getFileName().toString().endsWith(".js"))
          .map(root::relativize)
          .map(p -> p.toString().replace(java.io.File.separatorChar, '/'))
          .sorted()
          .forEach(payloadJs::add);
    }
    Map<String, String> violations = new LinkedHashMap<>();
    for (String rel : payloadJs) {
      if (MINICOLORS_EXCLUSION.contains(rel)) {
        continue;
      }
      String content = Files.readString(root.resolve(rel), StandardCharsets.UTF_8);
      String trigger =
          UPSTREAM_BODY_MARKERS.stream().filter(content::contains).findFirst().orElse(null);
      if (trigger == null) {
        continue;
      }
      String header = headerOf(content);
      boolean noticed =
          UPSTREAM_NOTICE_MARKERS.stream().anyMatch(header::contains);
      if (!noticed) {
        violations.put(rel, "upstream body marker '" + trigger + "' with no upstream notice");
      }
    }
    assertTrue(
        violations.isEmpty(),
        "Vendored payload JS carries upstream code under a first-party-only header "
            + "(issue #6734) — restore the upstream copyright/license notice: "
            + violations);
  }
}
