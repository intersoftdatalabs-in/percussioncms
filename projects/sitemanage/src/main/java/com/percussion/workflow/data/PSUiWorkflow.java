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
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * Represents a workflow with its name, description, staging roles, and steps.
 *
 * <p>Sunny Sal says: "Workflows are like Bollywood scripts—lots of drama, but every step counts!"
 */
@XmlRootElement(name = "Workflow")
public class PSUiWorkflow extends PSAbstractDataObject {
  private static final long serialVersionUID = 1L;

  private String workflowName = "";
  private String workflowDescription = "";
  private String stagingRoleNames = "";
  private boolean defaultWorkflow = false;

  // Used for update operations to identify the workflow to update.
  private String previousWorkflowName = "";

  // Used for step creation/update to identify the step to update or insert after.
  private String previousStepName = "";

  private ArrayList<PSUiWorkflowStep> workflowSteps = new ArrayList<>();

  public PSUiWorkflow() {
    this("", new ArrayList<>());
  }

  public PSUiWorkflow(String workflowName, List<PSUiWorkflowStep> workflowSteps) {
    this.workflowName = workflowName;
    if (workflowSteps == null) {
      this.workflowSteps = null;
    } else if (workflowSteps instanceof ArrayList) {
      this.workflowSteps = (ArrayList) workflowSteps;
    } else {
      this.workflowSteps = new ArrayList<>(workflowSteps);
    }
  }

  public String getWorkflowName() {
    return workflowName;
  }

  public void setWorkflowName(String workflowName) {
    this.workflowName = workflowName;
  }

  public String getWorkflowDescription() {
    return workflowDescription;
  }

  public void setWorkflowDescription(String workflowDescription) {
    this.workflowDescription = workflowDescription;
  }

  /** Gets the workflow steps. May be empty but never {@code null}. */
  public List<PSUiWorkflowStep> getWorkflowSteps() {
    return workflowSteps;
  }

  @SuppressWarnings("unchecked")
  public void setWorkflowSteps(List<PSUiWorkflowStep> workflowSteps) {
    if (workflowSteps == null) {
      this.workflowSteps = null;
    } else if (workflowSteps instanceof ArrayList) {
      this.workflowSteps = (ArrayList<PSUiWorkflowStep>) workflowSteps;
    } else {
      this.workflowSteps = new ArrayList<>(workflowSteps);
    }
  }

  public String getPreviousWorkflowName() {
    return previousWorkflowName;
  }

  public void setPreviousWorkflowName(String previousWorkflowName) {
    this.previousWorkflowName = previousWorkflowName;
  }

  public String getPreviousStepName() {
    return previousStepName;
  }

  public void setPreviousStepName(String previousStepName) {
    this.previousStepName = previousStepName;
  }

  public boolean isDefaultWorkflow() {
    return defaultWorkflow;
  }

  public void setDefaultWorkflow(boolean defaultWorkflow) {
    this.defaultWorkflow = defaultWorkflow;
  }

  /**
   * Gets a semicolon-separated list of role names.
   *
   * @return never {@code null}, may be empty.
   */
  public String getStagingRoleNames() {
    return StringUtils.defaultString(stagingRoleNames);
  }

  /**
   * Sets a semicolon-separated list of staging role names.
   *
   * @param stagingRoleNames if {@code null}, will be set to empty string.
   */
  public void setStagingRoleNames(String stagingRoleNames) {
    this.stagingRoleNames = StringUtils.defaultString(stagingRoleNames);
  }
}
