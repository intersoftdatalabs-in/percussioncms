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

// REFACTORED: CP-JAVA11

package com.percussion.rest.folders;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Represents a request to copy a folder or item.
 *
 * <p>Wire getters return plain types (not {@code Optional}) so Jackson/CXF JSON emits path
 * scalars (issue #3413 / #3388).
 */
@XmlRootElement
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Represents a request to copy a folder or item")
public class CopyFolderItemRequest {

  @Schema(name = "targetFolderPath", required = true, description = "Target folder path")
  private String targetFolderPath;

  @Schema(name = "itemPath", required = true, description = "Item path")
  private String itemPath;

  public CopyFolderItemRequest() {
    // Default constructor
  }

  @JsonCreator
  public CopyFolderItemRequest(
      @JsonProperty(value = "targetFolderPath") String targetFolderPath,
      @JsonProperty(value = "itemPath") String itemPath) {
    this.targetFolderPath = targetFolderPath;
    this.itemPath = itemPath;
  }

  /**
   * Gets the target folder path.
   *
   * @return the target folder path, or {@code null} if unset
   */
  public String getTargetFolderPath() {
    return targetFolderPath;
  }

  public void setTargetFolderPath(String targetFolderPath) {
    this.targetFolderPath = targetFolderPath;
  }

  /**
   * Gets the item path.
   *
   * @return the item path, or {@code null} if unset
   */
  public String getItemPath() {
    return itemPath;
  }

  public void setItemPath(String itemPath) {
    this.itemPath = itemPath;
  }
}
