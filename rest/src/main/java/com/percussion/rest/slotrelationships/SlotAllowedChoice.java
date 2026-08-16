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

/** Allowed content type or snippet template for a slot. */
@XmlRootElement(name = "SlotAllowedChoice")
@Schema(description = "Allowed type or template for a slot")
public class SlotAllowedChoice {

  @Schema(description = "Numeric id (content type or template)")
  private int id;

  @Schema(description = "Internal name")
  private String name;

  @Schema(description = "Display label")
  private String label;

  public SlotAllowedChoice() {}

  public SlotAllowedChoice(int id, String name, String label) {
    this.id = id;
    this.name = name;
    this.label = label;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SlotAllowedChoice that)) {
      return false;
    }
    return id == that.id && Objects.equals(name, that.name) && Objects.equals(label, that.label);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, label);
  }
}
