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
 * Native IR binary resource (Slice D): portable-safe local fixture path + content type. No live
 * internet fetch.
 */
public class PipelineBinaryResourceIr {

  public static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

  private String path;
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

  /** True when a fixture path is configured. */
  public boolean isPresent() {
    return StringUtils.isNotBlank(path);
  }

  /** Content type for retrieve; default {@link #DEFAULT_CONTENT_TYPE}. */
  public String resolvedContentType() {
    if (StringUtils.isBlank(contentType)) {
      return DEFAULT_CONTENT_TYPE;
    }
    return contentType.trim();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PipelineBinaryResourceIr that)) {
      return false;
    }
    return Objects.equals(path, that.path) && Objects.equals(contentType, that.contentType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(path, contentType);
  }
}
