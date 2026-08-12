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
import java.util.ArrayList;
import java.util.List;

/** Direct children of a folder (items + folders, or folders-only). */
@XmlRootElement(name = "RxFolderChildList")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "List of direct folder children")
public class RxFolderChildList {

  @Schema(description = "Parent folder path (normalized RX form) when requested by path")
  private String parentPath;

  @Schema(description = "Parent folder id when requested by id")
  private String parentId;

  @Schema(description = "Direct children")
  private List<RxFolderSummary> children = new ArrayList<>();

  public RxFolderChildList() {}

  public RxFolderChildList(List<RxFolderSummary> children) {
    this.children = children != null ? children : new ArrayList<>();
  }

  public String getParentPath() {
    return parentPath;
  }

  public void setParentPath(String parentPath) {
    this.parentPath = parentPath;
  }

  public String getParentId() {
    return parentId;
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  public List<RxFolderSummary> getChildren() {
    return children;
  }

  public void setChildren(List<RxFolderSummary> children) {
    this.children = children != null ? children : new ArrayList<>();
  }
}
