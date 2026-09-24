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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.itemmanagement.service.IPSItemService;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.itemmanagement.service.IPSWorkflowHelper;
import com.percussion.licensemanagement.service.IPSLicenseService;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pubserver.IPSPubServerService;
import com.percussion.pubserver.data.PSPublishServerInfo;
import com.percussion.services.contentchange.IPSContentChangeService;
import com.percussion.services.contentchange.data.PSContentChangeType;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.services.pubserver.data.PSPubServer;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.share.service.IPSDataItemSummaryService;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.sitemanage.dao.IPSSitePublishDao;
import com.percussion.sitemanage.dao.IPSiteDao;
import com.percussion.sitemanage.service.IPSSitePublishServiceHelper;
import com.percussion.ui.service.IPSListViewHelper;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.publishing.IPSPublishingWs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Clears every incremental-queue event for a site and server (#4836). */
class PSSitePublishServiceClearQueueTest {

  private IPSPublishingWs pubWs;
  private IPSPubServerService pubServerService;
  private IPSContentChangeService contentChangeService;
  private PSSitePublishService service;

  @BeforeEach
  void setUp() {
    pubWs = mock(IPSPublishingWs.class);
    pubServerService = mock(IPSPubServerService.class);
    contentChangeService = mock(IPSContentChangeService.class);
    service =
        new PSSitePublishService(
            pubWs,
            mock(IPSIdMapper.class),
            mock(IPSContentWs.class),
            mock(IPSWidgetAssetRelationshipService.class),
            mock(IPSItemWorkflowService.class),
            mock(IPSDataItemSummaryService.class),
            mock(IPSiteDao.class),
            pubServerService,
            mock(IPSLicenseService.class),
            mock(IPSItemService.class),
            mock(IPSCmsObjectMgr.class),
            contentChangeService,
            mock(IPSListViewHelper.class),
            mock(IPSWorkflowHelper.class),
            mock(IPSPageService.class),
            mock(IPSSitePublishDao.class),
            mock(IPSSitePublishServiceHelper.class));
  }

  @Test
  void clearsLiveQueueForProductionServer() throws Exception {
    stubSiteAndServer("PRODUCTION");

    service.clearQueuedIncrementalContent("MySite", "FTP-Prod");

    verify(contentChangeService)
        .deleteChangeEventsForSite(9L, PSContentChangeType.PENDING_LIVE);
    verify(contentChangeService, never())
        .deleteChangeEventsForSite(9L, PSContentChangeType.PENDING_STAGED);
  }

  @Test
  void clearsStagingQueueForStagingServer() throws Exception {
    stubSiteAndServer(PSPubServer.STAGING);

    service.clearQueuedIncrementalContent("MySite", "Stage");

    verify(contentChangeService)
        .deleteChangeEventsForSite(9L, PSContentChangeType.PENDING_STAGED);
  }

  @Test
  void missingSiteIs404AndDoesNotDelete() {
    when(pubWs.findSite("MySite")).thenReturn(null);

    PSIncrementalQueueStatusException ex =
        assertThrows(
            PSIncrementalQueueStatusException.class,
            () -> service.clearQueuedIncrementalContent("MySite", "FTP-Prod"));

    assertEquals(404, ex.status());
    verify(contentChangeService, never())
        .deleteChangeEventsForSite(9L, PSContentChangeType.PENDING_LIVE);
  }

  private void stubSiteAndServer(String serverType) throws Exception {
    IPSSite site = mock(IPSSite.class);
    when(site.getSiteId()).thenReturn(9L);
    when(pubWs.findSite("MySite")).thenReturn(site);
    PSPublishServerInfo info = new PSPublishServerInfo();
    info.setServerName(serverType.equals(PSPubServer.STAGING) ? "Stage" : "FTP-Prod");
    info.setServerType(serverType);
    when(pubServerService.getPubServerList("9")).thenReturn(java.util.List.of(info));
  }
}
