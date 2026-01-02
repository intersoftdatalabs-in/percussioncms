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

<<<<<<< HEAD
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
=======
import static org.junit.Assert.*;
>>>>>>> development-8.1.x

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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
<<<<<<< HEAD
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
=======
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;

>>>>>>> development-8.1.x
public class PSORevisionCorrectingItemFilterTest {

  private static final Logger log = LogManager.getLogger(PSORevisionCorrectingItemFilterTest.class);

<<<<<<< HEAD
  @Mock private IPSGuidManager gmgr;
  @Mock private IPSWorkflowService work;
  @Mock private IPSCmsContentSummaries summ;
  @Mock private IPSFilterItem item;
  @Mock private IPSFilterItem item2;
  @Mock private PSComponentSummary summary;
  @Mock private PSState state;

  private PSORevisionCorrectingItemFilter cut;

  @BeforeEach
  public void setUp() {
    cut = new PSORevisionCorrectingItemFilter();
    PSORevisionCorrectingItemFilter.setGmgr(gmgr);
    PSORevisionCorrectingItemFilter.setWork(work);
=======
  Mockery context;
  PSORevisionCorrectingItemFilter cut;

  IPSGuidManager gmgr;
  IPSWorkflowService work;
  IPSCmsContentSummaries summ;

  @Before
  public void setUp() throws Exception {
    context =
        new Mockery() {
          {
            setImposteriser(ClassImposteriser.INSTANCE);
          }
        };

    cut = new PSORevisionCorrectingItemFilter();

    gmgr = context.mock(IPSGuidManager.class, "gmgr");
    PSORevisionCorrectingItemFilter.setGmgr(gmgr);

    work = context.mock(IPSWorkflowService.class, "work");
    PSORevisionCorrectingItemFilter.setWork(work);

    summ = context.mock(IPSCmsContentSummaries.class, "summ");
>>>>>>> development-8.1.x
    PSORevisionCorrectingItemFilter.setSumm(summ);
  }

  @Test
<<<<<<< HEAD
  void testFilterListOfIPSFilterItemMapOfStringString() {
=======
  public final void testFilterListOfIPSFilterItemMapOfStringString() {
>>>>>>> development-8.1.x
    String wfStates = "fee,fi,fo,fum";
    Map<String, String> params = new HashMap<String, String>();
    params.put(PSORevisionCorrectingItemFilter.WORKFLOW_STATES, wfStates);

<<<<<<< HEAD
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
=======
    final IPSFilterItem item = context.mock(IPSFilterItem.class, "item");
    final IPSFilterItem item2 = context.mock(IPSFilterItem.class, "item2");
    final IPSGuid originalGuid = new PSLegacyGuid(3, 2);
    final IPSGuid correctedGuid = new PSLegacyGuid(3, 1);
    final PSLocator badLocator = new PSLocator(3, 1);
    final PSLocator goodLocator = new PSLocator(3, 2);

    final IPSGuid workflowAppGuid = new PSLegacyGuid(4, 1);
    final IPSGuid workflowStateGuid = new PSLegacyGuid(5, 1);

    final PSComponentSummary summary = context.mock(PSComponentSummary.class, "summary");

    final PSState state = context.mock(PSState.class, "state");

    try {
      context.checking(
          new Expectations() {
            {
              one(summ).loadComponentSummary(3);
              will(returnValue(summary));
              one(summary).getWorkflowAppId();
              will(returnValue(4));
              one(summary).getContentStateId();
              will(returnValue(5));
              one(gmgr).makeGuid(4, PSTypeEnum.WORKFLOW);
              will(returnValue(workflowAppGuid));
              one(gmgr).makeGuid(5, PSTypeEnum.WORKFLOW_STATE);
              will(returnValue(workflowStateGuid));
              one(item).getItemId();
              will(returnValue(originalGuid));
              one(gmgr).makeLocator(originalGuid);
              will(returnValue(badLocator));
              one(item).clone(with(any(IPSGuid.class)));
              will(returnValue(item2));
              one(work).loadWorkflowState(workflowStateGuid, workflowAppGuid);
              will(returnValue(state));
              one(state).getName();
              will(returnValue("fee"));
              one(summary).getCurrentLocator();
              will(returnValue(badLocator));
              one(gmgr).makeGuid(badLocator);
              will(returnValue(correctedGuid));
              one(item2).getItemId();
              will(returnValue(correctedGuid));
            }
          });
>>>>>>> development-8.1.x

      List<IPSFilterItem> res = cut.filter(Collections.<IPSFilterItem>singletonList(item), params);
      assertNotNull(res);
      assertEquals(1, res.size());
      assertEquals(correctedGuid, res.get(0).getItemId());
<<<<<<< HEAD
=======

      context.assertIsSatisfied();

>>>>>>> development-8.1.x
    } catch (PSFilterException ex) {
      log.error("Unexpected Exception " + ex, ex);
      fail("Exception");
    }
  }
}
