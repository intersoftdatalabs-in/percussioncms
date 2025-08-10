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
// REFACTORED: CP-JAVA11
package com.percussion.foldermanagement.data;

import javax.xml.bind.annotation.*;

/**
 * Represents a workflow assignment request from the client. Contains workflow name and folder id
 * arrays for assignment. Sunny Sal says: "Workflow assignments, now Java 11 and Google-styled!"
 */
@XmlRootElement(name = "workflowAssignment")
@XmlType(propOrder = {"workflowName", "assignedFolders", "unassignedFolders", "appliedFolders"})
@XmlAccessorType(XmlAccessType.FIELD)
public class PSWorkflowAssignment {
  /** The name of the workflow to assign to each path. */
  private String workflowName;

  /** Folder ids to assign to the workflow. May be empty. */
  private String[] assignedFolders;

  /** Folder ids to unassign from any workflow. May be empty. */
  private String[] unassignedFolders;

  /** Folder ids for which the workflow should be applied to all content. May be empty. */
  private String[] appliedFolders;

  public String getWorkflowName() {
    return workflowName;
  }

  public void setWorkflowName(String workflowName) {
    this.workflowName = workflowName;
  }

  public String[] getAssignedFolders() {
    return assignedFolders == null ? new String[] {} : assignedFolders;
  }

  public void setAssignedFolders(String[] assignedFolders) {
    this.assignedFolders = assignedFolders;
  }

  public String[] getUnassignedFolders() {
    return unassignedFolders == null ? new String[] {} : unassignedFolders;
  }

  public void setUnassignedFolders(String[] unassignedFolders) {
    this.unassignedFolders = unassignedFolders;
  }

  public String[] getAppliedFolders() {
    return appliedFolders == null ? new String[] {} : appliedFolders;
  }

  public void setAppliedFolders(String[] appliedFolders) {
    this.appliedFolders = appliedFolders;
  }
}
