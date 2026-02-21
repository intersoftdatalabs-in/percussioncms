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

package com.percussion.sitemanage.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.rx.publisher.IPSPublisherJobStatus;
import com.percussion.rx.publisher.IPSRxPublisherServiceInternal;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.publisher.IPSEdition;
import com.percussion.services.publisher.IPSPubItemStatus;
import com.percussion.services.publisher.IPSPubStatus;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.publisher.IPSSiteItem;
import com.percussion.services.pubserver.IPSPubServerDao;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.utils.guid.IPSGuid;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

// REFACTORED: CP-JAVA11

class PSSitePublishStatusServiceTest {

  private static final Logger log = LogManager.getLogger(PSSitePublishStatusServiceTest.class);

  Mockery context;

  PSSitePublishStatusService cut;

  IPSGuidManager guidMgr;
  IPSSiteManager siteMgr;
  IPSPublisherService pubSvc;
  IPSRxPublisherServiceInternal rxPubSvc;
  IPSPubServerDao pubServerDao;

  @BeforeEach
  void setup() {
    context = new Mockery();
    guidMgr = context.mock(IPSGuidManager.class);
    siteMgr = context.mock(IPSSiteManager.class);
    pubSvc = context.mock(IPSPublisherService.class);
    rxPubSvc = context.mock(IPSRxPublisherServiceInternal.class);
    pubServerDao = context.mock(IPSPubServerDao.class);

    cut = new PSSitePublishStatusService(rxPubSvc, pubSvc, siteMgr, guidMgr, pubServerDao);
  }

  @Test
  void testBuildCurrentJobs() throws PSNotFoundException {
    var status = context.mock(IPSPublisherJobStatus.class);
    var editionId = context.mock(IPSGuid.class, "editionId");

    context.checking(
        new Expectations() {
          {
            one(rxPubSvc).getActiveJobIds();
            will(returnValue(Collections.singletonList(42L)));
            one(rxPubSvc).getPublishingJobStatus(42L);
            will(returnValue(status));
            one(status).getState();
            will(returnValue(IPSPublisherJobStatus.State.WORKING));
            one(status).countItemsDelivered();
            will(returnValue(186));
            one(status).countTotalItems();
            will(returnValue(384));
            one(status).getElapsed();
            will(returnValue(6371L));
            one(status).countFailedItems();
            will(returnValue(63));
            exactly(2).of(status).getStartTime();
            will(returnValue(new Date()));
            exactly(2).of(status).getEditionId();
            will(returnValue(editionId));
            one(status).getState();
            will(returnValue(IPSPublisherJobStatus.State.WORKING));
          }
        });

    siteExpectations(editionId);

    var jobs = cut.buildCurrentJobs(null);
    assertNotNull(jobs);
    assertEquals(1, jobs.size());
    var job = jobs.get(0);
    assertNotNull(job);
    assertEquals(42L, job.getJobId());
    assertEquals("Running", job.getStatus());
    assertEquals("Site1", job.getSiteName());

    context.assertIsSatisfied();
  }

  @Test
  void testBuildCurrentJobsByServerId() throws PSNotFoundException {
    var status = context.mock(IPSPublisherJobStatus.class);
    var editionId = context.mock(IPSGuid.class, "editionId");
    var siteId = context.mock(IPSGuid.class, "1");

    context.checking(
        new Expectations() {
          {
            one(rxPubSvc).getActiveJobIds(siteId);
            will(returnValue(Collections.singletonList(42L)));
            allowing(guidMgr).makeGuid("1", PSTypeEnum.SITE);
            will(returnValue(siteId));
            one(rxPubSvc).getPublishingJobStatus(42L);
            will(returnValue(status));
            one(status).getState();
            will(returnValue(IPSPublisherJobStatus.State.WORKING));
            one(status).countItemsDelivered();
            will(returnValue(186));
            one(status).countTotalItems();
            will(returnValue(384));
            one(status).getElapsed();
            will(returnValue(6371L));
            one(status).countFailedItems();
            will(returnValue(63));
            exactly(2).of(status).getStartTime();
            will(returnValue(new Date()));
            exactly(2).of(status).getEditionId();
            will(returnValue(editionId));
            one(status).getState();
            will(returnValue(IPSPublisherJobStatus.State.WORKING));
          }
        });

    siteExpectations(editionId);

    var jobs = cut.buildCurrentJobs("1");
    assertNotNull(jobs);
    assertEquals(1, jobs.size());
    var job = jobs.get(0);
    assertNotNull(job);
    assertEquals(42L, job.getJobId());
    assertEquals("Running", job.getStatus());
    assertEquals("Site1", job.getSiteName());

    context.assertIsSatisfied();
  }

