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
 * <p>Slice 8 (issue #6403) migrates the imageSlider-widget
 * {@code slick.min.js} to the npm {@code slick-carousel@2.0.0} dist
 * (exact pin in the frontend manifest, so Dependabot watches it).
 * Version archaeology: the payload banner said 1.9.0, but 1.9.0 was never
 * published to npm (the registry holds 1.8.1 then 2.0.0) - the 1.9.0 cut
 * came from the archived GitHub tag, invisible to Dependabot. The 1.8.1
 * and 2.0.0 dists carry identical widget-facing APIs (same defaults keys,
 * same Slick.prototype methods; 2.0.0 only modernizes the
 * page-visibility detection and internals), so tracking latest 2.0.0 is a
 * safe upgrade for the snippet's option set (fade/arrows/dots/infinite/
 * slidesToShow/slidesToScroll/autoplay/speed/responsive/asNavFor/
 * centerMode/focusOnSelect). The 2.0.0 dist ships no version banner, so
 * the pin below uses the stable {@code slick-initialized} runtime marker;
 * byte-identity with the npm dist is enforced by the frontend vitest
 * parity test.
 *
 * <p>Slice 9 (issue #6522) migrates the calendar-widget qTip twins
 * ({@code calendar/js/jquery.qtip.js} qTip1-era non-minified cut with a
 * prepended Percussion header, plus the {@code calendarTwo} twin, a
 * differently-minified qTip2-era cut) to the npm {@code qtip2@3.0.3}
 * full dist (exact pin in the frontend manifest, so Dependabot watches
 * it). qTip2 is the direct upstream successor of the qTip1 API the two
 * calendar snippets consume, and 3.0.3 is its final published dist
 * (MIT-licensed). Both twins are now byte-identical to the stock dist
 * (same identical-twin precedent as the MiniColors pair). The two
 * snippet call sites ({@code percCalendarSnippet.vm} and
 * {@code percCalendarTwoSnippet.vm} {@code eventRender} summary
 * tooltips) move from the qTip1 options shape ({@code content} string,
 * {@code style.name}/{@code position.corner}) to the qTip2 shape
 * ({@code content.text}, {@code position.my/at},
 * {@code style.classes: 'qtip-light'}), preserving the anchored
 * bottom-middle summary tooltip. (Slice 12 / issue #6564 later drops the
 * v1 widget and its snippet, leaving the {@code percCalendarTwoSnippet.vm}
 * call site.) The qTip2 dist calls
 * {@code jQuery.type}, removed from the harness jQuery 4 but present on
 * published pages via the shipped {@code jquery-migrate} (the retired
 * twin likewise required migrate-era {@code jQuery.browser}), so the
 * frontend parity test loads migrate before the payload. Remaining #6522
 * rows (v1 calendar widget, nav hoverIntent, MiniColors twins,
 * Foundation 4.x) stay vendored pending replace-or-drop decisions or
 * legal sign-off.
 *
 * <p>Slice 10 (issue #6319) migrates the image-widget
 * {@code lightbox.js} to the npm {@code lightbox2@2.10.0} dist (exact pin
 * in the frontend manifest, so Dependabot watches it). Version archaeology
 * showed the payload was a prettier-reformatted cut of stock 2.10.0 carrying
 * two Percussion mods: the CodeQL js/xss-through-dom (#1441) selector
 * hardening (exact-attr-match {@code .filter()} instead of interpolating
 * {@code data-lightbox}/{@code rel} values into selector strings — the
 * snippet renders {@code data-lightbox="<assetId>"} so the hardening is
 * load-bearing) and — accidentally — dead {@code $.proxy} wrappers that
 * broke resize-overlay and keyboard navigation versus stock. The rebased
 * payload is byte-identical to the stock dist apart from the two
 * documented #1441 hunks plus one adjudicated empty-catch suppression
 * comment on the upstream touch-probe (upstream banner restored,
 * Percussion header dropped, working {@code $.proxy} handlers restored),
 * as reconstructed by the frontend vitest parity test. The image-widget
 * {@code jquery.imageAssetControl.js} twins are recorded first-party, and
 * the audited package set grows to include {@code perc.widgets.image} —
 * the first payload package outside the original #6319 inventory, proving
 * the audit sweep continues. Remaining #6319 rows (v1 calendar widget,
 * nav hoverIntent, MiniColors twins, Foundation 4.x) stay vendored pending
 * replace-or-drop decisions or legal sign-off; the unaudited-package
 * sweep follow-ons (PageAutoList {@code date.js}, further rows) move to
 * residuals.
  *
  * <p>Slice 11 (parent audit #5627 close-out sweep) extends the guard to
  * every remaining payload package that ships {@code .js} so the whole
  * {@code Packages} tree is now pinned: {@code perc.Baseline},
  * {@code perc.CategoryDropDownControl}, {@code perc.ImageAutoListWidget},
  * {@code perc.PageAutoListWidget}, {@code perc.baseWidgets},
  * {@code perc.defaultLanguage}, {@code perc.widget.blogIndexPage},
  * {@code perc.widget.cookieConsent}, {@code perc.widget.fileAutoList},
  * {@code perc.widget.secureLogin}, {@code perc.widget.socialButtons} and
  * {@code perc.widget.taglist}. The sweep found no new vendored
  * third-party code: 21 of the 22 newly pinned files are first-party
  * Percussion sources (Apache headers), and the one third-party file -
  * the {@code perc.baseWidgets} published {@code cm/jslib/jquery-ui.js} -
  * is byte-identical to the npm {@code jquery-ui@1.14.2} minified dist
  * refreshed by #5660 and already Dependabot-tracked via the WebUI
  * frontend manifests (the {@code .js} filename carrying minified bytes is
  * a pre-existing published-name quirk, out of scope to rename). No new
  * migration issue is filed; the banner pin below
  * ({@code jQuery UI - v1.14.2}) fails loudly if the payload copy ever
  * drifts from the tracked npm line.
 *
 * <p>Slice 12 (issue #6564, parent #6522 remainder) drops the v1
 * {@code percCalendar} widget (FullCalendar 1.6.7, EOL, no 1.x on npm) per
 * the recorded maintainer Drop decision: the {@code widgets/percCalendar}
 * component, the {@code web_resources/widgets/calendar} + {@code
 * rx_resources/widgets/calendar} payload trees, the {@code
 * rxconfig/Resources/percCalendar.xml} resource file, and their
 * {@code psx_archiveInfo.xml} / {@code psx_archiveManifest.xml} entries
 * are removed. The shared {@code percCalendarAsset} content-type defs stay
 * (still consumed by {@code percCalendarTwo}); expected sets and banner
 * pins drop only the two v1 rows. MiniColors stays vendored under the
 * recorded MIT-ok ruling: upstream {@code jquery-minicolors} is
 * MIT-licensed (npm {@code license: MIT}, {@code @license:
 * http://opensource.org/licenses/MIT} banner), so no GPL code ships; the
 * twins match no published npm dist (the registry holds only 2.1.10, which
 * lacks the {@code swatches}/{@code keywords} surface the
 * {@code percCalendarTwo} editor consumes), so byte-identical npm-tracking
 * is impossible and the pair stays pinned as documented vendored code,
 * same precedent as the nav hoverIntent row.
 *
 * <p>Slice 13 (issue #6654, parent #6522) drops the
 * {@code perc.responsiveTemplates} package (Foundation 4.x, 2013, EOL) per
 * the recorded maintainer Drop decision: the {@code pages/perc.resp.*}
 * sources, the {@code web_resources/cm/foundation} payload tree (JS + CSS),
 * the template thumbnails, and their {@code psx_archiveInfo.xml} entries
 * are removed, alongside the package's five installer-list rows. The
 * {@code sys_assembly.vm} {@code perc.resp} foundation/normalize include
 * blocks (plus the viewport meta they carried) are removed in the same
 * change, so published pages no longer emit
 * {@code /web_resources/cm/foundation} references. Expected sets and
 * banner pins drop the package and its single Foundation row; no other
 * audited package ships foundation bytes.
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

  /**
   * Widget packages covered by the audit (paths relative to Packages root).
   *
   * <p>Original #6319 set plus every remaining {@code .js}-shipping package
   * added by the #5627 close-out sweep (slice 11) - the whole payload tree
   * is now pinned.
   */
  private static final Set<String> AUDITED_PACKAGES =
      Set.of(
          "perc.Baseline",
          "perc.CategoryDropDownControl",
          "perc.ImageAutoListWidget",
          "perc.PageAutoListWidget",
          "perc.baseWidgets",
          "perc.defaultLanguage",
          "perc.widget.blogIndexPage",
          "perc.widget.cookieConsent",
          "perc.widget.fileAutoList",
          "perc.widget.secureLogin",
          "perc.widget.socialButtons",
          "perc.widget.taglist",
          "perc.widget.calendar",
          "perc.widget.categoryList",
          "perc.widget.commentForm",
          "perc.widget.directory",
          "perc.widget.form",
          "perc.widget.imageSlider",
          "perc.widget.login",
          "perc.widget.poll",
          "perc.widget.registration",
          "perc.widgets.image",
          "perc.widgets.nav");

  /** Third-party .js shipped in the audited widget payloads (paths relative to Packages root). */
  private static final Set<String> EXPECTED_THIRD_PARTY_JS =
      Set.of(
          "perc.baseWidgets/sys__UserDependency--web_resources/cm/jslib/jquery-ui.js",
          "perc.widget.calendar/SupportFile-rx_resources/widgets/percCalendarTwo/js/jquery.minicolors.js",
          "perc.widget.calendar/sys__UserDependency--rx_resources/widgets/percCalendarTwo/js/jquery.minicolors.js",
          "perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js",
          "perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.min.js",
          "perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/gcal.js",
          "perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/jquery.qtip.js",
          "perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/lang-all.js",
          "perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/moment.min.js",
          "perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/list.min.js",
          "perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/lodash.min.js",
          "perc.widget.imageSlider/sys__UserDependency--web_resources/widgets/imageSlider/js/slick.min.js",
          "perc.widgets.image/sys__UserDependency--web_resources/widgets/image/lightbox/lightbox.js",
          "perc.widgets.nav/sys__UserDependency--web_resources/widgets/navBar/js/hoverIntent.js",
          "perc.widgets.nav/sys__UserDependency--web_resources/widgets/navBar/js/hoverIntent.min.js",
          "perc.widgets.nav/sys__UserDependency--web_resources/widgets/navBar/js/superfish.js",
          "perc.widgets.nav/sys__UserDependency--web_resources/widgets/navBar/js/superfish.min.js");

  /** First-party Percussion JS in the audited payloads — recorded so additions stand out. */
  private static final Set<String> EXPECTED_FIRST_PARTY_JS =
      Set.of(
          "perc.Baseline/SupportFile-rx_resources/controls/percQueryControl/js/percQueryControl.js",
          "perc.Baseline/SupportFile-rx_resources/js/percTagListControl.js",
          "perc.CategoryDropDownControl/SupportFile-rx_resources/widgets/categoryDropDown/js/categoryDropdown.js",
          "perc.ImageAutoListWidget/SupportFile-rx_resources/widgets/imageAutoList/js/jquery.imageAutoList.js",
          "perc.ImageAutoListWidget/sys__UserDependency--rx_resources/widgets/imageAutoList/js/jquery.imageAutoList.js",
          "perc.PageAutoListWidget/SupportFile-rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js",
          "perc.PageAutoListWidget/sys__UserDependency--rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js",
          "perc.defaultLanguage/SupportFile-rx_resources/widgets/percDefaultLang/js/percDefaultLang.js",
          "perc.defaultLanguage/SupportFile-rx_resources/widgets/percLocalLang/js/percLocalLang.js",
          "perc.defaultLanguage/sys__UserDependency--rx_resources/widgets/percDefaultLang/js/percDefaultLang.js",
          "perc.defaultLanguage/sys__UserDependency--rx_resources/widgets/percLocalLang/js/percLocalLang.js",
          "perc.widget.blogIndexPage/SupportFile-rx_resources/widgets/blogIndexPage/js/jquery.blogIndexPage.js",
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
          "perc.widget.cookieConsent/sys__UserDependency--web_resources/widgets/cookieConsent/js/cookie-consent.js",
          "perc.widget.fileAutoList/SupportFile-rx_resources/controls/percResourceAutoListControl/js/jquery.resourceAutoList.js",
          "perc.widget.fileAutoList/sys__UserDependency--rx_resources/controls/percResourceAutoListControl/js/jquery.resourceAutoList.js",
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
          "perc.widget.secureLogin/sys__UserDependency--rx_resources/widgets/percSecureLogin/js/percSecureLogin.js",
          "perc.widget.socialButtons/SupportFile-rx_resources/widgets/percSocialButtons/js/percSocialButtons.js",
          "perc.widget.socialButtons/sys__UserDependency--rx_resources/widgets/percSocialButtons/js/percSocialButtons.js",
          "perc.widget.socialButtons/sys__UserDependency--web_resources/widgets/socialButtons/js/percSocialButtons.js",
          "perc.widget.taglist/SupportFile-rx_resources/widgets/tagList/js/jquery.tagList.js",
          "perc.widget.taglist/sys__UserDependency--rx_resources/widgets/tagList/js/jquery.tagList.js",
          "perc.widgets.image/SupportFile-rx_resources/widgets/image/js/jquery.imageAssetControl.js",
          "perc.widgets.image/sys__UserDependency--rx_resources/widgets/image/js/jquery.imageAssetControl.js",
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
              "slick-initialized"),
          Map.entry(
              "perc.widgets.image/sys__UserDependency--web_resources/widgets/image/lightbox/lightbox.js",
              "Lightbox v2.10.0"),
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
              "perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/jquery.qtip.js",
              "qTip2 - Pretty powerful tooltips - v3.0.3"),
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
              "hoverIntent_t"),
          Map.entry(
              "perc.baseWidgets/sys__UserDependency--web_resources/cm/jslib/jquery-ui.js",
              "jQuery UI - v1.14.2"));

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
            + "update the expected sets and file/refresh the migration issues (#5627/#6319/#6320/#6321).");
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
  void imageLightboxCarriesXssHardening() throws IOException {
    Path root = packagesRoot();
    String rel =
        "perc.widgets.image/sys__UserDependency--web_resources/widgets/image/lightbox/lightbox.js";
    String content = Files.readString(root.resolve(rel), StandardCharsets.UTF_8);
    // Slice 10 (#6319) rebase: the payload is the stock lightbox2@2.10.0
    // dist with only the two documented #1441 selector-hardening hunks
    // plus the adjudicated empty-catch suppression comment.
    for (String marker :
        List.of(
            "Lightbox v2.10.0",
            "CodeQL js/xss-through-dom (#1441)",
            "return $(this).attr('data-lightbox') === dataLightboxValue;",
            "return $(this).attr('rel') === relValue;",
            "mkd-code-review[tests.empty-catch]",
            "$.proxy(this.sizeOverlay, this)",
            "$.proxy(this.keyboardAction, this)")) {
      assertTrue(
          content.contains(marker), rel + " missing rebased-mod marker: " + marker);
    }
    // The interpolation sinks the #1441 mod removed must not come back,
    // and the fork's prepended Percussion header stays dropped (upstream
    // banner restored, same precedent as the superfish/qTip slices).
    for (String sink : List.of("[data-lightbox=\"' +", "[rel=\"' +", "Copyright 1999-2023 Percussion")) {
      assertTrue(!content.contains(sink), rel + " still ships removed pattern: " + sink);
    }
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
