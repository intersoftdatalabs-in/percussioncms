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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.share.data.PSDataItemSummary;
import com.percussion.share.service.IPSDataItemSummaryService;
import com.percussion.sitemanage.dao.IPSiteDao;
import com.percussion.sitemanage.data.PSPublishingAction;
import com.percussion.sitemanage.service.IPSSitePublishService;
import java.util.Collections;
import java.util.List;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

/**
 * Covers {@code PSSitePublishService.getPublishingActions} (issue #4581):
 * unknown item ids must surface {@code PSNotFoundException} (HTTP 404 via the
 * web adapter) instead of an NPE on a {@code null} item summary.
 */
class PSSitePublishServiceGetPublishingActionsTest {

  private PSSitePublishService serviceWith(
      IPSDataItemSummaryService summaries, IPSiteDao siteDao) {
    return new PSSitePublishService(
        null,
        null,
        null,
        null,
        mock(IPSItemWorkflowService.class),
        summaries,
        siteDao,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  @Test
  void unknownIdThrowsNotFoundInsteadOfNpe() throws Exception {
    IPSDataItemSummaryService summaries = mock(IPSDataItemSummaryService.class);
    when(summaries.find(anyString())).thenReturn(null);
    PSSitePublishService service =
        serviceWith(summaries, mock(IPSiteDao.class));
    assertThrows(
        PSNotFoundException.class, () -> service.getPublishingActions("nope"));
  }

  @Test
  void resourceWithoutTriggersReturnsThreeDisabledActions() throws Exception {
    IPSDataItemSummaryService summaries = mock(IPSDataItemSummaryService.class);
    PSDataItemSummary sum = mock(PSDataItemSummary.class);
    when(sum.isPage()).thenReturn(false);
    when(sum.isResource()).thenReturn(true);
    when(summaries.find(anyString())).thenReturn(sum);
    IPSiteDao siteDao = mock(IPSiteDao.class);
    when(siteDao.findAllSummaries()).thenReturn(Collections.emptyList());

    List<PSPublishingAction> actions =
        serviceWith(summaries, siteDao).getPublishingActions("42");

    assertEquals(3, actions.size());
    assertEquals(PSPublishingAction.PUBLISHING_ACTION_PUBLISH, actions.get(0).getName());
    assertEquals(PSPublishingAction.PUBLISHING_ACTION_SCHEDULE, actions.get(1).getName());
    assertEquals(PSPublishingAction.PUBLISHING_ACTION_TAKEDOWN, actions.get(2).getName());
    assertTrue(actions.stream().noneMatch(PSPublishingAction::isEnabled));
  }

  @Test
  void adapterReturns404ForUnknownId() throws Exception {
    IPSSitePublishService publishService = mock(IPSSitePublishService.class);
    when(publishService.getPublishingActions(anyString()))
        .thenThrow(new PSNotFoundException("Item not found for id: nope"));
    PSSitePublishServiceWebAdapter adapter =
        new PSSitePublishServiceWebAdapter(publishService);
    Response response = adapter.getPublishingActions("nope");
    assertEquals(404, response.getStatus());
    assertEquals("Item not found", response.getEntity());
  }

  @Test
  void adapter404EntityDoesNotEchoPathParam() throws Exception {
    IPSSitePublishService publishService = mock(IPSSitePublishService.class);
    when(publishService.getPublishingActions(anyString()))
        .thenThrow(new PSNotFoundException("missing"));
    PSSitePublishServiceWebAdapter adapter =
        new PSSitePublishServiceWebAdapter(publishService);
    String injected = "<script>alert(1)</script>";
    Response response = adapter.getPublishingActions(injected);
    assertEquals(404, response.getStatus());
    Object entity = response.getEntity();
    assertTrue(entity instanceof String);
    assertFalse(((String) entity).contains(injected));
    assertFalse(((String) entity).contains("<script>"));
  }

  @Test
  void adapterReturnsActionListForKnownId() throws Exception {
    IPSSitePublishService publishService = mock(IPSSitePublishService.class);
    when(publishService.getPublishingActions(anyString()))
        .thenReturn(
            List.of(new PSPublishingAction(PSPublishingAction.PUBLISHING_ACTION_PUBLISH, true)));
    PSSitePublishServiceWebAdapter adapter =
        new PSSitePublishServiceWebAdapter(publishService);
    Response response = adapter.getPublishingActions("42");
    assertEquals(200, response.getStatus());
    assertTrue(response.getEntity() instanceof List);
  }
}
