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
 * Request to create a folder under an Explorer parent (#4637).
 *
 * <p>Wire getters return plain types so Jackson/CXF JSON emits path scalars. {@code
 * @JsonRootName} / JAXB root keep WRAP_ROOT_VALUE ({@code {"CreateFolderRequest":{...}}}).
 */
@XmlRootElement(name = "CreateFolderRequest")
@JsonRootName("CreateFolderRequest")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Create a folder under a parent Explorer path")
public class CreateFolderRequest {

  @Schema(name = "parentPath", required = true, description = "Full parent folder path")
  private String parentPath;

  @Schema(name = "name", required = true, description = "New folder name (single path segment)")
  private String name;

  public CreateFolderRequest() {
    // Default constructor
  }

  @JsonCreator
  public CreateFolderRequest(
      @JsonProperty(value = "parentPath") String parentPath,
      @JsonProperty(value = "name") String name) {
    this.parentPath = parentPath;
    this.name = name;
  }

  public String getParentPath() {
    return parentPath;
  }

  public void setParentPath(String parentPath) {
    this.parentPath = parentPath;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
