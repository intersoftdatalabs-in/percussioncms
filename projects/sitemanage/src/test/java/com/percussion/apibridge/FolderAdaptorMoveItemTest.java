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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.pathmanagement.data.PSMoveFolderItem;
import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.pathmanagement.service.IPSPathService;
import com.percussion.pathmanagement.service.IPSPathService.PSPathNotFoundServiceException;
import com.percussion.rest.errors.FolderNotFoundException;
import com.percussion.rest.errors.NotAuthorizedException;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.sitemanage.service.IPSSiteDataService;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifies the FolderAdaptor move REST contract used by {@code
 * FoldersResource#moveFolderItem} and {@code FoldersResource#moveFolder}.
 *
 * <p>The slice contract (#4601): missing source/target → 404 ({@link
 * FolderNotFoundException}); non-admin caller → 403 ({@link
 * NotAuthorizedException}). Successful moves delegate to the shared
 * pathmanagement {@code moveItem} and re-resolve the moved leaf.
 */
@Tag("UnitTest")
@ExtendWith(MockitoExtension.class)
class FolderAdaptorMoveItemTest {

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
  void moveFolderItemNormalizesFinderAssetPathAndDelegatesToPathService()
      throws Exception {
    PSPathItem source = new PSPathItem();
    source.setName("qa4601itm");
    source.setPath("/Folders/$System$/Assets/src/qa4601itm");
    when(pathService.find("/Folders/$System$/Assets/src/qa4601itm")).thenReturn(source);
    PSPathItem moved = new PSPathItem();
    moved.setName("qa4601itm");
    moved.setPath("/Folders/$System$/Assets/dst/qa4601itm");
    when(pathService.find("/Folders/$System$/Assets/dst/qa4601itm")).thenReturn(moved);

    assertDoesNotThrow(
        () ->
            adaptor.moveFolderItem(
                base,
                "/Folders/$System$/Assets/src/qa4601itm",
                "/Folders/$System$/Assets/dst"));

    verify(pathService)
        .find("/Folders/$System$/Assets/src/qa4601itm");
    verify(pathService).moveItem(any(PSMoveFolderItem.class));
    verify(pathService).find("/Folders/$System$/Assets/dst/qa4601itm");
  }

  @Test
  void moveFolderItemThrowsNotAuthorizedWhenUserIsNotAdmin() throws Exception {
    when(userService.isAdminUser("admin1")).thenReturn(false);
    assertThrows(
        NotAuthorizedException.class,
        () -> adaptor.moveFolderItem(base, "/Assets/src/item", "/Assets/dst"));
    verify(pathService, never()).moveItem(any(PSMoveFolderItem.class));
  }

  @Test
  void moveFolderItemMapsMissingSourceToFolderNotFound() throws Exception {
    when(pathService.find("/Assets/missing/item"))
        .thenThrow(new PSPathNotFoundServiceException("missing"));
    assertThrows(
        FolderNotFoundException.class,
        () -> adaptor.moveFolderItem(base, "/Assets/missing/item", "/Assets/dst"));
  }

  @Test
  void moveFolderMapsMissingSourceFolderToFolderNotFound() throws Exception {
    when(pathService.find("/Assets/missing/folder"))
        .thenThrow(new PSPathNotFoundServiceException("missing"));
    assertThrows(
        FolderNotFoundException.class,
        () -> adaptor.moveFolder(base, "/Assets/missing/folder", "/Assets/dst"));
  }
}
