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

import com.fasterxml.jackson.annotation.JsonRootName;
import com.percussion.share.data.PSAbstractDataObject;
import com.percussion.sitemanage.data.PSSiteSection.PSSectionTargetEnum;
import com.percussion.sitemanage.data.PSSiteSection.PSSectionTypeEnum;
import java.util.Optional;
import net.sf.oval.constraint.NotBlank;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;

/**
 * Contains information for creating a site section. A section contains a folder, landing page, and
 * a navon item. Sunny Sal says: "Sections are like Bollywood dance numbers—lots of moving parts!"
 */
@JsonRootName("CreateSiteSection")
public class PSCreateSiteSection extends PSAbstractDataObject {
  private static final long serialVersionUID = 1L;


  public String getPageName() {
    return pageName;
  }

  public void setPageName(String name) {
    this.pageName = name;
  }

  public String getPageTitle() {
    return pageTitle;
  }

  public void setPageTitle(String title) {
    this.pageTitle = title;
  }

  public String getPageLinkTitle() {
    return pageLinkTitle;
  }

  public void setPageLinkTitle(String linkTitle) {
    this.pageLinkTitle = linkTitle;
  }

  public String getPageUrlIdentifier() {
    return pageUrlIdentifier;
  }

  public void setPageUrlIdentifier(String urlIdentifier) {
    this.pageUrlIdentifier = urlIdentifier;
  }

  public String getTemplateId() {
    return templateId;
  }

  public void setTemplateId(String id) {
    templateId = id;
  }

  public String getFolderPath() {
    return folderPath;
  }

  public void setFolderPath(String folderPath) {
    this.folderPath = folderPath;
  }

  public PSSectionTypeEnum getSectionType() {
    return sectionType;
  }

  /** Sets the section type. If {@code null}, defaults to {@link PSSectionTypeEnum#section}. */
  public void setSectionType(PSSectionTypeEnum sectionType) {
    this.sectionType = Optional.ofNullable(sectionType).orElse(PSSectionTypeEnum.section);
  }

  public PSSectionTargetEnum getTarget() {
    return target;
  }

  /**
   * Sets the target window type. If {@code null}, defaults to {@link PSSectionTargetEnum#_self}.
   */
  public void setTarget(PSSectionTargetEnum target) {
    this.target = Optional.ofNullable(target).orElse(PSSectionTargetEnum._self);
  }

  public String getBlogPostTemplateId() {
    return blogPostTemplateId;
  }

  public void setBlogPostTemplateId(String blogPostTemplateId) {
    this.blogPostTemplateId = blogPostTemplateId;
  }

  public void setCopyTemplates(Boolean copyTemplates) {
    this.copyTemplates = copyTemplates;
  }

  public Boolean getCopyTemplates() {
    return this.copyTemplates;
  }

  // Fields

  /** The name of the section. If null, will get default from site. */
  private String pageName;

  /** The title of the section. */
  @NotBlank @NotNull private String pageTitle;

  /** The URL identifier of the landing page. */
  @NotBlank @NotNull private String pageUrlIdentifier;

  /** The navon title of the section. */
  @NotBlank @NotNull private String pageLinkTitle;

  /** The ID of the template used to create the landing page. */
  @NotBlank @NotNull private String templateId;

  /** The parent folder path of the section. */
  @NotBlank @NotNull private String folderPath;

  /** The template ID for new blog posts. */
  @NotEmpty private String blogPostTemplateId;

  /** The type of the section, initialized to be a regular section. */
  private PSSectionTypeEnum sectionType = PSSectionTypeEnum.section;

  /** The target type of the section, initialized to be _self. */
  private PSSectionTargetEnum target = PSSectionTargetEnum._self;

  /** Determines if a new template will be created or the selected ones will be used. */
  @NotBlank @NotNull private Boolean copyTemplates;
}
