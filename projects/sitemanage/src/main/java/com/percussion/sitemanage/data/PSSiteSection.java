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

package com.percussion.sitemanage.data;

import com.percussion.share.data.IPSFolderPath;
import com.percussion.share.data.PSAbstractPersistantObject;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.sf.oval.constraint.NotNull;

/**
 * Represents a site section in the architecture page. The site section is comprised of the folder,
 * navon (navtree for site home page), and landing page.
 *
 * <p>Sunny Sal says: "A section a day keeps the chaos away!"
 */
@XmlRootElement(name = "SiteSection")
public class PSSiteSection extends PSAbstractPersistantObject implements IPSFolderPath {
  private static final long serialVersionUID = -1L;

  /** The navigation title. Also the link title of the landing page. */
  private String title;

  /** The string representation of the GUID of the navon item of the section. */
  @XmlElement private String id;

  /** The folder path for this section. */
  private String folderPath;

  /** The URL of the external link section. Meaningful only when type is External Link. */
  private String externalLinkUrl;

  @NotNull private PSSectionTypeEnum sectionType = PSSectionTypeEnum.section;

  @NotNull private PSSectionTargetEnum target = PSSectionTargetEnum._self;

  /** The IDs of sub-sections. */
  private ArrayList<String> childIds = new ArrayList<>();

  /** Field to note if the section requires login. */
  private boolean requiresLogin;

  /** Field to save the groups that are allowed to enter the section. */
  private String allowAccessTo;

  /** Field to save the CSS class names used when rendering navigation widgets. */
  private String cssClassNames;

  private String displayTitlePath;

  /**
   * Gets the title of the section. It is the navigation title of the navigation node and the link
   * title of the landing page of the node.
   *
   * @return the title of the section. It should not be blank for a properly configured section.
   */
  public String getTitle() {
    return title;
  }

  /**
   * Sets the title of the section.
   *
   * @param title the new title. It should not be blank for a valid section.
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * Gets the IDs of the sub-sections.
   *
   * @return an unmodifiable list of sub-section IDs. May be empty, never null.
   */
  public List<String> getChildIds() {
    return childIds == null ? Collections.emptyList() : Collections.unmodifiableList(childIds);
  }

  /**
   * Sets the IDs of sub-sections.
   *
   * @param ids the IDs of sub-sections, may be null or empty.
   */
  @SuppressWarnings("unchecked")
  public void setChildIds(List<String> ids) {
    if (ids == null) {
      this.childIds = new ArrayList<>();
    } else if (ids instanceof ArrayList) {
      this.childIds = (ArrayList<String>) ids;
    } else {
      this.childIds = new ArrayList<>(ids);
    }
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getFolderPath() {
    return folderPath;
  }

  public void setFolderPath(String folderPath) {
    this.folderPath = folderPath;
  }

  /**
   * @return external link URL, may be null if not set. Meaningful only for external link sections.
   */
  public Optional<String> getExternalLinkUrl() {
    return Optional.ofNullable(externalLinkUrl);
  }

  /**
   * @param externalLinkUrl external link URL, may be null or empty.
   */
  public void setExternalLinkUrl(String externalLinkUrl) {
    this.externalLinkUrl = externalLinkUrl;
  }

  /**
   * @return the section type, never null.
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
   * @return the target type of the section, never null.
   */
  public PSSectionTargetEnum getTarget() {
    return target;
  }

  /**
   * @param target The target window type to set, if null initialized to {@link
   *     PSSectionTargetEnum#_self}
   */
  public void setTarget(PSSectionTargetEnum target) {
    this.target = target == null ? PSSectionTargetEnum._self : target;
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

  /**
   * @param cssClassNames the class names used with navigation widget.
   */
  public void setCssClassNames(String cssClassNames) {
    this.cssClassNames = cssClassNames;
  }

  /**
   * Gets the CSS class names of the section folder.
   *
   * @return the CSS class names used with navigation widget.
   */
  public Optional<String> getCssClassNames() {
    return Optional.ofNullable(cssClassNames);
  }

  /**
   * Setter for the display title property.
   *
   * @param displayTitlePath the displayTitlePath to set, may be null or empty.
   * @see #getDisplayTitlePath()
   */
  public void setDisplayTitlePath(String displayTitlePath) {
    this.displayTitlePath = displayTitlePath;
  }

  /**
   * The display title path of this section. The display title path is only available for section
   * link nodes. This path is formed by the 'display title' of the root to the current node.
   *
   * @return the display title path. May be null or empty if the node is not a section link.
   */
  public Optional<String> getDisplayTitlePath() {
    return Optional.ofNullable(displayTitlePath);
  }

  /** Describes the type of section. */
  public enum PSSectionTypeEnum {
    /** Regular section. */
    section,
    /** Link to a regular section. */
    sectionlink,
    /** External link type section. */
    externallink,
    /** Blog section. */
    blog
  }

  public enum PSSectionTargetEnum {
    /** Default option. */
    _self,
    /** New window option. */
    _blank,
    /** Top window option. */
    _top,
    /** Parent window option. */
    _parent
  }
}
