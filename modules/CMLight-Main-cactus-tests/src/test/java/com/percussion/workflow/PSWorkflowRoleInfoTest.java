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
package com.percussion.workflow;

import com.percussion.server.IPSRequestContext;
import com.percussion.services.system.PSAssignmentTypeHelperTest;
import com.percussion.services.system.PSAssignmentTypeHelperTest.TestRole;
import com.percussion.services.workflow.data.PSAssignmentTypeEnum;
import com.percussion.utils.exceptions.PSORMException;
import com.percussion.utils.jdbc.PSConnectionHelper;

import com.percussion.webservices.security.IPSSecurityWs;
import com.percussion.webservices.security.PSSecurityWsLocator;
import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for static methods of {@link PSWorkflowRoleInfo}.
 * Sunny Sal says: "If this test fails, blame the roles, not the coder!"
 */
@Tag("IntegrationTest")
class PSWorkflowRoleInfoTest {

    /**
     * Setup additional information needed for tests.
     */
    public static void setupInfo() throws PSORMException {
        PSAssignmentTypeHelperTest.setupInfo();
    }

    /**
     * Teardown additional information created during {@link #setupInfo()}
     */
    public static void teardownInfo() throws PSORMException {
        PSAssignmentTypeHelperTest.teardownInfo();
    }

