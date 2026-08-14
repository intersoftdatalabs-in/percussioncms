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
package com.percussion.sitemanage.dao.impl;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.fastforward.managednav.IPSManagedNavService;
import com.percussion.fastforward.managednav.IPSNavigationErrors;
import com.percussion.fastforward.managednav.PSNavException;
import com.percussion.pagemanagement.assembler.IPSRenderAssemblyBridge;
import com.percussion.pagemanagement.dao.IPSPageDao;
import com.percussion.pagemanagement.dao.IPSPageDaoHelper;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.pathmanagement.data.PSFolderPermission;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.share.dao.IPSContentItemDao;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.sitemanage.data.PSSite;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.content.IPSContentWs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Traditional sites can skip NavTree seed when managedNavigation is false. */
class PSSiteContentDaoManagedNavTest {

  @Mock private IPSAssemblyService assemblyService;
  @Mock private IPSContentItemDao contentItemDao;
  @Mock private IPSFolderHelper folderHelper;
  @Mock private IPSIdMapper idMapper;
  @Mock private IPSPageDao pageDao;
  @Mock private IPSPageDaoHelper pageDaoHelper;
  @Mock private IPSTemplateService templateService;
  @Mock private IPSManagedNavService navService;
  @Mock private IPSRenderAssemblyBridge asmBridge;
  @Mock private IPSContentDesignWs contentDesignWs;
  @Mock private IPSContentWs contentWs;

  private PSSiteContentDao dao;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    dao =
        new PSSiteContentDao(
            assemblyService,
            contentItemDao,
            folderHelper,
            idMapper,
            pageDao,
            pageDaoHelper,
            templateService,
            navService,
            asmBridge,
            contentDesignWs,
            contentWs);
  }

  @Test
  void skipsNavTreeWhenManagedNavigationFalse() throws Exception {
    PSSite site = traditionalSite();
    site.setManagedNavigation(Boolean.FALSE);

    dao.createRelatedItems(site);

    verify(folderHelper)
        .createFolder("//Sites/Bare", PSFolderPermission.Access.WRITE);
    verify(navService, never()).addNavTreeToFolder(anyString(), anyString(), anyString(), anyInt());
    verify(navService, never()).findNavSummary(anyString());
  }

  @Test
  void createsNavTreeWhenFlagOmitted() throws Exception {
    PSSite site = traditionalSite();
    when(navService.findNavSummary("//Sites/Bare")).thenReturn(null);
    when(navService.addNavTreeToFolder(anyString(), anyString(), anyString(), anyInt()))
        .thenThrow(new RuntimeException("stop after nav create"));
    when(pageDaoHelper.getWorkflowIdForPath(anyString())).thenReturn(1);

    try {
      dao.createRelatedItems(site);
    } catch (RuntimeException e) {
      // expected once nav seed is attempted (homepage path not stubbed)
    }

    verify(navService)
        .addNavTreeToFolder("//Sites/Bare", "Bare-NavTree", "Home", 1);
  }

  @Test
  void rethrowsInvalidNavTreeCreateWithoutWrapping() throws Exception {
    PSSite site = traditionalSite();
    PSNavException already =
        new PSNavException(
            IPSNavigationErrors.NAVIGATION_SERVICE_NAVTREE_CANNOT_BE_ADDED_TO_FOLDER_WITH_NAVTREE);
    when(navService.findNavSummary("//Sites/Bare")).thenReturn(null);
    when(pageDaoHelper.getWorkflowIdForPath(anyString())).thenReturn(1);
    when(navService.addNavTreeToFolder(anyString(), anyString(), anyString(), anyInt()))
        .thenThrow(already);

    PSNavException thrown =
        assertThrows(PSNavException.class, () -> dao.createRelatedItems(site));
    assertSame(already, thrown);
  }

  private static PSSite traditionalSite() {
    PSSite site = new PSSite();
    site.setName("Bare");
    site.setFolderPath("//Sites/Bare");
    site.setNavigationTitle("Home");
    site.setHomePageTitle("Home");
    site.setLabel("Bare");
    site.setBaseTemplateName("perc.base.plain");
    site.setTemplateName("BareTemplate");
    return site;
  }
}
