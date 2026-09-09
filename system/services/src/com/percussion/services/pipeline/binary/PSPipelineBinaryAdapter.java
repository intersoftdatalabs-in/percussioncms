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

package com.percussion.services.pipeline.binary;

import com.percussion.services.pipeline.PSPipelineIrException;
import com.percussion.services.pipeline.model.PipelineBinaryPayload;
import com.percussion.services.pipeline.model.PipelineBinaryResourceIr;
import com.percussion.services.pipeline.model.PipelineResourceIr;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * Retrieve native pipeline binary fixture bytes: bundled classpath fixture or a portable-safe
 * relative file under an optional application-owned sandbox. Missing/empty fixtures are errors
 * (no invented content). No live internet fetch.
 */
public class PSPipelineBinaryAdapter {

  /** JVM override for {@link #DEFAULT_MAX_BODY_BYTES}; non-positive or unparsable values are ignored. */
  public static final String MAX_BODY_BYTES_PROPERTY = "perc.pipeline.binary.maxBodyBytes";

  public static final int DEFAULT_MAX_BODY_BYTES = 1_000_000;

  static final int MAX_BODY_BYTES = DEFAULT_MAX_BODY_BYTES;

  /** Known bundled fixture UTF-8 marker (classpath {@link PSPipelineBinaryPath#BUNDLED_RESOURCE}). */
  public static final String BUNDLED_FIXTURE_UTF8 = "PIPE-BIN-FIXTURE\n";

  private final Path sandboxRoot;

  private final int maxBodyBytes;

  public PSPipelineBinaryAdapter() {
    this(null);
  }

  /**
   * @param sandboxRoot optional application-owned files root; relative paths resolve under {@code
   *     sandboxRoot/&lt;appName&gt;/}. {@code null} means only the bundled fixture can retrieve.
   */
  public PSPipelineBinaryAdapter(Path sandboxRoot) {
    this(sandboxRoot, resolveMaxBodyBytes());
  }

  /**
   * @param sandboxRoot optional application-owned files root
   * @param maxBodyBytes positive retrieve size cap (bytes)
   */
  public PSPipelineBinaryAdapter(Path sandboxRoot, int maxBodyBytes) {
    this.sandboxRoot =
        sandboxRoot != null ? sandboxRoot.toAbsolutePath().normalize() : null;
    if (maxBodyBytes < 1) {
      throw new IllegalArgumentException("maxBodyBytes must be positive");
    }
    this.maxBodyBytes = maxBodyBytes;
  }

  public static int resolveMaxBodyBytes() {
    String raw = System.getProperty(MAX_BODY_BYTES_PROPERTY);
    if (raw == null || raw.isBlank()) {
      return DEFAULT_MAX_BODY_BYTES;
    }
    try {
      int parsed = Integer.parseInt(raw.trim());
      return parsed > 0 ? parsed : DEFAULT_MAX_BODY_BYTES;
    } catch (NumberFormatException e) {
      return DEFAULT_MAX_BODY_BYTES;
    }
  }

  /**
   * Load fixture bytes for a BINARY resource.
   *
   * @param appName trusted application name (sandbox child); may be blank for bundled-only
   */
  public PipelineBinaryPayload retrieve(String appName, PipelineResourceIr resource)
      throws PSPipelineIrException {
    Objects.requireNonNull(resource, "resource");
    PipelineBinaryResourceIr binary = resource.getBinary();
    if (binary == null || !binary.isPresent()) {
      throw new PSPipelineIrException("Binary resource not found in IR");
    }
    String path = PSPipelineBinaryPath.requireSafe(binary.getPath());
    String contentType = PSPipelineBinaryPath.requireSafeContentType(binary.getContentType());
    byte[] bytes;
    if (PSPipelineBinaryPath.isBundledFixture(path)) {
      bytes = readBundledFixture();
    } else {
      bytes = readSandboxFile(appName, path);
    }
    if (bytes == null || bytes.length == 0) {
      throw new PSPipelineIrException("Binary fixture not found");
    }
    if (bytes.length > maxBodyBytes) {
      throw new PSPipelineIrException("Binary fixture exceeds size limit");
    }
    return new PipelineBinaryPayload(contentType, bytes, path);
  }

  static byte[] readBundledFixture() throws PSPipelineIrException {
    try (InputStream in =
        PSPipelineBinaryAdapter.class.getResourceAsStream(PSPipelineBinaryPath.BUNDLED_RESOURCE)) {
      if (in == null) {
        throw new PSPipelineIrException("Binary fixture not found");
      }
      byte[] bytes = in.readAllBytes();
      if (bytes.length == 0) {
        throw new PSPipelineIrException("Binary fixture not found");
      }
      return bytes;
    } catch (IOException e) {
      throw new PSPipelineIrException("Failed to read bundled binary pipeline fixture", e);
    }
  }

  byte[] readSandboxFile(String appName, String relativePath) throws PSPipelineIrException {
    if (sandboxRoot == null) {
      throw new PSPipelineIrException("Binary fixture not found");
    }
    if (StringUtils.isBlank(appName) || appName.contains("..") || appName.indexOf('/') >= 0
        || appName.indexOf('\\') >= 0 || appName.indexOf('\0') >= 0) {
      throw new PSPipelineIrException("Binary fixture not found");
    }
    Path appDir = sandboxRoot.resolve(appName.trim()).normalize();
    if (!appDir.startsWith(sandboxRoot)) {
      throw new PSPipelineIrException("Binary resource path must not contain path traversal");
    }
    Path target = appDir.resolve(relativePath).normalize();
    if (!target.startsWith(appDir)) {
      throw new PSPipelineIrException("Binary resource path must not contain path traversal");
    }
    if (!Files.isRegularFile(target)) {
      throw new PSPipelineIrException("Binary fixture not found");
    }
    try {
      byte[] bytes = Files.readAllBytes(target);
      if (bytes.length == 0) {
        throw new PSPipelineIrException("Binary fixture not found");
      }
      return bytes;
    } catch (IOException e) {
      throw new PSPipelineIrException("Failed to read application-owned binary fixture", e);
    }
  }
}
