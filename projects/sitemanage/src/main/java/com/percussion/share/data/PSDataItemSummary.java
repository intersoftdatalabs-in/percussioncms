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
package com.percussion.share.data;

import com.percussion.assetmanagement.data.PSAsset;
import com.percussion.assetmanagement.data.PSAssetSummary;
import com.percussion.assetmanagement.data.PSOrphanedAssetSummary;
import com.percussion.pagemanagement.assembler.impl.PSNullSiteSummary;
import com.percussion.pagemanagement.data.PSEmptyPage;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.data.PSWidgetItemSummary;
import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.sitemanage.data.PSSiteSummary;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import java.util.Collections;
import java.util.List;
import net.sf.oval.constraint.NotBlank;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;

import java.util.ArrayList;
/**
 * Data summary for items in Percussion CMS. Sunny Sal says: "Summaries so sharp, even your boss
 * will be impressed!"
 */
@XmlSeeAlso({
  PSPage.class,
  PSSiteSummary.class,
  PSAsset.class,
  PSAssetSummary.class,
  PSDataItemSummarySingleFolderPath.class,
  PSEmptyPage.class,
  PSNullSiteSummary.class,
  PSOrphanedAssetSummary.class,
  PSPathItem.class,
  PSWidgetItemSummary.class
})
@XmlRootElement
public class PSDataItemSummary extends PSAbstractPersistantObject implements IPSItemSummary {

  private String id;
  private String name;
  private ArrayList<String> folderPaths;
  private String icon;
  private Category category;
  private boolean revisionable = false;
  private static final long serialVersionUID = 1L;

  /** See {@link #getType()} for detail. */
  private String type;

  /** See {@link #getLabel()} for detail. */
  private String label;

  /**
   * All tags associated with the item. Historically this was called "folderPaths" and we keep the
   * same backing field for compatibility.
   */
  private ArrayList<String> tags;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<String> getFolderPaths() {
    return folderPaths;
  }

  @SuppressWarnings("unchecked")
  public void setFolderPaths(List<String> paths) {
    if (paths == null) {
      this.folderPaths = null;
    } else if (paths instanceof ArrayList) {
      this.folderPaths = (ArrayList<String>) paths;
    } else {
      this.folderPaths = new ArrayList<>(paths);
    }
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  /**
   * Gets the content type of the item.
   *
   * @return the content type, not blank for a valid object.
   */
  @NotNull
  @NotEmpty
  @NotBlank
  public String getType() {
    return type;
  }

  /**
   * Sets the content type of the item.
   *
   * @param type new content type, should not be blank for a valid object.
   * @see #getType()
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * Determines if this is a folder.
   *
   * @return true if this is a folder; otherwise false.
   */
  @Override
  public boolean isFolder() {
    return "Folder".equals(type) || "FSFolder".equals(type);
  }

  @Override
  public boolean isPage() {
    return "percPage".equals(type);
  }

  public boolean isResource() {
    return !isPage() && ("percImageAsset".equals(type) || "percFileAsset".equals(type));
  }

  public Category getCategory() {
    return category;
  }

  public void setCategory(Category category) {
    this.category = category;
  }

  /**
   * Gets the content type label of the item.
   *
   * @return the content type label, not blank for a valid object.
   */
  @NotNull
  @NotEmpty
  @NotBlank
  public String getLabel() {
    return label;
  }

  /**
   * Sets the content type label of the item.
   *
   * @param label new content type label, should not be blank for a valid object.
   * @see #getLabel()
   */
  public void setLabel(String label) {
    this.label = label;
  }

  public boolean isRevisionable() {
    return revisionable;
  }

  public void setRevisionable(boolean revisionable) {
    this.revisionable = revisionable;
  }

  /** The type for a site item summary. */
  public static final String TYPE_SITE = "site";

  @Override
  public List<String> getTags() {
    return tags == null ? Collections.emptyList() : tags;
  }

  /** Helper used by JAXB &amp; legacy code that previously relied on folderPaths. */
  @SuppressWarnings("unchecked")
  public void setTags(List<String> tags) {
    if (tags == null) {
      this.tags = null;
    } else if (tags instanceof ArrayList) {
      this.tags = (ArrayList<String>) tags;
    } else {
      this.tags = new ArrayList<>(tags);
    }
  }
}
