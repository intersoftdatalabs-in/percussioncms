// REFACTORED: CP-JAVA11
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
package com.percussion.pathmanagement.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.percussion.pathmanagement.data.xmladapters.PSMapAdapter;
import com.percussion.share.data.IPSFolderPath;
import com.percussion.share.data.IPSItemSummary;
import com.percussion.share.data.PSDataItemSummary;
import com.percussion.share.data.PSMapWrapper;
import jakarta.xml.bind.annotation.*;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 * Represents a path item in the Percussion CMS. Sunny Sal says: "Path items: the breadcrumbs of
 * your CMS journey!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "PathItem")
@JsonRootName("PathItem")
public class PSPathItem extends PSDataItemSummary implements IPSItemSummary, IPSFolderPath {

  @XmlElement private boolean leaf = true;
  private boolean hasItemChildren = false;
  private boolean hasFolderChildren = false;
  private boolean hasSectionChildren = false;
  private boolean mobilePreviewEnabled = false;

  @XmlElement private String path;
  private PSFolderPermission.Access accessLevel;

  /**
   * Runtime association only (e.g. a {@code File} for design FS items). Must stay out of Java
   * serialization ({@code transient}) and JSON ({@code @JsonIgnore} on accessors).
   *
   * <p>Do not put JAXB annotations on this field: Glassfish JAXB rejects {@code @XmlTransient} (or
   * any JAXB annotation) on a {@code transient} field with {@code IllegalAnnotationExceptions}
   * ("Transient field relatedObject cannot have any JAXB annotations"), which surfaced as HTTP 500
   * on Explorer path list/find (#3196).
   */
  private transient Object relatedObject;

  @XmlElement(name = "columnData")
  @XmlJavaTypeAdapter(PSMapAdapter.class)
  protected HashMap<String, String> displayProperties = new HashMap<>();

  /** Used to return properties that are specific to the type of item the path item represents. */
  private PSMapWrapper typeProperties = new PSMapWrapper();

  private String folderPath;

  {
    this.folderPaths = new ArrayList<>();
  }

  public PSMapWrapper getTypeProperties() {
    return typeProperties;
  }

  public void setTypeProperties(PSMapWrapper typeProperties) {
    this.typeProperties = typeProperties;
  }

  /**
   * Add the specified property to the map of properties.
   *
   * @param name property name
   * @param value property value
   */
  public void setTypeProperty(String name, String value) {
    typeProperties.getEntries().put(name, value);
  }

  public String getPath() {
    return path;
  }

  /**
   * Sets the path for this item. Adds a trailing slash for folder items.
   *
   * @param path the path to set
   */
  public void setPath(String path) {
    var tmpPath = path;
    if (isFolder() && !path.endsWith("/")) {
      // Add trailing slash for folder items
      tmpPath += '/';
    }
    this.path = tmpPath;
  }

  public boolean isLeaf() {
    return leaf;
  }

  public void setLeaf(boolean leaf) {
    this.leaf = leaf;
  }

  public boolean hasItemChildren() {
    return hasItemChildren;
  }

  public void setHasItemChildren(boolean hasItemChildren) {
    this.hasItemChildren = hasItemChildren;
  }

  public boolean hasFolderChildren() {
    return hasFolderChildren;
  }

  public void setHasFolderChildren(boolean hasFolderChildren) {
    this.hasFolderChildren = hasFolderChildren;
  }

  public boolean hasSectionChildren() {
    return hasSectionChildren;
  }

  public void setHasSectionChildren(boolean hasSectionChildren) {
    this.hasSectionChildren = hasSectionChildren;
  }

  public boolean isMobilePreviewEnabled() {
    return mobilePreviewEnabled;
  }

  public void setMobilePreviewEnabled(boolean mobilePreviewEnabled) {
    this.mobilePreviewEnabled = mobilePreviewEnabled;
  }

  /**
   * Gets the folder path for this item. If not set, returns the first folder path from the list.
   *
   * @return the folder path
   */
  public String getFolderPath() {
    if (folderPath == null) {
      var paths = getFolderPaths();
      if (paths != null && !paths.isEmpty()) {
        folderPath = paths.get(0);
      }
    }
    return folderPath;
  }

  public void setFolderPath(String folderPath) {
    this.folderPath = folderPath;
  }

  /**
   * Gets the access level of the folder item.
   *
   * @return the access level of a folder, or null if not a folder
   */
  public PSFolderPermission.Access getAccessLevel() {
    return accessLevel;
  }

  /**
   * Sets the access level for the item.
   *
   * @param accessLevel the new access level, or null if not a folder
   */
  public void setAccessLevel(PSFolderPermission.Access accessLevel) {
    // Skip PSPathItem that points to the file system.
    if (StringUtils.startsWith(this.getType(), "FS")) {
      return;
    }
    this.accessLevel = accessLevel;
  }

  @JsonIgnore
  public Object getRelatedObject() {
    return relatedObject;
  }

  @JsonIgnore
  public void setRelatedObject(Object relatedObject) {
    this.relatedObject = relatedObject;
  }

  public Map<String, String> getDisplayProperties() {
    return displayProperties;
  }

  @SuppressWarnings("unchecked")
  public void setDisplayProperties(Map<String, String> value) {
    if (value == null) {
      this.displayProperties = null;
    } else if (value instanceof HashMap) {
      this.displayProperties = (HashMap<String, String>) value;
    } else {
      this.displayProperties = new HashMap<>(value);
    }
  }

  private static final long serialVersionUID = -1L;
}
