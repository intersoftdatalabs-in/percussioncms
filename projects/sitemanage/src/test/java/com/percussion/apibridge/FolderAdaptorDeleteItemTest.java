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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.pathmanagement.service.IPSPathService;
import com.percussion.pathmanagement.service.IPSPathService.PSPathNotFoundServiceException;
import com.percussion.rest.errors.FolderNotFoundException;
import com.percussion.rest.errors.NotAuthorizedException;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.sitemanage.service.IPSSiteDataService;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import jakarta.ws.rs.WebApplicationException;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Recycle selected Explorer items via {@code FoldersResource#deleteFolderItem}
 * ({@code DELETE /rest/folders/item/{path}}) (#4602).
 */
@Tag("UnitTest")
@ExtendWith(MockitoExtension.class)
class FolderAdaptorDeleteItemTest {

  @Mock private IPSPathService pathService;
  @Mock private IPSFolderHelper folderHelper;
  @Mock private IPSUserService userService;
  @Mock private IPSSiteDataService siteDataService;

  private FolderAdaptor adaptor;
  private final URI base = URI.create("http://localhost/rest");

  @BeforeEach
  void setUp() throws Exception {
    adaptor =
        new FolderAdaptor(
            pathService,
            folderHelper,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            userService,
            null,
            siteDataService,
            null);
    PSCurrentUser admin = new PSCurrentUser();
    admin.setName("admin1");
    when(userService.getCurrentUser()).thenReturn(admin);
    when(userService.isAdminUser("admin1")).thenReturn(true);
  }

  @Test
  void deleteFolderItemRecyclesLeafViaFolderHelper() throws Exception {
    PSPathItem item = new PSPathItem();
    item.setName("qa4602itm");
    item.setPath("/Folders/$System$/Assets/src/qa4602itm");
    item.setFolderPath("/Folders/$System$/Assets/src");
    item.setId("1-101-9");
    item.setType("percSimpleTextAsset");
    item.setLeaf(true);
    when(pathService.find("/Assets/src/qa4602itm")).thenReturn(item);

    assertDoesNotThrow(() -> adaptor.deleteFolderItem(base, "/Assets/src/qa4602itm"));

    verify(folderHelper).removeItem("//Folders/$System$/Assets/src", "1-101-9", false);
  }

  @Test
  void deleteFolderItemThrowsNotAuthorizedWhenUserIsNotAdmin() throws Exception {
    when(userService.isAdminUser("admin1")).thenReturn(false);
    assertThrows(
        NotAuthorizedException.class,
        () -> adaptor.deleteFolderItem(base, "/Assets/src/item"));
    verify(pathService, never()).find(anyString());
  }

  @Test
  void deleteFolderItemMapsMissingToFolderNotFound() throws Exception {
    when(pathService.find("/Assets/missing"))
        .thenThrow(new PSPathNotFoundServiceException("missing"));
    assertThrows(
        FolderNotFoundException.class, () -> adaptor.deleteFolderItem(base, "/Assets/missing"));
  }

  @Test
  void deleteFolderItemConflictsWhenPathIsAFolder() throws Exception {
    PSPathItem folder = new PSPathItem();
    folder.setPath("/Assets/src");
    folder.setType("Folder");
    folder.setLeaf(false);
    when(pathService.find("/Assets/src")).thenReturn(folder);
    WebApplicationException thrown =
        assertThrows(
            WebApplicationException.class, () -> adaptor.deleteFolderItem(base, "/Assets/src"));
    assertEquals(409, thrown.getResponse().getStatus());
    verify(folderHelper, never()).removeItem(anyString(), anyString(), eq(false));
  }
}
