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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.packages.manifest.PSComponentPackageManifest;
import com.percussion.packages.manifest.PSComponentPackageManifestIo;
import com.percussion.packages.manifest.PSComponentPackageManifestValidator;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
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
    assertEquals("0-4-557", guids.get("perc.base.Box"));
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
      assertEquals(guids.get(id), install.getGuid(), "GUID for " + id);
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
    assertEquals("0-4-597", guids.get("perc.resp.Banded"));
    assertEquals("0-4-599", guids.get("perc.resp.Basic"));
    assertEquals("0-4-627", guids.get("perc.resp.plain"));

    for (PSPageXmlCompileResult r : modern) {
      String id = r.getManifest().getId();
      assertTrue(Files.isRegularFile(staging.resolve(id + ".templateDef")));
      assertTrue(Boolean.TRUE.equals(r.getManifest().getCatalog().getResponsive()));
    }
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
