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

package com.percussion.rest.relationshiptypes;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/** One conditional clause on a relationship-type effect ({@code PSConditional} in a {@code PSRule}). */
@XmlRootElement(name = "RelationshipTypeEffectCondition")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Relationship type effect condition (variable/operator/value)")
public class RelationshipTypeEffectCondition {

  @Schema(description = "Left-hand replacement value text (e.g. sys_title)")
  private String variable;

  @Schema(description = "Comparison operator (e.g. =, !=, is null)")
  private String operator;

  @Schema(description = "Right-hand value text; optional for is null / is not null")
  private String value;

  @Schema(description = "Boolean join to the next clause (AND/OR); omitted on the last clause")
  private String booleanOperator;

  public RelationshipTypeEffectCondition() {}

  public String getVariable() {
    return variable;
  }

  public void setVariable(String variable) {
    this.variable = variable;
  }

  public String getOperator() {
    return operator;
  }

  public void setOperator(String operator) {
    this.operator = operator;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getBooleanOperator() {
    return booleanOperator;
  }

  public void setBooleanOperator(String booleanOperator) {
    this.booleanOperator = booleanOperator;
  }
}
