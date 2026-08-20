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

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSCoreItem;
import com.percussion.fastforward.managednav.IPSManagedNavService;
import com.percussion.pagemanagement.assembler.IPSRenderAssemblyBridge;
import com.percussion.pagemanagement.dao.IPSPageDao;
import com.percussion.pagemanagement.dao.IPSPageDaoHelper;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.pathmanagement.service.IPSPathService;
import com.percussion.recent.service.rest.IPSRecentService;
import com.percussion.redirect.service.IPSRedirectService;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.data.PSDataItemSummary;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.sitemanage.service.IPSSiteDataService;
import com.percussion.sitemanage.service.IPSSiteSectionService;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentWs;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("UnitTest")
@ExtendWith(MockitoExtension.class)
class FolderAdaptorCopyFolderItemTest {

  @Mock private IPSPathService pathService;
  @Mock private IPSFolderHelper folderHelper;
  @Mock private IPSSiteSectionService sectionService;
  @Mock private IPSManagedNavService navSrv;
  @Mock private IPSIdMapper idMapper;
  @Mock private IPSPageService pageService;
  @Mock private IPSTemplateService templateService;
  @Mock private IPSPageDaoHelper pageDaoHelper;
  @Mock private IPSPageDao pageDao;
  @Mock private IPSContentWs contentService;
  @Mock private IPSRenderAssemblyBridge asmBridge;
  @Mock private IPSUserService userService;
  @Mock private IPSRedirectService redirectService;
  @Mock private IPSSiteDataService siteDataService;
  @Mock private IPSRecentService recentService;

  private FolderAdaptor adaptor;
  private final URI base = URI.create("http://localhost/rest");

  @BeforeEach
  void setUp() throws Exception {
    adaptor =
        new FolderAdaptor(
            pathService,
            folderHelper,
            sectionService,
            navSrv,
            idMapper,
            pageService,
            templateService,
            pageDaoHelper,
            pageDao,
            contentService,
            asmBridge,
            userService,
            redirectService,
            siteDataService,
            recentService);
    PSCurrentUser admin = new PSCurrentUser();
    admin.setName("admin1");
    when(userService.getCurrentUser()).thenReturn(admin);
    when(userService.isAdminUser("admin1")).thenReturn(true);
  }

  @Test
  void copyFolderItemNormalizesSlashPathAndClonesWithNullRelationshipType() throws Exception {
    PSDataItemSummary source = new PSDataItemSummary();
    source.setId("1-101-7");
    source.setType("percSimpleTextAsset");
    when(folderHelper.findItem("//Folders/$System$/Assets/src/item")).thenReturn(source);
    PSLegacyGuid guid = new PSLegacyGuid(101, 1);
    when(idMapper.getGuid("1-101-7")).thenReturn(guid);
    when(contentService.newCopies(anyList(), anyList(), isNull(), eq(false)))
        .thenReturn(Collections.singletonList(mock(PSCoreItem.class)));

    adaptor.copyFolderItem(
        base, "/Folders/$System$/Assets/src/item", "/Folders/$System$/Assets/dst");

    verify(folderHelper).findItem("//Folders/$System$/Assets/src/item");
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<IPSGuid>> guidCap = ArgumentCaptor.forClass(List.class);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<String>> pathCap = ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<String> relCap = ArgumentCaptor.forClass(String.class);
    verify(contentService)
        .newCopies(guidCap.capture(), pathCap.capture(), relCap.capture(), eq(false));
    assertEquals(1, guidCap.getValue().size());
    assertEquals(guid, guidCap.getValue().get(0));
    assertEquals(List.of("//Folders/$System$/Assets/dst"), pathCap.getValue());
    assertNull(relCap.getValue());
  }

  @Test
  void copyFolderItemNormalizesFinderAssetPath() throws Exception {
    PSDataItemSummary source = new PSDataItemSummary();
    source.setId("1-101-9");
    source.setType("percSimpleTextAsset");
    when(folderHelper.findItem("//Folders/$System$/Assets/src/item")).thenReturn(source);
    PSLegacyGuid guid = new PSLegacyGuid(109, 1);
    when(idMapper.getGuid("1-101-9")).thenReturn(guid);
    when(contentService.newCopies(anyList(), anyList(), isNull(), eq(false)))
        .thenReturn(Collections.singletonList(mock(PSCoreItem.class)));

    adaptor.copyFolderItem(base, "/Assets/src/item", "/Assets/dst");

    verify(folderHelper).findItem("//Folders/$System$/Assets/src/item");
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<String>> pathCap = ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<String> relCap = ArgumentCaptor.forClass(String.class);
    verify(contentService)
        .newCopies(anyList(), pathCap.capture(), relCap.capture(), eq(false));
    assertEquals(List.of("//Folders/$System$/Assets/dst"), pathCap.getValue());
    assertNull(relCap.getValue());
  }
}
