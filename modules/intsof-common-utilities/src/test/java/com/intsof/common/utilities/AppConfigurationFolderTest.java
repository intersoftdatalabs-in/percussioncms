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
package com.intsof.common.utilities;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppConfigurationFolderTest {

  @TempDir Path tempHome;

  private AppConfigurationFolder app;

  @BeforeEach
  void setUp() throws IOException {
    app = UserConfiguration.open(tempHome).createApplication("test-app");
  }

  @Test
  void listFilesEmptyInitially() throws IOException {
    assertTrue(app.listFiles().isEmpty());
  }

  @Test
  void listFilesReturnsSortedRegularFilesOnly() throws IOException {
    app.addFile("b.txt");
    app.addFile("a.txt");
    Files.createDirectory(app.getPath().resolve("subdir"));
    List<Path> files = app.listFiles();
    assertEquals(2, files.size());
    assertEquals("a.txt", files.get(0).getFileName().toString());
    assertEquals("b.txt", files.get(1).getFileName().toString());
  }

  @Test
  void addFileCreatesEmptyFileAndIsIdempotent() throws IOException {
    Path first = app.addFile("empty.properties");
    assertTrue(Files.isRegularFile(first));
    assertEquals(0, Files.size(first));
    Path second = app.addFile("empty.properties");
    assertEquals(first, second);
  }

  @Test
  void addFileWithBytesWritesContent() throws IOException {
    byte[] payload = "key=value".getBytes(StandardCharsets.UTF_8);
    Path file = app.addFile("settings.properties", payload);
    assertArrayEquals(payload, Files.readAllBytes(file));
  }

  @Test
  void addFileWithNullBytesWritesEmpty() throws IOException {
    Path file = app.addFile("null-bytes.txt", (byte[]) null);
    assertEquals(0, Files.size(file));
  }

  @Test
  void addFileWithStreamWritesContent() throws IOException {
    byte[] payload = "from-stream".getBytes(StandardCharsets.UTF_8);
    Path file = app.addFile("stream.txt", new ByteArrayInputStream(payload));
    assertArrayEquals(payload, Files.readAllBytes(file));
  }

  @Test
  void addFileWithNullStreamRejected() {
    assertThrows(
        NullPointerException.class, () -> app.addFile("x.txt", (java.io.InputStream) null));
  }

  @Test
  void getOptionalWhenMissingAndPresent() throws IOException {
    assertEquals(Optional.empty(), app.get("missing.txt"));
    Path created = app.addFile("present.txt", "hi".getBytes(StandardCharsets.UTF_8));
    Optional<Path> found = app.get("present.txt");
    assertTrue(found.isPresent());
    assertEquals(created, found.get());
  }

  @Test
  void getCreateIfMissingTrueCreatesEmptyFile() throws IOException {
    Path created = app.get("auto.properties", true);
    assertTrue(Files.isRegularFile(created));
    assertEquals(0, Files.size(created));
    Path again = app.get("auto.properties", true);
    assertEquals(created, again);
  }

  @Test
  void getCreateIfMissingFalseThrowsWhenAbsent() {
    assertThrows(NoSuchElementException.class, () -> app.get("nope.txt", false));
  }

  @Test
  void getCreateIfMissingFalseReturnsExisting() throws IOException {
    Path existing = app.addFile("exists.txt", "x".getBytes(StandardCharsets.UTF_8));
    assertEquals(existing, app.get("exists.txt", false));
  }

  @Test
  void removeFileReturnsTrueFalse() throws IOException {
    assertFalse(app.removeFile("gone.txt"));
    app.addFile("gone.txt");
    assertTrue(app.removeFile("gone.txt"));
    assertFalse(app.fileExists("gone.txt"));
    assertFalse(app.removeFile("gone.txt"));
  }

  @Test
  void fileExistsReflectsState() throws IOException {
    assertFalse(app.fileExists("x.cfg"));
    app.addFile("x.cfg");
    assertTrue(app.fileExists("x.cfg"));
  }

  @Test
  void rejectsInvalidFileNames() {
    assertThrows(IllegalArgumentException.class, () -> app.addFile(null));
    assertThrows(IllegalArgumentException.class, () -> app.addFile(""));
    assertThrows(IllegalArgumentException.class, () -> app.addFile(".."));
    assertThrows(IllegalArgumentException.class, () -> app.addFile("a/b"));
    assertThrows(IllegalArgumentException.class, () -> app.addFile("a\\b"));
    assertThrows(IllegalArgumentException.class, () -> app.get("a/b"));
    assertThrows(IllegalArgumentException.class, () -> app.removeFile(".."));
    assertThrows(IllegalArgumentException.class, () -> app.fileExists("CON"));
  }

  @Test
  void addFileOverwritesContent() throws IOException {
    app.addFile("rw.txt", "one".getBytes(StandardCharsets.UTF_8));
    app.addFile("rw.txt", "two".getBytes(StandardCharsets.UTF_8));
    assertEquals("two", Files.readString(app.get("rw.txt").orElseThrow()));
  }
}
