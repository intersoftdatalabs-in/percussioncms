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
 * Rhythmyx folder wire DTO for the content-explorer folders façade (not the CM1 site-centric {@code
 * Folder} type).
 */
@XmlRootElement(name = "RxFolder")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Rhythmyx folder (IPSContentWs / PSFolder façade)")
public class RxFolder {

  @Schema(description = "Folder guid string")
  private String id;

  @Schema(description = "Legacy content id")
  private Long contentId;

  @Schema(description = "Folder name / label")
  private String name;

  @Schema(description = "Fully qualified RX path when known (//Folders/... or //Sites/...)")
  private String path;

  @Schema(description = "Description")
  private String description;

  @Schema(description = "Community id (-1 = all communities)")
  private Integer communityId;

  @Schema(description = "Community name when loaded with transient data")
  private String communityName;

  @Schema(description = "Locale / language string (e.g. en-us)")
  private String locale;

  @Schema(description = "Default display format name when loaded with transient data")
  private String displayFormatName;

  @Schema(description = "Caller permission bitmask when available (transient)")
  private Integer permissions;

  @Schema(description = "Folder properties")
  private List<RxFolderProperty> properties = new ArrayList<>();

  public RxFolder() {}

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Long getContentId() {
    return contentId;
  }

  public void setContentId(Long contentId) {
    this.contentId = contentId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Integer getCommunityId() {
    return communityId;
  }

  public void setCommunityId(Integer communityId) {
    this.communityId = communityId;
  }

  public String getCommunityName() {
    return communityName;
  }

  public void setCommunityName(String communityName) {
    this.communityName = communityName;
  }

  public String getLocale() {
    return locale;
  }

  public void setLocale(String locale) {
    this.locale = locale;
  }

  public String getDisplayFormatName() {
    return displayFormatName;
  }

  public void setDisplayFormatName(String displayFormatName) {
    this.displayFormatName = displayFormatName;
  }

  public Integer getPermissions() {
    return permissions;
  }

  public void setPermissions(Integer permissions) {
    this.permissions = permissions;
  }

  public List<RxFolderProperty> getProperties() {
    return properties;
  }

  public void setProperties(List<RxFolderProperty> properties) {
    this.properties = properties != null ? properties : new ArrayList<>();
  }
}
