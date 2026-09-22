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

package com.percussion.rest.workflows;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Create/update body for a workflow step (slice 30 Developer workflow step write).
 *
 * <p>{@code name} is required and must match workflow-admin state-name rules. {@code afterStep} is
 * the existing step to insert after on create (defaults to the first step when omitted). {@code
 * roleNames} is optional; empty uses {@code Admin}. Packaged default workflows are rejected by the
 * adaptor. Jackson root wrap is {@code WorkflowStepWrite}.
 */
@XmlRootElement(name = "WorkflowStepWrite")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Workflow step create/update body (name required)")
public class WorkflowStepWrite {

  @Schema(
      required = true,
      description =
          "Step name (required, unique in the workflow; letters, digits, underscore, hyphen,"
              + " space; max 50 chars). System states Pending/Live/Approved cannot be created.")
  private String name;

  @Schema(
      description =
          "Existing step to insert after on create. Ignored on update. When omitted, the first"
              + " catalog step is used.")
  private String afterStep;

  @Schema(description = "Role names assigned to the step. Empty uses Admin.")
  private List<String> roleNames;

  public WorkflowStepWrite() {}

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getAfterStep() {
    return afterStep;
  }

  public void setAfterStep(String afterStep) {
    this.afterStep = afterStep;
  }

  public List<String> getRoleNames() {
    return roleNames;
  }

  public void setRoleNames(List<String> roleNames) {
    this.roleNames = roleNames == null ? null : new ArrayList<>(roleNames);
  }
}
