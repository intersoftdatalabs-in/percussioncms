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

package com.percussion.rest.contentexplorer.folders;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/** Request body for {@code IPSContentWs#addFolder}. */
@XmlRootElement(name = "AddFolderRequest")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Create a single folder under an existing parent path")
public class AddFolderRequest {

  @Schema(description = "New folder name (unique under parent)", requiredMode = Schema.RequiredMode.REQUIRED)
  private String name;

  @Schema(
      description =
          "Fully qualified parent RX path (//Folders/… or //Sites/…; single-slash forms accepted)",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String parentPath;

  @Schema(description = "Optional source folder path to clone ACLs/props from")
  private String sourcePath;

  public AddFolderRequest() {}

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getParentPath() {
    return parentPath;
  }

  public void setParentPath(String parentPath) {
    this.parentPath = parentPath;
  }

  public String getSourcePath() {
    return sourcePath;
  }

  public void setSourcePath(String sourcePath) {
    this.sourcePath = sourcePath;
  }
}
