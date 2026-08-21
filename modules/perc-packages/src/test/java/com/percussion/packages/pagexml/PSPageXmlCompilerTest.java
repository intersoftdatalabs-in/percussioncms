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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
 * Unit + golden parity tests for Page {@code *.templateDef} → Component Package Manifest compiler
 * (#2770 / parent #2630).
 */
class PSPageXmlCompilerTest {

  private static final String FIXTURE_PLAIN = "/pagexml/perc.base.plain.templateDef";
  private static final String FIXTURE_HEADER_FOOTER =
      "/pagexml/perc.base.headerFooter.templateDef";
  private static final String GOLDEN_PLAIN_MANIFEST =
      "/pagexml/golden/perc.base.plain.component-package.json";
  private static final String GOLDEN_PLAIN_TEMPLATE = "/pagexml/golden/perc.base.plain.vm";

  @TempDir Path tempDir;

  @Test
  void parsePlain_populatesNameAssemblerRegionsAndBody() throws Exception {
    PSPageXmlModel model = parseClasspath(FIXTURE_PLAIN, "perc.base.plain.templateDef");

    assertEquals("perc.base.plain", model.getName());
    assertEquals("Plain", model.getLabel());
    assertEquals("Java/global/percussion/assembly/pageAssembler", model.getAssembler());
    assertEquals("Page", model.getOutputFormat());
    assertEquals("perc.base.plain", model.pageStem());
    assertNotNull(model.getTemplateBody());
    assertTrue(model.getTemplateBody().contains("#region(\"perc-content\""));
    assertTrue(model.getTemplateBody().contains("<div id=\"perc-content\""));
    assertEquals(1, model.getRegionHoles().size());
    assertEquals("perc-content", model.getRegionHoles().get(0).getRegionId());
    assertEquals("vertical", model.getRegionHoles().get(0).getLayoutHints().get("orientation"));
    assertEquals(
        "perc-region perc-vertical",
        model.getRegionHoles().get(0).getStyleHints().get("rootclass"));
  }

  @Test
  void parseHeaderFooter_threeRegionsInOrder() throws Exception {
    PSPageXmlModel model =
        parseClasspath(FIXTURE_HEADER_FOOTER, "perc.base.headerFooter.templateDef");

    assertEquals("perc.base.headerFooter", model.getName());
    List<String> ids =
        model.getRegionHoles().stream()
            .map(PSPageXmlModel.RegionHole::getRegionId)
            .collect(Collectors.toList());
    assertEquals(List.of("header", "content", "footer"), ids);
  }

  @Test
  void compilePlain_matchesGoldenManifestAndTemplate() throws Exception {
    PSPageXmlModel model = parseClasspath(FIXTURE_PLAIN, "perc.base.plain.templateDef");
    PSPageXmlPackageContext ctx = baseTemplatesLikeContext();

    PSPageXmlCompileResult result = PSPageXmlCompiler.compile(model, ctx);
    PSComponentPackageManifest manifest = result.getManifest();

    PSComponentPackageManifestValidator.validate(manifest);

    String expectedJson = readClasspath(GOLDEN_PLAIN_MANIFEST);
    PSComponentPackageManifest golden = PSComponentPackageManifestIo.parse(expectedJson);

    assertEquals(golden.getSchemaVersion(), manifest.getSchemaVersion());
    assertEquals(golden.getId(), manifest.getId());
    assertEquals(golden.getName(), manifest.getName());
    assertEquals(golden.getVersion(), manifest.getVersion());
    assertEquals(golden.getDescription(), manifest.getDescription());
    assertEquals(golden.getPublisher(), manifest.getPublisher());
    assertEquals(golden.getCmsVersion(), manifest.getCmsVersion());
    assertEquals(golden.getCatalog(), manifest.getCatalog());
    assertEquals(golden.getTemplates().size(), manifest.getTemplates().size());
    assertEquals(golden.getTemplates().get(0).getName(), manifest.getTemplates().get(0).getName());
    assertEquals(
        golden.getTemplates().get(0).getType(), manifest.getTemplates().get(0).getType());
    assertEquals(
        golden.getTemplates().get(0).getAssembler(),
        manifest.getTemplates().get(0).getAssembler());
    assertEquals(
        golden.getTemplates().get(0).getSourceRef(),
        manifest.getTemplates().get(0).getSourceRef());
    assertEquals(golden.getSlots(), manifest.getSlots());

    PSComponentPackageManifest reparsed =
        PSComponentPackageManifestIo.parse(PSComponentPackageManifestIo.toJson(manifest));
    assertEquals(
        PSComponentPackageManifestIo.parse(expectedJson),
        reparsed,
        "compiled manifest must equal golden fixture");

    String expectedTemplate = normalizeNewlines(readClasspath(GOLDEN_PLAIN_TEMPLATE));
    String actualTemplate =
        normalizeNewlines(result.getTextArtifacts().get("templates/perc.base.plain.vm"));
    assertEquals(expectedTemplate, actualTemplate, "template source golden parity");
  }

  @Test
  void compilePlain_writeArtifacts_roundTrips() throws Exception {
    PSPageXmlModel model = parseClasspath(FIXTURE_PLAIN, "perc.base.plain.templateDef");
    PSPageXmlCompileResult result = PSPageXmlCompiler.compile(model, baseTemplatesLikeContext());

    Path out = tempDir.resolve("plain-out");
    PSPageXmlCompiler.writeArtifacts(result, out);

    Path manifestPath = out.resolve(PSComponentPackageManifest.DEFAULT_MANIFEST_FILE_NAME);
    assertTrue(Files.isRegularFile(manifestPath));
    PSComponentPackageManifest loaded = PSComponentPackageManifestIo.read(manifestPath);
    PSComponentPackageManifestValidator.validate(loaded);
    assertEquals(result.getManifest().getId(), loaded.getId());

    Path template = out.resolve("templates").resolve("perc.base.plain.vm");
    assertTrue(Files.isRegularFile(template));
    assertEquals(
        normalizeNewlines(result.getTextArtifacts().get("templates/perc.base.plain.vm")),
        normalizeNewlines(Files.readString(template, StandardCharsets.UTF_8)));
  }

  @Test
  void compileBaseTemplatesPackage_allValidPageTemplates() throws Exception {
    Path packageDir = locateBaseTemplatesPackage();
    if (packageDir == null) {
      System.err.println(
          "WARN: perc.baseTemplates package sources not found; skipping package test");
      return;
    }

    // #2786: product authoring is modern pages/; compilePackage prefers that path.
    assertTrue(
        PSPageXmlDualShip.hasModernPageSources(packageDir),
        "baseTemplates must ship modern pages/ sources");

    List<PSPageXmlCompileResult> results = PSPageXmlPackageCompiler.compilePackage(packageDir);
    assertTrue(results.size() >= 20, "baseTemplates should ship many layout templates");

    Map<String, PSPageXmlCompileResult> byId =
        results.stream()
            .collect(Collectors.toMap(r -> r.getManifest().getId(), r -> r, (a, b) -> a));

    assertTrue(byId.containsKey("perc.base.plain"));
    assertTrue(byId.containsKey("perc.base.headerFooter"));
    assertTrue(byId.containsKey("perc.base.Box"));

    for (PSPageXmlCompileResult r : results) {
      PSComponentPackageManifestValidator.validate(r.getManifest());
      assertEquals("1.1.5", r.getManifest().getVersion());
      assertNotNull(r.getManifest().getPublisher());
      assertEquals("page", r.getManifest().getCatalog().getKind());
      assertEquals("page", r.getManifest().getTemplates().get(0).getType());
      assertEquals("pageAssembler", r.getManifest().getTemplates().get(0).getAssembler());
      assertFalse(r.getTextArtifacts().isEmpty());
      assertFalse(r.getManifest().getSlots().isEmpty(), r.getManifest().getId());
    }

    PSPageXmlCompileResult plain = byId.get("perc.base.plain");
    assertEquals(1, plain.getManifest().getSlots().size());
    assertEquals("perc-content", plain.getManifest().getSlots().get(0).getName());

    Path outRoot = tempDir.resolve("baseTemplates-out");
    PSPageXmlPackageCompiler.writeAll(results, outRoot);
    assertTrue(
        Files.isRegularFile(
            outRoot
                .resolve("perc.base.plain")
                .resolve(PSComponentPackageManifest.DEFAULT_MANIFEST_FILE_NAME)));
  }

  @Test
  void parseEmptyXml_throws() {
    assertThrows(PSPageXmlException.class, () -> PSPageXmlParser.parse("   "));
  }

  @Test
  void parseNonAssemblyRoot_throws() {
    assertThrows(PSPageXmlException.class, () -> PSPageXmlParser.parse("<Widget/>"));
  }

  @Test
  void mapAssembler_extensionPathAndShortNames() {
    assertEquals(
        "pageAssembler",
        PSPageXmlCompiler.mapAssembler("Java/global/percussion/assembly/pageAssembler"));
    assertEquals(
        "velocityAssembler",
        PSPageXmlCompiler.mapAssembler("Java/global/percussion/assembly/velocityAssembler"));
    assertEquals("htmlAssembler", PSPageXmlCompiler.mapAssembler("htmlAssembler"));
    assertEquals(
        "binaryAssembler",
        PSPageXmlCompiler.mapAssembler("Java/global/percussion/assembly/binaryAssembler"));
  }

  @Test
  void mapTemplateType_outputFormat() {
    assertEquals("page", PSPageXmlCompiler.mapTemplateType("Page", "pageAssembler"));
    assertEquals("global", PSPageXmlCompiler.mapTemplateType("Global", "velocityAssembler"));
    assertEquals("snippet", PSPageXmlCompiler.mapTemplateType("Snippet", "velocityAssembler"));
    assertEquals("binary", PSPageXmlCompiler.mapTemplateType("Binary", "binaryAssembler"));
  }

  @Test
  void compileFileBinary_bindingsAssemblerAndSharedType() throws Exception {
    PSPageXmlModel model =
        parseClasspath("/pagexml/perc.fileBinary.templateDef", "perc.fileBinary.templateDef");
    assertEquals("perc.fileBinary", model.getName());
    assertEquals("Binary", model.getOutputFormat());
    assertEquals("Java/global/percussion/assembly/binaryAssembler", model.getAssembler());
    assertEquals("Shared", model.getTemplateType());
    assertEquals(6, model.getBindings().size());
    assertEquals("$sys.binary", model.getBindings().get(0).getVariable());
    assertEquals("$sys.mimetype", model.getBindings().get(1).getVariable());

    PSPageXmlCompileResult result = PSPageXmlCompiler.compile(model, fileWidgetLikeContext());
    PSComponentPackageManifestValidator.validate(result.getManifest());
    PSComponentPackageManifest.TemplateRef t = result.getManifest().getTemplates().get(0);
    assertEquals("perc.fileBinary", result.getManifest().getId());
    assertEquals("binary", t.getType());
    assertEquals("binaryAssembler", t.getAssembler());
    assertEquals("templates/perc.fileBinary.vm", t.getSourceRef());
    assertNull(t.getLegacyTemplateType());
    assertEquals(6, t.getBindings().size());
    assertEquals("$sys.binary", t.getBindings().get(0).getVariable());
    assertEquals(
        "$sys.item.getProperty(\"rx:item_file_attachment\")",
        t.getBindings().get(0).getExpression());
  }

  @Test
  void compileImageBinaries_localTemplateType() throws Exception {
    PSPageXmlModel main =
        parseClasspath(
            "/pagexml/perc.imageMainBinary.templateDef", "perc.imageMainBinary.templateDef");
    PSPageXmlCompileResult mainResult =
        PSPageXmlCompiler.compile(main, imageWidgetLikeContext());
    PSComponentPackageManifestValidator.validate(mainResult.getManifest());
    assertEquals("perc.imageMainBinary", mainResult.getManifest().getId());
    assertEquals("Local", mainResult.getManifest().getTemplates().get(0).getLegacyTemplateType());
    assertEquals("binary", mainResult.getManifest().getTemplates().get(0).getType());
    assertEquals(
        "binaryAssembler", mainResult.getManifest().getTemplates().get(0).getAssembler());

    PSPageXmlModel thumb =
        parseClasspath(
            "/pagexml/perc.imageThumbBinary.templateDef", "perc.imageThumbBinary.templateDef");
    assertEquals("perc.imageThumbBinary", thumb.getName());
    assertEquals("perc.imageThumbnailBinary", thumb.getLabel());
    PSPageXmlCompileResult thumbResult =
        PSPageXmlCompiler.compile(thumb, imageWidgetLikeContext());
    PSComponentPackageManifestValidator.validate(thumbResult.getManifest());
    assertEquals("perc.imageThumbBinary", thumbResult.getManifest().getId());
    assertEquals("perc.imageThumbnailBinary", thumbResult.getManifest().getName());
    assertEquals("Local", thumbResult.getManifest().getTemplates().get(0).getLegacyTemplateType());
    assertEquals(2, thumbResult.getManifest().getTemplates().get(0).getBindings().size());
  }

  @Test
  void productWidgetBinaryPages_compileFromModernSources() throws Exception {
    Path filePkg = locatePackage("perc.FileAssetWidget");
    Path imagePkg = locatePackage("perc.widgets.image");
    if (filePkg == null || imagePkg == null) {
      System.err.println("WARN: widget packages not found; skipping product binary page compile");
      return;
    }

    List<PSPageXmlCompileResult> fileResults = PSPageXmlPackageCompiler.compilePackage(filePkg);
    assertEquals(1, fileResults.size());
    PSComponentPackageManifestValidator.validate(fileResults.get(0).getManifest());
    assertEquals("perc.fileBinary", fileResults.get(0).getManifest().getId());
    assertEquals("binary", fileResults.get(0).getManifest().getTemplates().get(0).getType());
    assertEquals(
        "binaryAssembler",
        fileResults.get(0).getManifest().getTemplates().get(0).getAssembler());

    List<PSPageXmlCompileResult> imageResults = PSPageXmlPackageCompiler.compilePackage(imagePkg);
    assertEquals(2, imageResults.size());
    Map<String, PSPageXmlCompileResult> byId =
        imageResults.stream()
            .collect(Collectors.toMap(r -> r.getManifest().getId(), r -> r, (a, b) -> a));
    assertTrue(byId.containsKey("perc.imageMainBinary"));
    assertTrue(byId.containsKey("perc.imageThumbBinary"));
    for (PSPageXmlCompileResult r : imageResults) {
      PSComponentPackageManifestValidator.validate(r.getManifest());
      assertEquals("binary", r.getManifest().getTemplates().get(0).getType());
      assertEquals("Local", r.getManifest().getTemplates().get(0).getLegacyTemplateType());
    }
  }

  private static PSPageXmlPackageContext baseTemplatesLikeContext() {
    PSPageXmlPackageContext ctx = new PSPageXmlPackageContext();
    ctx.setPackageId("perc.baseTemplates");
    ctx.setPackageName("perc.baseTemplates");
    ctx.setVersion("1.1.5");
    ctx.setDescription("Percussion base layout templates.");
    ctx.setPublisherName("Percussion Software Inc.");
    ctx.setPublisherUrl("http://www.percussion.com");
    ctx.setCmsMin("1.0.0");
    ctx.setCmsMax("9.0.0");
    return ctx;
  }

  private static PSPageXmlPackageContext fileWidgetLikeContext() {
    PSPageXmlPackageContext ctx = new PSPageXmlPackageContext();
    ctx.setPackageId("perc.FileAssetWidget");
    ctx.setPackageName("perc.FileAssetWidget");
    ctx.setVersion("1.1.6");
    ctx.setDescription("FileAssetWidget");
    ctx.setPublisherName("Percussion Software Inc.");
    ctx.setPublisherUrl("https://www.percussion.com");
    ctx.setCmsMin("1.0.0");
    ctx.setCmsMax("9.0.0");
    return ctx;
  }

  private static PSPageXmlPackageContext imageWidgetLikeContext() {
    PSPageXmlPackageContext ctx = new PSPageXmlPackageContext();
    ctx.setPackageId("perc.widgets.image");
    ctx.setPackageName("perc.widgets.image");
    ctx.setVersion("1.1.8");
    ctx.setDescription("Image Widget");
    ctx.setPublisherName("Percussion Software");
    ctx.setPublisherUrl("http://www.percussion.com");
    ctx.setCmsMin("1.0.1");
    ctx.setCmsMax("9.0.0");
    return ctx;
  }

  private static PSPageXmlModel parseClasspath(String resource, String fileName) throws Exception {
    try (InputStream in = PSPageXmlCompilerTest.class.getResourceAsStream(resource)) {
      assertNotNull(in, "missing test resource: " + resource);
      return PSPageXmlParser.parse(in, fileName);
    }
  }

  private static String readClasspath(String resource) throws Exception {
    try (InputStream in = PSPageXmlCompilerTest.class.getResourceAsStream(resource)) {
      assertNotNull(in, "missing test resource: " + resource);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static String normalizeNewlines(String s) {
    return s == null ? null : s.replace("\r\n", "\n").replace('\r', '\n');
  }

  /**
   * Locate product {@code perc.baseTemplates} sources relative to the module working directory
   * (Maven surefire cwd = module root).
   */
  private static Path locateBaseTemplatesPackage() {
    return locatePackage("perc.baseTemplates");
  }

  private static Path locatePackage(String packageName) {
    Path candidate = Path.of("src", "main", "resources", "Packages", packageName);
    if (Files.isDirectory(candidate)) {
      return candidate.toAbsolutePath().normalize();
    }
    Path cwd = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
    Path alt =
        cwd.resolve("src")
            .resolve("main")
            .resolve("resources")
            .resolve("Packages")
            .resolve(packageName);
    return Files.isDirectory(alt) ? alt : null;
  }
}
