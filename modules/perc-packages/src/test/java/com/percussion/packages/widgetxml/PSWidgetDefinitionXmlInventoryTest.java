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
 * G4 CI/inventory gate: zero non-waived product Widget definition XML under Packages (issue #3026 /
 * parent #2630). Explicit waiver: {@code perc.Test} only.
 *
 * <p>Cross-platform: all path construction uses {@link Path#resolve(String)} / {@link Files}.
 */
class PSWidgetDefinitionXmlInventoryTest {

  @TempDir Path tempDir;

  @Test
  void waivedPackageSet_isExplicitlyPercTestOnly() {
    assertEquals(Set.of("perc.Test"), PSWidgetDefinitionXmlInventory.WAIVED_PACKAGE_DIRS);
    assertTrue(PSWidgetDefinitionXmlInventory.isWaivedPackage("perc.Test"));
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
            "G4: non-waived Widget def XML must not reappear under product Packages; found: "
                + report.nonWaived());

    // Waived residual may still ship Widget XML (perc.Test / PSWidget_TestProperties).
    assertFalse(
        report.waived().isEmpty(),
        "expected at least one waived Widget XML under perc.Test on the product tree");
    for (PSWidgetDefinitionXmlInventory.Finding f : report.waived()) {
      assertEquals("perc.Test", f.packageDirName());
      assertTrue(f.waived());
      assertTrue(Files.isRegularFile(f.xmlPath()));
    }

    // Fail-fast helper used by optional CLI must also pass on the live tree.
    PSWidgetDefinitionXmlInventory.assertNoNonWaivedWidgetDefinitionXml(packagesRoot);
  }

  @Test
  void tempTree_onlyWaivedXml_isClean() throws Exception {
    Path packages = tempDir.resolve("Packages");
    Path widgets =
        packages
            .resolve("perc.Test")
            .resolve("sys__UserDependency--rxconfig")
            .resolve("Widgets");
    Files.createDirectories(widgets);
    Path xmlFile =
        widgets.resolve("PSWidget_TestProperties.xml");
    Files.writeString(
        xmlFile,
        "<Widget id=\"PSWidget_TestProperties\"/>",
        StandardCharsets.UTF_8);

    // Modern-only package with no Widgets XML must not pollute inventory.
    Files.createDirectories(packages.resolve("perc.baseWidgets").resolve("widgets"));

    PSWidgetDefinitionXmlInventory.Report report =
        PSWidgetDefinitionXmlInventory.scan(packages);
    assertEquals(1, report.all().size());
    assertEquals(0, report.nonWaived().size());
    assertEquals(1, report.waived().size());
    assertTrue(report.isClean());
    Path expectedAbs = xmlFile.toAbsolutePath().normalize();
    assertEquals(expectedAbs, report.waived().get(0).xmlPath());
    assertTrue(report.waived().get(0).xmlPath().isAbsolute());
    assertTrue(report.waived().get(0).xmlPath().startsWith(widgets.toAbsolutePath().normalize()));
    PSWidgetDefinitionXmlInventory.assertNoNonWaivedWidgetDefinitionXml(packages);
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
  void tempTree_mixedWaivedAndNonWaived_reportsOnlyNonWaivedAsFailures() throws Exception {
    Path packages = tempDir.resolve("Packages");

    Path waivedWidgets =
        packages
            .resolve("perc.Test")
            .resolve("sys__UserDependency--rxconfig")
            .resolve("Widgets");
    Files.createDirectories(waivedWidgets);
    Files.writeString(
        waivedWidgets.resolve("PSWidget_TestProperties.xml"),
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
    assertEquals(1, report.waived().size());
    assertEquals(1, report.nonWaived().size());
    assertEquals("perc.widget.form", report.nonWaived().get(0).packageDirName());
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
