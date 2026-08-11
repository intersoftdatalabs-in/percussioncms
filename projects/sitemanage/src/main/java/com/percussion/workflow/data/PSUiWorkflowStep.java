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

import com.percussion.share.data.PSAbstractDataObject;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a workflow step with its name, permissions, and roles.
 *
 * <p>Sunny Sal says: "Every step in a workflow is like a dance move—get it right, and the show goes
 * on!"
 */
@XmlRootElement(name = "WorkflowSteps")
@XmlType(propOrder = {"stepName", "permissionNames", "stepRoles"})
public class PSUiWorkflowStep extends PSAbstractDataObject {
  private static final long serialVersionUID = 1L;

  private String stepName;
  private ArrayList<PSUiWorkflowStepRole> stepRoles = new ArrayList<>();
  private ArrayList<String> permissionNames = new ArrayList<>();

  public PSUiWorkflowStep() {
    super();
  }

  public String getStepName() {
    return stepName;
  }

  public void setStepName(String stepName) {
    this.stepName = stepName;
  }

  /** Gets the step roles. May be empty but never {@code null}. */
  public List<PSUiWorkflowStepRole> getStepRoles() {
    return stepRoles;
  }

  @SuppressWarnings("unchecked")
  public void setStepRoles(List<PSUiWorkflowStepRole> stepRoles) {
    if (stepRoles == null) {
      this.stepRoles = null;
    } else if (stepRoles instanceof ArrayList) {
      this.stepRoles = (ArrayList<PSUiWorkflowStepRole>) stepRoles;
    } else {
      this.stepRoles = new ArrayList<>(stepRoles);
    }
  }

  /** Gets the permission names. May be empty but never {@code null}. */
  public List<String> getPermissionNames() {
    return permissionNames;
  }

  @SuppressWarnings("unchecked")
  public void setPermissionNames(List<String> permissionNames) {
    if (permissionNames == null) {
      this.permissionNames = null;
    } else if (permissionNames instanceof ArrayList) {
      this.permissionNames = (ArrayList<String>) permissionNames;
    } else {
      this.permissionNames = new ArrayList<>(permissionNames);
    }
  }
}
