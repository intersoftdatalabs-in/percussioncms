/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

import com.percussion.error.PSExceptionUtils;
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
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

import static com.percussion.share.service.exception.PSParameterValidationUtils.rejectIfBlank;

/**
 * 
 * 
 * @author leonardohildt
 * @author rafaelsalis
 * 
 */
@Path("/workflows")
@Component("steppedWorkflowRestService")
@Lazy
public class PSSteppedWorkflowRestService
{
    private IPSSteppedWorkflowService service;

    private static final Logger log = LogManager.getLogger(PSSteppedWorkflowRestService.class);
    /**
     * The comparator to use when ordering the list of states.
     */
    private static final PSStringComparator stringComparator = 
        new PSStringComparator(PSStringComparator.SORT_CASE_INSENSITIVE_ASC);
    
    @Autowired
    public PSSteppedWorkflowRestService(IPSSteppedWorkflowService service)
    {
        this.service = service;
    }

    /**
     * Load the workflow information based on the workflow name as the parameter, and 
     * builds a <code>PSUiWorkflow</code> object with it.
     * 
     * @param workflowName the name of the workflow of which it is going to retrieve
     * the information from. Cannot be empty or <code>null</code>
     * @return a <code><PSUiWorkflow></code> object maybe empty never <code>null</code>
     * @throws PSWorkflowEditorServiceException, if the supplied object is invalid.
     */
    @GET
    @Path("/{workflowName}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public PSUiWorkflow getWorflow(@PathParam("workflowName") String workflowName) 
            throws PSWorkflowEditorServiceException
    {
        return service.getWorkflow(workflowName);
    }
    
    /**
     * Retrieves a list of all workflow names existing.
     * 
     * @return a <code>PSEnumVals</code> containing the list of workflow names, never 
     * empty or <code>null</code>
     * @throws PSWorkflowEditorServiceException, if the supplied object is invalid.
     */
    @GET
    @Path("/")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public PSEnumVals getWorflowList() throws PSWorkflowEditorServiceException
    {
        return service.getWorkflowList();
    }
    
    /**
     * Retrieves a list of all workflow existing including their metadata information.
     * 
     * @return a list of <code>PSUiWorkflow</code>, never empty or <code>null</code>
     * @throws PSWorkflowEditorServiceException, if the supplied object is invalid.
     */
    @GET
    @Path("/metadata")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public List<PSUiWorkflow> getWorflowMetadataList() throws PSWorkflowEditorServiceException
    {
        return new PSUiWorkflowList(service.getWorkflowMetadataList());
    }

    /**
     * Retrieves the information of the default workflow (including id).
     * 
     * @return a <code>PSEnumVals</code> containing the information, never empty or <code>null</code>
     */
    @GET
    @Path("/metadata/default")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public PSEnumVals getDefaultWorkflowMetadata()
    {
        return service.getDefaultWorkflowMetadata();
    }
    
    /**
     * Creates a new workflow with the name provided.
     * 
     * @param workflowName the name of the workflow to be created
     * @param uiWorkflow the <code>PSUiWorkflow</code> object containing the workflow information to add to
     * that workflow. Must not be emtpy or <code>null</code>
     * @return a <code>PSUiWorkflow</code> object maybe empty never <code>null</code>
     * @throws PSWorkflowEditorServiceException, if the supplied object is invalid.
     */
    @POST
    @Path("/{workflowName:.*}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public PSUiWorkflow createWorflow(@PathParam("workflowName") String workflowName, PSUiWorkflow uiWorkflow) 
            throws PSWorkflowEditorServiceException
    {
        try {
            return service.createWorkflow(getReadableName(workflowName), uiWorkflow);
        } catch (PSDataServiceException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new WebApplicationException(e);
        }
    }
    
    /**
     * Updates a workflow with the name provided.
     * 
     * @param workflowName the name of the workflow to be updated
     * @param uiWorkflow the <code>PSUiWorkflow</code> object containing the workflow information to update
     * that workflow (previous and actual workflow name). Must not be emtpy or <code>null</code>
     * @return a <code>PSUiWorkflow</code> object never empty or <code>null</code>
     * @throws PSWorkflowEditorServiceException, if the supplied object is invalid.
     */
    @PUT
    @Path("/{workflowName:.*}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public PSUiWorkflow updateWorkflow(@PathParam("workflowName") String workflowName, PSUiWorkflow uiWorkflow) 
            throws PSWorkflowEditorServiceException
    {
        try {
            return service.updateWorkflow(getReadableName(workflowName), uiWorkflow);
        } catch (PSDataServiceException | PSNotFoundException  e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new WebApplicationException(e);
        }
    }
    
    /**
     * Deletes a workflow with the name provided.
     * 
     * @param workflowName the name of the workflow to be deleted. Cannot be empty or <code>null</code>
     * @throws PSWorkflowEditorServiceException if the supplied workflow is not found in the system or there are items
     * associated with the workflow.
     */
    @DELETE
    @Path("/{workflowName:.*}")
    public void deleteWorkflow(@PathParam("workflowName") String workflowName) 
            throws PSWorkflowEditorServiceException
    {
        service.deleteWorkflow(workflowName);
    }
    
    /**
     * Creates a new step with the information provided in the supplied workflow object.
     * 
     * @param workflowName name of the workflow
     * @param stepName the name of the step to be created
     * @param uiWorkflow the <code>PSUiWorkflow</code> object containing the state to add to
     * that workflow. Must not be empty or <code>null</code>
     * @return a <code>PSUiWorkflow</code> object never empty or <code>null</code>
     * @throws PSWorkflowEditorServiceException, if the supplied object is invalid.
     */
    @POST
    @Path("/{workflowName}/steps/{stepName:.*}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public PSUiWorkflow createStep(@PathParam("workflowName") String workflowName, @PathParam("stepName") String stepName, PSUiWorkflow uiWorkflow) 
            throws PSWorkflowEditorServiceException
    {
        try {
            return service.createStep(getReadableName(workflowName), getReadableName(stepName), uiWorkflow);
        } catch (PSDataServiceException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new WebApplicationException(e);
        }
    }
    
    /**
     * Updates the step with the information provided in the supplied workflow object.
     * @param uiWorkflow the <code>PSUiWorkflow</code> object containing the state to add to
     * that workflow. Must not be <code>null</code>
     * @param workflowName the name of the workflow
     * @param stepName the name of the step to be updated
     * that step (previous and actual step name). Must not be empty or <code>null</code>
     * @return PSUiWorkflow The updated workflow, never empty or <code>null</code>.
     * @throws PSWorkflowEditorServiceException, if the supplied object is invalid.
     */
    @PUT
    @Path("/{workflowName}/steps/{stepName:.*}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public PSUiWorkflow updateStep(@PathParam("workflowName") String workflowName, @PathParam("stepName") String stepName, PSUiWorkflow uiWorkflow) 
            throws PSWorkflowEditorServiceException
    {
        try {
            return service.updateStep(getReadableName(workflowName), getReadableName(stepName), uiWorkflow);
        } catch (PSDataServiceException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new WebApplicationException(e);
        }
    }

    /**
     * Deletes the given step from the given workflow, re-points the submit transition of the previous step to next step and 
     * reject transitions of next step to the previous step.  
     * 
     * @param workflowName the name of the workflow
     * @param stepName the name of the step to be deleted
     * @return PSUiWorkflow The updated workflow, never empty or <code>null</code>.
     * @throws PSWorkflowEditorServiceException if the supplied workflow or step are not found in the system or the supplied step
     * name is a reserved step name or if items exist in that workflow.
     */
    @DELETE
    @Path("/{workflowName}/steps/{stepName}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public PSUiWorkflow deleteStep(@PathParam("workflowName") String workflowName, @PathParam("stepName") String stepName) 
            throws PSWorkflowEditorServiceException
    {
        return service.deleteStep(workflowName, stepName);
    }
    
    /**
     * Gets the list of states that belong to the workflow passed as a parameter, and 
     * builds a <code>PSEnumVals</code> object with them, so they can be presented as 
     * choices in the screen.
     * If the workflow passed as parameter doesn't exist, the system selects the first
     * workflow from the list returned from service.getWorkflowList().
     * 
     * @param workflowName the name of the workflow from where we want to retrieve the states
     * @return a <code>PSEnumVals</code> object, may be empty but never <code>null</code>
     */
    @GET
    @Path("/{workflowName}/states/choices")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public PSEnumVals getStatesChoices(@PathParam("workflowName") String workflowName)
    {
        try {
            rejectIfBlank("getStateChoices", "workflowName", workflowName);

            // get the states and build the PSEnumVals object
            List<PSState> states = service.getStates(workflowName);

            if (states == null) {
                PSEnumVals workflows = service.getWorkflowList();
                states = service.getStates(workflows.getEntries().get(0).getValue());
            }

            PSEnumVals choices = new PSEnumVals();

            if (states != null) {
                for (PSState state : states) {
                    choices.addEntry(state.getName(), String.valueOf(state.getGUID().longValue()));
                }
            }

            return choices;
        } catch (PSValidationException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new WebApplicationException(e);
        }
    }

    /**
     * Gets a list of workflow states, builds another list with their names,
     * and then returns that list ordered alphabetically in ascending order.
     * 
     * @param states the list of states. Assumed not <code>null</code>
     * @return 
     *  an ordered list of strings.
     */
    @SuppressWarnings("unchecked")
    private List<String> orderStateNames(List<PSState> states)
    {
        List<String> names = new ArrayList<>();
        
        for(PSState state : states)
        {
            names.add(state.getName());
        }
        
        names.sort(stringComparator);
        
        return names;
    }
    
    
    /**
     * Cleans up a given name, in order to make it readable
     * and then returns it to the caller.
     * 
     * @param encodedName The encoded name to be cleaned up. Assumed not <code>null</code>
     * @return the readable name without double quotes
     */
    private String getReadableName(String encodedName) throws PSDataServiceException {
        String decodedName = StringUtils.EMPTY;
        
        try
        {
            decodedName = encodedName.replace("\"", "");
        }
        catch (Exception e)
        {
            throw new PSDataServiceException("Failed to decode name = " + encodedName, e);
        }
        return decodedName;
    }
    
}
