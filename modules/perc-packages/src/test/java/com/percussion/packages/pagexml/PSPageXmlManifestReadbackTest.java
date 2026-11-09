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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.packages.manifest.PSComponentPackageManifest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Assembler-model readback parity through the modern Component Package Manifest path (issue #5862
 * / parent #2626).
 *
 * <p>A normalized definition read back from {@code component-package.json} + template sources must
 * agree field-for-field with the legacy definition-XML readback ({@link PSPageXmlParser}) of the
 * equivalent install XML ({@link PSPageXmlTemplateDefEmitter}). The assembler fallback path is
 * removed behind migrated definitions (issue #5864): blank or unregistered manifest assembler
 * values fail fast instead of defaulting or passing through.
 */
class PSPageXmlManifestReadbackTest {

  private static final String FIXTURE_PLAIN = "/pagexml/perc.base.plain.templateDef";
  private static final String FIXTURE_BINARY = "/pagexml/perc.fileBinary.templateDef";

  @TempDir Path tempDir;

  @Test
  void roundTrip_parityWithLegacyDefinitionXmlReadback() throws Exception {
    PSPageXmlModel legacy = PSPageXmlParser.parse(readClasspath(FIXTURE_PLAIN));
    legacy.setSourceFileName("perc.base.plain.templateDef");
    PSPageXmlCompileResult compiled = PSPageXmlCompiler.compile(legacy, null);
    // Write to a page dir like package build does, then read back via the manifest path.
    Path pageDir = tempDir.resolve("perc.base.plain");
    PSPageXmlCompiler.writeArtifacts(compiled, pageDir);

    PSPageXmlModel readback = PSPageXmlManifestReadback.readback(pageDir, "0-4-591");

    // Legacy readback of the equivalent install XML — the parity oracle.
    String installXml =
        PSPageXmlTemplateDefEmitter.emit(
            compiled.getManifest(),
            compiled.getTextArtifacts().values().iterator().next(),
            "0-4-591");
    PSPageXmlModel oracle = PSPageXmlParser.parse(installXml);

    assertModelsEqual(oracle, readback);
    // And the normalized shape survives the full legacy → manifest → readback loop.
    assertEquals("perc.base.plain", readback.getName());
    assertEquals("Plain", readback.getLabel());
    assertEquals(
        "Java/global/percussion/assembly/pageAssembler", readback.getAssembler());
    assertEquals("Page", readback.getOutputFormat());
    assertEquals("text/html", readback.getMimeType());
    assertEquals("UTF-8", readback.getCharset());
    assertEquals("Normal", readback.getActiveAssemblyType());
    assertEquals("Default", readback.getPublishWhen());
    assertEquals("Shared", readback.getTemplateType());
    assertEquals("0-4-591", readback.getGuid());
    assertEquals(1, readback.getRegionHoles().size());
    assertEquals("perc-content", readback.getRegionHoles().get(0).getRegionId());
  }

  @Test
  void artifactMap_readbackMatchesPageDirReadback() throws Exception {
    PSPageXmlModel legacy = PSPageXmlParser.parse(readClasspath(FIXTURE_PLAIN));
    legacy.setSourceFileName("perc.base.plain.templateDef");
    PSPageXmlCompileResult compiled = PSPageXmlCompiler.compile(legacy, null);

    PSPageXmlModel fromMap =
        PSPageXmlManifestReadback.readback(compiled.getManifest(), compiled.getTextArtifacts());

    Path pageDir = tempDir.resolve("from-map");
    PSPageXmlCompiler.writeArtifacts(compiled, pageDir);
    PSPageXmlModel fromDir = PSPageXmlManifestReadback.readback(pageDir);

    assertModelsEqual(fromDir, fromMap);
  }

  @Test
  void binaryTemplate_omitsMimeAndCharsetWithParity() throws Exception {
    PSPageXmlModel legacy = PSPageXmlParser.parse(readClasspath(FIXTURE_BINARY));
    legacy.setSourceFileName("perc.fileBinary.templateDef");
    PSPageXmlCompileResult compiled = PSPageXmlCompiler.compile(legacy, null);

    PSPageXmlModel readback =
        PSPageXmlManifestReadback.readback(
            compiled.getManifest(),
            compiled.getTextArtifacts().values().iterator().next(),
            "0-4-607");

    String installXml =
        PSPageXmlTemplateDefEmitter.emit(
            compiled.getManifest(),
            compiled.getTextArtifacts().values().iterator().next(),
            "0-4-607");
    assertModelsEqual(PSPageXmlParser.parse(installXml), readback);
    assertEquals("Binary", readback.getOutputFormat());
    assertNull(readback.getMimeType());
    assertNull(readback.getCharset());
    assertEquals(6, readback.getBindings().size());
    assertEquals("$sys.binary", readback.getBindings().get(0).getVariable());
  }

  @Test
  void minimalManifest_appliesNormalizedDefaults() throws Exception {
    PSComponentPackageManifest manifest = new PSComponentPackageManifest();
    manifest.setId("perc.modern.minimal");
    manifest.setName("Minimal");
    PSComponentPackageManifest.TemplateRef template =
        new PSComponentPackageManifest.TemplateRef();
    template.setName("perc.modern.minimal");
    template.setType("page");
    // Modern-only strictness (issue #5864): migrated definitions name a registered assembler;
    // the blank default fallback is removed, so the minimal manifest states it explicitly.
    template.setAssembler("pageAssembler");
    manifest.setTemplates(List.of(template));

    PSPageXmlModel readback = PSPageXmlManifestReadback.readback(manifest, "<p>hi</p>");

    assertEquals("perc.modern.minimal", readback.getName());
    assertEquals("Minimal", readback.getLabel());
    assertEquals(
        "Java/global/percussion/assembly/pageAssembler", readback.getAssembler());
    assertEquals("Page", readback.getOutputFormat());
    assertEquals("text/html", readback.getMimeType());
    assertEquals("UTF-8", readback.getCharset());
    assertEquals("Normal", readback.getActiveAssemblyType());
    assertEquals("Default", readback.getPublishWhen());
    assertEquals("Shared", readback.getTemplateType());
    assertNull(readback.getGuid());
    assertNull(readback.getLocationPrefix());
    assertNull(readback.getLocationSuffix());
    assertTrue(readback.getBindings().isEmpty());
    assertTrue(readback.getRegionHoles().isEmpty());
    assertEquals("<p>hi</p>", readback.getTemplateBody());
  }

  @Test
  void productBasePlain_readbackMatchesInstallReadback() throws Exception {
    Path product = locatePackage("perc.baseTemplates");
    if (product == null) {
      System.err.println("WARN: perc.baseTemplates not found; skipping product readback test");
      return;
    }
    Path pageDir = product.resolve("pages").resolve("perc.base.plain");
    Map<String, String> guids = PSPageXmlDualShip.loadGuidsFromMapping(product);
    String guid = guids.get("perc.base.plain");

    PSPageXmlModel readback = PSPageXmlManifestReadback.readback(pageDir, guid);

    PSPageXmlInstallArtifact install = PSPageXmlNativeInstall.fromModernPageDir(pageDir, guid);
    Path staged = tempDir.resolve("perc.base.plain.templateDef");
    Files.writeString(staged, install.templateDefXml(), StandardCharsets.UTF_8);
    assertModelsEqual(PSPageXmlParser.parse(staged), readback);

    assertEquals("perc.base.plain", readback.getName());
    assertEquals("Java/global/percussion/assembly/pageAssembler", readback.getAssembler());
    assertEquals("Page", readback.getOutputFormat());
  }

  @Test
  void blankAssembler_failsFastWithoutDefaultFallback() {
    // Issue #5864: behind migrated definitions there is no blank → pageAssembler fallback.
    PSComponentPackageManifest manifest = new PSComponentPackageManifest();
    manifest.setId("perc.blank-assembler");
    PSComponentPackageManifest.TemplateRef template =
        new PSComponentPackageManifest.TemplateRef();
    template.setName("perc.blank-assembler");
    template.setType("page");
    manifest.setTemplates(List.of(template));
    PSPageXmlException ex =
        assertThrows(
            PSPageXmlException.class, () -> PSPageXmlManifestReadback.readback(manifest, "body"));
    assertTrue(ex.getMessage().contains("unregistered assembler"));
    assertTrue(ex.getMessage().contains("perc.blank-assembler"));
  }

  @Test
  void unregisteredAssembler_failsFastWithoutPassthroughFallback() {
    // Issue #5864: custom ids no longer gain the product extension prefix on the modern path.
    PSComponentPackageManifest manifest = new PSComponentPackageManifest();
    manifest.setId("perc.custom-assembler");
    PSComponentPackageManifest.TemplateRef template =
        new PSComponentPackageManifest.TemplateRef();
    template.setName("perc.custom-assembler");
    template.setType("page");
    template.setAssembler("siteAssembler");
    manifest.setTemplates(List.of(template));
    PSPageXmlException ex =
        assertThrows(
            PSPageXmlException.class, () -> PSPageXmlManifestReadback.readback(manifest, "body"));
    assertTrue(ex.getMessage().contains("unregistered assembler"));
    assertTrue(ex.getMessage().contains("siteAssembler"));
  }

  @Test
  void registeredAssemblerVariants_resolveToCatalogPaths() throws Exception {
    // Registered short id, full extension path, legacy alias, and casing all resolve (no throw).
    assertReadbackAssembler(
        "pageAssembler", "Java/global/percussion/assembly/pageAssembler");
    assertReadbackAssembler(
        "Java/global/percussion/assembly/htmlAssembler",
        "Java/global/percussion/assembly/htmlAssembler");
    assertReadbackAssembler(
        "xslAssembler", "Java/global/percussion/assembly/legacyAssembler");
    assertReadbackAssembler(
        "MARKDOWNASSEMBLER", "Java/global/percussion/assembly/markdownAssembler");
  }

  private static void assertReadbackAssembler(String manifestAssembler, String expectedPath)
      throws Exception {
    PSComponentPackageManifest manifest = new PSComponentPackageManifest();
    manifest.setId("perc.variant");
    PSComponentPackageManifest.TemplateRef template =
        new PSComponentPackageManifest.TemplateRef();
    template.setName("perc.variant");
    template.setType("page");
    template.setAssembler(manifestAssembler);
    manifest.setTemplates(List.of(template));
    assertEquals(
        expectedPath,
        PSPageXmlManifestReadback.readback(manifest, "<p>hi</p>").getAssembler());
  }

  @Test
  void emptyTemplates_failsFast() {
    PSComponentPackageManifest manifest = new PSComponentPackageManifest();
    manifest.setId("perc.empty");
    manifest.setTemplates(List.of());
    PSPageXmlException ex =
        assertThrows(
            PSPageXmlException.class, () -> PSPageXmlManifestReadback.readback(manifest, "body"));
    assertTrue(ex.getMessage().contains("templates must not be empty"));
  }

  @Test
  void missingSourceArtifact_failsFast() {
    PSComponentPackageManifest manifest = new PSComponentPackageManifest();
    manifest.setId("perc.missing-src");
    PSComponentPackageManifest.TemplateRef template =
        new PSComponentPackageManifest.TemplateRef();
    template.setName("perc.missing-src");
    template.setType("page");
    template.setSourceRef("templates/gone.vm");
    manifest.setTemplates(List.of(template));
    PSPageXmlException ex =
        assertThrows(
            PSPageXmlException.class,
            () -> PSPageXmlManifestReadback.readback(manifest, Map.of()));
    assertTrue(ex.getMessage().contains("Missing template source artifact"));
  }

  @Test
  void missingManifestFile_failsFast() {
    Path pageDir = tempDir.resolve("no-manifest");
    Exception ex =
        assertThrows(Exception.class, () -> PSPageXmlManifestReadback.readback(pageDir));
    assertTrue(ex instanceof PSPageXmlException || ex instanceof java.io.IOException);
  }

  /** Field-for-field parity: sourceFileName is transport, not definition — excluded. */
  private static void assertModelsEqual(PSPageXmlModel expected, PSPageXmlModel actual) {
    assertEquals(expected.getName(), actual.getName());
    assertEquals(expected.getLabel(), actual.getLabel());
    assertEquals(expected.getDescription(), actual.getDescription());
    assertEquals(expected.getGuid(), actual.getGuid());
    assertEquals(expected.getAssembler(), actual.getAssembler());
    assertEquals(expected.getOutputFormat(), actual.getOutputFormat());
    assertEquals(expected.getTemplateType(), actual.getTemplateType());
    assertEquals(expected.getMimeType(), actual.getMimeType());
    assertEquals(expected.getCharset(), actual.getCharset());
    assertEquals(expected.getActiveAssemblyType(), actual.getActiveAssemblyType());
    assertEquals(expected.getPublishWhen(), actual.getPublishWhen());
    assertEquals(expected.getLocationPrefix(), actual.getLocationPrefix());
    assertEquals(expected.getLocationSuffix(), actual.getLocationSuffix());
    assertEquals(expected.getTemplateBody(), actual.getTemplateBody());
    assertEquals(expected.getBindings().size(), actual.getBindings().size());
    for (int i = 0; i < expected.getBindings().size(); i++) {
      assertEquals(expected.getBindings().get(i).getVariable(), actual.getBindings().get(i).getVariable());
      assertEquals(
          expected.getBindings().get(i).getExpression(),
          actual.getBindings().get(i).getExpression());
    }
    assertEquals(expected.getRegionHoles().size(), actual.getRegionHoles().size());
    for (int i = 0; i < expected.getRegionHoles().size(); i++) {
      assertEquals(
          expected.getRegionHoles().get(i).getRegionId(),
          actual.getRegionHoles().get(i).getRegionId());
      assertEquals(
          expected.getRegionHoles().get(i).getCssClass(),
          actual.getRegionHoles().get(i).getCssClass());
      assertEquals(
          expected.getRegionHoles().get(i).getLayoutHints(),
          actual.getRegionHoles().get(i).getLayoutHints());
      assertEquals(
          expected.getRegionHoles().get(i).getStyleHints(),
          actual.getRegionHoles().get(i).getStyleHints());
    }
  }

  private static String readClasspath(String resource) throws Exception {
    try (var in = PSPageXmlManifestReadbackTest.class.getResourceAsStream(resource)) {
      if (in == null) {
        throw new IllegalStateException("missing test resource: " + resource);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
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
