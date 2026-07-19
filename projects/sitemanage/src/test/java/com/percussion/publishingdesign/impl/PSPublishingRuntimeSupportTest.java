/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.publishingdesign.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.publishingdesign.data.PSDemandPublishRequest;
import com.percussion.publishingdesign.data.PSRuntimeEditionStatus;
import com.percussion.publishingdesign.data.PSRuntimeJobResponse;
import com.percussion.rx.publisher.IPSPublisherJobStatus;
import com.percussion.rx.publisher.IPSRxPublisherService;
import com.percussion.rx.publisher.data.PSDemandWork;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.publisher.IPSEdition;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.publishing.IPSPublishingWs;
import jakarta.ws.rs.WebApplicationException;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PSPublishingRuntimeSupportTest {

  @Mock private IPSPublisherService publisherService;
  @Mock private IPSGuidManager guidManager;
  @Mock private IPSRxPublisherService rxPublisherService;
  @Mock private IPSPublishingWs publishingWs;
  @Mock private IPSContentWs contentWs;
  @Mock private IPSGuid siteGuid;
  @Mock private IPSGuid editionGuid;

  private PSPublishingRuntimeSupport support;

  @BeforeEach
  void setUp() {
    support =
        new PSPublishingRuntimeSupport(
            publisherService, guidManager, rxPublisherService, publishingWs, contentWs);
  }

  @Test
  void listRuntimeEditions_includesJobId() {
    when(guidManager.makeGuid(eq("42"), eq(PSTypeEnum.SITE))).thenReturn(siteGuid);
    IPSEdition edition = mock(IPSEdition.class);
    when(edition.getGUID()).thenReturn(editionGuid);
    when(editionGuid.getUUID()).thenReturn(10);
    when(edition.getName()).thenReturn("Full");
    when(publisherService.findAllEditionsBySite(siteGuid))
        .thenReturn(Collections.singletonList(edition));
    when(rxPublisherService.getEditionJobId(editionGuid)).thenReturn(55L);

    List<PSRuntimeEditionStatus> list = support.listRuntimeEditions("42");
    assertEquals(1, list.size());
    assertEquals("Full", list.get(0).getName());
    assertEquals(55L, list.get(0).getRunningJobId());
  }

  @Test
  void startEdition_returnsJobId() {
    when(guidManager.makeGuid(eq("10"), eq(PSTypeEnum.EDITION))).thenReturn(editionGuid);
    when(rxPublisherService.startPublishingJob(eq(editionGuid), isNull())).thenReturn(77L);

    PSRuntimeJobResponse res = support.startEdition("10");
    assertEquals(77L, res.getJobId());
    assertEquals("10", res.getEditionId());
  }

  @Test
  void startEdition_conflictWhenAlreadyRunning() {
    when(guidManager.makeGuid(eq("10"), eq(PSTypeEnum.EDITION))).thenReturn(editionGuid);
    when(rxPublisherService.startPublishingJob(eq(editionGuid), isNull()))
        .thenThrow(new IllegalStateException("already running"));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> support.startEdition("10"));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  void stopJob_cancels() {
    PSRuntimeJobResponse res = support.stopJob("99");
    verify(rxPublisherService).cancelPublishingJob(99L);
    assertEquals("cancelled", res.getStatus());
  }

  @Test
  void getJobStatus_mapsFields() {
    IPSPublisherJobStatus st = mock(IPSPublisherJobStatus.class);
    when(rxPublisherService.getPublishingJobStatus(5L)).thenReturn(st);
    when(st.getJobId()).thenReturn(5L);
    when(st.getState()).thenReturn(IPSPublisherJobStatus.State.COMPLETED);
    when(st.countItemsDelivered()).thenReturn(3);
    when(st.countFailedItems()).thenReturn(1);

    PSRuntimeJobResponse res = support.getJobStatus("5");
    assertEquals(5L, res.getJobId());
    assertEquals(3L, res.getDelivered());
    assertEquals(1L, res.getFailed());
  }

  @Test
  void demandPublish_requiresContentIds() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> support.demandPublish("10", new PSDemandPublishRequest()));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void demandPublish_queuesWithFolder() {
    when(guidManager.makeGuid(eq("10"), eq(PSTypeEnum.EDITION))).thenReturn(editionGuid);
    when(editionGuid.getUUID()).thenReturn(10);
    IPSGuid contentGuid = mock(IPSGuid.class);
    IPSGuid folderGuid = mock(IPSGuid.class);
    when(guidManager.makeGuid(eq(101L), eq(PSTypeEnum.LEGACY_CONTENT))).thenReturn(contentGuid);
    when(guidManager.makeGuid(eq(200L), eq(PSTypeEnum.LEGACY_CONTENT))).thenReturn(folderGuid);
    when(publishingWs.queueDemandWork(eq(10), any(PSDemandWork.class))).thenReturn(888L);

    PSDemandPublishRequest req = new PSDemandPublishRequest();
    req.setContentIds(List.of("101"));
    req.setFolderIds(List.of("200"));

    PSRuntimeJobResponse res = support.demandPublish("10", req);
    assertEquals(888L, res.getRequestId());
    assertEquals("queued", res.getStatus());
  }

  @Test
  void clearSiteItems_delegates() {
    when(guidManager.makeGuid(eq("42"), eq(PSTypeEnum.SITE))).thenReturn(siteGuid);
    support.clearSiteItems("42");
    verify(publishingWs).deleteSiteItems(siteGuid);
  }

  @Test
  void purgeJobLog_delegates() {
    support.purgeJobLog("12");
    verify(publishingWs).purgeJobLog(12L);
  }

  @Test
  void missingRx_returns503() {
    PSPublishingRuntimeSupport noRx =
        new PSPublishingRuntimeSupport(publisherService, guidManager, null, publishingWs, contentWs);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> noRx.startEdition("1"));
    assertEquals(503, ex.getResponse().getStatus());
  }
}
