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
package com.percussion.pagemanagement.data;

import com.percussion.pagemanagement.data.PSResourceDefinitionGroup.PSResourceDefinition;
import com.percussion.pagemanagement.data.PSResourceDefinitionGroup.PSResourceDefinitionType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

/**
 * Represents an inline link inside a rich text editor. There are many legacy properties that are
 * needed for the inline link parser that will be removed someday.
 *
 * @author adamgent
 */
@XmlRootElement(name = "InlineRenderLink")
public class PSInlineRenderLink extends PSRenderLink {

  private static final long serialVersionUID = 1L;

  private String targetId;
  private String thumbUrl;
  private String title;
  private String altText;
  private transient PSResourceDefinition thumbResourceDefinition;
  private PSResourceDefinitionType thumbResourceType;
  private String thumbResourceDefinitionId;

  @Deprecated private Integer legacyDependentVariantId;
  @Deprecated private Integer legacyThumbDependentVariantId;
  @Deprecated private String legacyRxInlineSlot;
  @Deprecated private Integer legacyDependentId;
  @Deprecated private String inlineType;

  private String stateClass;

  public PSInlineRenderLink() {
    this.url = "";
    this.thumbUrl = "";
    this.title = "";
    this.stateClass = "";
  }

  /**
   * Gets the title for tool tip from item.
   *
   * @return may be {@code null}.
   */
  public String getTitle() {
    return title;
  }

  /**
   * Sets the title for tool tip from item.
   *
   * @param title may be {@code null}.
   */
  public void setTitle(String title) {
    this.title = title;
  }

  @Deprecated
  @XmlElement(name = "sys_dependentvariantid")
  public Integer getLegacyDependentVariantId() {
    return legacyDependentVariantId;
  }

  @Deprecated
  public void setLegacyDependentVariantId(Integer sys_dependentvariantid) {
    this.legacyDependentVariantId = sys_dependentvariantid;
  }

  @Deprecated
  @XmlElement(name = "rxinlineslot")
  public String getLegacyRxInlineSlot() {
    return legacyRxInlineSlot;
  }

  @Deprecated
  public void setLegacyRxInlineSlot(String rxinlineslot) {
    this.legacyRxInlineSlot = rxinlineslot;
  }

  @Deprecated
  @XmlElement(name = "sys_dependentid")
  public Integer getLegacyDependentId() {
    return legacyDependentId;
  }

  @Deprecated
  public void setLegacyDependentId(Integer sys_dependentid) {
    this.legacyDependentId = sys_dependentid;
  }

  @XmlElement(name = "inlinetype")
  @Deprecated
  public String getInlineType() {
    return inlineType;
  }

  @Deprecated
  public void setInlineType(String inlinetype) {
    this.inlineType = inlinetype;
  }

  /**
   * Gets the thumbnail URL if image has a thumbnail.
   *
   * @return may be {@code null}.
   */
  public String getThumbUrl() {
    return thumbUrl;
  }

  /**
   * Sets the thumbnail URL if image has a thumbnail.
   *
   * @param thumbUrl never {@code null}.
   */
  public void setThumbUrl(String thumbUrl) {
    this.thumbUrl = thumbUrl;
  }

  /**
   * Gets the alt text used for the img HTML tag.
   *
   * @return never {@code null}.
   */
  public String getAltText() {
    return altText;
  }

  /**
   * Sets the alt text used for the img HTML tag.
   *
   * @param altText never {@code null}.
   */
  public void setAltText(String altText) {
    this.altText = altText;
  }

  @Deprecated
  @XmlElement(name = "thumbsys_dependentvariantid")
  public Integer getLegacyThumbDependentVariantId() {
    return legacyThumbDependentVariantId;
  }

  @Deprecated
  public void setLegacyThumbDependentVariantId(Integer thumbsys_dependentvariantid) {
    this.legacyThumbDependentVariantId = thumbsys_dependentvariantid;
  }

  /**
   * Gets the ID of the object that link is pointing to.
   *
   * @return never {@code null}.
   */
  public String getTargetId() {
    return targetId;
  }

  public void setTargetId(String pageId) {
    this.targetId = pageId;
  }

  @XmlTransient
  public PSResourceDefinition getThumbResourceDefinition() {
    return thumbResourceDefinition;
  }

  public void setThumbResourceDefinition(PSResourceDefinition thumbResourceDefinition) {
    this.thumbResourceDefinition = thumbResourceDefinition;
    if (thumbResourceDefinition != null) {
      setResourceType(thumbResourceDefinition.getResourceType());
      setThumbResourceDefinitionId(thumbResourceDefinition.getUniqueId());
    }
  }

  public PSResourceDefinitionType getThumbResourceType() {
    return thumbResourceType;
  }

  public void setThumbResourceType(PSResourceDefinitionType thumbResourceType) {
    this.thumbResourceType = thumbResourceType;
  }

  public String getThumbResourceDefinitionId() {
    return thumbResourceDefinitionId;
  }

  public void setThumbResourceDefinitionId(String thumbResourceDefinitionId) {
    this.thumbResourceDefinitionId = thumbResourceDefinitionId;
  }

  public String getStateClass() {
    return this.stateClass;
  }

  public void setStateClass(String stateClass) {
    this.stateClass = stateClass;
  }
}
