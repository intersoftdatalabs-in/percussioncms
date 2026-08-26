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

package com.percussion.rest.contenttypes;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.percussion.rest.DesignGap;
import com.percussion.rest.Guid;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Content type design summary for the Developer module (read + partial write).
 *
 * <p>Field rule expressions use {@code GET/PUT .../fields/{fieldName}/ruleExpressions} (held
 * design lock for write). Control properties use {@code GET/PUT
 * .../fields/{fieldName}/controlProperties}. Partial update supports label, description, enabled,
 * field searchable / occurrence, allowed workflows (+ default), and allowed templates. PUT
 * requires a design-session lock already held by the current user ({@code POST
 * /services/contenttypes/{idOrName}/lock}) and does not release it. Field rule expressions on this
 * detail payload remain summary strings (not written by PUT detail).
 *
 * <p><strong>GET:</strong> adaptors always populate {@code allowedWorkflows} and {@code
 * allowedTemplates} (empty arrays when none) so clients see stable wire shape.
 *
 * <p><strong>PUT:</strong> {@code allowedWorkflows} / {@code allowedTemplates} {@code null}
 * (omitted) leaves associations unchanged; a non-null list (including empty) is a full replace.
 * Field patches use a different convention — empty/omitted {@code fields} means no field changes.
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

  /**
   * Workflow associations. On GET always non-null (may be empty). On PUT request body: null/omitted
   * = leave unchanged; non-null = full replace.
   */
  @Schema(
      description =
          "Allowed workflows. GET: always present (may be []). PUT: omit/null leave unchanged;"
              + " non-null list full replace (empty clears).")
  private List<NamedObjectRef> allowedWorkflows;

  private NamedObjectRef defaultWorkflow;

  /**
   * Template associations. On GET always non-null (may be empty). On PUT request body: null/omitted
   * = leave unchanged; non-null = full replace.
   */
  @Schema(
      description =
          "Allowed templates. GET: always present (may be []). PUT: omit/null leave unchanged;"
              + " non-null list full replace (empty clears).")
  private List<NamedObjectRef> allowedTemplates;

  /**
   * Structured design capability gaps (REST-GAPS-01).
   *
   * <p><strong>BREAKING wire change:</strong> previously a free-text {@code string[]} of messages;
   * now an array of {@link DesignGap} objects {@code {code,message}}. Integrators must not treat
   * entries as bare strings. Documented in product-docs {@code developer/rest.md} (Design capability
   * gaps). Unmigrated peer detail resources may still emit string arrays.
   */
  @Schema(
      description =
          "BREAKING (REST-GAPS-01): designGaps is DesignGap[] objects {code,message}, not"
              + " free-text string[]. Structured capability notes vs full Workbench design."
              + " See product-docs developer/rest.md.")
  private List<DesignGap> designGaps = new ArrayList<>();

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
    this.allowedWorkflows = allowedWorkflows;
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
    this.allowedTemplates = allowedTemplates;
  }

  public List<DesignGap> getDesignGaps() {
    return designGaps;
  }

  public void setDesignGaps(List<DesignGap> designGaps) {
    this.designGaps = designGaps != null ? designGaps : new ArrayList<>();
  }
}
