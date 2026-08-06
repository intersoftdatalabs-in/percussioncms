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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.security.validation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
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

  /** Default filename of the per-server allow-list file under {@code rxconfig/Server}. */
  public static final String ALLOWED_FILE_NAME = "allowedUrls.properties";

  /** Default filename of the per-server block-list file under {@code rxconfig/Server}. */
  public static final String BLOCKED_FILE_NAME = "blockedUrls.properties";

  /** Directory beneath the install root that holds the URL allow/block list files. */
  public static final String SERVER_RELATIVE_DIR = "rxconfig/Server";

  /** Classpath resource path for default allow template. */
  public static final String DEFAULT_ALLOWED_RESOURCE =
      "com/percussion/security/validation/allowedUrls.properties";

  /** Classpath resource path for default block template. */
  public static final String DEFAULT_BLOCKED_RESOURCE =
      "com/percussion/security/validation/blockedUrls.properties";

  /** No-op utility constructor. */
  private URLListFileLoader() {}

  /**
   * Parses active URL patterns from a list file. Comments ({@code #}), blank lines, and lone {@code
   * *} are ignored.
   *
   * @param file the URL list file to parse, may be {@code null}.
   * @return an immutable list of pattern strings, never {@code null} but possibly empty if the file
   *     is {@code null}, missing, or contains no active patterns.
   * @throws IOException if the file cannot be read.
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
   * @param target the file to seed; if {@code null} or already existing, the method is a no-op
   *     (returning {@code false} in the latter case).
   * @param classpathResource the classpath-relative path of the default template resource, must not
   *     be {@code null}.
   * @return {@code true} if a new file was created; {@code false} if the file already existed or
   *     either argument was {@code null}.
   * @throws IOException if the parent directories cannot be created or the resource cannot be
   *     copied.
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

  /**
   * Seeds both allow and block files under {@code serverConfigDir} when missing. No-op if the
   * supplied directory is {@code null}.
   *
   * @param serverConfigDir the directory under which to create the default allow / block files; may
   *     be {@code null} (in which case the call is a no-op).
   * @throws IOException if any of the seed copies fails.
   */
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
   *
   * @return the resolved server-config directory, or {@code null} if the {@code rxdeploydir} system
   *     property is unset / blank.
   */
  public static Path resolveServerConfigDirFromRxDeployDir() {
    String rx = System.getProperty("rxdeploydir");
    if (rx == null || rx.isBlank()) {
      return null;
    }
    return Path.of(rx.trim(), "rxconfig", "Server");
  }

  /**
   * Loads patterns after optional seed; returns empty list if file still absent.
   *
   * @param file the URL list file to load; may be {@code null}.
   * @param seedResource classpath resource to seed the file with if it does not yet exist.
   * @return an immutable list of pattern strings, never {@code null} but possibly empty.
   * @throws IOException if the seed or the read fails.
   */
  public static List<String> loadPatternsAfterSeed(Path file, String seedResource)
      throws IOException {
    if (file == null) {
      return Collections.emptyList();
    }
    seedIfMissing(file, seedResource);
    return parsePatterns(file);
  }

  /**
   * Reads classpath resource as UTF-8 string (for tests).
   *
   * @param resource the classpath resource to read, must not be {@code null}.
   * @return the resource contents decoded as UTF-8.
   * @throws IOException if the resource cannot be found or read.
   */
  public static String readClasspathResource(String resource) throws IOException {
    try (InputStream in = URLListFileLoader.class.getClassLoader().getResourceAsStream(resource)) {
      if (in == null) {
        throw new IOException("Missing classpath resource: " + resource);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
