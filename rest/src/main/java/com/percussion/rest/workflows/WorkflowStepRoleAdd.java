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

/**
 * Add one existing workflow role onto one step (slice 63).
 *
 * <p>This is not {@link WorkflowStepRoleAssignmentWrite}. {@code roleName} must already be a role
 * on the workflow and must not already be assigned to the path step. {@code assignmentType} is
 * {@code READER} or {@code ASSIGNEE} only. Notify and inbox are not fields on this body. Jackson
 * root wrap is {@code WorkflowStepRoleAdd}.
 */
@XmlRootElement(name = "WorkflowStepRoleAdd")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Add one existing workflow role onto one step as Reader or Assignee")
public class WorkflowStepRoleAdd {

  @Schema(required = true, description = "Workflow role name that is not already on the step.")
  private String roleName;

  @Schema(
      required = true,
      description = "READER or ASSIGNEE. Admin and ad-hoc types are not accepted.")
  private String assignmentType;

  public WorkflowStepRoleAdd() {}

  public String getRoleName() {
    return roleName;
  }

  public void setRoleName(String roleName) {
    this.roleName = roleName;
  }

  public String getAssignmentType() {
    return assignmentType;
  }

  public void setAssignmentType(String assignmentType) {
    this.assignmentType = assignmentType;
  }
}
