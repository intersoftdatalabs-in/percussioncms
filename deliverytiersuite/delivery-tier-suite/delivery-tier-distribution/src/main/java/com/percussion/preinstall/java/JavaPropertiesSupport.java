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

package com.percussion.preinstall.java;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * Pure helpers for the install-root {@code java.properties} file used by CMS/DTS runtime start,
 * stop, and service install scripts.
 *
 * <p>The contract (see {@code specs/991-system-java-home/contracts/java-properties-contract.md})
 * defines the file as a standard Java {@code .properties} file carrying at least {@code JAVA_HOME}
 * (absolute JRE/JDK home) and ideally {@code JAVA} (absolute launcher path). Writers must validate
 * that the selected home reports major version 21 before persisting and must merge with existing
 * keys so unrelated settings survive.
 */
public final class JavaPropertiesSupport {

  private JavaPropertiesSupport() {
    // Static-only utility.
  }

  /** Property key for the absolute Java home (JRE/JDK root). */
  public static final String KEY_JAVA_HOME = "JAVA_HOME";

  /** Property key for the absolute launcher ({@code bin/java} / {@code bin\java.exe}). */
  public static final String KEY_JAVA = "JAVA";

  /** Marker key written/updated by this feature for round-trip diagnostics. */
  public static final String KEY_WRITTEN_BY = "#written-by";

  /** Marker value stamped into {@link #KEY_WRITTEN_BY} by this feature for diagnostics. */
  public static final String WRITTEN_BY_VALUE = "perc-preinstall-java-home";

  /**
   * Loads properties from {@code <installRoot>/java.properties} if present. Returns an empty map
   * when the file does not exist so callers can treat absent-file as "no product config". Malformed
   * entries are surfaced via {@link JavaLoadResult} to allow callers to log without throwing.
   *
   * @param installRoot the install directory; must not be {@code null}
   * @return the load result carrying the file location, parsed keys, presence flag, and any I/O
   *     error encountered; never {@code null}
   * @throws IOException when the file path is invalid (defensive — most errors surface via the
   *     returned {@link JavaLoadResult#error()} instead)
   */
  public static JavaLoadResult load(Path installRoot) throws IOException {
    if (installRoot == null) {
      throw new IllegalArgumentException("installRoot must not be null");
    }
    Path file = installRoot.resolve("java.properties");
    if (!Files.exists(file)) {
      return new JavaLoadResult(file, new LinkedHashMap<>(), false, null);
    }
    Properties raw = new Properties();
    try (InputStream in = Files.newInputStream(file)) {
      raw.load(in);
    } catch (IOException io) {
      return new JavaLoadResult(file, Collections.emptyMap(), true, io);
    }
    Map<String, String> merged = new LinkedHashMap<>();
    for (String name : raw.stringPropertyNames()) {
      merged.put(name, raw.getProperty(name));
    }
    return new JavaLoadResult(file, merged, true, null);
  }

  /**
   * Returns the persisted {@code JAVA_HOME} value as a string, or {@code null} when not set or
   * blank. Callers must validate the path before treating it as resolved.
   *
   * @param installRoot the install directory; must not be {@code null}
   * @return the trimmed {@code JAVA_HOME} value, or {@code null} when not set or blank
   * @throws IOException when the underlying {@link #load(Path)} call fails to read the file
   */
  public static String readJavaHome(Path installRoot) throws IOException {
    return readString(installRoot, KEY_JAVA_HOME);
  }

  /**
   * Returns the persisted {@code JAVA} (launcher) value, or {@code null} when not set.
   *
   * @param installRoot the install directory; must not be {@code null}
   * @return the trimmed {@code JAVA} value, or {@code null} when not set or blank
   * @throws IOException when the underlying {@link #load(Path)} call fails to read the file
   */
  public static String readJava(Path installRoot) throws IOException {
    return readString(installRoot, KEY_JAVA);
  }

