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

/** Request body for {@code IPSContentWs#addFolderTree}. */
@XmlRootElement(name = "AddFolderTreeRequest")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Create missing path segments for a fully qualified RX folder path")
public class AddFolderTreeRequest {

  @Schema(
      description = "Fully qualified RX path to ensure (//Folders/a/b/c)",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String path;

  public AddFolderTreeRequest() {}

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }
}
