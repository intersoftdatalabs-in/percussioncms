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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Pins the widget-payload JavaScript inventory audited under issue #6319
 * (parent audit #5627).
 *
 * <p>The {@code sys__UserDependency--web_resources} and
 * {@code SupportFile-rx_resources} trees under
 * {@code modules/perc-packages/src/main/resources/Packages} install verbatim
 * onto every CMS and publish to live sites, so any third-party {@code .js}
 * checked in here ships to customers invisible to {@code npm audit} and
 * Dependabot. The #6319 audit found 19 third-party files (FullCalendar 1.6.7 +
 * 2.9.1, Moment 2.13.0, Lodash 4.14.0, Slick 1.9.0, Foundation 4.3.1,
 * Superfish 1.7.10, plus unknown-version List.js / hoverIntent / supersubs /
 * gcal / qTip / MiniColors copies) with migration follow-on #6319, alongside
 * related follow-ons #6320 (system Bootstrap copies) and #6321 (FontAwesome).
 * Slice 1 of #6319 refreshed the directory lodash payload to npm 4.17.21 and
 * the calendarTwo moment payload to npm 2.31.0 (both Dependabot-tracked via
 * the WebUI frontend manifests, bytes pinned byte-identical to the npm dist
 * by the frontend parity test); the remaining rows stay vendored and are
 * tracked in the #6319 leftover residual.
 *
 * <p>Slice 2 of #6319 migrated the nav-widget superfish pair to the npm
 * superfish 1.7.10 dist (already Dependabot-tracked via the frontend
 * manifests for the classic-UI copies since #6016): the published
 * {@code superfish.min.js} was already byte-identical to the npm dist, and
 * the preview {@code superfish.js} was refreshed to the npm dist bytes,
 * restoring the upstream Joel Birch banner in place of the prepended
 * Percussion header. The nav hoverIntent/supersubs rows stay vendored and
 * are tracked in the #6319 leftover residual (supersubs is dropped by
 * #6403 slice 7 below; hoverIntent stays).
 *
 * <p>Slice 3 of #6319 migrated the directory-widget {@code list.min.js} to
 * the npm list.js 1.2.0 dist (byte-identical to the payload;
 * Dependabot-tracked via the WebUI frontend manifests with an exact 1.2.0
 * pin, since a caret range would float to 1.5.0+ whose dist bytes differ),
 * and refreshed the directory lodash payload to the npm 4.18.1 dist as a
 * Dependabot #6411 follow-on (the manifests had already moved to ^4.18.1).
 * The list banner pin below carries a stable List.js API marker
 * ({@code valueNames}) because the 1.2.0 minified dist ships no version
 * banner; byte-identity with the npm dist is enforced by the frontend
 * vitest parity test.
 *
 * <p>Slice 4 of #6403 pins stable markers for every remaining
 * unknown-version row (nav hoverIntent/supersubs pairs, MiniColors twins,
 * qTip twins, calendarTwo gcal + lang-all), so silent swaps fail loudly.
 * Slice 4 verified each leftover against the npm registry and found no
 * byte-identity migration: the calendarTwo fullcalendar pair carries
 * Percussion mods ({@code reportEvents}/{@code Perc.range}/{@code
 * computeRange}) and the stock {@code fullcalendar@2.9.1} dist ships
 * unexpanded {@code <%= meta.* %>} banners; {@code slick-carousel} has no
 * 1.9.0; npm {@code hoverintent} is the incompatible vanilla rewrite;
 * npm {@code supersubs} is unrelated; MiniColors matches no published
 * dist (provenance unclear); qTip is v1-era EOL; Foundation is 4.x EOL;
 * FullCalendar 1.6.7 is absent from npm. Replace-or-drop decisions and
 * legal flags move to the remainder follow-on #6502; #6403 stays open.
 *
 * <p>Slice 5 (issue #6502) rebases the calendarTwo fullcalendar pair onto
 * the npm {@code fullcalendar@2.9.1} dist (exact pin in both WebUI frontend
 * manifests, so Dependabot watches the base version) with the two
 * documented Percussion mods re-applied: {@code reportEvents} resolves the
 * per-widget {@code Perc.events} deferred consumed by
 * {@code percCalendarTwoSnippet.vm}, and month-view {@code computeRange}
 * uses {@code Perc.range} (6 for calendar layout, 52 for list layout) in
 * place of the stock hard-coded 6. Both payload files are byte-identical
 * to the stock dist apart from the banner expansion and those two hunks,
 * as reconstructed by the frontend vitest parity test. The rebase also
 * fixes two inherited warts: the published {@code .min.js} previously
 * carried a different uglify mangling than the stock dist with an
 * unexpanded {@code version:"<%= meta.version %>"} field (now
 * {@code "2.9.1"}), and the preview {@code .js} was a prettier
 * reformatting rather than the upstream bundle. The
 * {@code version:"<%= meta.version %>"} wart inside the non-min stock
 * dist itself is preserved verbatim (it ships in {@code fullcalendar@2.9.1}
 * upstream). The calendarTwo gcal + lang-all rows stay vendored — their
 * payload bytes match no 2.9.1 dist file — and move to the #6502
 * follow-on residual.
 *
 * <p>Slice 6 (issue #6522) migrates those calendarTwo gcal + lang-all
 * payloads to the npm {@code fullcalendar@2.9.1} dists (exact pin already
 * in both WebUI frontend manifests from slice 5, so Dependabot still
 * watches the base version). Version archaeology showed the old payloads
 * were differently-minified builds of the same 2.9.1 sources: same
 * 56-locale set with only minifier-artifact diffs (trailing-semicolon
 * elision, comma-expression parens). Both payload files are now
 * byte-identical to the stock dists, as enforced by the frontend vitest
 * parity test; the stock gcal banner ships the unexpanded
 * {@code <%= meta.* %>} wart verbatim (comment-only, inert: same
 * precedent as the slice-5 non-min version wart). The remaining #6522
 * rows (v1 calendar widget, slick, nav hoverIntent/supersubs, MiniColors
 * twins, qTip twins, Foundation 4.x) stay vendored pending
 * replace-or-drop decisions or legal sign-off.
 *
 * <p>Slice 7 (issue #6403) drops the nav-widget supersubs pair
 * ({@code supersubs.js} preview copy + {@code supersubs.min.js} published
 * copy). Version archaeology closed the byte-identity path: npm
 * {@code supersubs} is an unrelated Pi extension, so no Dependabot-tracked
 * dist exists. A repo-wide caller search found zero {@code .supersubs(}
 * invocations — {@code navbar.js} drives the menu only through
 * {@code $.fn.superfish}, and neither snippet template references the
 * plugin — and both copies are pure plugin definitions with no load-time
 * side effects, so removal is behavior-preserving. The
 * {@code percNavBar.xml} preview/publish entries and the
 * {@code psx_archiveInfo.xml} / {@code psx_archiveManifest.xml} dependency
 * entries are removed in the same change. The nav hoverIntent pair stays
 * vendored: superfish consumes it at runtime ({@code $.fn.hoverIntent &&
 * !o.disableHI}), npm {@code hoverintent} 2.x is the incompatible vanilla
 * rewrite, and the payload cut matches no {@code jquery-hoverintent} dist.
 *
 * <p>This test fails when a {@code .js} file is added to or removed from the
 * audited widget packages, or when a pinned version banner changes, so the
 * inventory can only drift together with an audit update. A migration PR that
 * legitimately removes a vendored copy must update the expected sets and the
 * banner pins in the same change.
 *
 * <p>Cross-platform: all path construction uses {@link Path#resolve(String)} /
 * {@link Files}; relative keys always use {@code '/'} separators.
 */
class WidgetPayloadVendoredJsAuditTest {

  /** Widget packages covered by the #6319 audit (paths relative to Packages root). */
  private static final Set<String> AUDITED_PACKAGES =
      Set.of(
          "perc.responsiveTemplates",
          "perc.widget.calendar",
          "perc.widget.categoryList",
          "perc.widget.commentForm",
          "perc.widget.directory",
          "perc.widget.form",
          "perc.widget.imageSlider",
          "perc.widget.login",
          "perc.widget.poll",
          "perc.widget.registration",
          "perc.widgets.nav");

  /** Third-party .js shipped in the audited widget payloads (paths relative to Packages root). */
  private static final Set<String> EXPECTED_THIRD_PARTY_JS =
      Set.of(
          "perc.responsiveTemplates/sys__UserDependency--web_resources/cm/foundation/js/foundation.min.js",
          "perc.widget.calendar/SupportFile-rx_resources/widgets/percCalendarTwo/js/jquery.minicolors.js",
          "perc.widget.calendar/sys__UserDependency--rx_resources/widgets/percCalendarTwo/js/jquery.minicolors.js",
          "perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendar/js/fullcalendar.min.js",
          "perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendar/js/jquery.qtip.js",
          "perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js",
          "perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.min.js",
          "perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/gcal.js",
          "perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/jquery.qtip.js",
          "perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/lang-all.js",
          "perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/moment.min.js",
          "perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/list.min.js",
          "perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/lodash.min.js",
          "perc.widget.imageSlider/sys__UserDependency--web_resources/widgets/imageSlider/js/slick.min.js",
          "perc.widgets.nav/sys__UserDependency--web_resources/widgets/navBar/js/hoverIntent.js",
          "perc.widgets.nav/sys__UserDependency--web_resources/widgets/navBar/js/hoverIntent.min.js",
          "perc.widgets.nav/sys__UserDependency--web_resources/widgets/navBar/js/superfish.js",
          "perc.widgets.nav/sys__UserDependency--web_resources/widgets/navBar/js/superfish.min.js");

  /** First-party Percussion JS in the audited payloads — recorded so additions stand out. */
  private static final Set<String> EXPECTED_FIRST_PARTY_JS =
      Set.of(
          "perc.widget.calendar/SupportFile-rx_resources/widgets/percCalendarTwo/js/percCalendarTwo.js",
          "perc.widget.calendar/sys__UserDependency--rx_resources/widgets/percCalendarTwo/js/percCalendarTwo.js",
          "perc.widget.categoryList/SupportFile-rx_resources/widgets/categoryList/js/categoryListWidget.js",
          "perc.widget.categoryList/SupportFile-rx_resources/widgets/categoryList/js/jquery.categoryList.js",
          "perc.widget.categoryList/sys__UserDependency--rx_resources/widgets/categoryList/js/categoryListWidget.js",
          "perc.widget.categoryList/sys__UserDependency--rx_resources/widgets/categoryList/js/jquery.categoryList.js",
          "perc.widget.categoryList/sys__UserDependency--web_resources/widgets/category/js/category.js",
          "perc.widget.categoryList/sys__UserDependency--web_resources/widgets/category/js/category.min.js",
          "perc.widget.commentForm/SupportFile-rx_resources/widgets/commentsForm/js/PercCommentsFormController.js",
          "perc.widget.commentForm/SupportFile-rx_resources/widgets/commentsForm/js/PercCommentsFormView.js",
          "perc.widget.commentForm/SupportFile-rx_resources/widgets/commentsForm/js/PercWidgetEditor.js",
          "perc.widget.commentForm/SupportFile-rx_resources/widgets/commentsForm/js/TinymceConfig.js",
          "perc.widget.commentForm/SupportFile-rx_resources/widgets/commentsForm/js/perc-tiptap-editor.bundle.min.js",
          "perc.widget.directory/SupportFile-rx_resources/widgets/AssetFinder/js/AssetFinder.js",
          "perc.widget.directory/SupportFile-rx_resources/widgets/percDirectory/js/directoryDropdown.js",
          "perc.widget.directory/sys__UserDependency--rx_resources/widgets/AssetFinder/js/AssetFinder.js",
          "perc.widget.directory/sys__UserDependency--rx_resources/widgets/percDepartment/js/percDepartment.js",
          "perc.widget.directory/sys__UserDependency--rx_resources/widgets/percDirectory/js/directoryDropdown.js",
          "perc.widget.directory/sys__UserDependency--rx_resources/widgets/percDirectory/js/percDirectory.js",
          "perc.widget.directory/sys__UserDependency--rx_resources/widgets/percOrganization/js/percOrganization.js",
          "perc.widget.directory/sys__UserDependency--rx_resources/widgets/percPerson/js/percPerson.js",
          "perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/perc-directory.js",
          "perc.widget.form/SupportFile-rx_resources/widgets/form/js/PercFormController.js",
          "perc.widget.form/SupportFile-rx_resources/widgets/form/js/PercFormView.js",
          "perc.widget.form/SupportFile-rx_resources/widgets/form/js/PercWidgetEditor.js",
          "perc.widget.form/SupportFile-rx_resources/widgets/form/js/TinymceConfig.js",
          "perc.widget.form/SupportFile-rx_resources/widgets/form/js/perc-tiptap-editor.bundle.min.js",
          "perc.widget.form/sys__UserDependency--web_resources/widgets/form/js/form.js",
          "perc.widget.form/sys__UserDependency--web_resources/widgets/form/js/form.min.js",
          "perc.widget.imageSlider/SupportFile-rx_resources/widgets/percImageSlider/js/percImageSlider.js",
          "perc.widget.imageSlider/sys__UserDependency--rx_resources/widgets/percImageSlider/js/percImageSlider.js",
          "perc.widget.login/sys__UserDependency--web_resources/widgets/login/js/loginform.js",
          "perc.widget.login/sys__UserDependency--web_resources/widgets/login/js/loginform.min.js",
          "perc.widget.poll/SupportFile-rx_resources/widgets/percPoll/js/PercPollEditor.min.js",
          "perc.widget.poll/sys__UserDependency--web_resources/widgets/percPoll/js/percPoll.js",
          "perc.widget.poll/sys__UserDependency--web_resources/widgets/percPoll/js/percPoll.min.js",
          "perc.widget.registration/sys__UserDependency--web_resources/widgets/registration/js/registrationform.js",
          "perc.widget.registration/sys__UserDependency--web_resources/widgets/registration/js/registrationform.min.js",
          "perc.widgets.nav/sys__UserDependency--web_resources/widgets/navBar/js/navbar.js",
          "perc.widgets.nav/sys__UserDependency--web_resources/widgets/navBar/js/navbar.min.js");

  private static final Set<String> EXPECTED_JS_FILES =
      Stream.concat(EXPECTED_THIRD_PARTY_JS.stream(), EXPECTED_FIRST_PARTY_JS.stream())
          .collect(Collectors.toUnmodifiableSet());

  /**
   * Version banners pinned by the audit. Unknown-version rows pin a stable
   * marker instead so silent swaps still fail loudly.
   */
  private static final Map<String, String> EXPECTED_VERSION_BANNERS =
      Map.ofEntries(
          Map.entry(
              "perc.responsiveTemplates/sys__UserDependency--web_resources/cm/foundation/js/foundation.min.js",
              "Foundation Responsive Library"),
          Map.entry(
              "perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendar/js/fullcalendar.min.js",
              "FullCalendar v1.6.7"),
          Map.entry(
              "perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js",
              "FullCalendar v2.9.1"),
          Map.entry(
              "perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.min.js",
              "FullCalendar v2.9.1"),
          Map.entry(
              "perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/moment.min.js",
              "version : 2.31.0"),
          Map.entry(
              "perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/list.min.js",
              "valueNames"),
          Map.entry(
              "perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/lodash.min.js",
              "\"4.18.1\""),
          Map.entry(
              "perc.widget.imageSlider/sys__UserDependency--web_resources/widgets/imageSlider/js/slick.min.js",
              "Version: 1.9.0"),
          Map.entry(
              "perc.widgets.nav/sys__UserDependency--web_resources/widgets/navBar/js/superfish.js",
              "jQuery Superfish Menu Plugin - v1.7.10"),
          Map.entry(
              "perc.widgets.nav/sys__UserDependency--web_resources/widgets/navBar/js/superfish.min.js",
              "Superfish Menu Plugin - v1.7.10"),
          Map.entry(
              "perc.widget.calendar/SupportFile-rx_resources/widgets/percCalendarTwo/js/jquery.minicolors.js",
              "minicolors = {"),
          Map.entry(
              "perc.widget.calendar/sys__UserDependency--rx_resources/widgets/percCalendarTwo/js/jquery.minicolors.js",
              "minicolors = {"),
          Map.entry(
              "perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendar/js/jquery.qtip.js",
              "NO_TOOLTIP_PRESENT"),
          Map.entry(
              "perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/jquery.qtip.js",
              "NO_TOOLTIP_PRESENT"),
          Map.entry(
              "perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/gcal.js",
              "googleCalendarId"),
          Map.entry(
              "perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/lang-all.js",
              "fullCalendar.datepickerLang"),
          Map.entry(
              "perc.widgets.nav/sys__UserDependency--web_resources/widgets/navBar/js/hoverIntent.js",
              "handlerIn, handlerOut"),
          Map.entry(
              "perc.widgets.nav/sys__UserDependency--web_resources/widgets/navBar/js/hoverIntent.min.js",
              "hoverIntent_t"));

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

  private static Set<String> collectAuditedJs(Path root) throws IOException {
    Set<String> found = new TreeSet<>();
    for (String pkg : AUDITED_PACKAGES) {
      Path pkgDir = root.resolve(pkg);
      if (!Files.isDirectory(pkgDir)) {
        fail("Audited widget package missing from payload: " + pkg);
      }
      try (Stream<Path> walk = Files.walk(pkgDir)) {
        walk.filter(Files::isRegularFile)
            .filter(p -> p.getFileName().toString().endsWith(".js"))
            .map(root::relativize)
            .map(p -> p.toString().replace(java.io.File.separatorChar, '/'))
            .forEach(found::add);
      }
    }
    return found;
  }

  @Test
  void payloadJsInventoryMatchesAudit6319() throws IOException {
    Path root = packagesRoot();
    Set<String> actual = collectAuditedJs(root);
    assertEquals(new TreeSet<>(EXPECTED_JS_FILES), actual,
        "Widget-payload JS inventory drifted from the #6319 audit — "
            + "update the expected sets and file/refresh the migration issues (#6319/#6320/#6321).");
  }

  @Test
  void versionBannersMatchAudit6319() throws IOException {
    Path root = packagesRoot();
    Map<String, String> mismatches = new LinkedHashMap<>();
    for (Map.Entry<String, String> pin : EXPECTED_VERSION_BANNERS.entrySet()) {
      Path file = root.resolve(pin.getKey());
      String banner = pin.getValue();
      String content = Files.readString(file, StandardCharsets.UTF_8);
      if (!content.contains(banner)) {
        mismatches.put(pin.getKey(), banner);
      }
    }
    assertTrue(mismatches.isEmpty(),
        "Version banner drift in widget payloads (upgraded or swapped without audit update): "
            + mismatches);
  }

  @Test
  void noUnlistedMinifiedBundleInAuditedPayloads() throws IOException {
    Path root = packagesRoot();
    Set<String> actual = collectAuditedJs(root);
    Set<String> unlistedMin =
        actual.stream()
            .filter(p -> p.endsWith(".min.js"))
            .filter(p -> !EXPECTED_JS_FILES.contains(p))
            .collect(Collectors.toCollection(TreeSet::new));
    assertTrue(unlistedMin.isEmpty(),
        "New minified (vendored) bundle in widget payloads — audit it under #5627: " + unlistedMin);
  }

  @Test
  void calendarTwoFullcalendarCarriesPercussionMods() throws IOException {
    Path root = packagesRoot();
    List<String> rebasedPair =
        List.of(
            "perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js",
            "perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.min.js");
    Map<String, String> missing = new LinkedHashMap<>();
    for (String rel : rebasedPair) {
      String content = Files.readString(root.resolve(rel), StandardCharsets.UTF_8);
      // Slice 5 (#6502) rebase: the reportEvents mod resolves the per-widget
      // Perc.events deferred consumed by percCalendarTwoSnippet.vm, and the
      // month-view computeRange mod reads Perc.range instead of hard-coded 6.
      for (String marker : List.of("Perc.events", ".resolve(", "Perc.range")) {
        if (!content.contains(marker)) {
          missing.put(rel, marker);
        }
      }
      assertTrue(!content.contains("<%= meta.title %>"),
          rel + " still ships an unexpanded meta banner — rebase from fullcalendar@2.9.1");
    }
    assertTrue(missing.isEmpty(),
        "Percussion fork mods missing from rebased calendarTwo fullcalendar pair: " + missing);
  }
}
