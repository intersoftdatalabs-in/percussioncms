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
 * Assigned roles and their assignment types for one workflow (slice 61).
 *
 * <p>Jackson root wrap is {@code WorkflowStepRoleAssignmentList}. Readers are included so a reload
 * can show Reader after an assignee is changed. This list does not replace the step role editor.
 */
@XmlRootElement(name = "WorkflowStepRoleAssignmentList")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Assignment types for roles already assigned to workflow steps")
public class WorkflowStepRoleAssignmentList {

  @Schema(description = "One row per assigned role on each step. May be empty.")
  private List<WorkflowStepRoleAssignment> assignments = new ArrayList<>();

  public WorkflowStepRoleAssignmentList() {}

  public List<WorkflowStepRoleAssignment> getAssignments() {
    return assignments;
  }

  public void setAssignments(List<WorkflowStepRoleAssignment> assignments) {
    this.assignments = assignments == null ? new ArrayList<>() : new ArrayList<>(assignments);
  }
}
