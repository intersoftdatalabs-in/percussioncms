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
package test.percussion.pso.demandpreview.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

import com.percussion.pso.demandpreview.service.impl.DemandPublisherBean;
import com.percussion.rx.publisher.IPSPublisherJobStatus.State;
import com.percussion.rx.publisher.IPSRxPublisherService;
import com.percussion.rx.publisher.data.PSDemandWork;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.publisher.IPSEdition;
import com.percussion.utils.guid.IPSGuid;
import java.util.concurrent.TimeoutException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DemandPublisherBeanTest {
  private static final Logger log = LogManager.getLogger(DemandPublisherBeanTest.class);

  @Mock IPSRxPublisherService rxPubSvc;
  TestableDemandPublisherBean cut;

  @BeforeEach
  public void setUp() throws Exception {
    cut = new TestableDemandPublisherBean();
    cut.setRxPubSvc(rxPubSvc);
  }

  @Test
  public final void testQueueDemandWork() {
    final IPSEdition edition = mock(IPSEdition.class);
    final IPSGuid content = mock(IPSGuid.class);
    final IPSGuid folder = mock(IPSGuid.class);
    final IPSGuid editionId = mock(IPSGuid.class);

    try {
      when(edition.getGUID()).thenReturn(editionId);
      when(editionId.getUUID()).thenReturn(12);
      when(rxPubSvc.queueDemandWork(eq(12), any(PSDemandWork.class))).thenReturn(42L);
      when(rxPubSvc.getDemandRequestJob(42L)).thenReturn(345L);

      long jobid = cut.queueDemandWork(edition, content, folder);
      assertEquals(42L, jobid);
      verify(rxPubSvc).queueDemandWork(eq(12), any(PSDemandWork.class));
    } catch (TimeoutException ex) {
      log.error("Timeout Exception " + ex, ex);
      fail("Exception");
    } catch (PSNotFoundException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      fail(e.getMessage());
    }
  }

  @Test
  public final void testWaitDemandWorkComplete() throws TimeoutException {
    final State tState = State.COMPLETED;
    final State qState = State.QUEUEING;

    when(rxPubSvc.getDemandWorkStatus(42L)).thenReturn(qState).thenReturn(tState);

    State state = cut.waitDemandWorkComplete(42L);
    assertTrue(state.isTerminal());
    assertEquals(State.COMPLETED, state);
  }

  @Test
  public final void testWaitDemandWorkTimeout() throws TimeoutException {
    final State qState = State.QUEUEING;
    cut.setTimeout(3);

    when(rxPubSvc.getDemandWorkStatus(42L)).thenReturn(qState);

    try {
      cut.waitDemandWorkComplete(42L);
      fail("did not get timeout exception");
    } catch (TimeoutException ex) {
      log.info("Caught Expected Exception " + ex);
    }
  }

  private class TestableDemandPublisherBean extends DemandPublisherBean {

    @Override
    public void setRxPubSvc(IPSRxPublisherService rxPubSvc) {
      super.setRxPubSvc(rxPubSvc);
    }
  }
}
