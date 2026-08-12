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

import com.percussion.pagemanagement.data.PSResourceDefinitionGroup.PSResourceDefinition;
import com.percussion.pagemanagement.data.PSResourceDefinitionGroup.PSResourceDefinitionType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.Objects;

// removed Optional import: getter now returns nullable value

/**
 * Represents a rendered link.
 *
 * @author adamgent
 */
@XmlRootElement(name = "RenderLink")
public class PSRenderLink implements Serializable {

  private static final long serialVersionUID = 1L;

  protected String url;
  private transient PSResourceDefinition resourceDefinition;
  private PSResourceDefinitionType resourceType;
  private String resourceDefinitionId;

  public PSRenderLink() {
    // Default constructor
  }

  public PSRenderLink(String url, PSResourceDefinition resourceDefinition) {
    this.url = url;
    applyResourceDefinition(resourceDefinition);
  }

  @XmlTransient
  public PSResourceDefinition getResourceDefinition() {
    return resourceDefinition;
  }

  public void setResourceDefinition(PSResourceDefinition resourceDefinition) {
    applyResourceDefinition(resourceDefinition);
  }

  /**
   * Shared constructor/setter path so derived fields stay in lockstep without overridable calls
   * during construction (this-escape safe).
   */
  private final void applyResourceDefinition(PSResourceDefinition resourceDefinition) {
    this.resourceDefinition = resourceDefinition;
    if (resourceDefinition != null) {
      this.resourceType = resourceDefinition.getResourceType();
      this.resourceDefinitionId = resourceDefinition.getUniqueId();
    }
  }

  /**
   * Gets the URL for this link.
   *
   * @return maybe {@code null}.
   */
  public String getUrl() {
    return url;
  }

  /**
   * Sets the URL for this link.
   *
   * @param url the URL string
   */
  public final void setUrl(String url) {
    this.url = url;
  }

  /**
   * Gets the resource definition unique id for this link. This may be null if the link was created
   * outside of the link service.
   *
   * @return maybe {@code null}.
   */
  public String getResourceDefinitionId() {
    return resourceDefinitionId;
  }

  public void setResourceDefinitionId(String resourceDefinitionId) {
    this.resourceDefinitionId = resourceDefinitionId;
  }

  /**
   * Gets the resource type.
   *
   * @return maybe {@code null}.
   */
  public PSResourceDefinitionType getResourceType() {
    return resourceType;
  }

  public void setResourceType(PSResourceDefinitionType resourceType) {
    this.resourceType = resourceType;
  }

  @Override
  public String toString() {
    return Objects.toString(url, "");
  }
}
