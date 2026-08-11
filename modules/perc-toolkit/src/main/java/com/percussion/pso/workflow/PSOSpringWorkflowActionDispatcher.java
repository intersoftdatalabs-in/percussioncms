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
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.IPSWorkFlowContext;
import com.percussion.extension.IPSWorkflowAction;
import com.percussion.extension.PSDefaultExtension;
import com.percussion.extension.PSExtensionException;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.server.IPSRequestContext;
import java.io.File;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A workflow action based on the PSOWFActionService bean.
 *
 * @author DavidBenua
 * @see IPSOWFActionService
 */
public class PSOSpringWorkflowActionDispatcher extends PSDefaultExtension
    implements IPSWorkflowAction {
  private static final Logger log = LogManager.getLogger(PSOSpringWorkflowActionDispatcher.class);
  IPSOWFActionService asvc = null;

  /**
   * Default constructor
   * Creates a new PSOSpringWorkflowActionDispatcher.
   *
   */
  public PSOSpringWorkflowActionDispatcher() {}

  /**
   * init operation.
   *
   * @see com.percussion.extension.PSDefaultExtension#init(com.percussion.extension.IPSExtensionDef,
   *     java.io.File)
   * @param extensionDef the extension def
   * @param codeRoot the code root
   * @throws PSExtensionException if an error occurs
   */
  public void init(IPSExtensionDef extensionDef, File codeRoot) throws PSExtensionException {
    super.init(extensionDef, codeRoot);
    log.debug("Initializing WFActionDispatcher...");

    if (asvc == null) {
      asvc = PSOWFActionServiceLocator.getPSOWFActionService();
    }
  }

  /**
   * Perform the action.
   *
   * @see
   *     com.percussion.extension.IPSWorkflowAction#performAction(com.percussion.extension.IPSWorkFlowContext,
   *     com.percussion.server.IPSRequestContext)
   * @param wfContext the wf context
   * @param request the request
   * @throws PSExtensionProcessingException if an error occurs
   */
  public void performAction(IPSWorkFlowContext wfContext, IPSRequestContext request)
      throws PSExtensionProcessingException {

    int transitionId = wfContext.getTransitionID();
    int workflowId = wfContext.getWorkflowID();
    log.debug("Workflow id: " + workflowId);
    log.debug("Transition Id: " + transitionId);
    try {
      List<IPSWorkflowAction> actions = asvc.getActions(workflowId, transitionId);
      for (IPSWorkflowAction act : actions) {
        log.debug("performing action " + act.getClass().getCanonicalName());
        act.performAction(wfContext, request);
      }
      log.debug("finished actions");
    } catch (Exception nfx) {
      log.error("unknown error " + nfx.getMessage(), nfx);
      throw new PSExtensionProcessingException("PSOWFActionDispatcher", nfx);
    }
  }

  /**
   * Sets the asvc.
   * @param asvc the asvc to set. Used for unit test only.
   */
  protected void setAsvc(IPSOWFActionService asvc) {
    this.asvc = asvc;
  }
}
