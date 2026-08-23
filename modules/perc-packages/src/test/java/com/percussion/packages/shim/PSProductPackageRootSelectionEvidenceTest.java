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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.packages.widgetxml.PSWidgetDefinitionXmlInventory;
import com.percussion.packages.widgetxml.PSWidgetXmlDualShip;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * M2 product / H2 zero-legacy-selection evidence harness (#3583 / parent #2630).
 *
 * <p>Asserts product package roots and H2-style {@code Packages/Modern} trees select modern-first
 * ({@code wouldUseLegacyShim == false} / {@link PSDefinitionSourceKind#MODERN_COMPONENT_PACKAGE}).
 * Fails on unexpected {@code LEGACY_*} for non-waived widgets. After #3736 {@code perc.Test} is
 * modern-first. Customer-only ids may still select {@link
 * PSDefinitionSourceKind#LEGACY_WIDGET_XML} — the shim stays (#2852). This is not M2 PASS overall
 * (M3 still FAIL).
 *
 * <p>Cross-platform: {@link Path} / {@link Files} only.
 */
class PSProductPackageRootSelectionEvidenceTest {

  @TempDir Path tempDir;

  @Test
  void knownProductWidgetStems_coverBatchesABCAndPercTest() {
    List<String> stems = PSProductPackageRootSelectionEvidence.KNOWN_PRODUCT_WIDGET_STEMS;
    assertEquals(48, stems.size(), "8 + 20 + 19 + 1 perc.Test widget stems");
    assertTrue(stems.containsAll(PSWidgetXmlDualShip.BATCH_A_WIDGET_STEMS));
    assertTrue(stems.containsAll(PSWidgetXmlDualShip.BATCH_B_WIDGET_STEMS));
    assertTrue(stems.containsAll(PSWidgetXmlDualShip.BATCH_C_WIDGET_STEMS));
    assertTrue(stems.containsAll(PSWidgetXmlDualShip.TEST_WIDGET_STEMS));
    assertTrue(stems.contains("PSWidget_TestProperties"));
    assertEquals(Set.of(), PSWidgetDefinitionXmlInventory.WAIVED_PACKAGE_DIRS);
  }

  @Test
  void productPackagesTree_nonWaivedWidgetRootsAreModernFirst() throws Exception {
    Path packagesRoot = locatePackagesRoot();
    assertNotNull(
        packagesRoot,
        "Packages root must be visible when running module Surefire from perc-packages");

    PSProductPackageRootSelectionEvidence.Report report =
        PSProductPackageRootSelectionEvidence.scanWidgetPackageRoots(packagesRoot);

    assertTrue(
        report.isClean(),
        () ->
            "unexpected non-waived LEGACY_* on product widget roots: "
                + report.unexpectedLegacyRoots());
    assertTrue(
        report.modernRootCount() >= 39,
        "expected at least batch A+B+C+perc.Test package dirs (5+14+19+1) to select MODERN; got "
            + report.modernRootCount()
            + " roots="
            + report.roots());
    assertEquals(
        0,
        report.waivedLegacyRootCount(),
        "perc.Test is no longer waived and must not select LEGACY_WIDGET_XML");

    Set<String> modernPackages = new HashSet<>();
    for (PSProductPackageRootSelectionEvidence.RootFinding f : report.roots()) {
      if (f.kind() == PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE) {
        assertFalse(PSLegacyDefinitionXmlShim.wouldUseLegacyShim(f.packageRoot()), f.packageDirName());
        modernPackages.add(f.packageDirName());
      }
      assertFalse(f.waived(), f.packageDirName());
    }
    for (String pkg : PSWidgetXmlDualShip.BATCH_A_PACKAGE_DIRS) {
      assertTrue(modernPackages.contains(pkg), "batch A missing modern-first: " + pkg);
    }
    for (String pkg : PSWidgetXmlDualShip.BATCH_B_PACKAGE_DIRS) {
      assertTrue(modernPackages.contains(pkg), "batch B missing modern-first: " + pkg);
    }
    for (String pkg : PSWidgetXmlDualShip.BATCH_C_PACKAGE_DIRS) {
      assertTrue(modernPackages.contains(pkg), "batch C missing modern-first: " + pkg);
    }
    for (String pkg : PSWidgetXmlDualShip.TEST_PACKAGE_DIRS) {
      assertTrue(modernPackages.contains(pkg), "perc.Test missing modern-first: " + pkg);
    }

    PSProductPackageRootSelectionEvidence.assertNoUnexpectedLegacyOnWidgetPackageRoots(
        packagesRoot);

    List<String> modernIds =
        PSProductPackageRootSelectionEvidence.listModernWidgetDefinitionIds(packagesRoot);
    assertTrue(
        modernIds.containsAll(PSProductPackageRootSelectionEvidence.KNOWN_PRODUCT_WIDGET_STEMS),
        () ->
            "product modern widgets missing known stems: expected "
                + PSProductPackageRootSelectionEvidence.KNOWN_PRODUCT_WIDGET_STEMS
                + " found "
                + modernIds);

    List<Path> modernRoots = new java.util.ArrayList<>();
    for (PSProductPackageRootSelectionEvidence.RootFinding f : report.roots()) {
      if (f.kind() == PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE) {
        modernRoots.add(f.packageRoot());
      }
    }
    PSProductPackageRootSelectionEvidence.assertNoUnexpectedLegacyDefinitions(
        modernRoots, null, modernIds, Set.of());
  }

  @Test
  void h2ClasspathMaterialize_discoveredRootsAreModernFirst() throws Exception {
    Path rx = tempDir.resolve("h2-rx");
    Files.createDirectories(rx);
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (cl == null) {
      cl = getClass().getClassLoader();
    }
    List<Path> roots = PSModernPackageRootDefaults.resolve(null, rx, cl);
    assertFalse(
        roots.isEmpty(),
        "H2 blank-property resolve must materialize product modern roots from classpath");

    PSProductPackageRootSelectionEvidence.Report report =
        PSProductPackageRootSelectionEvidence.scanDiscoveredModernRoots(roots);
    assertTrue(
        report.isClean(),
        () -> "H2 modern roots unexpected LEGACY_*: " + report.unexpectedLegacyRoots());
    assertEquals(0, report.unexpectedLegacyRoots().size());
    assertTrue(report.modernRootCount() >= 39, "H2 modern root count=" + report.modernRootCount());
    for (PSProductPackageRootSelectionEvidence.RootFinding f : report.roots()) {
      assertEquals(PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE, f.kind(), f.packageDirName());
      assertFalse(f.wouldUseLegacyShim(), f.packageDirName());
    }
    PSProductPackageRootSelectionEvidence.assertNoUnexpectedLegacyOnDiscoveredModernRoots(roots);

    List<String> ids = PSProductPackageRootSelectionEvidence.listModernWidgetDefinitionIds(roots);
    assertTrue(
        ids.containsAll(PSProductPackageRootSelectionEvidence.KNOWN_PRODUCT_WIDGET_STEMS),
        () -> "H2 materialized stems missing known product widgets: " + ids);

    Path widgetsDir = rx.resolve("rxconfig").resolve("Widgets");
    Files.createDirectories(widgetsDir);
    for (String id : ids) {
      Files.writeString(
          widgetsDir.resolve(id + ".xml"),
          "<Widget id=\"" + id + "\"/>",
          StandardCharsets.UTF_8);
    }
    // Customer-only install XML must still select LEGACY (shim kept).
    Files.writeString(
        widgetsDir.resolve("customerOnlyXml.xml"),
        "<Widget id=\"customerOnlyXml\"/>",
        StandardCharsets.UTF_8);

    PSProductPackageRootSelectionEvidence.assertNoUnexpectedLegacyDefinitions(
        roots, widgetsDir, ids, Set.of());

    PSProductPackageRootSelectionEvidence.Report defs =
        PSProductPackageRootSelectionEvidence.scanDefinitionSelection(
            roots, widgetsDir, List.of("customerOnlyXml"), Set.of());
    assertEquals(1, defs.definitions().size());
    assertEquals(
        PSDefinitionSourceKind.LEGACY_WIDGET_XML, defs.definitions().get(0).kind());
    assertTrue(defs.definitions().get(0).isUnexpectedLegacy());
    // Customer-only is not a product widget — harness fails only when the caller
    // includes it as a non-waived product id. Product scan above excluded it.
  }

  @Test
  void tempTree_dummyNonWaivedLegacyOnly_failsGate() throws Exception {
    Path packages = tempDir.resolve("Packages");
    Path widgets =
        packages
            .resolve("perc.baseWidgets")
            .resolve("sys__UserDependency--rxconfig")
            .resolve("Widgets");
    Files.createDirectories(widgets);
    Files.writeString(widgets.resolve("percSimpleText.xml"), "<Widget/>", StandardCharsets.UTF_8);

    PSProductPackageRootSelectionEvidence.Report report =
        PSProductPackageRootSelectionEvidence.scanWidgetPackageRoots(packages);
    assertFalse(report.isClean());
    assertEquals(1, report.unexpectedLegacyRoots().size());
    assertEquals("perc.baseWidgets", report.unexpectedLegacyRoots().get(0).packageDirName());
    assertEquals(
        PSDefinitionSourceKind.LEGACY_WIDGET_XML, report.unexpectedLegacyRoots().get(0).kind());
    assertTrue(report.unexpectedLegacyRoots().get(0).wouldUseLegacyShim());

    IllegalStateException err =
        assertThrows(
            IllegalStateException.class,
            () ->
                PSProductPackageRootSelectionEvidence.assertNoUnexpectedLegacyOnWidgetPackageRoots(
                    packages));
    assertTrue(err.getMessage().contains("#3583"));
    assertTrue(err.getMessage().contains("perc.baseWidgets"));
    assertTrue(err.getMessage().contains("LEGACY"));
  }

  @Test
  void tempTree_percTestLegacyOnly_failsGateAfterWaiverDrop() throws Exception {
    Path packages = tempDir.resolve("Packages");
    Path widgets =
        packages
            .resolve("perc.Test")
            .resolve("sys__UserDependency--rxconfig")
            .resolve("Widgets");
    Files.createDirectories(widgets);
    Files.writeString(
        widgets.resolve("PSWidget_TestProperties.xml"), "<Widget/>", StandardCharsets.UTF_8);

    PSProductPackageRootSelectionEvidence.Report report =
        PSProductPackageRootSelectionEvidence.scanWidgetPackageRoots(packages);
    assertFalse(report.isClean());
    assertEquals(1, report.roots().size());
    assertEquals("perc.Test", report.roots().get(0).packageDirName());
    assertFalse(report.roots().get(0).waived());
    assertTrue(report.roots().get(0).wouldUseLegacyShim());
    assertEquals(0, report.waivedLegacyRootCount());
    assertThrows(
        IllegalStateException.class,
        () ->
            PSProductPackageRootSelectionEvidence.assertNoUnexpectedLegacyOnWidgetPackageRoots(
                packages));
  }

  @Test
  void tempTree_modernWinsWhenInstallXmlAlsoPresent() throws Exception {
    Path packages = tempDir.resolve("Packages");
    Path pkg = packages.resolve("perc.baseWidgets");
    Path modern = pkg.resolve("widgets").resolve("percSimpleText");
    Files.createDirectories(modern);
    Files.writeString(
        modern.resolve(PSLegacyDefinitionXmlShim.MODERN_MANIFEST_FILE_NAME),
        "{\"schemaVersion\":\"1.0\",\"id\":\"percSimpleText\"}",
        StandardCharsets.UTF_8);
    Path xmlDir = pkg.resolve("sys__UserDependency--rxconfig").resolve("Widgets");
    Files.createDirectories(xmlDir);
    Files.writeString(xmlDir.resolve("percSimpleText.xml"), "<Widget/>", StandardCharsets.UTF_8);

    PSProductPackageRootSelectionEvidence.Report report =
        PSProductPackageRootSelectionEvidence.scanWidgetPackageRoots(packages);
    assertTrue(report.isClean());
    assertEquals(1, report.modernRootCount());
    assertEquals(
        PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE, report.roots().get(0).kind());
    assertFalse(report.roots().get(0).wouldUseLegacyShim());
  }

  @Test
  void tempTree_nonWidgetPackage_isSkipped() throws Exception {
    Path packages = tempDir.resolve("Packages");
    Files.createDirectories(packages.resolve("perc.workflow"));
    PSProductPackageRootSelectionEvidence.Report report =
        PSProductPackageRootSelectionEvidence.scanWidgetPackageRoots(packages);
    assertTrue(report.roots().isEmpty());
    assertTrue(report.isClean());
  }

  @Test
  void scanDefinitionSelection_waivedIdMayBeLegacy() {
    Path widgets = tempDir.resolve("Widgets");
    try {
      Files.createDirectories(widgets);
      Files.writeString(
          widgets.resolve("PSWidget_TestProperties.xml"), "<Widget/>", StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
    PSProductPackageRootSelectionEvidence.Report report =
        PSProductPackageRootSelectionEvidence.scanDefinitionSelection(
            List.of(),
            widgets,
            List.of("PSWidget_TestProperties"),
            Set.of("PSWidget_TestProperties"));
    assertTrue(report.isClean());
    assertEquals(PSDefinitionSourceKind.LEGACY_WIDGET_XML, report.definitions().get(0).kind());
    assertFalse(report.definitions().get(0).isUnexpectedLegacy());
  }

  @Test
  void scan_rejectsNonDirectoryRoot() {
    Path missing = tempDir.resolve("does-not-exist");
    assertThrows(
        IllegalArgumentException.class,
        () -> PSProductPackageRootSelectionEvidence.scanWidgetPackageRoots(missing));
  }

  /**
   * Prefer module-cwd relative layout used by standalone {@code mvnw clean install} from {@code
   * modules/perc-packages}.
   */
  private static Path locatePackagesRoot() {
    Path candidate = Path.of("src", "main", "resources", "Packages");
    if (Files.isDirectory(candidate)) {
      return candidate.toAbsolutePath().normalize();
    }
    Path cwd = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
    Path alt = cwd.resolve("src").resolve("main").resolve("resources").resolve("Packages");
    return Files.isDirectory(alt) ? alt : null;
  }
}
