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

package com.percussion.packages.gadgetxml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.packages.manifest.PSComponentPackageManifest;
import com.percussion.packages.manifest.PSComponentPackageManifestIo;
import com.percussion.packages.manifest.PSComponentPackageManifestValidator;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit + golden parity tests for GadgetRegistry.xml → modern gadget catalog / component packages
 * (#2771 / ADR-004 Phase 3).
 */
class PSGadgetRegistryCompilerTest {

  private static final String FIXTURE_REGISTRY = "/gadgetxml/GadgetRegistry.xml";
  private static final String GOLDEN_WELCOME =
      "/gadgetxml/golden/cm1_welcome_gadget.component-package.json";
  private static final String GOLDEN_CATALOG = "/gadgetxml/golden/gadget-catalog.json";
  private static final String PRODUCT_VERSION = "8.2.0";

  @TempDir Path tempDir;

  @Test
  void parseProductRegistry_populatesGroupsAndEntries() throws Exception {
    PSGadgetRegistryModel model = parseClasspath(FIXTURE_REGISTRY, "GadgetRegistry.xml");

    assertEquals(21, model.getGadgets().size(), "product registry has 21 gadgets");
    assertEquals("Percussion", model.toNameGroupMap().get("Welcome"));
    assertEquals("Deprecated", model.toNameGroupMap().get("Activity"));
    assertFalse(model.toNameGroupMap().containsKey("Redirect Management"));

    PSGadgetRegistryEntry welcome = model.findByName("Welcome");
    assertNotNull(welcome);
    assertEquals("cm1_welcome_gadget", welcome.gadgetId());
    assertEquals("/cm/gadgets/repository/cm1_welcome_gadget", welcome.getBaseUri());
    assertEquals("perc_welcome_gadget.xml", welcome.getLegacyDefinitionFile());
    assertFalse(welcome.isDeprecated());

    PSGadgetRegistryEntry activity = model.findById("perc_activity_gadget");
    assertNotNull(activity);
    assertTrue(activity.isDeprecated());
  }

  @Test
  void compileWelcome_matchesGoldenManifest() throws Exception {
    PSGadgetRegistryModel model = parseClasspath(FIXTURE_REGISTRY, "GadgetRegistry.xml");
    PSGadgetRegistryCompileResult result =
        PSGadgetRegistryCompiler.compile(model, PRODUCT_VERSION);

    PSComponentPackageManifest welcome = result.getGadgetPackage("cm1_welcome_gadget");
    assertNotNull(welcome, "Welcome gadget package must be present");
    PSComponentPackageManifestValidator.validate(welcome);

    String expectedJson = readClasspath(GOLDEN_WELCOME);
    PSComponentPackageManifest golden = PSComponentPackageManifestIo.parse(expectedJson);
    PSComponentPackageManifest reparsed =
        PSComponentPackageManifestIo.parse(PSComponentPackageManifestIo.toJson(welcome));

    assertEquals(golden.getSchemaVersion(), reparsed.getSchemaVersion());
    assertEquals(golden.getId(), reparsed.getId());
    assertEquals(golden.getName(), reparsed.getName());
    assertEquals(golden.getVersion(), reparsed.getVersion());
    assertEquals(golden.getDescription(), reparsed.getDescription());
    assertEquals(golden.getPublisher(), reparsed.getPublisher());
    assertEquals(golden.getCatalog(), reparsed.getCatalog());
    assertEquals(golden.getResources(), reparsed.getResources());
    assertEquals(
        PSComponentPackageManifestIo.parse(expectedJson),
        reparsed,
        "compiled Welcome gadget package must equal golden fixture");
  }

  @Test
  void compileProductRegistry_matchesGoldenCatalog() throws Exception {
    PSGadgetRegistryModel model = parseClasspath(FIXTURE_REGISTRY, "GadgetRegistry.xml");
    PSGadgetRegistryCompileResult result =
        PSGadgetRegistryCompiler.compile(model, PRODUCT_VERSION);

    PSGadgetCatalog catalog = result.getCatalog();
    assertEquals(21, catalog.getGadgets().size());
    assertEquals(PRODUCT_VERSION, catalog.getVersion());
    assertEquals("perc.gadgetCatalog", catalog.getId());

    String expectedJson = readClasspath(GOLDEN_CATALOG);
    PSGadgetCatalog golden = PSGadgetCatalogIo.parse(expectedJson);
    PSGadgetCatalog reparsed = PSGadgetCatalogIo.parse(PSGadgetCatalogIo.toJson(catalog));
    assertEquals(golden, reparsed, "compiled gadget catalog must equal golden fixture");

    // All packages validate; deprecated ones hide from palette.
    for (Map.Entry<String, PSComponentPackageManifest> e : result.getGadgetPackages().entrySet()) {
      PSComponentPackageManifestValidator.validate(e.getValue());
      assertEquals(
          PSGadgetRegistryCompiler.CATALOG_KIND_GADGET, e.getValue().getCatalog().getKind());
    }
    assertEquals(Boolean.FALSE, result.getGadgetPackage("perc_activity_gadget").getCatalog().getPaletteVisible());
    assertEquals(Boolean.TRUE, result.getGadgetPackage("cm1_welcome_gadget").getCatalog().getPaletteVisible());
  }

  @Test
  void writeArtifacts_roundTripsCatalogAndWelcomePackage() throws Exception {
    PSGadgetRegistryModel model = parseClasspath(FIXTURE_REGISTRY, "GadgetRegistry.xml");
    PSGadgetRegistryCompileResult result =
        PSGadgetRegistryCompiler.compile(model, PRODUCT_VERSION);

    Path out = tempDir.resolve("gadget-out");
    PSGadgetRegistryCompiler.writeArtifacts(result, out);

    Path catalogPath = out.resolve(PSGadgetCatalog.DEFAULT_CATALOG_FILE_NAME);
    assertTrue(Files.isRegularFile(catalogPath));
    PSGadgetCatalog loaded = PSGadgetCatalogIo.read(catalogPath);
    assertEquals(21, loaded.getGadgets().size());

    Path welcomeManifest =
        out.resolve("gadgets")
            .resolve("cm1_welcome_gadget")
            .resolve(PSComponentPackageManifest.DEFAULT_MANIFEST_FILE_NAME);
    assertTrue(Files.isRegularFile(welcomeManifest));
    PSComponentPackageManifest welcome = PSComponentPackageManifestIo.read(welcomeManifest);
    PSComponentPackageManifestValidator.validate(welcome);
    assertEquals("cm1_welcome_gadget", welcome.getId());

    Path hostRef =
        out.resolve("gadgets")
            .resolve("cm1_welcome_gadget")
            .resolve("resources")
            .resolve("host-ref.txt");
    assertTrue(Files.isRegularFile(hostRef));
    String hostBody = normalizeNewlines(Files.readString(hostRef, StandardCharsets.UTF_8)).trim();
    assertEquals("cm/gadgets/repository/cm1_welcome_gadget", hostBody);
  }

  @Test
  void parseEmptyRegistry_throws() {
    assertThrows(PSGadgetRegistryException.class, () -> PSGadgetRegistryParser.parse(""));
    assertThrows(
        PSGadgetRegistryException.class,
        () -> PSGadgetRegistryParser.parse("<gadgets></gadgets>"));
  }

  @Test
  void gadgetPackageWithoutTitle_failsValidation() {
    PSComponentPackageManifest m = new PSComponentPackageManifest();
    m.setSchemaVersion("1.0");
    m.setId("bad-gadget");
    m.setName("Bad");
    m.setVersion("1.0.0");
    PSComponentPackageManifest.Catalog cat = new PSComponentPackageManifest.Catalog();
    cat.setKind("gadget");
    // title missing
    m.setCatalog(cat);
    assertThrows(
        Exception.class, () -> PSComponentPackageManifestValidator.validate(m));
  }

  private static PSGadgetRegistryModel parseClasspath(String resource, String sourceFileName)
      throws Exception {
    String xml = readClasspath(resource);
    PSGadgetRegistryModel model = PSGadgetRegistryParser.parse(xml);
    model.setSourceFileName(sourceFileName);
    return model;
  }

  private static String readClasspath(String resource) throws Exception {
    try (InputStream in = PSGadgetRegistryCompilerTest.class.getResourceAsStream(resource)) {
      assertNotNull(in, "missing classpath resource: " + resource);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static String normalizeNewlines(String s) {
    return s.replace("\r\n", "\n").replace('\r', '\n');
  }
}
