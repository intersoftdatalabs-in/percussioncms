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
import static org.mockito.ArgumentMatchers.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.percussion.extension.IPSExtensionManager;
import com.percussion.extension.IPSWorkflowAction;
import com.percussion.extension.PSExtensionRef;
import com.percussion.pso.workflow.IPSOWorkflowInfoFinder;
import com.percussion.pso.workflow.PSOWFActionService;
import com.percussion.services.workflow.data.PSTransition;
import com.percussion.services.workflow.data.PSWorkflow;

// REFACTORED: CP-JAVA11
@ExtendWith(MockitoExtension.class)
public class PSOWFActionServiceTest
{
   private static final Logger log = LogManager.getLogger(PSOWFActionServiceTest.class);
   
   TestablePSOWFActionService cut; 
   
   @Mock
   IPSExtensionManager emgr;
   
   @Mock
   IPSOWorkflowInfoFinder wfFinder; 
   
   @Mock
   IPSWorkflowAction action; 
   
   @Mock
   PSWorkflow wf;
   
   @Mock
   PSTransition trans;
   
   @BeforeEach
   public void setUp() throws Exception
   {
      cut = new TestablePSOWFActionService();
      cut.setExtMgr(emgr);
      cut.setWfFinder(wfFinder); 
   }
   
   @Test
   public final void testGetWorkflowAction()
   {
      final PSExtensionRef ref = new PSExtensionRef("java/user/xyz");
      
      try
      {
         Iterator<PSExtensionRef> refIterator = Arrays.asList(ref).iterator();
         when(emgr.getExtensionNames("Java", null, IPSWorkflowAction.class.getName(), "xyz"))
            .thenReturn(refIterator);
         when(emgr.prepareExtension(ref, null))
            .thenReturn(action);
         
         IPSWorkflowAction result = cut.getWorkflowAction("xyz");
         assertNotNull(result);
         assertEquals(action,result); 
         
         verify(emgr).getExtensionNames("Java", null, IPSWorkflowAction.class.getName(), "xyz");
         verify(emgr).prepareExtension(ref, null);
         
      } catch (Exception ex)
      {
          log.error("Unexpected Exception " + ex,ex);
          fail("Exception");
      }
   }
   
   @Test
   public final void testGetTransitionActions()
   {
      final PSExtensionRef ref = new PSExtensionRef("java/user/xyz");
      Map<String, Map<String,List<String>>> wconfig = new HashMap<String, Map<String,List<String>>>();
      Map<String,List<String>> tconfig = new HashMap<String, List<String>>();
      List<String> aconfig = new ArrayList<String>();
      aconfig.add("xyz");
      tconfig.put("trans1", aconfig); 
      wconfig.put("wf1", tconfig);
      
      cut.setTransitionActions(wconfig); 
      
      try
      {
         when(wfFinder.findWorkflow(1)).thenReturn(wf);
         when(wf.getName()).thenReturn("wf1");
         when(wfFinder.findWorkflowAnyTransition(wf, 1)).thenReturn(trans);
         when(trans.getLabel()).thenReturn("trans1");
         
         Iterator<PSExtensionRef> refIterator = Arrays.asList(ref).iterator();
         when(emgr.getExtensionNames("Java", null, IPSWorkflowAction.class.getName(), "xyz"))
            .thenReturn(refIterator);
         when(emgr.prepareExtension(ref, null))
            .thenReturn(action);
         
         List<IPSWorkflowAction> result = cut.getActions(1, 1);
         assertNotNull(result);
         assertEquals(1,result.size());  
         IPSWorkflowAction act2 = result.get(0);
         assertEquals(action, act2); 
         
         verify(wfFinder).findWorkflow(1);
         verify(wf).getName();
         verify(wfFinder).findWorkflowAnyTransition(wf, 1);
         verify(trans).getLabel();
         verify(emgr).getExtensionNames("Java", null, IPSWorkflowAction.class.getName(), "xyz");
         verify(emgr).prepareExtension(ref, null);
         
      } catch (Exception ex)
      {
          log.error("Unexpected Exception " + ex,ex);
          fail("Exception");
      }
   }
   
   private class TestablePSOWFActionService extends PSOWFActionService
   {

      /**
       * @see com.percussion.pso.workflow.PSOWFActionService#setExtMgr(com.percussion.extension.IPSExtensionManager)
       */
      @Override
      public void setExtMgr(IPSExtensionManager extMgr)
      {
         super.setExtMgr(extMgr);
      }

      /**
       * @see com.percussion.pso.workflow.PSOWFActionService#setWfFinder(com.percussion.pso.workflow.IPSOWorkflowInfoFinder)
       */
      @Override
      public void setWfFinder(IPSOWorkflowInfoFinder wfFinder)
      {
         super.setWfFinder(wfFinder);
      }
      
   }
}
