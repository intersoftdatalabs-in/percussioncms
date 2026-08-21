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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Dual-ship install parity for modern page packages (issue #2786 / parent #2630 / ADR-004).
 *
 * <p>Product authoring: {@code pages/&lt;id&gt;/component-package.json}. Install path: generated
 * root {@code *.templateDef} with stable GUIDs from mapping.properties.
 */
class PSPageXmlDualShipTest {

  private static final String FIXTURE_PLAIN = "/pagexml/perc.base.plain.templateDef";

  @TempDir Path tempDir;

  @Test
  void emitPlain_roundTripsSemanticFieldsAndBody() throws Exception {
    PSPageXmlModel original = PSPageXmlParser.parse(readClasspath(FIXTURE_PLAIN));
    original.setSourceFileName("perc.base.plain.templateDef");
    PSPageXmlPackageContext ctx = baseTemplatesLikeContext();
    PSPageXmlCompileResult compiled = PSPageXmlCompiler.compile(original, ctx);

    String xml =
        PSPageXmlTemplateDefEmitter.emit(
            compiled.getManifest(),
            compiled.getTextArtifacts().get("templates/perc.base.plain.vm"),
            "0-4-591");

    PSPageXmlModel reparsed = PSPageXmlParser.parse(xml);
    assertEquals("perc.base.plain", reparsed.getName());
    assertEquals("Plain", reparsed.getLabel());
    assertEquals("0-4-591", reparsed.getGuid());
    assertEquals("Java/global/percussion/assembly/pageAssembler", reparsed.getAssembler());
    assertEquals("Page", reparsed.getOutputFormat());
    assertEquals(
        normalizeNewlines(original.getTemplateBody()),
        normalizeNewlines(reparsed.getTemplateBody()),
        "template body must survive dual-ship round-trip");
    assertEquals(1, reparsed.getRegionHoles().size());
    assertEquals("perc-content", reparsed.getRegionHoles().get(0).getRegionId());
  }

  @Test
  void materializeInstall_fromModernPages_writesGuidedTemplateDefs() throws Exception {
    Path packageDir = tempDir.resolve("perc.baseTemplates");
    Files.createDirectories(packageDir);

    // Minimal mapping for GUID
    Files.writeString(
        packageDir.resolve("perc.baseTemplates.mapping.properties"),
        "perc.base.plain.templateDef=TemplateDef-591\n",
        StandardCharsets.UTF_8);

    Path pageDir = packageDir.resolve(PSPageXmlDualShip.PAGES_DIR_NAME).resolve("perc.base.plain");
    Files.createDirectories(pageDir);

    PSPageXmlModel model = PSPageXmlParser.parse(readClasspath(FIXTURE_PLAIN));
    model.setSourceFileName("perc.base.plain.templateDef");
    PSPageXmlCompileResult compiled =
        PSPageXmlCompiler.compile(model, baseTemplatesLikeContext());
    PSPageXmlCompiler.writeArtifacts(compiled, pageDir);

    assertTrue(PSPageXmlDualShip.hasModernPageSources(packageDir));
    int n = PSPageXmlDualShip.materializeInstallTemplateDefs(packageDir);
    assertEquals(1, n);

    Path templateDef = packageDir.resolve("perc.base.plain.templateDef");
    assertTrue(Files.isRegularFile(templateDef));
    PSPageXmlModel installModel = PSPageXmlParser.parse(templateDef);
    assertEquals("0-4-591", installModel.getGuid());
    assertEquals("perc.base.plain", installModel.getName());
    assertEquals(
        normalizeNewlines(model.getTemplateBody()),
        normalizeNewlines(installModel.getTemplateBody()));
  }

  @Test
  void materializeInstall_missingGuid_failsFastWithClearError() throws Exception {
    Path packageDir = tempDir.resolve("perc.missingGuid");
    Files.createDirectories(packageDir);
    // mapping present but for a different stem — target stem has no GUID entry
    Files.writeString(
        packageDir.resolve("perc.missingGuid.mapping.properties"),
        "other.template.templateDef=TemplateDef-1\n",
        StandardCharsets.UTF_8);

    Path pageDir = packageDir.resolve(PSPageXmlDualShip.PAGES_DIR_NAME).resolve("perc.base.plain");
    Files.createDirectories(pageDir);
    PSPageXmlModel model = PSPageXmlParser.parse(readClasspath(FIXTURE_PLAIN));
    model.setSourceFileName("perc.base.plain.templateDef");
    PSPageXmlCompileResult compiled =
        PSPageXmlCompiler.compile(model, baseTemplatesLikeContext());
    PSPageXmlCompiler.writeArtifacts(compiled, pageDir);

    PSPageXmlException ex =
        assertThrows(
            PSPageXmlException.class,
            () -> PSPageXmlDualShip.materializeInstallTemplateDefs(packageDir));
    assertTrue(
        ex.getMessage().contains("Missing stable install GUID"),
        "message should name the failure: " + ex.getMessage());
    assertTrue(
        ex.getMessage().contains("perc.base.plain"),
        "message should name the stem: " + ex.getMessage());
    assertFalse(
        Files.isRegularFile(packageDir.resolve("perc.base.plain.templateDef")),
        "must not emit templateDef without a GUID");
  }

  @Test
  void loadModernAsCompileResult_readsEachTemplateSourceIndependently() throws Exception {
    Path pageDir = tempDir.resolve("multi-template-page");
    Path templates = pageDir.resolve("templates");
    Files.createDirectories(templates);
    Files.writeString(templates.resolve("primary.vm"), "PRIMARY-BODY", StandardCharsets.UTF_8);
    Files.writeString(templates.resolve("secondary.vm"), "SECONDARY-BODY", StandardCharsets.UTF_8);

    String manifestJson =
        """
        {
          "schemaVersion": "1.0",
          "id": "perc.test.multi",
          "name": "Multi",
          "version": "1.0.0",
          "description": "multi-template dual-ship fixture",
          "publisher": { "name": "Test", "url": "https://example.test" },
          "cmsVersion": { "min": "1.0.0", "max": "9.0.0" },
          "dependencies": [],
          "catalog": {
            "kind": "page",
            "title": "Multi",
            "category": "page",
            "paletteVisible": true
          },
          "contentTypes": [],
          "templates": [
            {
              "name": "perc.test.multi",
              "type": "page",
              "assembler": "pageAssembler",
              "sourceRef": "templates/primary.vm",
              "bindings": []
            },
            {
              "name": "perc.test.multi.alt",
              "type": "page",
              "assembler": "pageAssembler",
              "sourceRef": "templates/secondary.vm",
              "bindings": []
            }
          ],
          "slots": [],
          "resources": [],
          "userPreferences": [],
          "cssPreferences": []
        }
        """;
    Files.writeString(
        pageDir.resolve(PSComponentPackageManifest.DEFAULT_MANIFEST_FILE_NAME),
        manifestJson,
        StandardCharsets.UTF_8);

    PSPageXmlCompileResult result = PSPageXmlDualShip.loadModernAsCompileResult(pageDir);
    assertEquals("PRIMARY-BODY", normalizeNewlines(result.getSource().getTemplateBody()));
    assertEquals(
        "PRIMARY-BODY",
        normalizeNewlines(result.getTextArtifacts().get("templates/primary.vm")));
    assertEquals(
        "SECONDARY-BODY",
        normalizeNewlines(result.getTextArtifacts().get("templates/secondary.vm")),
        "each sourceRef must load its own file, not reuse templates[0]");
  }

  @Test
  void loadGuidsFromMapping_findsMappingUnderStagingCopyName() throws Exception {
    Path staging = tempDir.resolve("perc.baseTemplates-copy");
    Files.createDirectories(staging);
    Files.writeString(
        staging.resolve("perc.baseTemplates.mapping.properties"),
        "perc.base.plain.templateDef=TemplateDef-591\n"
            + "perc.base.plain.templateDef.aclDef=AclDef-1\n"
            + "perc.base.Box.templateDef=TemplateDef-557\n",
        StandardCharsets.UTF_8);

    Map<String, String> guids = PSPageXmlDualShip.loadGuidsFromMapping(staging);
    assertEquals("0-4-591", guids.get("perc.base.plain"));
    // Mixed-case product ids are stored under Locale.ROOT lowercase keys.
    assertEquals("0-4-557", guids.get("perc.base.box"));
    assertFalse(guids.containsKey("perc.base.plain.templateDef.aclDef"));
  }

  @Test
  void productBaseTemplates_modernAuthoring_dualShipInstallParity() throws Exception {
    Path product = locatePackage("perc.baseTemplates");
    if (product == null) {
      System.err.println("WARN: perc.baseTemplates not found; skipping product dual-ship test");
      return;
    }

    assertTrue(
        PSPageXmlDualShip.hasModernPageSources(product),
        "baseTemplates must author modern pages/ sources (#2786)");
    assertTrue(
        PSPageXmlPackageCompiler.listTemplateDefs(product).isEmpty(),
        "product baseTemplates must not author root *.templateDef (dual-run removed)");

    List<PSPageXmlCompileResult> modern = PSPageXmlPackageCompiler.compilePackage(product);
    assertTrue(modern.size() >= 20, "expected ≥20 base layout templates");

    Map<String, PSPageXmlCompileResult> byId =
        modern.stream()
            .collect(Collectors.toMap(r -> r.getManifest().getId(), r -> r, (a, b) -> a));
    assertTrue(byId.containsKey("perc.base.plain"));
    assertTrue(byId.containsKey("perc.base.headerFooter"));
    assertTrue(byId.containsKey("perc.base.Box"));

    for (PSPageXmlCompileResult r : modern) {
      PSComponentPackageManifestValidator.validate(r.getManifest());
      assertEquals("page", r.getManifest().getCatalog().getKind());
      assertFalse(r.getManifest().getSlots().isEmpty(), r.getManifest().getId());
    }

    // Dual-ship into a staging copy (mirrors PSPackageBuilder temp1)
    Path staging = tempDir.resolve("perc.baseTemplates-copy");
    copyTree(product, staging);
    int written = PSPageXmlDualShip.materializeInstallTemplateDefs(staging);
    assertEquals(modern.size(), written);

    Map<String, String> guids = PSPageXmlDualShip.loadGuidsFromMapping(staging);
    assertEquals("0-4-591", guids.get("perc.base.plain"));

    // Install parity: each generated templateDef recompiles to same id / body / slots
    for (PSPageXmlCompileResult expected : modern) {
      String id = expected.getManifest().getId();
      Path def = staging.resolve(id + ".templateDef");
      assertTrue(Files.isRegularFile(def), "missing dual-ship templateDef for " + id);

      PSPageXmlModel install = PSPageXmlParser.parse(def);
      assertEquals(id, install.getName());
      assertEquals(
          guids.get(id.toLowerCase(Locale.ROOT)), install.getGuid(), "GUID for " + id);
      assertEquals(
          "Java/global/percussion/assembly/pageAssembler", install.getAssembler(), id);
      assertEquals("Page", install.getOutputFormat(), id);

      String expectedBody =
          normalizeNewlines(
              expected.getTextArtifacts().values().stream().findFirst().orElse(""));
      assertEquals(
          expectedBody,
          normalizeNewlines(install.getTemplateBody()),
          "body parity for " + id);

      // Re-compile install path → same slot names
      PSPageXmlCompileResult fromInstall =
          PSPageXmlCompiler.compile(install, baseTemplatesLikeContext());
      assertEquals(
          expected.getManifest().getSlots().stream()
              .map(PSComponentPackageManifest.SlotRef::getName)
              .collect(Collectors.toList()),
          fromInstall.getManifest().getSlots().stream()
              .map(PSComponentPackageManifest.SlotRef::getName)
              .collect(Collectors.toList()),
          "slot names for " + id);
    }
  }

  @Test
  void productResponsiveTemplates_modernAuthoring_dualShipInstallParity() throws Exception {
    Path product = locatePackage("perc.responsiveTemplates");
    if (product == null) {
      System.err.println(
          "WARN: perc.responsiveTemplates not found; skipping product dual-ship test");
      return;
    }

    assertTrue(PSPageXmlDualShip.hasModernPageSources(product));
    assertTrue(PSPageXmlPackageCompiler.listTemplateDefs(product).isEmpty());

    List<PSPageXmlCompileResult> modern = PSPageXmlPackageCompiler.compilePackage(product);
    assertEquals(3, modern.size(), "responsive ships Banded/Basic/plain");

    Path staging = tempDir.resolve("perc.responsiveTemplates-copy");
    copyTree(product, staging);
    int written = PSPageXmlDualShip.materializeInstallTemplateDefs(staging);
    assertEquals(3, written);

    Map<String, String> guids = PSPageXmlDualShip.loadGuidsFromMapping(staging);
    assertEquals("0-4-597", guids.get("perc.resp.banded"));
    assertEquals("0-4-599", guids.get("perc.resp.basic"));
    assertEquals("0-4-627", guids.get("perc.resp.plain"));

    for (PSPageXmlCompileResult r : modern) {
      String id = r.getManifest().getId();
      assertTrue(Files.isRegularFile(staging.resolve(id + ".templateDef")));
      assertTrue(Boolean.TRUE.equals(r.getManifest().getCatalog().getResponsive()));
    }
  }

  @Test
  void productBaseline_modernAuthoring_dualShipInstallParity() throws Exception {
    Path product = locatePackage("perc.Baseline");
    if (product == null) {
      System.err.println("WARN: perc.Baseline not found; skipping product dual-ship test");
      return;
    }

    assertTrue(
        PSPageXmlDualShip.hasModernPageSources(product),
        "Baseline must author modern pages/ sources (#2805)");
    assertTrue(
        PSPageXmlPackageCompiler.listTemplateDefs(product).isEmpty(),
        "product Baseline must not author root *.templateDef (dual-ship install only)");

    List<PSPageXmlCompileResult> modern = PSPageXmlPackageCompiler.compilePackage(product);
    assertEquals(7, modern.size(), "Baseline system assembly templates");

    Map<String, PSPageXmlCompileResult> byId =
        modern.stream()
            .collect(Collectors.toMap(r -> r.getManifest().getId(), r -> r, (a, b) -> a));
    for (String expected :
        List.of(
            "perc.page",
            "perc.pageDatabase",
            "perc.pageDispatcher",
            "perc.pageXml",
            "perc.sys.resource",
            "perc.widget",
            "perc.widgetDispatcher")) {
      assertTrue(byId.containsKey(expected), "missing modern package " + expected);
      PSComponentPackageManifestValidator.validate(byId.get(expected).getManifest());
    }

    // System assembly fields preserved in modern manifests
    PSComponentPackageManifest.TemplateRef pageXml =
        byId.get("perc.pageXml").getManifest().getTemplates().get(0);
    assertEquals("pageVariantAssembler", pageXml.getAssembler());
    assertEquals("snippet", pageXml.getType());
    assertEquals("text/xml", pageXml.getMimeType());
    assertEquals("Never", pageXml.getPublishWhen());
    assertEquals(".xml", pageXml.getLocationSuffix());

    PSComponentPackageManifest.TemplateRef page =
        byId.get("perc.page").getManifest().getTemplates().get(0);
    assertEquals("velocityAssembler", page.getAssembler());
    assertEquals("global", page.getType());
    assertEquals("Unspecified", page.getPublishWhen());

    PSComponentPackageManifest.TemplateRef dispatcher =
        byId.get("perc.pageDispatcher").getManifest().getTemplates().get(0);
    assertEquals("dispatchAssembler", dispatcher.getAssembler());
    assertFalse(dispatcher.getBindings().isEmpty());
    assertEquals("$sys.template", dispatcher.getBindings().get(0).getVariable());

    Path staging = tempDir.resolve("perc.Baseline-copy");
    copyTree(product, staging);
    int written = PSPageXmlDualShip.materializeInstallTemplateDefs(staging);
    assertEquals(7, written);

    Map<String, String> guids = PSPageXmlDualShip.loadGuidsFromMapping(staging);
    assertEquals("0-4-602", guids.get("perc.page"));
    assertEquals("0-4-604", guids.get("perc.pagedatabase"));
    assertEquals("0-4-606", guids.get("perc.pagedispatcher"));
    assertEquals("0-4-608", guids.get("perc.pagexml"));
    assertEquals("0-4-610", guids.get("perc.sys.resource"));
    assertEquals("0-4-612", guids.get("perc.widget"));
    assertEquals("0-4-614", guids.get("perc.widgetdispatcher"));

    for (PSPageXmlCompileResult expected : modern) {
      String id = expected.getManifest().getId();
      Path def = staging.resolve(id + ".templateDef");
      assertTrue(Files.isRegularFile(def), "missing dual-ship templateDef for " + id);

      PSPageXmlModel install = PSPageXmlParser.parse(def);
      assertEquals(id, install.getName());
      assertEquals(
          guids.get(id.toLowerCase(Locale.ROOT)), install.getGuid(), "GUID for " + id);

      PSComponentPackageManifest.TemplateRef t = expected.getManifest().getTemplates().get(0);
      assertEquals(
          PSPageXmlTemplateDefEmitter.toLegacyAssemblerPath(t.getAssembler()),
          install.getAssembler(),
          id);
      assertEquals(
          PSPageXmlTemplateDefEmitter.toLegacyOutputFormat(t.getType()),
          install.getOutputFormat(),
          id);
      if (t.getMimeType() != null) {
        assertEquals(t.getMimeType(), install.getMimeType(), "mime for " + id);
      }
      if (t.getPublishWhen() != null) {
        assertEquals(t.getPublishWhen(), install.getPublishWhen(), "publish-when for " + id);
      }
      if (t.getLocationSuffix() != null && !t.getLocationSuffix().isBlank()) {
        assertEquals(t.getLocationSuffix(), install.getLocationSuffix(), "suffix for " + id);
      }

      String expectedBody =
          normalizeNewlines(
              expected.getTextArtifacts().values().stream().findFirst().orElse(""));
      assertEquals(
          expectedBody,
          normalizeNewlines(install.getTemplateBody() != null ? install.getTemplateBody() : ""),
          "body parity for " + id);

      // Binding variables survive dual-ship (simple JEXL form)
      assertEquals(
          t.getBindings().stream()
              .map(PSComponentPackageManifest.Binding::getVariable)
              .collect(Collectors.toList()),
          install.getBindings().stream()
              .map(PSPageXmlModel.Binding::getVariable)
              .collect(Collectors.toList()),
          "binding variables for " + id);
    }
  }

  @Test
  void emit_preservesMimePublishWhenLocationSuffix() throws Exception {
    PSComponentPackageManifest manifest = new PSComponentPackageManifest();
    manifest.setSchemaVersion(PSComponentPackageManifest.SUPPORTED_SCHEMA_VERSION);
    manifest.setId("perc.pageXml");
    manifest.setName("Page - XML Template");
    manifest.setVersion("1.0.0");
    PSComponentPackageManifest.Catalog catalog = new PSComponentPackageManifest.Catalog();
    catalog.setKind("page");
    catalog.setTitle("Page - XML Template");
    catalog.setDescription("Template used to render the page as XML.");
    catalog.setCategory("page");
    catalog.setPaletteVisible(Boolean.TRUE);
    manifest.setCatalog(catalog);

    PSComponentPackageManifest.TemplateRef t = new PSComponentPackageManifest.TemplateRef();
    t.setName("perc.pageXml");
    t.setType("snippet");
    t.setAssembler("pageVariantAssembler");
    t.setSourceRef("templates/perc.pageXml.vm");
    t.setMimeType("text/xml");
    t.setPublishWhen("Never");
    t.setLocationSuffix(".xml");
    manifest.setTemplates(List.of(t));
    manifest.setSlots(List.of());
    manifest.setContentTypes(List.of());
    manifest.setResources(List.of());
    manifest.setUserPreferences(List.of());
    manifest.setCssPreferences(List.of());
    PSComponentPackageManifestValidator.validate(manifest);

    String body = "<?xml version=\"1.0\"?><percPage/>";
    String xml = PSPageXmlTemplateDefEmitter.emit(manifest, body, "0-4-608");
    PSPageXmlModel reparsed = PSPageXmlParser.parse(xml);
    assertEquals("text/xml", reparsed.getMimeType());
    assertEquals("Never", reparsed.getPublishWhen());
    assertEquals(".xml", reparsed.getLocationSuffix());
    assertEquals("Snippet", reparsed.getOutputFormat());
    assertEquals(
        "Java/global/percussion/assembly/pageVariantAssembler", reparsed.getAssembler());
    assertEquals("0-4-608", reparsed.getGuid());
    assertEquals(normalizeNewlines(body), normalizeNewlines(reparsed.getTemplateBody()));
    assertEquals(
        "Template used to render the page as XML.",
        reparsed.getDescription(),
        "catalog description must survive dual-ship emit");
  }

  @Test
  void emit_fallsBackToPackageDescriptionWhenCatalogDescriptionBlank() throws Exception {
    PSComponentPackageManifest manifest = new PSComponentPackageManifest();
    manifest.setSchemaVersion(PSComponentPackageManifest.SUPPORTED_SCHEMA_VERSION);
    manifest.setId("perc.page");
    manifest.setName("Page");
    manifest.setVersion("1.0.0");
    manifest.setDescription("Package-level page description");
    PSComponentPackageManifest.Catalog catalog = new PSComponentPackageManifest.Catalog();
    catalog.setKind("page");
    catalog.setTitle("Page");
    catalog.setCategory("page");
    catalog.setPaletteVisible(Boolean.TRUE);
    manifest.setCatalog(catalog);

    PSComponentPackageManifest.TemplateRef t = new PSComponentPackageManifest.TemplateRef();
    t.setName("perc.page");
    t.setType("page");
    t.setAssembler("pageAssembler");
    t.setSourceRef("templates/perc.page.vm");
    manifest.setTemplates(List.of(t));
    manifest.setSlots(List.of());
    manifest.setContentTypes(List.of());
    manifest.setResources(List.of());
    manifest.setUserPreferences(List.of());
    manifest.setCssPreferences(List.of());
    PSComponentPackageManifestValidator.validate(manifest);

    String xml = PSPageXmlTemplateDefEmitter.emit(manifest, "<html/>", "0-4-602");
    PSPageXmlModel reparsed = PSPageXmlParser.parse(xml);
    assertEquals(
        "Package-level page description",
        reparsed.getDescription(),
        "package description used when catalog has none");
  }

  @Test
  void loadGuidsFromMapping_normalizesMixedCaseTemplateDefKeys() throws Exception {
    Path staging = tempDir.resolve("mixedCaseGuid-copy");
    Files.createDirectories(staging);
    // Mixed-case key must still resolve under lowercase manifest id lookup.
    Files.writeString(
        staging.resolve("mixedCaseGuid.mapping.properties"),
        "perc.Page.templateDef=TemplateDef-99\n",
        StandardCharsets.UTF_8);
    Map<String, String> guids = PSPageXmlDualShip.loadGuidsFromMapping(staging);
    assertEquals("0-4-99", guids.get("perc.page"));
    assertNull(guids.get("perc.Page"), "map keys are lowercase only");
  }

  @Test
  void emit_binary_emptyMimeCharset_andLocalTemplateType() throws Exception {
    PSComponentPackageManifest manifest = new PSComponentPackageManifest();
    manifest.setSchemaVersion(PSComponentPackageManifest.SUPPORTED_SCHEMA_VERSION);
    manifest.setId("perc.imageThumbBinary");
    manifest.setName("perc.imageThumbnailBinary");
    manifest.setVersion("1.1.8");
    PSComponentPackageManifest.Catalog catalog = new PSComponentPackageManifest.Catalog();
    catalog.setKind("page");
    catalog.setTitle("perc.imageThumbnailBinary");
    catalog.setCategory("binary");
    catalog.setPaletteVisible(Boolean.FALSE);
    manifest.setCatalog(catalog);

    PSComponentPackageManifest.TemplateRef t = new PSComponentPackageManifest.TemplateRef();
    t.setName("perc.imageThumbBinary");
    t.setType("binary");
    t.setAssembler("binaryAssembler");
    t.setSourceRef("templates/perc.imageThumbBinary.vm");
    t.setLegacyTemplateType("Local");
    PSComponentPackageManifest.Binding b1 = new PSComponentPackageManifest.Binding();
    b1.setVariable("$sys.binary");
    b1.setExpression("$sys.item.getProperty('rx:img2')");
    PSComponentPackageManifest.Binding b2 = new PSComponentPackageManifest.Binding();
    b2.setVariable("$sys.mimetype");
    b2.setExpression("$sys.item.getProperty('rx:img2_type')");
    t.setBindings(List.of(b1, b2));
    manifest.setTemplates(List.of(t));
    manifest.setSlots(List.of());
    manifest.setContentTypes(List.of());
    manifest.setResources(List.of());
    manifest.setUserPreferences(List.of());
    manifest.setCssPreferences(List.of());
    PSComponentPackageManifestValidator.validate(manifest);

    String xml = PSPageXmlTemplateDefEmitter.emit(manifest, "", "0-4-620");
    PSPageXmlModel reparsed = PSPageXmlParser.parse(xml);
    assertEquals("perc.imageThumbBinary", reparsed.getName());
    assertEquals("perc.imageThumbnailBinary", reparsed.getLabel());
    assertEquals("0-4-620", reparsed.getGuid());
    assertEquals("Binary", reparsed.getOutputFormat());
    assertEquals("Local", reparsed.getTemplateType());
    assertEquals(
        "Java/global/percussion/assembly/binaryAssembler", reparsed.getAssembler());
    assertTrue(reparsed.getMimeType() == null || reparsed.getMimeType().isBlank());
    assertTrue(reparsed.getCharset() == null || reparsed.getCharset().isBlank());
    assertEquals(2, reparsed.getBindings().size());
    assertEquals("$sys.binary", reparsed.getBindings().get(0).getVariable());
    assertTrue(xml.contains("<charset/>"));
    assertTrue(xml.contains("<mime-type/>"));
  }

  @Test
  void toLegacyAssemblerPath_knownShortNames() {
    assertEquals(
        "Java/global/percussion/assembly/pageAssembler",
        PSPageXmlTemplateDefEmitter.toLegacyAssemblerPath("pageAssembler"));
    assertEquals(
        "Java/global/percussion/assembly/velocityAssembler",
        PSPageXmlTemplateDefEmitter.toLegacyAssemblerPath("velocityAssembler"));
    assertEquals(
        "Java/global/percussion/assembly/dispatchAssembler",
        PSPageXmlTemplateDefEmitter.toLegacyAssemblerPath("dispatchAssembler"));
    assertEquals(
        "Java/global/percussion/assembly/resourceAssembler",
        PSPageXmlTemplateDefEmitter.toLegacyAssemblerPath("resourceAssembler"));
    assertEquals(
        "Java/global/percussion/assembly/pageDatabaseAssembler",
        PSPageXmlTemplateDefEmitter.toLegacyAssemblerPath("pageDatabaseAssembler"));
    assertEquals(
        "Java/global/percussion/assembly/binaryAssembler",
        PSPageXmlTemplateDefEmitter.toLegacyAssemblerPath("binaryAssembler"));
    assertEquals(
        "Java/global/percussion/assembly/pageAssembler",
        PSPageXmlTemplateDefEmitter.toLegacyAssemblerPath(
            "Java/global/percussion/assembly/pageAssembler"));
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

  private static String readClasspath(String resource) throws Exception {
    try (var in = PSPageXmlDualShipTest.class.getResourceAsStream(resource)) {
      assertNotNull(in, "missing test resource: " + resource);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static String normalizeNewlines(String s) {
    return s == null ? null : s.replace("\r\n", "\n").replace('\r', '\n');
  }

  private static Path locatePackage(String packageName) {
    Path candidate =
        Path.of("src", "main", "resources", "Packages", packageName);
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

  private static void copyTree(Path source, Path target) throws Exception {
    Files.walk(source)
        .forEach(
            src -> {
              try {
                Path rel = source.relativize(src);
                Path dst = target.resolve(rel);
                if (Files.isDirectory(src)) {
                  Files.createDirectories(dst);
                } else {
                  Files.createDirectories(dst.getParent());
                  Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                }
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            });
  }
}
