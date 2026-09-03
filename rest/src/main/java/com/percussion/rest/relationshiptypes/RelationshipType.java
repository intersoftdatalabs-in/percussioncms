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

package com.percussion.rest.relationshiptypes;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.percussion.rest.Guid;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/** Design-time relationship type (PSRelationshipConfig) catalog / Admin write projection. */
@XmlRootElement(name = "RelationshipType")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Relationship type design object (catalog + Admin user-type write)")
public class RelationshipType {

  @Schema(description = "Relationship type GUID")
  private Guid guid;

  @Schema(description = "Internal name")
  private String name;

  @Schema(description = "Display label")
  private String label;

  @Schema(description = "Description")
  private String description;

  @Schema(description = "Type discriminator (system or user)")
  private String type;

  @Schema(description = "Category code (e.g. rs_activeassembly) or label (e.g. Active Assembly)")
  private String category;

  @Schema(description = "Human-readable category label when known")
  private String categoryLabel;

  @Schema(description = "True when this is a system relationship type")
  private boolean systemType;

  @Schema(description = "True when this is a user-defined relationship type")
  private boolean userType;

  @Schema(description = "Whether cloning is allowed")
  private boolean allowCloning;

  @Schema(description = "Use owner revision flag")
  private boolean useOwnerRevision;

  @Schema(description = "Use dependent revision flag")
  private boolean useDependentRevision;

  @Schema(
      description =
          "On POST only: name or GUID of an existing type to copy mutable fields from"
              + " (Workbench copy-from-system). Ignored on GET/PUT.")
  private String copyFrom;

  @Schema(description = "Conditional effects")
  private List<RelationshipTypeEffect> effects = new ArrayList<>();

  @Schema(description = "System properties")
  private List<RelationshipTypeProperty> systemProperties = new ArrayList<>();

  @Schema(description = "User properties")
  private List<RelationshipTypeProperty> userProperties = new ArrayList<>();

  @Schema(
      description =
          "Honest design gaps for this surface. Present on detail GET; typically omitted on"
              + " list rows to avoid repeating the same catalog-level array (REST-GAPS-02)")
  private List<String> designGaps = new ArrayList<>();

  public RelationshipType() {}

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

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getCategoryLabel() {
    return categoryLabel;
  }

  public void setCategoryLabel(String categoryLabel) {
    this.categoryLabel = categoryLabel;
  }

  public boolean isSystemType() {
    return systemType;
  }

  public void setSystemType(boolean systemType) {
    this.systemType = systemType;
  }

  public boolean isUserType() {
    return userType;
  }

  public void setUserType(boolean userType) {
    this.userType = userType;
  }

  public boolean isAllowCloning() {
    return allowCloning;
  }

  public void setAllowCloning(boolean allowCloning) {
    this.allowCloning = allowCloning;
  }

  public boolean isUseOwnerRevision() {
    return useOwnerRevision;
  }

  public void setUseOwnerRevision(boolean useOwnerRevision) {
    this.useOwnerRevision = useOwnerRevision;
  }

  public boolean isUseDependentRevision() {
    return useDependentRevision;
  }

  public void setUseDependentRevision(boolean useDependentRevision) {
    this.useDependentRevision = useDependentRevision;
  }

  public String getCopyFrom() {
    return copyFrom;
  }

  public void setCopyFrom(String copyFrom) {
    this.copyFrom = copyFrom;
  }

  public List<RelationshipTypeEffect> getEffects() {
    return effects;
  }

  public void setEffects(List<RelationshipTypeEffect> effects) {
    this.effects = effects;
  }

  public List<RelationshipTypeProperty> getSystemProperties() {
    return systemProperties;
  }

  public void setSystemProperties(List<RelationshipTypeProperty> systemProperties) {
    this.systemProperties = systemProperties;
  }

  public List<RelationshipTypeProperty> getUserProperties() {
    return userProperties;
  }

  public void setUserProperties(List<RelationshipTypeProperty> userProperties) {
    this.userProperties = userProperties;
  }

  public List<String> getDesignGaps() {
    return designGaps;
  }

  public void setDesignGaps(List<String> designGaps) {
    this.designGaps = designGaps;
  }
}