  private static String readString(Path installRoot, String key) throws IOException {
    JavaLoadResult loaded = load(installRoot);
    String value = loaded.properties().get(key);
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  /**
   * Writes the supplied {@code JAVA_HOME} and (when derivable) {@code JAVA} values to {@code
   * <installRoot>/java.properties}, preserving any unrelated keys already on disk. Parent
   * directories are created as needed. Relative paths supplied by the caller are rejected to avoid
   * runtime ambiguity; absolute paths only.
   *
   * @param installRoot the install directory; must not be {@code null}
   * @param javaHome absolute path to the selected Java home; must be a non-empty absolute path
   * @param javaLauncher absolute path to the launcher; may be {@code null} or empty, in which case
   *     the launcher is derived from {@code javaHome} using the host-platform bin suffix
   * @throws IOException when reading or writing the {@code java.properties} file fails
   */
  public static void write(Path installRoot, String javaHome, String javaLauncher)
      throws IOException {
    if (installRoot == null) {
      throw new IllegalArgumentException("installRoot must not be null");
    }
    if (javaHome == null || javaHome.trim().isEmpty()) {
      throw new IllegalArgumentException("javaHome must be a non-empty absolute path");
    }
    if (!Path.of(javaHome).isAbsolute()) {
      throw new IllegalArgumentException("javaHome must be absolute: " + javaHome);
    }
    if (javaLauncher != null && !javaLauncher.isEmpty() && !Path.of(javaLauncher).isAbsolute()) {
      throw new IllegalArgumentException(
          "javaLauncher must be absolute when provided: " + javaLauncher);
    }

    JavaLoadResult existing = load(installRoot);
    Map<String, String> merged = new LinkedHashMap<>(existing.properties());
    merged.put(KEY_JAVA_HOME, javaHome);
    merged.put(
        KEY_JAVA,
        javaLauncher != null
            ? javaLauncher
            : javaHome + inferBinSuffix() + "java" + inferExeSuffix());
    merged.put(KEY_WRITTEN_BY, WRITTEN_BY_VALUE);

    Files.createDirectories(installRoot);
    Path target = installRoot.resolve("java.properties");
    Properties out = new Properties();
    for (Map.Entry<String, String> e : merged.entrySet()) {
      out.setProperty(e.getKey(), e.getValue());
    }
    try (java.io.OutputStream os = Files.newOutputStream(target)) {
      out.store(os, "Written by Percussion preinstall — Java for CMS/DTS runtime");
    }
  }

  /**
   * Returns a map consisting of existing properties in the install-root {@code java.properties}
   * file, merged with {@code additional}. Existing values take precedence — only keys absent on
   * disk are added from {@code additional}. The file on disk is not modified by this call.
   *
   * @param installRoot the install directory; must not be {@code null}
   * @param additional keys to add when absent on disk; {@code null} is treated as empty
   * @return an immutable map of merged properties; never {@code null}
   * @throws IOException when reading the {@code java.properties} file fails
   */
  public static Map<String, String> mergePreserving(
      Path installRoot, Map<String, String> additional) throws IOException {
    JavaLoadResult existing = load(installRoot);
    Map<String, String> merged = new LinkedHashMap<>(existing.properties());
    if (additional != null) {
      for (Map.Entry<String, String> e : additional.entrySet()) {
        merged.putIfAbsent(e.getKey(), e.getValue());
      }
    }
    return merged;
  }

  /**
   * Indicates the platform's {@code bin} directory separator. For portable test expectations we
   * read {@link java.io.File#separator} but fall back to {@code /} (the URL/zip separator) when a
   * normal separator is unavailable (rare test envs).
   */
  static String inferBinSuffix() {
    String sep = java.io.File.separator;
    return sep == null ? "/" : sep;
  }

  /** Returns {@code .exe} on Windows, empty otherwise. */
  static String inferExeSuffix() {
    String os = System.getProperty("os.name", "");
    return os.toLowerCase(Locale.ROOT).contains("win") ? ".exe" : "";
  }

  /**
   * Result of loading {@code java.properties}.
   *
   * @param location the resolved file location under the install root
   * @param properties the parsed key/value pairs; empty when the file is absent
   * @param present {@code true} when the {@code java.properties} file exists on disk
   * @param error any I/O error encountered during the load (typically {@code null})
   */
  public record JavaLoadResult(
      Path location, Map<String, String> properties, boolean present, IOException error) {

    /**
     * Returns the keys present in the loaded properties, useful for diagnostics.
     *
     * @return a mutable list of keys (insertion order); never {@code null}
     */
    public List<String> keysForDebug() {
      return new ArrayList<>(properties.keySet());
    }

    /**
     * Pretty-prints the resulting properties without values, suitable for logs.
     *
     * @return a single-line summary; never {@code null}
     */
    public String summary() {
      return "java.properties " + (present ? "loaded" : "absent") + " keys=" + keysForDebug();
    }

    /**
     * Renders a path for log output, mapping {@code null} to a sentinel string.
     *
     * @param path the path to render; may be {@code null}
     * @return the path's string form, or {@code "<null>"} when {@code path} is {@code null}
     */
    public static String forLogPath(Path path) {
      return path == null ? "<null>" : path.toString();
    }
  }

  /** Utility for tests to render round-trip content from bytes. */
  static String normalize(String content) {
    return content.replace("\r\n", "\n");
  }

  /** Utility for tests; never used at runtime. */
  static byte[] utf8(String s) {
    return s.getBytes(StandardCharsets.UTF_8);
  }
}
