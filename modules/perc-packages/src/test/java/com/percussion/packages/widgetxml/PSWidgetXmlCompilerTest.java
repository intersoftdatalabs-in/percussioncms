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

package com.percussion.packages.widgetxml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.percussion.packages.manifest.PSComponentPackageManifest;
import com.percussion.packages.manifest.PSComponentPackageManifestIo;
import com.percussion.packages.manifest.PSComponentPackageManifestValidator;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit + golden parity tests for Widget XML → Component Package Manifest compiler (#2751
 * baseWidgets, #2772 high-traffic residual batch, #2789 residual product batch, #2802 remaining
 * product residual batch, #2830 perc.Test residual).
 */
class PSWidgetXmlCompilerTest {

  private static final String FIXTURE_SIMPLE = "/widgetxml/percSimpleText.xml";
  private static final String GOLDEN_SIMPLE_MANIFEST =
      "/widgetxml/golden/percSimpleText.component-package.json";
  private static final String GOLDEN_SIMPLE_TEMPLATE =
      "/widgetxml/golden/percSimpleTextSnippet.vm";

  private static final String FIXTURE_TITLE = "/widgetxml/percTitle.xml";
  private static final String GOLDEN_TITLE_MANIFEST =
      "/widgetxml/golden/percTitle.component-package.json";
  private static final String GOLDEN_TITLE_TEMPLATE = "/widgetxml/golden/percTitleSnippet.vm";

  private static final String FIXTURE_LIST = "/widgetxml/simplePageAutoList.xml";
  private static final String GOLDEN_LIST_MANIFEST =
      "/widgetxml/golden/simplePageAutoList.component-package.json";
  private static final String GOLDEN_LIST_TEMPLATE =
      "/widgetxml/golden/simplePageAutoListSnippet.vm";

  private static final String FIXTURE_BREADCRUMB = "/widgetxml/percNavBreadcrumb.xml";
  private static final String GOLDEN_BREADCRUMB_MANIFEST =
      "/widgetxml/golden/percNavBreadcrumb.component-package.json";
  private static final String GOLDEN_BREADCRUMB_TEMPLATE =
      "/widgetxml/golden/percNavBreadcrumbSnippet.vm";

  private static final String FIXTURE_FORM = "/widgetxml/percForm.xml";
  private static final String GOLDEN_FORM_MANIFEST =
      "/widgetxml/golden/percForm.component-package.json";
  private static final String GOLDEN_FORM_TEMPLATE = "/widgetxml/golden/percFormSnippet.vm";

  private static final String FIXTURE_POLL = "/widgetxml/percPoll.xml";
  private static final String GOLDEN_POLL_MANIFEST =
      "/widgetxml/golden/percPoll.component-package.json";
  private static final String GOLDEN_POLL_TEMPLATE = "/widgetxml/golden/percPollSnippet.vm";

  private static final String FIXTURE_IFRAME = "/widgetxml/percIframe.xml";
  private static final String GOLDEN_IFRAME_MANIFEST =
      "/widgetxml/golden/percIframe.component-package.json";
  private static final String GOLDEN_IFRAME_TEMPLATE = "/widgetxml/golden/percIframeSnippet.vm";

  // #2802 remaining residual representatives
  private static final String FIXTURE_IMAGE_AUTO_LIST = "/widgetxml/percImageAutoList.xml";
  private static final String GOLDEN_IMAGE_AUTO_LIST_MANIFEST =
      "/widgetxml/golden/percImageAutoList.component-package.json";
  private static final String GOLDEN_IMAGE_AUTO_LIST_TEMPLATE =
      "/widgetxml/golden/percImageAutoListSnippet.vm";

  private static final String FIXTURE_COMMENTS = "/widgetxml/percComments.xml";
  private static final String GOLDEN_COMMENTS_MANIFEST =
      "/widgetxml/golden/percComments.component-package.json";
  private static final String GOLDEN_COMMENTS_TEMPLATE = "/widgetxml/golden/percCommentsSnippet.vm";

  private static final String FIXTURE_EVENT = "/widgetxml/percEvent.xml";
  private static final String GOLDEN_EVENT_MANIFEST =
      "/widgetxml/golden/percEvent.component-package.json";
  private static final String GOLDEN_EVENT_TEMPLATE = "/widgetxml/golden/percEventSnippet.vm";

  // #2830 perc.Test residual
  private static final String FIXTURE_TEST_PROPERTIES = "/widgetxml/PSWidget_TestProperties.xml";
  private static final String GOLDEN_TEST_PROPERTIES_MANIFEST =
      "/widgetxml/golden/PSWidget_TestProperties.component-package.json";
  private static final String GOLDEN_TEST_PROPERTIES_TEMPLATE =
      "/widgetxml/golden/PSWidget_TestPropertiesSnippet.vm";

  @TempDir Path tempDir;

  @Test
  void parseSimpleText_populatesPrefsCodeAndContent() throws Exception {
    PSWidgetXmlModel model = parseClasspath(FIXTURE_SIMPLE, "percSimpleText.xml");

    assertEquals("Simple Text", model.getTitle());
    assertEquals("percSimpleTextAsset", model.getContentTypeName());
    assertEquals("content", model.getCategory());
    assertEquals("jexl", model.getCodeType());
    assertEquals("velocity", model.getContentType());
    assertNotNull(model.getCodeBody());
    assertTrue(model.getCodeBody().contains("$rootclass"));
    assertNotNull(model.getContentBody());
    assertTrue(model.getContentBody().contains("#loadRelatedWidgetContents()"));
    assertEquals(1, model.getCssPrefs().size());
    assertEquals("rootclass", model.getCssPrefs().get(0).getName());
    assertEquals("percSimpleText", model.widgetStem());
  }

  @Test
  void compileSimpleText_matchesGoldenManifestAndTemplate() throws Exception {
    PSWidgetXmlModel model = parseClasspath(FIXTURE_SIMPLE, "percSimpleText.xml");
    PSWidgetXmlPackageContext ctx = baseWidgetsLikeContext();

    PSWidgetXmlCompileResult result = PSWidgetXmlCompiler.compile(model, ctx);
    PSComponentPackageManifest manifest = result.getManifest();

    PSComponentPackageManifestValidator.validate(manifest);

    String expectedJson = readClasspath(GOLDEN_SIMPLE_MANIFEST);
    PSComponentPackageManifest golden = PSComponentPackageManifestIo.parse(expectedJson);

    assertEquals(golden.getSchemaVersion(), manifest.getSchemaVersion());
    assertEquals(golden.getId(), manifest.getId());
    assertEquals(golden.getName(), manifest.getName());
    assertEquals(golden.getVersion(), manifest.getVersion());
    assertEquals(golden.getDescription(), manifest.getDescription());
    assertEquals(golden.getPublisher(), manifest.getPublisher());
    assertEquals(golden.getCmsVersion(), manifest.getCmsVersion());
    assertEquals(golden.getCatalog(), manifest.getCatalog());
    assertEquals(golden.getContentTypes(), manifest.getContentTypes());
    assertEquals(golden.getTemplates().size(), manifest.getTemplates().size());
    assertEquals(golden.getTemplates().get(0).getName(), manifest.getTemplates().get(0).getName());
    assertEquals(
        golden.getTemplates().get(0).getAssembler(),
        manifest.getTemplates().get(0).getAssembler());
    assertEquals(
        golden.getTemplates().get(0).getSourceRef(),
        manifest.getTemplates().get(0).getSourceRef());
    assertEquals(
        golden.getTemplates().get(0).getContentType(),
        manifest.getTemplates().get(0).getContentType());
    assertEquals(
        golden.getTemplates().get(0).getBindings(),
        manifest.getTemplates().get(0).getBindings());
    assertEquals(golden.getSlots(), manifest.getSlots());
    assertEquals(golden.getResources(), manifest.getResources());
    assertEquals(golden.getCssPreferences(), manifest.getCssPreferences());
    assertEquals(golden.getUserPreferences(), manifest.getUserPreferences());

    // Full structural equality (after collection normalize via toJson/parse).
    PSComponentPackageManifest reparsed =
        PSComponentPackageManifestIo.parse(PSComponentPackageManifestIo.toJson(manifest));
    assertEquals(
        PSComponentPackageManifestIo.parse(expectedJson),
        reparsed,
        "compiled manifest must equal golden fixture");

    String expectedTemplate = normalizeNewlines(readClasspath(GOLDEN_SIMPLE_TEMPLATE));
    String actualTemplate =
        normalizeNewlines(result.getTextArtifacts().get("templates/percSimpleTextSnippet.vm"));
    assertEquals(expectedTemplate, actualTemplate, "template source golden parity");
  }

  @Test
  void compileSimpleText_writeArtifacts_roundTrips() throws Exception {
    PSWidgetXmlModel model = parseClasspath(FIXTURE_SIMPLE, "percSimpleText.xml");
    PSWidgetXmlCompileResult result =
        PSWidgetXmlCompiler.compile(model, baseWidgetsLikeContext());

    Path out = tempDir.resolve("simple-out");
    PSWidgetXmlCompiler.writeArtifacts(result, out);

    Path manifestPath = out.resolve(PSComponentPackageManifest.DEFAULT_MANIFEST_FILE_NAME);
    assertTrue(Files.isRegularFile(manifestPath));
    PSComponentPackageManifest loaded = PSComponentPackageManifestIo.read(manifestPath);
    PSComponentPackageManifestValidator.validate(loaded);
    assertEquals(result.getManifest().getId(), loaded.getId());

    Path template = out.resolve("templates").resolve("percSimpleTextSnippet.vm");
    assertTrue(Files.isRegularFile(template));
    assertEquals(
        normalizeNewlines(result.getTextArtifacts().get("templates/percSimpleTextSnippet.vm")),
        normalizeNewlines(Files.readString(template, StandardCharsets.UTF_8)));
  }

  @Test
  void compileRawHtml_noCodeStillValid_withContentTypeAndTemplate() throws Exception {
    PSWidgetXmlModel model = parseClasspath("/widgetxml/percRawHtml.xml", "percRawHtml.xml");
    PSWidgetXmlCompileResult result =
        PSWidgetXmlCompiler.compile(model, baseWidgetsLikeContext());

    PSComponentPackageManifestValidator.validate(result.getManifest());
    assertEquals("percRawHtml", result.getManifest().getId());
    assertEquals("HTML", result.getManifest().getName());
    assertEquals(1, result.getManifest().getContentTypes().size());
    assertEquals("percRawHtmlAsset", result.getManifest().getContentTypes().get(0).getName());
    assertEquals(1, result.getManifest().getTemplates().size());
    assertTrue(result.getManifest().getTemplates().get(0).getBindings().isEmpty());
    assertTrue(
        result
            .getTextArtifacts()
            .get("templates/percRawHtmlSnippet.vm")
            .contains("#loadRelatedWidgetContents()"));
  }

  @Test
  void compileBaseWidgetsPackage_threeWidgetsAllValid() throws Exception {
    Path packageDir = locateBaseWidgetsPackage();
    if (packageDir == null) {
      // Module resources layout not present in this environment — skip with explicit fail soft.
      // Night/CI worktree always has package sources under modules/perc-packages.
      System.err.println("WARN: perc.baseWidgets package sources not found; skipping package test");
      return;
    }

    List<PSWidgetXmlCompileResult> results = PSWidgetXmlPackageCompiler.compilePackage(packageDir);
    assertEquals(3, results.size(), "baseWidgets should have RawHtml, RichText, SimpleText");

    Map<String, PSWidgetXmlCompileResult> byId =
        results.stream()
            .collect(Collectors.toMap(r -> r.getManifest().getId(), r -> r, (a, b) -> a));

    assertTrue(byId.containsKey("percRawHtml"));
    assertTrue(byId.containsKey("percRichText"));
    assertTrue(byId.containsKey("percSimpleText"));

    for (PSWidgetXmlCompileResult r : results) {
      PSComponentPackageManifestValidator.validate(r.getManifest());
      assertEquals("1.1.9", r.getManifest().getVersion());
      assertNotNull(r.getManifest().getPublisher());
      assertEquals("velocityAssembler", r.getManifest().getTemplates().get(0).getAssembler());
      assertFalse(r.getTextArtifacts().isEmpty());
    }

    // Rich Text carries CssPref + JEXL code.
    PSWidgetXmlCompileResult rich = byId.get("percRichText");
    assertEquals(1, rich.getManifest().getCssPreferences().size());
    assertEquals("rootclass", rich.getManifest().getCssPreferences().get(0).getName());
    assertTrue(
        rich.getManifest().getTemplates().get(0).getBindings().stream()
            .anyMatch(
                b -> PSWidgetXmlCompiler.FULL_CODE_BINDING_VARIABLE.equals(b.getVariable())));
    assertTrue(
        rich.getTextArtifacts()
            .get("templates/percRichTextSnippet.vm")
            .contains("$node.getProperty('rx:text')"));

    Path outRoot = tempDir.resolve("baseWidgets-out");
    PSWidgetXmlPackageCompiler.writeAll(results, outRoot);
    assertTrue(
        Files.isRegularFile(
            outRoot
                .resolve("percSimpleText")
                .resolve(PSComponentPackageManifest.DEFAULT_MANIFEST_FILE_NAME)));
  }

  @Test
  void parseEmptyXml_throws() {
    assertThrows(PSWidgetXmlException.class, () -> PSWidgetXmlParser.parse("   "));
  }

  @Test
  void parseNonWidgetRoot_throws() {
    assertThrows(
        PSWidgetXmlException.class, () -> PSWidgetXmlParser.parse("<NotWidget/>"));
  }

  @Test
  void toPackageRelativePath_stripsLeadingSlash() {
    assertEquals(
        "rx_resources/widgets/x.png",
        PSWidgetXmlCompiler.toPackageRelativePath("/rx_resources/widgets/x.png"));
    assertEquals(
        "rx_resources/widgets/x.png",
        PSWidgetXmlCompiler.toPackageRelativePath("rx_resources/widgets/x.png"));
  }

  @Test
  void mapAssembler_velocityHtmlMarkdown() {
    assertEquals("velocityAssembler", PSWidgetXmlCompiler.mapAssembler("velocity"));
    assertEquals("htmlAssembler", PSWidgetXmlCompiler.mapAssembler("html"));
    assertEquals("markdownAssembler", PSWidgetXmlCompiler.mapAssembler("markdown"));
  }

  @Test
  void parseTitle_userPrefEnumsAndCreateSharedAsset() throws Exception {
    PSWidgetXmlModel model = parseClasspath(FIXTURE_TITLE, "percTitle.xml");
    assertEquals("Title", model.getTitle());
    assertEquals("percTitleAsset", model.getContentTypeName());
    assertEquals(1, model.getUserPrefs().size());
    assertEquals("wrapper", model.getUserPrefs().get(0).getName());
    assertEquals("enum", model.getUserPrefs().get(0).getDatatype());
    assertEquals(8, model.getUserPrefs().get(0).getEnumValues().size());
    assertEquals(Boolean.FALSE, model.getCreateSharedAsset());
    assertTrue(model.getResources().isEmpty());
  }

  @Test
  void compileTitle_matchesGoldenManifestAndTemplate() throws Exception {
    assertGoldenParity(
        FIXTURE_TITLE,
        "percTitle.xml",
        "perc.widget.title",
        GOLDEN_TITLE_MANIFEST,
        GOLDEN_TITLE_TEMPLATE,
        "templates/percTitleSnippet.vm");
  }

  @Test
  void parseSimplePageAutoList_resourcesAndLayoutPrefs() throws Exception {
    PSWidgetXmlModel model = parseClasspath(FIXTURE_LIST, "simplePageAutoList.xml");
    assertEquals("Auto List", model.getTitle());
    assertEquals(1, model.getResources().size());
    assertEquals("/rx_resources/widgets/simpleList/css/style.css", model.getResources().get(0).getHref());
    assertEquals("css", model.getResources().get(0).getType());
    assertEquals("head", model.getResources().get(0).getPlacement());
    assertTrue(model.getUserPrefs().stream().anyMatch(p -> "layout".equals(p.getName())));
    assertTrue(model.getUserPrefs().stream().anyMatch(p -> "maxlength".equals(p.getName())));
  }

  @Test
  void compileSimplePageAutoList_matchesGolden_andEmitsCssResource() throws Exception {
    PSWidgetXmlCompileResult result =
        assertGoldenParity(
            FIXTURE_LIST,
            "simplePageAutoList.xml",
            "perc.widgets.lists",
            GOLDEN_LIST_MANIFEST,
            GOLDEN_LIST_TEMPLATE,
            "templates/simplePageAutoListSnippet.vm");
    assertTrue(
        result.getManifest().getResources().stream()
            .anyMatch(r -> "css".equals(r.getType()) && r.getTarget().endsWith("style.css")),
        "list widget must emit declared CSS Resource");
    assertEquals(
        "0",
        String.valueOf(result.getManifest().getSlots().get(0).getLayout().get("maxlength")));
  }

  @Test
  void parseNavBreadcrumb_chromeWidgetNoContentType_withResource() throws Exception {
    PSWidgetXmlModel model = parseClasspath(FIXTURE_BREADCRUMB, "percNavBreadcrumb.xml");
    assertEquals("Breadcrumb", model.getTitle());
    assertTrue(
        model.getContentTypeName() == null || model.getContentTypeName().isBlank(),
        "nav breadcrumb is chrome (no asset CT)");
    assertEquals(1, model.getResources().size());
    assertEquals("css", model.getResources().get(0).getType());
    assertFalse(model.getUserPrefs().isEmpty());
  }

  @Test
  void compileNavBreadcrumb_matchesGolden_chromeSlotAndCssResource() throws Exception {
    PSWidgetXmlCompileResult result =
        assertGoldenParity(
            FIXTURE_BREADCRUMB,
            "percNavBreadcrumb.xml",
            "perc.widgets.nav",
            GOLDEN_BREADCRUMB_MANIFEST,
            GOLDEN_BREADCRUMB_TEMPLATE,
            "templates/percNavBreadcrumbSnippet.vm");
    assertTrue(
        result.getManifest().getContentTypes() == null
            || result.getManifest().getContentTypes().isEmpty());
    assertEquals("percNavBreadcrumbChrome", result.getManifest().getSlots().get(0).getName());
    assertTrue(
        result.getManifest().getResources().stream().anyMatch(r -> "css".equals(r.getType())));
  }

  @Test
  void compileHighTrafficPackages_allValidate() throws Exception {
    Path packagesRoot = locatePackagesRoot();
    if (packagesRoot == null) {
      System.err.println("WARN: Packages root not found; skipping high-traffic package test");
      return;
    }

    List<PSWidgetXmlCompileResult> results =
        PSWidgetXmlPackageCompiler.compileHighTrafficPackages(packagesRoot);
    // title(1) + lists(2) + nav(2) + file(1) + image(1) = 7 widgets
    assertEquals(
        7,
        results.size(),
        "high-traffic batch should compile title, lists×2, nav×2, file, image");

    Map<String, PSWidgetXmlCompileResult> byId =
        results.stream()
            .collect(Collectors.toMap(r -> r.getManifest().getId(), r -> r, (a, b) -> a));

    for (String id :
        List.of(
            "percTitle",
            "simplePageAutoList",
            "simpleTextAutoList",
            "percNavBar",
            "percNavBreadcrumb",
            "percFile",
            "percImage")) {
      assertTrue(byId.containsKey(id), "missing compiled widget: " + id);
      PSComponentPackageManifestValidator.validate(byId.get(id).getManifest());
      assertFalse(byId.get(id).getTextArtifacts().isEmpty());
      assertEquals("velocityAssembler", byId.get(id).getManifest().getTemplates().get(0).getAssembler());
    }

    // Image declares CSS Resource + asset CT.
    assertTrue(
        byId.get("percImage").getManifest().getResources().stream()
            .anyMatch(r -> "css".equals(r.getType())));
    assertEquals("percImageAsset", byId.get("percImage").getManifest().getContentTypes().get(0).getName());

    // NavBar is chrome (no CT) with styles slot.
    assertTrue(
        byId.get("percNavBar").getManifest().getContentTypes() == null
            || byId.get("percNavBar").getManifest().getContentTypes().isEmpty());
    assertEquals("percNavBarChrome", byId.get("percNavBar").getManifest().getSlots().get(0).getName());

    Path outRoot = tempDir.resolve("high-traffic-out");
    PSWidgetXmlPackageCompiler.writeAll(results, outRoot);
    for (String stem :
        List.of(
            "percTitle",
            "simplePageAutoList",
            "simpleTextAutoList",
            "percNavBar",
            "percNavBreadcrumb",
            "percFile",
            "percImage")) {
      assertTrue(
          Files.isRegularFile(
              outRoot
                  .resolve(stem)
                  .resolve(PSComponentPackageManifest.DEFAULT_MANIFEST_FILE_NAME)),
          "writeAll missing manifest for " + stem);
    }
  }

  @Test
  void parseForm_integrationWidgetWithLabelAlignPref() throws Exception {
    PSWidgetXmlModel model = parseClasspath(FIXTURE_FORM, "percForm.xml");
    assertEquals("Form", model.getTitle());
    assertEquals("percFormAsset", model.getContentTypeName());
    assertEquals("integration", model.getCategory());
    assertEquals(1, model.getUserPrefs().size());
    assertEquals("labelalign", model.getUserPrefs().get(0).getName());
    assertEquals(1, model.getCssPrefs().size());
  }

  @Test
  void compileForm_matchesGoldenManifestAndTemplate() throws Exception {
    PSWidgetXmlCompileResult result =
        assertGoldenParity(
            FIXTURE_FORM,
            "percForm.xml",
            "perc.widget.form",
            GOLDEN_FORM_MANIFEST,
            GOLDEN_FORM_TEMPLATE,
            "templates/percFormSnippet.vm");
    assertEquals("percFormAsset", result.getManifest().getContentTypes().get(0).getName());
    assertEquals("percFormContent", result.getManifest().getSlots().get(0).getName());
    assertTrue(
        result
            .getTextArtifacts()
            .get("templates/percFormSnippet.vm")
            .contains("perc-form"));
  }

  @Test
  void parsePoll_socialBlogWidgetWithRestrictionPref() throws Exception {
    PSWidgetXmlModel model = parseClasspath(FIXTURE_POLL, "percPoll.xml");
    assertEquals("Polls", model.getTitle());
    assertEquals("percPollAsset", model.getContentTypeName());
    assertTrue(model.getCategory() != null && model.getCategory().contains("social"));
    assertEquals(7, model.getUserPrefs().size());
    assertTrue(
        model.getUserPrefs().stream().anyMatch(p -> "pollRestrictionType".equals(p.getName())));
  }

  @Test
  void compilePoll_matchesGoldenManifestAndTemplate() throws Exception {
    PSWidgetXmlCompileResult result =
        assertGoldenParity(
            FIXTURE_POLL,
            "percPoll.xml",
            "perc.widget.poll",
            GOLDEN_POLL_MANIFEST,
            GOLDEN_POLL_TEMPLATE,
            "templates/percPollSnippet.vm");
    assertEquals("percPollAsset", result.getManifest().getContentTypes().get(0).getName());
    assertTrue(
        result.getManifest().getUserPreferences().stream()
            .anyMatch(p -> "pollRestrictionType".equals(p.getName())));
    assertTrue(
        result
            .getTextArtifacts()
            .get("templates/percPollSnippet.vm")
            .contains("perc-polls"));
  }

  @Test
  void parseIframe_chromeWidgetNoContentType_withUserPrefs() throws Exception {
    PSWidgetXmlModel model = parseClasspath(FIXTURE_IFRAME, "percIframe.xml");
    assertEquals("Iframe", model.getTitle());
    assertTrue(
        model.getContentTypeName() == null || model.getContentTypeName().isBlank(),
        "iframe is chrome/logic (no asset CT)");
    assertEquals(11, model.getUserPrefs().size());
    assertEquals(1, model.getCssPrefs().size());
  }

  @Test
  void compileIframe_matchesGolden_chromeSlotWithoutContentType() throws Exception {
    PSWidgetXmlCompileResult result =
        assertGoldenParity(
            FIXTURE_IFRAME,
            "percIframe.xml",
            "perc.widget.iframe",
            GOLDEN_IFRAME_MANIFEST,
            GOLDEN_IFRAME_TEMPLATE,
            "templates/percIframeSnippet.vm");
    assertTrue(
        result.getManifest().getContentTypes() == null
            || result.getManifest().getContentTypes().isEmpty());
    assertEquals("percIframeChrome", result.getManifest().getSlots().get(0).getName());
    assertTrue(
        result.getManifest().getSlots().get(0).getStyles().containsKey("rootclass"));
  }

  @Test
  void compileResidualProductPackages_allValidate() throws Exception {
    Path packagesRoot = locatePackagesRoot();
    if (packagesRoot == null) {
      System.err.println("WARN: Packages root not found; skipping residual product package test");
      return;
    }

    List<PSWidgetXmlCompileResult> results =
        PSWidgetXmlPackageCompiler.compileResidualProductPackages(packagesRoot);
    // blog(1)+calendar(2)+directory(4)+social(1)+form(1)+poll(1)+login(1)+rss(1)+iframe(1)=13
    assertEquals(
        13,
        results.size(),
        "residual #2789 batch should compile blog, calendar×2, directory×4, social, form, poll,"
            + " login, rss, iframe");

    Map<String, PSWidgetXmlCompileResult> byId =
        results.stream()
            .collect(Collectors.toMap(r -> r.getManifest().getId(), r -> r, (a, b) -> a));

    for (String id :
        List.of(
            "percBlogPost",
            "percCalendar",
            "percCalendarTwo",
            "percDepartment",
            "percDirectory",
            "percOrganization",
            "percPerson",
            "percSocialButtons",
            "percForm",
            "percPoll",
            "percLogin",
            "percRss",
            "percIframe")) {
      assertTrue(byId.containsKey(id), "missing compiled residual widget: " + id);
      PSComponentPackageManifestValidator.validate(byId.get(id).getManifest());
      assertFalse(byId.get(id).getTextArtifacts().isEmpty());
      assertEquals(
          "velocityAssembler",
          byId.get(id).getManifest().getTemplates().get(0).getAssembler());
    }

    // Blog post is content CT with many user prefs (title format, locales, …).
    assertEquals(
        "percBlogPostAsset",
        byId.get("percBlogPost").getManifest().getContentTypes().get(0).getName());
    assertTrue(byId.get("percBlogPost").getManifest().getUserPreferences().size() >= 10);

    // Directory multi-widget package shares package context version.
    String dirVersion = byId.get("percDirectory").getManifest().getVersion();
    assertEquals(dirVersion, byId.get("percPerson").getManifest().getVersion());
    assertEquals(dirVersion, byId.get("percDepartment").getManifest().getVersion());

    // Iframe residual golden shape: chrome slot, no CT.
    assertTrue(
        byId.get("percIframe").getManifest().getContentTypes() == null
            || byId.get("percIframe").getManifest().getContentTypes().isEmpty());
    assertEquals(
        "percIframeChrome", byId.get("percIframe").getManifest().getSlots().get(0).getName());

    Path outRoot = tempDir.resolve("residual-out");
    PSWidgetXmlPackageCompiler.writeAll(results, outRoot);
    for (String stem :
        List.of(
            "percBlogPost",
            "percCalendar",
            "percCalendarTwo",
            "percDepartment",
            "percDirectory",
            "percOrganization",
            "percPerson",
            "percSocialButtons",
            "percForm",
            "percPoll",
            "percLogin",
            "percRss",
            "percIframe")) {
      assertTrue(
          Files.isRegularFile(
              outRoot
                  .resolve(stem)
                  .resolve(PSComponentPackageManifest.DEFAULT_MANIFEST_FILE_NAME)),
          "writeAll missing residual manifest for " + stem);
    }
  }

  @Test
  void residualProductPackageDirs_disjointFromHighTrafficAndBase() {
    assertFalse(
        PSWidgetXmlPackageCompiler.RESIDUAL_PRODUCT_PACKAGE_DIRS.contains("perc.baseWidgets"));
    for (String high : PSWidgetXmlPackageCompiler.HIGH_TRAFFIC_PACKAGE_DIRS) {
      assertFalse(
          PSWidgetXmlPackageCompiler.RESIDUAL_PRODUCT_PACKAGE_DIRS.contains(high),
          "residual must not re-list high-traffic package: " + high);
    }
    assertEquals(9, PSWidgetXmlPackageCompiler.RESIDUAL_PRODUCT_PACKAGE_DIRS.size());
  }

  @Test
  void parseImageAutoList_autoListWithManyPrefsAndCss() throws Exception {
    PSWidgetXmlModel model = parseClasspath(FIXTURE_IMAGE_AUTO_LIST, "percImageAutoList.xml");
    assertEquals("Image Auto List", model.getTitle());
    assertEquals("percImageAutoList", model.getContentTypeName());
    assertTrue(model.getCategory() != null && model.getCategory().contains("search"));
    assertEquals(16, model.getUserPrefs().size());
    assertTrue(model.getCssPrefs().size() >= 3);
  }

  @Test
  void compileImageAutoList_matchesGoldenManifestAndTemplate() throws Exception {
    PSWidgetXmlCompileResult result =
        assertGoldenParity(
            FIXTURE_IMAGE_AUTO_LIST,
            "percImageAutoList.xml",
            "perc.ImageAutoListWidget",
            GOLDEN_IMAGE_AUTO_LIST_MANIFEST,
            GOLDEN_IMAGE_AUTO_LIST_TEMPLATE,
            "templates/percImageAutoListSnippet.vm");
    assertEquals(
        "percImageAutoList", result.getManifest().getContentTypes().get(0).getName());
    assertTrue(result.getManifest().getUserPreferences().size() >= 10);
    assertTrue(result.getManifest().getCssPreferences().size() >= 3);
  }

  @Test
  void parseComments_chromeWidgetNoContentType() throws Exception {
    PSWidgetXmlModel model = parseClasspath(FIXTURE_COMMENTS, "percComments.xml");
    assertEquals("Comments", model.getTitle());
    assertTrue(
        model.getContentTypeName() == null || model.getContentTypeName().isBlank(),
        "comments is chrome/logic (no asset CT)");
    assertEquals(1, model.getUserPrefs().size());
    assertEquals(1, model.getCssPrefs().size());
  }

  @Test
  void compileComments_matchesGolden_chromeSlotWithoutContentType() throws Exception {
    PSWidgetXmlCompileResult result =
        assertGoldenParity(
            FIXTURE_COMMENTS,
            "percComments.xml",
            "perc.widgets.comments",
            GOLDEN_COMMENTS_MANIFEST,
            GOLDEN_COMMENTS_TEMPLATE,
            "templates/percCommentsSnippet.vm");
    assertTrue(
        result.getManifest().getContentTypes() == null
            || result.getManifest().getContentTypes().isEmpty());
    assertEquals("percCommentsChrome", result.getManifest().getSlots().get(0).getName());
  }

  @Test
  void parseEvent_contentWidgetWithUserPrefs() throws Exception {
    PSWidgetXmlModel model = parseClasspath(FIXTURE_EVENT, "percEvent.xml");
    assertEquals("Event", model.getTitle());
    assertEquals("percEventAsset", model.getContentTypeName());
    assertEquals(9, model.getUserPrefs().size());
    assertEquals(1, model.getCssPrefs().size());
  }

  @Test
  void compileEvent_matchesGoldenManifestAndTemplate() throws Exception {
    PSWidgetXmlCompileResult result =
        assertGoldenParity(
            FIXTURE_EVENT,
            "percEvent.xml",
            "perc.eventWidget",
            GOLDEN_EVENT_MANIFEST,
            GOLDEN_EVENT_TEMPLATE,
            "templates/percEventSnippet.vm");
    assertEquals("percEventAsset", result.getManifest().getContentTypes().get(0).getName());
    assertEquals("percEventContent", result.getManifest().getSlots().get(0).getName());
    assertEquals(9, result.getManifest().getUserPreferences().size());
  }

  @Test
  void compileRemainingProductPackages_allValidate() throws Exception {
    Path packagesRoot = locatePackagesRoot();
    if (packagesRoot == null) {
      System.err.println(
          "WARN: Packages root not found; skipping remaining product package test");
      return;
    }

    List<PSWidgetXmlCompileResult> results =
        PSWidgetXmlPackageCompiler.compileRemainingProductPackages(packagesRoot);
    // auto-lists(3)+blog companions(5)+social/comments/cards(5)+misc(11 widgets in 10 pkgs)
    // defaultLanguage has 2 widgets; all others 1 → 3+5+5+10+1(defaultLang second)+1? =
    // 23 packages: 22 single-widget + defaultLanguage(2) = 24 widgets
    assertEquals(
        24,
        results.size(),
        "remaining #2802 batch should compile 24 product widgets across 23 packages");

    Map<String, PSWidgetXmlCompileResult> byId =
        results.stream()
            .collect(Collectors.toMap(r -> r.getManifest().getId(), r -> r, (a, b) -> a));

    for (String id :
        List.of(
            "percImageAutoList",
            "percPageAutoList",
            "percFileAutoList",
            "percBlogIndexPage",
            "percArchiveList",
            "percCategoryList",
            "percTagList",
            "percMostReadBlogPosts",
            "percComments",
            "percLiked",
            "percCommentsForm",
            "percOpenGraph",
            "percTwitterSummaryCards",
            "percEvent",
            "percImageSlider",
            "percCookieConsent",
            "percJQueryWidget",
            "percJQueryUIWidget",
            "percRegistration",
            "percSecureLogin",
            "percResult",
            "percRedirect",
            "percDefaultLang",
            "percLocalLang")) {
      assertTrue(byId.containsKey(id), "missing compiled remaining widget: " + id);
      PSComponentPackageManifestValidator.validate(byId.get(id).getManifest());
      assertFalse(byId.get(id).getTextArtifacts().isEmpty());
      assertEquals(
          "velocityAssembler",
          byId.get(id).getManifest().getTemplates().get(0).getAssembler());
    }

    // Auto-list shape: asset CT + many user prefs.
    assertEquals(
        "percImageAutoList",
        byId.get("percImageAutoList").getManifest().getContentTypes().get(0).getName());
    assertTrue(byId.get("percImageAutoList").getManifest().getUserPreferences().size() >= 10);

    // Chrome widgets without CT.
    for (String chromeId :
        List.of(
            "percComments",
            "percLiked",
            "percMostReadBlogPosts",
            "percResult",
            "percJQueryWidget",
            "percJQueryUIWidget")) {
      assertTrue(
          byId.get(chromeId).getManifest().getContentTypes() == null
              || byId.get(chromeId).getManifest().getContentTypes().isEmpty(),
          chromeId + " should be chrome (no CT)");
    }

    // defaultLanguage multi-widget package shares version.
    assertEquals(
        byId.get("percDefaultLang").getManifest().getVersion(),
        byId.get("percLocalLang").getManifest().getVersion());

    Path outRoot = tempDir.resolve("remaining-out");
    PSWidgetXmlPackageCompiler.writeAll(results, outRoot);
    for (String stem :
        List.of(
            "percImageAutoList",
            "percComments",
            "percEvent",
            "percDefaultLang",
            "percLocalLang",
            "percRedirect")) {
      assertTrue(
          Files.isRegularFile(
              outRoot
                  .resolve(stem)
                  .resolve(PSComponentPackageManifest.DEFAULT_MANIFEST_FILE_NAME)),
          "writeAll missing remaining manifest for " + stem);
    }
  }

  @Test
  void remainingProductPackageDirs_disjointFromPriorBatches() {
    assertFalse(
        PSWidgetXmlPackageCompiler.REMAINING_PRODUCT_PACKAGE_DIRS.contains("perc.baseWidgets"));
    assertFalse(
        PSWidgetXmlPackageCompiler.REMAINING_PRODUCT_PACKAGE_DIRS.contains("perc.Test"));
    for (String high : PSWidgetXmlPackageCompiler.HIGH_TRAFFIC_PACKAGE_DIRS) {
      assertFalse(
          PSWidgetXmlPackageCompiler.REMAINING_PRODUCT_PACKAGE_DIRS.contains(high),
          "remaining must not re-list high-traffic package: " + high);
    }
    for (String residual : PSWidgetXmlPackageCompiler.RESIDUAL_PRODUCT_PACKAGE_DIRS) {
      assertFalse(
          PSWidgetXmlPackageCompiler.REMAINING_PRODUCT_PACKAGE_DIRS.contains(residual),
          "remaining must not re-list #2789 residual package: " + residual);
    }
    assertEquals(23, PSWidgetXmlPackageCompiler.REMAINING_PRODUCT_PACKAGE_DIRS.size());
  }

  @Test
  void parseTestProperties_userPrefsEnumAndVelocityContent() throws Exception {
    PSWidgetXmlModel model =
        parseClasspath(FIXTURE_TEST_PROPERTIES, "PSWidget_TestProperties.xml");
    assertEquals("Test Properties", model.getTitle());
    assertEquals("PSWidget_TestProperties", model.getContentTypeName());
    assertEquals("Widget for testing", model.getDescription());
    assertEquals("velocity", model.getContentType());
    assertNotNull(model.getContentBody());
    assertTrue(model.getContentBody().contains("$perc.widget.item.name"));
    assertTrue(model.getContentBody().contains("day_of_week"));
    assertEquals(4, model.getUserPrefs().size());
    assertEquals("show_date", model.getUserPrefs().get(0).getName());
    assertEquals("bool", model.getUserPrefs().get(0).getDatatype());
    assertEquals("day_of_week", model.getUserPrefs().get(3).getName());
    assertEquals("enum", model.getUserPrefs().get(3).getDatatype());
    assertEquals(5, model.getUserPrefs().get(3).getEnumValues().size());
    assertEquals("PSWidget_TestProperties", model.widgetStem());
  }

  @Test
  void compileTestProperties_matchesGoldenManifestAndTemplate() throws Exception {
    PSWidgetXmlCompileResult result =
        assertGoldenParity(
            FIXTURE_TEST_PROPERTIES,
            "PSWidget_TestProperties.xml",
            "perc.Test",
            GOLDEN_TEST_PROPERTIES_MANIFEST,
            GOLDEN_TEST_PROPERTIES_TEMPLATE,
            "templates/PSWidget_TestPropertiesSnippet.vm");
    assertEquals(
        "PSWidget_TestProperties",
        result.getManifest().getContentTypes().get(0).getName());
    assertEquals(
        "PSWidget_TestPropertiesContent", result.getManifest().getSlots().get(0).getName());
    assertEquals(4, result.getManifest().getUserPreferences().size());
    assertTrue(
        result.getManifest().getUserPreferences().stream()
            .anyMatch(
                p ->
                    "day_of_week".equals(p.getName())
                        && p.getEnumValues() != null
                        && p.getEnumValues().size() == 5));
    assertEquals("1.0.3", result.getManifest().getVersion());
    assertEquals("velocityAssembler", result.getManifest().getTemplates().get(0).getAssembler());
  }

  @Test
  void compileTestProductPackages_allValidate() throws Exception {
    Path packagesRoot = locatePackagesRoot();
    if (packagesRoot == null) {
      System.err.println("WARN: Packages root not found; skipping perc.Test product package test");
      return;
    }

    List<PSWidgetXmlCompileResult> results =
        PSWidgetXmlPackageCompiler.compileTestProductPackages(packagesRoot);
    assertEquals(
        1,
        results.size(),
        "perc.Test (#3736) should compile PSWidget_TestProperties from modern widgets/");
    assertFalse(
        PSWidgetXmlInstallEmitter.hasCommittedWidgetXml(packagesRoot.resolve("perc.Test")),
        "perc.Test must not commit install Widget XML");

    PSWidgetXmlCompileResult testProps = results.get(0);
    assertEquals("PSWidget_TestProperties", testProps.getManifest().getId());
    PSComponentPackageManifestValidator.validate(testProps.getManifest());
    assertFalse(testProps.getTextArtifacts().isEmpty());
    assertEquals(
        "velocityAssembler", testProps.getManifest().getTemplates().get(0).getAssembler());
    assertEquals(
        "PSWidget_TestProperties",
        testProps.getManifest().getContentTypes().get(0).getName());
    assertEquals(4, testProps.getManifest().getUserPreferences().size());
    assertEquals("1.0.3", testProps.getManifest().getVersion());

    Path outRoot = tempDir.resolve("test-product-out");
    PSWidgetXmlPackageCompiler.writeAll(results, outRoot);
    assertTrue(
        Files.isRegularFile(
            outRoot
                .resolve("PSWidget_TestProperties")
                .resolve(PSComponentPackageManifest.DEFAULT_MANIFEST_FILE_NAME)),
        "writeAll missing perc.Test manifest for PSWidget_TestProperties");
  }

  @Test
  void testProductPackageDirs_disjointFromPriorBatches() {
    assertEquals(1, PSWidgetXmlPackageCompiler.TEST_PRODUCT_PACKAGE_DIRS.size());
    assertTrue(PSWidgetXmlPackageCompiler.TEST_PRODUCT_PACKAGE_DIRS.contains("perc.Test"));
    assertFalse(
        PSWidgetXmlPackageCompiler.TEST_PRODUCT_PACKAGE_DIRS.contains("perc.baseWidgets"));
    for (String high : PSWidgetXmlPackageCompiler.HIGH_TRAFFIC_PACKAGE_DIRS) {
      assertFalse(
          PSWidgetXmlPackageCompiler.TEST_PRODUCT_PACKAGE_DIRS.contains(high),
          "test residual must not re-list high-traffic package: " + high);
    }
    for (String residual : PSWidgetXmlPackageCompiler.RESIDUAL_PRODUCT_PACKAGE_DIRS) {
      assertFalse(
          PSWidgetXmlPackageCompiler.TEST_PRODUCT_PACKAGE_DIRS.contains(residual),
          "test residual must not re-list #2789 residual package: " + residual);
    }
    for (String remaining : PSWidgetXmlPackageCompiler.REMAINING_PRODUCT_PACKAGE_DIRS) {
      assertFalse(
          PSWidgetXmlPackageCompiler.TEST_PRODUCT_PACKAGE_DIRS.contains(remaining),
          "test residual must not re-list #2802 remaining package: " + remaining);
    }
  }

  private static PSWidgetXmlCompileResult assertGoldenParity(
      String fixtureResource,
      String fileName,
      String packageDirName,
      String goldenManifestResource,
      String goldenTemplateResource,
      String templateArtifactKey)
      throws Exception {
    Path packageDir = locatePackage(packageDirName);
    assertNotNull(packageDir, "package sources missing: " + packageDirName);
    PSWidgetXmlPackageContext ctx = PSWidgetXmlPackageContext.fromPackageDir(packageDir);
    PSWidgetXmlModel model = parseClasspath(fixtureResource, fileName);
    PSWidgetXmlCompileResult result = PSWidgetXmlCompiler.compile(model, ctx);
    PSComponentPackageManifestValidator.validate(result.getManifest());

    String expectedJson = readClasspath(goldenManifestResource);
    PSComponentPackageManifest reparsed =
        PSComponentPackageManifestIo.parse(PSComponentPackageManifestIo.toJson(result.getManifest()));
    assertEquals(
        PSComponentPackageManifestIo.parse(expectedJson),
        reparsed,
        "compiled manifest must equal golden for " + fileName);

    String expectedTemplate = normalizeNewlines(readClasspath(goldenTemplateResource));
    String actualTemplate =
        normalizeNewlines(result.getTextArtifacts().get(templateArtifactKey));
    assertEquals(expectedTemplate, actualTemplate, "template source golden parity for " + fileName);
    return result;
  }

  private static PSWidgetXmlPackageContext baseWidgetsLikeContext() {
    PSWidgetXmlPackageContext ctx = new PSWidgetXmlPackageContext();
    ctx.setPackageId("perc.baseWidgets");
    ctx.setPackageName("perc.baseWidgets");
    ctx.setVersion("1.1.9");
    ctx.setDescription("The base widgets plugin for Percussion CM.");
    ctx.setPublisherName("Percussion Software Inc.");
    ctx.setPublisherUrl("http://www.percussion.com");
    ctx.setCmsMin("1.0.0");
    ctx.setCmsMax("9.0.0");
    return ctx;
  }

  private static PSWidgetXmlModel parseClasspath(String resource, String fileName)
      throws Exception {
    try (InputStream in = PSWidgetXmlCompilerTest.class.getResourceAsStream(resource)) {
      assertNotNull(in, "missing test resource: " + resource);
      return PSWidgetXmlParser.parse(in, fileName);
    }
  }

  private static String readClasspath(String resource) throws Exception {
    try (InputStream in = PSWidgetXmlCompilerTest.class.getResourceAsStream(resource)) {
      assertNotNull(in, "missing test resource: " + resource);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static String normalizeNewlines(String s) {
    return s == null ? null : s.replace("\r\n", "\n").replace('\r', '\n');
  }

  /**
   * Locate product {@code perc.baseWidgets} sources relative to the module working directory (Maven
   * surefire cwd = module root).
   */
  private static Path locateBaseWidgetsPackage() {
    return locatePackage("perc.baseWidgets");
  }

  private static Path locatePackage(String packageDirName) {
    Path packages = locatePackagesRoot();
    if (packages == null) {
      return null;
    }
    Path candidate = packages.resolve(packageDirName);
    return Files.isDirectory(candidate) ? candidate : null;
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
