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
 * Request to add one owned non-folder relationship from the Explorer relationships panel.
 *
 * <p>{@code configName} is the relationship type name (for example {@code Translation}). Folder
 * membership types are rejected.
 */
@XmlRootElement(name = "PSExplorerRelationshipCreate")
@JsonRootName("PSExplorerRelationshipCreate")
public class PSExplorerRelationshipCreate extends PSAbstractDataObject {

  private static final long serialVersionUID = 1L;

  private String targetItemId = "";
  private String configName = "";

  public PSExplorerRelationshipCreate() {
    super();
  }

  public PSExplorerRelationshipCreate(String targetItemId, String configName) {
    this.targetItemId = targetItemId == null ? "" : targetItemId;
    this.configName = configName == null ? "" : configName;
  }

  public String getTargetItemId() {
    return targetItemId;
  }

  public void setTargetItemId(String targetItemId) {
    this.targetItemId = targetItemId == null ? "" : targetItemId;
  }

  public String getConfigName() {
    return configName;
  }

  public void setConfigName(String configName) {
    this.configName = configName == null ? "" : configName;
  }
}
