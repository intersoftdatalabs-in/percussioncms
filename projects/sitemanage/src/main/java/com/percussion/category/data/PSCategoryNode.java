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

package com.percussion.category.data;

import com.fasterxml.jackson.annotation.*;
import com.percussion.share.data.PSAbstractDataObject;
import jakarta.xml.bind.annotation.*;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.text.Collator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.json.JsonMapper;

/** Represents a node in the category tree. */
@XmlRootElement(name = "Category")
@JsonRootName("")
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlAccessorType(XmlAccessType.PROPERTY)
@JsonInclude()
public class PSCategoryNode extends PSAbstractDataObject
    implements Comparable<PSCategoryNode>, Cloneable {

  @JsonProperty("id")
  private String id;

  @JsonProperty("title")
  private String title;

  @JsonProperty(value = "selectable", defaultValue = "true")
  private boolean selectable = true;

  @JsonProperty("previousCategoryName")
  private String previousCategoryName;

  @JsonProperty private boolean showInPgMetaData = true;

  @JsonProperty private boolean initialViewCollapsed = true;

  @JsonProperty private String createdBy;

  @JsonProperty("creationDate")
  @JsonSerialize(using = LocalDateSerializer.class)
  @JsonDeserialize(using = LocalDateDeserializer.class)
  private LocalDateTime creationDate;

  @JsonProperty private String lastModifiedBy;

  @JsonProperty("lastModifiedDate")
  @JsonSerialize(using = LocalDateSerializer.class)
  @JsonDeserialize(using = LocalDateDeserializer.class)
  private LocalDateTime lastModifiedDate;

  @JsonProperty
  @JsonSerialize(using = LocalDateSerializer.class)
  @JsonDeserialize(using = LocalDateDeserializer.class)
  private LocalDateTime publishDate;

  @JsonProperty private boolean deleted = false;

  @JsonProperty("children")
  private List<PSCategoryNode> childNodes = new ArrayList<>();

  @JsonProperty private boolean selected = false;

  @JsonProperty private String oldId;

  @JsonProperty private String allowedSites;

  public PSCategoryNode() {
    super();
  }

  @XmlElement(name = "Child")
  public List<PSCategoryNode> getChildNodes() {
    return childNodes;
  }

  public void setChildNodes(List<PSCategoryNode> children) {
    this.childNodes = children;
  }

  @XmlAttribute(name = "id")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @XmlAttribute(name = "title")
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  @XmlAttribute(name = "selectable")
  public boolean isSelectable() {
    return selectable;
  }

  public void setSelectable(boolean selectable) {
    this.selectable = selectable;
  }

  @XmlAttribute(name = "previousCategoryName")
  public String getPreviousCategoryName() {
    return previousCategoryName;
  }

  public void setPreviousCategoryName(String previousCategoryName) {
    this.previousCategoryName = previousCategoryName;
  }

  @XmlAttribute(name = "showInPgMetaData")
  public boolean isShowInPgMetaData() {
    return showInPgMetaData;
  }

  public void setShowInPgMetaData(boolean showInPgMetaData) {
    this.showInPgMetaData = showInPgMetaData;
  }

  @XmlAttribute(name = "initialViewCollapsed")
  public boolean isInitialViewCollapsed() {
    return initialViewCollapsed;
  }

  public void setInitialViewCollapsed(boolean initialViewCollapsed) {
    this.initialViewCollapsed = initialViewCollapsed;
  }

  @XmlAttribute(name = "createdBy")
  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  @XmlAttribute(name = "creationDate")
  @XmlJavaTypeAdapter(PSDateAdapter.class)
  public LocalDateTime getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(LocalDateTime creationDate) {
    this.creationDate = creationDate;
  }

  @XmlAttribute(name = "lastModifiedBy")
  public String getLastModifiedBy() {
    return lastModifiedBy;
  }

  public void setLastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
  }

  @XmlAttribute(name = "lastModifiedDate")
  @XmlJavaTypeAdapter(PSDateAdapter.class)
  public LocalDateTime getLastModifiedDate() {
    return lastModifiedDate;
  }

  public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }

  @XmlAttribute(name = "publishDate")
  @XmlJavaTypeAdapter(PSDateAdapter.class)
  public LocalDateTime getPublishDate() {
    return publishDate;
  }

  public void setPublishDate(LocalDateTime publishDate) {
    this.publishDate = publishDate;
  }

  @XmlAttribute(name = "deleted")
  public boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

  @XmlTransient
  public boolean isSelected() {
    return selected;
  }

  public void setSelected(boolean selected) {
    this.selected = selected;
  }

  @XmlTransient
  public String getOldId() {
    return oldId;
  }

  public void setOldId(String oldId) {
    this.oldId = oldId;
  }

  @XmlAttribute(name = "allowedSites")
  public String getAllowedSites() {
    return allowedSites;
  }

  public void setAllowedSites(String allowedSites) {
    this.allowedSites = allowedSites;
  }

  @Override
  public String toString() {
    return "PSCategoryNode [id="
        + id
        + ", title="
        + title
        + ", selectable="
        + selectable
        + ", previousCategoryName="
        + previousCategoryName
        + ", showInPgMetaData="
        + showInPgMetaData
        + ", initialViewCollapsed="
        + initialViewCollapsed
        + ", createdBy="
        + createdBy
        + ", creationDate="
        + creationDate
        + ", lastModifiedBy="
        + lastModifiedBy
        + ", lastModifiedDate="
        + lastModifiedDate
        + ", allowedSites="
        + allowedSites
        + ", publishDate="
        + publishDate
        + ", deleted="
        + deleted
        + ", childNodes="
        + childNodes
        + ", selected="
        + selected
        + ", oldId="
        + oldId
        + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    var other = (PSCategoryNode) obj;
    if (id == null) {
      return other.id == null;
    } else return id.equals(other.id);
  }

  @Override
  public PSCategoryNode clone() throws CloneNotSupportedException {
    var categoryNode = (PSCategoryNode) super.clone();
    categoryNode.setId(this.getId());
    categoryNode.setTitle(this.getTitle());
    categoryNode.setCreatedBy(this.getCreatedBy());
    categoryNode.setCreationDate(this.getCreationDate());
    categoryNode.setDeleted(this.isDeleted());
    categoryNode.setInitialViewCollapsed(this.isInitialViewCollapsed());
    categoryNode.setLastModifiedBy(this.getLastModifiedBy());
    categoryNode.setLastModifiedDate(this.getLastModifiedDate());
    categoryNode.setPreviousCategoryName(this.getPreviousCategoryName());
    categoryNode.setSelectable(this.isSelectable());
    categoryNode.setShowInPgMetaData(this.isShowInPgMetaData());
    categoryNode.setOldId(this.getOldId());
    categoryNode.setAllowedSites(this.getAllowedSites());
    if (this.getChildNodes() != null) {
      categoryNode.setChildNodes(new ArrayList<>(this.getChildNodes()));
    }
    return categoryNode;
  }

  @Override
  public int compareTo(PSCategoryNode o) {
    return Collator.getInstance().compare(this.getId(), o.getId());
  }

  /**
   * Hydrate this object from a JSON string.
   *
   * @param json the JSON string
   */
  public void fromJSON(String json) {
    // Not implemented; consider using ObjectMapper.readValue if needed.
  }

  /**
   * Convert this object to a JSON string.
   *
   * @return JSON string representation or null if serialization fails.
   */
  public String toJSON() {
    try {
      var mapper = JsonMapper.builder().build();
      return mapper.writeValueAsString(this);
    } catch (JacksonException e) {
      System.out.println(e.getMessage());
      return null;
    }
  }
}
