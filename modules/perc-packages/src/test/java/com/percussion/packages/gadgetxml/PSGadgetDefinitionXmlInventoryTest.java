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

import com.percussion.packages.inventory.PSDefinitionXmlShipPathInventory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * G4 CI/inventory gate: zero non-waived product Gadget definition XML under Packages (issue #3581 /
 * parent #2630). Explicit waiver: {@code perc.Test} only.
 *
 * <p>Cross-platform: all path construction uses {@link Path#resolve(String)} / {@link Files}.
 */
class PSGadgetDefinitionXmlInventoryTest {

  @TempDir Path tempDir;

  @Test
  void waivedPackageSet_isExplicitlyPercTestOnly() {
    assertEquals(Set.of("perc.Test"), PSGadgetDefinitionXmlInventory.WAIVED_PACKAGE_DIRS);
    assertTrue(PSGadgetDefinitionXmlInventory.isWaivedPackage("perc.Test"));
    assertFalse(PSGadgetDefinitionXmlInventory.isWaivedPackage("perc.Baseline"));
    assertFalse(PSGadgetDefinitionXmlInventory.isWaivedPackage(null));
    assertFalse(PSGadgetDefinitionXmlInventory.isWaivedPackage(""));
  }

  @Test
  void productPackagesTree_hasZeroNonWaivedGadgetDefinitionXml() throws Exception {
    Path packagesRoot = locatePackagesRoot();
    assertNotNull(
        packagesRoot,
        "Packages root must be visible when running module Surefire from perc-packages "
            + "(src/main/resources/Packages)");

    PSDefinitionXmlShipPathInventory.Report report =
        PSGadgetDefinitionXmlInventory.scan(packagesRoot);
    assertTrue(
        report.isClean(),
        () ->
            "G4: non-waived Gadget def XML must not reappear under product Packages; found: "
                + report.nonWaived());
    assertEquals(0, report.nonWaived().size());

    PSGadgetDefinitionXmlInventory.assertNoNonWaivedGadgetDefinitionXml(packagesRoot);
  }

  @Test
  void tempTree_onlyWaivedXml_isClean() throws Exception {
    Path packages = tempDir.resolve("Packages");
    Path gadgets =
        packages
            .resolve("perc.Test")
            .resolve("sys__UserDependency--rxconfig")
            .resolve("Gadgets");
    Files.createDirectories(gadgets);
    Path xmlFile = gadgets.resolve("PSGadget_TestProperties.xml");
    Files.writeString(xmlFile, "<Module id=\"PSGadget_TestProperties\"/>", StandardCharsets.UTF_8);

    PSDefinitionXmlShipPathInventory.Report report = PSGadgetDefinitionXmlInventory.scan(packages);
    assertEquals(1, report.all().size());
    assertEquals(0, report.nonWaived().size());
    assertEquals(1, report.waived().size());
    assertTrue(report.isClean());
    Path expectedAbs = xmlFile.toAbsolutePath().normalize();
    assertEquals(expectedAbs, report.waived().get(0).xmlPath());
    assertTrue(report.waived().get(0).xmlPath().isAbsolute());
    assertTrue(report.waived().get(0).xmlPath().startsWith(gadgets.toAbsolutePath().normalize()));
    PSGadgetDefinitionXmlInventory.assertNoNonWaivedGadgetDefinitionXml(packages);
  }

  @Test
  void findingXmlPath_relativePackagesRoot_isAbsoluteUnderGadgetsDir() throws Exception {
    Path packages = tempDir.resolve("Packages");
    Path gadgets =
        packages
            .resolve("perc.Test")
            .resolve("sys__UserDependency--rxconfig")
            .resolve("Gadgets");
    Files.createDirectories(gadgets);
    Path xmlFile = gadgets.resolve("PSGadget_TestProperties.xml");
    Files.writeString(xmlFile, "<Module/>", StandardCharsets.UTF_8);

    Path relativePackages =
        Path.of(".").toAbsolutePath().normalize().relativize(packages.toAbsolutePath().normalize());
    assertFalse(relativePackages.isAbsolute(), "precondition: packages root is relative");

    PSDefinitionXmlShipPathInventory.Report report =
        PSGadgetDefinitionXmlInventory.scan(relativePackages);
    assertEquals(1, report.all().size());
    Path found = report.all().get(0).xmlPath();
    assertTrue(found.isAbsolute());
    assertEquals(xmlFile.toAbsolutePath().normalize(), found);
    assertTrue(found.startsWith(gadgets.toAbsolutePath().normalize()));
    assertTrue(Files.isRegularFile(found));
  }

  @Test
  void tempTree_dummyNonWaivedXml_failsGate() throws Exception {
    Path packages = tempDir.resolve("Packages");
    Path gadgets =
        packages
            .resolve("perc.Baseline")
            .resolve("sys__UserDependency--rxconfig")
            .resolve("Gadgets");
    Files.createDirectories(gadgets);
    Path dummy = gadgets.resolve("percWelcome.xml");
    Files.writeString(dummy, "<Module id=\"cm1_welcome_gadget\"/>", StandardCharsets.UTF_8);

    PSDefinitionXmlShipPathInventory.Report report = PSGadgetDefinitionXmlInventory.scan(packages);
    assertFalse(report.isClean());
    assertEquals(1, report.nonWaived().size());
    assertEquals("perc.Baseline", report.nonWaived().get(0).packageDirName());
    assertEquals("percWelcome.xml", report.nonWaived().get(0).xmlPath().getFileName().toString());

    IllegalStateException err =
        assertThrows(
            IllegalStateException.class,
            () -> PSGadgetDefinitionXmlInventory.assertNoNonWaivedGadgetDefinitionXml(packages));
    assertTrue(err.getMessage().contains("G4"));
    assertTrue(err.getMessage().contains("perc.Baseline"));
    assertTrue(err.getMessage().contains("percWelcome.xml"));
  }

  @Test
  void tempTree_rxconfigShipPath_isAlsoInventoried() throws Exception {
    Path packages = tempDir.resolve("Packages");
    Path gadgets = packages.resolve("perc.Baseline").resolve("rxconfig").resolve("Gadgets");
    Files.createDirectories(gadgets);
    Files.writeString(gadgets.resolve("percWelcome.xml"), "<Module/>", StandardCharsets.UTF_8);

    PSDefinitionXmlShipPathInventory.Report report = PSGadgetDefinitionXmlInventory.scan(packages);
    assertFalse(report.isClean());
    assertEquals(1, report.nonWaived().size());
    assertEquals("perc.Baseline", report.nonWaived().get(0).packageDirName());
  }

  @Test
  void modernGadgetCatalog_isNotDefinitionXml() throws Exception {
    Path packages = tempDir.resolve("Packages");
    Path catalog = packages.resolve("perc.Baseline").resolve("gadgets");
    Files.createDirectories(catalog);
    Files.writeString(
        catalog.resolve("gadget-catalog.json"), "{\"gadgets\":[]}", StandardCharsets.UTF_8);
    Files.writeString(catalog.resolve("orphan.xml"), "<ignored/>", StandardCharsets.UTF_8);

    PSDefinitionXmlShipPathInventory.Report report = PSGadgetDefinitionXmlInventory.scan(packages);
    assertTrue(report.isClean());
    assertEquals(0, report.all().size());
  }

  @Test
  void tempTree_mixedWaivedAndNonWaived_reportsOnlyNonWaivedAsFailures() throws Exception {
    Path packages = tempDir.resolve("Packages");

    Path waived =
        packages
            .resolve("perc.Test")
            .resolve("sys__UserDependency--rxconfig")
            .resolve("Gadgets");
    Files.createDirectories(waived);
    Files.writeString(waived.resolve("PSGadget_TestProperties.xml"), "<Module/>", StandardCharsets.UTF_8);

    Path bad =
        packages
            .resolve("perc.Baseline")
            .resolve("sys__UserDependency--rxconfig")
            .resolve("Gadgets");
    Files.createDirectories(bad);
    Files.writeString(bad.resolve("percWelcome.xml"), "<Module/>", StandardCharsets.UTF_8);

    PSDefinitionXmlShipPathInventory.Report report = PSGadgetDefinitionXmlInventory.scan(packages);
    assertEquals(2, report.all().size());
    assertEquals(1, report.waived().size());
    assertEquals(1, report.nonWaived().size());
    assertEquals("perc.Baseline", report.nonWaived().get(0).packageDirName());
    assertFalse(report.isClean());
  }

  @Test
  void resolveGadgetsDir_usesPortableSegments() {
    Path pkg = Path.of("pkgRoot");
    Path gadgets = PSGadgetDefinitionXmlInventory.resolveGadgetsDir(pkg);
    assertEquals(pkg.resolve("sys__UserDependency--rxconfig").resolve("Gadgets"), gadgets);
    Path rx = PSGadgetDefinitionXmlInventory.resolveRxconfigGadgetsDir(pkg);
    assertEquals(pkg.resolve("rxconfig").resolve("Gadgets"), rx);
  }

  @Test
  void scan_rejectsNonDirectoryRoot() {
    Path missing = tempDir.resolve("does-not-exist");
    assertThrows(IllegalArgumentException.class, () -> PSGadgetDefinitionXmlInventory.scan(missing));
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
