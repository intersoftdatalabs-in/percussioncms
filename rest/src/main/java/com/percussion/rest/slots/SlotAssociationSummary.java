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

package com.percussion.rest.slots;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.percussion.rest.Guid;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Content-type ↔ template association on a slot.
 *
 * <p>GET includes names (and labels when cataloged) plus guids. PUT accepts {@code name} or {@code
 * guid} on each side (guid wins when both are present).
 */
@XmlRootElement(name = "SlotAssociation")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Slot content-type / template association (names and guids)")
public class SlotAssociationSummary {

  private Guid contentTypeGuid;
  private String contentTypeName;
  private String contentTypeLabel;
  private Guid templateGuid;
  private String templateName;
  private String templateLabel;

  public SlotAssociationSummary() {}

  public Guid getContentTypeGuid() {
    return contentTypeGuid;
  }

  public void setContentTypeGuid(Guid contentTypeGuid) {
    this.contentTypeGuid = contentTypeGuid;
  }

  public String getContentTypeName() {
    return contentTypeName;
  }

  public void setContentTypeName(String contentTypeName) {
    this.contentTypeName = contentTypeName;
  }

  public String getContentTypeLabel() {
    return contentTypeLabel;
  }

  public void setContentTypeLabel(String contentTypeLabel) {
    this.contentTypeLabel = contentTypeLabel;
  }

  public Guid getTemplateGuid() {
    return templateGuid;
  }

  public void setTemplateGuid(Guid templateGuid) {
    this.templateGuid = templateGuid;
  }

  public String getTemplateName() {
    return templateName;
  }

  public void setTemplateName(String templateName) {
    this.templateName = templateName;
  }

  public String getTemplateLabel() {
    return templateLabel;
  }

  public void setTemplateLabel(String templateLabel) {
    this.templateLabel = templateLabel;
  }
}
