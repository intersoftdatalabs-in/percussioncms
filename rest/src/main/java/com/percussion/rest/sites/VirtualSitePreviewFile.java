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
package com.percussion.rest.sites;

import java.util.Objects;

/**
 * Bytes of one assembled Virtual Site file for the Admin preview stream.
 *
 * <p>Not a public JSON entity — {@link SitesResource} maps this to an HTTP {@code Response}.
 */
public class VirtualSitePreviewFile {

  private final String mediaType;
  private final String relativePath;
  private final byte[] content;

  public VirtualSitePreviewFile(String mediaType, String relativePath, byte[] content) {
    this.mediaType = mediaType != null && !mediaType.isBlank() ? mediaType : "application/octet-stream";
    this.relativePath = relativePath != null ? relativePath : "";
    this.content = content != null ? content : new byte[0];
  }

  public String getMediaType() {
    return mediaType;
  }

  public String getRelativePath() {
    return relativePath;
  }

  public byte[] getContent() {
    return content;
  }

  public boolean isHtml() {
    return mediaType.toLowerCase().startsWith("text/html");
  }

  @Override
  public String toString() {
    return "VirtualSitePreviewFile{mediaType="
        + mediaType
        + ", relativePath="
        + relativePath
        + ", bytes="
        + content.length
        + "}";
  }

  @Override
  public int hashCode() {
    return Objects.hash(mediaType, relativePath, content.length);
  }
}
