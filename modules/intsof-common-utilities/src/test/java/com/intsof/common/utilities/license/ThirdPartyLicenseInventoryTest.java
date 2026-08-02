/*
 * Copyright 2026 Intersoft Data Labs (https://intsof.com)
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intsof.common.utilities.license;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intsof.common.utilities.license.ThirdPartyLicenseInventory.NpmPackage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ThirdPartyLicenseInventoryTest {

  @TempDir Path tempDir;

  @Test
  void packageNameFromLockKeyHandlesScopedAndNested() {
    assertEquals("react", ThirdPartyLicenseInventory.packageNameFromLockKey("node_modules/react"));
    assertEquals(
        "@scope/pkg", ThirdPartyLicenseInventory.packageNameFromLockKey("node_modules/@scope/pkg"));
    assertEquals(
        "bar",
        ThirdPartyLicenseInventory.packageNameFromLockKey("node_modules/foo/node_modules/bar"));
    assertEquals(null, ThirdPartyLicenseInventory.packageNameFromLockKey(""));
    assertEquals(null, ThirdPartyLicenseInventory.packageNameFromLockKey("../../../vendor/x"));
  }

  @Test
  void readProductionPackagesSkipsDevAndRequiresVersion() throws Exception {
    Path lock = tempDir.resolve("package-lock.json");
    Files.writeString(
        lock,
        """
        {
          "lockfileVersion": 3,
          "packages": {
            "": { "name": "app", "version": "1.0.0" },
            "node_modules/react": { "version": "19.2.8", "license": "MIT" },
            "node_modules/vitest": { "version": "4.1.0", "license": "MIT", "dev": true },
            "node_modules/@scope/pkg": { "version": "1.2.3", "license": "Apache-2.0" },
            "node_modules/noversion": { "license": "MIT" }
          }
        }
        """,
        StandardCharsets.UTF_8);

    List<NpmPackage> pkgs =
        ThirdPartyLicenseInventory.readProductionPackagesFromLockFile(lock, tempDir);
    Set<String> names = pkgs.stream().map(NpmPackage::name).collect(Collectors.toSet());
    assertTrue(names.contains("react"));
    assertTrue(names.contains("@scope/pkg"));
    assertFalse(names.contains("vitest"));
    assertFalse(names.contains("noversion"));

    NpmPackage react =
        pkgs.stream().filter(p -> p.name().equals("react")).findFirst().orElseThrow();
    assertEquals("19.2.8", react.version());
    assertEquals("MIT", react.license());
    assertTrue(react.toInventoryLine().contains("npm:react:19.2.8"));
  }

  @Test
  void mergeContainsBothSections() {
    String merged =
        ThirdPartyLicenseInventory.mergeMavenAndNpm(
            "Lists of 1 third-party dependencies.\n     (MIT) foo",
            "Lists of 1 third-party npm dependencies (production).\n     (MIT) react",
            "Demo inventory");
    assertTrue(merged.startsWith("Demo inventory"));
    assertTrue(merged.contains("Maven third-party dependencies"));
    assertTrue(merged.contains("npm third-party dependencies (production)"));
    assertTrue(merged.contains("foo"));
    assertTrue(merged.contains("react"));
  }

  @Test
  void generateMergedInventoryWritesFiles() throws Exception {
    Path root = tempDir;
    Path out = root.resolve("out");
    Files.createDirectories(out);
    Files.writeString(
        out.resolve(ThirdPartyLicenseInventory.DEFAULT_MAVEN_FILE_NAME),
        "Lists of 1 third-party dependencies.\n     (Apache License, Version 2.0) guava\n",
        StandardCharsets.UTF_8);

    Path ui = root.resolve("ui");
    Files.createDirectories(ui);
    Files.writeString(
        ui.resolve("package-lock.json"),
        """
        {
          "lockfileVersion": 3,
          "packages": {
            "": {},
            "node_modules/jquery": { "version": "3.7.1", "license": "MIT" }
          }
        }
        """,
        StandardCharsets.UTF_8);
    Path list = root.resolve("locks.txt");
    Files.writeString(list, "ui/package-lock.json\n", StandardCharsets.UTF_8);

    Path merged =
        ThirdPartyLicenseInventory.generateMergedInventory(
            root,
            out,
            ThirdPartyLicenseInventory.DEFAULT_MAVEN_FILE_NAME,
            ThirdPartyLicenseInventory.DEFAULT_NPM_FILE_NAME,
            ThirdPartyLicenseInventory.DEFAULT_MERGED_FILE_NAME,
            list,
            "Test product inventory",
            true);

    String text = Files.readString(merged, StandardCharsets.UTF_8);
    assertTrue(text.contains("guava"));
    assertTrue(text.contains("jquery"));
    assertTrue(text.contains("npm:jquery:3.7.1"));
    assertTrue(Files.isRegularFile(out.resolve(ThirdPartyLicenseInventory.DEFAULT_NPM_FILE_NAME)));
  }

  @Test
  void requireMavenFailsWhenMissing() {
    Path out = tempDir.resolve("out");
    Path list = tempDir.resolve("locks.txt");
    assertThrows(
        IllegalStateException.class,
        () ->
            ThirdPartyLicenseInventory.generateMergedInventory(
                tempDir,
                out,
                ThirdPartyLicenseInventory.DEFAULT_MAVEN_FILE_NAME,
                ThirdPartyLicenseInventory.DEFAULT_NPM_FILE_NAME,
                ThirdPartyLicenseInventory.DEFAULT_MERGED_FILE_NAME,
                list,
                null,
                true));
  }

  @Test
  void npmPackageRejectsBlankName() {
    assertThrows(IllegalArgumentException.class, () -> new NpmPackage("  ", "1.0.0", "MIT", "src"));
  }
}
