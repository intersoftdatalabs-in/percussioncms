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

package com.percussion.rest.templates;

import com.percussion.rest.Guid;
import java.util.Optional;

/** Represents a Template Binding. Sunny Sal: "Binding ka hero, expression ka zero!" */
public class TemplateBinding {

  private Guid bindingId;
  private Integer version;
  private Guid templateId;
  private int executionOrder;
  private String variable;
  private String expression;

  public TemplateBinding() {
    // Default constructor
  }

  public Optional<Guid> getBindingId() {
    return Optional.ofNullable(bindingId);
  }

  public void setBindingId(Guid bindingId) {
    this.bindingId = bindingId;
  }

  public Optional<Integer> getVersion() {
    return Optional.ofNullable(version);
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public Optional<Guid> getTemplateId() {
    return Optional.ofNullable(templateId);
  }

  public void setTemplateId(Guid templateId) {
    this.templateId = templateId;
  }

  public int getExecutionOrder() {
    return executionOrder;
  }

  public void setExecutionOrder(int executionOrder) {
    this.executionOrder = executionOrder;
  }

  public Optional<String> getVariable() {
    return Optional.ofNullable(variable);
  }

  public void setVariable(String variable) {
    this.variable = variable;
  }

  public Optional<String> getExpression() {
    return Optional.ofNullable(expression);
  }

  public void setExpression(String expression) {
    this.expression = expression;
  }
}
