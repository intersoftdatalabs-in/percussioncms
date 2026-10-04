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
 * Set the assignment type of one role that is already on a workflow step (slice 61).
 *
 * <p>{@code roleName} must already be assigned to the path step. {@code assignmentType} is {@code
 * READER} or {@code ASSIGNEE} only. Does not rename the step, add or remove roles, or change
 * notify / inbox flags. Jackson root wrap is {@code WorkflowStepRoleAssignmentWrite}.
 */
@XmlRootElement(name = "WorkflowStepRoleAssignmentWrite")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Reader or Assignee for one role already on a workflow step")
public class WorkflowStepRoleAssignmentWrite {

  @Schema(required = true, description = "Role name already assigned to the step.")
  private String roleName;

  @Schema(
      required = true,
      description = "READER or ASSIGNEE. Admin and ad-hoc types are not accepted.")
  private String assignmentType;

  public WorkflowStepRoleAssignmentWrite() {}

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
