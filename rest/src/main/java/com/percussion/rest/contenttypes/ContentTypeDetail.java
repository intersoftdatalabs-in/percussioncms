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

package com.percussion.rest.contenttypes;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.percussion.rest.Guid;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Content type design summary for the Developer module (read + partial write).
 *
 * <p>Full field rule expressions and control properties remain Workbench / SOAP parity
 * work. Partial update supports label, description, enabled, and field searchable /
 * occurrence under a design-session lock.
 */
@XmlRootElement(name = "ContentTypeDetail")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Content type detail with field catalog")
public class ContentTypeDetail {

  private Guid guid;
  private String name;
  private String label;
  private String description;
  private Boolean enabled;
  private Boolean hideFromMenu;
  private String appName;
  private String editorUrl;
  private List<ContentTypeField> fields = new ArrayList<>();
  private List<String> childFieldSets = new ArrayList<>();
  private List<NamedObjectRef> allowedWorkflows = new ArrayList<>();
  private NamedObjectRef defaultWorkflow;
  private List<NamedObjectRef> allowedTemplates = new ArrayList<>();

  @Schema(
      description =
          "Capability notes for clients — what this payload includes vs full Workbench design object")
  private List<String> designGaps = new ArrayList<>();

  public ContentTypeDetail() {}

  public Guid getGuid() {
    return guid;
  }

  public void setGuid(Guid guid) {
    this.guid = guid;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public Boolean getHideFromMenu() {
    return hideFromMenu;
  }

  public void setHideFromMenu(Boolean hideFromMenu) {
    this.hideFromMenu = hideFromMenu;
  }

  public String getAppName() {
    return appName;
  }

  public void setAppName(String appName) {
    this.appName = appName;
  }

  public String getEditorUrl() {
    return editorUrl;
  }

  public void setEditorUrl(String editorUrl) {
    this.editorUrl = editorUrl;
  }

  public List<ContentTypeField> getFields() {
    return fields;
  }

  public void setFields(List<ContentTypeField> fields) {
    this.fields = fields != null ? fields : new ArrayList<>();
  }

  public List<String> getChildFieldSets() {
    return childFieldSets;
  }

  public void setChildFieldSets(List<String> childFieldSets) {
    this.childFieldSets = childFieldSets != null ? childFieldSets : new ArrayList<>();
  }

  public List<NamedObjectRef> getAllowedWorkflows() {
    return allowedWorkflows;
  }

  public void setAllowedWorkflows(List<NamedObjectRef> allowedWorkflows) {
    this.allowedWorkflows = allowedWorkflows != null ? allowedWorkflows : new ArrayList<>();
  }

  public NamedObjectRef getDefaultWorkflow() {
    return defaultWorkflow;
  }

  public void setDefaultWorkflow(NamedObjectRef defaultWorkflow) {
    this.defaultWorkflow = defaultWorkflow;
  }

  public List<NamedObjectRef> getAllowedTemplates() {
    return allowedTemplates;
  }

  public void setAllowedTemplates(List<NamedObjectRef> allowedTemplates) {
    this.allowedTemplates = allowedTemplates != null ? allowedTemplates : new ArrayList<>();
  }

  public List<String> getDesignGaps() {
    return designGaps;
  }

  public void setDesignGaps(List<String> designGaps) {
    this.designGaps = designGaps != null ? designGaps : new ArrayList<>();
  }
}
