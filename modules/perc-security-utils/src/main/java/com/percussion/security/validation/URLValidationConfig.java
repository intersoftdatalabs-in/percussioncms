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

  public static synchronized URLValidationConfig getDefault() {
    if (INSTANCE == null) {
      INSTANCE = loadFromInstallRoot();
    }
    return INSTANCE;
  }

  public static synchronized void setDefault(URLValidationConfig config) {
    INSTANCE = config;
  }

  /** Clears singleton so next {@link #getDefault()} reloads (tests). */
  public static synchronized void resetDefault() {
    INSTANCE = null;
  }

  public List<String> getAllowPatterns() {
    return allowPatterns;
  }

  public List<String> getBlockPatterns() {
    return blockPatterns;
  }

  public boolean matchesAllow(String normalizedUrl) {
    return matchesAny(allowPatterns, normalizedUrl);
  }

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

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private final List<String> allow = new ArrayList<>();
    private final List<String> block = new ArrayList<>();

    public Builder addAllowPattern(String pattern) {
      if (pattern != null && !pattern.isBlank() && !"*".equals(pattern.trim())) {
        allow.add(pattern.trim());
      }
      return this;
    }

    public Builder addBlockPattern(String pattern) {
      if (pattern != null && !pattern.isBlank() && !"*".equals(pattern.trim())) {
        block.add(pattern.trim());
      }
      return this;
    }

    public URLValidationConfig build() {
      return new URLValidationConfig(allow, block);
    }
  }
}
