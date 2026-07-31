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
package com.percussion.pagemanagement.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import tools.jackson.databind.annotation.JsonSerialize;
import com.percussion.share.data.PSAbstractPersistantObject;
import jakarta.xml.bind.annotation.*;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;

/**
 * The summary information of a Template. This is an immutable class.
 *
 * @author YuBingChen, Sunny Sal
 */
@XmlRootElement(name = "TemplateSummary")
@JsonSerialize(as = PSTemplateSummary.class)
@JsonRootName("TemplateSummary")
public class PSTemplateSummary extends PSAbstractPersistantObject {

  private static final long serialVersionUID = -2647068336786632480L;

  protected static final int MAX_DESCRIPTION = -1;
  protected static final int MAX_SOURCE_TEMPLATE = 200;
  protected static final int MAX_LABEL = 300;
  protected static final int MAX_THEME = 300;
  protected static final int MAX_PROTECTED_REGION = 200;
  protected static final int MAX_DOCTYPE = 1000;
  protected static final int MAX_TYPE = 100;

  @NotEmpty private String id;

  @NotNull @NotEmpty private String name;

  private String label;
  private String description;
  private String imageThumbPath;
  private boolean isReadOnly;
  private String sourceTemplateName;
  private String type;
  private String contentMigrationVersion = "0";

  public PSTemplateSummary() {
    super();
  }

  public PSTemplateSummary(PSTemplate template) {
    this.contentMigrationVersion = template.getContentMigrationVersion();
    this.description = template.getDescription();
    this.id = template.getId();
    this.imageThumbPath = template.getImageThumbPath();
    this.isReadOnly = template.isReadOnly();
    this.label = template.getLabel();
    this.type = template.getType();
    this.name = template.getName();
    this.sourceTemplateName = template.getSourceTemplateName();
  }

  @Override
  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getLabel() {
    return label;
  }

  public String getDescription() {
    return description;
  }

  public String getImageThumbPath() {
    return imageThumbPath;
  }

  public void setImageThumbPath(String imageThumbPath) {
    this.imageThumbPath = imageThumbPath;
  }

  public boolean isReadOnly() {
    return isReadOnly;
  }

  public void setReadOnly(boolean isReadOnly) {
    this.isReadOnly = isReadOnly;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setLabel(String label) {
    if (label != null && label.length() > MAX_LABEL) {
      label = label.substring(0, MAX_LABEL);
    }
    this.label = label;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Gets the name of the source template. The source template was used to create this template.
   *
   * @return the source template name, may be null or empty.
   */
  public String getSourceTemplateName() {
    return sourceTemplateName;
  }

  /**
   * Sets the name of the source template. The source template was used to create this template.
   *
   * @param srcTemplate the new source template name, never null or empty.
   */
  public void setSourceTemplateName(String srcTemplate) {
    if (srcTemplate != null && srcTemplate.length() > MAX_SOURCE_TEMPLATE) {
      srcTemplate = srcTemplate.substring(0, MAX_SOURCE_TEMPLATE);
    }
    this.sourceTemplateName = srcTemplate;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    if (type != null && type.length() > MAX_TYPE) {
      type = type.substring(0, MAX_TYPE);
    }
    this.type = type;
  }

  /**
   * Get the version that is incremented each time the template is saved with changes that require
   * content migration for pages using the template.
   *
   * @return the version, 0 if no such changes have been saved.
   */
  public String getContentMigrationVersion() {
    return contentMigrationVersion;
  }

  /**
   * Set the content migration version. See {@link #getContentMigrationVersion()}.
   *
   * @param version the version to set.
   */
  public void setContentMigrationVersion(String version) {
    this.contentMigrationVersion = version;
  }
}
