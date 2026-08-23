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
package com.percussion.pagemanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.data.PSTemplate;
import com.percussion.recent.service.rest.IPSRecentService;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.sitemanage.data.PSSiteSummary;
import com.percussion.utils.guid.IPSGuid;
import org.junit.jupiter.api.Test;

/**
 * Home Create Page with FastForward assembly templates must not mark the save TX rollback-only
 * (#3728).
 */
class PSPageServiceSaveRollbackHelpersTest {

  @Test
  void assemblyTemplateGuidIsDetectedForSaveStub() {
    IPSIdMapper mapper = mock(IPSIdMapper.class);
    IPSGuid guid = mock(IPSGuid.class);
    when(mapper.getGuid("0-4-1050")).thenReturn(guid);
    when(guid.getType()).thenReturn(PSTypeEnum.TEMPLATE.getOrdinal());
    assertTrue(PSPageService.isAssemblyTemplateGuid("0-4-1050", mapper));
    assertFalse(PSPageService.isRecentTemplateItemGuid("0-4-1050", mapper));
    PSTemplate stub = PSPageService.assemblyTemplateStub("0-4-1050");
    assertEquals("0-4-1050", stub.getId());
    assertEquals("0", stub.getContentMigrationVersion());
  }

  @Test
  void assemblyTemplateGuidIsNotRecentItem() {
    IPSIdMapper mapper = mock(IPSIdMapper.class);
    IPSGuid guid = mock(IPSGuid.class);
    when(mapper.getGuid("0-4-1050")).thenReturn(guid);
    when(guid.getType()).thenReturn(PSTypeEnum.TEMPLATE.getOrdinal());
    assertFalse(PSPageService.isRecentTemplateItemGuid("0-4-1050", mapper));
  }

  @Test
  void percTemplateContentGuidIsRecentItem() {
    IPSIdMapper mapper = mock(IPSIdMapper.class);
    IPSGuid guid = mock(IPSGuid.class);
    when(mapper.getGuid("1-101-705")).thenReturn(guid);
    when(guid.getType()).thenReturn(PSTypeEnum.LEGACY_CONTENT.getOrdinal());
    assertTrue(PSPageService.isRecentTemplateItemGuid("1-101-705", mapper));
  }

  @Test
  void blankOrBadGuidIsNotRecentItem() {
    IPSIdMapper mapper = mock(IPSIdMapper.class);
    when(mapper.getGuid("not-a-guid")).thenThrow(new IllegalArgumentException("bad"));
    assertFalse(PSPageService.isRecentTemplateItemGuid(null, mapper));
    assertFalse(PSPageService.isRecentTemplateItemGuid(" ", mapper));
    assertFalse(PSPageService.isRecentTemplateItemGuid("not-a-guid", mapper));
    assertFalse(PSPageService.isRecentTemplateItemGuid("1-101-705", null));
  }

  @Test
  void resolveRepositoryFolderMapsSitenameToFolderLeaf() {
    PSSiteSummary site = new PSSiteSummary();
    site.setName("Corporate_Investments");
    site.setFolderPath("//Sites/CorporateInvestments");
    assertEquals(
        "//Sites/CorporateInvestments",
        PSPageService.resolveRepositoryFolderPath(
            "//Sites/Corporate_Investments", site));
    assertEquals(
        "/Sites/CorporateInvestments/Home",
        PSPageService.resolveRepositoryFolderPath(
            "/Sites/Corporate_Investments/Home", site));
  }

  @Test
  void resolveRepositoryFolderLeavesMatchingPath() {
    PSSiteSummary site = new PSSiteSummary();
    site.setName("Corporate_Investments");
    site.setFolderPath("//Sites/CorporateInvestments");
    assertEquals(
        "//Sites/CorporateInvestments",
        PSPageService.resolveRepositoryFolderPath("//Sites/CorporateInvestments", site));
    assertEquals("/Sites/Demo", PSPageService.resolveRepositoryFolderPath("/Sites/Demo", null));
    assertEquals(
        "//Sites/CorporateInvestments",
        PSPageService.resolveRepositoryFolderPath(
            "//Sites/Corporate_Investments", null));
  }

  @Test
  void recordRecentSkipsAssemblyTemplateGuid() {
    IPSRecentService recent = mock(IPSRecentService.class);
    IPSIdMapper mapper = mock(IPSIdMapper.class);
    IPSGuid guid = mock(IPSGuid.class);
    when(mapper.getGuid("0-4-1050")).thenReturn(guid);
    when(guid.getType()).thenReturn(PSTypeEnum.TEMPLATE.getOrdinal());
    PSPage page = new PSPage();
    page.setId("1-101-900");
    page.setFolderPath("//Sites/CorporateInvestments");
    PSPageService.recordRecentAfterPageSave(recent, mapper, page, "0-4-1050", true);
    verify(recent).addRecentItem("1-101-900");
    verify(recent).addRecentSiteFolder("//Sites/CorporateInvestments");
    verify(recent, never()).addRecentTemplate(anyString(), anyString());
  }

  @Test
  void recordRecentSwallowsRecentServiceRuntimeException() {
    IPSRecentService recent = mock(IPSRecentService.class);
    doThrow(new IllegalArgumentException("Value must be a template guid"))
        .when(recent)
        .addRecentItem("1-101-900");
    PSPage page = new PSPage();
    page.setId("1-101-900");
    page.setFolderPath("//Sites/CorporateInvestments");
    PSPageService.recordRecentAfterPageSave(recent, mock(IPSIdMapper.class), page, "0-4-1050", true);
    verify(recent).addRecentItem("1-101-900");
  }
}
