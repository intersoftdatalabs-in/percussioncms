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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.packages.pagexml.PSPageXmlMigrationDryRun.Entry;
import com.percussion.packages.pagexml.PSPageXmlMigrationDryRun.Report;
import com.percussion.packages.pagexml.PSPageXmlMigrationDryRun.Status;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Legacy definition-XML migration dry-run + per-package report panel (issue #5863 / parent
 * #2626, vertical slice 2).
 *
 * <p>The dry-run simulates parse → compile → manifest readback → install-XML parity without
 * writing anything; the report renders per-package migration status (what would change, what
 * blocks). The assembler fallback path is removed behind migrated definitions (issue #5864):
 * unregistered manifest assemblers fail the readback and surface as {@link Status#BLOCKED}.
 */
class PSPageXmlMigrationDryRunTest {

  private static final List<String> FIXTURES = List.of(
      "perc.base.plain.templateDef",
      "perc.base.headerFooter.templateDef",
      "perc.fileBinary.templateDef",
      "perc.imageMainBinary.templateDef",
      "perc.imageThumbBinary.templateDef");

  @TempDir Path tempDir;

  @Test
  void fixtureDir_allReadyWithExpectedManifestShape() throws Exception {
    Path legacyDir = stageFixtures("legacy");

    Report report = PSPageXmlMigrationDryRun.dryRun(legacyDir);

    assertEquals(FIXTURES.size(), report.getTotalCount());
    assertEquals(FIXTURES.size(), report.getReadyCount());
    assertEquals(0, report.getBlockedCount());
    assertTrue(report.isClean());
    for (Entry e : report.getEntries()) {
      assertEquals(Status.READY, e.getStatus());
      assertNull(e.getBlockReason());
      assertNotNull(e.getManifestId());
      assertNotNull(e.getTemplateType());
      assertNotNull(e.getAssembler());
      assertTrue(e.getSlotCount() >= 0);
      assertTrue(e.getBindingCount() >= 0);
    }
    Entry plain = byFile(report, "perc.base.plain.templateDef");
    assertNotNull(plain);
    assertEquals("perc.base.plain", plain.getManifestId());
    assertEquals("page", plain.getTemplateType());
    assertEquals("pageAssembler", plain.getAssembler());
    assertEquals(1, plain.getSlotCount());
    assertEquals(1, plain.getRegionCount());
    Entry binary = byFile(report, "perc.fileBinary.templateDef");
    assertNotNull(binary);
    assertEquals("binary", binary.getTemplateType());
    assertEquals("binaryAssembler", binary.getAssembler());
    assertEquals(6, binary.getBindingCount());
  }

  @Test
  void dryRun_writesNothing() throws Exception {
    Path legacyDir = stageFixtures("legacy");
    Path probe = legacyDir.resolve("component-package.json");

    PSPageXmlMigrationDryRun.dryRun(legacyDir);

    assertFalse(Files.exists(probe));
    assertFalse(Files.exists(legacyDir.resolve("templates")));
    try (var stream = Files.list(legacyDir)) {
      assertEquals(
          FIXTURES.size(), stream.filter(Files::isRegularFile).count(),
          "dry-run must not create files beside the legacy inputs");
    }
  }

  @Test
  void malformedFile_blockedWithReason() throws Exception {
    Path legacyDir = tempDir.resolve("legacy");
    Files.createDirectories(legacyDir);
    Path broken = legacyDir.resolve("perc.broken.templateDef");
    Files.writeString(broken, "this is not <xml at all", StandardCharsets.UTF_8);

    Entry entry = PSPageXmlMigrationDryRun.dryRunOne(broken);

    assertEquals(Status.BLOCKED, entry.getStatus());
    assertNotNull(entry.getBlockReason());
    assertFalse(entry.getBlockReason().isBlank());
    assertNull(entry.getManifestId());

    Report report = PSPageXmlMigrationDryRun.dryRun(legacyDir);
    assertEquals(1, report.getTotalCount());
    assertEquals(0, report.getReadyCount());
    assertEquals(1, report.getBlockedCount());
    assertFalse(report.isClean());
  }

  @Test
  void emptyDir_emptyCleanReport() throws Exception {
    Path legacyDir = tempDir.resolve("empty");
    Files.createDirectories(legacyDir);

    Report report = PSPageXmlMigrationDryRun.dryRun(legacyDir);

    assertEquals(0, report.getTotalCount());
    assertTrue(report.isClean());
    assertTrue(report.getEntries().isEmpty());
  }

  @Test
  void reportPanel_rendersPerPackageStatus() throws Exception {
    Path legacyDir = stageFixtures("mixed");
    Files.writeString(
        legacyDir.resolve("perc.broken.templateDef"), "<broken", StandardCharsets.UTF_8);

    Report report = PSPageXmlMigrationDryRun.dryRun(legacyDir);
    assertEquals(FIXTURES.size() + 1, report.getTotalCount());
    assertEquals(FIXTURES.size(), report.getReadyCount());
    assertEquals(1, report.getBlockedCount());

    String markdown = normalize(report.toMarkdown());
    assertTrue(markdown.contains("Ready: " + FIXTURES.size()));
    assertTrue(markdown.contains("Blocked: 1"));
    for (String fixture : FIXTURES) {
      assertTrue(markdown.contains("| " + fixture + " | READY |"),
          "markdown panel must render a READY row for " + fixture);
    }
    assertTrue(markdown.contains("| perc.broken.templateDef | BLOCKED |"),
        "markdown panel must render a BLOCKED row for the malformed file");
    assertTrue(markdown.contains("perc.base.plain"),
        "markdown panel must show the would-be manifest id");

    String text = normalize(report.toText());
    assertTrue(text.contains("READY perc.base.plain.templateDef -> perc.base.plain"));
    assertTrue(text.contains("BLOCKED perc.broken.templateDef :"));
  }

  @Test
  void customAssembler_blockedWithUnregisteredAssemblerReason() throws Exception {
    // Issue #5864: the upgrade compile still parses legacy XML with a site-local custom
    // assembler, but the modern-only readback fails closed — the panel names the blocker.
    Path legacyDir = tempDir.resolve("custom-assembler");
    Files.createDirectories(legacyDir);
    String custom;
    try (var in =
        PSPageXmlMigrationDryRunTest.class.getResourceAsStream(
            "/pagexml/perc.base.plain.templateDef")) {
      assertNotNull(in);
      custom =
          new String(in.readAllBytes(), StandardCharsets.UTF_8)
              .replace(
                  "<assembler>Java/global/percussion/assembly/pageAssembler</assembler>",
                  "<assembler>Java/global/percussion/assembly/siteAssembler</assembler>");
    }
    Path file = legacyDir.resolve("perc.base.custom.templateDef");
    Files.writeString(file, custom, StandardCharsets.UTF_8);

    Entry entry = PSPageXmlMigrationDryRun.dryRunOne(file);

    assertEquals(Status.BLOCKED, entry.getStatus());
    assertNotNull(entry.getBlockReason());
    assertTrue(
        entry.getBlockReason().toLowerCase(java.util.Locale.ROOT).contains("assembler"),
        "block reason must name the unregistered assembler, was: " + entry.getBlockReason());
    assertNull(entry.getManifestId());
  }

  private static Entry byFile(Report report, String fileName) {
    for (Entry e : report.getEntries()) {
      if (fileName.equals(e.getFileName())) {
        return e;
      }
    }
    return null;
  }

  private Path stageFixtures(String dirName) throws Exception {
    Path legacyDir = tempDir.resolve(dirName);
    Files.createDirectories(legacyDir);
    for (String fixture : FIXTURES) {
      String resource = "/pagexml/" + fixture;
      try (var in = PSPageXmlMigrationDryRunTest.class.getResourceAsStream(resource)) {
        assertNotNull(in, "missing test resource: " + resource);
        Files.write(legacyDir.resolve(fixture), in.readAllBytes());
      }
    }
    return legacyDir;
  }

  /** Normalize line endings so assertions hold on Windows (\r\n) and Unix (\n). */
  private static String normalize(String value) {
    return value.replace("\r\n", "\n");
  }
}
