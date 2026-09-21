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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.fastforward.managednav.IPSManagedNavService;
import com.percussion.pagemanagement.assembler.IPSRenderAssemblyBridge;
import com.percussion.pagemanagement.dao.IPSPageDao;
import com.percussion.pagemanagement.dao.IPSPageDaoHelper;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.pathmanagement.service.IPSPathService;
import com.percussion.pathmanagement.service.IPSPathService.PSPathNotFoundServiceException;
import com.percussion.recent.service.rest.IPSRecentService;
import com.percussion.redirect.service.IPSRedirectService;
import com.percussion.rest.errors.FolderNotFoundException;
import com.percussion.rest.errors.NotAuthorizedException;
import com.percussion.rest.folders.Folder;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.sitemanage.service.IPSSiteDataService;
import com.percussion.sitemanage.service.IPSSiteSectionService;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import com.percussion.webservices.content.IPSContentWs;
import jakarta.ws.rs.WebApplicationException;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("UnitTest")
@ExtendWith(MockitoExtension.class)
class FolderAdaptorCreateFolderTest {

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
  void createFolderThrowsNotAuthorizedWhenUserIsNotAdmin() {
    when(userService.isAdminUser("admin1")).thenReturn(false);
    assertThrows(
        NotAuthorizedException.class, () -> adaptor.createFolder(base, "/Assets", "qa4637"));
  }

  @Test
  void createFolderMapsMissingParentToFolderNotFound() throws Exception {
    when(pathService.find("/Assets/missing"))
        .thenThrow(new PSPathNotFoundServiceException("missing"));
    assertThrows(
        FolderNotFoundException.class,
        () -> adaptor.createFolder(base, "/Assets/missing", "qa4637"));
  }

  @Test
  void createFolderRejectsPathSeparatorsInName() {
    WebApplicationException thrown =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.createFolder(base, "/Assets", "a/b"));
    assertEquals(
        jakarta.ws.rs.core.Response.Status.BAD_REQUEST.getStatusCode(),
        thrown.getResponse().getStatus());
  }

  @Test
  void createFolderRejectsExistingNameAsConflict() throws Exception {
    PSPathItem parent = new PSPathItem();
    parent.setType("Folder");
    parent.setPath("/Assets/");
    when(pathService.find("/Assets")).thenReturn(parent);
    when(folderHelper.concatPath("/Assets", "dup", "/")).thenReturn("/Assets/dup/");
    PSPathItem existing = new PSPathItem();
    existing.setName("dup");
    when(pathService.find("/Assets/dup/")).thenReturn(existing);

    WebApplicationException thrown =
        assertThrows(
            WebApplicationException.class, () -> adaptor.createFolder(base, "/Assets", "dup"));
    assertEquals(
        jakarta.ws.rs.core.Response.Status.CONFLICT.getStatusCode(),
        thrown.getResponse().getStatus());
    verify(pathService, never()).addFolder(anyString());
  }

  @Test
  void createFolderAddsNamedFolder() throws Exception {
    PSPathItem parent = new PSPathItem();
    parent.setType("Folder");
    parent.setPath("/Assets/");
    when(pathService.find("/Assets")).thenReturn(parent);
    when(folderHelper.concatPath("/Assets", "qa4637", "/")).thenReturn("/Assets/qa4637/");
    when(pathService.find("/Assets/qa4637/"))
        .thenThrow(new PSPathNotFoundServiceException("free"));
    PSPathItem created = new PSPathItem();
    created.setId("1-4637-1");
    created.setName("qa4637");
    created.setPath("/Assets/qa4637/");
    when(pathService.addFolder("/Assets/qa4637/")).thenReturn(created);

    Folder folder = adaptor.createFolder(base, "/Assets", "qa4637");
    assertEquals("qa4637", folder.getName());
    assertEquals("/Assets/qa4637/", folder.getPath());
    assertEquals("1-4637-1", folder.getId());
    verify(pathService).addFolder("/Assets/qa4637/");
  }
}
