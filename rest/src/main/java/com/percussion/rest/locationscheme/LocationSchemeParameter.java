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

package com.percussion.rest.locationscheme;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Represents a Location Scheme Parameter.
 *
 * <p>Wire getters return plain types (not {@code Optional}) so Jackson/CXF JSON emits {@code name}
 * and {@code value} when set. Optional-returning getters historically serialized as empty/present
 * beans or dropped fields under {@code @JsonInclude(NON_NULL)} (issue #3412 / #3388).
 */
@XmlRootElement(name = "LocationSchemeParameter")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Represents a Location Scheme Parameter")
public class LocationSchemeParameter {

  @Schema(name = "name", description = "The globally unique name of this parameter")
  private String name;

  @Schema(name = "sequence", description = "The order of this parameter")
  private Integer sequence;

  @Schema(name = "type", description = "The type of this parameter")
  private String type;

  @Schema(name = "value", description = "The value for this parameter")
  private String value;

  public LocationSchemeParameter() {
    // Default constructor
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getSequence() {
    return sequence;
  }

  public void setSequence(Integer sequence) {
    this.sequence = sequence;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
