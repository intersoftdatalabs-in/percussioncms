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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.percussion.fastforward.managednav.IPSManagedNavService;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.share.data.PSDataItemSummary;
import com.percussion.share.service.IPSDataItemSummaryService;
import com.percussion.sitemanage.data.PSSiteArchitecture;
import com.percussion.sitemanage.data.PSSiteSection;
import com.percussion.webservices.publishing.IPSPublishingWs;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** percNavTree and rffNavTree are the same Managed Nav tree role (#3357). */
class PSSiteArchitectureDaoNavTreeAliasTest {

  @Mock private IPSDataItemSummaryService summaries;
  @Mock private IPSPublishingWs pubWs;
  @Mock private IPSManagedNavService navService;
  @Mock private IPSSite site;

  private PSSiteArchitectureDao dao;

  @BeforeEach
  void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
    dao = new PSSiteArchitectureDao(summaries, pubWs, navService);
    when(pubWs.findSite("Demo")).thenReturn(site);
    when(site.getFolderRoot()).thenReturn("//Sites/Demo");
    when(summaries.pathToId("//Sites/Demo")).thenReturn("folder-1");
    when(navService.getNavTreeContentTypeNames()).thenReturn(List.of("percNavTree", "rffNavTree"));
  }

  @Test
  void findsRffNavTreeWhenCatalogStillUsesFastForwardName() throws Exception {
    PSDataItemSummary tree = new PSDataItemSummary();
    tree.setId("tree-1");
    tree.setName("Demo-NavTree");
    tree.setType("rffNavTree");
    when(summaries.findFolderChildren("folder-1")).thenReturn(List.of(tree));

    PSSiteArchitecture arch = dao.find("Demo");
    PSSiteSection section = firstSection(arch);
    assertEquals("tree-1", section.getId());
    assertEquals("Demo-NavTree", section.getTitle());
  }

  @Test
  void findsRffNavTreeWhenConfigListsOnlyPercName() throws Exception {
    when(navService.getNavTreeContentTypeNames()).thenReturn(List.of("percNavTree"));
    PSDataItemSummary tree = new PSDataItemSummary();
    tree.setId("tree-alias");
    tree.setName("Demo-NavTree");
    tree.setType("rffNavTree");
    when(summaries.findFolderChildren("folder-1")).thenReturn(List.of(tree));

    assertEquals("tree-alias", firstSection(dao.find("Demo")).getId());
  }

  @Test
  void findsPercNavTree() throws Exception {
    PSDataItemSummary tree = new PSDataItemSummary();
    tree.setId("tree-2");
    tree.setName("Demo-NavTree");
    tree.setType("percNavTree");
    when(summaries.findFolderChildren("folder-1")).thenReturn(List.of(tree));

    PSSiteArchitecture arch = dao.find("Demo");
    assertEquals("tree-2", firstSection(arch).getId());
  }

  @Test
  void ignoresNonNavChildren() throws Exception {
    PSDataItemSummary page = new PSDataItemSummary();
    page.setId("page-1");
    page.setName("index");
    page.setType("percPage");
    when(summaries.findFolderChildren("folder-1")).thenReturn(List.of(page));

    PSSiteArchitecture arch = dao.find("Demo");
    assertNull(firstSection(arch).getId());
  }

  private static PSSiteSection firstSection(PSSiteArchitecture arch) {
    List<PSSiteSection> sections =
        arch.getSections().orElseThrow(() -> new AssertionError("expected sections"));
    return sections.get(0);
  }
}
