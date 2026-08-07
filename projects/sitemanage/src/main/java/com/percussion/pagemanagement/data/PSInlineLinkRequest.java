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

import com.percussion.share.data.PSAbstractDataObject;
import jakarta.xml.bind.annotation.XmlRootElement;
import net.sf.oval.constraint.NotBlank;
import net.sf.oval.constraint.NotNull;

/**
 * Represents a request to get an inline resource link. The link service will convert this object to
 * a {@link PSInlineRenderLink}. This object can use legacy template names instead of resource
 * definitions for the inline link generator.
 *
 * @author adamgent
 * @see PSInlineRenderLink
 */
@XmlRootElement(name = "InlineLinkRequest")
public class PSInlineLinkRequest extends PSAbstractDataObject {

  private static final long serialVersionUID = 1L;

  @NotNull @NotBlank private String targetId;
  private String resourceDefinitionId;
  private String thumbResourceDefinitionId;
  /**
   * Optional content field name for the inline link/image {@code title} attribute (control setting
   * {@code InlineLinkTitleField} / query param {@code titleField}). Empty or null = product default
   * (assets: {@code displaytitle}; pages: link title). See #2242 / #946.
   */
  private String titleField;

  /**
   * Gets the ID of the asset resource that we are linking to.
   *
   * @return never {@code null}.
   */
  public String getTargetId() {
    return targetId;
  }

  public void setTargetId(String contentId) {
    this.targetId = contentId;
  }

  public String getThumbResourceDefinitionId() {
    return thumbResourceDefinitionId;
  }

  public void setThumbResourceDefinitionId(String thumbResourceDefinitionId) {
    this.thumbResourceDefinitionId = thumbResourceDefinitionId;
  }

  /**
   * Gets the fully qualified resource definition ID.
   *
   * @return may be {@code null}.
   */
  public String getResourceDefinitionId() {
    return resourceDefinitionId;
  }

  public void setResourceDefinitionId(String resourceDefinitionId) {
    this.resourceDefinitionId = resourceDefinitionId;
  }

  /**
   * Optional field name used to resolve {@link PSInlineRenderLink#getTitle()}.
   *
   * @return may be {@code null} or blank for product defaults
   */
  public String getTitleField() {
    return titleField;
  }

  public void setTitleField(String titleField) {
    this.titleField = titleField;
  }
}
