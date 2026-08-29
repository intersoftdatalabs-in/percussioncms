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

/** Dependent field used to filter a dynamic choice catalog (CD-07). */
@XmlRootElement(name = "ContentTypeChoiceFilterField")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Dependent field for a content type field choice filter")
public class ContentTypeChoiceFilterField {

  @Schema(required = true, description = "Field submit name used as a filter input")
  private String fieldRef;

  @Schema(required = true, description = "optional | required")
  private String dependencyType;

  public ContentTypeChoiceFilterField() {}

  public ContentTypeChoiceFilterField(String fieldRef, String dependencyType) {
    this.fieldRef = fieldRef;
    this.dependencyType = dependencyType;
  }

  public String getFieldRef() {
    return fieldRef;
  }

  public void setFieldRef(String fieldRef) {
    this.fieldRef = fieldRef;
  }

  public String getDependencyType() {
    return dependencyType;
  }

  public void setDependencyType(String dependencyType) {
    this.dependencyType = dependencyType;
  }
}