  protected void siteExpectations(final IPSGuid editionId) throws PSNotFoundException {
    var edition = context.mock(IPSEdition.class);
    var siteId = context.mock(IPSGuid.class, "siteId");
    var site = context.mock(IPSSite.class);
    var pubServerId = context.mock(IPSGuid.class, "pubServerId");
    context.checking(
        new Expectations() {
          {
            exactly(2).of(pubSvc).loadEdition(editionId);
            will(returnValue(edition));
            one(edition).getSiteId();
            will(returnValue(siteId));
            one(siteMgr).loadSite(siteId);
            will(returnValue(site));
            one(site).getName();
            will(returnValue("Site1"));
            one(edition).getPubServerId();
            will(returnValue(pubServerId));
            one(pubServerId).longValue();
            will(returnValue(1L));
          }
        });
  }

  @Disabled
  void testBuildLogs() throws PSNotFoundException {
    var pubStatus = context.mock(IPSPubStatus.class);
    var editionId = context.mock(IPSGuid.class, "editionId");
    var startDate = new Date();

    context.checking(
        new Expectations() {
          {
            one(pubSvc).findAllPubStatus();
            will(returnValue(Collections.singletonList(pubStatus)));
            atLeast(1).of(pubStatus).getStartDate();
            will(returnValue(startDate));
            one(pubStatus).getStatusId();
            will(returnValue(47L));
            exactly(2).of(pubStatus).getDeliveredCount();
            will(returnValue(428));
            exactly(2).of(pubStatus).getFailedCount();
            will(returnValue(37));
            exactly(2).of(pubStatus).getRemovedCount();
            will(returnValue(28));
            one(pubStatus).getEndDate();
            will(returnValue(startDate));
            one(pubStatus).getEditionId();
            will(returnValue(302L));
            one(pubStatus).getEndingState();
            will(returnValue(IPSPubStatus.EndingState.COMPLETED));
            one(guidMgr).makeGuid(302L, PSTypeEnum.EDITION);
            will(returnValue(editionId));
          }
        });

    siteExpectations(editionId);

    var jobs = cut.buildLogs(null, "Server1", 10, 100);
    assertNotNull(jobs);
    assertEquals(1, jobs.size());
    var job = jobs.get(0);
    assertNotNull(job);
    assertEquals(47L, job.getJobId());
    assertEquals("Completed", job.getStatus());
    assertEquals("Site1", job.getSiteName());

    context.assertIsSatisfied();
  }

  @Disabled
  void testBuildLogsByServerId() throws PSNotFoundException {
    var pubStatus = context.mock(IPSPubStatus.class);
    var editionId = context.mock(IPSGuid.class, "editionId");
    var startDate = new Date();
    var siteId = context.mock(IPSGuid.class, "1");

    context.checking(
        new Expectations() {
          {
            one(pubSvc).findPubStatusBySite(siteId);
            will(returnValue(Collections.singletonList(pubStatus)));
            allowing(guidMgr).makeGuid("1", PSTypeEnum.SITE);
            will(returnValue(siteId));
            atLeast(1).of(pubStatus).getStartDate();
            will(returnValue(startDate));
            one(pubStatus).getStatusId();
            will(returnValue(47L));
            exactly(2).of(pubStatus).getDeliveredCount();
            will(returnValue(428));
            exactly(2).of(pubStatus).getFailedCount();
            will(returnValue(37));
            exactly(2).of(pubStatus).getRemovedCount();
            will(returnValue(28));
            one(pubStatus).getEndDate();
            will(returnValue(startDate));
            one(pubStatus).getEditionId();
            will(returnValue(302L));
            one(pubStatus).getEndingState();
            will(returnValue(IPSPubStatus.EndingState.COMPLETED));
            one(guidMgr).makeGuid(302L, PSTypeEnum.EDITION);
            will(returnValue(editionId));
          }
        });

    siteExpectations(editionId);

    var jobs = cut.buildLogs("1", null, 10, 100);
    assertNotNull(jobs);
    assertEquals(1, jobs.size());
    var job = jobs.get(0);
    assertNotNull(job);
    assertEquals(47L, job.getJobId());
    assertEquals("Completed", job.getStatus());
    assertEquals("Site1", job.getSiteName());

    context.assertIsSatisfied();
  }

