/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
package com.percussion.sitemanage.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.publisher.IPSPubStatus;
import com.percussion.services.publisher.IPSPubStatus.EndingState;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.publisher.impl.PSPubStatusLogQuery;
import com.percussion.sitemanage.data.PSSitePublishJob;
import com.percussion.utils.guid.IPSGuid;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Logs day window and failures-only are passed to the publisher query, not trimmed in the JVM. */
class PSSitePublishLogWindowTest {

  @Test
  void siteAndServerDayWindowUsesFilteredQueryIncludingNonFailureRowsFromThatQuery()
      throws Exception {
    IPSPublisherService pub = mock(IPSPublisherService.class);
    IPSGuidManager guids = mock(IPSGuidManager.class);
    IPSGuid site = mock(IPSGuid.class);
    IPSGuid server = mock(IPSGuid.class);
    when(guids.makeGuid("9", PSTypeEnum.SITE)).thenReturn(site);
    when(guids.makeGuid("3", PSTypeEnum.PUBLISHING_SERVER)).thenReturn(server);

    IPSPubStatus row = mock(IPSPubStatus.class);
    when(row.getStatusId()).thenReturn(42L);
    when(row.getEndingState()).thenReturn(EndingState.COMPLETED);
    when(pub.findPubStatusBySiteAndServerWithFilters(site, server, 1, 20, 5, true))
        .thenReturn(List.of(row));

    Probe svc = new Probe(pub, guids);
    List<PSSitePublishJob> jobs = svc.buildLogs("9", "3", 1, 20, 5, false);

    assertEquals(1, jobs.size());
    assertEquals(42L, jobs.get(0).getJobId());
    verify(pub).findPubStatusBySiteAndServerWithFilters(site, server, 1, 20, 5, true);
    verify(pub, never()).findPubStatusBySiteAndServerWithFilters(site, server, 1, 20);
  }

  @Test
  void siteOnlyAndAllSitesPassShowAllWithoutFailurePredicate() throws Exception {
    IPSPublisherService pub = mock(IPSPublisherService.class);
    IPSGuidManager guids = mock(IPSGuidManager.class);
    IPSGuid site = mock(IPSGuid.class);
    when(guids.makeGuid("9", PSTypeEnum.SITE)).thenReturn(site);
    when(pub.findPubStatusBySiteWithFilters(site, 10, 30, 0, false)).thenReturn(List.of());
    when(pub.findAllPubStatusWithFilters(3, 20, 2, false)).thenReturn(List.of());

    Probe svc = new Probe(pub, guids);
    assertTrue(svc.buildLogs("9", " ", 10, 30, 0, true).isEmpty());
    assertTrue(svc.buildLogs(null, null, 3, 20, 2, true).isEmpty());

    verify(pub).findPubStatusBySiteWithFilters(site, 10, 30, 0, false);
    verify(pub).findAllPubStatusWithFilters(3, 20, 2, false);
    verify(pub, never()).findPubStatusBySiteWithFilters(site, 10, 30);
    verify(pub, never()).findAllPubStatusWithFilters(3, 20);
  }

  @Test
  void failurePredicateMatchesQueryOrdinals() {
    Probe svc = new Probe(mock(IPSPublisherService.class), mock(IPSGuidManager.class));
    for (EndingState state : EndingState.values()) {
      boolean failure = PSPubStatusLogQuery.failureEndingOrdinals().contains(state.ordinal());
      assertEquals(failure, svc.exposeFailure(state), state.name());
    }
  }

  private static final class Probe extends PSSitePublishStatusService {
    Probe(IPSPublisherService pub, IPSGuidManager guids) {
      super(null, pub, null, guids, null);
    }

    @Override
    protected PSSitePublishJob buildJob(IPSPubStatus status) {
      PSSitePublishJob job = new PSSitePublishJob();
      job.setJobId(status.getStatusId());
      return job;
    }

    boolean exposeFailure(EndingState state) {
      return isFailure(state);
    }
  }
}
