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

package com.percussion.rest.assembly;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

/** Assembly preview location for Explorer template preview. */
@XmlRootElement(name = "PreviewLocation")
@Schema(description = "Assembly preview URL for a content item and template")
public class PreviewLocation {

  @Schema(description = "Context-root-relative assembly preview path and query")
  private String previewUrl;

  @Schema(description = "Content item id")
  private int contentId;

  @Schema(description = "Assembly template id (legacy variant id)")
  private int templateId;

  @Schema(description = "Revision used to build the URL")
  private int revision;

  public PreviewLocation() {}

  public PreviewLocation(String previewUrl, int contentId, int templateId, int revision) {
    this.previewUrl = previewUrl;
    this.contentId = contentId;
    this.templateId = templateId;
    this.revision = revision;
  }

  public String getPreviewUrl() {
    return previewUrl;
  }

  public void setPreviewUrl(String previewUrl) {
    this.previewUrl = previewUrl;
  }

  public int getContentId() {
    return contentId;
  }

  public void setContentId(int contentId) {
    this.contentId = contentId;
  }

  public int getTemplateId() {
    return templateId;
  }

  public void setTemplateId(int templateId) {
    this.templateId = templateId;
  }

  public int getRevision() {
    return revision;
  }

  public void setRevision(int revision) {
    this.revision = revision;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PreviewLocation that)) {
      return false;
    }
    return contentId == that.contentId
        && templateId == that.templateId
        && revision == that.revision
        && Objects.equals(previewUrl, that.previewUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(previewUrl, contentId, templateId, revision);
  }
}
