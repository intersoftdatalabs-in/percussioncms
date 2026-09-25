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
package com.percussion.share.dao.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.IPSSiteManager;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Folder-list page rows must not call {@code loadSite} (#4874). */
class PSFolderHelperPagePreviewTest {

  @Test
  void repositoryLeafUsesSiteFolderRootWhenSiteNameDiffers() {
    IPSSiteManager sites = org.mockito.Mockito.mock(IPSSiteManager.class);
    IPSSite site = org.mockito.Mockito.mock(IPSSite.class);
    when(sites.findSite("EnterpriseInvestments")).thenReturn(null);
    when(site.getFolderRoot()).thenReturn("//Sites/EnterpriseInvestments");
    when(site.isMobilePreviewEnabled()).thenReturn(true);
    when(sites.findAllSites()).thenReturn(List.of(site));

    PSPathItem item = new PSPathItem();
    item.setType("percPage");
    item.setFolderPaths(List.of("//Sites/EnterpriseInvestments/Pages"));

    PSFolderHelper.applyPageMobilePreview(item, sites);

    assertTrue(item.isMobilePreviewEnabled());
    verify(sites, never()).loadSite(org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void longerFolderNameDoesNotStealMobilePreview() {
    IPSSiteManager sites = org.mockito.Mockito.mock(IPSSiteManager.class);
    IPSSite archive = org.mockito.Mockito.mock(IPSSite.class);
    IPSSite real = org.mockito.Mockito.mock(IPSSite.class);
    when(sites.findSite("EnterpriseInvestments")).thenReturn(null);
    when(archive.getFolderRoot()).thenReturn("//Sites/EnterpriseInvestmentsArchive");
    when(archive.isMobilePreviewEnabled()).thenReturn(false);
    when(real.getFolderRoot()).thenReturn("//Sites/EnterpriseInvestments/");
    when(real.isMobilePreviewEnabled()).thenReturn(true);
    when(sites.findAllSites()).thenReturn(List.of(archive, real));

    PSPathItem item = new PSPathItem();
    item.setType("percPage");
    item.setFolderPaths(List.of("//Sites/EnterpriseInvestments/Pages"));

    PSFolderHelper.applyPageMobilePreview(item, sites);

    assertTrue(item.isMobilePreviewEnabled());
    verify(sites, never()).loadSite(org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void missingSiteLeavesPreviewOff() {
    IPSSiteManager sites = org.mockito.Mockito.mock(IPSSiteManager.class);
    when(sites.findSite("EnterpriseInvestments")).thenReturn(null);
    when(sites.findAllSites()).thenReturn(List.of());

    PSPathItem item = new PSPathItem();
    item.setType("percPage");
    item.setFolderPaths(List.of("//Sites/EnterpriseInvestments/Pages"));

    PSFolderHelper.applyPageMobilePreview(item, sites);

    assertFalse(item.isMobilePreviewEnabled());
    verify(sites, never()).loadSite(org.mockito.ArgumentMatchers.anyString());
  }
}
