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
 * distributed under the License is distributed on an AS IS BASIS,
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

import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.pathmanagement.service.IPSPathService;
import com.percussion.recycle.service.IPSRecycleService;
import com.percussion.rest.errors.FolderNotFoundException;
import com.percussion.rest.errors.NotAuthorizedException;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.sitemanage.service.IPSSiteDataService;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import jakarta.ws.rs.WebApplicationException;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Restore recycled Explorer items via {@code PUT /rest/folders/recycle/restore/{guid}} (#4700). */
@Tag("UnitTest")
@ExtendWith(MockitoExtension.class)
class FolderAdaptorRestoreRecycledItemTest {

  @Mock private IPSPathService pathService;
  @Mock private IPSFolderHelper folderHelper;
  @Mock private IPSUserService userService;
  @Mock private IPSSiteDataService siteDataService;
  @Mock private IPSRecycleService recycleService;

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
    adaptor.setRecycleService(recycleService);
    PSCurrentUser admin = new PSCurrentUser();
    admin.setName("admin1");
    when(userService.getCurrentUser()).thenReturn(admin);
    when(userService.isAdminUser("admin1")).thenReturn(true);
  }

  private PSPathItem recycledLeaf() {
    PSPathItem item = new PSPathItem();
    item.setName("qa4700itm");
    item.setId("1-101-9");
    item.setType("percSimpleTextAsset");
    item.setLeaf(true);
    item.setFolderPaths(List.of("//Folders/$System$/Recycling/Assets"));
    return item;
  }

  private PSPathItem recycledFolder() {
    PSPathItem item = new PSPathItem();
    item.setName("qa4700fld");
    item.setId("1-101-8");
    item.setType("Folder");
    item.setLeaf(false);
    item.setFolderPaths(List.of("//Folders/$System$/Recycling/Assets"));
    return item;
  }

  @Test
  void restoreRecycledItemRestoresLeafViaRecycleService() throws Exception {
    PSPathItem item = recycledLeaf();
    when(folderHelper.findItemById("1-101-9", PSRelationshipConfig.TYPE_RECYCLED_CONTENT))
        .thenReturn(item);
    when(recycleService.isInRecycler("1-101-9")).thenReturn(true);
    when(folderHelper.isFolderValidForRecycleOrRestore(
            anyString(),
            anyString(),
            eq(PSRelationshipConfig.TYPE_FOLDER_CONTENT),
            eq(PSRelationshipConfig.TYPE_RECYCLED_CONTENT)))
        .thenReturn(true);

    assertDoesNotThrow(() -> adaptor.restoreRecycledItem(base, "1-101-9"));

    verify(recycleService).restoreItem("1-101-9");
    verify(recycleService, never()).restoreFolder(anyString());
  }

  @Test
  void restoreRecycledItemRestoresFolderViaRecycleService() throws Exception {
    PSPathItem item = recycledFolder();
    when(folderHelper.findItemById("1-101-8", PSRelationshipConfig.TYPE_RECYCLED_CONTENT))
        .thenReturn(item);
    when(recycleService.isInRecycler("1-101-8")).thenReturn(true);
    when(folderHelper.isFolderValidForRecycleOrRestore(
            anyString(),
            anyString(),
            eq(PSRelationshipConfig.TYPE_FOLDER_CONTENT),
            eq(PSRelationshipConfig.TYPE_RECYCLED_CONTENT)))
        .thenReturn(true);

    assertDoesNotThrow(() -> adaptor.restoreRecycledItem(base, "1-101-8"));

    verify(recycleService).restoreFolder("1-101-8");
    verify(recycleService, never()).restoreItem(anyString());
  }

  @Test
  void restoreRecycledItemThrowsNotAuthorizedWhenUserIsNotAdmin() throws Exception {
    when(userService.isAdminUser("admin1")).thenReturn(false);
    assertThrows(
        NotAuthorizedException.class, () -> adaptor.restoreRecycledItem(base, "1-101-9"));
    verify(recycleService, never()).restoreItem(anyString());
  }

  @Test
  void restoreRecycledItemMapsMissingToFolderNotFound() throws Exception {
    when(folderHelper.findItemById("missing", PSRelationshipConfig.TYPE_RECYCLED_CONTENT))
        .thenThrow(new RuntimeException("missing"));
    assertThrows(FolderNotFoundException.class, () -> adaptor.restoreRecycledItem(base, "missing"));
  }

  @Test
  void restoreRecycledItemConflictsWhenDestinationOccupied() throws Exception {
    PSPathItem item = recycledLeaf();
    when(folderHelper.findItemById("1-101-9", PSRelationshipConfig.TYPE_RECYCLED_CONTENT))
        .thenReturn(item);
    when(recycleService.isInRecycler("1-101-9")).thenReturn(true);
    when(folderHelper.isFolderValidForRecycleOrRestore(
            anyString(),
            anyString(),
            eq(PSRelationshipConfig.TYPE_FOLDER_CONTENT),
            eq(PSRelationshipConfig.TYPE_RECYCLED_CONTENT)))
        .thenReturn(false);
    WebApplicationException thrown =
        assertThrows(
            WebApplicationException.class, () -> adaptor.restoreRecycledItem(base, "1-101-9"));
    assertEquals(409, thrown.getResponse().getStatus());
    verify(recycleService, never()).restoreItem(anyString());
  }
}
