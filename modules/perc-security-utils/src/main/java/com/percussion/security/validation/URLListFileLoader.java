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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Loads and optionally seeds allow/block URL list property files (issue #1205). Create-if-absent;
 * never overwrites existing files.
 */
public final class URLListFileLoader {

  private static final Logger log = LogManager.getLogger(URLListFileLoader.class);

  public static final String ALLOWED_FILE_NAME = "allowedUrls.properties";
  public static final String BLOCKED_FILE_NAME = "blockedUrls.properties";
  public static final String SERVER_RELATIVE_DIR = "rxconfig/Server";

  /** Classpath resource path for default allow template. */
  public static final String DEFAULT_ALLOWED_RESOURCE =
      "com/percussion/security/validation/allowedUrls.properties";

  /** Classpath resource path for default block template. */
  public static final String DEFAULT_BLOCKED_RESOURCE =
      "com/percussion/security/validation/blockedUrls.properties";

  private URLListFileLoader() {}

  /**
   * Parses active URL patterns from a list file. Comments ({@code #}), blank lines, and lone {@code
   * *} are ignored.
   */
  public static List<String> parsePatterns(Path file) throws IOException {
    if (file == null || !Files.isRegularFile(file)) {
      return Collections.emptyList();
    }
    try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
      return parsePatterns(reader);
    }
  }

  static List<String> parsePatterns(BufferedReader reader) throws IOException {
    List<String> patterns = new ArrayList<>();
    String line;
    while ((line = reader.readLine()) != null) {
      String trimmed = line.trim();
      if (trimmed.isEmpty() || trimmed.startsWith("#")) {
        continue;
      }
      if ("*".equals(trimmed)) {
        log.warn("Ignoring lone '*' URL list pattern (not allowed)");
        continue;
      }
      patterns.add(trimmed);
    }
    return Collections.unmodifiableList(patterns);
  }

  /**
   * If {@code target} does not exist, copies the classpath default resource to it. Parent
   * directories are created as needed. Existing files are never modified.
   *
   * @return true if a new file was created
   */
  public static boolean seedIfMissing(Path target, String classpathResource) throws IOException {
    if (target == null || classpathResource == null) {
      throw new IllegalArgumentException("target and classpathResource required");
    }
    if (Files.exists(target)) {
      return false;
    }
    Path parent = target.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    try (InputStream in =
        URLListFileLoader.class.getClassLoader().getResourceAsStream(classpathResource)) {
      if (in == null) {
        throw new IOException("Missing classpath resource: " + classpathResource);
      }
      Files.copy(in, target);
      log.info("Created default URL list file: {}", target.toAbsolutePath());
      return true;
    }
  }

  /** Seeds both allow and block files under {@code serverConfigDir} when missing. */
  public static void seedServerConfigDir(Path serverConfigDir) throws IOException {
    if (serverConfigDir == null) {
      return;
    }
    seedIfMissing(serverConfigDir.resolve(ALLOWED_FILE_NAME), DEFAULT_ALLOWED_RESOURCE);
    seedIfMissing(serverConfigDir.resolve(BLOCKED_FILE_NAME), DEFAULT_BLOCKED_RESOURCE);
  }

  /**
   * Resolves install-root {@code rxconfig/Server} from {@code rxdeploydir} system property, or
   * {@code null} if unset/blank.
   */
  public static Path resolveServerConfigDirFromRxDeployDir() {
    String rx = System.getProperty("rxdeploydir");
    if (rx == null || rx.isBlank()) {
      return null;
    }
    return Path.of(rx.trim(), "rxconfig", "Server");
  }

  /** Loads patterns after optional seed; returns empty list if file still absent. */
  public static List<String> loadPatternsAfterSeed(Path file, String seedResource)
      throws IOException {
    if (file == null) {
      return Collections.emptyList();
    }
    seedIfMissing(file, seedResource);
    return parsePatterns(file);
  }

  /** Reads classpath resource as UTF-8 string (for tests). */
  public static String readClasspathResource(String resource) throws IOException {
    try (InputStream in =
        URLListFileLoader.class.getClassLoader().getResourceAsStream(resource)) {
      if (in == null) {
        throw new IOException("Missing classpath resource: " + resource);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
