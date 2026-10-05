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
 * Set the adhoc type on one Reader or Assignee role already on a workflow step (slice 69).
 *
 * <p>{@code roleName} must already be assigned to the path step. {@code adhocType} is {@code
 * disabled}, {@code enabled}, or {@code anonymous} ({@code PSAdhocTypeEnum}). Does not change the
 * assignment type, add or remove the role, or edit notify or inbox. Jackson root wrap is {@code
 * WorkflowStepRoleAdhocWrite}.
 */
@XmlRootElement(name = "WorkflowStepRoleAdhocWrite")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
    description =
        "Adhoc type for one Reader or Assignee role already on a workflow step")
public class WorkflowStepRoleAdhocWrite {

  @Schema(required = true, description = "Role name already assigned to the step.")
  private String roleName;

  @Schema(
      required = true,
      description = "New adhoc type: disabled, enabled, or anonymous.")
  private String adhocType;

  public WorkflowStepRoleAdhocWrite() {}

  public String getRoleName() {
    return roleName;
  }

  public void setRoleName(String roleName) {
    this.roleName = roleName;
  }

  public String getAdhocType() {
    return adhocType;
  }

  public void setAdhocType(String adhocType) {
    this.adhocType = adhocType;
  }
}
