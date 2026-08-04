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

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Configuration for URL validation (SSRF / CWE-918). Hold allow and block URL globs loaded from
 * install-root files under {@code rxconfig/Server/} (issue #1205). JVM system properties for
 * allow-hosts/ports/ranges are not used.
 */
public class URLValidationConfig {

  private static final Logger log = LogManager.getLogger(URLValidationConfig.class);

  private static URLValidationConfig INSTANCE;

  private final List<String> allowPatterns;
  private final List<String> blockPatterns;

  /** Empty lists (baseline-only + hard deny). Used by tests. */
  public URLValidationConfig() {
    this(Collections.emptyList(), Collections.emptyList());
  }

  /**
   * Constructs a configuration holding the supplied allow and block patterns. {@code null}
   * arguments are treated as empty lists; the lists are defensively copied.
   *
   * @param allowPatterns additive allow globs (may be null)
   * @param blockPatterns block globs (may be null)
   */
  public URLValidationConfig(List<String> allowPatterns, List<String> blockPatterns) {
    this.allowPatterns =
        allowPatterns != null
            ? Collections.unmodifiableList(new ArrayList<>(allowPatterns))
            : Collections.emptyList();
    this.blockPatterns =
        blockPatterns != null
            ? Collections.unmodifiableList(new ArrayList<>(blockPatterns))
            : Collections.emptyList();
  }

  /**
   * Loads from explicit file paths (tests / custom wiring). Seeds missing files from classpath
   * defaults when parent directories are writable.
   *
   * @param allowedFile the allow-list file path, may be {@code null} (in which case the allow list
   *     is left empty).
   * @param blockedFile the block-list file path, may be {@code null} (in which case the block list
   *     is left empty).
   * @return a configuration whose patterns were loaded (and possibly seeded) from the supplied
   *     files.
   */
  public static URLValidationConfig fromFiles(Path allowedFile, Path blockedFile) {
    List<String> allow = Collections.emptyList();
    List<String> block = Collections.emptyList();
    try {
      if (allowedFile != null) {
        allow =
            URLListFileLoader.loadPatternsAfterSeed(
                allowedFile, URLListFileLoader.DEFAULT_ALLOWED_RESOURCE);
      }
      if (blockedFile != null) {
        block =
            URLListFileLoader.loadPatternsAfterSeed(
                blockedFile, URLListFileLoader.DEFAULT_BLOCKED_RESOURCE);
      }
    } catch (IOException e) {
      log.warn("Failed to load URL list files: {}", e.toString());
      log.debug(e);
    }
    return new URLValidationConfig(allow, block);
  }

  /**
   * Loads from {@code ${rxdeploydir}/rxconfig/Server/} when {@code rxdeploydir} is set; otherwise
   * empty lists (baseline only until setDefault is called).
   *
   * @return the configuration loaded from the install root, or an empty configuration when {@code
   *     rxdeploydir} is unset.
   */
  public static URLValidationConfig loadFromInstallRoot() {
    Path serverDir = URLListFileLoader.resolveServerConfigDirFromRxDeployDir();
    if (serverDir == null) {
      log.debug("rxdeploydir not set; URL allow/block lists empty until configured");
      return new URLValidationConfig();
    }
    try {
      URLListFileLoader.seedServerConfigDir(serverDir);
    } catch (IOException e) {
      log.warn("Could not seed URL list files under {}: {}", serverDir, e.toString());
      log.debug(e);
    }
    return fromFiles(
        serverDir.resolve(URLListFileLoader.ALLOWED_FILE_NAME),
        serverDir.resolve(URLListFileLoader.BLOCKED_FILE_NAME));
  }

  /**
   * Lazily initializes and returns the process-wide default configuration, loaded from the install
   * root on first access.
   *
   * @return the cached default configuration; never {@code null}.
   */
  public static synchronized URLValidationConfig getDefault() {
    if (INSTANCE == null) {
      INSTANCE = loadFromInstallRoot();
    }
    return INSTANCE;
  }

  /**
   * Replaces the process-wide default configuration with the supplied value.
   *
   * @param config the configuration to install as the new default; may be {@code null}.
   */
  public static synchronized void setDefault(URLValidationConfig config) {
    INSTANCE = config;
  }

  /** Clears singleton so next {@link #getDefault()} reloads (tests). */
  public static synchronized void resetDefault() {
    INSTANCE = null;
  }

  /**
   * Gets the immutable list of allow-list glob patterns.
   *
   * @return the allow patterns, never {@code null}.
   */
  public List<String> getAllowPatterns() {
    return allowPatterns;
  }

  /**
   * Gets the immutable list of block-list glob patterns.
   *
   * @return the block patterns, never {@code null}.
   */
  public List<String> getBlockPatterns() {
    return blockPatterns;
  }

  /**
   * Tests whether the supplied normalized URL matches any allow pattern.
   *
   * @param normalizedUrl the URL string normalized by {@link URLGlobMatcher#normalize}; may be
   *     {@code null}.
   * @return {@code true} if any allow pattern matches the URL; {@code false} otherwise.
   */
  public boolean matchesAllow(String normalizedUrl) {
    return matchesAny(allowPatterns, normalizedUrl);
  }

  /**
   * Tests whether the supplied normalized URL matches any block pattern.
   *
   * @param normalizedUrl the URL string normalized by {@link URLGlobMatcher#normalize}; may be
   *     {@code null}.
   * @return {@code true} if any block pattern matches the URL; {@code false} otherwise.
   */
  public boolean matchesBlock(String normalizedUrl) {
    return matchesAny(blockPatterns, normalizedUrl);
  }

  private static boolean matchesAny(List<String> patterns, String normalizedUrl) {
    if (patterns == null || patterns.isEmpty() || normalizedUrl == null) {
      return false;
    }
    for (String p : patterns) {
      if (URLGlobMatcher.matches(p, normalizedUrl)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns a new {@link Builder} for fluent construction of a {@link URLValidationConfig}.
   *
   * @return a fresh builder instance, never {@code null}.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Fluent builder for {@link URLValidationConfig}. Blank patterns and the bare {@code *} pattern
   * are silently skipped on add so callers can feed loose property-file input without
   * pre-filtering.
   */
  public static class Builder {
    /** No-op default constructor. */
    public Builder() {}

    private final List<String> allow = new ArrayList<>();
    private final List<String> block = new ArrayList<>();

    /**
     * Adds a single allow-list pattern, ignoring blank or bare-{@code *} inputs.
     *
     * @param pattern the glob pattern to add; may be {@code null}, blank, or {@code *}.
     * @return this builder, never {@code null}.
     */
    public Builder addAllowPattern(String pattern) {
      if (pattern != null && !pattern.isBlank() && !"*".equals(pattern.trim())) {
        allow.add(pattern.trim());
      }
      return this;
    }

    /**
     * Adds a single block-list pattern, ignoring blank or bare-{@code *} inputs.
     *
     * @param pattern the glob pattern to add; may be {@code null}, blank, or {@code *}.
     * @return this builder, never {@code null}.
     */
    public Builder addBlockPattern(String pattern) {
      if (pattern != null && !pattern.isBlank() && !"*".equals(pattern.trim())) {
        block.add(pattern.trim());
      }
      return this;
    }

    /**
     * Builds the configured {@link URLValidationConfig}.
     *
     * @return a new configuration holding the accumulated allow and block patterns.
     */
    public URLValidationConfig build() {
      return new URLValidationConfig(allow, block);
    }
  }
}
