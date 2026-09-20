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

package com.percussion.rest.folders;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Request to rename a non-folder item in Content Explorer (#4636).
 *
 * <p>Wire getters return plain types so Jackson/CXF JSON emits path scalars. {@code
 * @JsonRootName} / JAXB root keep WRAP_ROOT_VALUE ({@code {"RenameFolderItemRequest":{...}}}).
 */
@XmlRootElement(name = "RenameFolderItemRequest")
@JsonRootName("RenameFolderItemRequest")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Rename a page, file, or asset by path")
public class RenameFolderItemRequest {

  @Schema(name = "itemPath", required = true, description = "Full path to the item")
  private String itemPath;

  @Schema(name = "newName", required = true, description = "New sys_title / name")
  private String newName;

  public RenameFolderItemRequest() {
    // Default constructor
  }

  @JsonCreator
  public RenameFolderItemRequest(
      @JsonProperty(value = "itemPath") String itemPath,
      @JsonProperty(value = "newName") String newName) {
    this.itemPath = itemPath;
    this.newName = newName;
  }

  public String getItemPath() {
    return itemPath;
  }

  public void setItemPath(String itemPath) {
    this.itemPath = itemPath;
  }

  public String getNewName() {
    return newName;
  }

  public void setNewName(String newName) {
    this.newName = newName;
  }
}
