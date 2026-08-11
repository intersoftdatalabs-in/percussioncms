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
package com.percussion.sitemanage.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.percussion.share.data.PSAbstractPersistantObject;
import com.percussion.sitemanage.data.PSSiteSection.PSSectionTypeEnum;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The section node contains summary information of a section and all direct child section nodes.
 * This can be used to construct a tree of sections for a specific site.
 */
@XmlRootElement(name = "SectionNode")
@JsonRootName("SectionNode")
public class PSSectionNode extends PSAbstractPersistantObject {

  private static final long serialVersionUID = 1L;

  private ArrayList<PSSectionNode> childNodes = new ArrayList<>();
  private String title;
  private String folderPath;
  private String id;
  private PSSectionTypeEnum sectionType = PSSectionTypeEnum.section;
  private boolean requiresLogin;
  private String allowAccessTo;

  @Override
  @XmlElement
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Gets the title of the section.
   *
   * @return the title of the section.
   */
  public String getTitle() {
    return title;
  }

  /**
   * Sets the title of the section.
   *
   * @param title the new title.
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * @return the section type
   */
  public PSSectionTypeEnum getSectionType() {
    return sectionType;
  }

  /**
   * @param sectionType to set, if null set to {@link PSSectionTypeEnum#section}
   */
  public void setSectionType(PSSectionTypeEnum sectionType) {
    this.sectionType = sectionType == null ? PSSectionTypeEnum.section : sectionType;
  }

  /**
   * Gets all (direct) child nodes.
   *
   * @return child nodes, never null, but may be empty.
   */
  @XmlElementWrapper(name = "childNodes")
  @XmlElements({@XmlElement(name = "SectionNode", type = PSSectionNode.class)})
  public List<PSSectionNode> getChildNodes() {
    return childNodes;
  }

  /**
   * Sets direct child nodes.
   *
   * @param nodes the new list of child nodes, null treated as empty list.
   */
  @SuppressWarnings("unchecked")
  public void setChildNodes(List<PSSectionNode> nodes) {
    if (nodes == null) {
      this.childNodes = new ArrayList<>();
    } else if (nodes instanceof ArrayList) {
      this.childNodes = (ArrayList<PSSectionNode>) nodes;
    } else {
      this.childNodes = new ArrayList<>(nodes);
    }
  }

  public boolean isRequiresLogin() {
    return requiresLogin;
  }

  public void setRequiresLogin(boolean requiresLogin) {
    this.requiresLogin = requiresLogin;
  }

  public Optional<String> getAllowAccessTo() {
    return Optional.ofNullable(allowAccessTo);
  }

  public void setAllowAccessTo(String allowAccessTo) {
    this.allowAccessTo = allowAccessTo;
  }

  public Optional<String> getFolderPath() {
    return Optional.ofNullable(folderPath);
  }

  public void setFolderPath(String folderPath) {
    this.folderPath = folderPath;
  }
}
