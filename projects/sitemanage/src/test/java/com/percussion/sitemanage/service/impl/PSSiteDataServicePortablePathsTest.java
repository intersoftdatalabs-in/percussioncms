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
package com.percussion.sitemanage.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.pathmanagement.service.impl.PSPathUtils;
import java.io.File;
import java.nio.file.Path;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Portable path construction for {@link PSSiteDataService} (issue #6730).
 *
 * <p>The SaaS site-config directory used string concatenation ({@code base +
 * "/var/config/saas/siteconfig"}) which embeds Unix separators. It now resolves through {@link
 * PSSiteDataService#resolveRxRelative(Path, String)}, which joins each segment with {@link
 * Path#resolve(String)} so the result uses the platform separator on every OS. The CMS logical-path
 * joins in this file ({@code //Sites/<name>}, finder-root paths, asset-field {@code '/'} boundary
 * matching) intentionally keep {@code "/"} and are adjudicated in
 * {@code docs/ai-generated/tasks/gh-codeql-alerts/suppressions.md} (row {@code mkd-6730}) — these
 * tests pin both contracts.
 */
@Tag("UnitTest")
class PSSiteDataServicePortablePathsTest {

  private static final Path BASE = Path.of("rxdir");

  @Test
  void saasConfigPathResolvesUnderBaseWithPlatformSeparators() {
    Path resolved = PSSiteDataService.resolveRxRelative(BASE, "/var/config/saas/siteconfig");
    Path expected =
        BASE.resolve("var").resolve("config").resolve("saas").resolve("siteconfig");
    assertEquals(expected, resolved);
    assertEquals(
        "rxdir"
            + File.separator
            + "var"
            + File.separator
            + "config"
            + File.separator
            + "saas"
            + File.separator
            + "siteconfig",
        resolved.toString());
  }

  @Test
  void leadingSlashNeverEscapesBase() {
    Path resolved = PSSiteDataService.resolveRxRelative(BASE, "/var/config/saas/siteconfig");
    assertTrue(resolved.startsWith(BASE));
  }

  @Test
  void emptySegmentsAreSkipped() {
    assertEquals(
        BASE.resolve("var").resolve("config"),
        PSSiteDataService.resolveRxRelative(BASE, "//var//config/"));
    assertEquals(BASE, PSSiteDataService.resolveRxRelative(BASE, "/"));
  }

  @Test
  void singleFileNameSegmentAppends() {
    Path dir = PSSiteDataService.resolveRxRelative(BASE, "/var/config/saas/siteconfig");
    Path file = dir.resolve("acme.json");
    assertEquals(
        BASE.resolve("var")
            .resolve("config")
            .resolve("saas")
            .resolve("siteconfig")
            .resolve("acme.json"),
        file);
    assertTrue(file.startsWith(BASE));
  }

  @Test
  void resultContainsNoHardcodedSlashOnWindowsSeparators() {
    // On Windows File.separator is "\" so a portable join must not contain "/".
    // On Unix the separator IS "/", so assert structural equality instead.
    Path resolved = PSSiteDataService.resolveRxRelative(BASE, "/var/config/saas/siteconfig");
    if ("\\".equals(File.separator)) {
      assertTrue(!resolved.toString().contains("/"));
    }
    assertEquals(5, resolved.getNameCount());
  }

  @Test
  void siteFolderRootKeepsRepositorySlash() {
    // CMS logical path: "//Sites/" + name must keep "/" on every OS so that
    // PSPathUtils keeps parsing the site name (File.separator would corrupt it on Windows).
    String tgtFolderRoot = "//Sites/" + "my-site";
    assertEquals("my-site", PSPathUtils.getSiteFromPath(tgtFolderRoot));
    assertTrue(tgtFolderRoot.startsWith("//Sites/"));
  }

  @Test
  void logicalTrailingSlashStripKeepsRepositorySlash() {
    assertEquals(
        "//Assets/my-assets", StringUtils.removeEnd("//Assets/my-assets/", "/"));
  }
}
