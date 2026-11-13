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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.packages.pagexml.PSPageXmlMigrationDryRun.Report;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Dry-run report persistence for the legacy definition-XML upgrade path (issue #6266 / parent
 * #2626).
 *
 * <p>The dry-run itself writes nothing beside the legacy inputs; {@link Report#writeTo(Path)}
 * persists the Markdown + text panels under an operator-chosen directory so upgrade runs can
 * be archived and audited. All file joins use NIO {@link Path#resolve} (no hardcoded
 * separators) and assertions normalize line endings for Windows / Unix.
 */
class PSPageXmlMigrationDryRunPersistenceTest {

  private static final List<String> FIXTURES = List.of(
      "perc.base.plain.templateDef",
      "perc.base.headerFooter.templateDef",
      "perc.fileBinary.templateDef",
      "perc.imageMainBinary.templateDef",
      "perc.imageThumbBinary.templateDef");

  @TempDir Path tempDir;

  @Test
  void writeTo_writesMarkdownAndTextPair() throws Exception {
    Path legacyDir = stageFixtures("legacy");
    Report report = PSPageXmlMigrationDryRun.dryRun(legacyDir);
    Path outDir = tempDir.resolve("reports");

    List<Path> written = report.writeTo(outDir);

    assertEquals(
        List.of(outDir.resolve(Report.MARKDOWN_FILE_NAME), outDir.resolve(Report.TEXT_FILE_NAME)),
        written);
    for (Path file : written) {
      assertTrue(Files.isRegularFile(file), "report pair file missing: " + file);
    }
    assertEquals(normalize(report.toMarkdown()), normalize(Files.readString(written.get(0))));
    assertEquals(normalize(report.toText()), normalize(Files.readString(written.get(1))));
  }

  @Test
  void writeTo_createsMissingNestedOutputDir() throws Exception {
    Path legacyDir = stageFixtures("legacy-nested");
    Report report = PSPageXmlMigrationDryRun.dryRun(legacyDir);
    Path outDir = tempDir.resolve("a").resolve("b").resolve("reports");
    assertFalse(Files.exists(outDir));

    List<Path> written = report.writeTo(outDir);

    assertTrue(Files.isDirectory(outDir));
    assertEquals(2, written.size());
    for (Path file : written) {
      assertTrue(Files.isRegularFile(file));
    }
  }

  @Test
  void writeTo_leavesLegacyInputsUntouched() throws Exception {
    Path legacyDir = stageFixtures("legacy-untouched");
    Report report = PSPageXmlMigrationDryRun.dryRun(legacyDir);

    report.writeTo(tempDir.resolve("elsewhere"));

    try (var stream = Files.list(legacyDir)) {
      assertEquals(
          FIXTURES.size(), stream.filter(Files::isRegularFile).count(),
          "persistence must not create files beside the legacy inputs");
    }
    assertFalse(Files.exists(legacyDir.resolve(Report.MARKDOWN_FILE_NAME)));
    assertFalse(Files.exists(legacyDir.resolve(Report.TEXT_FILE_NAME)));
  }

  @Test
  void writeTo_overwritesStalePairWithLatestReport() throws Exception {
    Path legacyDir = stageFixtures("legacy-refresh");
    Path outDir = tempDir.resolve("refresh");
    PSPageXmlMigrationDryRun.dryRun(legacyDir).writeTo(outDir);
    assertFalse(normalize(Files.readString(outDir.resolve(Report.MARKDOWN_FILE_NAME)))
        .contains("BLOCKED"));

    Files.writeString(
        legacyDir.resolve("perc.broken.templateDef"), "<broken", StandardCharsets.UTF_8);
    Report refreshed = PSPageXmlMigrationDryRun.dryRun(legacyDir);

    refreshed.writeTo(outDir);

    String markdown = normalize(Files.readString(outDir.resolve(Report.MARKDOWN_FILE_NAME)));
    assertTrue(markdown.contains("| perc.broken.templateDef | BLOCKED |"),
        "re-persisted panel must carry the new BLOCKED row");
    String text = normalize(Files.readString(outDir.resolve(Report.TEXT_FILE_NAME)));
    assertTrue(text.contains("BLOCKED perc.broken.templateDef :"));
  }

  @Test
  void writeTo_singleFileOutputsMatchPairContents() throws Exception {
    Path legacyDir = stageFixtures("legacy-single");
    Report report = PSPageXmlMigrationDryRun.dryRun(legacyDir);
    Path markdownFile = tempDir.resolve("one").resolve("panel.md");
    Path textFile = tempDir.resolve("two").resolve("panel.txt");

    report.writeMarkdown(markdownFile);
    report.writeText(textFile);

    assertEquals(normalize(report.toMarkdown()), normalize(Files.readString(markdownFile)));
    assertEquals(normalize(report.toText()), normalize(Files.readString(textFile)));
  }

  @Test
  void writeTo_nullTargetsRejected() {
    Report report = PSPageXmlMigrationDryRun.dryRun(List.of());

    assertThrows(NullPointerException.class, () -> report.writeTo(null));
    assertThrows(NullPointerException.class, () -> report.writeMarkdown(null));
    assertThrows(NullPointerException.class, () -> report.writeText(null));
  }

  private Path stageFixtures(String dirName) throws Exception {
    Path legacyDir = tempDir.resolve(dirName);
    Files.createDirectories(legacyDir);
    for (String fixture : FIXTURES) {
      String resource = "/pagexml/" + fixture;
      try (var in = PSPageXmlMigrationDryRunPersistenceTest.class.getResourceAsStream(resource)) {
        if (in == null) {
          throw new IllegalStateException("missing test resource: " + resource);
        }
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
