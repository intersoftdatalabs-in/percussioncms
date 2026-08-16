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
import java.util.Objects;

/** One Active Assembly content relationship (item in a slot). */
@XmlRootElement(name = "SlotRelationship")
@Schema(description = "Active Assembly slot relationship")
public class SlotRelationship {

  @Schema(description = "Relationship id")
  private int relationshipId;

  @Schema(description = "Owner (page or snippet) content id")
  private int ownerId;

  @Schema(description = "Dependent content id")
  private int dependentId;

  @Schema(description = "Slot id")
  private int slotId;

  @Schema(description = "Snippet template id")
  private int templateId;

  @Schema(description = "Zero-based sort rank in the slot")
  private int sortRank;

  public SlotRelationship() {}

  public SlotRelationship(
      int relationshipId, int ownerId, int dependentId, int slotId, int templateId, int sortRank) {
    this.relationshipId = relationshipId;
    this.ownerId = ownerId;
    this.dependentId = dependentId;
    this.slotId = slotId;
    this.templateId = templateId;
    this.sortRank = sortRank;
  }

  public int getRelationshipId() {
    return relationshipId;
  }

  public void setRelationshipId(int relationshipId) {
    this.relationshipId = relationshipId;
  }

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

  public int getSortRank() {
    return sortRank;
  }

  public void setSortRank(int sortRank) {
    this.sortRank = sortRank;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SlotRelationship that)) {
      return false;
    }
    return relationshipId == that.relationshipId
        && ownerId == that.ownerId
        && dependentId == that.dependentId
        && slotId == that.slotId
        && templateId == that.templateId
        && sortRank == that.sortRank;
  }

  @Override
  public int hashCode() {
    return Objects.hash(relationshipId, ownerId, dependentId, slotId, templateId, sortRank);
  }
}