  @Disabled
  void testBuildLogsFailuresOnly() throws PSNotFoundException {
    var pubStatusGood = context.mock(IPSPubStatus.class, "pubStatusGood");
    var pubStatusBad = context.mock(IPSPubStatus.class, "pubStatusBad");
    var editionId = context.mock(IPSGuid.class, "editionId");
    var startDate = new Date();
    var pubStatusList = List.of(pubStatusGood, pubStatusBad);

    context.checking(
        new Expectations() {
          {
            one(pubSvc).findAllPubStatus();
            will(returnValue(pubStatusList));

            atLeast(1).of(pubStatusGood).getStartDate();
            will(returnValue(startDate));
            atLeast(1).of(pubStatusGood).getEndingState();
            will(returnValue(IPSPubStatus.EndingState.COMPLETED));

            atLeast(1).of(pubStatusBad).getStartDate();
            will(returnValue(startDate));
            one(pubStatusBad).getStatusId();
            will(returnValue(48L));
            exactly(2).of(pubStatusBad).getDeliveredCount();
            will(returnValue(0));
            exactly(2).of(pubStatusBad).getFailedCount();
            will(returnValue(548));
            exactly(2).of(pubStatusBad).getRemovedCount();
            will(returnValue(0));
            one(pubStatusBad).getEndDate();
            will(returnValue(startDate));
            one(pubStatusBad).getEditionId();
            will(returnValue(302L));
            atLeast(2).of(pubStatusBad).getEndingState();
            will(returnValue(IPSPubStatus.EndingState.COMPLETED_W_FAILURE));

            one(guidMgr).makeGuid(302L, PSTypeEnum.EDITION);
            will(returnValue(editionId));
          }
        });

    siteExpectations(editionId);

    var jobs = cut.buildLogs(null, "Server1", 10, 100, 0, false);
    assertNotNull(jobs);
    assertEquals(1, jobs.size());
    var job = jobs.get(0);
    assertNotNull(job);
    assertEquals(48L, job.getJobId());
    assertEquals("Completed with failures", job.getStatus());
    assertEquals("Site1", job.getSiteName());

    context.assertIsSatisfied();
  }

  @Disabled
  void testBuildLogsSkip() throws PSNotFoundException {
    var pubStatusGood = context.mock(IPSPubStatus.class, "pubStatusGood");
    var pubStatusBad = context.mock(IPSPubStatus.class, "pubStatusBad");
    var editionId = context.mock(IPSGuid.class, "editionId");
    var startDate = new Date();
    var pubStatusList = List.of(pubStatusGood, pubStatusBad);

    context.checking(
        new Expectations() {
          {
            one(pubSvc).findAllPubStatus();
            will(returnValue(pubStatusList));

            atLeast(1).of(pubStatusBad).getStartDate();
            will(returnValue(startDate));
            one(pubStatusBad).getStatusId();
            will(returnValue(48L));
            exactly(2).of(pubStatusBad).getDeliveredCount();
            will(returnValue(0));
            exactly(2).of(pubStatusBad).getFailedCount();
            will(returnValue(548));
            exactly(2).of(pubStatusBad).getRemovedCount();
            will(returnValue(0));
            one(pubStatusBad).getEndDate();
            will(returnValue(startDate));
            one(pubStatusBad).getEditionId();
            will(returnValue(302L));
            atLeast(1).of(pubStatusBad).getEndingState();
            will(returnValue(IPSPubStatus.EndingState.COMPLETED_W_FAILURE));

            one(guidMgr).makeGuid(302L, PSTypeEnum.EDITION);
            will(returnValue(editionId));
          }
        });

    siteExpectations(editionId);

    var jobs = cut.buildLogs(null, "Server1", 10, 100, 1, true);
    assertNotNull(jobs);
    assertEquals(1, jobs.size());
    var job = jobs.get(0);
    assertNotNull(job);
    assertEquals(48L, job.getJobId());
    assertEquals("Completed with failures", job.getStatus());
    assertEquals("Site1", job.getSiteName());

    context.assertIsSatisfied();
  }

