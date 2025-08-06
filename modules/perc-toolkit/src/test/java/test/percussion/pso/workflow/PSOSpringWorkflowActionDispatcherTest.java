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
package test.percussion.pso.workflow;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.percussion.extension.IPSWorkFlowContext;
import com.percussion.extension.IPSWorkflowAction;
import com.percussion.pso.workflow.IPSOWFActionService;
import com.percussion.pso.workflow.PSOSpringWorkflowActionDispatcher;
import com.percussion.server.IPSRequestContext;

// REFACTORED: CP-JAVA11
@ExtendWith(MockitoExtension.class)
public class PSOSpringWorkflowActionDispatcherTest
{
   private static final Logger log = LogManager.getLogger(PSOSpringWorkflowActionDispatcherTest.class);
   
   TestablePSOSpringWorkflowActionDispatcher cut; 
   
   @Mock
   IPSOWFActionService asvc; 
   
   @Mock
   IPSRequestContext request;
   
   @Mock
   IPSWorkFlowContext wfContext;
   
   @Mock
   IPSWorkflowAction action;
   
   @BeforeEach
   public void setUp() throws Exception
   {
      cut = new TestablePSOSpringWorkflowActionDispatcher();
      cut.setAsvc(asvc);       
   }
   
   @Test
   public final void testPerformAction()
   {
      final List<IPSWorkflowAction> acts = new ArrayList<IPSWorkflowAction>();
      acts.add(action); 
      try
      {
         when(wfContext.getWorkflowID()).thenReturn(1);
         when(wfContext.getTransitionID()).thenReturn(2);
         when(asvc.getActions(1, 2)).thenReturn(acts);
         
         cut.performAction(wfContext, request);
         
         verify(wfContext).getWorkflowID();
         verify(wfContext).getTransitionID();
         verify(asvc).getActions(1, 2);
         verify(action).performAction(wfContext, request);
         
      } catch (Exception ex)
      {
         log.error("Unexpected Exception " + ex,ex);
         fail("Exception"); 
      } 
   }
   
   private class TestablePSOSpringWorkflowActionDispatcher extends PSOSpringWorkflowActionDispatcher
   {

      @Override
      public void setAsvc(IPSOWFActionService asvc)
      {
         super.setAsvc(asvc);
      }
      
   }
}
