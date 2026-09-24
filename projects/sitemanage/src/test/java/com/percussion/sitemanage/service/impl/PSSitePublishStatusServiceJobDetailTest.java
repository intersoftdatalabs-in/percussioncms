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
import static org.mockito.Mockito.when;

import com.percussion.rx.publisher.IPSPublisherJobStatus.State;
import com.percussion.rx.publisher.IPSRxPublisherServiceInternal;
import com.percussion.rx.publisher.data.PSPublisherJobStatus;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.publisher.IPSEdition;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.pubserver.IPSPubServerDao;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.sitemanage.data.PSSitePublishJob;
import com.percussion.utils.guid.IPSGuid;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Current-job mapping copies edition name and a failed job's publisher message (#4789). */
@ExtendWith(MockitoExtension.class)
class PSSitePublishStatusServiceJobDetailTest {

  @Mock private IPSRxPublisherServiceInternal rxPubSvc;
  @Mock private IPSPublisherService pubSvc;
  @Mock private IPSSiteManager siteMgr;
  @Mock private IPSGuidManager guidMgr;
  @Mock private IPSPubServerDao pubServerDao;
  @Mock private IPSEdition edition;
  @Mock private IPSSite site;
  @Mock private IPSGuid editionId;
  @Mock private IPSGuid siteId;
  @Mock private IPSGuid serverId;

  private PSSitePublishStatusService service;

  @BeforeEach
  void setUp() throws Exception {
    when(pubSvc.loadEdition(editionId)).thenReturn(edition);
    when(edition.getName()).thenReturn("Nightly Full");
    when(edition.getSiteId()).thenReturn(siteId);
    when(siteMgr.loadSite(siteId)).thenReturn(site);
    when(site.getName()).thenReturn("FastForward");
    when(edition.getPubServerId()).thenReturn(serverId);
    when(serverId.longValue()).thenReturn(9L);
    service = new PSSitePublishStatusService(rxPubSvc, pubSvc, siteMgr, guidMgr, pubServerDao);
  }

  @Test
  void failedJobCopiesEditionNameAndErrorText() throws Exception {
    PSPublisherJobStatus status = liveStatus(State.ABORTED, "disk full");
    PSSitePublishJob job = service.buildJob(42L, status);
    assertEquals("Nightly Full", job.getEditionName().orElseThrow());
    assertEquals("disk full", job.getErrorMessage().orElseThrow());
    assertEquals("Failed", job.getStatus().orElseThrow());
  }

  @Test
  void runningJobKeepsEditionButOmitsErrorText() throws Exception {
    PSPublisherJobStatus status = liveStatus(State.WORKING, "still going");
    PSSitePublishJob job = service.buildJob(7L, status);
    assertEquals("Nightly Full", job.getEditionName().orElseThrow());
    assertTrue(job.getErrorMessage().isEmpty());
  }

  @Test
  void currentJobsKeepFailedJobSoDetailCanShowError() throws Exception {
    PSPublisherJobStatus status = liveStatus(State.ABORTED, "disk full");
    when(rxPubSvc.getActiveJobIds()).thenReturn(List.of(42L));
    when(rxPubSvc.getPublishingJobStatus(42L)).thenReturn(status);
    List<PSSitePublishJob> jobs = service.buildCurrentJobs(null);
    assertEquals(1, jobs.size());
    assertEquals("disk full", jobs.get(0).getErrorMessage().orElseThrow());
    assertEquals("Failed", jobs.get(0).getStatus().orElseThrow());
  }

  @Test
  void blankEditionNameStaysEmpty() throws Exception {
    when(edition.getName()).thenReturn("  ");
    PSPublisherJobStatus status = liveStatus(State.WORKING, null);
    PSSitePublishJob job = service.buildJob(8L, status);
    assertTrue(job.getEditionName().isEmpty());
    assertTrue(job.getErrorMessage().isEmpty());
  }

  private PSPublisherJobStatus liveStatus(State state, String message) {
    PSPublisherJobStatus status = new PSPublisherJobStatus();
    status.setEditionId(editionId);
    status.setState(state);
    status.setStartTime(new Date());
    status.setMessage(message);
    return status;
  }
}
