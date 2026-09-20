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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import com.percussion.pathmanagement.service.IPSPathService.PSPathNotFoundServiceException;
import com.percussion.recent.service.rest.IPSRecentService;
import com.percussion.redirect.service.IPSRedirectService;
import com.percussion.rest.errors.FolderNotFoundException;
import com.percussion.rest.errors.NotAuthorizedException;
import com.percussion.services.content.data.PSItemStatus;
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
import jakarta.ws.rs.WebApplicationException;
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
class FolderAdaptorRenameFolderItemTest {

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
  void renameFolderItemThrowsNotAuthorizedWhenUserIsNotAdmin() {
    when(userService.isAdminUser("admin1")).thenReturn(false);
    assertThrows(
        NotAuthorizedException.class,
        () -> adaptor.renameFolderItem(base, "/Assets/src/item", "new"));
  }

  @Test
  void renameFolderItemMapsMissingSourceToFolderNotFound() throws Exception {
    when(folderHelper.findItem("//Folders/$System$/Assets/missing/item"))
        .thenThrow(new PSPathNotFoundServiceException("missing"));
    assertThrows(
        FolderNotFoundException.class,
        () -> adaptor.renameFolderItem(base, "/Assets/missing/item", "new"));
  }

  @Test
  void renameFolderItemRejectsFolderSelectionAsConflict() throws Exception {
    PSDataItemSummary folder = new PSDataItemSummary();
    folder.setId("1-101-1");
    folder.setType("Folder");
    when(folderHelper.findItem("//Folders/$System$/Assets/src")).thenReturn(folder);
    WebApplicationException thrown =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.renameFolderItem(base, "/Assets/src", "new"));
    assertEquals(jakarta.ws.rs.core.Response.Status.CONFLICT.getStatusCode(), thrown.getResponse().getStatus());
  }

  @Test
  void renameFolderItemSavesPageName() throws Exception {
    PSDataItemSummary pageSummary = new PSDataItemSummary();
    pageSummary.setId("1-202-1");
    pageSummary.setType("percPage");
    when(folderHelper.findItem("//Sites/Demo/Home")).thenReturn(pageSummary);
    PSLegacyGuid guid = new PSLegacyGuid(202, 1);
    when(idMapper.getGuid("1-202-1")).thenReturn(guid);
    PSItemStatus status = mock(PSItemStatus.class);
    when(contentService.prepareForEdit(guid)).thenReturn(status);
    PSCoreItem core = mock(PSCoreItem.class);
    when(contentService.loadItems(anyList(), eq(true), eq(false), eq(false), eq(false)))
        .thenReturn(Collections.singletonList(core));
    when(contentService.getIdByPath("//Sites/Demo")).thenReturn(guid);
    when(contentService.saveItems(anyList(), eq(false), eq(false), eq(guid)))
        .thenReturn(List.of(guid));

    adaptor.renameFolderItem(base, "/Sites/Demo/Home", "Home2");

    verify(core).setTextField("sys_title", "Home2");
    verify(core).setTextField("filename", "Home2");
    verify(contentService).saveItems(anyList(), eq(false), eq(false), eq(guid));
    verify(contentService).releaseFromEdit(status, false);
  }

  @Test
  void renameFolderItemTreatsRollbackOnlyAfterSaveAsSuccess() throws Exception {
    PSDataItemSummary source = new PSDataItemSummary();
    source.setId("1-101-7");
    source.setType("percSimpleTextAsset");
    when(folderHelper.findItem("//Folders/$System$/Assets/src/item")).thenReturn(source);
    PSLegacyGuid guid = new PSLegacyGuid(101, 1);
    when(idMapper.getGuid("1-101-7")).thenReturn(guid);
    PSItemStatus status = mock(PSItemStatus.class);
    when(contentService.prepareForEdit(guid)).thenReturn(status);
    PSCoreItem core = mock(PSCoreItem.class);
    when(contentService.loadItems(anyList(), eq(false), eq(false), eq(false), eq(false)))
        .thenReturn(Collections.singletonList(core));
    when(contentService.getIdByPath("//Folders/$System$/Assets/src")).thenReturn(guid);
    when(contentService.saveItems(anyList(), eq(false), eq(false), eq(guid)))
        .thenThrow(
            new org.springframework.transaction.UnexpectedRollbackException("rollback-only"));

    adaptor.renameFolderItem(base, "/Assets/src/item", "qa-renamed");
    verify(core).setTextField("sys_title", "qa-renamed");
    verify(contentService).releaseFromEdit(status, false);
  }

  @Test
  void renameFolderItemSavesAssetSysTitle() throws Exception {
    PSDataItemSummary source = new PSDataItemSummary();
    source.setId("1-101-7");
    source.setType("percSimpleTextAsset");
    when(folderHelper.findItem("//Folders/$System$/Assets/src/item")).thenReturn(source);
    PSLegacyGuid guid = new PSLegacyGuid(101, 1);
    when(idMapper.getGuid("1-101-7")).thenReturn(guid);
    PSItemStatus status = mock(PSItemStatus.class);
    when(contentService.prepareForEdit(guid)).thenReturn(status);
    PSCoreItem core = mock(PSCoreItem.class);
    when(contentService.loadItems(anyList(), eq(false), eq(false), eq(false), eq(false)))
        .thenReturn(Collections.singletonList(core));
    when(contentService.getIdByPath("//Folders/$System$/Assets/src")).thenReturn(guid);
    when(contentService.saveItems(anyList(), eq(false), eq(false), eq(guid)))
        .thenReturn(List.of(guid));

    adaptor.renameFolderItem(base, "/Assets/src/item", "qa-renamed");

    verify(core).setTextField("sys_title", "qa-renamed");
    verify(core, never()).setTextField(eq("filename"), eq("qa-renamed"));
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<PSCoreItem>> cap = ArgumentCaptor.forClass(List.class);
    verify(contentService).saveItems(cap.capture(), eq(false), eq(false), eq(guid));
    assertEquals(1, cap.getValue().size());
    verify(contentService).releaseFromEdit(status, false);
    assertTrue(FolderAdaptor.isFileAssetType("percFileAsset"));
    assertFalse(FolderAdaptor.isFileAssetType("percSimpleTextAsset"));
  }
}
