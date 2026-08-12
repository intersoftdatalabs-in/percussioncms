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

/**
 * Lightweight child / folder summary for content-explorer folder APIs (RX {@code PSItemSummary}
 * shape).
 */
@XmlRootElement(name = "RxFolderSummary")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Folder or item summary under a parent folder")
public class RxFolderSummary {

  @Schema(description = "Legacy content id")
  private Long contentId;

  @Schema(description = "Guid string when available")
  private String id;

  @Schema(description = "Display name (folder name or sys_title)")
  private String name;

  @Schema(description = "Object type: FOLDER or ITEM")
  private String objectType;

  @Schema(description = "Content type name when known")
  private String contentTypeName;

  @Schema(description = "Content type id when known")
  private Integer contentTypeId;

  public RxFolderSummary() {}

  public Long getContentId() {
    return contentId;
  }

  public void setContentId(Long contentId) {
    this.contentId = contentId;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getObjectType() {
    return objectType;
  }

  public void setObjectType(String objectType) {
    this.objectType = objectType;
  }

  public String getContentTypeName() {
    return contentTypeName;
  }

  public void setContentTypeName(String contentTypeName) {
    this.contentTypeName = contentTypeName;
  }

  public Integer getContentTypeId() {
    return contentTypeId;
  }

  public void setContentTypeId(Integer contentTypeId) {
    this.contentTypeId = contentTypeId;
  }
}
