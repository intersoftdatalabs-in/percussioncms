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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rx.publisher.IPSPublisherJobStatus.State;
import com.percussion.rx.publisher.IPSRxPublisherServiceInternal;
import com.percussion.rx.publisher.data.PSPublisherJobStatus;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.publisher.IPSEdition;
import com.percussion.services.publisher.IPSPubStatus;
import com.percussion.services.publisher.IPSPubStatus.EndingState;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.pubserver.IPSPubServerDao;
import com.percussion.services.pubserver.data.PSPubServer;
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
  @Mock private PSPubServer pubServer;
  @Mock private IPSEdition edition;
  @Mock private IPSSite site;
  @Mock private IPSGuid editionId;
  @Mock private IPSGuid siteId;
  @Mock private IPSGuid serverId;
  @Mock private IPSPubStatus failedPersisted;
  @Mock private IPSPubStatus cleanPersisted;
  @Mock private IPSPubStatus cancelledPersisted;

  private PSSitePublishStatusService service;

  @BeforeEach
  void setUp() throws Exception {
    when(pubSvc.loadEdition(editionId)).thenReturn(edition);
    when(edition.getName()).thenReturn("Nightly Full");
    when(edition.getSiteId()).thenReturn(siteId);
    when(siteMgr.loadSite(siteId)).thenReturn(site);
    when(site.getName()).thenReturn("FastForward");
    lenient().when(edition.getPubServerId()).thenReturn(serverId);
    lenient().when(serverId.longValue()).thenReturn(9L);
    lenient().when(pubServerDao.loadPubServer(serverId)).thenReturn(pubServer);
    lenient().when(pubServer.getName()).thenReturn("Staging");
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
  void finishedFailureStaysListedAfterItLeavesActiveIds() throws Exception {
    stubEnding(failedPersisted, 4807L, EndingState.COMPLETED_W_FAILURE);
    stubBuilt(failedPersisted);
    stubEnding(cleanPersisted, 11L, EndingState.COMPLETED);
    stubEnding(cancelledPersisted, 12L, EndingState.CANCELED_BY_USER);
    when(rxPubSvc.getActiveJobIds()).thenReturn(List.of());
    when(pubSvc.findAllPubStatusWithFilters(
            PSSitePublishStatusService.FAILED_JOB_LOOKBACK_DAYS,
            PSSitePublishStatusService.FAILED_JOB_SCAN_MAX))
        .thenReturn(List.of(cleanPersisted, failedPersisted, cancelledPersisted));

    List<PSSitePublishJob> jobs = service.buildCurrentJobs(null);

    assertEquals(1, jobs.size());
    assertEquals(4807L, jobs.get(0).getJobId());
    assertEquals("Completed with failures", jobs.get(0).getStatus().orElseThrow());
    assertEquals("Nightly Full", jobs.get(0).getEditionName().orElseThrow());
  }

  @Test
  void abortedAndRestartNeededStayListedWithoutActiveIds() throws Exception {
    IPSPubStatus aborted = org.mockito.Mockito.mock(IPSPubStatus.class);
    IPSPubStatus restart = org.mockito.Mockito.mock(IPSPubStatus.class);
    stubEnding(aborted, 3L, EndingState.ABORTED);
    stubBuilt(aborted);
    stubEnding(restart, 4L, EndingState.RESTARTNEEDED);
    stubBuilt(restart);
    when(rxPubSvc.getActiveJobIds()).thenReturn(List.of());
    when(pubSvc.findAllPubStatusWithFilters(
            PSSitePublishStatusService.FAILED_JOB_LOOKBACK_DAYS,
            PSSitePublishStatusService.FAILED_JOB_SCAN_MAX))
        .thenReturn(List.of(aborted, restart));

    List<PSSitePublishJob> jobs = service.buildCurrentJobs("");

    assertEquals(2, jobs.size());
    assertEquals("Failed", jobs.get(0).getStatus().orElseThrow());
    assertEquals("Failed", jobs.get(1).getStatus().orElseThrow());
    assertEquals(3L, jobs.get(0).getJobId());
    assertEquals(4L, jobs.get(1).getJobId());
  }

  @Test
  void runningJobAndDroppedFailureBothStayOnCurrentList() throws Exception {
    PSPublisherJobStatus running = liveStatus(State.WORKING, null);
    stubEnding(failedPersisted, 4807L, EndingState.COMPLETED_W_FAILURE);
    stubBuilt(failedPersisted);
    when(rxPubSvc.getActiveJobIds()).thenReturn(List.of(7L));
    when(rxPubSvc.getPublishingJobStatus(7L)).thenReturn(running);
    when(pubSvc.findAllPubStatusWithFilters(
            PSSitePublishStatusService.FAILED_JOB_LOOKBACK_DAYS,
            PSSitePublishStatusService.FAILED_JOB_SCAN_MAX))
        .thenReturn(List.of(failedPersisted));

    List<PSSitePublishJob> jobs = service.buildCurrentJobs(null);

    assertEquals(2, jobs.size());
    assertEquals(7L, jobs.get(0).getJobId());
    assertEquals("Running", jobs.get(0).getStatus().orElseThrow());
    assertEquals(4807L, jobs.get(1).getJobId());
    assertEquals("Completed with failures", jobs.get(1).getStatus().orElseThrow());
  }

  @Test
  void liveFailedJobIsNotDuplicatedByPersistedRow() throws Exception {
    PSPublisherJobStatus live = liveStatus(State.ABORTED, "disk full");
    when(failedPersisted.getStatusId()).thenReturn(42L);
    when(rxPubSvc.getActiveJobIds()).thenReturn(List.of(42L));
    when(rxPubSvc.getPublishingJobStatus(42L)).thenReturn(live);
    when(pubSvc.findAllPubStatusWithFilters(
            PSSitePublishStatusService.FAILED_JOB_LOOKBACK_DAYS,
            PSSitePublishStatusService.FAILED_JOB_SCAN_MAX))
        .thenReturn(List.of(failedPersisted));

    List<PSSitePublishJob> jobs = service.buildCurrentJobs(null);

    assertEquals(1, jobs.size());
    assertEquals("Failed", jobs.get(0).getStatus().orElseThrow());
    assertEquals("disk full", jobs.get(0).getErrorMessage().orElseThrow());
  }

  @Test
  void siteScopedCurrentListUsesSiteStatusNotAllSites() throws Exception {
    stubEnding(failedPersisted, 4807L, EndingState.COMPLETED_W_FAILURE);
    stubBuilt(failedPersisted);
    when(guidMgr.makeGuid("10", PSTypeEnum.SITE)).thenReturn(siteId);
    when(rxPubSvc.getActiveJobIds(siteId)).thenReturn(List.of());
    when(pubSvc.findPubStatusBySiteWithFilters(
            siteId,
            PSSitePublishStatusService.FAILED_JOB_LOOKBACK_DAYS,
            PSSitePublishStatusService.FAILED_JOB_SCAN_MAX))
        .thenReturn(List.of(failedPersisted));

    List<PSSitePublishJob> jobs = service.buildCurrentJobs("10");

    assertEquals(1, jobs.size());
    assertEquals(4807L, jobs.get(0).getJobId());
    verify(pubSvc, never())
        .findAllPubStatusWithFilters(
            PSSitePublishStatusService.FAILED_JOB_LOOKBACK_DAYS,
            PSSitePublishStatusService.FAILED_JOB_SCAN_MAX);
  }

  @Test
  void blankEditionNameStaysEmpty() throws Exception {
    when(edition.getName()).thenReturn("  ");
    PSPublisherJobStatus status = liveStatus(State.WORKING, null);
    PSSitePublishJob job = service.buildJob(8L, status);
    assertTrue(job.getEditionName().isEmpty());
    assertTrue(job.getErrorMessage().isEmpty());
  }

  private void stubEnding(IPSPubStatus status, long jobId, EndingState ending) {
    when(status.getStatusId()).thenReturn(jobId);
    when(status.getEndingState()).thenReturn(ending);
  }

  private void stubBuilt(IPSPubStatus status) {
    when(status.getEditionId()).thenReturn(99L);
    when(status.getStartDate()).thenReturn(new Date());
    when(status.getDeliveredCount()).thenReturn(1);
    when(status.getRemovedCount()).thenReturn(0);
    when(status.getFailedCount()).thenReturn(2);
    when(guidMgr.makeGuid(99L, PSTypeEnum.EDITION)).thenReturn(editionId);
    lenient().when(guidMgr.makeGuid(0L, PSTypeEnum.PUBLISHING_SERVER)).thenReturn(serverId);
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