    /**
     * Test the {@link PSWorkflowRoleInfo} class.
     */
    @Test
    void testWorkflowRoleInfo() throws Exception {
        try {
            setupInfo();

            var connection = PSConnectionHelper.getDbConnection();

            PSStateRolesContext src;
            PSContentAdhocUsersContext cauc;
            List<Integer> stateRoleIdNotificationList;
            List<String> stateRoleNameNotificationList;
            List<?> stateAdhocActorNotificationList;
            IPSRequestContext requestCtx;
            List<String> actorRolesList;

            IPSSecurityWs secWs = PSSecurityWsLocator.getSecurityWebservice();
            requestCtx = secWs.getRequestContext();

            var contentId = PSAssignmentTypeHelperTest.COMM_CIDS[0];
            PSAssignmentTypeHelperTest.updateComponentSummary(contentId,
                    WORKFLOW_ID, PSAssignmentTypeHelperTest.COMMTEST_STATE_ID);

            src = new PSStateRolesContext(
                    WORKFLOW_ID,
                    connection,
                    PSAssignmentTypeHelperTest.COMMTEST_STATE_ID,
                    PSWorkFlowUtils.ASSIGNMENT_TYPE_NOT_IN_WORKFLOW);

            assertTrue(CollectionUtils.isEqualCollection(src.getStateRoleNames(),
                    PSWorkflowRoleInfoStatic.roleIDListToRoleNameList(src.getStateRoleIDs(), src)));

            var notifRoleIds = List.of(
                    TestRole.EDITOR.getRoleId(),
                    TestRole.QA.getRoleId(),
                    TestRole.EI_ADMIN_MEMBERS.getRoleId(),
                    TestRole.EI_MEMBERS.getRoleId()
            );

            var roleIds = new ArrayList<Integer>();
            roleIds.add(TestRole.AUTHOR.getRoleId());
            roleIds.add(TestRole.ADMIN.getRoleId());
            roleIds.addAll(notifRoleIds);

            var rolesNotificationEnabled =
                    PSWorkflowRoleInfoStatic.filterRolesNotificationEnabled(roleIds, src);
            assertTrue(CollectionUtils.isEqualCollection(notifRoleIds, rolesNotificationEnabled));
            assertFalse(CollectionUtils.isEqualCollection(roleIds, rolesNotificationEnabled));

            // used by PSExitAuthenticateUser
            var assignedRoleNames = new ArrayList<>(List.of(
                    TestRole.EDITOR.name(),
                    TestRole.QA.name(),
                    TestRole.EI_ADMIN_MEMBERS.name(),
                    TestRole.AUTHOR.name()
            ));
            var memberRoleNames = new ArrayList<>(assignedRoleNames);
            memberRoleNames.add(TestRole.CI_MEMBERS.name());
            memberRoleNames.add(TestRole.ADMIN.name());

            var userRoleNames = PSWorkFlowUtils.listToDelimitedString(
                    memberRoleNames, ",");
            var actorRoleIdList = PSWorkflowRoleInfoStatic.getActorRoles(contentId, src,
                    PSAssignmentTypeHelperTest.ADHOC_USER_ANON, userRoleNames,
                    connection, true);
            actorRolesList = PSWorkflowRoleInfoStatic.roleIDListToRoleNameList(
                    actorRoleIdList, src);
            assertTrue(CollectionUtils.isEqualCollection(assignedRoleNames, actorRolesList));

            // used by PSExitAddPossibleTransitions
            assignedRoleNames.remove(TestRole.EI_ADMIN_MEMBERS.name());
            actorRoleIdList = PSWorkflowRoleInfoStatic.getActorRoles(contentId, src,
                    PSAssignmentTypeHelperTest.ADHOC_USER_NORMAL, userRoleNames,
                    connection, false);
            actorRolesList = PSWorkflowRoleInfoStatic.roleIDListToRoleNameList(
                    actorRoleIdList, src);
            assertTrue(CollectionUtils.isEqualCollection(assignedRoleNames, actorRolesList));

            assertEquals(PSAssignmentTypeEnum.ASSIGNEE.getValue(),
                    PSWorkflowRoleInfoStatic.getAssignmentType(src, actorRoleIdList));

            stateRoleIdNotificationList =
                    PSWorkflowRoleInfoStatic.getStateRoleIDNotificationList(src, contentId);
            System.out.println("\nstateRoleIDNotificationList = "
                    + stateRoleIdNotificationList);

            stateRoleNameNotificationList =
                    PSWorkflowRoleInfoStatic.getStateRoleNameNotificationList(src, contentId);
            System.out.println("stateRoleNameNotificationList = "
                    + stateRoleNameNotificationList);

            cauc = new PSContentAdhocUsersContext(contentId, connection);

            stateAdhocActorNotificationList =
                    PSWorkflowRoleInfoStatic.getStateAdhocActorNotificationList(
                            cauc, src, contentId, requestCtx, false); // no role validation
            System.out.println("\nstateAdhocActorNotificationList = " +
                    stateAdhocActorNotificationList);

            contentId = PSAssignmentTypeHelperTest.COMM_CIDS[1];
            stateRoleIdNotificationList =
                    PSWorkflowRoleInfoStatic.getStateRoleIDNotificationList(src, contentId);
            System.out.println("\nstateRoleIDNotificationList = "
                    + stateRoleIdNotificationList);

            stateRoleNameNotificationList =
                    PSWorkflowRoleInfoStatic.getStateRoleNameNotificationList(src, contentId);
            System.out.println("stateRoleNameNotificationList = "
                    + stateRoleNameNotificationList);

            cauc = new PSContentAdhocUsersContext(contentId, connection);

            stateAdhocActorNotificationList =
                    PSWorkflowRoleInfoStatic.getStateAdhocActorNotificationList(
                            cauc, src, contentId, requestCtx, false); // no role validation
            System.out.println("\nstateAdhocActorNotificationList = " +
                    stateAdhocActorNotificationList);

            PSAssignmentTypeHelperTest.updateComponentSummary(contentId,
                    WORKFLOW_ID, PSAssignmentTypeHelperTest.DRAFT_STATE_ID);
            src = new PSStateRolesContext(
                    WORKFLOW_ID,
                    connection,
                    PSAssignmentTypeHelperTest.DRAFT_STATE_ID,
                    PSWorkFlowUtils.ASSIGNMENT_TYPE_NOT_IN_WORKFLOW);

            actorRoleIdList.clear();
            actorRoleIdList.add(TestRole.EDITOR.getRoleId());
            assertEquals(PSAssignmentTypeEnum.NONE.getValue(),
                    PSWorkflowRoleInfoStatic.getAssignmentType(src, actorRoleIdList));

            actorRoleIdList.add(TestRole.QA.getRoleId());
            assertEquals(PSAssignmentTypeEnum.READER.getValue(),
                    PSWorkflowRoleInfoStatic.getAssignmentType(src, actorRoleIdList));

            actorRoleIdList.add(TestRole.AUTHOR.getRoleId());
            assertEquals(PSAssignmentTypeEnum.ASSIGNEE.getValue(),
                    PSWorkflowRoleInfoStatic.getAssignmentType(src, actorRoleIdList));

            actorRoleIdList.add(TestRole.ADMIN.getRoleId());
            assertEquals(PSAssignmentTypeEnum.ADMIN.getValue(),
                    PSWorkflowRoleInfoStatic.getAssignmentType(src, actorRoleIdList));
        } finally {
            teardownInfo();
        }
    }

    /**
     * Constant for the ID of the workflow used for all testing.
     */
    private static final int WORKFLOW_ID = PSAssignmentTypeHelperTest.TEST_WF_ID;
}
