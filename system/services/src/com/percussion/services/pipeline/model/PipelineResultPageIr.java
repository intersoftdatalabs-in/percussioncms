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

package com.percussion.services.pipeline.model;

import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * Native IR result-page binding (Slice D): request extension + MIME + stylesheet URI. Used as the
 * HTML apply binding ({@code resultPage}) and as classic-import inspect entries ({@code
 * resultPages[]}). Classic XML Applications are not rewritten.
 */
public class PipelineResultPageIr {

  public static final String DEFAULT_REQUEST_EXTENSION = ".html";

  public static final String DEFAULT_MIME_TYPE = "text/html";

  private String requestExtension;
  private String mimeType;
  private String stylesheetUri;

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

  /** True when a stylesheet URI is configured. */
  public boolean isPresent() {
    return StringUtils.isNotBlank(stylesheetUri);
  }

  public String resolvedRequestExtension() {
    if (StringUtils.isBlank(requestExtension)) {
      return DEFAULT_REQUEST_EXTENSION;
    }
    return requestExtension.trim();
  }

  public String resolvedMimeType() {
    if (StringUtils.isBlank(mimeType)) {
      return DEFAULT_MIME_TYPE;
    }
    return mimeType.trim();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PipelineResultPageIr that)) {
      return false;
    }
    return Objects.equals(requestExtension, that.requestExtension)
        && Objects.equals(mimeType, that.mimeType)
        && Objects.equals(stylesheetUri, that.stylesheetUri);
  }

  @Override
  public int hashCode() {
    return Objects.hash(requestExtension, mimeType, stylesheetUri);
  }
}
