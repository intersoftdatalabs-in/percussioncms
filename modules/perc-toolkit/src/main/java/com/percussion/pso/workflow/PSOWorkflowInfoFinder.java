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
 * @author DavidBenua
 *
 */
package com.percussion.pso.workflow;

// REFACTORED: CP-JAVA11
import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.error.PSException;
import com.percussion.pso.utils.PSOItemSummaryFinder;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.services.workflow.data.PSState;
import com.percussion.services.workflow.data.PSTransition;
import com.percussion.services.workflow.data.PSTransitionBase;
import com.percussion.services.workflow.data.PSWorkflow;
import com.percussion.webservices.system.IPSSystemWs;
import com.percussion.webservices.system.PSSystemWsLocator;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Finds basic info about the workflow state for an item.
 *
 * <p>This class loads the workflow list from the system web service, and will not detect changes
 * until the next restart.
 *
 * @author DavidBenua
 */
public class PSOWorkflowInfoFinder implements IPSOWorkflowInfoFinder {
  /** Logger for this class */
  private static final Logger log = LogManager.getLogger(PSOWorkflowInfoFinder.class);

  private IPSSystemWs sws = null;
  private IPSGuidManager gmgr = null;
  private List<PSWorkflow> workflows = null;

  /**
   * Creates a new finder.
   * Creates a new PSOWorkflowInfoFinder.
   *
   */
  public PSOWorkflowInfoFinder() {
    super();
  }

  /** Initializes system services. */
  private void initServices() {
    if (sws == null) {
      sws = PSSystemWsLocator.getSystemWebservice();
    }
    if (gmgr == null) {
      gmgr = PSGuidManagerLocator.getGuidMgr();
    }
    if (workflows == null) {
      workflows = sws.loadWorkflows(null);
    }
  }

  /**
   * See referenced member.
   * @see com.percussion.pso.workflow.IPSOWorkflowInfoFinder#findWorkflow(int)
   * @param id the id
   * @return the result
   */
  public PSWorkflow findWorkflow(int id) {
    initServices();
    for (PSWorkflow wf : workflows) {
      if (wf.getGUID().longValue() == id) {
        return wf;
      }
    }
    return null;
  }

  /**
   * findWorkflowState operation.
   *
   * @see
   *     com.percussion.pso.workflow.IPSOWorkflowInfoFinder#findWorkflowState(com.percussion.services.workflow.data.PSWorkflow,
   *     int)
   * @param wf the wf
   * @param state the state
   * @return the result
   */
  public PSState findWorkflowState(PSWorkflow wf, int state) {
    for (PSState st : wf.getStates()) {
      long st2 = st.getGUID().longValue();
      if (st2 == state) {
        return st;
      }
    }
    return null;
  }

  /**
   * See referenced member.
   * @see com.percussion.pso.workflow.IPSOWorkflowInfoFinder#findWorkflowTransition(int, int)
   * @param workflow the workflow
   * @param transid the transid
   * @return the result
   */
  public PSTransition findWorkflowTransition(int workflow, int transid) {
    PSWorkflow wf = findWorkflow(workflow);
    if (wf == null) {
      String emsg = "Workflow id " + workflow + " not found";
      log.error(emsg);
      throw new IllegalArgumentException(emsg);
    }
    return findWorkflowTransition(wf, transid);
  }

  /**
   * See referenced member.
   * @see com.percussion.pso.workflow.IPSOWorkflowInfoFinder#findWorkflowAnyTransition(int, int)
   * @param workflow the workflow
   * @param transid the transid
   * @return the result
   */
  public PSTransitionBase findWorkflowAnyTransition(int workflow, int transid) {
    PSWorkflow wf = findWorkflow(workflow);
    if (wf == null) {
      String emsg = "Workflow id " + workflow + " not found";
      log.error(emsg);
      throw new IllegalArgumentException(emsg);
    }
    return findWorkflowAnyTransition(wf, transid);
  }

  /**
   * findWorkflowTransition operation.
   *
   * @see
   *     com.percussion.pso.workflow.IPSOWorkflowInfoFinder#findWorkflowTransition(com.percussion.services.workflow.data.PSWorkflow,
   *     int)
   * @param wf the wf
   * @param transid the transid
   * @return the result
   */
  public PSTransition findWorkflowTransition(PSWorkflow wf, int transid) {
    return (PSTransition) findWorkflowAnyTransitionInternal(wf, transid, false);
  }

  /**
   * findWorkflowAnyTransition operation.
   *
   * @param wf the wf
   * @param transid the transid
   * @return the result
   */
  public PSTransitionBase findWorkflowAnyTransition(PSWorkflow wf, int transid) {
    return findWorkflowAnyTransitionInternal(wf, transid, true);
  }

  /**
   * findWorkflowAnyTransitionInternal operation.
   *
   * @param wf the wf
   * @param transid the transid
   * @param includeAging the include aging
   * @return the result
   */
  protected PSTransitionBase findWorkflowAnyTransitionInternal(
      PSWorkflow wf, int transid, boolean includeAging) {
    PSTransitionBase result = null;
    for (PSState st : wf.getStates()) {
      result = findTransitionInternal(st.getTransitions(), transid);
      if (result != null) return result;
      if (includeAging) {
        result = findTransitionInternal(st.getAgingTransitions(), transid);
        if (result != null) return result;
      }
    }
    return null;
  }

