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

package com.percussion.services.pipeline;

import com.percussion.services.pipeline.model.PipelineIrDocument;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

/**
 * File-backed native pipeline IR store under a configurable base directory.
 *
 * <p>Mirrors the classic design objectstore pattern of one document per application name, but
 * stores versioned JSON IR ({@code &lt;safeName&gt;.pipeline.json}) instead of {@code
 * PSXApplication} XML. Paths are resolved with {@link Path} only (portable Windows/Unix); names are
 * sanitized against traversal.
 */
public class PSPipelineIrFileStore {

  /** Filename suffix for native IR documents. */
  public static final String FILE_SUFFIX = ".pipeline.json";

  private final Path baseDir;

  /**
   * @param baseDir directory that will hold {@code *.pipeline.json} files (created on first save)
   */
  public PSPipelineIrFileStore(Path baseDir) {
    this.baseDir = Objects.requireNonNull(baseDir, "baseDir").toAbsolutePath().normalize();
  }

  /** Absolute normalized store root. */
  public Path getBaseDir() {
    return baseDir;
  }

  /**
   * Persist IR under {@code baseDir/&lt;appName&gt;.pipeline.json}.
   *
   * @param document must include a safe non-blank {@code app.name}
   */
  public void save(PipelineIrDocument document) throws PSPipelineIrException {
    Objects.requireNonNull(document, "document");
    if (document.getApp() == null || StringUtils.isBlank(document.getApp().getName())) {
      throw new PSPipelineIrException("Pipeline IR app.name is required to save");
    }
    String name = document.getApp().getName().trim();
    if (!isSafeApplicationName(name)) {
      throw new PSPipelineIrException("Unsafe pipeline application name for store: " + name);
    }
    Path target = resolveExistingOrNew(name);
    try {
      Files.createDirectories(baseDir);
      String json = PSPipelineIrJsonCodec.toJson(document);
      // Write via temp + move for atomicity on the same filesystem when supported
      Path tmp = target.resolveSibling(target.getFileName().toString() + ".tmp");
      Files.writeString(tmp, json, StandardCharsets.UTF_8);
      try {
        Files.move(tmp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
            java.nio.file.StandardCopyOption.ATOMIC_MOVE);
      } catch (IOException atomicFailed) {
        Files.move(tmp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      throw new PSPipelineIrException("Failed to save pipeline IR for " + name, e);
    }
  }

  /**
   * Load IR by application name.
   *
   * @return empty when file is missing
   */
  public Optional<PipelineIrDocument> load(String appName) throws PSPipelineIrException {
    if (!isSafeApplicationName(appName)) {
      throw new PSPipelineIrException("Unsafe pipeline application name for store: " + appName);
    }
    Path target = resolveExistingOrNew(appName.trim());
    if (!Files.isRegularFile(target)) {
      return Optional.empty();
    }
    try {
      String json = Files.readString(target, StandardCharsets.UTF_8);
      return Optional.of(PSPipelineIrJsonCodec.fromJson(json));
    } catch (IOException e) {
      throw new PSPipelineIrException("Failed to load pipeline IR for " + appName, e);
    }
  }

  /** @return true when a native IR file exists for the name */
  public boolean exists(String appName) throws PSPipelineIrException {
    if (!isSafeApplicationName(appName)) {
      throw new PSPipelineIrException("Unsafe pipeline application name for store: " + appName);
    }
    return Files.isRegularFile(resolveExistingOrNew(appName.trim()));
  }

  /**
   * Delete native IR file if present.
   *
   * @return true when a file was removed
   */
  public boolean delete(String appName) throws PSPipelineIrException {
    if (!isSafeApplicationName(appName)) {
      throw new PSPipelineIrException("Unsafe pipeline application name for store: " + appName);
    }
    Path target = resolveExistingOrNew(appName.trim());
    try {
      return Files.deleteIfExists(target);
    } catch (IOException e) {
      throw new PSPipelineIrException("Failed to delete pipeline IR for " + appName, e);
    }
  }

  /**
   * Single path component only — matches catalog path-injection sanitizer patterns used by
   * {@code PipelinesAdaptor}.
   */
  public static boolean isSafeApplicationName(String name) {
    if (StringUtils.isBlank(name)) {
      return false;
    }
    return !name.contains("..")
        && name.indexOf('/') < 0
        && name.indexOf('\\') < 0
        && name.indexOf('\0') < 0;
  }

  /**
   * Resolve storage path and ensure it remains under {@link #baseDir}.
   */
  Path resolveExistingOrNew(String safeName) throws PSPipelineIrException {
    Path candidate = baseDir.resolve(safeName + FILE_SUFFIX).normalize();
    if (!candidate.startsWith(baseDir)) {
      throw new PSPipelineIrException("Resolved path escapes pipeline IR store root");
    }
    return candidate;
  }
}
