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
/*
 * com.percussion.pso.workflow PSOPublishContent.java
 *
 * @author DavidBenua
 *
 */
package com.percussion.pso.workflow;

// REFACTORED: CP-JAVA11
import com.percussion.extension.IPSWorkFlowContext;
import com.percussion.extension.IPSWorkflowAction;
import com.percussion.extension.PSDefaultExtension;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.server.IPSRequestContext;
import com.percussion.system.utils.IPSHtmlParameters;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Workflow action that publishes content by invoking a publish edition.
 *
 * @author DavidBenua
 */
public class PSOPublishContent extends PSDefaultExtension implements IPSWorkflowAction {
  /** Logger for this class */
  private static final Logger log = LogManager.getLogger(PSOPublishContent.class);

  private static PublishEditionService svc = null;

    /**
     * Creates a new PSOPublishContent.
     */
    public PSOPublishContent() {
    super();
  }

  private static void initServices() {
    if (svc == null) {
      svc = PSOPublishEditionServiceLocator.getPublishEditionService();
    }
  }

  /**
   * performAction operation.
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
    initServices();

    int workflowId = wfContext.getWorkflowID();
    int transitionId = wfContext.getTransitionID();
    String community = (String) request.getSessionPrivateObject(IPSHtmlParameters.SYS_COMMUNITY);
    if (StringUtils.isBlank(community)) {
      String errmsg = "Community must not be blank";
      log.error(errmsg);
      throw new PSExtensionProcessingException(0, errmsg);
    }
    int communityid = Integer.parseInt(community);

    int editionId = svc.findEdition(workflowId, transitionId, communityid);
    log.debug("found edition " + editionId);
    svc.runEdition(String.valueOf(editionId));
  }
}
