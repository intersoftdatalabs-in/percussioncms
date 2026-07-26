/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

package com.percussion.preinstall;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Behavioral tests for {@link MainDTSPreInstall#extractArchive(Path, Path, String)}.
 *
 * <p>Covers GH-1180: extract must use {@code PathValidation} on the runtime classpath (shaded into
 * the installer jar) and reject ZipSlip entries without aborting the whole extraction.
 */
class MainDTSPreInstallExtractArchiveTest {

  private static final String PREFIX = "distribution";

  @TempDir Path tempDir;

  @Test
  void extractArchive_writesSafeEntriesUnderPrefix() throws Exception {
    Path archive = tempDir.resolve("safe.zip");
    // Flat file only: extractArchive does not create parent dirs for nested
    // file entries unless the zip also contains directory ZipEntry records
    // (cargo/rootFiles packaging includes those). GH-1180 scope is classpath.
    writeZip(archive, entry(PREFIX + "/ok.txt", "hello"));

    Path dest = tempDir.resolve("out");
    MainDTSPreInstall.extractArchive(archive, dest, PREFIX);

    assertTrue(Files.isRegularFile(dest.resolve("ok.txt")));
    assertTrue(
        new String(Files.readAllBytes(dest.resolve("ok.txt")), StandardCharsets.UTF_8)
            .contains("hello"));
  }

  @Test
  void extractArchive_skipsEntriesOutsidePrefix() throws Exception {
    Path archive = tempDir.resolve("prefix.zip");
    writeZip(archive, entry("other/ignore.txt", "nope"), entry(PREFIX + "/keep.txt", "yes"));

    Path dest = tempDir.resolve("out");
    MainDTSPreInstall.extractArchive(archive, dest, PREFIX);

    assertTrue(Files.isRegularFile(dest.resolve("keep.txt")));
    assertFalse(Files.exists(dest.resolve("ignore.txt")));
    assertFalse(Files.exists(dest.resolve("other")));
  }

  @Test
  void extractArchive_rejectsZipSlipAndContinues() throws Exception {
    Path archive = tempDir.resolve("slip.zip");
    // Classic ZipSlip: entry name walks above the extract root
    writeZip(
        archive,
        entry(PREFIX + "/good.txt", "safe"),
        entry(PREFIX + "/../../zipslip-outside.txt", "evil"));

    Path dest = tempDir.resolve("extract");
    Files.createDirectories(dest);
    Path siblingProbe = tempDir.resolve("zipslip-outside.txt");

    MainDTSPreInstall.extractArchive(archive, dest, PREFIX);

    assertTrue(Files.isRegularFile(dest.resolve("good.txt")), "safe entry must extract");
    assertFalse(
        Files.exists(siblingProbe),
        "ZipSlip entry must not be written outside the extract directory");
  }

  @Test
  void extractArchive_rejectsAbsoluteStyleUserPathComponents() throws Exception {
    Path archive = tempDir.resolve("abs.zip");
    // PathValidation.looksAbsolute rejects leading / or \ in the relative component
    writeZip(archive, entry(PREFIX + "/good2.txt", "ok"), entry(PREFIX + "/../escape.txt", "bad"));

    Path dest = tempDir.resolve("extract2");
    MainDTSPreInstall.extractArchive(archive, dest, PREFIX);

    assertTrue(Files.isRegularFile(dest.resolve("good2.txt")));
    assertFalse(Files.exists(tempDir.resolve("escape.txt")));
  }

  private static ZipEntrySpec entry(String name, String content) {
    return new ZipEntrySpec(name, content.getBytes(StandardCharsets.UTF_8));
  }

  private static void writeZip(Path zipFile, ZipEntrySpec... entries) throws IOException {
    try (OutputStream fos = Files.newOutputStream(zipFile);
        ZipOutputStream zos = new ZipOutputStream(fos)) {
      for (ZipEntrySpec e : entries) {
        ZipEntry ze = new ZipEntry(e.name);
        zos.putNextEntry(ze);
        zos.write(e.bytes);
        zos.closeEntry();
      }
    }
  }

  private static final class ZipEntrySpec {
    final String name;
    final byte[] bytes;

    ZipEntrySpec(String name, byte[] bytes) {
      this.name = name;
      this.bytes = bytes;
    }
  }
}
