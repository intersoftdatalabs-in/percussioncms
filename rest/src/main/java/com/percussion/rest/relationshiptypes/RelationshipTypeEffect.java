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
import java.util.List;

/** Projection of a relationship conditional effect, including conditions and execution contexts. */
@XmlRootElement(name = "RelationshipTypeEffect")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Relationship type effect (extension call, conditions, execution contexts)")
public class RelationshipTypeEffect {

  @Schema(description = "Effect extension name")
  private String name;

  @Schema(description = "Fully qualified extension reference string")
  private String extensionRef;

  @Schema(description = "Activation end point (owner/dependent/either)")
  private String activationEndPoint;

  @Schema(
      description =
          "Effect conditions. PUT replaces this list when present; empty list clears."
              + " Null (omitted) leaves existing conditions unchanged.")
  private List<RelationshipTypeEffectCondition> conditions;

  @Schema(
      description =
          "Execution context names (e.g. PreConstruction, PostWorkflow). PUT replaces when"
              + " present; empty list clears. Null leaves existing contexts unchanged.")
  private List<String> executionContexts;

  @Schema(
      description =
          "When true, clears effect conditions (same intent as conditions: [])."
              + " Use when a JAX-RS/Jackson path drops empty arrays.")
  private Boolean clearConditions;

  @Schema(
      description =
          "When true, clears execution contexts (same intent as executionContexts: []).")
  private Boolean clearExecutionContexts;

  public RelationshipTypeEffect() {}

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getExtensionRef() {
    return extensionRef;
  }

  public void setExtensionRef(String extensionRef) {
    this.extensionRef = extensionRef;
  }

  public String getActivationEndPoint() {
    return activationEndPoint;
  }

  public void setActivationEndPoint(String activationEndPoint) {
    this.activationEndPoint = activationEndPoint;
  }

  public List<RelationshipTypeEffectCondition> getConditions() {
    return conditions;
  }

  public void setConditions(List<RelationshipTypeEffectCondition> conditions) {
    this.conditions = conditions;
  }

  public List<String> getExecutionContexts() {
    return executionContexts;
  }

  public void setExecutionContexts(List<String> executionContexts) {
    this.executionContexts = executionContexts;
  }

  public Boolean getClearConditions() {
    return clearConditions;
  }

  public void setClearConditions(Boolean clearConditions) {
    this.clearConditions = clearConditions;
  }

  public Boolean getClearExecutionContexts() {
    return clearExecutionContexts;
  }

  public void setClearExecutionContexts(Boolean clearExecutionContexts) {
    this.clearExecutionContexts = clearExecutionContexts;
  }
}
