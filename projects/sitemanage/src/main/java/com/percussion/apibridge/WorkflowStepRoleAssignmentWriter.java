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
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * Reads assignment rows and changes one stored field on a role already assigned to one step.
 *
 * <p>{@link #setType} changes only the assignment type and only for Reader or Assignee.
 * {@link #setNotify} changes only {@code ISNOTIFYON} on any assigned role. {@link #setInbox}
 * changes only {@code SHOWININBOX} and only for Reader or Assignee. None of these calls renames
 * the step, adds or removes roles, or edits ad-hoc flags.
 */
public final class WorkflowStepRoleAssignmentWriter {

  private WorkflowStepRoleAssignmentWriter() {}

  /**
   * One stored assignment. {@code assignmentType} is the enum name.
   *
   * @param stepName step (state) name
   * @param roleName role already assigned to that step
   * @param assignmentType enum name such as READER or ASSIGNEE
   * @param notifyOn stored {@code ISNOTIFYON}. Not named {@code notify}: that collides with
   *     {@link Object#notify()}.
   * @param showInInbox stored {@code SHOWININBOX}
   */
  public record StepRoleAssignment(
      String stepName,
      String roleName,
      String assignmentType,
      boolean notifyOn,
      boolean showInInbox) {}

  public static List<StepRoleAssignment> list(List<PSState> states, List<PSWorkflowRole> roles) {
    List<StepRoleAssignment> out = new ArrayList<>();
    if (states == null) {
      return out;
    }
    for (PSState state : states) {
      if (state == null || StringUtils.isBlank(state.getName())) {
        continue;
      }
      List<PSAssignedRole> assigned = state.getAssignedRoles();
      if (assigned == null) {
        continue;
      }
      for (PSAssignedRole role : assigned) {
        if (role == null) {
          continue;
        }
        String roleName = roleName(roles, roleUuid(role.getGUID()));
        if (StringUtils.isBlank(roleName)) {
          continue;
        }
        PSAssignmentTypeEnum type = role.getAssignmentType();
        out.add(
            new StepRoleAssignment(
                state.getName(), roleName, type.name(), role.isDoNotify(), role.isShowInInbox()));
      }
    }
    return out;
  }

  /**
   * Sets {@code next} on the named role of the named step. The role must already be assigned.
   * Other roles, the step name, and notify / inbox / ad-hoc flags are left as they were.
   */
  public static void setType(
      List<PSState> states,
      List<PSWorkflowRole> roles,
      String stepName,
      String roleName,
      String assignmentType) {
    String stepWant = requireText(stepName, "Step name");
    String roleWant = requireText(roleName, "Role name");
    PSAssignmentTypeEnum next = parseMutable(assignmentType);
    PSState state = findStep(states, stepWant);
    int roleId = findWorkflowRoleId(roles, roleWant);
    PSAssignedRole hit = findAssigned(state, roleId, roleWant);
    PSAssignmentTypeEnum current = hit.getAssignmentType();
    if (current != PSAssignmentTypeEnum.READER && current != PSAssignmentTypeEnum.ASSIGNEE) {
      throw new WebApplicationException(
          "Only Reader and Assignee assignment types can be changed from this surface", 409);
    }
    if (current == next) {
      throw new IllegalArgumentException("assignment type is unchanged");
    }
    hit.setAssignmentType(next);
  }

  /**
   * Sets {@code ISNOTIFYON} on the named role of the named step. The role must already be
   * assigned. Assignment type, inbox, ad-hoc, the step name, and every other role stay as they
   * were. An unchanged flag is rejected.
   */
  public static void setNotify(
      List<PSState> states,
      List<PSWorkflowRole> roles,
      String stepName,
      String roleName,
      Boolean notify) {
    String stepWant = requireText(stepName, "Step name");
    String roleWant = requireText(roleName, "Role name");
    if (notify == null) {
      throw new IllegalArgumentException("notify is required");
    }
    PSState state = findStep(states, stepWant);
    int roleId = findWorkflowRoleId(roles, roleWant);
    PSAssignedRole hit = findAssigned(state, roleId, roleWant);
    if (hit.isDoNotify() == notify.booleanValue()) {
      throw new IllegalArgumentException("notify is unchanged");
    }
    hit.setDoNotify(notify.booleanValue());
  }

  /**
   * Sets {@code SHOWININBOX} on the named Reader or Assignee of the named step. The role must
   * already be assigned. Assignment type, notify, ad-hoc, the step name, and every other role stay
   * as they were. Admin and None are rejected. An unchanged flag is rejected.
   */
  public static void setInbox(
      List<PSState> states,
      List<PSWorkflowRole> roles,
      String stepName,
      String roleName,
      Boolean inbox) {
    String stepWant = requireText(stepName, "Step name");
    String roleWant = requireText(roleName, "Role name");
    if (inbox == null) {
      throw new IllegalArgumentException("inbox is required");
    }
    PSState state = findStep(states, stepWant);
    int roleId = findWorkflowRoleId(roles, roleWant);
    PSAssignedRole hit = findAssigned(state, roleId, roleWant);
    PSAssignmentTypeEnum current = hit.getAssignmentType();
    if (current != PSAssignmentTypeEnum.READER && current != PSAssignmentTypeEnum.ASSIGNEE) {
      throw new WebApplicationException(
          "Only Reader and Assignee roles can change inbox from this surface", 409);
    }
    if (hit.isShowInInbox() == inbox.booleanValue()) {
      throw new IllegalArgumentException("inbox is unchanged");
    }
    hit.setShowInInbox(inbox.booleanValue());
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
    throw new WebApplicationException("Role is not assigned to this step: " + roleName, 404);
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

  private static String roleName(List<PSWorkflowRole> roles, int roleId) {
    if (roleId == 0 || roles == null) {
      return "";
    }
    for (PSWorkflowRole role : roles) {
      if (role != null && roleUuid(role.getGUID()) == roleId && role.getName() != null) {
        return role.getName().trim();
      }
    }
    return "";
  }

  private static int roleUuid(IPSGuid guid) {
    return guid == null ? 0 : guid.getUUID();
  }
}
