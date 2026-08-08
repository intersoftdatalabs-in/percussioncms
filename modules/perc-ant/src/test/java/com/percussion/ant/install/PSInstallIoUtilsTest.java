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
package com.percussion.ant.install;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for {@link PSInstallIoUtils} install-time file helpers. */
@Tag("UnitTest")
public class PSInstallIoUtilsTest {

  @TempDir Path tempDir;

  @Test
  public void getFileContentReadsUtf8() throws Exception {
    Path file = tempDir.resolve("sample.txt");
    Files.writeString(file, "hello-utf8-✓", StandardCharsets.UTF_8);

    assertEquals("hello-utf8-✓", PSInstallIoUtils.getFileContent(file.toFile()));
  }

  @Test
  public void copyFileOverwritesDestination() throws Exception {
    Path source = tempDir.resolve("src.bin");
    Path dest = tempDir.resolve("dest.bin");
    Files.writeString(source, "payload", StandardCharsets.UTF_8);
    Files.writeString(dest, "old", StandardCharsets.UTF_8);

    PSInstallIoUtils.copyFile(source.toFile(), dest.toFile());

    assertEquals("payload", Files.readString(dest, StandardCharsets.UTF_8));
  }

  @Test
  public void writeStreamCopiesReaderToWriter() throws Exception {
    StringWriter out = new StringWriter();
    PSInstallIoUtils.writeStream(new StringReader("stream-data"), out);
    assertEquals("stream-data", out.toString());
  }

  @Test
  public void copyToDirCopiesFileAndDirectory() throws Exception {
    Path srcFile = tempDir.resolve("item.txt");
    Files.writeString(srcFile, "file-body", StandardCharsets.UTF_8);
    Path srcDir = tempDir.resolve("nested");
    Files.createDirectories(srcDir);
    Files.writeString(srcDir.resolve("child.txt"), "child", StandardCharsets.UTF_8);

    Path target = tempDir.resolve("target");
    Files.createDirectories(target);

    PSInstallIoUtils.copyToDir(srcFile.toFile(), target.toFile());
    PSInstallIoUtils.copyToDir(srcDir.toFile(), target.toFile());

    assertEquals("file-body", Files.readString(target.resolve("item.txt"), StandardCharsets.UTF_8));
    assertEquals(
        "child", Files.readString(target.resolve("nested").resolve("child.txt"), StandardCharsets.UTF_8));
  }

  @Test
  public void createBackupFileUsesSequentialSuffixes() throws Exception {
    Path original = tempDir.resolve("config.properties");
    Files.writeString(original, "v1", StandardCharsets.UTF_8);

    File backup0 = PSInstallIoUtils.createBackupFile(original.toFile());
    assertEquals("config.000", backup0.getName());
    assertEquals("v1", Files.readString(backup0.toPath(), StandardCharsets.UTF_8));

    File backup1 = PSInstallIoUtils.createBackupFile(original.toFile());
    assertEquals("config.001", backup1.getName());
    assertTrue(backup1.exists());
    assertFalse(backup0.getName().equals(backup1.getName()));
  }

  @Test
  public void createTempFileCopiesContents() throws Exception {
    Path original = tempDir.resolve("template.xml");
    Files.writeString(original, "<root/>", StandardCharsets.UTF_8);

    File temp = PSInstallIoUtils.createTempFile(original.toFile());
    temp.deleteOnExit();
    assertTrue(temp.exists());
    assertEquals("<root/>", Files.readString(temp.toPath(), StandardCharsets.UTF_8));
  }
}
