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

package com.percussion.rest.displayformat;

import com.percussion.rest.Guid;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Objects;

/** Represents a Display Format in Percussion CMS. */
@XmlRootElement(name = "DisplayFormat")
@Schema(description = "Represents a DisplayFormat.")
public class DisplayFormat {

  @Schema(description = "The global unique id for this item.")
  private Guid guid;

  /**
   * Plain {@code host-type-uuid} string for SPA Object ACL binding when nested {@link Guid}
   * Optional/wrap shapes are hard to read (#3200). Always set by the adaptor when a GUID exists.
   */
  @Schema(description = "Plain GUID string (host-type-uuid) for Object ACL and detail header.")
  private String guidString;

  @Schema(description = "The name of this Display Format")
  private String name;

  private String label;
  private boolean validForRelatedContent;
  private String sortedColumnNames;
  private boolean ascendingSort;
  private boolean descendingSort;
  private boolean validForViewsAndSearches;
  private boolean validForFolder;
  private String invalidFolderFieldNames;
  private int displayId;
  private DisplayFormatPropertyList properties;
  private DisplayFormatColumnList columns;
  private String internalName;

  /**
   * Allowed communities. Empty list is all communities ({@code sys_community=-1}). {@code null} on
   * PUT leaves visibility unchanged.
   */
  @Schema(
      description =
          "Allowed communities (guid + name). Empty array is all communities. Omit on PUT to leave"
              + " visibility unchanged. Unknown community is 400.")
  private List<DisplayFormatCommunity> allowedCommunities;

  private String description;
  private String displayName;

  public DisplayFormat() {}

