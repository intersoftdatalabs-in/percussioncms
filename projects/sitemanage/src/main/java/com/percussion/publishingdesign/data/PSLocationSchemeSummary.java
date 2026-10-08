/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
package com.percussion.publishingdesign.data;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "locationScheme")
public class PSLocationSchemeSummary {
  private String schemeId;
  private String name;
  private String description;
  private String contextId;
  private String generator;
  private Long contentTypeId;
  private Long templateId;

  /**
   * True when this create is a copy of an existing scheme. The context,
   * template, and content type triple is unique, so a copy must not reuse an
   * occupied assignment.
   */
  private Boolean copy;

  /** modern | legacy | unknown */
  private String schemeType;

  private List<PSSchemeParameter> parameters;

  /**
   * When true, {@code parameters} is exactly one parameter to append. Stored parameters stay.
   * Other scheme fields are still applied only when present on the body.
   */
  private Boolean addParameter;

  /**
   * When true, {@code parameters} is exactly one parameter to remove by name. Other stored
   * parameters stay. Other scheme fields are still applied only when present on the body.
   * Cannot be combined with {@link #addParameter}, {@link #updateParameterValue},
   * {@link #updateParameterType}, or {@link #updateParameterSequence}.
   */
  private Boolean removeParameter;

  /**
   * When true, {@code parameters} is exactly one stored parameter whose value is replaced. Name,
   * type, and sequence of that parameter stay. Other stored parameters stay. Other scheme fields
   * are still applied only when present on the body. Cannot be combined with {@link #addParameter},
   * {@link #removeParameter}, {@link #updateParameterType}, or {@link #updateParameterSequence}.
   */
  private Boolean updateParameterValue;

  /**
   * When true, {@code parameters} is exactly one stored parameter whose type is replaced. Name,
   * value, and sequence of that parameter stay. Other stored parameters stay. Other scheme fields
   * are still applied only when present on the body. Cannot be combined with {@link #addParameter},
   * {@link #removeParameter}, {@link #updateParameterValue}, or {@link #updateParameterSequence}.
   */
  private Boolean updateParameterType;

  /**
   * When true, {@code parameters} is exactly one stored parameter whose sequence is replaced. Name,
   * type, and value of that parameter stay. Other stored parameters stay, including their
   * sequences. Other scheme fields are still applied only when present on the body. Cannot be
   * combined with {@link #addParameter}, {@link #removeParameter}, {@link #updateParameterValue},
   * or {@link #updateParameterType}.
   */
  private Boolean updateParameterSequence;

  public String getSchemeId() {
    return schemeId;
  }

  public void setSchemeId(String schemeId) {
    this.schemeId = schemeId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getContextId() {
    return contextId;
  }

  public void setContextId(String contextId) {
    this.contextId = contextId;
  }

  public String getGenerator() {
    return generator;
  }

  public void setGenerator(String generator) {
    this.generator = generator;
  }

  public String getSchemeType() {
    return schemeType;
  }

  public void setSchemeType(String schemeType) {
    this.schemeType = schemeType;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Long getContentTypeId() {
    return contentTypeId;
  }

  public void setContentTypeId(Long contentTypeId) {
    this.contentTypeId = contentTypeId;
  }

  public Long getTemplateId() {
    return templateId;
  }

  public void setTemplateId(Long templateId) {
    this.templateId = templateId;
  }

  public Boolean getCopy() {
    return copy;
  }

  public void setCopy(Boolean copy) {
    this.copy = copy;
  }

  @XmlElementWrapper(name = "parameters")
  @XmlElement(name = "schemeParameter")
  public List<PSSchemeParameter> getParameters() {
    return parameters;
  }

  public void setParameters(List<PSSchemeParameter> parameters) {
    this.parameters = parameters;
  }

  public Boolean getAddParameter() {
    return addParameter;
  }

  public void setAddParameter(Boolean addParameter) {
    this.addParameter = addParameter;
  }

  public Boolean getRemoveParameter() {
    return removeParameter;
  }

  public void setRemoveParameter(Boolean removeParameter) {
    this.removeParameter = removeParameter;
  }

  public Boolean getUpdateParameterValue() {
    return updateParameterValue;
  }

  public void setUpdateParameterValue(Boolean updateParameterValue) {
    this.updateParameterValue = updateParameterValue;
  }

  public Boolean getUpdateParameterType() {
    return updateParameterType;
  }

  public void setUpdateParameterType(Boolean updateParameterType) {
    this.updateParameterType = updateParameterType;
  }

  public Boolean getUpdateParameterSequence() {
    return updateParameterSequence;
  }

  public void setUpdateParameterSequence(Boolean updateParameterSequence) {
    this.updateParameterSequence = updateParameterSequence;
  }
}
