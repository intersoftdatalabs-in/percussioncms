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
import java.util.ArrayList;
import java.util.List;

/**
 * One field validation or visibility rule (conditional set, extension call, or named rule
 * reference).
 *
 * <p>{@code type} is {@code conditional}, {@code extension}, or {@code reference}. GET {@code
 * summary} is a human-readable dump and is ignored on PUT.
 */
@XmlRootElement(name = "ContentTypeFieldRule")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "One field validation or visibility rule")
public class ContentTypeFieldRule {

  public static final String TYPE_CONDITIONAL = "conditional";
  public static final String TYPE_EXTENSION = "extension";
  public static final String TYPE_REFERENCE = "reference";

  @Schema(
      required = true,
      description = "Rule kind: conditional | extension | reference. Required on PUT.")
  private String type;

  @Schema(description = "Conditionals when type is conditional. Required and non-empty on PUT.")
  private List<ContentTypeFieldConditional> conditionals;

  @Schema(
      description =
          "Fully-qualified extension ref when type is extension, e.g."
              + " Java/global/percussion/generic/sys_ToUpperCase")
  private String extension;

  @Schema(description = "Short extension name (GET convenience; ignored on PUT when extension is set)")
  private String name;

  @Schema(description = "Extension call parameters (literal values) when type is extension")
  private List<ContentTypeItemExitParam> parameters = new ArrayList<>();

  @Schema(description = "Named validation rule reference when type is reference")
  private String reference;

  @Schema(description = "Human-readable summary (GET); ignored on PUT")
  private String summary;

  public ContentTypeFieldRule() {}

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public List<ContentTypeFieldConditional> getConditionals() {
    return conditionals;
  }

  public void setConditionals(List<ContentTypeFieldConditional> conditionals) {
    this.conditionals = conditionals;
  }

  public String getExtension() {
    return extension;
  }

  public void setExtension(String extension) {
    this.extension = extension;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<ContentTypeItemExitParam> getParameters() {
    return parameters;
  }

  public void setParameters(List<ContentTypeItemExitParam> parameters) {
    this.parameters = parameters != null ? parameters : new ArrayList<>();
  }

  public String getReference() {
    return reference;
  }

  public void setReference(String reference) {
    this.reference = reference;
  }

  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }
}
