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
package com.percussion.workflow.service.impl;

import static com.percussion.share.service.exception.PSParameterValidationUtils.rejectIfBlank;

import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.workflow.data.PSState;
import com.percussion.share.data.PSEnumVals;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.util.PSStringComparator;
import com.percussion.workflow.data.PSUiWorkflow;
import com.percussion.workflow.data.PSUiWorkflowList;
import com.percussion.workflow.service.IPSSteppedWorkflowService;
import com.percussion.workflow.service.IPSSteppedWorkflowService.PSWorkflowEditorServiceException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/** REST service for stepped workflow operations. */
@Path("/workflows")
@Component("steppedWorkflowRestService")
@Lazy
public class PSSteppedWorkflowRestService {

  private final IPSSteppedWorkflowService service;
  private static final Logger log = LogManager.getLogger(PSSteppedWorkflowRestService.class);

  /** The comparator to use when ordering the list of states. */
  private static final PSStringComparator stringComparator =
      new PSStringComparator(PSStringComparator.SORT_CASE_INSENSITIVE_ASC);

  @Autowired
  public PSSteppedWorkflowRestService(IPSSteppedWorkflowService service) {
    this.service = service;
  }

  @GET
  @Path("/{workflowName}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSUiWorkflow getWorflow(@PathParam("workflowName") String workflowName)
      throws PSWorkflowEditorServiceException {
    return service.getWorkflow(workflowName);
  }

  @GET
  @Path("/")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSEnumVals getWorflowList() throws PSWorkflowEditorServiceException {
    return service.getWorkflowList();
  }

  @GET
  @Path("/metadata")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public List<PSUiWorkflow> getWorflowMetadataList() throws PSWorkflowEditorServiceException {
    return new PSUiWorkflowList(service.getWorkflowMetadataList());
  }

  @GET
  @Path("/metadata/default")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSEnumVals getDefaultWorkflowMetadata() {
    return service.getDefaultWorkflowMetadata();
  }

  @POST
  @Path("/{workflowName:.*}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSUiWorkflow createWorflow(
      @PathParam("workflowName") String workflowName, PSUiWorkflow uiWorkflow)
      throws PSWorkflowEditorServiceException {
    try {
      return service.createWorkflow(getReadableName(workflowName), uiWorkflow);
    } catch (PSDataServiceException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  @PUT
  @Path("/{workflowName:.*}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSUiWorkflow updateWorkflow(
      @PathParam("workflowName") String workflowName, PSUiWorkflow uiWorkflow)
      throws PSWorkflowEditorServiceException {
    try {
      return service.updateWorkflow(getReadableName(workflowName), uiWorkflow);
    } catch (PSDataServiceException | PSNotFoundException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  @DELETE
  @Path("/{workflowName:.*}")
  public void deleteWorkflow(@PathParam("workflowName") String workflowName)
      throws PSWorkflowEditorServiceException {
    service.deleteWorkflow(workflowName);
  }

  @POST
  @Path("/{workflowName}/steps/{stepName:.*}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSUiWorkflow createStep(
      @PathParam("workflowName") String workflowName,
      @PathParam("stepName") String stepName,
      PSUiWorkflow uiWorkflow)
      throws PSWorkflowEditorServiceException {
    try {
      return service.createStep(
          getReadableName(workflowName), getReadableName(stepName), uiWorkflow);
    } catch (PSDataServiceException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  @PUT
  @Path("/{workflowName}/steps/{stepName:.*}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSUiWorkflow updateStep(
      @PathParam("workflowName") String workflowName,
      @PathParam("stepName") String stepName,
      PSUiWorkflow uiWorkflow)
      throws PSWorkflowEditorServiceException {
    try {
      return service.updateStep(
          getReadableName(workflowName), getReadableName(stepName), uiWorkflow);
    } catch (PSDataServiceException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  @DELETE
  @Path("/{workflowName}/steps/{stepName}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSUiWorkflow deleteStep(
      @PathParam("workflowName") String workflowName, @PathParam("stepName") String stepName)
      throws PSWorkflowEditorServiceException {
    return service.deleteStep(workflowName, stepName);
  }

  @GET
  @Path("/{workflowName}/states/choices")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSEnumVals getStatesChoices(@PathParam("workflowName") String workflowName) {
    try {
      rejectIfBlank("getStateChoices", "workflowName", workflowName);

      var states = service.getStates(workflowName);

      if (states == null) {
        var workflows = service.getWorkflowList();
        states = service.getStates(workflows.getEntries().get(0).getValue());
      }

      var choices = new PSEnumVals();

      if (states != null) {
        states.forEach(
            state ->
                choices.addEntry(state.getName(), String.valueOf(state.getGUID().longValue())));
      }

      return choices;
    } catch (PSValidationException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  /** Orders state names alphabetically (ascending). */

  private List<String> orderStateNames(List<PSState> states) {
    var names = new ArrayList<String>();
    for (var state : states) {
      names.add(state.getName());
    }
    names.sort(stringComparator);
    return names;
  }

  /** Cleans up a given name, in order to make it readable and then returns it to the caller. */
  private String getReadableName(String encodedName) throws PSDataServiceException {
    try {
      return encodedName.replace("\"", "");
    } catch (Exception e) {
      throw new PSDataServiceException("Failed to decode name = " + encodedName, e);
    }
  }
}
