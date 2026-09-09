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

import java.util.Arrays;
import java.util.Objects;

/** Retrieved native pipeline binary fixture bytes (never invented). */
public class PipelineBinaryPayload {

  private final String contentType;
  private final byte[] bytes;
  private final String path;

  public PipelineBinaryPayload(String contentType, byte[] bytes, String path) {
    this.contentType = contentType;
    this.bytes = bytes != null ? bytes.clone() : new byte[0];
    this.path = path;
  }

  public String getContentType() {
    return contentType;
  }

  public byte[] getBytes() {
    return bytes.clone();
  }

  public int getByteLength() {
    return bytes.length;
  }

  public String getPath() {
    return path;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PipelineBinaryPayload that)) {
      return false;
    }
    return Objects.equals(contentType, that.contentType)
        && Arrays.equals(bytes, that.bytes)
        && Objects.equals(path, that.path);
  }

  @Override
  public int hashCode() {
    return Objects.hash(contentType, Arrays.hashCode(bytes), path);
  }
}
