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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.security.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("URLListFileLoader")
class URLListFileLoaderTest {

  @TempDir Path tempDir;

  @Test
  @DisplayName("parse skips comments blanks and lone star")
  void testParse() throws Exception {
    Path f = tempDir.resolve("list.properties");
    Files.writeString(
        f,
        """
        # comment
        https://a.example.com/*

        *
        http://b.example.com/x
        """,
        StandardCharsets.UTF_8);
    List<String> patterns = URLListFileLoader.parsePatterns(f);
    assertEquals(2, patterns.size());
    assertEquals("https://a.example.com/*", patterns.get(0));
    assertEquals("http://b.example.com/x", patterns.get(1));
  }

  @Test
  @DisplayName("seed creates missing file and does not overwrite")
  void testSeed() throws Exception {
    Path server = tempDir.resolve("rxconfig").resolve("Server");
    Path allowed = server.resolve(URLListFileLoader.ALLOWED_FILE_NAME);
    assertTrue(
        URLListFileLoader.seedIfMissing(allowed, URLListFileLoader.DEFAULT_ALLOWED_RESOURCE));
    assertTrue(Files.isRegularFile(allowed));
    String first = Files.readString(allowed, StandardCharsets.UTF_8);
    assertTrue(first.contains("allowedUrls") || first.contains("ADDITIVE") || first.contains("#"));

    Files.writeString(allowed, "https://custom.example.com/*\n", StandardCharsets.UTF_8);
    assertFalse(
        URLListFileLoader.seedIfMissing(allowed, URLListFileLoader.DEFAULT_ALLOWED_RESOURCE));
    assertEquals(
        "https://custom.example.com/*\n", Files.readString(allowed, StandardCharsets.UTF_8));
  }

  @Test
  @DisplayName("seedServerConfigDir creates both files; partial missing only one")
  void testSeedBothAndPartial() throws Exception {
    Path server = tempDir.resolve("Server");
    URLListFileLoader.seedServerConfigDir(server);
    assertTrue(Files.isRegularFile(server.resolve(URLListFileLoader.ALLOWED_FILE_NAME)));
    assertTrue(Files.isRegularFile(server.resolve(URLListFileLoader.BLOCKED_FILE_NAME)));

    Path allowed = server.resolve(URLListFileLoader.ALLOWED_FILE_NAME);
    Files.writeString(allowed, "# keep me\n", StandardCharsets.UTF_8);
    Files.delete(server.resolve(URLListFileLoader.BLOCKED_FILE_NAME));
    URLListFileLoader.seedServerConfigDir(server);
    assertEquals("# keep me\n", Files.readString(allowed, StandardCharsets.UTF_8));
    assertTrue(Files.isRegularFile(server.resolve(URLListFileLoader.BLOCKED_FILE_NAME)));
  }

  @Test
  @DisplayName("default allow template has no active patterns")
  void testDefaultAllowInactive() throws Exception {
    String body =
        URLListFileLoader.readClasspathResource(URLListFileLoader.DEFAULT_ALLOWED_RESOURCE);
    Path f = tempDir.resolve("a.properties");
    Files.writeString(f, body, StandardCharsets.UTF_8);
    assertTrue(URLListFileLoader.parsePatterns(f).isEmpty());
  }

  @Test
  @DisplayName("default block template has active metadata patterns")
  void testDefaultBlockActive() throws Exception {
    String body =
        URLListFileLoader.readClasspathResource(URLListFileLoader.DEFAULT_BLOCKED_RESOURCE);
    Path f = tempDir.resolve("b.properties");
    Files.writeString(f, body, StandardCharsets.UTF_8);
    List<String> patterns = URLListFileLoader.parsePatterns(f);
    assertFalse(patterns.isEmpty());
    assertTrue(patterns.stream().anyMatch(p -> p.contains("169.254.169.254")));
  }
}
