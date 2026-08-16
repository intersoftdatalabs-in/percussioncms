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

/** One slot on the Active Assembly canvas, with current relationships. */
@XmlRootElement(name = "SlotCanvasSlot")
@Schema(description = "Slot on the assembly canvas")
public class SlotCanvasSlot {

  @Schema(description = "Slot id")
  private int slotId;

  @Schema(description = "Slot unique name")
  private String name;

  @Schema(description = "Display label")
  private String label;

  @Schema(description = "Items currently in the slot, sort-rank order")
  private List<SlotRelationship> items = new ArrayList<>();

  public SlotCanvasSlot() {}

  public SlotCanvasSlot(int slotId, String name, String label, List<SlotRelationship> items) {
    this.slotId = slotId;
    this.name = name;
    this.label = label;
    this.items = items != null ? items : new ArrayList<>();
  }

  public int getSlotId() {
    return slotId;
  }

  public void setSlotId(int slotId) {
    this.slotId = slotId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public List<SlotRelationship> getItems() {
    return items;
  }

  public void setItems(List<SlotRelationship> items) {
    this.items = items != null ? items : new ArrayList<>();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SlotCanvasSlot that)) {
      return false;
    }
    return slotId == that.slotId
        && Objects.equals(name, that.name)
        && Objects.equals(label, that.label)
        && Objects.equals(items, that.items);
  }

  @Override
  public int hashCode() {
    return Objects.hash(slotId, name, label, items);
  }
}