  @Test
  void testBuildItemDetails() {
    var status = context.mock(IPSPubItemStatus.class);

    context.checking(
        new Expectations() {
          {
            one(pubSvc).findPubItemStatusForJob(57L);
            will(returnValue(Collections.singletonList(status)));
            one(status).getStatusId();
            will(returnValue(482L));
            one(status).getContentId();
            will(returnValue(44));
            exactly(2).of(status).getLocation();
            will(returnValue("/foo/bar/baz.htm"));
            one(status).getElapsed();
            will(returnValue(1677));
            one(status).getOperation();
            will(returnValue(IPSSiteItem.Operation.PUBLISH));
            one(status).getStatus();
            will(returnValue(IPSSiteItem.Status.SUCCESS));
          }
        });

    var details = cut.buildItemDetails(57L);
    assertNotNull(details);
    assertEquals(1, details.size());

    context.assertIsSatisfied();
  }

  @Test
  void testBuildItemDetailsFail() {
    var statusGood = context.mock(IPSPubItemStatus.class, "statusGood");
    var statusBad = context.mock(IPSPubItemStatus.class, "statusBad");
    var statusList = List.of(statusGood, statusBad);

    context.checking(
        new Expectations() {
          {
            one(pubSvc).findPubItemStatusForJob(57L);
            will(returnValue(statusList));

            atLeast(1).of(statusGood).getStatus();
            will(returnValue(IPSSiteItem.Status.SUCCESS));

            one(statusBad).getStatusId();
            will(returnValue(483L));
            one(statusBad).getContentId();
            will(returnValue(47));
            exactly(2).of(statusBad).getLocation();
            will(returnValue("/foo/bar/baq.htm"));
            one(statusBad).getElapsed();
            will(returnValue(1677));
            one(statusBad).getOperation();
            will(returnValue(IPSSiteItem.Operation.PUBLISH));
            atLeast(1).of(statusBad).getStatus();
            will(returnValue(IPSSiteItem.Status.FAILURE));
            one(statusBad).getMessage();
          }
        });

    var details = cut.buildItemDetails(57L, 0, true);
    assertNotNull(details);
    assertEquals(1, details.size());
    var detail = details.get(0);
    assertEquals("Failed", detail.getStatus());

    context.assertIsSatisfied();
  }

  @Test
  void testBuildItemDetailsSkip() {
    var statusGood = context.mock(IPSPubItemStatus.class, "statusGood");
    var statusBad = context.mock(IPSPubItemStatus.class, "statusBad");
    var statusList = List.of(statusGood, statusBad);

    context.checking(
        new Expectations() {
          {
            one(pubSvc).findPubItemStatusForJob(57L);
            will(returnValue(statusList));

            one(statusBad).getStatusId();
            will(returnValue(483L));
            one(statusBad).getContentId();
            will(returnValue(47));
            exactly(2).of(statusBad).getLocation();
            will(returnValue("/foo/bar/baq.htm"));
            one(statusBad).getElapsed();
            will(returnValue(1677));
            one(statusBad).getOperation();
            will(returnValue(IPSSiteItem.Operation.PUBLISH));
            atLeast(1).of(statusBad).getStatus();
            will(returnValue(IPSSiteItem.Status.FAILURE));
            one(statusBad).getMessage();
          }
        });

    var details = cut.buildItemDetails(57L, 1, false);
    assertNotNull(details);
    assertEquals(1, details.size());
    var detail = details.get(0);
    assertEquals("Failed", detail.getStatus());

    context.assertIsSatisfied();
  }

