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

package com.percussion.apibridge;

import com.percussion.services.workflow.data.PSAssignedRole;
import com.percussion.services.workflow.data.PSAssignmentTypeEnum;
import com.percussion.services.workflow.data.PSState;
import com.percussion.services.workflow.data.PSWorkflowRole;
import com.percussion.utils.guid.IPSGuid;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;

/**
 * Adds one existing workflow role onto one step with {@link PSState#addAssignedRole}.
 *
 * <p>Does not rename the step, remove roles, or edit notify and inbox. Those flags stay at the
 * {@link PSAssignedRole} field defaults. Only {@link PSAssignmentTypeEnum#READER} and {@link
 * PSAssignmentTypeEnum#ASSIGNEE} can be written.
 */
public final class WorkflowStepRoleAdder {

  private WorkflowStepRoleAdder() {}

  /**
   * Adds {@code roleName} to {@code stepName}. The role must already exist on the workflow and
   * must not already be assigned to that step.
   */
  public static void add(
      List<PSState> states,
      List<PSWorkflowRole> roles,
      String stepName,
      String roleName,
      String assignmentType) {
    String stepWant = requireText(stepName, "Step name");
    String roleWant = requireText(roleName, "Role name");
    PSAssignmentTypeEnum next = parseMutable(assignmentType);
    PSState state = findStep(states, stepWant);
    PSWorkflowRole workflowRole = findWorkflowRole(roles, roleWant);
    if (findAssigned(state, roleUuid(workflowRole.getGUID())) != null) {
      throw new WebApplicationException("Role is already assigned to this step: " + roleWant, 409);
    }
    PSAssignedRole created = new PSAssignedRole();
    created.setGUID(workflowRole.getGUID());
    created.setStateId(state.getStateId());
    created.setWorkflowId(state.getWorkflowId());
    created.setAssignmentType(next);
    // Notify and inbox stay at PSAssignedRole field defaults; this slice does not edit them.
    state.addAssignedRole(created);
  }

  static PSAssignmentTypeEnum parseMutable(String raw) {
    String n = raw == null ? "" : raw.trim();
    if (n.equalsIgnoreCase(PSAssignmentTypeEnum.READER.name())
        || n.equalsIgnoreCase(PSAssignmentTypeEnum.READER.getLabel())) {
      return PSAssignmentTypeEnum.READER;
    }
    if (n.equalsIgnoreCase(PSAssignmentTypeEnum.ASSIGNEE.name())
        || n.equalsIgnoreCase(PSAssignmentTypeEnum.ASSIGNEE.getLabel())) {
      return PSAssignmentTypeEnum.ASSIGNEE;
    }
    throw new IllegalArgumentException("assignment type must be READER or ASSIGNEE");
  }

  private static String requireText(String raw, String label) {
    String n = raw == null ? "" : raw.trim();
    if (n.isEmpty()) {
      throw new IllegalArgumentException(label + " is required");
    }
    return n;
  }

  private static PSState findStep(List<PSState> states, String stepName) {
    if (states != null) {
      for (PSState state : states) {
        if (state != null
            && state.getName() != null
            && state.getName().trim().equalsIgnoreCase(stepName)) {
          return state;
        }
      }
    }
    throw new WebApplicationException("Workflow step not found: " + stepName, 404);
  }

  private static PSWorkflowRole findWorkflowRole(List<PSWorkflowRole> roles, String roleName) {
    if (roles != null) {
      for (PSWorkflowRole role : roles) {
        if (role != null
            && role.getName() != null
            && role.getName().trim().equalsIgnoreCase(roleName)
            && roleUuid(role.getGUID()) != 0) {
          return role;
        }
      }
    }
    throw new WebApplicationException("Role not found: " + roleName, 404);
  }

  private static PSAssignedRole findAssigned(PSState state, int roleId) {
    List<PSAssignedRole> assigned = state.getAssignedRoles();
    if (assigned == null || roleId == 0) {
      return null;
    }
    for (PSAssignedRole role : assigned) {
      if (role != null && roleUuid(role.getGUID()) == roleId) {
        return role;
      }
    }
    return null;
  }

  private static int roleUuid(IPSGuid guid) {
    return guid == null ? 0 : guid.getUUID();
  }
}
