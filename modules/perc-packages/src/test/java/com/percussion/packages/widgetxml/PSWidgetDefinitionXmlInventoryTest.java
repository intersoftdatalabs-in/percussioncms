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

package com.percussion.packages.widgetxml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * G4 CI/inventory gate: zero product Widget definition XML under Packages (issue #3026 / #3736 /
 * parent #2630). Waiver set is empty after perc.Test ship-exit.
 *
 * <p>Cross-platform: all path construction uses {@link Path#resolve(String)} / {@link Files}.
 */
class PSWidgetDefinitionXmlInventoryTest {

  @TempDir Path tempDir;

  @Test
  void waivedPackageSet_isEmptyAfterPercTestShipExit() {
    assertEquals(Set.of(), PSWidgetDefinitionXmlInventory.WAIVED_PACKAGE_DIRS);
    assertFalse(PSWidgetDefinitionXmlInventory.isWaivedPackage("perc.Test"));
    assertFalse(PSWidgetDefinitionXmlInventory.isWaivedPackage("perc.baseWidgets"));
    assertFalse(PSWidgetDefinitionXmlInventory.isWaivedPackage("perc.widget.form"));
    assertFalse(PSWidgetDefinitionXmlInventory.isWaivedPackage(null));
    assertFalse(PSWidgetDefinitionXmlInventory.isWaivedPackage(""));
  }

  @Test
  void productPackagesTree_hasZeroNonWaivedWidgetDefinitionXml() throws Exception {
    Path packagesRoot = locatePackagesRoot();
    assertNotNull(
        packagesRoot,
        "Packages root must be visible when running module Surefire from perc-packages "
            + "(src/main/resources/Packages)");

    PSWidgetDefinitionXmlInventory.Report report =
        PSWidgetDefinitionXmlInventory.scan(packagesRoot);
    assertTrue(
        report.isClean(),
        () ->
            "G4: Widget def XML must not reappear under product Packages; found: "
                + report.nonWaived());
    assertEquals(0, report.all().size(), "expected zero Widget def XML including perc.Test");
    assertEquals(0, report.waived().size());
    assertEquals(0, report.nonWaived().size());

    // Fail-fast helper used by optional CLI must also pass on the live tree.
    PSWidgetDefinitionXmlInventory.assertNoNonWaivedWidgetDefinitionXml(packagesRoot);
  }

  @Test
  void tempTree_modernOnlyPackages_isClean() throws Exception {
    Path packages = tempDir.resolve("Packages");
    Files.createDirectories(packages.resolve("perc.Test").resolve("widgets"));
    Files.createDirectories(packages.resolve("perc.baseWidgets").resolve("widgets"));

    PSWidgetDefinitionXmlInventory.Report report =
        PSWidgetDefinitionXmlInventory.scan(packages);
    assertEquals(0, report.all().size());
    assertEquals(0, report.nonWaived().size());
    assertEquals(0, report.waived().size());
    assertTrue(report.isClean());
    PSWidgetDefinitionXmlInventory.assertNoNonWaivedWidgetDefinitionXml(packages);
  }

  @Test
  void tempTree_percTestXml_failsGateAfterWaiverDrop() throws Exception {
    Path packages = tempDir.resolve("Packages");
    Path widgets =
        packages
            .resolve("perc.Test")
            .resolve("sys__UserDependency--rxconfig")
            .resolve("Widgets");
    Files.createDirectories(widgets);
    Path xmlFile = widgets.resolve("PSWidget_TestProperties.xml");
    Files.writeString(
        xmlFile, "<Widget id=\"PSWidget_TestProperties\"/>", StandardCharsets.UTF_8);

    PSWidgetDefinitionXmlInventory.Report report =
        PSWidgetDefinitionXmlInventory.scan(packages);
    assertFalse(report.isClean());
    assertEquals(1, report.nonWaived().size());
    assertEquals("perc.Test", report.nonWaived().get(0).packageDirName());
    assertEquals(xmlFile.toAbsolutePath().normalize(), report.nonWaived().get(0).xmlPath());
    assertThrows(
        IllegalStateException.class,
        () -> PSWidgetDefinitionXmlInventory.assertNoNonWaivedWidgetDefinitionXml(packages));
  }

  /**
   * Finding paths must be anchored under the scanned Widgets dir even when {@code packagesRoot} is
   * a relative path (not pre-absolutized). Regression for review: bare {@code
   * xml.toAbsolutePath()} must not leave paths only relative to CWD / outside widgetsDir.
   */
  @Test
  void findingXmlPath_relativePackagesRoot_isAbsoluteUnderWidgetsDir() throws Exception {
    Path packages = tempDir.resolve("Packages");
    Path widgets =
        packages
            .resolve("perc.Test")
            .resolve("sys__UserDependency--rxconfig")
            .resolve("Widgets");
    Files.createDirectories(widgets);
    Path xmlFile = widgets.resolve("PSWidget_TestProperties.xml");
    Files.writeString(xmlFile, "<Widget/>", StandardCharsets.UTF_8);

    // Relative packages root (same tree as absolute tempDir path, different Path form).
    Path relativePackages =
        Path.of(".").toAbsolutePath().normalize().relativize(packages.toAbsolutePath().normalize());
    assertFalse(relativePackages.isAbsolute(), "precondition: packages root is relative");

    PSWidgetDefinitionXmlInventory.Report report =
        PSWidgetDefinitionXmlInventory.scan(relativePackages);
    assertEquals(1, report.all().size());
    Path found = report.all().get(0).xmlPath();
    assertTrue(found.isAbsolute());
    assertEquals(xmlFile.toAbsolutePath().normalize(), found);
    assertTrue(found.startsWith(widgets.toAbsolutePath().normalize()));
    assertTrue(Files.isRegularFile(found));
  }

  @Test
  void tempTree_dummyNonWaivedXml_failsGate() throws Exception {
    Path packages = tempDir.resolve("Packages");
    Path widgets =
        packages
            .resolve("perc.baseWidgets")
            .resolve("sys__UserDependency--rxconfig")
            .resolve("Widgets");
    Files.createDirectories(widgets);
    Path dummy = widgets.resolve("percSimpleText.xml");
    Files.writeString(dummy, "<Widget id=\"percSimpleText\"/>", StandardCharsets.UTF_8);

    PSWidgetDefinitionXmlInventory.Report report =
        PSWidgetDefinitionXmlInventory.scan(packages);
    assertFalse(report.isClean());
    assertEquals(1, report.nonWaived().size());
    assertEquals("perc.baseWidgets", report.nonWaived().get(0).packageDirName());
    assertTrue(
        report.nonWaived().get(0).xmlPath().getFileName().toString().equals("percSimpleText.xml"));

    IllegalStateException err =
        assertThrows(
            IllegalStateException.class,
            () -> PSWidgetDefinitionXmlInventory.assertNoNonWaivedWidgetDefinitionXml(packages));
    assertTrue(err.getMessage().contains("G4"));
    assertTrue(err.getMessage().contains("perc.baseWidgets"));
    assertTrue(err.getMessage().contains("percSimpleText.xml"));
  }

  @Test
  void tempTree_percTestAndFormXml_bothFailAsNonWaived() throws Exception {
    Path packages = tempDir.resolve("Packages");

    Path testWidgets =
        packages
            .resolve("perc.Test")
            .resolve("sys__UserDependency--rxconfig")
            .resolve("Widgets");
    Files.createDirectories(testWidgets);
    Files.writeString(
        testWidgets.resolve("PSWidget_TestProperties.xml"),
        "<Widget/>",
        StandardCharsets.UTF_8);

    Path badWidgets =
        packages
            .resolve("perc.widget.form")
            .resolve("sys__UserDependency--rxconfig")
            .resolve("Widgets");
    Files.createDirectories(badWidgets);
    Files.writeString(badWidgets.resolve("percForm.xml"), "<Widget/>", StandardCharsets.UTF_8);

    PSWidgetDefinitionXmlInventory.Report report =
        PSWidgetDefinitionXmlInventory.scan(packages);
    assertEquals(2, report.all().size());
    assertEquals(0, report.waived().size());
    assertEquals(2, report.nonWaived().size());
    assertFalse(report.isClean());
  }

  @Test
  void resolveWidgetsDir_usesPortableSegments() {
    Path pkg = Path.of("pkgRoot");
    Path widgets = PSWidgetDefinitionXmlInventory.resolveWidgetsDir(pkg);
    assertEquals(
        pkg.resolve("sys__UserDependency--rxconfig").resolve("Widgets"), widgets);
  }

  @Test
  void scan_rejectsNonDirectoryRoot() {
    Path missing = tempDir.resolve("does-not-exist");
    assertThrows(IllegalArgumentException.class, () -> PSWidgetDefinitionXmlInventory.scan(missing));
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
    Path alt =
        cwd.resolve("src").resolve("main").resolve("resources").resolve("Packages");
    return Files.isDirectory(alt) ? alt : null;
  }
}
