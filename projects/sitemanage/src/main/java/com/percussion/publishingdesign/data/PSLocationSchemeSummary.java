/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "locationScheme")
public class PSLocationSchemeSummary {
  private String schemeId;
  private String name;
  private String description;
  private String contextId;
  private String generator;
  private Long contentTypeId;
  private Long templateId;
  /** modern | legacy | unknown */
  private String schemeType;
  private java.util.List<PSSchemeParameter> parameters;

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

  public java.util.List<PSSchemeParameter> getParameters() {
    return parameters;
  }

  public void setParameters(java.util.List<PSSchemeParameter> parameters) {
    this.parameters = parameters;
  }
}
