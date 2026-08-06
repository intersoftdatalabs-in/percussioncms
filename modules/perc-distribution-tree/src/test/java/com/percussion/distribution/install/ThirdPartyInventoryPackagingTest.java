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

package com.percussion.distribution.install;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

/**
 * Packaging guard for the build-generated third-party license inventory (issue #1689).
 *
 * <p>When the reactor root has already produced {@code
 * target/generated-sources/license/THIRD-PARTY.txt} (full reactor build or {@code mvn
 * license:aggregate-add-third-party} without {@code -N}), the distribution assembly root must also
 * contain that file after {@code generate-resources} (copy-license-inventory). Standalone module
 * test runs without a prior generation step skip the assembly assertion rather than inventing a
 * hand-curated inventory.
 */
public class ThirdPartyInventoryPackagingTest {

  @Test
  void assemblyShipsGeneratedThirdPartyInventoryWhenAvailable() throws IOException {
    Path repoRoot = resolveRepoRoot();
    Path generated =
        repoRoot
            .resolve("target")
            .resolve("generated-sources")
            .resolve("license")
            .resolve("THIRD-PARTY.txt");
    assumeTrue(
        Files.isRegularFile(generated) && Files.size(generated) > 0,
        "THIRD-PARTY.txt not generated yet — run from repo root (no -N):"
            + " mvnw license:aggregate-add-third-party");

    Path assemblyCopy =
        Paths.get("target")
            .resolve("classes")
            .resolve("distribution")
            .resolve("THIRD-PARTY.txt")
            .toAbsolutePath()
            .normalize();
    assumeTrue(
        Files.isRegularFile(assemblyCopy),
        "Assembly copy not present yet (generate-resources / copy-license-inventory not run).");

    String text = Files.readString(assemblyCopy, StandardCharsets.UTF_8);
    assertFalse(text.isBlank(), "Packaged THIRD-PARTY.txt must not be blank");
    // Merged inventory shape (Maven + npm) — issue #1689
    assertTrue(
        text.contains("Maven third-party dependencies"),
        "Packaged THIRD-PARTY.txt should include the Maven inventory section header");
    assertTrue(
        text.contains("npm third-party dependencies (production)"),
        "Packaged THIRD-PARTY.txt should include the npm production section header");
    assertTrue(
        text.contains("npm:"),
        "Packaged THIRD-PARTY.txt should list at least one npm:coordinate entry");
  }

  @Test
  void assemblyShipsStableLicenseAndNoticeWhenPresent() throws IOException {
    Path repoRoot = resolveRepoRoot();
    Path licenseSrc = repoRoot.resolve("LICENSE.txt");
    Path noticeSrc = repoRoot.resolve("NOTICE.txt");
    assumeTrue(Files.isRegularFile(licenseSrc), "LICENSE.txt missing at repo root");
    assumeTrue(Files.isRegularFile(noticeSrc), "NOTICE.txt missing at repo root");

    Path assemblyDir =
        Paths.get("target").resolve("classes").resolve("distribution").toAbsolutePath().normalize();
    assumeTrue(
        Files.isDirectory(assemblyDir),
        "Assembly directory not built yet — generate-resources not run");

    Path licenseOut = assemblyDir.resolve("LICENSE.txt");
    Path noticeOut = assemblyDir.resolve("NOTICE.txt");
    assumeTrue(
        Files.isRegularFile(licenseOut) && Files.isRegularFile(noticeOut),
        "LICENSE.txt/NOTICE.txt not yet copied into assembly (copy-license-inventory not run)");

    assertFalse(Files.readString(licenseOut, StandardCharsets.UTF_8).isBlank());
    String notice = Files.readString(noticeOut, StandardCharsets.UTF_8);
    assertTrue(
        notice.contains("THIRD-PARTY.txt"),
        "Packaged NOTICE.txt must point at the generated inventory");
  }

  /**
   * Resolves the monorepo root whether Surefire runs from {@code modules/perc-distribution-tree} or
   * another cwd. Portable Path API only.
   */
  private static Path resolveRepoRoot() {
    Path cwd = Paths.get("").toAbsolutePath().normalize();
    Path probe = cwd;
    for (int i = 0; i < 8 && probe != null; i++) {
      if (Files.isRegularFile(probe.resolve("LICENSE.txt"))
          && Files.isRegularFile(probe.resolve("pom.xml"))) {
        return probe;
      }
      probe = probe.getParent();
    }
    // Fallback: module is two levels under root.
    return cwd.resolve("..").resolve("..").normalize();
  }
}
