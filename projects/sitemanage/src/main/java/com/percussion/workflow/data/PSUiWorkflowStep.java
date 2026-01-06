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
  private List<PSUiWorkflowStepRole> stepRoles = new ArrayList<>();
  private List<String> permissionNames = new ArrayList<>();

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

  public void setStepRoles(List<PSUiWorkflowStepRole> stepRoles) {
    this.stepRoles = stepRoles;
  }

  /** Gets the permission names. May be empty but never {@code null}. */
  public List<String> getPermissionNames() {
    return permissionNames;
  }

  public void setPermissionNames(List<String> permissionNames) {
    this.permissionNames = permissionNames;
  }
}
