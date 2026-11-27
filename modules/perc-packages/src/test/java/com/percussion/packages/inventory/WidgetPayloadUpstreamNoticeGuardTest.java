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
 * JavaScript and stylesheets (JS: issue #6734; CSS: issue #7002, parent
 * audit #5627).
 *
 * <p>The MIT license requires keeping the original copyright and permission
 * notice in copies and substantial portions. Two payload rows carried a
 * Percussion/Intersoft header in place of the upstream notice: the
 * nav-widget hoverIntent pair (upstream MIT banner restored and kept by the
 * merged #6725 shim, issue #6689) and the MiniColors twins (banner restored
 * by the merged #6724 rebase, issue #6686, and kept byte-identical through
 * the #6723 2.3.6 rebase — the twins carry no identifiable upstream body
 * markers, so the generic sweep below covers them trivially).</p>
 *
 * <p>The CSS sweep (issue #7002) closes the same hole for payload
 * stylesheets: the FullCalendar stylesheet banner rewrite reviewed in
 * #6994 dropped the upstream notice without anything going red because the
 * guard only swept {@code .js}. Ten shipped CSS payloads carried upstream
 * hooks under a missing or first-party-only header (MiniColors twins, Slick
 * pair, Superfish quad, Lightbox, jQuery UI 1.8.9 ThemeRoller copy); each
 * now carries a {@code /*!} preserve-comment attribution banner mirroring
 * its sibling-script banner, pinned by {@link #VENDORED_CSS_ATTRIBUTION}.</p>
 *
 * <p>The generic sweeps fail when any payload {@code .js} / {@code .css}
 * carries identifiable upstream code (author/origin markers) while its
 * header holds no upstream copyright/license notice, so a future vendored
 * file cannot land with only a first-party header the way MiniColors did.</p>
 *
 * <p>Cross-platform: all path construction uses {@link Path#resolve(String)}
 * / {@link Files}; relative keys always use {@code '/'} separators.
 */
class WidgetPayloadUpstreamNoticeGuardTest {

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
   * (hoverIntent minified twins, Slick pair), dual-license lines
   * (superfish JS + CSS), {@code Released under the MIT} (qTip2, Lightbox
   * JS + CSS), the {@code Docs & License} form (FullCalendar pair, gcal
   * stock banner), the MiniColors permission URL, and the
   * {@code Licensed MIT} form (jQuery UI CSS banner).
   */
  private static final List<String> UPSTREAM_NOTICE_MARKERS =
      List.of(
          "Permission is hereby granted",
          "opensource.org/licenses",
          "Dual licensed",
          "Released under the MIT",
          "Docs & License:",
          "MIT license",
          "Licensed MIT");

  /**
   * Markers that identify an upstream stylesheet body. Each fires only on
   * genuine third-party payload CSS (verified November 2026): none of them
   * appears in first-party payload stylesheets — {@code .ui-helper} hooks
   * were rejected as a marker because the form/comment widgets reuse those
   * class names, while {@code ui-bg_flat} ThemeRoller sprite URLs fire only
   * on the two jQuery UI copies (including the banner-less 1.8.9 custom
   * theme).
   */
  private static final List<String> UPSTREAM_CSS_BODY_MARKERS =
      List.of(
          "Adam Shaw",
          ".minicolors-sprite",
          ".slick-slider",
          ".sf-menu",
          "lb-disable-scrolling",
          "ui-bg_flat");

  /** Nav-widget hoverIntent files whose upstream MIT notice must survive. */
  private static final List<String> HOVER_INTENT_FILES =
      List.of(
          "perc.widgets.nav/sys__UserDependency--web_resources/widgets/navBar/js/hoverIntent.js",
          "perc.widgets.nav/sys__UserDependency--web_resources/widgets/navBar/js/hoverIntent.min.js",
          "perc.widgets.nav/sys__UserDependency--web_resources/widgets/navBar/js/perc-hover-intent.js",
          "perc.widgets.nav/sys__UserDependency--web_resources/widgets/navBar/js/perc-hover-intent.min.js");

  /**
   * Vendored payload stylesheets and the upstream attribution snippet each
   * header must keep (issue #7002). Snippets mirror the sibling-script
   * banners where one exists (MiniColors, superfish, Lightbox) so the CSS
   * and JS rows of the same library cite the same author.
   */
  private static final Map<String, String> VENDORED_CSS_ATTRIBUTION =
      Map.ofEntries(
          Map.entry(
              "perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/css/fullcalendar.min.css",
              "Adam Shaw"),
          Map.entry(
              "perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/css/jquery-ui.min.css",
              "jQuery Foundation"),
          Map.entry(
              "perc.baseWidgets/sys__UserDependency--web_resources/cm/themes/smoothness/jquery-ui.css",
              "OpenJS Foundation"),
          Map.entry(
              "perc.widget.calendar/SupportFile-rx_resources/widgets/percCalendarTwo/css/jquery.minicolors.css",
              "Cory LaViska"),
          Map.entry(
              "perc.widget.calendar/sys__UserDependency--rx_resources/widgets/percCalendarTwo/css/jquery.minicolors.css",
              "Cory LaViska"),
          Map.entry(
              "perc.widget.imageSlider/sys__UserDependency--web_resources/widgets/imageSlider/css/slick.css",
              "Ken Wheeler"),
          Map.entry(
              "perc.widget.imageSlider/sys__UserDependency--web_resources/widgets/imageSlider/css/slick-theme.css",
              "Ken Wheeler"),
          Map.entry(
              "perc.widgets.nav/sys__UserDependency--web_resources/widgets/navBar/css/superfish.css",
              "Joel Birch"),
          Map.entry(
              "perc.widgets.nav/sys__UserDependency--web_resources/widgets/navBar/css/superfish-navbar.css",
              "Joel Birch"),
          Map.entry(
              "perc.widgets.nav/sys__UserDependency--web_resources/widgets/navBar/css/superfish-vertical.css",
              "Joel Birch"),
          Map.entry(
              "perc.widgets.nav/sys__UserDependency--web_resources/widgets/navBar/css/megafish.css",
              "Joel Birch"),
          Map.entry(
              "perc.widgets.image/sys__UserDependency--web_resources/widgets/image/lightbox/lightbox.css",
              "Lokesh Dhakar"));

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
    Map<String, String> violations = sweepPayloads(root, ".js", UPSTREAM_BODY_MARKERS);
    assertTrue(
        violations.isEmpty(),
        "Vendored payload JS carries upstream code under a first-party-only header "
            + "(issue #6734) — restore the upstream copyright/license notice: "
            + violations);
  }

  @Test
  void vendoredCssKeepsUpstreamNotice() throws IOException {
    Path root = packagesRoot();
    Map<String, String> missing = new LinkedHashMap<>();
    for (Map.Entry<String, String> pin : VENDORED_CSS_ATTRIBUTION.entrySet()) {
      String content = Files.readString(root.resolve(pin.getKey()), StandardCharsets.UTF_8);
      if (!headerOf(content).contains(pin.getValue())) {
        missing.put(pin.getKey(), "upstream attribution '" + pin.getValue() + "'");
      }
    }
    assertTrue(
        missing.isEmpty(),
        "Vendored payload CSS lost the upstream attribution banner (issue #7002) — "
            + "restore the upstream copyright/license notice: "
            + missing);
  }

  @Test
  void noFirstPartyHeaderOverUpstreamCssBody() throws IOException {
    Path root = packagesRoot();
    Map<String, String> violations = sweepPayloads(root, ".css", UPSTREAM_CSS_BODY_MARKERS);
    assertTrue(
        violations.isEmpty(),
        "Vendored payload CSS carries upstream code under a first-party-only header "
            + "(issue #7002) — restore the upstream copyright/license notice: "
            + violations);
  }

  private static Map<String, String> sweepPayloads(
      Path root, String extension, List<String> bodyMarkers) throws IOException {
    List<String> payloadFiles = new ArrayList<>();
    try (Stream<Path> walk = Files.walk(root)) {
      walk.filter(Files::isRegularFile)
          .filter(p -> p.getFileName().toString().endsWith(extension))
          .map(root::relativize)
          .map(p -> p.toString().replace(java.io.File.separatorChar, '/'))
          .sorted()
          .forEach(payloadFiles::add);
    }
    Map<String, String> violations = new LinkedHashMap<>();
    for (String rel : payloadFiles) {
      String content = Files.readString(root.resolve(rel), StandardCharsets.UTF_8);
      String trigger =
          bodyMarkers.stream().filter(content::contains).findFirst().orElse(null);
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
    return violations;
  }
}
