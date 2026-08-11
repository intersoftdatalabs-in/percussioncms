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

package com.percussion.packages.shim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Behavioral tests for runtime legacy definition-XML shim selection (issue #2752 / ADR-004).
 *
 * <p>Policy: modern component package preferred; legacy Widget/Page/Gadget XML when modern absent;
 * clear error when neither is present.
 */
class PSLegacyDefinitionXmlShimTest {

  @TempDir Path tempDir;

  @Test
  void selectByPresence_prefersModernEvenWhenLegacyAlsoPresent() throws Exception {
    PSDefinitionSourceSelection s =
        PSLegacyDefinitionXmlShim.selectByPresence(
            "percSimpleText", true, true, true, true);
    assertEquals(PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE, s.getKind());
    assertTrue(s.isModern());
    assertFalse(s.isLegacyXml());
  }

  @Test
  void selectByPresence_fallsBackToWidgetXmlWhenModernAbsent() throws Exception {
    PSDefinitionSourceSelection s =
        PSLegacyDefinitionXmlShim.selectByPresence("w1", false, true, true, true);
    assertEquals(PSDefinitionSourceKind.LEGACY_WIDGET_XML, s.getKind());
    assertTrue(s.isLegacyXml());
  }

  @Test
  void selectByPresence_pageThenGadgetWhenWidgetAbsent() throws Exception {
    PSDefinitionSourceSelection page =
        PSLegacyDefinitionXmlShim.selectByPresence("p1", false, false, true, true);
    assertEquals(PSDefinitionSourceKind.LEGACY_PAGE_XML, page.getKind());

    PSDefinitionSourceSelection gadget =
        PSLegacyDefinitionXmlShim.selectByPresence("g1", false, false, false, true);
    assertEquals(PSDefinitionSourceKind.LEGACY_GADGET_XML, gadget.getKind());
  }

  @Test
  void selectByPresence_neitherThrowsClearError() {
    PSDefinitionSourceNotFoundException ex =
        assertThrows(
            PSDefinitionSourceNotFoundException.class,
            () ->
                PSLegacyDefinitionXmlShim.selectByPresence(
                    "missingWidget", false, false, false, false));
    assertEquals("missingWidget", ex.getDefinitionId());
    assertTrue(ex.getMessage().contains("component-package.json"));
    assertTrue(ex.getMessage().contains("legacy"));
    assertTrue(ex.getMessage().contains("missingWidget"));
  }

  @Test
  void selectForPackageRoot_modernPreferredWhenBothPresent() throws Exception {
    Path pkg = tempDir.resolve("perc.widget.demo");
    Files.createDirectories(pkg);
    Path manifest = pkg.resolve(PSLegacyDefinitionXmlShim.MODERN_MANIFEST_FILE_NAME);
    Files.writeString(manifest, "{\"schemaVersion\":\"1.0\",\"id\":\"demo\"}", StandardCharsets.UTF_8);

    Path widgets = pkg.resolve("sys__UserDependency--rxconfig").resolve("Widgets");
    Files.createDirectories(widgets);
    Files.writeString(widgets.resolve("demo.xml"), "<Widget/>", StandardCharsets.UTF_8);

    PSDefinitionSourceSelection s = PSLegacyDefinitionXmlShim.selectForPackageRoot(pkg);
    assertEquals(PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE, s.getKind());
    assertEquals(manifest, s.getPrimaryPath().orElseThrow());
    assertEquals("perc.widget.demo", s.getDefinitionId().orElseThrow());
  }

  @Test
  void selectForPackageRoot_legacyWidgetWhenModernAbsent() throws Exception {
    Path pkg = tempDir.resolve("legacyPkg");
    Path widgets = pkg.resolve("sys__UserDependency--rxconfig").resolve("Widgets");
    Files.createDirectories(widgets);
    Path xml = widgets.resolve("percSimpleText.xml");
    Files.writeString(xml, "<Widget id=\"percSimpleText\"/>", StandardCharsets.UTF_8);

    PSDefinitionSourceSelection s = PSLegacyDefinitionXmlShim.selectForPackageRoot(pkg);
    assertEquals(PSDefinitionSourceKind.LEGACY_WIDGET_XML, s.getKind());
    assertEquals(xml, s.getPrimaryPath().orElseThrow());
    assertTrue(PSLegacyDefinitionXmlShim.wouldUseLegacyShim(pkg));
  }

  @Test
  void selectForPackageRoot_prefersDualShipWidgetsDirOverLegacyXml() throws Exception {
    Path pkg = tempDir.resolve("perc.baseWidgets");
    Path modernWidget = pkg.resolve("widgets").resolve("percSimpleText");
    Files.createDirectories(modernWidget);
    Path manifest = modernWidget.resolve(PSLegacyDefinitionXmlShim.MODERN_MANIFEST_FILE_NAME);
    Files.writeString(
        manifest,
        "{\"schemaVersion\":\"1.0\",\"id\":\"percSimpleText\",\"name\":\"Simple Text\",\"version\":\"1.0.0\"}",
        StandardCharsets.UTF_8);

    Path widgets = pkg.resolve("sys__UserDependency--rxconfig").resolve("Widgets");
    Files.createDirectories(widgets);
    Files.writeString(widgets.resolve("percSimpleText.xml"), "<Widget/>", StandardCharsets.UTF_8);

    PSDefinitionSourceSelection s = PSLegacyDefinitionXmlShim.selectForPackageRoot(pkg);
    assertEquals(PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE, s.getKind());
    assertEquals(manifest, s.getPrimaryPath().orElseThrow());
    assertFalse(PSLegacyDefinitionXmlShim.wouldUseLegacyShim(pkg));
  }

  @Test
  void selectForPackageRoot_installRelativeWidgetsPath() throws Exception {
    Path pkg = tempDir.resolve("installStyle");
    Path widgets = pkg.resolve("rxconfig").resolve("Widgets");
    Files.createDirectories(widgets);
    Path xml = widgets.resolve("w.xml");
    Files.writeString(xml, "<Widget/>", StandardCharsets.UTF_8);

    PSDefinitionSourceSelection s = PSLegacyDefinitionXmlShim.selectForPackageRoot(pkg);
    assertEquals(PSDefinitionSourceKind.LEGACY_WIDGET_XML, s.getKind());
    assertEquals(xml, s.getPrimaryPath().orElseThrow());
  }

  @Test
  void selectForPackageRoot_neitherThrows() throws Exception {
    Path pkg = tempDir.resolve("emptyPkg");
    Files.createDirectories(pkg);

    PSDefinitionSourceNotFoundException ex =
        assertThrows(
            PSDefinitionSourceNotFoundException.class,
            () -> PSLegacyDefinitionXmlShim.selectForPackageRoot(pkg));
    assertTrue(ex.getMessage().contains(PSLegacyDefinitionXmlShim.MODERN_MANIFEST_FILE_NAME));
    assertFalse(PSLegacyDefinitionXmlShim.wouldUseLegacyShim(pkg));
  }

  @Test
  void selectDefinition_modernWinsOverLegacyXml() throws Exception {
    Path modernRoot = tempDir.resolve("percSimpleText");
    Files.createDirectories(modernRoot);
    Path manifest = modernRoot.resolve(PSLegacyDefinitionXmlShim.MODERN_MANIFEST_FILE_NAME);
    Files.writeString(manifest, "{}", StandardCharsets.UTF_8);

    Path widgets = tempDir.resolve("Widgets");
    Files.createDirectories(widgets);
    Files.writeString(widgets.resolve("percSimpleText.xml"), "<Widget/>", StandardCharsets.UTF_8);

    PSDefinitionSourceSelection s =
        PSLegacyDefinitionXmlShim.selectDefinition(
            "percSimpleText", List.of(modernRoot), widgets, null, null);
    assertEquals(PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE, s.getKind());
    assertEquals(manifest, s.getPrimaryPath().orElseThrow());
  }

  @Test
  void selectDefinition_legacyWidgetWhenNoModern() throws Exception {
    Path widgets = tempDir.resolve("rxconfig").resolve("Widgets");
    Files.createDirectories(widgets);
    Path xml = widgets.resolve("percRawHtml.xml");
    Files.writeString(xml, "<Widget/>", StandardCharsets.UTF_8);

    PSDefinitionSourceSelection s =
        PSLegacyDefinitionXmlShim.selectDefinition(
            "percRawHtml", List.of(), widgets, null, null);
    assertEquals(PSDefinitionSourceKind.LEGACY_WIDGET_XML, s.getKind());
    assertEquals(xml, s.getPrimaryPath().orElseThrow());
  }

  @Test
  void selectDefinition_neitherThrowsOperatorFacingMessage() {
    Path widgets = tempDir.resolve("WidgetsEmpty");
    try {
      Files.createDirectories(widgets);
    } catch (IOException e) {
      throw new AssertionError(e);
    }

    PSDefinitionSourceNotFoundException ex =
        assertThrows(
            PSDefinitionSourceNotFoundException.class,
            () ->
                PSLegacyDefinitionXmlShim.selectDefinition(
                    "noSuchWidget", List.of(), widgets, null, null));
    assertEquals("noSuchWidget", ex.getDefinitionId());
    String msg = ex.getMessage();
    assertTrue(msg.contains("noSuchWidget"));
    assertTrue(msg.contains("component-package.json") || msg.contains("modern"));
    assertTrue(msg.contains("legacy") || msg.contains("XML"));
  }

  @Test
  void selectDefinition_blankIdRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSLegacyDefinitionXmlShim.selectDefinition("  ", List.of(), null, null, null));
  }

  @Test
  void selectDefinition_fallsBackToPageThenGadget() throws Exception {
    Path pages = tempDir.resolve("Pages");
    Files.createDirectories(pages);
    Path pageXml = pages.resolve("homeMeta.xml");
    Files.writeString(pageXml, "<Page/>", StandardCharsets.UTF_8);

    PSDefinitionSourceSelection page =
        PSLegacyDefinitionXmlShim.selectDefinition("homeMeta", List.of(), null, pages, null);
    assertEquals(PSDefinitionSourceKind.LEGACY_PAGE_XML, page.getKind());

    Path gadgets = tempDir.resolve("Gadgets");
    Files.createDirectories(gadgets);
    Path gadgetXml = gadgets.resolve("myGadget.xml");
    Files.writeString(gadgetXml, "<Module/>", StandardCharsets.UTF_8);

    PSDefinitionSourceSelection gadget =
        PSLegacyDefinitionXmlShim.selectDefinition("myGadget", List.of(), null, null, gadgets);
    assertEquals(PSDefinitionSourceKind.LEGACY_GADGET_XML, gadget.getKind());
  }
}
