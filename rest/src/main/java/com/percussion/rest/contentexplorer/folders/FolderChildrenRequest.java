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

/**
 * Shared request body for multi-child folder ops (move / add / remove children).
 *
 * <p>Identify the parent (and for move, the source) by path or id. Child ids are content/folder
 * guid strings.
 */
@XmlRootElement(name = "FolderChildrenRequest")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Multi-child folder operation request")
public class FolderChildrenRequest {

  @Schema(description = "Source folder path (move only)")
  private String sourcePath;

  @Schema(description = "Source folder id (move only)")
  private String sourceId;

  @Schema(description = "Target / parent folder path (add, remove, move target)")
  private String targetPath;

  @Schema(description = "Target / parent folder id")
  private String targetId;

  @Schema(description = "Parent path alias for add/remove (same as targetPath)")
  private String parentPath;

  @Schema(description = "Parent id alias for add/remove (same as targetId)")
  private String parentId;

  @Schema(description = "Child content/folder guid strings (or content ids)")
  private List<String> childIds = new ArrayList<>();

  @Schema(description = "When true, purge items on remove (requires admin). Default false.")
  private Boolean purgeItems;

  @Schema(description = "When true, enforce folder permission on move. Default true for WS.")
  private Boolean checkFolderPermission;

  public FolderChildrenRequest() {}

  public String getSourcePath() {
    return sourcePath;
  }

  public void setSourcePath(String sourcePath) {
    this.sourcePath = sourcePath;
  }

  public String getSourceId() {
    return sourceId;
  }

  public void setSourceId(String sourceId) {
    this.sourceId = sourceId;
  }

  public String getTargetPath() {
    return targetPath;
  }

  public void setTargetPath(String targetPath) {
    this.targetPath = targetPath;
  }

  public String getTargetId() {
    return targetId;
  }

  public void setTargetId(String targetId) {
    this.targetId = targetId;
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

  public List<String> getChildIds() {
    return childIds;
  }

  public void setChildIds(List<String> childIds) {
    this.childIds = childIds != null ? childIds : new ArrayList<>();
  }

  public Boolean getPurgeItems() {
    return purgeItems;
  }

  public void setPurgeItems(Boolean purgeItems) {
    this.purgeItems = purgeItems;
  }

  public Boolean getCheckFolderPermission() {
    return checkFolderPermission;
  }

  public void setCheckFolderPermission(Boolean checkFolderPermission) {
    this.checkFolderPermission = checkFolderPermission;
  }
}
