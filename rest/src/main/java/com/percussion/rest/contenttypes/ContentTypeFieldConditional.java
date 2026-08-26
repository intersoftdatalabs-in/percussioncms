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

/**
 * One field-rule conditional ({@code variable operator value}). Variable and value are stored as
 * text literals on PUT.
 */
@XmlRootElement(name = "ContentTypeFieldConditional")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "One field validation/visibility conditional")
public class ContentTypeFieldConditional {

  @Schema(description = "Left-hand variable (field name or literal). Required on PUT.")
  private String variable;

  @Schema(
      description =
          "Relational operator: =, <>, <, <=, >, >=, IS NULL, IS NOT NULL, BETWEEN, NOT BETWEEN,"
              + " IN, NOT IN, LIKE, NOT LIKE. Alias != is accepted as <>.")
  private String operator;

  @Schema(description = "Right-hand value. Optional for IS NULL / IS NOT NULL.")
  private String value;

  @Schema(description = "Boolean join to the next conditional in the same rule: AND (default) or OR")
  private String booleanOperator;

  public ContentTypeFieldConditional() {}

  public ContentTypeFieldConditional(String variable, String operator, String value) {
    this.variable = variable;
    this.operator = operator;
    this.value = value;
  }

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
