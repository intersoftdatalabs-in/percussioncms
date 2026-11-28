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

package com.percussion.packages.pagexml;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Fleet-wide JEXL binding-order conformance sweep over every real package manifest (issue #7387 /
 * parent #2626, mass-conversion gate for #7307).
 *
 * <p>Walks the real product {@code Packages} tree (not fixtures) through {@code
 * PSFleetBindingOrderConformance} and enforces the next-a contract on every {@code pages/} +
 * {@code widgets/} manifest: no unreadable/invalid manifests, no null entries, no blank variables.
 * Duplicate variables are the Velocity snippet branch-shadowing idiom (later entries shadow earlier
 * ones at assembly time, e.g. {@code $rootclass} / {@code $target} / {@code $link} reassigned
 * across conditional branches); each shadowed template carries a recorded disposition below. A
 * conversion that adds, removes, or reorders a shadowed binding fails the disposition test until an
 * operator verifies the snippet intent and updates the record — that review is the gate.
 *
 * <p>To update the disposition after verifying a snippet change: re-run the sweep, confirm the new
 * duplicate set matches the snippet's conditional branches in document order, then update {@code
 * RECORDED_SHADOWING} for that template only. Never delete a record to green the build.
 *
 * <p>Cross-platform: all path construction uses {@link Path#resolve(String)} / {@link Files} — no
 * hardcoded filesystem separators.
 */
class PSFleetBindingOrderConformanceTest {

  /**
   * Recorded shadowing disposition: template name to expected binding count plus expected
   * duplicate variables in first-duplicate encounter (document) order. Audited 2026-11-28: 40 of
   * 77 fleet templates shadow; zero null entries, zero blank variables, zero invalid manifests.
   * The count pins occurrence totals (a new shadowing occurrence changes the count); the list
   * pins the shadowed set in document order.
   */
  private static final Map<String, ShadowDisposition> RECORDED_SHADOWING =
      Map.ofEntries(
          Map.entry(
              "percFileSnippet",
              new ShadowDisposition(
                  16, List.of("rootclass", "target", "link"))),
          Map.entry(
              "percImageAutoListSnippet",
              new ShadowDisposition(
                  55, List.of("rootclass", "listclass", "listAriaRole", "autoListDivId",
                  "thumblinkclass", "captionclass", "maxlength", "sortDirection", "target",
                  "query", "titleFormatStartTag", "titleFormatEndTag", "listAriaLabel", "dsUrl"))),
          Map.entry(
              "percPageAutoListSnippet",
              new ShadowDisposition(
                  140, List.of("postDateTimeFormat", "locale", "summaryclass", "listAriaRole",
                  "rootclass", "totalMaxResults", "maxlength", "autoListDivId", "sortbyVal",
                  "sortDirection", "enableRssField", "query", "listAriaLabel",
                  "hidePastResultsFilterType", "criteria", "folderpath", "counter", "inCl",
                  "orderby", "dynamicListData"))),
          Map.entry(
              "percDefaultLangSnippet",
              new ShadowDisposition(
                  39, List.of("protocol", "countryLang", "default", "temp", "path", "xdefault",
                  "configured"))),
          Map.entry(
              "percLocalLangSnippet",
              new ShadowDisposition(
                  38, List.of("protocol", "default", "countryLang", "pageId"))),
          Map.entry(
              "percEventSnippet",
              new ShadowDisposition(
                  32, List.of("rootclass", "eventStart", "eventStartISO", "tmp", "eventEnd",
                  "eventEndISO", "dsUrl"))),
          Map.entry(
              "percOpenGraphSnippet",
              new ShadowDisposition(
                  94, List.of("rootclass", "summary", "use_site_name", "use_fb_app_id",
                  "use_locale", "use_title", "use_type", "use_url", "summary_text",
                  "use_description", "use_image", "use_image_height", "use_image_width",
                  "meta_image", "addlHead"))),
          Map.entry(
              "percTwitterSummaryCardsSnippet",
              new ShadowDisposition(
                  66, List.of("rootclass", "summary", "use_twitter_site", "use_title",
                  "use_type", "twitter_image_alt", "summary_text", "use_description",
                  "use_image", "meta_image", "use_twitter_image_alt", "addlHead"))),
          Map.entry(
              "percMostReadBlogPostsSnippet",
              new ShadowDisposition(
                  35, List.of("rootclass", "ariaLabel", "ariaRole", "data"))),
          Map.entry(
              "percResultSnippet",
              new ShadowDisposition(
                  41, List.of("rootclass", "resultEntriesTextPlural",
                  "resultEntriesTextSingular", "locale", "orderby", "data", "totalMaxResults"))),
          Map.entry(
              "percArchiveListSnippet",
              new ShadowDisposition(
                  68, List.of("archiveListMaxNum", "archiveLayout", "archiveLocale", "rootclass",
                  "query", "criteria", "folderpath", "counter", "inCl", "dynamicListData"))),
          Map.entry(
              "percBlogIndexPageSnippet",
              new ShadowDisposition(
                  121, List.of("sortByVal", "sortDirection", "listAriaLabel", "listAriaRole",
                  "autoListDivId", "resultEntriesTextPlural", "resultEntriesTextSingular",
                  "locale", "rootclass", "navLoc", "query", "perc_max_post_entries",
                  "totalMaxResults", "criteria", "folderpath", "counter", "orderby",
                  "dynamicListData"))),
          Map.entry(
              "percCalendarTwoSnippet",
              new ShadowDisposition(
                  49, List.of("perc_time_format", "id", "calTitle", "calStartDate", "pagesInCal",
                  "rootclass"))),
          Map.entry(
              "percCategoryListSnippet",
              new ShadowDisposition(
                  63, List.of("maxCat", "rootclass", "query", "criteria", "folderpath",
                  "counter", "inCl", "dynamicListData"))),
          Map.entry(
              "percCommentsFormSnippet",
              new ShadowDisposition(
                  21, List.of("rootclass", "labelalignclass", "counter", "pagePath"))),
          Map.entry(
              "percCookieConsentSnippet",
              new ShadowDisposition(
                  30, List.of("overlayPosClass", "privacyPolicyLinkText", "cookiePolicyLinkText",
                  "dynamicListData"))),
          Map.entry(
              "percDepartmentSnippet",
              new ShadowDisposition(
                  68, List.of("orgPage_path", "orgPage_title", "dptAddress3", "dptWebsite_title",
                  "dptWebsite_path"))),
          Map.entry(
              "percDirectorySnippet",
              new ShadowDisposition(
                  51, List.of("customPlaceholderImg_alt_text", "customPlaceholderImg_title",
                  "doNotShowDeptFilter", "params", "finderName", "orgName", "query"))),
          Map.entry(
              "percOrganizationSnippet",
              new ShadowDisposition(
                  65, List.of("orgAddress3", "orgLogo_alt_text", "orgLogo_title",
                  "orgPage_title", "orgPage_path"))),
          Map.entry(
              "percPersonSnippet",
              new ShadowDisposition(
                  74, List.of("personName", "emailFormPage_title", "orgName", "params", "query",
                  "finderName", "dptName", "personImage_alt_text", "personImage_title",
                  "personPage_title"))),
          Map.entry(
              "percFileAutoListSnippet",
              new ShadowDisposition(
                  47, List.of("locale", "rootclass", "autoListDivId", "listAriaRole",
                  "listclass", "maxlength", "target", "query", "listAriaLabel"))),
          Map.entry(
              "percFormSnippet",
              new ShadowDisposition(
                  8, List.of("rootclass", "labelalignclass"))),
          Map.entry(
              "percIframeSnippet",
              new ShadowDisposition(
                  33, List.of("rootclass", "iframeurl", "frameborder", "overflow", "height",
                  "width", "marginheight", "marginwidth", "dsUrl"))),
          Map.entry(
              "percImageSliderSnippet",
              new ShadowDisposition(
                  55, List.of("rootclass", "captionclass", "perc_slide_caption_alignment",
                  "perc_slide_fade", "perc_slide_show_number_desktop",
                  "perc_slide_show_number_tablet", "perc_slide_show_number_mobile",
                  "sliderJsonData", "json", "validNumbers"))),
          Map.entry(
              "percLoginSnippet",
              new ShadowDisposition(
                  26, List.of("loginPage", "pwresetPage", "isLoginPage", "rootclass", "dsUrl"))),
          Map.entry(
              "percPollSnippet",
              new ShadowDisposition(
                  24, List.of("pollQuestion", "inputType"))),
          Map.entry(
              "percRegistrationSnippet",
              new ShadowDisposition(
                  31, List.of("rootclass", "siteName", "percConfirmationPage", "pwResetPage",
                  "assetItems", "dsUrl"))),
          Map.entry(
              "percRssSnippet",
              new ShadowDisposition(
                  37, List.of("itemElement", "titleElement", "itemTitleElement",
                  "itemDateElement", "itemDescriptionElement", "feedId", "ariaRole",
                  "percUrlFeed", "percFeedDescriptionLength"))),
          Map.entry(
              "percSecureLoginSnippet",
              new ShadowDisposition(
                  34, List.of("rootclass", "labelclass", "inputclass", "buttonclass",
                  "labelalignclass", "pwresetPage", "pwresetPageLink", "forgotPasswordLinkHTML",
                  "dsUrl"))),
          Map.entry("percSocialButtonsSnippet", new ShadowDisposition(22, List.of("outputColor"))),
          Map.entry(
              "percTagListSnippet",
              new ShadowDisposition(
                  57, List.of("rootclass", "criteria", "folderpath", "counter", "inCl",
                  "dynamicListData"))),
          Map.entry("percTitleSnippet", new ShadowDisposition(8, List.of("dsUrl"))),
          Map.entry(
              "percRedirectSnippet",
              new ShadowDisposition(
                  10, List.of("enabled", "delayInSeconds"))),
          Map.entry(
              "percBlogPostSnippet",
              new ShadowDisposition(
                  89, List.of("locale", "newerText", "olderText", "olderPostClass",
                  "newerPostClass", "navLoc", "navSelf", "blogIndexName", "blogPostTempId",
                  "criteria", "pagePath", "siteName", "folderpath", "counter", "tagValues",
                  "categoryValues", "dynamicListData"))),
          Map.entry("percCommentsSnippet", new ShadowDisposition(10, List.of("rootclass", "dUrl"))),
          Map.entry(
              "percImageSnippet",
              new ShadowDisposition(
                  31, List.of("img_alt", "img_title", "img_title_attr", "img_height",
                  "img_width", "link", "biglink", "dsUrl"))),
          Map.entry(
              "percLikedSnippet",
              new ShadowDisposition(
                  15, List.of("rootclass", "sampleTooltip", "dsUrl"))),
          Map.entry("simplePageAutoListSnippet", new ShadowDisposition(11, List.of("rootclass"))),
          Map.entry(
              "percNavBarSnippet",
              new ShadowDisposition(
                  28, List.of("generateSkipLink", "titleNavigateToText", "addTitleAttrs",
                  "maxlevels", "rootlevel", "rootclass", "sampleTooltip"))),
          Map.entry(
              "percNavBreadcrumbSnippet",
              new ShadowDisposition(
                  14, List.of("rootclass", "sampleTooltip"))));


  /**
   * Recorded disposition for one shadowed template: audited binding count plus expected duplicate
   * variables in first-duplicate encounter (document) order.
   */
  private record ShadowDisposition(int bindingCount, List<String> duplicates) {
    ShadowDisposition {
      duplicates = List.copyOf(duplicates);
    }
  }

  @Test
  void fleet_hasNoHardBindingDefects() throws IOException {
    PSFleetBindingOrderConformance.Report report = scanFleet();
    assertFalse(
        report.hasHardDefects(),
        () -> "fleet hard binding defects (fix manifests in place): "
            + System.lineSeparator()
            + report.summarize());
    assertTrue(
        report.getManifestErrorFindings().isEmpty(),
        () -> "unreadable/invalid fleet manifests: " + report.summarize());
    assertTrue(
        report.getHardBindingDefectFindings().isEmpty(),
        () -> "null/blank fleet binding defects: " + report.summarize());
  }

  @Test
  void fleet_shadowingMatchesRecordedDisposition() throws IOException {
    PSFleetBindingOrderConformance.Report report = scanFleet();
    List<String> mismatches = collectDispositionMismatches(report);
    assertTrue(
        mismatches.isEmpty(),
        () -> "shadowing drift — verify the snippet's conditional branches in document order, "
            + "then update RECORDED_SHADOWING for that template only: "
            + mismatches);
    List<String> stale = collectStaleDispositionRecords(report);
    assertTrue(
        stale.isEmpty(),
        () -> "stale shadowing records for templates no longer in the fleet "
            + "(remove the record): " + stale);
  }

  private static List<String> collectDispositionMismatches(
      PSFleetBindingOrderConformance.Report report) {
    List<String> mismatches = new ArrayList<>();
    for (PSFleetBindingOrderConformance.TemplateFinding finding : report.getFindings()) {
      String mismatch = describeDispositionMismatch(finding);
      if (mismatch != null) {
        mismatches.add(mismatch);
      }
    }
    return mismatches;
  }

  private static String describeDispositionMismatch(
      PSFleetBindingOrderConformance.TemplateFinding finding) {
    if (finding.hasManifestErrors() || finding.getTemplateName() == null) {
      return null;
    }
    List<String> actual = finding.getResult().getDuplicateVariables();
    int actualCount = finding.getResult().getBindingCount();
    ShadowDisposition expected = RECORDED_SHADOWING.get(finding.getTemplateName());
    if (expected == null) {
      return actual.isEmpty()
          ? null
          : finding.getDisplayKey()
              + " template '"
              + finding.getTemplateName()
              + "': unrecorded shadowing "
              + actual
              + " — verify the snippet's conditional branches in document order, then add a"
              + " RECORDED_SHADOWING entry for that template";
    }
    if (expected.duplicates().equals(actual) && expected.bindingCount() == actualCount) {
      return null;
    }
    return finding.getDisplayKey()
        + " template '"
        + finding.getTemplateName()
        + "': recorded "
        + expected.bindingCount()
        + " bindings/shadowing "
        + expected.duplicates()
        + " but swept "
        + actualCount
        + " bindings/shadowing "
        + actual;
  }

  private static List<String> collectStaleDispositionRecords(
      PSFleetBindingOrderConformance.Report report) {
    Set<String> seen = new HashSet<>();
    for (PSFleetBindingOrderConformance.TemplateFinding finding : report.getFindings()) {
      if (finding.getTemplateName() != null) {
        seen.add(finding.getTemplateName());
      }
    }
    List<String> stale = new ArrayList<>();
    for (String recorded : RECORDED_SHADOWING.keySet()) {
      if (!seen.contains(recorded)) {
        stale.add(recorded);
      }
    }
    return stale;
  }

  @Test
  void fleet_coversAllPagesAndWidgetsManifests() throws IOException {
    PSFleetBindingOrderConformance.Report report = scanFleet();
    assertFalse(
        report.getFindings().isEmpty(), "fleet sweep must discover at least one manifest");
    for (PSFleetBindingOrderConformance.TemplateFinding finding : report.getFindings()) {
      String kind = finding.getKindDirName();
      assertTrue(
          "pages".equals(kind) || "widgets".equals(kind),
          () -> "sweep scope is pages/ + widgets/ only: " + finding.getDisplayKey());
    }
    // Audited floor (2026-11-28): 77 single-template manifests. The fleet only grows by
    // conversion; a smaller count means manifests were deleted — say so explicitly.
    assertTrue(
        report.getManifestCount() >= 77,
        () -> "fleet shrank below the audited 77 manifests: " + report.summarize());
  }

  private static PSFleetBindingOrderConformance.Report scanFleet() throws IOException {
    Path packagesRoot = locatePackagesRoot();
    assertNotNull(packagesRoot, "Packages tree must be visible under perc-packages Surefire");
    return PSFleetBindingOrderConformance.scan(packagesRoot);
  }

  private static Path locatePackagesRoot() {
    Path candidate = Path.of("src", "main", "resources", "Packages");
    if (Files.isDirectory(candidate)) {
      return candidate.toAbsolutePath().normalize();
    }
    Path cwd = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
    Path alt =
        cwd.resolve("src").resolve("main").resolve("resources").resolve("Packages");
    return Files.isDirectory(alt) ? alt : null;
  }
}
