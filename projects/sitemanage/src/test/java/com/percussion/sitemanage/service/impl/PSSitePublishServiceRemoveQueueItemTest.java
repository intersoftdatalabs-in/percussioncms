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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Removes one content id from a site incremental queue (#4788). */
class PSSitePublishServiceRemoveQueueItemTest {

  private IPSPublishingWs pubWs;
  private IPSIdMapper idMapper;
  private IPSPubServerService pubServerService;
  private IPSContentChangeService contentChangeService;
  private PSSitePublishService service;

  @BeforeEach
  void setUp() {
    pubWs = mock(IPSPublishingWs.class);
    idMapper = mock(IPSIdMapper.class);
    pubServerService = mock(IPSPubServerService.class);
    contentChangeService = mock(IPSContentChangeService.class);
    service =
        new PSSitePublishService(
            pubWs,
            idMapper,
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
  void removesQueuedContentIdForProductionServer() throws Exception {
    stubSiteAndServer("PRODUCTION", List.of(301, 88));
    when(idMapper.getContentId("301")).thenReturn(301);

    service.removeQueuedIncrementalContent("MySite", "FTP-Prod", "301");

    verify(contentChangeService)
        .deleteChangeEvents(9L, 301, PSContentChangeType.PENDING_LIVE);
  }

  @Test
  void usesStagingQueueForStagingServer() throws Exception {
    stubSiteAndServer(PSPubServer.STAGING, List.of(12));
    when(idMapper.getContentId("12")).thenReturn(12);

    service.removeQueuedIncrementalContent("MySite", "Stage", "12");

    verify(contentChangeService)
        .deleteChangeEvents(9L, 12, PSContentChangeType.PENDING_STAGED);
  }

  @Test
  void missingQueueItemIs404AndDoesNotDelete() throws Exception {
    stubSiteAndServer("PRODUCTION", List.of(88));
    when(idMapper.getContentId("301")).thenReturn(301);

    PSIncrementalQueueStatusException ex =
        assertThrows(
            PSIncrementalQueueStatusException.class,
            () -> service.removeQueuedIncrementalContent("MySite", "FTP-Prod", "301"));

    assertEquals(404, ex.status());
    verify(contentChangeService, never())
        .deleteChangeEvents(9L, 301, PSContentChangeType.PENDING_LIVE);
  }

  @Test
  void unknownContentIdIs404() {
    when(idMapper.getContentId("nope")).thenThrow(new IllegalArgumentException("bad id"));

    PSIncrementalQueueStatusException ex =
        assertThrows(
            PSIncrementalQueueStatusException.class,
            () -> service.removeQueuedIncrementalContent("MySite", "FTP-Prod", "nope"));

    assertEquals(404, ex.status());
  }

  private void stubSiteAndServer(String serverType, List<Integer> queued) throws Exception {
    IPSSite site = mock(IPSSite.class);
    when(site.getSiteId()).thenReturn(9L);
    when(pubWs.findSite("MySite")).thenReturn(site);
    PSPublishServerInfo info = new PSPublishServerInfo();
    info.setServerName(serverType.equals(PSPubServer.STAGING) ? "Stage" : "FTP-Prod");
    info.setServerType(serverType);
    when(pubServerService.getPubServerList("9")).thenReturn(List.of(info));
    PSContentChangeType changeType =
        PSPubServer.STAGING.equalsIgnoreCase(serverType)
            ? PSContentChangeType.PENDING_STAGED
            : PSContentChangeType.PENDING_LIVE;
    when(contentChangeService.getChangedContent(9L, changeType)).thenReturn(queued);
  }
}
