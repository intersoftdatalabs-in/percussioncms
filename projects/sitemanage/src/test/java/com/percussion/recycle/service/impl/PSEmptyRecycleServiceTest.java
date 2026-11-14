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
package com.percussion.recycle.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.pathmanagement.data.PSDeleteFolderCriteria;
import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.pathmanagement.service.IPSPathService;
import com.percussion.recycle.data.PSEmptyRecycleResult;
import com.percussion.recycle.service.IPSEmptyRecycleService.PSEmptyRecycleNotAuthorizedException;
import com.percussion.recycle.service.IPSEmptyRecycleService.PSEmptyRecycleNotFoundException;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PSEmptyRecycleServiceTest {

  @Mock private IPSPathService pathService;
  @Mock private IPSUserService userService;
  @Mock private IPSFolderHelper folderHelper;

  private PSEmptyRecycleService service;

  @BeforeEach
  void setUp() {
    service = new PSEmptyRecycleService(pathService, userService, folderHelper);
  }

  @Test
  void empty_whenAlreadyEmpty_isIdempotent() throws Exception {
    stubAdmin("admin");
    when(pathService.findChildren(PSEmptyRecycleService.RECYCLING_FINDER_ROOT))
        .thenReturn(Collections.emptyList());

    PSEmptyRecycleResult result = service.emptyRecyclingBin();

    assertTrue(result.isAlreadyEmpty());
    assertEquals(0, result.getPurgedFolderCount());
    assertEquals(0, result.getPurgedItemCount());
    assertEquals(0, result.getUndeletedCount());
    verify(pathService, never()).deleteFolder(any());
  }

  @Test
  void empty_whenRecyclingRootMissing_throwsNotFound() throws Exception {
    stubAdmin("admin");
    when(pathService.findChildren(PSEmptyRecycleService.RECYCLING_FINDER_ROOT))
        .thenThrow(new IPSPathService.PSPathNotFoundServiceException("missing"));

    assertThrows(PSEmptyRecycleNotFoundException.class, () -> service.emptyRecyclingBin());
    verify(pathService, never()).deleteFolder(any());
  }

  @Test
  void empty_whenNonAdmin_throwsNotAuthorized() throws Exception {
    PSCurrentUser user = new PSCurrentUser();
    user.setName("editor");
    when(userService.getCurrentUser()).thenReturn(user);
    when(userService.isAdminUser("editor")).thenReturn(false);

    assertThrows(PSEmptyRecycleNotAuthorizedException.class, () -> service.emptyRecyclingBin());
    verify(pathService, never()).findChildren(any());
  }

  @Test
  void empty_preservesTopLevelStructureAndPurgesNestedFolders() throws Exception {
    stubAdmin("admin");
    PSPathItem assets = folderItem("/Recycling/Assets/", "guid-assets");
    PSPathItem nested = folderItem("/Recycling/Assets/zz1/", "guid-zz1");
    when(pathService.findChildren(PSEmptyRecycleService.RECYCLING_FINDER_ROOT))
        .thenReturn(List.of(assets));
    when(pathService.findChildren("/Recycling/Assets/")).thenReturn(List.of(nested));
    when(pathService.deleteFolder(any())).thenReturn(0);

    PSEmptyRecycleResult result = service.emptyRecyclingBin();

    assertFalse(result.isAlreadyEmpty());
    assertEquals(1, result.getPurgedFolderCount());
    assertEquals(0, result.getUndeletedCount());

    // The structural mirror itself is never deleted — only its contents.
    ArgumentCaptor<PSDeleteFolderCriteria> captor =
        ArgumentCaptor.forClass(PSDeleteFolderCriteria.class);
    verify(pathService).deleteFolder(captor.capture());
    PSDeleteFolderCriteria criteria = captor.getValue();
    assertTrue(criteria.getShouldPurge());
    assertEquals(PSDeleteFolderCriteria.SkipItemsType.YES, criteria.getSkipItems());
    assertEquals("/Recycling/Assets/zz1/", criteria.getPath());
    assertEquals("guid-zz1", criteria.getGuid());
  }

  @Test
  void empty_preservesEmptyTopLevelStructure() throws Exception {
    stubAdmin("admin");
    PSPathItem assets = folderItem("/Recycling/Assets/", "guid-assets");
    when(pathService.findChildren(PSEmptyRecycleService.RECYCLING_FINDER_ROOT))
        .thenReturn(List.of(assets));
    when(pathService.findChildren("/Recycling/Assets/")).thenReturn(Collections.emptyList());

    PSEmptyRecycleResult result = service.emptyRecyclingBin();

    assertFalse(result.isAlreadyEmpty());
    assertEquals(0, result.getPurgedFolderCount());
    assertEquals(0, result.getPurgedItemCount());
    assertEquals(0, result.getUndeletedCount());
    assertTrue(result.getErrors().isEmpty());
    verify(pathService, never()).deleteFolder(any());
    verify(folderHelper, never()).removeItem(any(), any(), any(Boolean.class));
  }

  @Test
  void empty_purgesNestedLeafViaActualRecycledParent() throws Exception {
    stubAdmin("admin");
    PSPathItem assets = folderItem("/Recycling/Assets/", "guid-assets");
    PSPathItem leaf = leafItem("/Recycling/Assets/leaf1", "guid-leaf1");
    leaf.setFolderPaths(List.of("//Folders/$System$/Recycling/Assets/"));
    when(pathService.findChildren(PSEmptyRecycleService.RECYCLING_FINDER_ROOT))
        .thenReturn(List.of(assets));
    when(pathService.findChildren("/Recycling/Assets/")).thenReturn(List.of(leaf));

    PSEmptyRecycleResult result = service.emptyRecyclingBin();

    assertEquals(1, result.getPurgedItemCount());
    assertEquals(0, result.getPurgedFolderCount());
    // Nested leaves purge under their actual recycled parent, never the bin root.
    verify(folderHelper)
        .removeItem(eq("//Folders/$System$/Recycling/Assets"), eq("guid-leaf1"), eq(true));
    verify(pathService, never()).deleteFolder(any());
  }

  @Test
  void empty_purgesLeafItemsViaFolderHelperOnlyUnderRecyclingRoot() throws Exception {
    stubAdmin("admin");
    PSPathItem leaf = leafItem("/Recycling/orphan-page", "guid-leaf");
    when(pathService.findChildren(PSEmptyRecycleService.RECYCLING_FINDER_ROOT))
        .thenReturn(List.of(leaf));

    PSEmptyRecycleResult result = service.emptyRecyclingBin();

    assertEquals(1, result.getPurgedItemCount());
    verify(folderHelper)
        .removeItem(eq(PSRecycleService.RECYCLING_ROOT), eq("guid-leaf"), eq(true));
    verify(pathService, never()).deleteFolder(any());
  }

  @Test
  void empty_refusesPathsOutsideRecycling() throws Exception {
    stubAdmin("admin");
    // Defensive: if a child somehow reported a non-Recycling path, do not purge it.
    PSPathItem evil = folderItem("/Sites/LiveSite/", "guid-live");
    when(pathService.findChildren(PSEmptyRecycleService.RECYCLING_FINDER_ROOT))
        .thenReturn(List.of(evil));

    PSEmptyRecycleResult result = service.emptyRecyclingBin();

    assertEquals(0, result.getPurgedFolderCount());
    assertEquals(1, result.getUndeletedCount());
    assertFalse(result.getErrors().isEmpty());
    verify(pathService, never()).deleteFolder(any());
    verify(folderHelper, never()).removeItem(any(), any(), any(Boolean.class));
  }

  @Test
  void empty_accumulatesUndeletedFromNestedDeleteFolder() throws Exception {
    stubAdmin("admin");
    PSPathItem assets = folderItem("/Recycling/Assets/", "guid-assets");
    PSPathItem nested = folderItem("/Recycling/Assets/zz1/", "guid-zz1");
    when(pathService.findChildren(PSEmptyRecycleService.RECYCLING_FINDER_ROOT))
        .thenReturn(List.of(assets));
    when(pathService.findChildren("/Recycling/Assets/")).thenReturn(List.of(nested));
    when(pathService.deleteFolder(any())).thenReturn(3);

    PSEmptyRecycleResult result = service.emptyRecyclingBin();

    assertEquals(1, result.getPurgedFolderCount());
    assertEquals(3, result.getUndeletedCount());
  }

  @Test
  void isUnderRecyclingRoot_safetyChecks() {
    assertTrue(PSEmptyRecycleService.isUnderRecyclingRoot("/Recycling/"));
    assertTrue(PSEmptyRecycleService.isUnderRecyclingRoot("/Recycling/Sites/"));
    assertTrue(PSEmptyRecycleService.isUnderRecyclingRoot("/recycling/assets/"));
    assertFalse(PSEmptyRecycleService.isUnderRecyclingRoot("/Sites/"));
    assertFalse(PSEmptyRecycleService.isUnderRecyclingRoot("/Assets/uploads/"));
    assertFalse(PSEmptyRecycleService.isUnderRecyclingRoot("/"));
    assertFalse(PSEmptyRecycleService.isUnderRecyclingRoot(""));
    assertFalse(PSEmptyRecycleService.isUnderRecyclingRoot(null));
    // Prefix confusion: /RecyclingEvil must not pass
    assertFalse(PSEmptyRecycleService.isUnderRecyclingRoot("/RecyclingEvil/"));
  }

  @Test
  void normalizeFinderPath_addsSlashes() {
    assertEquals("/Recycling/Sites/", PSEmptyRecycleService.normalizeFinderPath("Recycling/Sites"));
    assertEquals("/Recycling/", PSEmptyRecycleService.normalizeFinderPath("/Recycling"));
  }

  @Test
  void recycledParentOf_returnsActualBinParent() {
    PSPathItem leaf = leafItem("/Recycling/Assets/leaf1", "guid-leaf1");
    leaf.setFolderPaths(List.of("//Folders/$System$/Recycling/Assets/"));
    assertEquals(
        "//Folders/$System$/Recycling/Assets", PSEmptyRecycleService.recycledParentOf(leaf));
  }

  @Test
  void recycledParentOf_fallsBackToRecyclingRoot() {
    assertEquals(
        PSRecycleService.RECYCLING_ROOT,
        PSEmptyRecycleService.recycledParentOf(leafItem("/Recycling/orphan", "guid-x")));
    assertEquals(PSRecycleService.RECYCLING_ROOT, PSEmptyRecycleService.recycledParentOf(null));
    PSPathItem outside = leafItem("/Sites/evil", "guid-y");
    outside.setFolderPaths(List.of("//Sites/Evil"));
    assertEquals(
        PSRecycleService.RECYCLING_ROOT, PSEmptyRecycleService.recycledParentOf(outside));
  }

  private void stubAdmin(String name) throws Exception {
    PSCurrentUser user = new PSCurrentUser();
    user.setName(name);
    when(userService.getCurrentUser()).thenReturn(user);
    when(userService.isAdminUser(name)).thenReturn(true);
  }

  private static PSPathItem folderItem(String path, String id) {
    PSPathItem item = new PSPathItem();
    item.setType("Folder");
    item.setLeaf(false);
    item.setId(id);
    item.setPath(path);
    return item;
  }

  private static PSPathItem leafItem(String path, String id) {
    PSPathItem item = new PSPathItem();
    item.setType("percPage");
    item.setLeaf(true);
    item.setId(id);
    item.setPath(path);
    return item;
  }
}
