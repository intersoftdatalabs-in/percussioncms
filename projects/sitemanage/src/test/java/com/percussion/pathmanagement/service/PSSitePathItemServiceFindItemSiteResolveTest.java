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
package com.percussion.pathmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.pathmanagement.service.IPSPathService.PSPathNotFoundServiceException;
import com.percussion.pathmanagement.service.impl.PSSitePathItemService;
import com.percussion.share.service.IPSDataService.DataServiceLoadException;
import com.percussion.share.service.IPSDataService.DataServiceNotFoundException;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.sitemanage.data.PSSiteSummary;
import com.percussion.sitemanage.service.IPSSiteDataService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * #3558: {@code GET /path/item/{path}} must resolve FOLDER_ROOT slugs (and missing
 * typed paths such as {@code /Demo/Home}) without {@code siteDataService.find(id)},
 * which marks the listing transaction rollback-only.
 */
class PSSitePathItemServiceFindItemSiteResolveTest {

  @Mock private IPSSiteDataService siteDataService;

  private TestableSitePathItemService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    service = new TestableSitePathItemService(siteDataService);
  }

  @Test
  void folderRootSlugResolvesViaFindAllWithoutFindById() throws Exception {
    when(siteDataService.findByPath(anyString()))
        .thenThrow(new DataServiceNotFoundException("folder-root slug is not SITENAME"));
    when(siteDataService.findAll()).thenReturn(List.of(corporateInvestmentsSite()));

    var item = service.exposeFindItem("/CorporateInvestments");

    assertEquals("Corporate_Investments", item.getId());
    assertEquals("Corporate_Investments", item.getName());
    assertEquals("/CorporateInvestments/", item.getPath());
    verify(siteDataService, never()).find(anyString());
  }

  @Test
  void sitenamePathUsesFindByPathAndNeverFindById() throws Exception {
    when(siteDataService.findByPath(anyString())).thenReturn(corporateInvestmentsSite());

    var item = service.exposeFindItem("/Corporate_Investments");

    assertEquals("Corporate_Investments", item.getId());
    assertEquals("/Corporate_Investments/", item.getPath());
    verify(siteDataService, never()).find(anyString());
    verify(siteDataService, never()).findAll();
  }

  @Test
  void missingDemoHomeIsNotFoundWithoutFindById() throws Exception {
    when(siteDataService.findByPath(anyString()))
        .thenThrow(new DataServiceNotFoundException("Site cannot be found"));
    when(siteDataService.findAll()).thenReturn(List.of(corporateInvestmentsSite()));

    assertThrows(
        PSPathNotFoundServiceException.class, () -> service.exposeFindItem("/Demo/Home/"));
    verify(siteDataService, never()).find(anyString());
  }

  @Test
  void folderPathLeafUsesLastCmsSegment() {
    assertEquals(
        "CorporateInvestments",
        PSSitePathItemService.folderPathLeaf("//Sites/CorporateInvestments/"));
    assertEquals("Home", PSSitePathItemService.folderPathLeaf("/Sites/Demo/Home"));
    assertEquals("", PSSitePathItemService.folderPathLeaf(""));
  }

  private static PSSiteSummary corporateInvestmentsSite() {
    var site = new PSSiteSummary();
    site.setId("Corporate_Investments");
    site.setName("Corporate_Investments");
    site.setFolderPath("//Sites/CorporateInvestments");
    site.setType("site");
    return site;
  }

  static final class TestableSitePathItemService extends PSSitePathItemService {
    TestableSitePathItemService(IPSSiteDataService siteDataService) {
      super(siteDataService, null, null, null, null, null, null, null, null, null, null, null);
    }

    PSPathItem exposeFindItem(String path)
        throws PSPathNotFoundServiceException,
            DataServiceNotFoundException,
            PSValidationException,
            DataServiceLoadException {
      return findItem(path);
    }
  }
}
