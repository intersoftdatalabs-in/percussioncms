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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Dual-ship page templateDef CI/inventory gate: zero dual-ship emitters under Packages (issue
 * #3675 / #3737 / parent #2630). Waiver set is empty after perc.Test page dual-ship exit ({@code
 * perc.Test} never authored {@code pages/}). #3674 leftover binaries were converted, not
 * dual-ship-retained.
 *
 * <p>Cross-platform: all path construction uses {@link Path#resolve(String)} / {@link Files}.
 */
class PSDualShipPageTemplateDefInventoryTest {

  @TempDir Path tempDir;

  @AfterEach
  void clearInstallModeSystemProperties() {
    System.clearProperty(PSPageXmlInstallPolicy.SYS_PROP_INSTALL_MODE);
    System.clearProperty(PSPageXmlInstallPolicy.SYS_PROP_DUAL_SHIP);
  }

  @Test
  void waivedPackageSet_isEmptyAfterPercTestPageShipExit() {
    assertEquals(Set.of(), PSDualShipPageTemplateDefInventory.RETAINED_WIDGET_BINARY_PACKAGE_DIRS);
    assertEquals(Set.of(), PSDualShipPageTemplateDefInventory.WAIVED_PACKAGE_DIRS);
    assertFalse(PSDualShipPageTemplateDefInventory.isWaivedPackage("perc.Test"));
    assertFalse(PSDualShipPageTemplateDefInventory.isWaivedPackage("perc.baseTemplates"));
    assertFalse(PSDualShipPageTemplateDefInventory.isWaivedPackage("perc.FileAssetWidget"));
    assertFalse(PSDualShipPageTemplateDefInventory.isWaivedPackage("perc.widgets.image"));
    assertFalse(PSDualShipPageTemplateDefInventory.isWaivedPackage(null));
    assertFalse(PSDualShipPageTemplateDefInventory.isWaivedPackage(""));
  }

  @Test
  void productPackagesTree_hasZeroNonWaivedDualShipPageTemplateDefs() throws Exception {
    Path packagesRoot = locatePackagesRoot();
    assertNotNull(
        packagesRoot,
        "Packages root must be visible when running module Surefire from perc-packages "
            + "(src/main/resources/Packages)");

    PSDualShipPageTemplateDefInventory.Report report =
        PSDualShipPageTemplateDefInventory.scan(packagesRoot);
    assertTrue(
        report.isClean(),
        () ->
            "dual-ship page templateDefs must not reappear; found: " + report.nonWaived());
    assertEquals(0, report.all().size(), "expected zero dual-ship emitters including perc.Test");
    assertEquals(0, report.waived().size());
    assertEquals(0, report.nonWaived().size());

    PSDualShipPageTemplateDefInventory.assertNoNonWaivedDualShipPageTemplateDefs(packagesRoot);
  }

  @Test
  void productPercTest_hasNoModernPagesAndIsNotWaived() throws Exception {
    Path packagesRoot = locatePackagesRoot();
    assertNotNull(packagesRoot);
    Path percTest = packagesRoot.resolve("perc.Test");
    assertTrue(Files.isDirectory(percTest), "missing product package perc.Test");
    assertFalse(
        PSPageXmlDualShip.hasModernPageSources(percTest),
        "perc.Test must not author pages/; do not reintroduce dual-ship page templateDefs");
    assertFalse(PSDualShipPageTemplateDefInventory.isWaivedPackage("perc.Test"));
  }

  @Test
  void productNativePagePackages_areNotDualShipEmitters() throws Exception {
    Path packagesRoot = locatePackagesRoot();
    assertNotNull(packagesRoot);
    for (String pkg : List.of("perc.baseTemplates", "perc.responsiveTemplates", "perc.Baseline")) {
      Path dir = packagesRoot.resolve(pkg);
      assertTrue(Files.isDirectory(dir), () -> "missing product package " + pkg);
      assertTrue(PSPageXmlDualShip.hasModernPageSources(dir), pkg + " must have modern pages/");
      assertEquals(
          PSPageXmlInstallMode.NATIVE,
          PSDualShipPageTemplateDefInventory.resolveCommittedInstallMode(dir),
          pkg + " must commit page.installMode=native");
    }
  }

  @Test
  void leftoverAuthoredRootTemplateDefWithoutModernPages_isNotDualShip() throws Exception {
    Path packages = tempDir.resolve("Packages");
    Path fileWidget = packages.resolve("perc.FileAssetWidget");
    Files.createDirectories(fileWidget);
    Files.writeString(
        fileWidget.resolve("perc.fileBinary.templateDef"),
        "<AssemblyTemplate/>",
        StandardCharsets.UTF_8);
    Path image = packages.resolve("perc.widgets.image");
    Files.createDirectories(image);
    Files.writeString(
        image.resolve("perc.imageMainBinary.templateDef"),
        "<AssemblyTemplate/>",
        StandardCharsets.UTF_8);

    PSDualShipPageTemplateDefInventory.Report report =
        PSDualShipPageTemplateDefInventory.scan(packages);
    assertTrue(report.isClean());
    assertEquals(0, report.all().size());
    PSDualShipPageTemplateDefInventory.assertNoNonWaivedDualShipPageTemplateDefs(packages);
  }

  @Test
  void tempTree_nativeModernPages_isClean() throws Exception {
    Path packages = tempDir.resolve("Packages");
    Path pkg = packages.resolve("perc.baseTemplates");
    writeModernPage(pkg, "perc.base.plain");
    Files.writeString(
        pkg.resolve(PSPageXmlInstallPolicy.PACKAGE_INSTALL_PROPS),
        PSPageXmlInstallPolicy.PROP_PAGE_INSTALL_MODE + "=native\n",
        StandardCharsets.UTF_8);

    PSDualShipPageTemplateDefInventory.Report report =
        PSDualShipPageTemplateDefInventory.scan(packages);
    assertTrue(report.isClean());
    assertEquals(0, report.all().size());
    PSDualShipPageTemplateDefInventory.assertNoNonWaivedDualShipPageTemplateDefs(packages);
  }

  @Test
  void tempTree_percTestDualShip_failsGateAfterWaiverDrop() throws Exception {
    Path packages = tempDir.resolve("Packages");
    Path pkg = packages.resolve("perc.Test");
    writeModernPage(pkg, "PSPage_TestProperties");

    PSDualShipPageTemplateDefInventory.Report report =
        PSDualShipPageTemplateDefInventory.scan(packages);
    assertFalse(report.isClean());
    assertEquals(1, report.all().size());
    assertEquals(1, report.nonWaived().size());
    assertEquals(0, report.waived().size());
    assertEquals("perc.Test", report.nonWaived().get(0).packageDirName());
    assertEquals(PSPageXmlInstallMode.DUAL_SHIP, report.nonWaived().get(0).committedMode());
    assertEquals(1, report.nonWaived().get(0).modernPageCount());
    Path expectedAbs = pkg.toAbsolutePath().normalize();
    assertEquals(expectedAbs, report.nonWaived().get(0).packageDir());
    assertTrue(report.nonWaived().get(0).packageDir().isAbsolute());
    assertThrows(
        IllegalStateException.class,
        () ->
            PSDualShipPageTemplateDefInventory.assertNoNonWaivedDualShipPageTemplateDefs(
                packages));
  }

  @Test
  void tempTree_dummyNonWaivedDualShip_failsGate() throws Exception {
    Path packages = tempDir.resolve("Packages");
    Path pkg = packages.resolve("perc.baseTemplates");
    writeModernPage(pkg, "perc.base.plain");

    PSDualShipPageTemplateDefInventory.Report report =
        PSDualShipPageTemplateDefInventory.scan(packages);
    assertFalse(report.isClean());
    assertEquals(1, report.nonWaived().size());
    assertEquals("perc.baseTemplates", report.nonWaived().get(0).packageDirName());
    assertEquals(PSPageXmlInstallMode.DUAL_SHIP, report.nonWaived().get(0).committedMode());
    assertEquals(1, report.nonWaived().get(0).modernPageCount());
    assertTrue(report.nonWaived().get(0).packageDir().isAbsolute());
    assertEquals(pkg.toAbsolutePath().normalize(), report.nonWaived().get(0).packageDir());

    IllegalStateException err =
        assertThrows(
            IllegalStateException.class,
            () ->
                PSDualShipPageTemplateDefInventory.assertNoNonWaivedDualShipPageTemplateDefs(
                    packages));
    assertTrue(err.getMessage().contains("#3675"));
    assertTrue(err.getMessage().contains("perc.baseTemplates"));
  }

  @Test
  void tempTree_explicitDualShipProperty_failsGate() throws Exception {
    Path packages = tempDir.resolve("Packages");
    Path pkg = packages.resolve("perc.responsiveTemplates");
    writeModernPage(pkg, "perc.resp.plain");
    Files.writeString(
        pkg.resolve(PSPageXmlInstallPolicy.PACKAGE_INSTALL_PROPS),
        PSPageXmlInstallPolicy.PROP_PAGE_INSTALL_MODE + "=dual-ship\n",
        StandardCharsets.UTF_8);

    PSDualShipPageTemplateDefInventory.Report report =
        PSDualShipPageTemplateDefInventory.scan(packages);
    assertFalse(report.isClean());
    assertEquals("perc.responsiveTemplates", report.nonWaived().get(0).packageDirName());
  }

  @Test
  void committedScan_ignoresJvmNativeOverride() throws Exception {
    Path packages = tempDir.resolve("Packages");
    Path pkg = packages.resolve("perc.baseTemplates");
    writeModernPage(pkg, "perc.base.plain");
    System.setProperty(PSPageXmlInstallPolicy.SYS_PROP_INSTALL_MODE, "native");
    System.setProperty(PSPageXmlInstallPolicy.SYS_PROP_DUAL_SHIP, "false");

    assertEquals(PSPageXmlInstallMode.NATIVE, PSPageXmlInstallPolicy.resolve(pkg));
    assertEquals(
        PSPageXmlInstallMode.DUAL_SHIP,
        PSDualShipPageTemplateDefInventory.resolveCommittedInstallMode(pkg));

    PSDualShipPageTemplateDefInventory.Report report =
        PSDualShipPageTemplateDefInventory.scan(packages);
    assertFalse(report.isClean(), "JVM native override must not hide missing package-local opt-in");
    assertEquals(1, report.nonWaived().size());
  }

  @Test
  void findingPackageDir_relativePackagesRoot_isAbsolute() throws Exception {
    Path packages = tempDir.resolve("Packages");
    Path pkg = packages.resolve("perc.Test");
    writeModernPage(pkg, "PSPage_TestProperties");

    Path relativePackages =
        Path.of(".").toAbsolutePath().normalize().relativize(packages.toAbsolutePath().normalize());
    assertFalse(relativePackages.isAbsolute(), "precondition: packages root is relative");

    PSDualShipPageTemplateDefInventory.Report report =
        PSDualShipPageTemplateDefInventory.scan(relativePackages);
    assertEquals(1, report.all().size());
    Path found = report.all().get(0).packageDir();
    assertTrue(found.isAbsolute());
    assertEquals(pkg.toAbsolutePath().normalize(), found);
    assertTrue(Files.isDirectory(found));
  }

  @Test
  void tempTree_percTestAndBaseTemplates_bothFailAsNonWaived() throws Exception {
    Path packages = tempDir.resolve("Packages");
    writeModernPage(packages.resolve("perc.Test"), "PSPage_TestProperties");
    writeModernPage(packages.resolve("perc.baseTemplates"), "perc.base.plain");

    PSDualShipPageTemplateDefInventory.Report report =
        PSDualShipPageTemplateDefInventory.scan(packages);
    assertEquals(2, report.all().size());
    assertEquals(0, report.waived().size());
    assertEquals(2, report.nonWaived().size());
    assertFalse(report.isClean());
  }

  @Test
  void logLine_formatAndParse_roundTrip() {
    String line = PSDualShipPageTemplateDefInventory.formatDualShipLogLine("perc.baseTemplates", 20);
    assertTrue(line.contains(PSDualShipPageTemplateDefInventory.DUAL_SHIP_LOG_MARKER));
    assertEquals(
        Optional.of("perc.baseTemplates"),
        PSDualShipPageTemplateDefInventory.parseDualShipPackageFromLogLine(line));
    assertEquals(
        Optional.empty(),
        PSDualShipPageTemplateDefInventory.parseDualShipPackageFromLogLine(
            "  native-install page TemplateDefs for perc.baseTemplates: 20 written"));
    assertEquals(
        Optional.empty(), PSDualShipPageTemplateDefInventory.parseDualShipPackageFromLogLine(null));
    assertEquals(
        Optional.empty(), PSDualShipPageTemplateDefInventory.parseDualShipPackageFromLogLine(""));
  }

  @Test
  void scanLogLines_percTestFailsAfterWaiverDrop() {
    String percTest =
        PSDualShipPageTemplateDefInventory.formatDualShipLogLine("perc.Test", 1);
    String nativeLine = "  native-install page TemplateDefs for perc.baseTemplates: 20 written";
    PSDualShipPageTemplateDefInventory.Report percTestOnly =
        PSDualShipPageTemplateDefInventory.scanLogLines(List.of(percTest, nativeLine));
    assertFalse(percTestOnly.isClean());
    assertEquals(1, percTestOnly.nonWaived().size());
    assertEquals("perc.Test", percTestOnly.nonWaived().get(0).packageDirName());
    assertNull(percTestOnly.nonWaived().get(0).packageDir());
    assertThrows(
        IllegalStateException.class,
        () ->
            PSDualShipPageTemplateDefInventory.assertNoNonWaivedDualShipLogLines(
                List.of(percTest)));

    String bad = PSDualShipPageTemplateDefInventory.formatDualShipLogLine("perc.baseTemplates", 20);
    PSDualShipPageTemplateDefInventory.Report fail =
        PSDualShipPageTemplateDefInventory.scanLogLines(List.of(nativeLine, bad, percTest));
    assertFalse(fail.isClean());
    assertEquals(2, fail.nonWaived().size());
    IllegalStateException err =
        assertThrows(
            IllegalStateException.class,
            () ->
                PSDualShipPageTemplateDefInventory.assertNoNonWaivedDualShipLogLines(
                    List.of(bad)));
    assertTrue(err.getMessage().contains("perc.baseTemplates"));
  }

  @Test
  void scanLogFile_readsUtf8Lines() throws Exception {
    Path log = tempDir.resolve("package-build.log");
    String content =
        PSDualShipPageTemplateDefInventory.formatDualShipLogLine("perc.baseTemplates", 3)
            + "\r\n"
            + "Package build complete: 1 built, 0 up-to-date\n";
    Files.writeString(log, content, StandardCharsets.UTF_8);

    PSDualShipPageTemplateDefInventory.Report report =
        PSDualShipPageTemplateDefInventory.scanLogFile(log);
    assertFalse(report.isClean());
    assertEquals("perc.baseTemplates", report.nonWaived().get(0).packageDirName());
  }

  @Test
  void assertDualShipMaterializationAllowed_rejectsAllPackagesIncludingPercTest() {
    IllegalStateException percTest =
        assertThrows(
            IllegalStateException.class,
            () ->
                PSDualShipPageTemplateDefInventory.assertDualShipMaterializationAllowed(
                    "perc.Test"));
    assertTrue(percTest.getMessage().contains("#3675"));
    assertTrue(percTest.getMessage().contains("perc.Test"));
    IllegalStateException err =
        assertThrows(
            IllegalStateException.class,
            () ->
                PSDualShipPageTemplateDefInventory.assertDualShipMaterializationAllowed(
                    "perc.baseTemplates"));
    assertTrue(err.getMessage().contains("#3675"));
    assertTrue(err.getMessage().contains("perc.baseTemplates"));
    assertTrue(err.getMessage().contains("native"));
  }

  @Test
  void scan_rejectsNonDirectoryRoot() {
    Path missing = tempDir.resolve("does-not-exist");
    assertThrows(
        IllegalArgumentException.class, () -> PSDualShipPageTemplateDefInventory.scan(missing));
  }

  @Test
  void scanLogFile_rejectsMissingFile() {
    Path missing = tempDir.resolve("missing.log");
    assertThrows(
        IllegalArgumentException.class,
        () -> PSDualShipPageTemplateDefInventory.scanLogFile(missing));
  }

  private static void writeModernPage(Path packageDir, String templateId) throws Exception {
    Path page = packageDir.resolve(PSPageXmlDualShip.PAGES_DIR_NAME).resolve(templateId);
    Files.createDirectories(page);
    Files.writeString(
        page.resolve("component-package.json"),
        "{\"id\":\"" + templateId + "\"}",
        StandardCharsets.UTF_8);
  }

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
