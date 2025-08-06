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
 * test.percussion.psoitemfilter PSORevisionCorrectingItemFilterTest.java
 *  
 * @author DavidBenua
 *
 */
package test.percussion.psoitemfilter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.pso.itemfilter.PSORevisionCorrectingItemFilter;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.filter.IPSFilterItem;
import com.percussion.services.filter.PSFilterException;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.legacy.IPSCmsContentSummaries;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.data.PSState;
import com.percussion.utils.guid.IPSGuid;

@ExtendWith(MockitoExtension.class)
public class PSORevisionCorrectingItemFilterTest
{
   
   private static final Logger log = LogManager.getLogger(PSORevisionCorrectingItemFilterTest.class);
   
   
   @Mock
   private IPSGuidManager gmgr;
   @Mock
   private IPSWorkflowService work;
   @Mock
   private IPSCmsContentSummaries summ;
   @Mock
   private IPSFilterItem item;
   @Mock
   private IPSFilterItem item2;
   @Mock
   private PSComponentSummary summary;
   @Mock
   private PSState state;

   private PSORevisionCorrectingItemFilter cut;

   @BeforeEach
   public void setUp() {
      cut = new PSORevisionCorrectingItemFilter();
      PSORevisionCorrectingItemFilter.setGmgr(gmgr);
      PSORevisionCorrectingItemFilter.setWork(work);
      PSORevisionCorrectingItemFilter.setSumm(summ);
   }
   @Test
   void testFilterListOfIPSFilterItemMapOfStringString()
   {
      String wfStates = "fee,fi,fo,fum"; 
      Map<String, String> params = new HashMap<String, String>(); 
      params.put(PSORevisionCorrectingItemFilter.WORKFLOW_STATES, wfStates); 
      
      final IPSGuid originalGuid = new PSGuid(PSTypeEnum.LEGACY_CONTENT, 3);
      final IPSGuid correctedGuid = new PSGuid(PSTypeEnum.LEGACY_CONTENT, 1);
      final PSLocator badLocator = new PSLocator(3, 1);
      final PSLocator goodLocator = new PSLocator(3, 2);
      final IPSGuid workflowAppGuid = new PSGuid(PSTypeEnum.WORKFLOW, 4);
      final IPSGuid workflowStateGuid = new PSGuid(PSTypeEnum.WORKFLOW_STATE, 5);

      try {
         when(summ.loadComponentSummary(3)).thenReturn(summary);
         when(summary.getWorkflowAppId()).thenReturn(4);
         when(summary.getContentStateId()).thenReturn(5);
         // Removed incorrect stubbing for makeGuid(int, PSTypeEnum)
         when(item.getItemId()).thenReturn(originalGuid);
         when(gmgr.makeLocator(originalGuid)).thenReturn(badLocator);
         when(item.clone(any(IPSGuid.class))).thenReturn(item2);
         when(work.loadWorkflowState(workflowStateGuid, workflowAppGuid)).thenReturn(state);
         when(state.getName()).thenReturn("fee");
         when(summary.getCurrentLocator()).thenReturn(badLocator);
         when(gmgr.makeGuid(badLocator)).thenReturn(correctedGuid);
         when(item2.getItemId()).thenReturn(correctedGuid);

         List<IPSFilterItem> res = cut.filter(Collections.<IPSFilterItem>singletonList(item), params);
         assertNotNull(res);
         assertEquals(1, res.size());
         assertEquals(correctedGuid, res.get(0).getItemId());
      } catch (PSFilterException ex) {
         log.error("Unexpected Exception " + ex, ex);
         fail("Exception");
      }
      
   }
}
