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
 * Turn inbox on or off for one Reader or Assignee role already on a workflow step (slice 66).
 *
 * <p>{@code roleName} must already be assigned to the path step. {@code inbox} is the new {@code
 * SHOWININBOX} flag. Does not change the assignment type, add or remove the role, or edit notify.
 * Jackson root wrap is {@code WorkflowStepRoleInboxWrite}.
 */
@XmlRootElement(name = "WorkflowStepRoleInboxWrite")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Inbox on or off for one Reader or Assignee role already on a workflow step")
public class WorkflowStepRoleInboxWrite {

  @Schema(required = true, description = "Role name already assigned to the step.")
  private String roleName;

  @Schema(required = true, description = "true to show the role's items in the inbox.")
  private Boolean inbox;

  public WorkflowStepRoleInboxWrite() {}

  public String getRoleName() {
    return roleName;
  }

  public void setRoleName(String roleName) {
    this.roleName = roleName;
  }

  public Boolean getInbox() {
    return inbox;
  }

  public void setInbox(Boolean inbox) {
    this.inbox = inbox;
  }
}
