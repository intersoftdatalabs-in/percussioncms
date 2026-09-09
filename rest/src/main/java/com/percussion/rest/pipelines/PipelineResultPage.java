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
 * Native IR write for Slice D: persist one result-page binding (stylesheet URI + request
 * extension + presentation). {@code presentation=none} omits/disables XSL. No classic XML
 * rewrite.
 */
@XmlRootElement(name = "PipelineResultPage")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Native pipeline result-page binding (local stylesheet URI or presentation=none)")
public class PipelineResultPage {

  static final int MAX_PATH_CHARS = 255;

  static final int MAX_MIME_CHARS = 64;

  static final int MAX_EXTENSION_CHARS = 16;

  static final int MAX_PRESENTATION_CHARS = 16;

  static final String PRESENTATION_NONE = "none";

  @Schema(
      description = "Request extension that selects this result page (.html / .htm)",
      maxLength = MAX_EXTENSION_CHARS)
  private String requestExtension;

  @Schema(description = "MIME type produced by the stylesheet (text/html)", maxLength = MAX_MIME_CHARS)
  private String mimeType;

  @Schema(
      description = "Bundled fixture token or portable-safe relative stylesheet path (no URLs)",
      maxLength = MAX_PATH_CHARS)
  private String stylesheetUri;

  @Schema(
      description = "html (apply XSL) or none (raw JSON/XML, no presentation)",
      maxLength = MAX_PRESENTATION_CHARS)
  private String presentation;

  public String getRequestExtension() {
    return requestExtension;
  }

  public void setRequestExtension(String requestExtension) {
    this.requestExtension = requestExtension;
  }

  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  public String getStylesheetUri() {
    return stylesheetUri;
  }

  public void setStylesheetUri(String stylesheetUri) {
    this.stylesheetUri = stylesheetUri;
  }

  public String getPresentation() {
    return presentation;
  }

  public void setPresentation(String presentation) {
    this.presentation = presentation;
  }

  public boolean isRawPresentation() {
    return presentation != null && PRESENTATION_NONE.equalsIgnoreCase(presentation.trim());
  }

  /**
   * Fail-fast REST-layer checks before the adaptor path guard. Does not replace
   * scheme/traversal validation. {@code presentation=none} allows a blank stylesheet (clear
   * binding).
   */
  public void requireWriteFields() {
    if (!isRawPresentation() && (stylesheetUri == null || stylesheetUri.isBlank())) {
      throw new IllegalArgumentException("Result page stylesheet URI is required");
    }
    if (stylesheetUri != null && stylesheetUri.length() > MAX_PATH_CHARS) {
      throw new IllegalArgumentException("Result page stylesheet URI exceeds length limit");
    }
    if (requestExtension != null && requestExtension.length() > MAX_EXTENSION_CHARS) {
      throw new IllegalArgumentException("Result page request extension exceeds length limit");
    }
    if (mimeType != null && mimeType.length() > MAX_MIME_CHARS) {
      throw new IllegalArgumentException("Result page mime type exceeds length limit");
    }
    if (presentation != null && presentation.length() > MAX_PRESENTATION_CHARS) {
      throw new IllegalArgumentException("Result page presentation exceeds length limit");
    }
  }
}
