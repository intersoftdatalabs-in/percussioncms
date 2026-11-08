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
 * M2 product / H2 zero-legacy-selection evidence harness (#3583 / #3738 / parent #2630).
 *
 * <p>Asserts product package roots and H2-style {@code Packages/Modern} trees select modern-first
 * ({@code wouldUseLegacyShim == false} / {@link PSDefinitionSourceKind#MODERN_COMPONENT_PACKAGE}).
 * Fails on unexpected {@code LEGACY_*} for non-waived widgets. {@code perc.Test} must be
 * modern-first now that the Widget G4 waive list is empty (#3736). Customer-only ids without a
 * modern manifest resolve to a {@code null} kind (unresolvable) — the legacy fallback is retired
 * (#2852), so retired customer XML is never selected.
 *
 * <p>Cross-platform: {@link Path} / {@link Files} only.
 */
class PSProductPackageRootSelectionEvidenceTest {

  @TempDir Path tempDir;

  @Test
  void widgetWaiverPolicy_isEmptyOrPercTestOnly() {
    PSProductPackageRootSelectionEvidence.assertWidgetWaiverPolicy();
    Set<String> waived = PSWidgetDefinitionXmlInventory.WAIVED_PACKAGE_DIRS;
    assertTrue(
        waived.isEmpty() || waived.equals(Set.of(PSProductPackageRootSelectionEvidence.PERC_TEST_PACKAGE_DIR)),
        () -> "Widget waive list must be empty or perc.Test only; found: " + waived);
    if (PSProductPackageRootSelectionEvidence.isPercTestWidgetWaiverDropped()) {
      assertTrue(PSProductPackageRootSelectionEvidence.currentWaivedDefinitionIds().isEmpty());
    } else {
      assertEquals(
          Set.of(PSProductPackageRootSelectionEvidence.PERC_TEST_WIDGET_STEM),
          PSProductPackageRootSelectionEvidence.currentWaivedDefinitionIds());
    }
  }

  @Test
  void knownProductWidgetStems_coverBatchesABC_andPercTestMatchesWaiver() {
    List<String> stems = PSProductPackageRootSelectionEvidence.KNOWN_PRODUCT_WIDGET_STEMS;
    assertTrue(stems.size() >= 47, "8 + 20 + 19 product widget stems (floor)");
    assertTrue(stems.containsAll(PSWidgetXmlDualShip.BATCH_A_WIDGET_STEMS));
    assertTrue(stems.containsAll(PSWidgetXmlDualShip.BATCH_B_WIDGET_STEMS));
    assertTrue(stems.containsAll(PSWidgetXmlDualShip.BATCH_C_WIDGET_STEMS));
    PSProductPackageRootSelectionEvidence.assertWidgetWaiverPolicy();
    if (PSProductPackageRootSelectionEvidence.isPercTestWidgetWaiverDropped()) {
      assertTrue(
          stems.contains(PSProductPackageRootSelectionEvidence.PERC_TEST_WIDGET_STEM),
          "after #3736 perc.Test stem must be a known modern product widget");
    } else {
      assertEquals(47, stems.size(), "8 + 20 + 19 product widget stems while perc.Test is waived");
      assertFalse(stems.contains(PSProductPackageRootSelectionEvidence.PERC_TEST_WIDGET_STEM));
      assertEquals(
          Set.of(PSProductPackageRootSelectionEvidence.PERC_TEST_PACKAGE_DIR),
          PSWidgetDefinitionXmlInventory.WAIVED_PACKAGE_DIRS);
    }
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
    boolean percTestWaiverDropped =
        PSProductPackageRootSelectionEvidence.isPercTestWidgetWaiverDropped();
    long minModern = percTestWaiverDropped ? 39L : 38L;
    assertTrue(
        report.modernRootCount() >= minModern,
        "expected at least batch A+B+C"
            + (percTestWaiverDropped ? "+perc.Test" : "")
            + " package dirs to select MODERN; got "
            + report.modernRootCount()
            + " roots="
            + report.roots());
    PSProductPackageRootSelectionEvidence.assertPercTestSelectionMatchesWaiver(report, true);

    Set<String> modernPackages = new HashSet<>();
    for (PSProductPackageRootSelectionEvidence.RootFinding f : report.roots()) {
      if (f.kind() == PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE) {
        assertFalse(PSLegacyDefinitionXmlShim.wouldUseLegacyShim(f.packageRoot()), f.packageDirName());
        modernPackages.add(f.packageDirName());
      }
      if (f.waived()) {
        assertEquals(PSProductPackageRootSelectionEvidence.PERC_TEST_PACKAGE_DIR, f.packageDirName());
        assertFalse(percTestWaiverDropped, f.packageDirName());
      }
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
    if (percTestWaiverDropped) {
      assertTrue(
          modernPackages.contains(PSProductPackageRootSelectionEvidence.PERC_TEST_PACKAGE_DIR),
          "perc.Test missing modern-first after waiver drop");
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
    boolean percTestWaiverDropped =
        PSProductPackageRootSelectionEvidence.isPercTestWidgetWaiverDropped();
    long minModern = percTestWaiverDropped ? 39L : 38L;
    assertTrue(
        report.modernRootCount() >= minModern, "H2 modern root count=" + report.modernRootCount());
    for (PSProductPackageRootSelectionEvidence.RootFinding f : report.roots()) {
      assertEquals(PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE, f.kind(), f.packageDirName());
      assertFalse(f.wouldUseLegacyShim(), f.packageDirName());
    }
    PSProductPackageRootSelectionEvidence.assertPercTestSelectionMatchesWaiver(report, false);
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
    // Customer-only install XML without a modern manifest is unresolvable under modern-only
    // selection (#2852): null kind, never a legacy selection.
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
    assertEquals(null, defs.definitions().get(0).kind());
    assertFalse(defs.definitions().get(0).isUnexpectedLegacy());
    assertTrue(defs.isClean());
    // Customer-only is not a product widget — the harness records it as unresolvable (null kind),
    // not as unexpected legacy. Product scan above excluded it.
  }

  @Test
  void tempTree_nonWaivedLegacyOnly_resolvesToNullKindModernOnly() throws Exception {
    Path packages = tempDir.resolve("Packages");
    Path widgets =
        packages
            .resolve("perc.baseWidgets")
            .resolve("sys__UserDependency--rxconfig")
            .resolve("Widgets");
    Files.createDirectories(widgets);
    Files.writeString(widgets.resolve("percSimpleText.xml"), "<Widget/>", StandardCharsets.UTF_8);

    // Modern-only selection (#2852) cannot resolve a legacy-only root: null kind, never legacy.
    // Ship-path reappearance of product definition XML remains guarded by the G4 file-presence
    // inventory (PSWidgetDefinitionXmlInventory), not by selection kind.
    PSProductPackageRootSelectionEvidence.Report report =
        PSProductPackageRootSelectionEvidence.scanWidgetPackageRoots(packages);
    assertEquals(1, report.roots().size());
    assertEquals(null, report.roots().get(0).kind());
    assertFalse(report.roots().get(0).wouldUseLegacyShim());
    assertTrue(report.isClean());
    assertEquals(0, report.unexpectedLegacyRoots().size());
    assertEquals("perc.baseWidgets", report.roots().get(0).packageDirName());

    // No LEGACY_* is selectable anymore, so the selection gate passes; the failure surface for
    // retired XML is the modern-only not-found error, asserted at the shim level.
    PSProductPackageRootSelectionEvidence.assertNoUnexpectedLegacyOnWidgetPackageRoots(packages);
  }

  @Test
  void tempTree_percTestLegacyOnly_matchesCurrentWaiver() throws Exception {
    Path packages = tempDir.resolve("Packages");
    Path widgets =
        packages
            .resolve(PSProductPackageRootSelectionEvidence.PERC_TEST_PACKAGE_DIR)
            .resolve("sys__UserDependency--rxconfig")
            .resolve("Widgets");
    Files.createDirectories(widgets);
    Files.writeString(
        widgets.resolve(PSProductPackageRootSelectionEvidence.PERC_TEST_WIDGET_STEM + ".xml"),
        "<Widget/>",
        StandardCharsets.UTF_8);

    PSProductPackageRootSelectionEvidence.Report report =
        PSProductPackageRootSelectionEvidence.scanWidgetPackageRoots(packages);
    assertEquals(1, report.roots().size());
    assertEquals(
        PSProductPackageRootSelectionEvidence.PERC_TEST_PACKAGE_DIR,
        report.roots().get(0).packageDirName());
    // Modern-only (#2852): a legacy-only root resolves to null kind, never legacy.
    assertEquals(null, report.roots().get(0).kind());
    assertFalse(report.roots().get(0).wouldUseLegacyShim());
    assertEquals(0, report.waivedLegacyRootCount());
    assertTrue(report.isClean());
    if (PSProductPackageRootSelectionEvidence.isPercTestWidgetWaiverDropped()) {
      assertFalse(report.roots().get(0).waived());
      PSProductPackageRootSelectionEvidence.assertNoUnexpectedLegacyOnWidgetPackageRoots(packages);
      // perc.Test legacy-only root is not modern-first, so the waiver-match assertion still
      // requires the modern root (covered by tempTree_percTestModernRoot_matchesCurrentWaiver).
      assertThrows(
          IllegalStateException.class,
          () ->
              PSProductPackageRootSelectionEvidence.assertPercTestSelectionMatchesWaiver(
                  report, true));
    } else {
      assertTrue(report.roots().get(0).waived());
      PSProductPackageRootSelectionEvidence.assertNoUnexpectedLegacyOnWidgetPackageRoots(packages);
      PSProductPackageRootSelectionEvidence.assertPercTestSelectionMatchesWaiver(report, true);
    }
  }

  @Test
  void tempTree_percTestModernRoot_matchesCurrentWaiver() throws Exception {
    Path packages = tempDir.resolve("Packages");
    Path modern =
        packages
            .resolve(PSProductPackageRootSelectionEvidence.PERC_TEST_PACKAGE_DIR)
            .resolve("widgets")
            .resolve(PSProductPackageRootSelectionEvidence.PERC_TEST_WIDGET_STEM);
    Files.createDirectories(modern);
    Files.writeString(
        modern.resolve(PSLegacyDefinitionXmlShim.MODERN_MANIFEST_FILE_NAME),
        "{\"schemaVersion\":\"1.0\",\"id\":\"PSWidget_TestProperties\"}",
        StandardCharsets.UTF_8);

    PSProductPackageRootSelectionEvidence.Report report =
        PSProductPackageRootSelectionEvidence.scanWidgetPackageRoots(packages);
    assertTrue(report.isClean());
    assertEquals(1, report.modernRootCount());
    assertEquals(
        PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE, report.roots().get(0).kind());
    assertFalse(report.roots().get(0).wouldUseLegacyShim());
    PSProductPackageRootSelectionEvidence.assertNoUnexpectedLegacyOnWidgetPackageRoots(packages);
    PSProductPackageRootSelectionEvidence.assertPercTestSelectionMatchesWaiver(report, true);
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
  void assertWidgetWaiverPolicy_rejectsUnknownPackage() {
    PSProductPackageRootSelectionEvidence.assertWidgetWaiverPolicy();
    PSProductPackageRootSelectionEvidence.assertWidgetWaiverPolicy(Set.of());
    PSProductPackageRootSelectionEvidence.assertWidgetWaiverPolicy(
        Set.of(PSProductPackageRootSelectionEvidence.PERC_TEST_PACKAGE_DIR));
    PSProductPackageRootSelectionEvidence.assertWidgetWaiverPolicy(null);
    IllegalStateException extra =
        assertThrows(
            IllegalStateException.class,
            () ->
                PSProductPackageRootSelectionEvidence.assertWidgetWaiverPolicy(
                    Set.of("perc.baseWidgets")));
    assertTrue(extra.getMessage().contains("perc.baseWidgets"));
    IllegalStateException mixed =
        assertThrows(
            IllegalStateException.class,
            () ->
                PSProductPackageRootSelectionEvidence.assertWidgetWaiverPolicy(
                    Set.of(
                        PSProductPackageRootSelectionEvidence.PERC_TEST_PACKAGE_DIR,
                        "perc.baseWidgets")));
    assertTrue(mixed.getMessage().contains("#3738"));
  }

  @Test
  void scanDefinitionSelection_waivedLegacyOnlyIdResolvesNullKindButStaysClean() {
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
    // Modern-only (#2852): even a waived id without a modern manifest is unresolvable (null
    // kind) — retired legacy XML is never selected.
    assertEquals(null, report.definitions().get(0).kind());
    assertFalse(report.definitions().get(0).isUnexpectedLegacy());
  }

  @Test
  void assertPercTestSelectionMatchesWaiver_failsWhenRequiredRootMissing() throws Exception {
    Path packages = tempDir.resolve("Packages");
    Files.createDirectories(packages.resolve("perc.workflow"));
    PSProductPackageRootSelectionEvidence.Report report =
        PSProductPackageRootSelectionEvidence.scanWidgetPackageRoots(packages);
    assertTrue(report.roots().isEmpty());
    IllegalStateException err =
        assertThrows(
            IllegalStateException.class,
            () ->
                PSProductPackageRootSelectionEvidence.assertPercTestSelectionMatchesWaiver(
                    report, true));
    assertTrue(err.getMessage().contains("perc.Test"));
    assertTrue(err.getMessage().contains("#3738"));
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
