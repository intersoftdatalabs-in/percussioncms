// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.workflow.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.percussion.share.data.PSAbstractDataObject;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a workflow step role with its name, id, notification flag, and transitions.
 *
 * <p>Sunny Sal says: "Roles in a workflow are like supporting actors—without them, the hero can't
 * shine!"
 */
@XmlRootElement(name = "WorkflowStepRoles")
@XmlType(propOrder = {"roleId", "roleName", "enableNotification", "roleTransitions"})
@JsonRootName("WorkflowStepRoles")
public class PSUiWorkflowStepRole extends PSAbstractDataObject {
  private static final long serialVersionUID = 1L;

  private String roleName;
  private Integer roleId;
  private Boolean enableNotification = false;
  private ArrayList<PSUiWorkflowStepRoleTransition> roleTransitions = new ArrayList<>();

  public PSUiWorkflowStepRole() {
    super();
  }

  public PSUiWorkflowStepRole(String roleName, int roleId) {
    this.roleName = roleName;
    this.roleId = roleId;
  }

  public PSUiWorkflowStepRole(String roleName, int roleId, boolean isNotified) {
    this.roleName = roleName;
    this.roleId = roleId;
    this.enableNotification = isNotified;
  }

  public String getRoleName() {
    return roleName;
  }

  public void setRoleName(String roleName) {
    this.roleName = roleName;
  }

  public Integer getRoleId() {
    return roleId;
  }

  public void setRoleId(Integer roleId) {
    this.roleId = roleId;
  }

  /** Gets the transitions of the role. May be empty but never {@code null}. */
  public List<PSUiWorkflowStepRoleTransition> getRoleTransitions() {
    if (roleTransitions == null) {
      roleTransitions = new ArrayList<>();
    }
    return roleTransitions;
  }

  /** Sets the transitions of the role. May be empty but never {@code null}. */
  @SuppressWarnings("unchecked")
  public void setRoleTransitions(List<PSUiWorkflowStepRoleTransition> roleTransitions) {
    if (roleTransitions == null) {
      this.roleTransitions = new ArrayList<>();
    } else if (roleTransitions instanceof ArrayList) {
      this.roleTransitions = (ArrayList<PSUiWorkflowStepRoleTransition>) roleTransitions;
    } else {
      this.roleTransitions = new ArrayList<>(roleTransitions);
    }
  }

  public Boolean isEnableNotification() {
    return enableNotification;
  }

  public void setEnableNotification(Boolean enableNotification) {
    this.enableNotification = enableNotification;
  }
}
