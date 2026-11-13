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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.pathmanagement.data.PSDeleteFolderCriteria;
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

/** Purge one recycled Explorer item via {@code DELETE /rest/folders/recycle/{guid}} (#4763). */
@Tag("UnitTest")
@ExtendWith(MockitoExtension.class)
class FolderAdaptorPurgeRecycledItemTest {

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
    lenient().when(userService.getCurrentUser()).thenReturn(admin);
    lenient().when(userService.isAdminUser("admin1")).thenReturn(true);
  }

  private PSPathItem recycledLeaf() {
    PSPathItem item = new PSPathItem();
    item.setName("qa4763itm");
    item.setId("1-101-9");
    item.setType("percSimpleTextAsset");
    item.setLeaf(true);
    item.setPath("/Recycling/Assets/qa4763itm");
    item.setFolderPaths(List.of("//Folders/$System$/Recycling/Assets/"));
    return item;
  }

  private PSPathItem recycledFolder() {
    PSPathItem item = new PSPathItem();
    item.setName("qa4763fld");
    item.setId("1-101-8");
    item.setType("Folder");
    item.setLeaf(false);
    // Recycled folder summaries carry no finder path; purge derives it from
    // the recycled parent (#6257).
    item.setFolderPaths(List.of("//Folders/$System$/Recycling/Assets/"));
    return item;
  }

  @Test
  void purgeRecycledItemPurgesLeaf() throws Exception {
    when(folderHelper.findItemById("1-101-9", PSRelationshipConfig.TYPE_RECYCLED_CONTENT))
        .thenReturn(recycledLeaf());

    assertDoesNotThrow(() -> adaptor.purgeRecycledItem(base, "1-101-9"));

    // Purge runs against the actual recycled parent, not the Recycling root:
    // nested recycled items are not direct children of the root id (#6257).
    verify(folderHelper)
        .removeItem("//Folders/$System$/Recycling/Assets", "1-101-9", true);
    verify(pathService, never()).deleteFolder(any());
  }

  @Test
  void purgeRecycledItemPurgesFolder() throws Exception {
    when(folderHelper.findItemById("1-101-8", PSRelationshipConfig.TYPE_RECYCLED_CONTENT))
        .thenReturn(recycledFolder());
    when(pathService.deleteFolder(any(PSDeleteFolderCriteria.class))).thenReturn(0);

    assertDoesNotThrow(() -> adaptor.purgeRecycledItem(base, "1-101-8"));

    org.mockito.ArgumentCaptor<PSDeleteFolderCriteria> captor =
        org.mockito.ArgumentCaptor.forClass(PSDeleteFolderCriteria.class);
    verify(pathService).deleteFolder(captor.capture());
    assertEquals("/Recycling/Assets/qa4763fld/", captor.getValue().getPath());
    verify(folderHelper, never()).removeItem(any(), eq("1-101-8"), eq(true));
  }

  @Test
  void recyclingFinderPathShapes() {
    assertEquals(
        "/Recycling/Assets/qa4763fld/",
        FolderAdaptor.recyclingFinderPath("//Folders/$System$/Recycling/Assets", "qa4763fld"));
    assertEquals(
        "/Recycling/qa4763fld/",
        FolderAdaptor.recyclingFinderPath("//Folders/$System$/Recycling", "qa4763fld"));
    assertNull(
        FolderAdaptor.recyclingFinderPath("//Folders/$System$/Assets", "qa4763fld"));
    assertNull(FolderAdaptor.recyclingFinderPath(null, "qa4763fld"));
    assertNull(FolderAdaptor.recyclingFinderPath("//Folders/$System$/Recycling/Assets", null));
    assertNull(FolderAdaptor.recyclingFinderPath("//Folders/$System$/Recycling/Assets", "a/b"));
  }

  @Test
  void purgeRecycledItemThrowsNotAuthorizedWhenUserIsNotAdmin() {
    when(userService.isAdminUser("admin1")).thenReturn(false);
    assertThrows(NotAuthorizedException.class, () -> adaptor.purgeRecycledItem(base, "1-101-9"));
  }

  @Test
  void purgeRecycledItemMapsMissingToFolderNotFound() throws Exception {
    when(folderHelper.findItemById("missing", PSRelationshipConfig.TYPE_RECYCLED_CONTENT))
        .thenThrow(new RuntimeException("missing"));
    assertThrows(FolderNotFoundException.class, () -> adaptor.purgeRecycledItem(base, "missing"));
  }

  @Test
  void purgeRecycledItemMapsNullLookupToNotFound() throws Exception {
    when(folderHelper.findItemById("1-101-9", PSRelationshipConfig.TYPE_RECYCLED_CONTENT))
        .thenReturn(null);
    assertThrows(FolderNotFoundException.class, () -> adaptor.purgeRecycledItem(base, "1-101-9"));
    verify(folderHelper, never()).removeItem(any(), any(), eq(true));
  }

  @Test
  void purgeRecycledItemMapsRemoveFailureToConflict() throws Exception {
    when(folderHelper.findItemById("1-101-9", PSRelationshipConfig.TYPE_RECYCLED_CONTENT))
        .thenReturn(recycledLeaf());
    org.mockito.Mockito.doThrow(new RuntimeException("locked"))
        .when(folderHelper)
        .removeItem("//Folders/$System$/Recycling/Assets", "1-101-9", true);
    WebApplicationException thrown =
        assertThrows(WebApplicationException.class, () -> adaptor.purgeRecycledItem(base, "1-101-9"));
    assertEquals(409, thrown.getResponse().getStatus());
  }

  @Test
  void purgeRecycledItemNormalizesSingleSlashParent() throws Exception {
    PSPathItem leaf = recycledLeaf();
    leaf.setFolderPaths(List.of("/Folders/$System$/Assets/"));
    when(folderHelper.findItemById("1-101-9", PSRelationshipConfig.TYPE_RECYCLED_CONTENT))
        .thenReturn(leaf);

    assertDoesNotThrow(() -> adaptor.purgeRecycledItem(base, "1-101-9"));

    verify(folderHelper).removeItem("//Folders/$System$/Assets", "1-101-9", true);
  }

  @Test
  void firstRecycledParentPathNormalizesShapes() {
    assertEquals(
        "//Folders/$System$/Recycling/Assets",
        FolderAdaptor.firstRecycledParentPath(recycledLeaf()));
    PSPathItem singleSlash = recycledLeaf();
    singleSlash.setFolderPaths(List.of("/Folders/$System$/Assets/"));
    assertEquals(
        "//Folders/$System$/Assets",
        FolderAdaptor.firstRecycledParentPath(singleSlash));
    PSPathItem empty = recycledLeaf();
    empty.setFolderPaths(List.of());
    assertNull(FolderAdaptor.firstRecycledParentPath(empty));
    assertNull(FolderAdaptor.firstRecycledParentPath(null));
  }

  @Test
  void purgeRecycledItemWithoutRecycledParentIsConflict() throws Exception {    PSPathItem orphan = recycledLeaf();
    orphan.setFolderPaths(List.of());
    when(folderHelper.findItemById("1-101-9", PSRelationshipConfig.TYPE_RECYCLED_CONTENT))
        .thenReturn(orphan);
    WebApplicationException thrown =
        assertThrows(WebApplicationException.class, () -> adaptor.purgeRecycledItem(base, "1-101-9"));
    assertEquals(409, thrown.getResponse().getStatus());
    verify(folderHelper, never()).removeItem(any(), any(), eq(true));
  }
}
