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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Regression for GH-510 / v8.1.7 PR #515: extracted {@code .sh} entries must be marked executable.
 */
class MainExtractExecutableTest {

  @TempDir Path tempDir;

  @Test
  void extractedShellScriptsAreExecutable() throws Exception {
    assumeTrue(
        !System.getProperty("os.name", "").toLowerCase().contains("win"),
        "executable bit not meaningful on Windows");

    Path zip = tempDir.resolve("dist.zip");
    try (OutputStream fos = Files.newOutputStream(zip);
        ZipOutputStream zos = new ZipOutputStream(fos)) {
      zos.putNextEntry(new ZipEntry("distribution/"));
      zos.closeEntry();
      zos.putNextEntry(new ZipEntry("distribution/bin/hello.sh"));
      zos.write("#!/bin/sh\necho hi\n".getBytes(StandardCharsets.UTF_8));
      zos.closeEntry();
      zos.putNextEntry(new ZipEntry("distribution/readme.txt"));
      zos.write("text\n".getBytes(StandardCharsets.UTF_8));
      zos.closeEntry();
    }

    Path dest = tempDir.resolve("out");
    Files.createDirectories(dest);
    Main.extractArchive(zip, dest, "distribution");

    Path script = dest.resolve("bin/hello.sh");
    assertTrue(Files.isRegularFile(script), "script extracted");
    assertTrue(Files.isExecutable(script), "script must be executable after extract");
  }
}
