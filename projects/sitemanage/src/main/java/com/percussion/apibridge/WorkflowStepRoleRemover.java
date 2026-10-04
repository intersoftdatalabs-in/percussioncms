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
import java.util.Iterator;
import java.util.List;

/**
 * Removes one Reader or Assignee role from one workflow step.
 *
 * <p>Does not rename the step, add roles, change other steps, or edit notify and inbox flags.
 * This is not {@link WorkflowStepRoleAssignmentWriter}, which only changes the assignment type.
 * Admin and None assignments are left in place.
 */
public final class WorkflowStepRoleRemover {

  private WorkflowStepRoleRemover() {}

  /**
   * Removes {@code roleName} from {@code stepName}. The role must already be assigned to that step
   * as Reader or Assignee. Other steps keep the same role.
   */
  public static void remove(
      List<PSState> states, List<PSWorkflowRole> roles, String stepName, String roleName) {
    String stepWant = requireText(stepName, "Step name");
    String roleWant = requireText(roleName, "Role name");
    PSState state = findStep(states, stepWant);
    int roleId = findWorkflowRoleId(roles, roleWant);
    PSAssignedRole hit = findAssigned(state, roleId, roleWant);
    PSAssignmentTypeEnum current = hit.getAssignmentType();
    if (current != PSAssignmentTypeEnum.READER && current != PSAssignmentTypeEnum.ASSIGNEE) {
      throw new WebApplicationException(
          "Only Reader and Assignee roles can be removed from this surface", 409);
    }
    List<PSAssignedRole> assigned = state.getAssignedRoles();
    Iterator<PSAssignedRole> it = assigned.iterator();
    while (it.hasNext()) {
      PSAssignedRole role = it.next();
      if (role != null && roleUuid(role.getGUID()) == roleId) {
        it.remove();
        return;
      }
    }
    throw new WebApplicationException("Role is not assigned to this step: " + roleWant, 404);
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

  private static int findWorkflowRoleId(List<PSWorkflowRole> roles, String roleName) {
    if (roles != null) {
      for (PSWorkflowRole role : roles) {
        if (role != null
            && role.getName() != null
            && role.getName().trim().equalsIgnoreCase(roleName)) {
          int id = roleUuid(role.getGUID());
          if (id != 0) {
            return id;
          }
        }
      }
    }
    throw new WebApplicationException("Role not found: " + roleName, 404);
  }

  private static PSAssignedRole findAssigned(PSState state, int roleId, String roleName) {
    List<PSAssignedRole> assigned = state.getAssignedRoles();
    if (assigned != null) {
      for (PSAssignedRole role : assigned) {
        if (role != null && roleUuid(role.getGUID()) == roleId) {
          return role;
        }
      }
    }
    throw new WebApplicationException("Role is not assigned to this step: " + roleName, 404);
  }

  private static int roleUuid(IPSGuid guid) {
    return guid == null ? 0 : guid.getUUID();
  }
}
