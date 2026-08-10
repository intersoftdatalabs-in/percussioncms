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

/** Unit + golden parity tests for Widget XML → Component Package Manifest compiler (#2751). */
class PSWidgetXmlCompilerTest {

  private static final String FIXTURE_SIMPLE = "/widgetxml/percSimpleText.xml";
  private static final String GOLDEN_SIMPLE_MANIFEST =
      "/widgetxml/golden/percSimpleText.component-package.json";
  private static final String GOLDEN_SIMPLE_TEMPLATE =
      "/widgetxml/golden/percSimpleTextSnippet.vm";

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
    Path candidate =
        Path.of("src", "main", "resources", "Packages", "perc.baseWidgets");
    if (Files.isDirectory(candidate)) {
      return candidate.toAbsolutePath().normalize();
    }
    // Fallback: walk up from user.dir (rarely needed).
    Path cwd = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
    Path alt =
        cwd.resolve("src")
            .resolve("main")
            .resolve("resources")
            .resolve("Packages")
            .resolve("perc.baseWidgets");
    return Files.isDirectory(alt) ? alt : null;
  }
}
