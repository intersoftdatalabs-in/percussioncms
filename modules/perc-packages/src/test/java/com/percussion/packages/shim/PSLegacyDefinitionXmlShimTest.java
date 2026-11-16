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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Behavioral tests for modern-only definition selection (issue #2852 / ADR-004; legacy fallback
 * retired).
 *
 * <p>Policy: modern component package required; legacy Widget/Page/Gadget XML never selected —
 * modern-absent (even with retired legacy present) fails with a clear {@link
 * PSDefinitionSourceNotFoundException} (no silent invent, no legacy load). The Widget (#6440)
 * and Page (#6446) legacy probes are removed (on-disk Widget/Page XML is inert and unnamed);
 * only the retired Gadget probe remains (sibling slice #6447).
 */
class PSLegacyDefinitionXmlShimTest {

  @TempDir Path tempDir;

  @Test
  void selectByPresence_prefersModernEvenWhenLegacyAlsoPresent() throws Exception {
    PSDefinitionSourceSelection s =
        PSLegacyDefinitionXmlShim.selectByPresence("percSimpleText", true, true);
    assertEquals(PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE, s.getKind());
    assertTrue(s.isModern());
    assertFalse(s.isLegacyXml());
  }

  @Test
  void selectByPresence_legacyOnlyThrowsRetiredError() {
    // Page legacy path removed (#6446): only the Gadget presence flag remains — the retired
    // error names "(Gadget)", never a Page branch.
    PSDefinitionSourceNotFoundException ex =
        assertThrows(
            PSDefinitionSourceNotFoundException.class,
            () -> PSLegacyDefinitionXmlShim.selectByPresence("w1", false, true));
    assertRetiredMessage(ex, "w1");
    assertTrue(ex.getMessage().contains("(Gadget)"), ex.getMessage());
  }

  @Test
  void selectByPresence_neitherThrowsClearError() {
    PSDefinitionSourceNotFoundException ex =
        assertThrows(
            PSDefinitionSourceNotFoundException.class,
            () ->
                PSLegacyDefinitionXmlShim.selectByPresence("missingWidget", false, false));
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
  void selectForPackageRoot_legacyWidgetOnlyThrowsRetiredErrorWithoutWidgetHint()
      throws Exception {
    // Widget legacy path removed (#6440): the retired Widget XML under the package root is
    // inert — the error no longer names it, and the package never selects legacy.
    Path pkg = tempDir.resolve("legacyPkg");
    Path widgets = pkg.resolve("sys__UserDependency--rxconfig").resolve("Widgets");
    Files.createDirectories(widgets);
    Files.writeString(
        widgets.resolve("percSimpleText.xml"), "<Widget id=\"percSimpleText\"/>", StandardCharsets.UTF_8);

    PSDefinitionSourceNotFoundException ex =
        assertThrows(
            PSDefinitionSourceNotFoundException.class,
            () -> PSLegacyDefinitionXmlShim.selectForPackageRoot(pkg));
    assertRetiredMessage(ex, "legacyPkg");
    assertFalse(
        ex.getMessage().contains("percSimpleText.xml"),
        "removed widget hint must not name the retired file: " + ex.getMessage());
    assertFalse(PSLegacyDefinitionXmlShim.wouldUseLegacyShim(pkg));
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
  void selectForPackageRoot_installRelativeWidgetsPathThrowsRetiredError() throws Exception {
    Path pkg = tempDir.resolve("installStyle");
    Path widgets = pkg.resolve("rxconfig").resolve("Widgets");
    Files.createDirectories(widgets);
    Files.writeString(widgets.resolve("w.xml"), "<Widget/>", StandardCharsets.UTF_8);

    PSDefinitionSourceNotFoundException ex =
        assertThrows(
            PSDefinitionSourceNotFoundException.class,
            () -> PSLegacyDefinitionXmlShim.selectForPackageRoot(pkg));
    assertRetiredMessage(ex, "installStyle");
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
  void selectDefinition_modernWinsSingleSource() throws Exception {
    Path modernRoot = tempDir.resolve("percSimpleText");
    Files.createDirectories(modernRoot);
    Path manifest = modernRoot.resolve(PSLegacyDefinitionXmlShim.MODERN_MANIFEST_FILE_NAME);
    Files.writeString(manifest, "{}", StandardCharsets.UTF_8);

    PSDefinitionSourceSelection s =
        PSLegacyDefinitionXmlShim.selectDefinition("percSimpleText", List.of(modernRoot), null);
    assertEquals(PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE, s.getKind());
    assertEquals(manifest, s.getPrimaryPath().orElseThrow());
  }

  @Test
  void selectDefinition_legacyWidgetOnlyThrowsRetiredErrorWithoutWidgetProbe() throws Exception {
    // Widget legacy path removed (#6440): on-disk Widget XML is not even probed — the retired
    // error names no widget file and no LEGACY_WIDGET_XML kind exists to select.
    Path widgets = tempDir.resolve("rxconfig").resolve("Widgets");
    Files.createDirectories(widgets);
    Files.writeString(widgets.resolve("percRawHtml.xml"), "<Widget/>", StandardCharsets.UTF_8);

    PSDefinitionSourceNotFoundException ex =
        assertThrows(
            PSDefinitionSourceNotFoundException.class,
            () -> PSLegacyDefinitionXmlShim.selectDefinition("percRawHtml", List.of(), null));
    assertEquals("percRawHtml", ex.getDefinitionId());
    assertRetiredMessage(ex, "percRawHtml");
    assertFalse(
        ex.getMessage().contains("percRawHtml.xml"),
        "removed widget probe must not name the retired file: " + ex.getMessage());
    for (PSDefinitionSourceKind kind : PSDefinitionSourceKind.values()) {
      assertFalse(
          kind.name().contains("WIDGET"),
          "no LEGACY_WIDGET_* kind may remain: " + kind);
    }
  }

  @Test
  void selectDefinition_legacyPageOnlyThrowsRetiredErrorWithoutPageProbe() throws Exception {
    // Page legacy path removed (#6446): on-disk Page XML is not even probed — the retired
    // error names no page file and no LEGACY_PAGE_XML kind exists to select.
    Path pages = tempDir.resolve("rxconfig").resolve("Pages");
    Files.createDirectories(pages);
    Files.writeString(pages.resolve("homeMeta.xml"), "<Page/>", StandardCharsets.UTF_8);

    PSDefinitionSourceNotFoundException ex =
        assertThrows(
            PSDefinitionSourceNotFoundException.class,
            () -> PSLegacyDefinitionXmlShim.selectDefinition("homeMeta", List.of(), null));
    assertEquals("homeMeta", ex.getDefinitionId());
    assertRetiredMessage(ex, "homeMeta");
    assertFalse(
        ex.getMessage().contains("homeMeta.xml"),
        "removed page probe must not name the retired file: " + ex.getMessage());
    for (PSDefinitionSourceKind kind : PSDefinitionSourceKind.values()) {
      assertFalse(
          kind.name().contains("PAGE"),
          "no LEGACY_PAGE_* kind may remain: " + kind);
    }
  }

  @Test
  void selectDefinition_neitherThrowsOperatorFacingMessage() {
    PSDefinitionSourceNotFoundException ex =
        assertThrows(
            PSDefinitionSourceNotFoundException.class,
            () ->
                PSLegacyDefinitionXmlShim.selectDefinition("noSuchWidget", List.of(), null));
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
        () -> PSLegacyDefinitionXmlShim.selectDefinition("  ", List.of(), null));
  }

  @Test
  void selectDefinition_gadgetOnlyThrowsRetiredError() throws Exception {
    Path gadgets = tempDir.resolve("Gadgets");
    Files.createDirectories(gadgets);
    Files.writeString(gadgets.resolve("myGadget.xml"), "<Module/>", StandardCharsets.UTF_8);

    PSDefinitionSourceNotFoundException gadgetEx =
        assertThrows(
            PSDefinitionSourceNotFoundException.class,
            () ->
                PSLegacyDefinitionXmlShim.selectDefinition("myGadget", List.of(), gadgets));
    assertRetiredMessage(gadgetEx, "myGadget");
  }

  @Test
  void wouldUseLegacyShim_alwaysFalseAfterRetirement() throws Exception {
    Path pkg = tempDir.resolve("legacyProbe");
    Path widgets = pkg.resolve("rxconfig").resolve("Widgets");
    Files.createDirectories(widgets);
    Files.writeString(widgets.resolve("w.xml"), "<Widget/>", StandardCharsets.UTF_8);

    assertFalse(PSLegacyDefinitionXmlShim.wouldUseLegacyShim(pkg));
    assertFalse(PSLegacyDefinitionXmlShim.wouldUseLegacyShim(tempDir.resolve("missing")));
  }

  private static void assertRetiredMessage(
      PSDefinitionSourceNotFoundException ex, String definitionId) {
    assertEquals(definitionId, ex.getDefinitionId());
    String msg = ex.getMessage();
    assertTrue(msg.contains("component-package.json"), msg);
    assertTrue(msg.contains("retired"), msg);
    assertTrue(msg.contains("#2852"), msg);
    assertTrue(msg.contains(definitionId), msg);
  }
}
