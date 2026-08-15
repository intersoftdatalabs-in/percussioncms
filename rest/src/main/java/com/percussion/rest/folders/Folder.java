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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.percussion.rest.LinkRef;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a folder or section based on a folder.
 *
 * <p>Wire getters return plain types (not {@code Optional}) so Jackson/CXF JSON emits {@code name},
 * {@code path}, {@code siteName}, and related fields when set. Optional-returning getters
 * historically serialized as empty/present beans or dropped fields under {@code
 * @JsonInclude(NON_NULL)} (issue #3413 / #3388). Matches {@link
 * com.percussion.rest.contenttypes.ContentType} getter style (issue #1693).
 */
@XmlRootElement(name = "Folder")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Represents a folder or section based on a folder")
public class Folder {

  public static final String ACCESS_LEVEL_ADMIN = "ADMIN";
  public static final String ACCESS_LEVEL_READ = "READ";
  public static final String ACCESS_LEVEL_WRITE = "WRITE";
  public static final String ACCESS_LEVEL_VIEW = "VIEW";

  @Schema(
      name = "id",
      description =
          "ID must match the id of the item for the same server path, usually best not to send id"
              + " to server.")
  private String id;

  @Schema(name = "name", description = "Name of the folder.")
  private String name;

  @Schema(name = "siteName", description = "Name of the site the folder lies under.")
  private String siteName;

  @Schema(name = "path", description = "String of the path from the site to the folder.")
  private String path;

  @Schema(name = "workflow", description = "Workflow state (Generally not needed for folder).")
  private String workflow;

  @Schema(
      name = "accessLevel",
      description = "Access level of site or folder defining access to users",
      allowableValues = "ADMIN,READ,WRITE,VIEW")
  private String accessLevel;

  @Schema(name = "editUsers", description = "List of users that can edit this folder.")
  private List<String> editUsers;

  private SectionInfo sectionInfo;

  @Schema(name = "pages", description = "Pages within the folder.")
  private List<LinkRef> pages;

  @Schema(name = "assets", description = "Assets within the folder.")
  private List<LinkRef> assets;

  @Schema(name = "subfolders", description = "Links to sub-folders.")
  private List<LinkRef> subfolders;

  @Schema(
      name = "subsections",
      description =
          "Links to sub-sections (This folder must also be a section to link to sub-sections).")
  private List<SectionLinkRef> subsections;

  @Schema(
      name = "recentUsers",
      description = "A list of users that have recently accessed the folder.")
  private List<String> recentUsers;

  @Schema(name = "communityId", description = "The default community id to use for this folder")
  private int communityId;

  @Schema(name = "communityName", description = "The default community name to use for this folder")
  private String communityName;

  @Schema(
      name = "defaultDisplayFormatName",
      description = "The default Display Format to use when rendering the contents of this folder")
  private String defaultDisplayFormatName;

  @Schema(name = "locale", description = "The default Locale to use for this folder")
  private String locale;

  public int getCommunityId() {
    return communityId;
  }

  public void setCommunityId(int communityId) {
    this.communityId = communityId;
  }

  public String getCommunityName() {
    return communityName;
  }

  public void setCommunityName(String communityName) {
    this.communityName = communityName;
  }

  public String getDefaultDisplayFormatName() {
    return defaultDisplayFormatName;
  }

  public void setDefaultDisplayFormatName(String defaultDisplayFormatName) {
    this.defaultDisplayFormatName = defaultDisplayFormatName;
  }

  public String getLocale() {
    return locale;
  }

  public void setLocale(String locale) {
    this.locale = locale;
  }

  public List<String> getRecentUsers() {
    if (recentUsers == null) {
      recentUsers = new ArrayList<>();
    }
    return recentUsers;
  }

  public void setRecentUsers(List<String> recentUsers) {
    this.recentUsers = recentUsers;
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

  public String getSiteName() {
    return siteName;
  }

  public void setSiteName(String siteName) {
    this.siteName = siteName;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getWorkflow() {
    return workflow;
  }

  public void setWorkflow(String workflow) {
    this.workflow = workflow;
  }

  public SectionInfo getSectionInfo() {
    return sectionInfo;
  }

  public void setSectionInfo(SectionInfo sectionInfo) {
    this.sectionInfo = sectionInfo;
  }

  public List<LinkRef> getPages() {
    return pages;
  }

  public void setPages(List<LinkRef> pages) {
    this.pages = pages;
  }

  public List<LinkRef> getAssets() {
    return assets;
  }

  public void setAssets(List<LinkRef> assets) {
    this.assets = assets;
  }

  public List<LinkRef> getSubfolders() {
    return subfolders;
  }

  public void setSubfolders(List<LinkRef> subfolders) {
    this.subfolders = subfolders;
  }

  public List<SectionLinkRef> getSubsections() {
    return subsections;
  }

  public void setSubsections(List<SectionLinkRef> subsections) {
    this.subsections = subsections;
  }

  public String getAccessLevel() {
    return accessLevel;
  }

  public void setAccessLevel(String accessLevel) {
    this.accessLevel = accessLevel;
  }

  public List<String> getEditUsers() {
    return editUsers;
  }

  public void setEditUsers(List<String> editUsers) {
    this.editUsers = editUsers;
  }

  @Override
  public String toString() {
    return "Folder [id="
        + id
        + ", name="
        + name
        + ", siteName="
        + siteName
        + ", path="
        + path
        + ", workflow="
        + workflow
        + ", sectionInfo="
        + sectionInfo
        + ", pages="
        + pages
        + ", subfolders="
        + subfolders
        + ", subsections="
        + subsections
        + "]";
  }

  /**
   * Returns the URI for this folder.
   *
   * @param baseURI The base URI.
   * @return The folder URI.
   */
  public URI getFolderURI(URI baseURI) {
    return getFolderURI(baseURI, siteName, path, name);
  }

  /**
   * Returns the URI for a folder given its site, path, and name.
   *
   * @param baseURI The base URI.
   * @param site The site name.
   * @param path The folder path.
   * @param name The folder name.
   * @return The folder URI.
   */
  public static URI getFolderURI(URI baseURI, String site, String path, String name) {
    var info = UriBuilder.fromUri(baseURI).path(FoldersResource.class).path("by-path").path(site);

    if (path != null && !path.isEmpty()) {
      info = info.path(path);
    }
    if (name != null && !name.isEmpty()) {
      info = info.path(name);
    }

    return info.build();
  }
}