  /**
   * Copy writable fields for POST create without mutating {@code source}. Identity ({@code guid},
   * {@code guidString}, {@code displayId}) is left unset so create cannot reuse a client id.
   */
  public static DisplayFormat copyForCreate(DisplayFormat source) {
    DisplayFormat copy = new DisplayFormat();
    if (source == null) {
      return copy;
    }
    copy.setName(source.getName());
    copy.setLabel(source.getLabel());
    copy.setValidForRelatedContent(source.isValidForRelatedContent());
    copy.setSortedColumnNames(source.getSortedColumnNames());
    copy.setAscendingSort(source.isAscendingSort());
    copy.setDescendingSort(source.isDescendingSort());
    copy.setValidForViewsAndSearches(source.isValidForViewsAndSearches());
    copy.setValidForFolder(source.isValidForFolder());
    copy.setInvalidFolderFieldNames(source.getInvalidFolderFieldNames());
    copy.setProperties(source.getProperties());
    copy.setColumns(source.getColumns());
    copy.setInternalName(source.getInternalName());
    copy.setAllowedCommunities(source.getAllowedCommunities());
    copy.setDescription(source.getDescription());
    copy.setDisplayName(source.getDisplayName());
    return copy;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public Guid getGuid() {
    return guid;
  }

  public void setGuid(Guid guid) {
    this.guid = guid;
  }

  public String getGuidString() {
    return guidString;
  }

  public void setGuidString(String guidString) {
    this.guidString = guidString;
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

  public boolean isValidForRelatedContent() {
    return validForRelatedContent;
  }

  public void setValidForRelatedContent(boolean validForRelatedContent) {
    this.validForRelatedContent = validForRelatedContent;
  }

  public String getSortedColumnNames() {
    return sortedColumnNames;
  }

  public void setSortedColumnNames(String sortedColumnNames) {
    this.sortedColumnNames = sortedColumnNames;
  }

  public boolean isAscendingSort() {
    return ascendingSort;
  }

  public void setAscendingSort(boolean ascendingSort) {
    this.ascendingSort = ascendingSort;
  }

  public boolean isDescendingSort() {
    return descendingSort;
  }

  public void setDescendingSort(boolean descendingSort) {
    this.descendingSort = descendingSort;
  }

  public boolean isValidForViewsAndSearches() {
    return validForViewsAndSearches;
  }

  public void setValidForViewsAndSearches(boolean validForViewsAndSearches) {
    this.validForViewsAndSearches = validForViewsAndSearches;
  }

  public boolean isValidForFolder() {
    return validForFolder;
  }

  public void setValidForFolder(boolean validForFolder) {
    this.validForFolder = validForFolder;
  }

  public String getInvalidFolderFieldNames() {
    return invalidFolderFieldNames;
  }

  public void setInvalidFolderFieldNames(String invalidFolderFieldNames) {
    this.invalidFolderFieldNames = invalidFolderFieldNames;
  }

  public int getDisplayId() {
    return displayId;
  }

  public void setDisplayId(int displayId) {
    this.displayId = displayId;
  }

  public DisplayFormatPropertyList getProperties() {
    return properties;
  }

  public void setProperties(DisplayFormatPropertyList properties) {
    this.properties = properties;
  }

  public DisplayFormatColumnList getColumns() {
    return columns;
  }

  public void setColumns(DisplayFormatColumnList columns) {
    this.columns = columns;
  }

  public String getInternalName() {
    return internalName;
  }

  public void setInternalName(String internalName) {
    this.internalName = internalName;
  }

  public List<DisplayFormatCommunity> getAllowedCommunities() {
    return allowedCommunities;
  }

  public void setAllowedCommunities(List<DisplayFormatCommunity> allowedCommunities) {
    this.allowedCommunities = allowedCommunities;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DisplayFormat)) return false;
    var that = (DisplayFormat) o;
    return validForRelatedContent == that.validForRelatedContent
        && ascendingSort == that.ascendingSort
        && descendingSort == that.descendingSort
        && validForViewsAndSearches == that.validForViewsAndSearches
        && validForFolder == that.validForFolder
        && displayId == that.displayId
        && Objects.equals(guid, that.guid)
        // guidString is derived from guid; omit so unset companions stay equal
        && Objects.equals(name, that.name)
        && Objects.equals(label, that.label)
        && Objects.equals(sortedColumnNames, that.sortedColumnNames)
        && Objects.equals(invalidFolderFieldNames, that.invalidFolderFieldNames)
        && Objects.equals(properties, that.properties)
        && Objects.equals(columns, that.columns)
        && Objects.equals(internalName, that.internalName)
        && Objects.equals(allowedCommunities, that.allowedCommunities)
        && Objects.equals(description, that.description)
        && Objects.equals(displayName, that.displayName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        guid,
        name,
        label,
        validForRelatedContent,
        sortedColumnNames,
        ascendingSort,
        descendingSort,
        validForViewsAndSearches,
        validForFolder,
        invalidFolderFieldNames,
        displayId,
        properties,
        columns,
        internalName,
        allowedCommunities,
        description,
        displayName);
  }

  @Override
  public String toString() {
    return "DisplayFormat{"
        + "guid="
        + guid
        + ", guidString='"
        + guidString
        + '\''
        + ", name='"
        + name
        + '\''
        + ", label='"
        + label
        + '\''
        + ", validForRelatedContent="
        + validForRelatedContent
        + ", sortedColumnNames='"
        + sortedColumnNames
        + '\''
        + ", ascendingSort="
        + ascendingSort
        + ", descendingSort="
        + descendingSort
        + ", validForViewsAndSearches="
        + validForViewsAndSearches
        + ", validForFolder="
        + validForFolder
        + ", invalidFolderFieldNames='"
        + invalidFolderFieldNames
        + '\''
        + ", displayId="
        + displayId
        + ", properties="
        + properties
        + ", columns="
        + columns
        + ", internalName='"
        + internalName
        + '\''
        + ", allowedCommunities="
        + allowedCommunities
        + ", description='"
        + description
        + '\''
        + ", displayName='"
        + displayName
        + '\''
        + '}';
  }
}
