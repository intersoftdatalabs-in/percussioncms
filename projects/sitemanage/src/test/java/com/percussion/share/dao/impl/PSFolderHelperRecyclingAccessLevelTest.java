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

package com.percussion.share.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSFolder;
import com.percussion.pathmanagement.data.PSFolderPermission;
import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.recycle.service.IPSRecycleService;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.services.system.IPSSystemService;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.share.service.IPSDataItemSummaryService;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.PSWebserviceUtils;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.content.IPSContentWs;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Recycling-bin rows resolve no live folder path, so the transient folder load in the access
 * check throws and — escaping the content-service transaction — marks the listing transaction
 * rollback-only (bin listing 500, folder purge/restore 404, #6257). Recycling rows must load
 * without transient data while live rows keep the full load.
 */
@Tag("UnitTest")
@ExtendWith(MockitoExtension.class)
class PSFolderHelperRecyclingAccessLevelTest {

  @Mock private IPSContentWs contentWs;
  @Mock private IPSDataItemSummaryService dataItemSummaryService;
  @Mock private IPSContentDesignWs contentDesignWs;
  @Mock private IPSIdMapper idMapper;
  @Mock private IPSPublisherService pubService;
  @Mock private IPSSystemService sysService;
  @Mock private IPSSiteManager siteMgr;
  @Mock private IPSWorkflowService workflowService;
  @Mock private IPSRecycleService recycleService;
  @Mock private IPSGuid folderGuid;
  @Mock private PSFolder folder;

  private PSFolderHelper helper;

  @BeforeEach
  void setUp() {
    helper =
        new PSFolderHelper(
            contentWs,
            dataItemSummaryService,
            contentDesignWs,
            idMapper,
            pubService,
            sysService,
            null,
            siteMgr,
            workflowService,
            recycleService);
  }

  @Test
  void isRecyclingRowMatchesRecyclingRoot() {
    assertTrue(
        PSFolderHelper.isRecyclingRow(
            row("//Folders/$System$/Recycling/Assets/qa-row", "Folder")));
    assertTrue(
        PSFolderHelper.isRecyclingRow(row(null, "Folder", "//Folders/$System$/Recycling/")));
    assertFalse(PSFolderHelper.isRecyclingRow(row("//Folders/$System$/Assets/qa", "Folder")));
    assertFalse(PSFolderHelper.isRecyclingRow(row(null, "Folder")));
    assertFalse(PSFolderHelper.isRecyclingRow(null));
  }

  @Test
  void recycledRowLoadsFolderWithoutTransientData() throws Exception {
    when(folder.getAcl()).thenReturn(null);
    when(idMapper.getGuid("1-101-8")).thenReturn(folderGuid);
    when(contentWs.loadFolder(folderGuid, false)).thenReturn(folder);

    try (MockedStatic<PSWebserviceUtils> ws = Mockito.mockStatic(PSWebserviceUtils.class)) {
      ws.when(PSWebserviceUtils::getUserName).thenReturn("admin1");
      ws.when(PSWebserviceUtils::getUserRoles).thenReturn(List.of("Admin"));

      assertEquals(
          PSFolderPermission.Access.ADMIN, helper.getFolderAccessLevel("1-101-8", true));
    }

    verify(contentWs).loadFolder(folderGuid, false);
    verify(contentWs, Mockito.never()).loadFolder(any(IPSGuid.class), eq(true));
  }

  @Test
  void liveRowKeepsTransientFolderLoad() throws Exception {
    when(folder.getAcl()).thenReturn(null);
    when(idMapper.getGuid("1-101-7")).thenReturn(folderGuid);
    when(contentWs.loadFolder(folderGuid, true)).thenReturn(folder);

    try (MockedStatic<PSWebserviceUtils> ws = Mockito.mockStatic(PSWebserviceUtils.class)) {
      ws.when(PSWebserviceUtils::getUserName).thenReturn("admin1");
      ws.when(PSWebserviceUtils::getUserRoles).thenReturn(List.of("Admin"));

      assertEquals(
          PSFolderPermission.Access.ADMIN, helper.getFolderAccessLevel("1-101-7"));
    }

    verify(contentWs).loadFolder(folderGuid, true);
  }

  @Test
  void setFolderAccessLevelOnRecycledFolderRowSkipsTransientLoad() throws Exception {
    PSPathItem row = row("//Folders/$System$/Recycling/Assets/qa-row", "Folder");
    row.setId("1-101-8");
    row.setLeaf(false);
    when(folder.getAcl()).thenReturn(null);
    when(idMapper.getGuid("1-101-8")).thenReturn(folderGuid);
    when(contentWs.loadFolder(folderGuid, false)).thenReturn(folder);

    try (MockedStatic<PSWebserviceUtils> ws = Mockito.mockStatic(PSWebserviceUtils.class)) {
      ws.when(PSWebserviceUtils::getUserName).thenReturn("admin1");
      ws.when(PSWebserviceUtils::getUserRoles).thenReturn(List.of("Admin"));

      helper.setFolderAccessLevel(row);
    }

    assertEquals(PSFolderPermission.Access.ADMIN, row.getAccessLevel());
    verify(contentWs).loadFolder(folderGuid, false);
  }

  private static PSPathItem row(String folderPath, String type) {
    return row(folderPath, type, null);
  }

  private static PSPathItem row(String folderPath, String type, String firstFolderPathsEntry) {
    PSPathItem item = new PSPathItem();
    item.setType(type);
    if (folderPath != null) {
      item.setFolderPath(folderPath);
    }
    if (firstFolderPathsEntry != null) {
      item.setFolderPaths(List.of(firstFolderPathsEntry));
    }
    return item;
  }
}
