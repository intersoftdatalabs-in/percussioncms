// REFACTORED: CP-JAVA11
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

import com.percussion.cx.objectstore.PSMenuAction;
import com.percussion.maintenance.service.IPSMaintenanceManager;
import com.percussion.maintenance.service.IPSMaintenanceProcess;
import com.percussion.services.catalog.data.PSObjectSummary;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.PSWorkflowException;
import com.percussion.services.workflow.data.PSAssignmentTypeEnum;
import com.percussion.services.workflow.data.PSContentAdhocUser;
import com.percussion.services.workflow.data.PSContentApproval;
import com.percussion.services.workflow.data.PSContentWorkflowState;
import com.percussion.services.workflow.data.PSNotification;
import com.percussion.services.workflow.data.PSState;
import com.percussion.services.workflow.data.PSTransition;
import com.percussion.services.workflow.data.PSWorkflow;
import com.percussion.utils.guid.IPSGuid;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for PSWorkflowCacheBuilder.
 */
public class PSWorkflowCacheBuilderTest {

    @Test
    public void testBuildWorkflowCache() {
        var maintMgr = new MockMaintMgr();
        var wfsvc = new MockWorkflowService();
        var cacheBuilder = new PSWorkflowCacheBuilder(wfsvc, maintMgr);

        cacheBuilder.buildWorkflowCache();

        int tries = 0;
        while (!wfsvc.didLoadWorkflows) {
            tries++;
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                fail("Threadus Interruptus");
            }

            if (tries > 1000) {
                fail("Did not build workflow cache in allotted time");
            }
        }

        assertTrue(maintMgr.didStartWork, "Maint proc not started");
        assertTrue(maintMgr.didStopWork, "Maint proc not stopped");
    }

    private static class MockMaintMgr implements IPSMaintenanceManager {

        boolean didStartWork = false;
        String procId = null;
        boolean didStopWork = false;
        boolean hasFailures = false;

        @Override
        public void startingWork(IPSMaintenanceProcess process) {
            procId = process.getProcessId();
            didStartWork = true;
        }

        @Override
        public boolean isWorkInProgress() {
            return didStartWork && !didStopWork;
        }

        @Override
        public void workCompleted(IPSMaintenanceProcess process) {
            if (process.getProcessId().equals(procId))
                didStopWork = true;
        }

        @Override
        public boolean hasFailures() {
            return hasFailures;
        }

        @Override
        public void workFailed(IPSMaintenanceProcess process) {
            hasFailures = true;
        }

        @Override
        public boolean clearFailures() {
            boolean hadFailures = hasFailures;
            hasFailures = false;
            return hadFailures;
        }
    }

    private static class MockWorkflowService implements IPSWorkflowService {
        private boolean didLoadWorkflows = false;

        @Override
        public List<PSWorkflow> findWorkflowsByName(String name) {
            didLoadWorkflows = true;
            return null;
        }

        @Override
        public List<PSObjectSummary> findWorkflowSummariesByName(String name) {
            return null;
        }

        @Override
        public PSWorkflow loadWorkflow(IPSGuid id) {
            return null;
        }

        @Override
        public PSWorkflow loadWorkflowDb(IPSGuid id) {
            return null;
        }

        @Override
        public void saveWorkflow(PSWorkflow workflow) {
        }

        @Override
        public void deleteWorkflow(IPSGuid wfid) throws Exception {
        }

        @Override
        public PSState loadWorkflowState(IPSGuid stateId, IPSGuid workflowId) {
            return null;
        }

        @Override
        public PSState loadWorkflowStateByName(String stateName, IPSGuid workflowId) {
            return null;
        }

        @Override
        public PSState createState(IPSGuid workflowId) {
            return null;
        }

        @Override
        public PSTransition createTransition(IPSGuid wfId, IPSGuid stateId) {
            return null;
        }

        @Override
        public PSNotification createNotification(IPSGuid wfId, IPSGuid transitionId) {
            return null;
        }

        @Override
        public boolean isPublic(IPSGuid stateid, IPSGuid workflowId) throws PSWorkflowException {
            return false;
        }

        @Override
        public List<PSContentAdhocUser> findAdhocInfoByUser(String username) {
            return null;
        }

        @Override
        public List<PSContentAdhocUser> findAdhocInfoByItem(IPSGuid contentId) {
            return null;
        }

        @Override
        public void saveContentAdhocUser(PSContentAdhocUser adhoc) {
        }

        @Override
        public void deleteContentAdhocUser(PSContentAdhocUser adhoc) {
        }

        @Override
        public List<PSContentWorkflowState> getWorkflowStateForContent(List<IPSGuid> contentids) {
            return null;
        }

        @Override
        public List<PSContentApproval> findApprovalsByUser(String username) {
            return null;
        }

        @Override
        public List<PSContentApproval> findApprovalsByItem(IPSGuid contentid) {
            return null;
        }

        @Override
        public void saveContentApproval(PSContentApproval approval) {
        }

        @Override
        public void deleteContentApprovals(IPSGuid contentid) {
        }

        @Override
        public List<PSMenuAction> getAllWorkflowActions(List<IPSGuid> contentids,
                                                        List<PSAssignmentTypeEnum> assignmentTypes, String userName, List<String> userRoles, String locale)
                throws PSWorkflowException {
            return null;
        }

        @Override
        public void updateWorkflowVersion(IPSGuid id) {
        }

        @Override
        public void addWorkflowRole(IPSGuid wfId, String roleName) {
        }

        @Override
        public void addRoleToWorkflow(IPSGuid id, String roleName, PSWorkflow wf) {
        }

        @Override
        public boolean removeWorkflowRole(IPSGuid wfId, String roleName) {
            return false;
        }

        @Override
        public PSWorkflow getDefaultWorkflow() {
            return null;
        }

        @Override
        public String getDefaultWorkflowName() {
            return null;
        }

        @Override
        public IPSGuid getDefaultWorkflowId() {
            return null;
        }

        @Override
        public void copyWorkflowToRole(String fromRole, String toRole) {
        }
    }
}
