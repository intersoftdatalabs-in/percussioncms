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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Slots and relationships for an assembled owner item. */
@XmlRootElement(name = "SlotCanvas")
@Schema(description = "Active Assembly slot canvas for an owner item")
public class SlotCanvas {

  @Schema(description = "Owner content id")
  private int ownerId;

  @Schema(description = "Page or snippet template id used to list slots")
  private Integer templateId;

  @Schema(description = "Slots on the template, with current relationships")
  private List<SlotCanvasSlot> slots = new ArrayList<>();

  public SlotCanvas() {}

  public SlotCanvas(int ownerId, Integer templateId, List<SlotCanvasSlot> slots) {
    this.ownerId = ownerId;
    this.templateId = templateId;
    this.slots = slots != null ? slots : new ArrayList<>();
  }

  public int getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(int ownerId) {
    this.ownerId = ownerId;
  }

  public Integer getTemplateId() {
    return templateId;
  }

  public void setTemplateId(Integer templateId) {
    this.templateId = templateId;
  }

  public List<SlotCanvasSlot> getSlots() {
    return slots;
  }

  public void setSlots(List<SlotCanvasSlot> slots) {
    this.slots = slots != null ? slots : new ArrayList<>();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SlotCanvas that)) {
      return false;
    }
    return ownerId == that.ownerId
        && Objects.equals(templateId, that.templateId)
        && Objects.equals(slots, that.slots);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ownerId, templateId, slots);
  }
}
