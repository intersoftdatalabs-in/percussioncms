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
package com.percussion.rest.slotrelationships;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/** Add an existing item to a slot. */
@XmlRootElement(name = "SlotAddRequest")
@Schema(description = "Add an item to an Active Assembly slot")
public class SlotAddRequest {

  @Schema(description = "Owner content id", requiredMode = Schema.RequiredMode.REQUIRED)
  private int ownerId;

  @Schema(description = "Dependent content id", requiredMode = Schema.RequiredMode.REQUIRED)
  private int dependentId;

  @Schema(description = "Slot id", requiredMode = Schema.RequiredMode.REQUIRED)
  private int slotId;

  @Schema(description = "Snippet template id", requiredMode = Schema.RequiredMode.REQUIRED)
  private int templateId;

  @Schema(description = "Optional folder id of the dependent")
  private Integer folderId;

  @Schema(description = "Optional site id of the dependent")
  private Integer siteId;

  @Schema(description = "Insert index; -1 appends")
  private Integer index;

  public int getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(int ownerId) {
    this.ownerId = ownerId;
  }

  public int getDependentId() {
    return dependentId;
  }

  public void setDependentId(int dependentId) {
    this.dependentId = dependentId;
  }

  public int getSlotId() {
    return slotId;
  }

  public void setSlotId(int slotId) {
    this.slotId = slotId;
  }

  public int getTemplateId() {
    return templateId;
  }

  public void setTemplateId(int templateId) {
    this.templateId = templateId;
  }

  public Integer getFolderId() {
    return folderId;
  }

  public void setFolderId(Integer folderId) {
    this.folderId = folderId;
  }

  public Integer getSiteId() {
    return siteId;
  }

  public void setSiteId(Integer siteId) {
    this.siteId = siteId;
  }

  public Integer getIndex() {
    return index;
  }

  public void setIndex(Integer index) {
    this.index = index;
  }
}
