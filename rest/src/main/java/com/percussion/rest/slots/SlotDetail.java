/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
import java.util.List;
import java.util.Map;

/** Assembly slot design detail for the Developer module (read + partial write). */
@XmlRootElement(name = "SlotDetail")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Slot detail with finder and content-type/template associations")
public class SlotDetail {

  private Guid guid;
  private String name;
  private String label;
  private String description;
  private String slotType;
  private Boolean systemSlot;
  private String finderName;
  private String relationshipName;

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private Map<String, String> finderArguments;

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private List<SlotAssociationSummary> associations;

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private List<String> designGaps;

  public SlotDetail() {}

  public Guid getGuid() {
    return guid;
  }

  public void setGuid(Guid guid) {
    this.guid = guid;
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

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getSlotType() {
    return slotType;
  }

  public void setSlotType(String slotType) {
    this.slotType = slotType;
  }

  public Boolean getSystemSlot() {
    return systemSlot;
  }

  public void setSystemSlot(Boolean systemSlot) {
    this.systemSlot = systemSlot;
  }

  public String getFinderName() {
    return finderName;
  }

  public void setFinderName(String finderName) {
    this.finderName = finderName;
  }

  public String getRelationshipName() {
    return relationshipName;
  }

  public void setRelationshipName(String relationshipName) {
    this.relationshipName = relationshipName;
  }

  public Map<String, String> getFinderArguments() {
    return finderArguments;
  }

  public void setFinderArguments(Map<String, String> finderArguments) {
    this.finderArguments = finderArguments;
  }

  public List<SlotAssociationSummary> getAssociations() {
    return associations;
  }

  public void setAssociations(List<SlotAssociationSummary> associations) {
    this.associations = associations;
  }

  public List<String> getDesignGaps() {
    return designGaps;
  }

  public void setDesignGaps(List<String> designGaps) {
    this.designGaps = designGaps;
  }
}
