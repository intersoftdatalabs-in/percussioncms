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
package com.percussion.activity.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.percussion.activity.data.PSContentTraffic;
import com.percussion.activity.data.PSContentTrafficRequest;
import com.percussion.activity.service.IPSActivityService;
import com.percussion.activity.service.IPSTrafficService.PSTrafficServiceException;
import com.percussion.analytics.service.IPSAnalyticsProviderQueryService;
import com.percussion.analytics.service.IPSAnalyticsProviderService;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pathmanagement.service.IPSPathService;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.service.IPSDataService.DataServiceNotFoundException;
import com.percussion.sitemanage.data.PSSiteSummary;
import com.percussion.sitemanage.service.IPSSiteDataService;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for content traffic empty-state behavior used by Home gadgets (unknown path / empty
 * install must not become HTTP 500).
 */
@ExtendWith(MockitoExtension.class)
class PSTrafficServiceTest {

  @Mock private IPSActivityService activityService;
  @Mock private IPSAnalyticsProviderQueryService analyticsService;
  @Mock private IPSAnalyticsProviderService providerService;
  @Mock private IPSSiteDataService siteDataService;
  @Mock private IPSPathService pathService;
  @Mock private IPSFolderHelper folderHelper;
  @Mock private IPSPageService pageService;

  private PSTrafficService trafficService;

  @BeforeEach
  void setUp() {
    trafficService =
        new PSTrafficService(
            activityService,
            analyticsService,
            providerService,
            siteDataService,
            pathService,
            folderHelper,
            pageService);
  }

  @Test
  void getContentTraffic_unknownPath_returnsEmptySeries() throws Exception {
    when(siteDataService.findByPath(anyString()))
        .thenThrow(new DataServiceNotFoundException("Site cannot be found for path: /Sites/Nope"));

    PSContentTrafficRequest request = sampleRequest("/Sites/Nope");
    PSContentTraffic traffic = trafficService.getContentTraffic(request);

    assertNotNull(traffic);
    assertTrue(traffic.getDates().isPresent());
    assertTrue(traffic.getDates().get().isEmpty());
    assertTrue(traffic.getVisits().isPresent());
    assertTrue(traffic.getVisits().get().isEmpty());
    assertTrue(traffic.getNewPages().isPresent());
    assertTrue(traffic.getNewPages().get().isEmpty());
    assertTrue(traffic.getPageUpdates().isPresent());
    assertTrue(traffic.getPageUpdates().get().isEmpty());
    assertTrue(traffic.getTakeDowns().isPresent());
    assertTrue(traffic.getTakeDowns().get().isEmpty());
    assertTrue(traffic.getLivePages().isPresent());
    assertTrue(traffic.getLivePages().get().isEmpty());
    assertTrue(traffic.getUpdateTotals().isPresent());
    assertTrue(traffic.getUpdateTotals().get().isEmpty());
    assertEquals("01/01/2024", traffic.getStartDate().orElse(null));
    assertEquals("01/07/2024", traffic.getEndDate().orElse(null));
  }

  @Test
  void getContentTraffic_malformedDate_throwsTrafficServiceException() throws Exception {
    PSSiteSummary site = new PSSiteSummary();
    site.setName("Demo");
    site.setId("//Sites/Demo");
    when(siteDataService.findByPath(anyString())).thenReturn(site);

    PSContentTrafficRequest request = sampleRequest("/Sites/Demo");
    request.setStartDate("not-a-date");
    request.setEndDate("also-bad");

    assertThrows(PSTrafficServiceException.class, () -> trafficService.getContentTraffic(request));
  }

  @Test
  void getContentTraffic_knownPath_returnsSeriesForRequestedTraffic() throws Exception {
    PSSiteSummary site = new PSSiteSummary();
    site.setName("Demo");
    site.setId("//Sites/Demo");
    when(siteDataService.findByPath("/Sites/Demo")).thenReturn(site);
    when(activityService.findPageIdsByPath("/Sites/Demo")).thenReturn(Collections.emptyList());
    // dateList is breakdown + end day; 7 calendar days -> 8 buckets before removeLast
    List<Integer> zeros = List.of(0, 0, 0, 0, 0, 0, 0, 0);
    when(activityService.findPublishedItems(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(zeros);
    when(activityService.findNewContentActivities(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(zeros);
    when(activityService.findNumberContentActivities(
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.anyString(),
            ArgumentMatchers.any()))
        .thenReturn(zeros);

    PSContentTrafficRequest request = sampleRequest("/Sites/Demo");
    request.setTrafficRequested(List.of("LIVE_PAGES", "NEW_PAGES", "UPDATED_PAGES", "TAKE_DOWNS"));

    PSContentTraffic traffic = trafficService.getContentTraffic(request);

    assertNotNull(traffic);
    assertTrue(traffic.getSite().isPresent());
    assertTrue(traffic.getDates().isPresent());
    assertEquals(7, traffic.getDates().get().size());
    assertTrue(traffic.getNewPages().isPresent());
    assertEquals(7, traffic.getNewPages().get().size());
    assertTrue(traffic.getLivePages().isPresent());
    assertEquals(7, traffic.getLivePages().get().size());
  }

  private static PSContentTrafficRequest sampleRequest(String path) {
    PSContentTrafficRequest request = new PSContentTrafficRequest();
    request.setPath(path);
    request.setStartDate("01/01/2024");
    request.setEndDate("01/07/2024");
    request.setGranularity("DAY");
    request.setTrafficRequested(List.of("NEW_PAGES"));
    return request;
  }
}
