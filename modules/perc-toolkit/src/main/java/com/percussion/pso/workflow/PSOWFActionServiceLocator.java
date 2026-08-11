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
package com.percussion.pso.workflow;

// REFACTORED: CP-JAVA11
import com.percussion.services.PSBaseServiceLocator;

/**
 * Locator for the IPSOWFActionService bean.
 *
 * @author DavidBenua
 * @see IPSOWFActionService
 * @see PSOSpringWorkflowActionDispatcher
 */
public class PSOWFActionServiceLocator extends PSBaseServiceLocator {
  /**
   * Creates a new PSOWFActionServiceLocator.
   */
  public PSOWFActionServiceLocator() {
    // default
  }

  /**
   * Gets the PSO Workflow Action Service bean.
   *
   * @return the PSO Workflow Action Service bean.
   */
  public static IPSOWFActionService getPSOWFActionService() {
    return (IPSOWFActionService) PSBaseServiceLocator.getBean(PSO_WF_ACTION_SERVICE_BEAN);
  }

  /** pso wf action service bean. */
  public static final String PSO_WF_ACTION_SERVICE_BEAN = "psoWFActionService";
}