  @Test
  void testIsJobActiveWorking() {
    var jobStatus = context.mock(IPSPublisherJobStatus.class);
    context.checking(
        new Expectations() {
          {
            one(jobStatus).getState();
            will(returnValue(IPSPublisherJobStatus.State.WORKING));
          }
        });
    boolean result = cut.isJobActive(jobStatus);
    assertTrue(result);
    context.assertIsSatisfied();
  }

  @Test
  void testIsJobActiveFailed() {
    var jobStatus = context.mock(IPSPublisherJobStatus.class);
    context.checking(
        new Expectations() {
          {
            one(jobStatus).getState();
            will(returnValue(IPSPublisherJobStatus.State.COMPLETED_W_FAILURE));
          }
        });
    boolean result = cut.isJobActive(jobStatus);
    assertFalse(result);
    context.assertIsSatisfied();
  }

  @Test
  void testGetStateDescriptionState() {
    String result = cut.getStateDescription(IPSPublisherJobStatus.State.ABORTED);
    assertEquals(PSSitePublishStatusService.STATE_FAILED, result);
    result = cut.getStateDescription(IPSPublisherJobStatus.State.COMPLETED);
    assertEquals(PSSitePublishStatusService.STATE_COMPLETE, result);
    result = cut.getStateDescription(IPSPublisherJobStatus.State.COMPLETED_W_FAILURE);
    assertEquals(PSSitePublishStatusService.STATE_COMPLETE_W_FAILURES, result);
    result = cut.getStateDescription(IPSPublisherJobStatus.State.WORKING);
    assertEquals(PSSitePublishStatusService.STATE_RUNNING, result);
    result = cut.getStateDescription(IPSPublisherJobStatus.State.CANCELLED);
    assertEquals(PSSitePublishStatusService.STATE_CANCELLED, result);
    result = cut.getStateDescription(IPSPublisherJobStatus.State.QUEUEING);
    assertEquals(PSSitePublishStatusService.STATE_PENDING, result);
  }

  @Test
  void testGetStateDescriptionEndingState() {
    String result = cut.getStateDescription(IPSPubStatus.EndingState.ABORTED);
    assertEquals(PSSitePublishStatusService.STATE_FAILED, result);
    result = cut.getStateDescription(IPSPubStatus.EndingState.COMPLETED);
    assertEquals(PSSitePublishStatusService.STATE_COMPLETE, result);
    result = cut.getStateDescription(IPSPubStatus.EndingState.COMPLETED_W_FAILURE);
    assertEquals(PSSitePublishStatusService.STATE_COMPLETE_W_FAILURES, result);
    result = cut.getStateDescription(IPSPubStatus.EndingState.CANCELED_BY_USER);
    assertEquals(PSSitePublishStatusService.STATE_CANCELLED, result);
  }

  @Test
  void testGetStateDescriptionStatus() {
    String result = cut.getStateDescription(IPSSiteItem.Status.FAILURE);
    assertEquals(PSSitePublishStatusService.STATE_FAILED, result);
    result = cut.getStateDescription(IPSSiteItem.Status.SUCCESS);
    assertEquals(PSSitePublishStatusService.STATE_COMPLETE, result);
    result = cut.getStateDescription(IPSSiteItem.Status.CANCELLED);
    assertEquals(PSSitePublishStatusService.STATE_CANCELLED, result);
  }

  @Test
  void testGetSiteNameLong() throws PSNotFoundException {
    var editionGuid = new PSGuid(PSTypeEnum.EDITION, 42L);
    var edition = context.mock(IPSEdition.class);
    var siteGuid = new PSGuid(PSTypeEnum.SITE, 301L);
    var site = context.mock(IPSSite.class);

    context.checking(
        new Expectations() {
          {
            one(guidMgr).makeGuid(42L, PSTypeEnum.EDITION);
            will(returnValue(editionGuid));
            one(pubSvc).loadEdition(editionGuid);
            will(returnValue(edition));
            one(edition).getSiteId();
            will(returnValue(siteGuid));
            one(siteMgr).loadSite(siteGuid);
            will(returnValue(site));
            one(site).getName();
            will(returnValue("site1"));
          }
        });

    String name = cut.getSiteName(42L);
    assertNotNull(name);
    assertEquals("site1", name);
    context.assertIsSatisfied();
  }
}
