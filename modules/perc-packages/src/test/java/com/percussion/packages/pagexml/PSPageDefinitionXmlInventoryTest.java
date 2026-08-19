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
 * G4 CI/inventory gate: zero non-waived product Page definition XML under Packages (issue #3581 /
 * parent #2630). Explicit waiver: {@code perc.Test} only.
 *
 * <p>Cross-platform: all path construction uses {@link Path#resolve(String)} / {@link Files}.
 */
class PSPageDefinitionXmlInventoryTest {

  @TempDir Path tempDir;

  @Test
  void waivedPackageSet_isExplicitlyPercTestOnly() {
    assertEquals(Set.of("perc.Test"), PSPageDefinitionXmlInventory.WAIVED_PACKAGE_DIRS);
    assertTrue(PSPageDefinitionXmlInventory.isWaivedPackage("perc.Test"));
    assertFalse(PSPageDefinitionXmlInventory.isWaivedPackage("perc.baseTemplates"));
    assertFalse(PSPageDefinitionXmlInventory.isWaivedPackage("perc.responsiveTemplates"));
    assertFalse(PSPageDefinitionXmlInventory.isWaivedPackage(null));
    assertFalse(PSPageDefinitionXmlInventory.isWaivedPackage(""));
  }

  @Test
  void productPackagesTree_hasZeroNonWaivedPageDefinitionXml() throws Exception {
    Path packagesRoot = locatePackagesRoot();
    assertNotNull(
        packagesRoot,
        "Packages root must be visible when running module Surefire from perc-packages "
            + "(src/main/resources/Packages)");

    PSDefinitionXmlShipPathInventory.Report report = PSPageDefinitionXmlInventory.scan(packagesRoot);
    assertTrue(
        report.isClean(),
        () ->
            "G4: non-waived Page def XML must not reappear under product Packages; found: "
                + report.nonWaived());
    assertEquals(0, report.nonWaived().size());

    PSPageDefinitionXmlInventory.assertNoNonWaivedPageDefinitionXml(packagesRoot);
  }

  @Test
  void tempTree_onlyWaivedXml_isClean() throws Exception {
    Path packages = tempDir.resolve("Packages");
    Path pages =
        packages
            .resolve("perc.Test")
            .resolve("sys__UserDependency--rxconfig")
            .resolve("Pages");
    Files.createDirectories(pages);
    Path xmlFile = pages.resolve("PSPage_TestProperties.xml");
    Files.writeString(xmlFile, "<Page id=\"PSPage_TestProperties\"/>", StandardCharsets.UTF_8);

    Files.createDirectories(packages.resolve("perc.baseTemplates").resolve("pages"));

    PSDefinitionXmlShipPathInventory.Report report = PSPageDefinitionXmlInventory.scan(packages);
    assertEquals(1, report.all().size());
    assertEquals(0, report.nonWaived().size());
    assertEquals(1, report.waived().size());
    assertTrue(report.isClean());
    Path expectedAbs = xmlFile.toAbsolutePath().normalize();
    assertEquals(expectedAbs, report.waived().get(0).xmlPath());
    assertTrue(report.waived().get(0).xmlPath().isAbsolute());
    assertTrue(report.waived().get(0).xmlPath().startsWith(pages.toAbsolutePath().normalize()));
    PSPageDefinitionXmlInventory.assertNoNonWaivedPageDefinitionXml(packages);
  }

  @Test
  void findingXmlPath_relativePackagesRoot_isAbsoluteUnderPagesDir() throws Exception {
    Path packages = tempDir.resolve("Packages");
    Path pages =
        packages
            .resolve("perc.Test")
            .resolve("sys__UserDependency--rxconfig")
            .resolve("Pages");
    Files.createDirectories(pages);
    Path xmlFile = pages.resolve("PSPage_TestProperties.xml");
    Files.writeString(xmlFile, "<Page/>", StandardCharsets.UTF_8);

    Path relativePackages =
        Path.of(".").toAbsolutePath().normalize().relativize(packages.toAbsolutePath().normalize());
    assertFalse(relativePackages.isAbsolute(), "precondition: packages root is relative");

    PSDefinitionXmlShipPathInventory.Report report =
        PSPageDefinitionXmlInventory.scan(relativePackages);
    assertEquals(1, report.all().size());
    Path found = report.all().get(0).xmlPath();
    assertTrue(found.isAbsolute());
    assertEquals(xmlFile.toAbsolutePath().normalize(), found);
    assertTrue(found.startsWith(pages.toAbsolutePath().normalize()));
    assertTrue(Files.isRegularFile(found));
  }

  @Test
  void tempTree_dummyNonWaivedXml_failsGate() throws Exception {
    Path packages = tempDir.resolve("Packages");
    Path pages =
        packages
            .resolve("perc.baseTemplates")
            .resolve("sys__UserDependency--rxconfig")
            .resolve("Pages");
    Files.createDirectories(pages);
    Path dummy = pages.resolve("percBasePlain.xml");
    Files.writeString(dummy, "<Page id=\"perc.base.plain\"/>", StandardCharsets.UTF_8);

    PSDefinitionXmlShipPathInventory.Report report = PSPageDefinitionXmlInventory.scan(packages);
    assertFalse(report.isClean());
    assertEquals(1, report.nonWaived().size());
    assertEquals("perc.baseTemplates", report.nonWaived().get(0).packageDirName());
    assertEquals("percBasePlain.xml", report.nonWaived().get(0).xmlPath().getFileName().toString());

    IllegalStateException err =
        assertThrows(
            IllegalStateException.class,
            () -> PSPageDefinitionXmlInventory.assertNoNonWaivedPageDefinitionXml(packages));
    assertTrue(err.getMessage().contains("G4"));
    assertTrue(err.getMessage().contains("perc.baseTemplates"));
    assertTrue(err.getMessage().contains("percBasePlain.xml"));
  }

  @Test
  void tempTree_rxconfigShipPath_isAlsoInventoried() throws Exception {
    Path packages = tempDir.resolve("Packages");
    Path pages = packages.resolve("perc.responsiveTemplates").resolve("rxconfig").resolve("Pages");
    Files.createDirectories(pages);
    Files.writeString(pages.resolve("percRespPlain.xml"), "<Page/>", StandardCharsets.UTF_8);

    PSDefinitionXmlShipPathInventory.Report report = PSPageDefinitionXmlInventory.scan(packages);
    assertFalse(report.isClean());
    assertEquals(1, report.nonWaived().size());
    assertEquals("perc.responsiveTemplates", report.nonWaived().get(0).packageDirName());
    assertEquals("percRespPlain.xml", report.nonWaived().get(0).xmlPath().getFileName().toString());
  }

  @Test
  void modernPagesAuthoringTree_isNotDefinitionXml() throws Exception {
    Path packages = tempDir.resolve("Packages");
    Path modern =
        packages
            .resolve("perc.baseTemplates")
            .resolve("pages")
            .resolve("perc.base.plain");
    Files.createDirectories(modern);
    Files.writeString(
        modern.resolve("component-package.json"),
        "{\"id\":\"perc.base.plain\"}",
        StandardCharsets.UTF_8);
    Files.writeString(modern.resolve("not-a-ship-path.xml"), "<ignored/>", StandardCharsets.UTF_8);

    PSDefinitionXmlShipPathInventory.Report report = PSPageDefinitionXmlInventory.scan(packages);
    assertTrue(report.isClean());
    assertEquals(0, report.all().size());
  }

  @Test
  void tempTree_mixedWaivedAndNonWaived_reportsOnlyNonWaivedAsFailures() throws Exception {
    Path packages = tempDir.resolve("Packages");

    Path waivedPages =
        packages
            .resolve("perc.Test")
            .resolve("sys__UserDependency--rxconfig")
            .resolve("Pages");
    Files.createDirectories(waivedPages);
    Files.writeString(waivedPages.resolve("PSPage_TestProperties.xml"), "<Page/>", StandardCharsets.UTF_8);

    Path badPages =
        packages
            .resolve("perc.baseTemplates")
            .resolve("sys__UserDependency--rxconfig")
            .resolve("Pages");
    Files.createDirectories(badPages);
    Files.writeString(badPages.resolve("percBasePlain.xml"), "<Page/>", StandardCharsets.UTF_8);

    PSDefinitionXmlShipPathInventory.Report report = PSPageDefinitionXmlInventory.scan(packages);
    assertEquals(2, report.all().size());
    assertEquals(1, report.waived().size());
    assertEquals(1, report.nonWaived().size());
    assertEquals("perc.baseTemplates", report.nonWaived().get(0).packageDirName());
    assertFalse(report.isClean());
  }

  @Test
  void resolvePagesDir_usesPortableSegments() {
    Path pkg = Path.of("pkgRoot");
    Path pages = PSPageDefinitionXmlInventory.resolvePagesDir(pkg);
    assertEquals(pkg.resolve("sys__UserDependency--rxconfig").resolve("Pages"), pages);
    Path rx = PSPageDefinitionXmlInventory.resolveRxconfigPagesDir(pkg);
    assertEquals(pkg.resolve("rxconfig").resolve("Pages"), rx);
  }

  @Test
  void scan_rejectsNonDirectoryRoot() {
    Path missing = tempDir.resolve("does-not-exist");
    assertThrows(IllegalArgumentException.class, () -> PSPageDefinitionXmlInventory.scan(missing));
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
