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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Authored root {@code *.templateDef} inventory after leftover binary conversion (issue #3674 /
 * parent #2630).
 *
 * <p>Cross-platform: {@link Path#resolve(String)} / {@link Files} only.
 */
class PSAuthoredRootTemplateDefInventoryTest {

  @TempDir Path tempDir;

  @Test
  void waivedSet_isEmptyAfterBinaryConversion() {
    assertTrue(PSAuthoredRootTemplateDefInventory.WAIVED_PACKAGE_DIRS.isEmpty());
    assertFalse(PSAuthoredRootTemplateDefInventory.isWaivedPackage("perc.FileAssetWidget"));
    assertFalse(PSAuthoredRootTemplateDefInventory.isWaivedPackage("perc.widgets.image"));
    assertFalse(PSAuthoredRootTemplateDefInventory.isWaivedPackage(null));
    assertFalse(PSAuthoredRootTemplateDefInventory.isWaivedPackage(""));
  }

  @Test
  void productPackagesTree_hasZeroAuthoredRootTemplateDefs() throws Exception {
    Path packagesRoot = locatePackagesRoot();
    assertNotNull(packagesRoot, "Packages root must be visible under perc-packages Surefire");

    PSAuthoredRootTemplateDefInventory.Report report =
        PSAuthoredRootTemplateDefInventory.scan(packagesRoot);
    assertTrue(
        report.isClean(),
        () ->
            "leftover authored root *.templateDef after #3674 conversion: " + report.nonWaived());
    assertTrue(report.all().isEmpty());
    PSAuthoredRootTemplateDefInventory.assertNoNonWaivedAuthoredRootTemplateDefs(packagesRoot);
  }

  @Test
  void scan_reportsPackageRootOnly_notNested() throws Exception {
    Path packages = tempDir.resolve("Packages");
    Path filePkg = packages.resolve("perc.FileAssetWidget");
    Path nested = filePkg.resolve("pages").resolve("perc.fileBinary");
    Files.createDirectories(nested);
    Path leftover = filePkg.resolve("perc.fileBinary.templateDef");
    Files.writeString(leftover, "<assembly-template/>", StandardCharsets.UTF_8);
    Files.writeString(
        nested.resolve("nested.templateDef"), "<assembly-template/>", StandardCharsets.UTF_8);

    PSAuthoredRootTemplateDefInventory.Report report =
        PSAuthoredRootTemplateDefInventory.scan(packages);
    assertEquals(1, report.all().size());
    assertEquals(1, report.nonWaived().size());
    assertEquals("perc.FileAssetWidget", report.nonWaived().get(0).packageDirName());
    assertEquals(
        leftover.toAbsolutePath().normalize(), report.nonWaived().get(0).templateDefPath());
    assertFalse(report.isClean());

    IllegalStateException err =
        assertThrows(
            IllegalStateException.class,
            () ->
                PSAuthoredRootTemplateDefInventory.assertNoNonWaivedAuthoredRootTemplateDefs(
                    packages));
    assertTrue(err.getMessage().contains("perc.FileAssetWidget"));
    assertTrue(err.getMessage().contains("perc.fileBinary.templateDef"));
  }

  @Test
  void scan_rejectsNonDirectory() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSAuthoredRootTemplateDefInventory.scan(tempDir.resolve("missing")));
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
