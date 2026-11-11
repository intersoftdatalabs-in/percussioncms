/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.percussion.packages.shim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.packages.gadgetxml.PSGadgetRegistryCompiler;
import com.percussion.packages.gadgetxml.PSGadgetRegistryCompileResult;
import com.percussion.packages.manifest.PSComponentPackageManifest;
import com.percussion.packages.manifest.PSComponentPackageManifestIo;
import com.percussion.packages.manifest.PSComponentPackageManifestValidator;
import com.percussion.packages.pagexml.PSPageXmlCompileResult;
import com.percussion.packages.pagexml.PSPageXmlCompiler;
import com.percussion.packages.pagexml.PSPageXmlModel;
import com.percussion.packages.pagexml.PSPageXmlParser;
import com.percussion.packages.widgetxml.PSWidgetXmlCompileResult;
import com.percussion.packages.widgetxml.PSWidgetXmlCompiler;
import com.percussion.packages.widgetxml.PSWidgetXmlModel;
import com.percussion.packages.widgetxml.PSWidgetXmlParser;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Customer legacy definition-XML upgrade-path verification (#6077, epic #2626).
 *
 * <p>The runtime dual-run shim is retired (modern-only selection, #2852): legacy Widget / Page /
 * Gadget definition XML is never loaded at runtime. The supported customer upgrade path is the
 * compilers — legacy XML as upgrade input, modern {@code component-package.json} as output —
 * followed by modern-only {@link PSLegacyDefinitionXmlShim} selection of the converted id. These
 * tests pin that full round trip for all three definition kinds with product fixtures, and pin the
 * clear retired-legacy failure a customer sees for an id that has not been converted yet.
 *
 * <p>Filesystem APIs use {@link Path} / {@link Files} (portable Windows / Linux / macOS).
 */
class PSCustomerLegacyXmlUpgradePathTest {

  private static final String WIDGET_DEFINITION_ID = "percSimpleText";
  private static final String WIDGET_FIXTURE = "/widgetxml/percSimpleText.xml";

  private static final String PAGE_DEFINITION_ID = "perc.base.plain";
  private static final String PAGE_FIXTURE = "/pagexml/perc.base.plain.templateDef";

  private static final String GADGET_DEFINITION_ID = "cm1_welcome_gadget";
  private static final String GADGET_FIXTURE = "/gadgetxml/GadgetRegistry.xml";

  @TempDir Path tempDir;

  @Test
  void widgetLegacyXml_compilesThroughNormalizedPath_resolvesModernSelection() throws Exception {
    PSWidgetXmlModel model = parseWidgetFixture(WIDGET_FIXTURE, WIDGET_DEFINITION_ID + ".xml");
    PSWidgetXmlCompileResult result = PSWidgetXmlCompiler.compile(model, null);
    PSComponentPackageManifest manifest = result.getManifest();
    assertEquals(WIDGET_DEFINITION_ID, manifest.getId());
    PSComponentPackageManifestValidator.validate(manifest);

    // Customer package root: converted modern package plus the retired legacy leftover.
    Path packageRoot = tempDir.resolve("customerWidgets");
    Path modernDir = packageRoot.resolve("widgets").resolve(WIDGET_DEFINITION_ID);
    Files.createDirectories(modernDir);
    Path manifestPath =
        modernDir.resolve(PSLegacyDefinitionXmlShim.MODERN_MANIFEST_FILE_NAME);
    PSComponentPackageManifestIo.write(manifest, manifestPath);

    Path legacyWidgetsDir = packageRoot.resolve("rxconfig").resolve("Widgets");
    Files.createDirectories(legacyWidgetsDir);
    Files.writeString(
        legacyWidgetsDir.resolve(WIDGET_DEFINITION_ID + ".xml"),
        readFixture(WIDGET_FIXTURE),
        StandardCharsets.UTF_8);

    PSDefinitionSourceSelection selection =
        PSLegacyDefinitionXmlShim.selectDefinition(
            WIDGET_DEFINITION_ID, List.of(packageRoot), legacyWidgetsDir, null, null);
    assertEquals(PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE, selection.getKind());
    assertTrue(selection.isModern());
    assertEquals(manifestPath, selection.getPrimaryPath().orElseThrow());
    assertEquals(WIDGET_DEFINITION_ID, selection.getDefinitionId().orElseThrow());
  }

  @Test
  void pageLegacyTemplateDef_compilesThroughNormalizedPath_resolvesModernSelection()
      throws Exception {
    PSPageXmlModel model = parsePageFixture(PAGE_FIXTURE, PAGE_DEFINITION_ID + ".templateDef");
    PSPageXmlCompileResult result = PSPageXmlCompiler.compile(model, null);
    PSComponentPackageManifest manifest = result.getManifest();
    assertEquals(PAGE_DEFINITION_ID, manifest.getId());
    PSComponentPackageManifestValidator.validate(manifest);

    Path packageRoot = tempDir.resolve("customerPages");
    Path modernDir = packageRoot.resolve(PAGE_DEFINITION_ID);
    Files.createDirectories(modernDir);
    Path manifestPath =
        modernDir.resolve(PSLegacyDefinitionXmlShim.MODERN_MANIFEST_FILE_NAME);
    PSComponentPackageManifestIo.write(manifest, manifestPath);

    PSDefinitionSourceSelection selection =
        PSLegacyDefinitionXmlShim.selectDefinition(
            PAGE_DEFINITION_ID, List.of(modernDir), null, null, null);
    assertEquals(PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE, selection.getKind());
    assertTrue(selection.isModern());
    assertEquals(manifestPath, selection.getPrimaryPath().orElseThrow());
  }

  @Test
  void gadgetLegacyRegistry_compilesThroughNormalizedPath_resolvesModernSelection()
      throws Exception {
    Path registryCopy = tempDir.resolve("GadgetRegistry.xml");
    Files.writeString(registryCopy, readFixture(GADGET_FIXTURE), StandardCharsets.UTF_8);
    PSGadgetRegistryCompileResult result =
        PSGadgetRegistryCompiler.compile(registryCopy, "8.2.0");
    PSComponentPackageManifest manifest = result.getGadgetPackage(GADGET_DEFINITION_ID);
    assertNotNull(manifest, "Welcome gadget package must be present");
    PSComponentPackageManifestValidator.validate(manifest);

    Path packageRoot = tempDir.resolve("customerGadgets");
    Path modernDir = packageRoot.resolve(GADGET_DEFINITION_ID);
    Files.createDirectories(modernDir);
    Path manifestPath =
        modernDir.resolve(PSLegacyDefinitionXmlShim.MODERN_MANIFEST_FILE_NAME);
    PSComponentPackageManifestIo.write(manifest, manifestPath);

    PSDefinitionSourceSelection selection =
        PSLegacyDefinitionXmlShim.selectDefinition(
            GADGET_DEFINITION_ID, List.of(modernDir), null, null, null);
    assertEquals(PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE, selection.getKind());
    assertTrue(selection.isModern());
    assertEquals(manifestPath, selection.getPrimaryPath().orElseThrow());
  }

  @Test
  @SuppressWarnings("deprecation") // pins wouldUseLegacyShim == false after retirement (#2852)
  void legacyOnlyCustomerXml_throwsRetiredErrorNamingCompilerUpgradePath() throws Exception {
    Path legacyWidgetsDir = tempDir.resolve("legacyWidgets");
    Files.createDirectories(legacyWidgetsDir);
    Files.writeString(
        legacyWidgetsDir.resolve(WIDGET_DEFINITION_ID + ".xml"),
        readFixture(WIDGET_FIXTURE),
        StandardCharsets.UTF_8);

    PSDefinitionSourceNotFoundException ex =
        assertThrows(
            PSDefinitionSourceNotFoundException.class,
            () ->
                PSLegacyDefinitionXmlShim.selectDefinition(
                    WIDGET_DEFINITION_ID, List.of(), legacyWidgetsDir, null, null));
    assertTrue(
        ex.getMessage().contains("compiler"),
        "retired error must point at the compiler upgrade path: " + ex.getMessage());
    assertTrue(
        ex.getMessage().contains(WIDGET_DEFINITION_ID + ".xml"),
        "retired error must name the retired file: " + ex.getMessage());
    assertFalse(PSLegacyDefinitionXmlShim.wouldUseLegacyShim(tempDir));
  }

  private static PSWidgetXmlModel parseWidgetFixture(String resource, String fileName)
      throws Exception {
    try (InputStream in = PSCustomerLegacyXmlUpgradePathTest.class.getResourceAsStream(resource)) {
      assertNotNull(in, "missing test resource: " + resource);
      return PSWidgetXmlParser.parse(in, fileName);
    }
  }

  private static PSPageXmlModel parsePageFixture(String resource, String fileName)
      throws Exception {
    try (InputStream in = PSCustomerLegacyXmlUpgradePathTest.class.getResourceAsStream(resource)) {
      assertNotNull(in, "missing test resource: " + resource);
      return PSPageXmlParser.parse(in, fileName);
    }
  }

  private static String readFixture(String resource) throws Exception {
    try (InputStream in = PSCustomerLegacyXmlUpgradePathTest.class.getResourceAsStream(resource)) {
      assertNotNull(in, "missing test resource: " + resource);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
