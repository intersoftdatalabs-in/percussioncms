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
package com.percussion.pathmanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.pathmanagement.data.PSMoveFolderItem;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pathmanagement {@code moveItem} must convert finder {@code /Assets} paths to
 * repository {@code //} before {@code folderHelper.moveItem} (#3655).
 */
@Tag("UnitTest")
@ExtendWith(MockitoExtension.class)
class PSPathItemServiceMoveTest {

  @Mock private IPSFolderHelper folderHelper;
  @Mock private IPSUserService userService;

  private TestFoldersPathItemService service;

  @BeforeEach
  void setUp() {
    service = new TestFoldersPathItemService(folderHelper, userService);
  }

  @Test
  void toMoveRepositoryPathConvertsAssetsFinderAndAddsTrailingSlash() {
    assertEquals(
        "//Folders/$System$/Assets/qa3655_src/",
        PSPathItemService.toMoveRepositoryPath("/Assets/qa3655_src"));
    assertEquals(
        "//Folders/$System$/Assets/qa3655_dst/",
        PSPathItemService.toMoveRepositoryPath("/Assets/qa3655_dst/"));
    assertEquals(
        "//Folders/$System$/Assets/qa3655_src/",
        PSPathItemService.toMoveRepositoryPath("//Folders/$System$/Assets/qa3655_src"));
    assertEquals(
        "//Folders/$System$/Assets/qa3655_dst/",
        PSPathItemService.toMoveRepositoryPath("Assets/qa3655_dst/"));
    assertEquals("//Sites/Help/", PSPathItemService.toMoveRepositoryPath("/Sites/Help"));
  }

  @Test
  void toMoveRepositoryPathRejectsBlank() {
    assertThrows(Exception.class, () -> PSPathItemService.toMoveRepositoryPath(""));
    assertThrows(Exception.class, () -> PSPathItemService.toMoveRepositoryPath(null));
  }

  @Test
  void moveItemPassesRepositoryPathsToFolderHelper() throws Exception {
    PSCurrentUser admin = new PSCurrentUser();
    admin.setAdminUser(true);
    when(userService.getCurrentUser()).thenReturn(admin);

    PSMoveFolderItem request = new PSMoveFolderItem();
    request.setItemPath("/Assets/qa3655_src");
    request.setTargetFolderPath("/Assets/qa3655_dst");
    service.moveItem(request);

    verify(folderHelper)
        .moveItem(
            "//Folders/$System$/Assets/qa3655_dst/",
            "//Folders/$System$/Assets/qa3655_src/",
            true);
  }

  private static final class TestFoldersPathItemService extends PSFoldersPathItemService {
    TestFoldersPathItemService(IPSFolderHelper folderHelper, IPSUserService userService) {
      super(folderHelper, null, null, null, null, null, null, null, null, userService);
    }
  }
}
