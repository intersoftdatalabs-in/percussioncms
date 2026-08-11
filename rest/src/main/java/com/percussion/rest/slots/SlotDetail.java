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
import com.percussion.rest.DesignGap;
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

  /**
   * Structural {@code slot_layout} map (ADR-003). Keys include {@code schemaVersion} plus layout
   * properties (e.g. {@code orientation}, {@code columns}, {@code maxItems}). On update, a non-null
   * map replaces the definition layout; omit or leave null to leave unchanged. Empty / schema-only
   * maps clear stored layout to defaults.
   */
  @Schema(
      description =
          "slot_layout map (schemaVersion + structural keys). Non-null on PUT replaces definition"
              + " layout; null leaves unchanged.")
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private Map<String, Object> slotLayout;

  /**
   * Presentational {@code slot_styles} map (ADR-003). Keys include {@code schemaVersion} plus style
   * tokens (e.g. {@code rootclass}, {@code itemclass}). Same update semantics as {@link
   * #slotLayout}.
   */
  @Schema(
      description =
          "slot_styles map (schemaVersion + style tokens). Non-null on PUT replaces definition"
              + " styles; null leaves unchanged.")
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private Map<String, Object> slotStyles;

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private List<SlotAssociationSummary> associations;

  @Schema(
      description =
          "Structured capability notes (code + message) vs full Workbench slot design"
              + " (REST-GAPS-01). Wire shape is objects, not free-text strings.")
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private List<DesignGap> designGaps;

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

  public Map<String, Object> getSlotLayout() {
    return slotLayout;
  }

  public void setSlotLayout(Map<String, Object> slotLayout) {
    this.slotLayout = slotLayout;
  }

  public Map<String, Object> getSlotStyles() {
    return slotStyles;
  }

  public void setSlotStyles(Map<String, Object> slotStyles) {
    this.slotStyles = slotStyles;
  }

  public List<SlotAssociationSummary> getAssociations() {
    return associations;
  }

  public void setAssociations(List<SlotAssociationSummary> associations) {
    this.associations = associations;
  }

  public List<DesignGap> getDesignGaps() {
    return designGaps;
  }

  public void setDesignGaps(List<DesignGap> designGaps) {
    this.designGaps = designGaps;
  }
}
