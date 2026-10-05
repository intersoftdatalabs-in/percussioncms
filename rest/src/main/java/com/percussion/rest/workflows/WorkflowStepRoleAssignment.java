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

/**
 * One role already assigned to a workflow step, with its stored assignment type.
 *
 * <p>{@code assignmentType} is the enum name ({@code READER}, {@code ASSIGNEE}, {@code ADMIN}, or
 * {@code NONE}). {@code notify} is the stored {@code ISNOTIFYON} flag. {@code inbox} is the stored
 * {@code SHOWININBOX} flag. {@code adhocType} is the stored adhoc type ({@code disabled}, {@code
 * enabled}, or {@code anonymous}). This row does not add or remove the role.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
    description =
        "Assignment type, notify flag, inbox flag, and adhoc type of one role on one workflow step")
public class WorkflowStepRoleAssignment {

  @Schema(description = "Step (state) name.")
  private String stepName;

  @Schema(description = "Role name already assigned to the step.")
  private String roleName;

  @Schema(description = "Stored assignment type: READER, ASSIGNEE, ADMIN, or NONE.")
  private String assignmentType;

  @Schema(description = "Stored notify flag (ISNOTIFYON).")
  private boolean notify;

  @Schema(description = "Stored inbox flag (SHOWININBOX).")
  private boolean inbox;

  @Schema(description = "Stored adhoc type: disabled, enabled, or anonymous.")
  private String adhocType;

  public WorkflowStepRoleAssignment() {}

  public String getStepName() {
    return stepName;
  }

  public void setStepName(String stepName) {
    this.stepName = stepName;
  }

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

  public boolean isNotify() {
    return notify;
  }

  public void setNotify(boolean notify) {
    this.notify = notify;
  }

  public boolean isInbox() {
    return inbox;
  }

  public void setInbox(boolean inbox) {
    this.inbox = inbox;
  }

  public String getAdhocType() {
    return adhocType;
  }

  public void setAdhocType(String adhocType) {
    this.adhocType = adhocType;
  }
}
