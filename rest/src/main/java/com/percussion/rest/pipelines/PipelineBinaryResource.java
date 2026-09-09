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

package com.percussion.rest.pipelines;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Native IR write for Slice D: persist a binary resource (content type + portable-safe local
 * fixture path). No live internet fetch.
 */
@XmlRootElement(name = "PipelineBinaryResource")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Native pipeline binary resource (local fixture path only)")
public class PipelineBinaryResource {

  static final int MAX_PATH_CHARS = 255;

  static final int MAX_CONTENT_TYPE_CHARS = 128;

  /** Portable-safe relative path or bundled fixture token. */
  @Schema(
      description = "Bundled fixture token or portable-safe relative path (no URLs)",
      maxLength = MAX_PATH_CHARS,
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String path;

  /** Content-Type returned on retrieve. */
  @Schema(description = "MIME type returned on retrieve", maxLength = MAX_CONTENT_TYPE_CHARS)
  private String contentType;

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  /**
   * Fail-fast REST-layer checks before the adaptor path guard. Does not replace
   * scheme/traversal validation.
   */
  public void requireWriteFields() {
    if (path == null || path.isBlank()) {
      throw new IllegalArgumentException("Binary resource path is required");
    }
    if (path.length() > MAX_PATH_CHARS) {
      throw new IllegalArgumentException("Binary resource path exceeds length limit");
    }
    if (contentType != null && contentType.length() > MAX_CONTENT_TYPE_CHARS) {
      throw new IllegalArgumentException("Binary resource content type exceeds length limit");
    }
  }
}
