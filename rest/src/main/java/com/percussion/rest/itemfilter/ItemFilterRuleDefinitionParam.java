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

package com.percussion.rest.itemfilter;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Represents an ItemFilter Rule Parameter.
 *
 * <p>Wire getters return plain types (not {@code Optional}) so Jackson/CXF JSON emits {@code name}
 * and {@code value} scalars rather than Optional beans ({@code empty}/{@code present}). Matches
 * {@link com.percussion.rest.contenttypes.ContentType} getter style (issue #3388 / #3431).
 */
@XmlRootElement(name = "ItemFilterRuleDefinitionParam")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Represents an ItemFilter Rule Parameter")
public class ItemFilterRuleDefinitionParam {

  @Schema(name = "name", description = "Unique name for this parameter.")
  private String name;

  @Schema(name = "value", description = "The parameter value")
  private String value;

  public ItemFilterRuleDefinitionParam() {
    // Default constructor
  }

  /** Gets the parameter name. */
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /** Gets the parameter value. */
  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
