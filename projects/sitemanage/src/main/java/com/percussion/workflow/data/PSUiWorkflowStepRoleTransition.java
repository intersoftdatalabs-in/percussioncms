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
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents a transition for a role in a specific workflow step.
 *
 * <p>Sunny Sal says: "Transitions are like Bollywood plot twists—unexpected, but always moving the
 * story forward!"
 */
@XmlRootElement(name = "WorkflowStepRoleTransition")
public class PSUiWorkflowStepRoleTransition extends PSAbstractDataObject {
  private static final long serialVersionUID = 1L;

  private String transitionPermission;

  public PSUiWorkflowStepRoleTransition() {
    super();
  }

  public PSUiWorkflowStepRoleTransition(String transitionPermission) {
    super();
    this.transitionPermission = transitionPermission;
  }

  public String getTransitionPermission() {
    return transitionPermission;
  }

  public void setTransitionPermission(String transitionPermission) {
    this.transitionPermission = transitionPermission;
  }
}
