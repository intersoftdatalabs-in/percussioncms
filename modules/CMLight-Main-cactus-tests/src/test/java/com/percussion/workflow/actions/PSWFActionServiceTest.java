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
package com.percussion.workflow.actions;

import com.percussion.error.PSNotFoundException;
import com.percussion.extension.IPSWorkflowAction;
import com.percussion.extension.PSExtensionException;
import com.percussion.utils.testing.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.experimental.categories.Category;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for PSWFActionService which is used by the workflow transition
 * action dispatcher extension.
 */
@Category(IntegrationTest.class)
class PSWFActionServiceTest {

    @Test
    void testGetActions() throws Exception {
        var wfActionService = PSWFActionServiceLocator.getPSWFActionService();

        List<IPSWorkflowAction> actionList = wfActionService.getActions(
                STANDARD_WORKFLOW, RETURN_TO_PUBLIC_TRANSITION);

        assertEquals(1, actionList.size(),
                "Found the wrong number of actions on Standard Workflow " +
                        "\"Return To Public\" Transition\n" +
                        "Found " + actionList.size() + " expected 1");

        var name = actionList.get(0).getClass().getSimpleName();

        assertEquals(PS_TOUCH_PARENT_ITEMS, name,
                "Action is " + name + " not " + PS_TOUCH_PARENT_ITEMS);

        actionList = wfActionService.getActions(STANDARD_WORKFLOW,
                MOVE_TO_QUICK_EDIT_TRANSITION);

        assertEquals(0, actionList.size(),
                "Found the wrong number of actions on Standard Workflow " +
                        "\"Move To Quick Edit\" Transition\n" +
                        "Found " + actionList.size() + " expected 0");
    }

    @Test
    void testGetWorkflowAction() throws PSExtensionException, PSNotFoundException {
        var wfActionService = PSWFActionServiceLocator.getPSWFActionService();

        var action = wfActionService.getWorkflowAction(SYS_TOUCH_PARENT_ITEMS);

        assertNotNull(action, PS_TOUCH_PARENT_ITEMS + " was not found!");

        var name = action.getClass().getSimpleName();

        assertEquals(PS_TOUCH_PARENT_ITEMS, name,
                "Action is " + name + " not " + PS_TOUCH_PARENT_ITEMS);

        action = wfActionService.getWorkflowAction(SYS_WORKFLOW_ACTION_DISPATCHER);

        assertNotNull(action, PS_SPRING_WORKFLOW_ACTION_DISPATCHER + " was not found!");

        name = action.getClass().getSimpleName();

        assertEquals(PS_SPRING_WORKFLOW_ACTION_DISPATCHER, name,
                "Action is " + name + " not " + PS_SPRING_WORKFLOW_ACTION_DISPATCHER);
    }

    private static final int STANDARD_WORKFLOW = 5;
    private static final int MOVE_TO_QUICK_EDIT_TRANSITION = 9;
    private static final int RETURN_TO_PUBLIC_TRANSITION = 11;
    private static final String PS_TOUCH_PARENT_ITEMS = "PSTouchParentItems";
    private static final String SYS_TOUCH_PARENT_ITEMS = "sys_TouchParentItems";
    private static final String PS_SPRING_WORKFLOW_ACTION_DISPATCHER = "PSSpringWorkflowActionDispatcher";
    private static final String SYS_WORKFLOW_ACTION_DISPATCHER = "sys_WorkflowActionDispatcher";
}
