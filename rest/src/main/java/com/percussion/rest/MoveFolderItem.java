/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

package com.percussion.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * This class is posted to the REST service as part of a request to move an item (from its original
 * folder) to a new (target) folder. All paths are relative to its current root folder.
 *
 * <p>Wire getters return plain types (not {@code Optional}) so Jackson/CXF JSON emits path
 * scalars (issue #3413 / #3388).
 *
 * @author yubingchen
 */
@XmlRootElement(name = "MoveFolderItem")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Represents a request to move a folder item.")
public class MoveFolderItem {
  @Schema(required = true, description = "path")
  private String targetFolderPath;

  @Schema(required = true, description = "path")
  private String itemPath;

  @JsonCreator
  public MoveFolderItem(
      @JsonProperty("itemPath") String itemPath,
      @JsonProperty("targetFolderPath") String targetFolderPath) {
    this.itemPath = itemPath;
    this.targetFolderPath = targetFolderPath;
  }

  public MoveFolderItem() {}

  /**
   * The target folder path where the item is moved to.
   *
   * @return the target folder path, not blank for a valid folder path.
   */
  public String getTargetFolderPath() {
    return targetFolderPath;
  }

  /**
   * Sets the target folder path.
   *
   * @param targetFolderPath the new target folder path, not blank for valid target folder path.
   */
  public void setTargetFolderPath(String targetFolderPath) {
    this.targetFolderPath = targetFolderPath;
  }

  /**
   * Gets the path of the moved item.
   *
   * @return item path, not blank for a valid path.
   */
  public String getItemPath() {
    return itemPath;
  }

  /**
   * Sets the path of the moved item.
   *
   * @param itemPath the new item path, not blank for a valid path.
   */
  public void setItemPath(String itemPath) {
    this.itemPath = itemPath;
  }
}
