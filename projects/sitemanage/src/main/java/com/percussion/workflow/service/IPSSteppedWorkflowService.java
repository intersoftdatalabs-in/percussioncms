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
package com.percussion.workflow.service;

import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.workflow.data.PSState;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.data.PSEnumVals;
import com.percussion.workflow.data.PSUiWorkflow;

import java.util.List;

/**
 * The workflow service is responsible for exposing workflow information.
 *
 * @author leonardohildt
 * @author rafaelsalis
 */
public interface IPSSteppedWorkflowService {

    /** Metadata entry key prefix for workflow staging roles. */
    String METADATA_STAGING_ROLES_KEY_PREFIX = "psx.workflow.staging.roles.";

    /** Workflow staging roles separator. */
    String METADATA_STAGING_ROLES_VALUE_SEPARATOR = ";";

    /**
     * Loads workflow information by workflow name and builds a {@code PSUiWorkflow} object.
     *
     * @param workflowName the workflow name; cannot be empty or {@code null}
     * @return a {@code PSUiWorkflow} object, never {@code null}
     * @throws PSWorkflowEditorServiceException if the supplied object is invalid
     */
    PSUiWorkflow getWorkflow(String workflowName) throws PSWorkflowEditorServiceException;

    /**
     * Retrieves a list of all workflow names.
     *
     * @return a {@code PSEnumVals} containing workflow names, never empty or {@code null}
     * @throws PSWorkflowEditorServiceException if the supplied object is invalid
     */
    PSEnumVals getWorkflowList() throws PSWorkflowEditorServiceException;

    /**
     * Retrieves a list of all workflows including metadata.
     *
     * @return a list of {@code PSUiWorkflow}, never empty or {@code null}
     * @throws PSWorkflowEditorServiceException if the supplied object is invalid
     */
    List<PSUiWorkflow> getWorkflowMetadataList() throws PSWorkflowEditorServiceException;

    /**
     * Retrieves the id and name for the current default workflow.
     *
     * @return a {@code PSEnumVals} containing the workflow name, never empty or {@code null}
     */
    PSEnumVals getDefaultWorkflowMetadata();

    /**
     * Creates a new workflow with the given name.
     *
     * @param workflowName the workflow name
     * @param uiWorkflow the workflow information to create; must not be empty or {@code null}
     * @return a {@code PSUiWorkflow} object, never {@code null}
     * @throws PSWorkflowEditorServiceException if the supplied object is invalid
     */
    PSUiWorkflow createWorkflow(String workflowName, PSUiWorkflow uiWorkflow) throws PSWorkflowEditorServiceException;

    /**
     * Updates a workflow with the given name.
     *
     * @param workflowName the workflow name
     * @param uiWorkflow the workflow information to update; must not be empty or {@code null}
     * @return the updated workflow, never {@code null}
     * @throws PSWorkflowEditorServiceException if the supplied object is invalid
     */
    PSUiWorkflow updateWorkflow(String workflowName, PSUiWorkflow uiWorkflow)
            throws PSWorkflowEditorServiceException, PSNotFoundException, IPSGenericDao.LoadException, IPSGenericDao.SaveException;

    /**
     * Deletes a workflow with the given name.
     *
     * @param workflowName the workflow name; cannot be empty or {@code null}
     * @throws PSWorkflowEditorServiceException if the workflow is not found or contains associated items
     */
    void deleteWorkflow(String workflowName) throws PSWorkflowEditorServiceException;

    /**
     * Creates a new step with the information provided in the workflow object.
     *
     * @param workflowName the workflow name
     * @param stepName the step name; must match the {@code stepName} in {@code uiWorkflow}
     * @param uiWorkflow the workflow object containing the state to add; must not be empty or {@code null}
     * @return a {@code PSUiWorkflow} object, never {@code null}
     * @throws PSWorkflowEditorServiceException if the supplied object is invalid
     */
    PSUiWorkflow createStep(String workflowName, String stepName, PSUiWorkflow uiWorkflow) throws PSWorkflowEditorServiceException;

    /**
     * Updates the step with the information provided in the workflow object.
     *
     * @param workflowName the workflow name
     * @param stepName the step name; must match {@code previousStepName} in {@code uiWorkflow}
     * @param uiWorkflow the workflow object containing the state to update; must not be empty or {@code null}
     * @return the updated workflow, never {@code null}
     * @throws PSWorkflowEditorServiceException if the supplied object is invalid
     */
    PSUiWorkflow updateStep(String workflowName, String stepName, PSUiWorkflow uiWorkflow) throws PSWorkflowEditorServiceException;

    /**
     * Deletes the given step from the workflow, repointing transitions as needed.
     *
     * @param workflowName the workflow name
     * @param stepName the step name to delete
     * @return the updated workflow, never {@code null}
     * @throws PSWorkflowEditorServiceException if the workflow or step are not found, or items exist
     */
    PSUiWorkflow deleteStep(String workflowName, String stepName) throws PSWorkflowEditorServiceException;

    /**
     * Finds all states belonging to the workflow with the given name.
     *
     * @param workflowName the workflow name; may be empty or {@code null}
     * @return a list of {@code PSState}, possibly empty or {@code null}
     */
    List<PSState> getStates(String workflowName);

    /**
     * Thrown when an error is encountered in the workflow service.
     */
    class PSWorkflowEditorServiceException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public PSWorkflowEditorServiceException() {
            super();
        }

        public PSWorkflowEditorServiceException(String message) {
            super(message);
        }

        public PSWorkflowEditorServiceException(String message, Throwable cause) {
            super(message, cause);
        }

        public PSWorkflowEditorServiceException(Throwable cause) {
            super(cause);
        }
    }
}
