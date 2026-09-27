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
package com.percussion.share.relationship.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.percussion.share.data.PSAbstractDataObject;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * One relationship the Explorer relationships panel can remove from the selected item.
 *
 * <p>Folder membership ({@code rs_folder}) is not included. The row is owned by the selected
 * content id.
 */
@XmlRootElement(name = "PSExplorerRelationshipEdge")
@JsonRootName("PSExplorerRelationshipEdge")
public class PSExplorerRelationshipEdge extends PSAbstractDataObject {

  private static final long serialVersionUID = 1L;

  private int relationshipId;
  private String configName = "";
  private String category = "";
  private int dependentId;
  private String label = "";

  public PSExplorerRelationshipEdge() {
    super();
  }

  public PSExplorerRelationshipEdge(
      int relationshipId, String configName, String category, int dependentId, String label) {
    this.relationshipId = relationshipId;
    this.configName = configName == null ? "" : configName;
    this.category = category == null ? "" : category;
    this.dependentId = dependentId;
    this.label = label == null ? "" : label;
  }

  public int getRelationshipId() {
    return relationshipId;
  }

  public void setRelationshipId(int relationshipId) {
    this.relationshipId = relationshipId;
  }

  public String getConfigName() {
    return configName;
  }

  public void setConfigName(String configName) {
    this.configName = configName == null ? "" : configName;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category == null ? "" : category;
  }

  public int getDependentId() {
    return dependentId;
  }

  public void setDependentId(int dependentId) {
    this.dependentId = dependentId;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label == null ? "" : label;
  }
}
