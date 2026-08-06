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
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/** Read-only field summary for a content type (Developer module design view). */
@XmlRootElement(name = "ContentTypeField")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Content type field summary (read-only design catalog)")
public class ContentTypeField {

  @Schema(description = "Field submit name (system name)")
  private String name;

  @Schema(description = "Display label when known")
  private String label;

  @Schema(description = "Field origin: local | system | shared | unknown")
  private String fieldType;

  @Schema(description = "Storage / data type (text, integer, date, binary, …)")
  private String dataType;

  @Schema(description = "True when field participates in search indexing")
  private Boolean searchable;

  @Schema(description = "True for required fields when known")
  private Boolean required;

  @Schema(description = "True when the field is read-only in the content editor")
  private Boolean readOnly;

  @Schema(
      description =
          "Occurrence dimension: optional | required | oneOrMore | zeroOrMore | count | unknown")
  private String occurrence;

  @Schema(description = "True when field validation rules are defined")
  private Boolean hasValidation;

  @Schema(description = "True when visibility rules are defined")
  private Boolean hasVisibilityRules;

  @Schema(description = "True when an input translation is defined")
  private Boolean hasInputTranslation;

  @Schema(description = "True when an output translation is defined")
  private Boolean hasOutputTranslation;

  @Schema(description = "Control name when resolved from display mapping")
  private String control;

  @Schema(description = "Child field-set name when this field is nested; null for parent fields")
  private String fieldSet;

  public ContentTypeField() {}

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

  public String getFieldType() {
    return fieldType;
  }

  public void setFieldType(String fieldType) {
    this.fieldType = fieldType;
  }

  public String getDataType() {
    return dataType;
  }

  public void setDataType(String dataType) {
    this.dataType = dataType;
  }

  public Boolean getSearchable() {
    return searchable;
  }

  public void setSearchable(Boolean searchable) {
    this.searchable = searchable;
  }

  public Boolean getRequired() {
    return required;
  }

  public void setRequired(Boolean required) {
    this.required = required;
  }

  public Boolean getReadOnly() {
    return readOnly;
  }

  public void setReadOnly(Boolean readOnly) {
    this.readOnly = readOnly;
  }

  public String getOccurrence() {
    return occurrence;
  }

  public void setOccurrence(String occurrence) {
    this.occurrence = occurrence;
  }

  public Boolean getHasValidation() {
    return hasValidation;
  }

  public void setHasValidation(Boolean hasValidation) {
    this.hasValidation = hasValidation;
  }

  public Boolean getHasVisibilityRules() {
    return hasVisibilityRules;
  }

  public void setHasVisibilityRules(Boolean hasVisibilityRules) {
    this.hasVisibilityRules = hasVisibilityRules;
  }

  public Boolean getHasInputTranslation() {
    return hasInputTranslation;
  }

  public void setHasInputTranslation(Boolean hasInputTranslation) {
    this.hasInputTranslation = hasInputTranslation;
  }

  public Boolean getHasOutputTranslation() {
    return hasOutputTranslation;
  }

  public void setHasOutputTranslation(Boolean hasOutputTranslation) {
    this.hasOutputTranslation = hasOutputTranslation;
  }

  public String getControl() {
    return control;
  }

  public void setControl(String control) {
    this.control = control;
  }

  public String getFieldSet() {
    return fieldSet;
  }

  public void setFieldSet(String fieldSet) {
    this.fieldSet = fieldSet;
  }
}
