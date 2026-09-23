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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.pathmanagement.data.PSDeleteFolderCriteria;
import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.pathmanagement.service.IPSPathService;
import com.percussion.recycle.service.IPSRecycleService;
import com.percussion.recycle.service.impl.PSRecycleService;
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
    when(userService.getCurrentUser()).thenReturn(admin);
    when(userService.isAdminUser("admin1")).thenReturn(true);
  }

  private PSPathItem recycledLeaf() {
    PSPathItem item = new PSPathItem();
    item.setName("qa4763itm");
    item.setId("1-101-9");
    item.setType("percSimpleTextAsset");
    item.setLeaf(true);
    item.setPath("/Recycling/Assets/qa4763itm");
    return item;
  }

  private PSPathItem recycledFolder() {
    PSPathItem item = new PSPathItem();
    item.setName("qa4763fld");
    item.setId("1-101-8");
    item.setType("Folder");
    item.setLeaf(false);
    item.setPath("/Recycling/Assets/qa4763fld");
    return item;
  }

  @Test
  void purgeRecycledItemPurgesLeaf() throws Exception {
    when(folderHelper.findItemById("1-101-9", PSRelationshipConfig.TYPE_RECYCLED_CONTENT))
        .thenReturn(recycledLeaf());

    assertDoesNotThrow(() -> adaptor.purgeRecycledItem(base, "1-101-9"));

    verify(folderHelper).removeItem(PSRecycleService.RECYCLING_ROOT, "1-101-9", true);
    verify(pathService, never()).deleteFolder(any());
  }

  @Test
  void purgeRecycledItemPurgesFolder() throws Exception {
    when(folderHelper.findItemById("1-101-8", PSRelationshipConfig.TYPE_RECYCLED_CONTENT))
        .thenReturn(recycledFolder());
    when(pathService.deleteFolder(any(PSDeleteFolderCriteria.class))).thenReturn(0);

    assertDoesNotThrow(() -> adaptor.purgeRecycledItem(base, "1-101-8"));

    verify(pathService).deleteFolder(any(PSDeleteFolderCriteria.class));
    verify(folderHelper, never()).removeItem(eq(PSRecycleService.RECYCLING_ROOT), eq("1-101-8"), eq(true));
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
        .removeItem(PSRecycleService.RECYCLING_ROOT, "1-101-9", true);
    WebApplicationException thrown =
        assertThrows(WebApplicationException.class, () -> adaptor.purgeRecycledItem(base, "1-101-9"));
    assertEquals(409, thrown.getResponse().getStatus());
  }
}
