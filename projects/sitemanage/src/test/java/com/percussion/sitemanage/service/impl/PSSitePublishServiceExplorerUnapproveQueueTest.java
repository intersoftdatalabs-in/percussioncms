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
import static org.mockito.ArgumentMatchers.anyLong;
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
import com.percussion.services.contentchange.IPSContentChangeService;
import com.percussion.services.contentchange.data.PSContentChangeType;
import com.percussion.services.legacy.IPSCmsObjectMgr;
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
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unapproves one Explorer item that is already on its site incremental queue (#5057). */
class PSSitePublishServiceExplorerUnapproveQueueTest {

  private IPSPublishingWs pubWs;
  private IPSIdMapper idMapper;
  private IPSContentChangeService contentChangeService;
  private IPSItemWorkflowService itemWorkflowService;
  private PSSitePublishService service;

  @BeforeEach
  void setUp() {
    pubWs = mock(IPSPublishingWs.class);
    idMapper = mock(IPSIdMapper.class);
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
            mock(IPSPubServerService.class),
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
  void rejectsQueuedItemAndLeavesItOnTheQueue() throws Exception {
    stubItem("301", 301, 44L);
    when(contentChangeService.getChangedContent(44L, PSContentChangeType.PENDING_LIVE))
        .thenReturn(List.of(301));

    service.unapproveExplorerItemOnIncrementalQueue("301");

    verify(itemWorkflowService)
        .transition("301-guid", IPSItemWorkflowService.TRANSITION_TRIGGER_REJECT);
    verify(contentChangeService, never())
        .deleteChangeEventsForSite(anyLong(), any(PSContentChangeType.class));
  }

  @Test
  void rejectsItemThatIsNotQueued() throws Exception {
    stubItem("301", 301, 44L);
    when(contentChangeService.getChangedContent(44L, PSContentChangeType.PENDING_LIVE))
        .thenReturn(List.of());

    PSIncrementalQueueStatusException ex =
        assertThrows(
            PSIncrementalQueueStatusException.class,
            () -> service.unapproveExplorerItemOnIncrementalQueue("301"));

    assertEquals(400, ex.status());
    verify(itemWorkflowService, never()).transition(any(), any());
  }

  @Test
  void rejectsInvalidContentIdWithoutUnapproving() {
    when(idMapper.getContentId("nope")).thenThrow(new IllegalArgumentException("bad"));

    PSIncrementalQueueStatusException ex =
        assertThrows(
            PSIncrementalQueueStatusException.class,
            () -> service.unapproveExplorerItemOnIncrementalQueue("nope"));

    assertEquals(400, ex.status());
    verify(itemWorkflowService, never()).transition(any(), any());
  }

  @Test
  void mapsForbiddenTransition() throws Exception {
    stubItem("301", 301, 44L);
    when(contentChangeService.getChangedContent(44L, PSContentChangeType.PENDING_LIVE))
        .thenReturn(List.of(301));
    when(itemWorkflowService.transition("301-guid", IPSItemWorkflowService.TRANSITION_TRIGGER_REJECT))
        .thenThrow(new WebApplicationException(Response.status(403).build()));

    PSIncrementalQueueStatusException ex =
        assertThrows(
            PSIncrementalQueueStatusException.class,
            () -> service.unapproveExplorerItemOnIncrementalQueue("301"));

    assertEquals(403, ex.status());
  }

  @Test
  void mapsConflictTransition() throws Exception {
    stubItem("301", 301, 44L);
    when(contentChangeService.getChangedContent(44L, PSContentChangeType.PENDING_LIVE))
        .thenReturn(List.of(301));
    when(itemWorkflowService.transition("301-guid", IPSItemWorkflowService.TRANSITION_TRIGGER_REJECT))
        .thenThrow(new WebApplicationException(Response.status(409).build()));

    PSIncrementalQueueStatusException ex =
        assertThrows(
            PSIncrementalQueueStatusException.class,
            () -> service.unapproveExplorerItemOnIncrementalQueue("301"));

    assertEquals(409, ex.status());
  }

  private void stubItem(String rawId, int contentId, long siteId) {
    when(idMapper.getContentId(rawId)).thenReturn(contentId);
    IPSGuid guid = mock(IPSGuid.class);
    when(guid.toString()).thenReturn(contentId + "-guid");
    when(idMapper.getGuid(any(PSLocator.class))).thenReturn(guid);
    IPSSite site = mock(IPSSite.class);
    when(site.getSiteId()).thenReturn(siteId);
    when(pubWs.getItemSites(guid)).thenReturn(List.of(site));
  }
}
