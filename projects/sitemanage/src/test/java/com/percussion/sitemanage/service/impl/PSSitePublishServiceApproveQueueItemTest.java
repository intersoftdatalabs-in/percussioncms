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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.itemmanagement.service.IPSItemService;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.itemmanagement.service.IPSWorkflowHelper;
import com.percussion.licensemanagement.service.IPSLicenseService;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pubserver.IPSPubServerService;
import com.percussion.pubserver.data.PSPublishServerInfo;
import com.percussion.services.contentchange.IPSContentChangeService;
import com.percussion.services.contentchange.data.PSContentChangeType;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.services.pubserver.data.PSPubServer;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.share.service.IPSDataItemSummaryService;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.sitemanage.dao.IPSSitePublishDao;
import com.percussion.sitemanage.dao.IPSiteDao;
import com.percussion.sitemanage.service.IPSSitePublishServiceHelper;
import com.percussion.ui.service.IPSListViewHelper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.publishing.IPSPublishingWs;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Approves one content id that is already on a site incremental queue (#4912). */
class PSSitePublishServiceApproveQueueItemTest {

  private IPSPublishingWs pubWs;
  private IPSIdMapper idMapper;
  private IPSPubServerService pubServerService;
  private IPSContentChangeService contentChangeService;
  private IPSItemWorkflowService itemWorkflowService;
  private PSSitePublishService service;

  @BeforeEach
  void setUp() {
    pubWs = mock(IPSPublishingWs.class);
    idMapper = mock(IPSIdMapper.class);
    pubServerService = mock(IPSPubServerService.class);
    contentChangeService = mock(IPSContentChangeService.class);
    itemWorkflowService = mock(IPSItemWorkflowService.class);
    service =
        new PSSitePublishService(
            pubWs,
            idMapper,
            mock(IPSContentWs.class),
            mock(IPSWidgetAssetRelationshipService.class),
            itemWorkflowService,
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
  void approvesQueuedContentIdAndLeavesTheQueue() throws Exception {
    stubSiteAndServer("PRODUCTION", List.of(301, 88));
    when(idMapper.getContentId("301")).thenReturn(301);
    IPSGuid guid = mock(IPSGuid.class);
    when(guid.toString()).thenReturn("301-guid");
    when(idMapper.getGuid(any(PSLocator.class))).thenReturn(guid);

    service.approveQueuedIncrementalContent("MySite", "FTP-Prod", "301");

    verify(itemWorkflowService).performApproveTransition("301-guid", false, null);
    verify(contentChangeService, never())
        .deleteChangeEvents(eq(9L), eq(301), eq(PSContentChangeType.PENDING_LIVE));
  }

  @Test
  void stagingQueueStillApprovesTheQueuedId() throws Exception {
    stubSiteAndServer(PSPubServer.STAGING, List.of(12));
    when(idMapper.getContentId("12")).thenReturn(12);
    IPSGuid guid = mock(IPSGuid.class);
    when(guid.toString()).thenReturn("12-guid");
    when(idMapper.getGuid(any(PSLocator.class))).thenReturn(guid);

    service.approveQueuedIncrementalContent("MySite", "Stage", "12");

    verify(contentChangeService)
        .getChangedContent(9L, PSContentChangeType.PENDING_STAGED);
    verify(itemWorkflowService).performApproveTransition("12-guid", false, null);
  }

  @Test
  void missingQueueItemIs404AndDoesNotApprove() throws Exception {
    stubSiteAndServer("PRODUCTION", List.of(88));
    when(idMapper.getContentId("301")).thenReturn(301);

    PSIncrementalQueueStatusException ex =
        assertThrows(
            PSIncrementalQueueStatusException.class,
            () -> service.approveQueuedIncrementalContent("MySite", "FTP-Prod", "301"));

    assertEquals(404, ex.status());
    verify(itemWorkflowService, never()).performApproveTransition(any(), eq(false), any());
  }

  @Test
  void malformedContentIdIs400() {
    when(idMapper.getContentId("nope")).thenThrow(new IllegalArgumentException("bad id"));

    PSIncrementalQueueStatusException ex =
        assertThrows(
            PSIncrementalQueueStatusException.class,
            () -> service.approveQueuedIncrementalContent("MySite", "FTP-Prod", "nope"));

    assertEquals(400, ex.status());
  }

  @Test
  void workflowRejectionIs400() throws Exception {
    stubSiteAndServer("PRODUCTION", List.of(301));
    when(idMapper.getContentId("301")).thenReturn(301);
    IPSGuid guid = mock(IPSGuid.class);
    when(guid.toString()).thenReturn("301-guid");
    when(idMapper.getGuid(any(PSLocator.class))).thenReturn(guid);
    when(itemWorkflowService.performApproveTransition("301-guid", false, null))
        .thenThrow(new IPSItemWorkflowService.PSItemWorkflowServiceException("rejected"));

    PSIncrementalQueueStatusException ex =
        assertThrows(
            PSIncrementalQueueStatusException.class,
            () -> service.approveQueuedIncrementalContent("MySite", "FTP-Prod", "301"));

    assertEquals(400, ex.status());
  }

  @Test
  void missingItemDuringTransitionIs404() throws Exception {
    stubSiteAndServer("PRODUCTION", List.of(301));
    when(idMapper.getContentId("301")).thenReturn(301);
    IPSGuid guid = mock(IPSGuid.class);
    when(guid.toString()).thenReturn("301-guid");
    when(idMapper.getGuid(any(PSLocator.class))).thenReturn(guid);
    when(itemWorkflowService.performApproveTransition("301-guid", false, null))
        .thenThrow(new PSNotFoundException("missing"));

    PSIncrementalQueueStatusException ex =
        assertThrows(
            PSIncrementalQueueStatusException.class,
            () -> service.approveQueuedIncrementalContent("MySite", "FTP-Prod", "301"));

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