  private PSTransitionBase findTransitionInternal(
      List<? extends PSTransitionBase> transList, long transid) {
    for (PSTransitionBase trans : transList) {
      long tid = trans.getGUID().longValue();
      if (tid == transid) {
        return trans;
      }
    }
    return null;
  }

  /**
   * See referenced member.
   * @see com.percussion.pso.workflow.IPSOWorkflowInfoFinder#findWorkflowState(int, int)
   * @param workflow the workflow
   * @param state the state
   * @return the result
   */
  public PSState findWorkflowState(int workflow, int state) {
    PSWorkflow wf = this.findWorkflow(workflow);
    if (wf == null) {
      String emsg = "Workflow id " + workflow + " not found";
      log.error(emsg);
      throw new IllegalArgumentException(emsg);
    }
    return findWorkflowState(wf, state);
  }

  /**
   * See referenced member.
   * @see com.percussion.pso.workflow.IPSOWorkflowInfoFinder#findWorkflowState(java.lang.String)
   * @param contentId the content id
   * @return the result
   * @throws PSException if an error occurs
   */
  public PSState findWorkflowState(String contentId) throws PSException {
    PSComponentSummary sum = PSOItemSummaryFinder.getSummary(contentId);
    int wfapp = sum.getWorkflowAppId();
    int wfst = sum.getContentStateId();
    return findWorkflowState(wfapp, wfst);
  }

  /**
   * See referenced member.
   * @see com.percussion.pso.workflow.IPSOWorkflowInfoFinder#findWorkflowStateName(java.lang.String)
   * @param contentId the content id
   * @return the result
   * @throws PSException if an error occurs
   */
  public String findWorkflowStateName(String contentId) throws PSException {
    PSState state = findWorkflowState(contentId);
    if (state == null) {
      String emsg = "Invalid workflow state for item " + contentId;
      log.error(emsg);
      throw new PSException(emsg);
    }
    return state.getName();
  }

  /**
   * IsWorkflowValid operation.
   *
   * @see com.percussion.pso.workflow.IPSOWorkflowInfoFinder#IsWorkflowValid(java.lang.String,
   *     java.util.Collection)
   * @param contentId the content id
   * @param validFlags the valid flags
   * @return the result
   * @throws PSException if an error occurs
   */
  public boolean IsWorkflowValid(String contentId, Collection<String> validFlags)
      throws PSException {
    PSState state = findWorkflowState(contentId);
    String cvalid = state.getContentValidValue();
    if (StringUtils.isBlank(cvalid)) {
      String emsg = "Invalid content valid flag for state " + state.getName();
      log.error(emsg);
      throw new PSException(emsg);
    }
    for (String v : validFlags) {
      if (cvalid.equalsIgnoreCase(v)) {
        return true;
      }
    }
    return false;
  }

  /**
   * findDestinationState operation.
   *
   * @see com.percussion.pso.workflow.IPSOWorkflowInfoFinder#findDestinationState(java.lang.String,
   *     java.lang.String)
   * @param contentId the content id
   * @param transitionId the transition id
   * @return the result
   * @throws PSException if an error occurs
   */
  public PSState findDestinationState(String contentId, String transitionId) throws PSException {
    PSState state = findWorkflowState(contentId);
    return findDestinationState(state, transitionId);
  }

  /**
   * findDestinationState operation.
   *
   * @see
   *     com.percussion.pso.workflow.IPSOWorkflowInfoFinder#findDestinationState(com.percussion.services.workflow.data.PSState,
   *     java.lang.String)
   * @param state the state
   * @param transitionId the transition id
   * @return the result
   * @throws PSException if an error occurs
   */
  public PSState findDestinationState(PSState state, String transitionId) throws PSException {
    String emsg;
    if (state == null) {
      emsg = "State must not be null";
      log.error(emsg);
      throw new PSException(emsg);
    }
    if (StringUtils.isBlank(transitionId)) {
      emsg = "Transition id must not be null";
      log.error(emsg);
      throw new PSException(emsg);
    }
    long tid;
    try {
      tid = Long.parseLong(transitionId);
    } catch (NumberFormatException ex) {
      emsg = "Invalid transition id " + transitionId;
      log.error(emsg);
      throw new PSException(emsg, ex);
    }
    PSTransitionBase trans = findTransitionInternal(state.getTransitions(), tid);
    if (trans == null) {
      trans = findTransitionInternal(state.getAgingTransitions(), tid);
    }
    if (trans == null) {
      log.warn("no transition found for " + state.getName() + " " + transitionId);
      return null;
    }
    PSState dest = findWorkflowState((int) state.getWorkflowId(), (int) trans.getToState());
    if (dest == null) { // stateid is invalid for this workflow.
      emsg = "no such state " + state.getWorkflowId() + " - " + trans.getToState();
      log.error(emsg);
      throw new PSException(emsg);
    }
    log.debug("found destination state " + dest.getName());
    return dest;
  }

  /**
   * Sets the system web service. Should be used only in unit tests.
   *
   * @param sws The sws to set.
   */
  public void setSws(IPSSystemWs sws) {
    this.sws = sws;
  }

  /**
   * Sets the workflow list. Should be used only in unit tests.
   *
   * @param workflows The workflows to set.
   */
  public void setWorkflows(List<PSWorkflow> workflows) {
    this.workflows = workflows;
  }
}
