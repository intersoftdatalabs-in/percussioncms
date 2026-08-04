/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

package com.percussion.rest.contenttypes;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.percussion.rest.Guid;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

/**
 * Represents a Content Type summary in Percussion CMS.
 *
 * <p>Wire getters return plain types (not {@code Optional}) so Jackson/CXF JSON always emits {@code
 * name}, {@code label}, and {@code guid} when set. Optional-returning getters historically dropped
 * those fields under {@code @JsonInclude(NON_NULL)} when the mapper did not unwrap {@code
 * Optional}, leaving only {@code hideFromMenu} on the list wire shape (issue #1693). Matches {@link
 * ContentTypeDetail} getter style.
 */
@XmlRootElement(name = "ContentType")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Represents a Content Type")
public class ContentType {

  @Schema(required = true, description = "The Content Type Id")
  private Guid guid;

  @Schema(required = false, description = "Guid for the Object Type content Type")
  private Guid objectType;

  @Schema(required = false, description = "A system unique name for the Content Type")
  private String name;

  @Schema(required = false, description = "A human friendly label for this Content Type")
  private String label;

  @Schema(
      required = false,
      description = "A human friendly Description of this Content Type's purpose")
  private String description;

  @Schema(
      required = false,
      description = "The url to use to request a new Item of this Content Type")
  private String newRequest;

  @Schema(
      required = false,
      description = "The url to use when searching for Items of this Content Type")
  private String queryRequest;

  @Schema(
      required = false,
      description = "The url to use for updating an Item of this Content Type")
  private String updateRequest;

  @Schema(
      required = false,
      description = "When true, this Content Type should be hidden from the user interface")
  private boolean hideFromMenu;

  public ContentType() {}

  public Guid getGuid() {
    return guid;
  }

  public void setGuid(Guid guid) {
    this.guid = guid;
  }

  public Guid getObjectType() {
    return objectType;
  }

  public void setObjectType(Guid objectType) {
    this.objectType = objectType;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getNewRequest() {
    return newRequest;
  }

  public void setNewRequest(String newRequest) {
    this.newRequest = newRequest;
  }

  public String getQueryRequest() {
    return queryRequest;
  }

  public void setQueryRequest(String queryRequest) {
    this.queryRequest = queryRequest;
  }

  public String getUpdateRequest() {
    return updateRequest;
  }

  public void setUpdateRequest(String updateRequest) {
    this.updateRequest = updateRequest;
  }

  public boolean isHideFromMenu() {
    return hideFromMenu;
  }

  public void setHideFromMenu(boolean hideFromMenu) {
    this.hideFromMenu = hideFromMenu;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ContentType)) return false;
    var that = (ContentType) o;
    return hideFromMenu == that.hideFromMenu
        && Objects.equals(guid, that.guid)
        && Objects.equals(objectType, that.objectType)
        && Objects.equals(name, that.name)
        && Objects.equals(label, that.label)
        && Objects.equals(description, that.description)
        && Objects.equals(newRequest, that.newRequest)
        && Objects.equals(queryRequest, that.queryRequest)
        && Objects.equals(updateRequest, that.updateRequest);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        guid,
        objectType,
        name,
        label,
        description,
        newRequest,
        queryRequest,
        updateRequest,
        hideFromMenu);
  }

  @Override
  public String toString() {
    return "ContentType{"
        + "guid="
        + guid
        + ", objectType="
        + objectType
        + ", name='"
        + name
        + '\''
        + ", label='"
        + label
        + '\''
        + ", description='"
        + description
        + '\''
        + ", newRequest='"
        + newRequest
        + '\''
        + ", queryRequest='"
        + queryRequest
        + '\''
        + ", updateRequest='"
        + updateRequest
        + '\''
        + ", hideFromMenu="
        + hideFromMenu
        + '}';
  }
}
