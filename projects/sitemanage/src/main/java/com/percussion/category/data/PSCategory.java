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
import java.util.ArrayList;
import java.util.List;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/** Represents a category tree for Percussion CMS. */
@XmlRootElement(name = "CategoryTree")
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlAccessorType(XmlAccessType.PROPERTY)
@JsonRootName(value = "CategoryTree")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PSCategory extends PSAbstractDataObject implements Cloneable {
  private static final long serialVersionUID = 1L;

  @JsonProperty private String title;

  @JsonProperty private String allowedSites;

  private List<PSCategoryNode> topLevelNodes = new ArrayList<>();

  @XmlElement(name = "Children")
  @JsonProperty("topLevelNodes")
  @XmlElementWrapper(nillable = true)
  public List<PSCategoryNode> getTopLevelNodes() {
    return topLevelNodes;
  }

  public void setTopLevelNodes(List<PSCategoryNode> children) {
    this.topLevelNodes = children;
  }

  @XmlAttribute(name = "title")
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
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
    return "PSCategory [title="
        + title
        + ", allowedSites="
        + allowedSites
        + ", topLevelNodes="
        + topLevelNodes
        + "]";
  }

  @Override
  public PSCategory clone() throws CloneNotSupportedException {
    var category = (PSCategory) super.clone();
    category.setTitle(this.getTitle());
    if (this.getTopLevelNodes() != null) {
      category.setTopLevelNodes(new ArrayList<>(this.getTopLevelNodes()));
    }
    return category;
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
