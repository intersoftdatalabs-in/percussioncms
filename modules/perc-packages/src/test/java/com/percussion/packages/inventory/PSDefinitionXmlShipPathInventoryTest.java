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

package com.percussion.packages.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.packages.inventory.PSDefinitionXmlShipPathInventory.Kind;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Combined G4 scanner coverage for Page + Gadget ship paths (issue #3581).
 *
 * <p>Cross-platform: all path construction uses {@link Path#resolve(String)} / {@link Files}.
 */
class PSDefinitionXmlShipPathInventoryTest {

  @TempDir Path tempDir;

  @Test
  void productPackagesTree_pagesAndGadgets_areClean() throws Exception {
    Path packagesRoot = locatePackagesRoot();
    assertNotNull(packagesRoot, "Packages root must be visible under perc-packages Surefire");

    PSDefinitionXmlShipPathInventory.Report report =
        PSDefinitionXmlShipPathInventory.scanPagesAndGadgets(packagesRoot);
    assertTrue(
        report.isClean(),
        () -> "G4: non-waived Page/Gadget def XML under product Packages: " + report.nonWaived());
    PSDefinitionXmlShipPathInventory.assertNoNonWaivedPageOrGadgetDefinitionXml(packagesRoot);
  }

  @Test
  void combinedScan_reportsBothKinds() throws Exception {
    Path packages = tempDir.resolve("Packages");
    Path pages =
        packages
            .resolve("perc.baseTemplates")
            .resolve("sys__UserDependency--rxconfig")
            .resolve("Pages");
    Path gadgets =
        packages.resolve("perc.Baseline").resolve("rxconfig").resolve("Gadgets");
    Files.createDirectories(pages);
    Files.createDirectories(gadgets);
    Files.writeString(pages.resolve("page.xml"), "<Page/>", StandardCharsets.UTF_8);
    Files.writeString(gadgets.resolve("gadget.xml"), "<Module/>", StandardCharsets.UTF_8);

    PSDefinitionXmlShipPathInventory.Report report =
        PSDefinitionXmlShipPathInventory.scanPagesAndGadgets(packages);
    assertEquals(2, report.all().size());
    assertEquals(2, report.nonWaived().size());
    assertFalse(report.isClean());
    assertEquals(1, report.nonWaived().stream().filter(f -> f.kind() == Kind.PAGE).count());
    assertEquals(1, report.nonWaived().stream().filter(f -> f.kind() == Kind.GADGET).count());

    IllegalStateException err =
        assertThrows(
            IllegalStateException.class,
            () ->
                PSDefinitionXmlShipPathInventory.assertNoNonWaivedPageOrGadgetDefinitionXml(
                    packages));
    assertTrue(err.getMessage().contains("Page"));
    assertTrue(err.getMessage().contains("Gadget"));
    assertTrue(err.getMessage().contains("page.xml"));
    assertTrue(err.getMessage().contains("gadget.xml"));
  }

  @Test
  void scan_singleKind_doesNotIncludeOtherKind() throws Exception {
    Path packages = tempDir.resolve("Packages");
    Path pages =
        packages
            .resolve("perc.baseTemplates")
            .resolve("sys__UserDependency--rxconfig")
            .resolve("Pages");
    Path gadgets =
        packages
            .resolve("perc.baseTemplates")
            .resolve("sys__UserDependency--rxconfig")
            .resolve("Gadgets");
    Files.createDirectories(pages);
    Files.createDirectories(gadgets);
    Files.writeString(pages.resolve("page.xml"), "<Page/>", StandardCharsets.UTF_8);
    Files.writeString(gadgets.resolve("gadget.xml"), "<Module/>", StandardCharsets.UTF_8);

    PSDefinitionXmlShipPathInventory.Report pagesOnly =
        PSDefinitionXmlShipPathInventory.scan(packages, Kind.PAGE);
    assertEquals(1, pagesOnly.all().size());
    assertEquals(Kind.PAGE, pagesOnly.all().get(0).kind());

    PSDefinitionXmlShipPathInventory.Report gadgetsOnly =
        PSDefinitionXmlShipPathInventory.scan(packages, Kind.GADGET);
    assertEquals(1, gadgetsOnly.all().size());
    assertEquals(Kind.GADGET, gadgetsOnly.all().get(0).kind());
  }

  @Test
  void scan_rejectsNullKind() {
    assertThrows(
        NullPointerException.class,
        () -> PSDefinitionXmlShipPathInventory.scan(tempDir, (Kind) null));
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
