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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.cms.handlers.PSRelationshipCommandHandler;
import com.percussion.cms.objectstore.IPSFieldValue;
import com.percussion.cms.objectstore.PSCoreItem;
import com.percussion.cms.objectstore.PSFolder;
import com.percussion.cms.objectstore.PSItemDefinition;
import com.percussion.cms.objectstore.PSItemField;
import com.percussion.cms.objectstore.server.PSRelationshipProcessor;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.fastforward.managednav.IPSManagedNavService;
import com.percussion.itemmanagement.service.IPSWorkflowHelper;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.share.service.IPSDataItemSummaryService;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.system.IPSSystemWs;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Behavioral coverage for the {@code renameIfRequired} decomposition (issue #6367): loading
 * guards, no-op when the computed destination name matches, and numeric-suffix rename on conflict.
 */
@ExtendWith(MockitoExtension.class)
class PSRecycleServiceRenameTest {

  private static final String RECYCLED = PSRelationshipConfig.TYPE_RECYCLED_CONTENT;
  private static final String FOLDER = PSRelationshipConfig.TYPE_FOLDER_CONTENT;

  @Mock private IPSSystemWs systemWs;
  @Mock private IPSIdMapper idMapper;
  @Mock private IPSDataItemSummaryService itemSummaryService;
  @Mock private IPSContentWs contentWs;
  @Mock private IPSWorkflowHelper workflowHelper;
  @Mock private IPSManagedNavService navService;
  @Mock private IPSWidgetAssetRelationshipService widgetAssetRelationshipService;

  private PSRecycleService service;

  @BeforeEach
  void setUp() {
    service =
        new PSRecycleService(
            systemWs,
            idMapper,
            itemSummaryService,
            contentWs,
            workflowHelper,
            navService,
            widgetAssetRelationshipService);
  }

  @Test
  void renameSkippedWhenItemHasNoFolderPaths() throws Exception {
    IPSGuid guid = new PSLegacyGuid(100, 1);
    PSCoreItem item = mockCoreItem("page", Collections.emptyList(), 100);
    when(contentWs.loadItems(anyList(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
            anyBoolean(), anyString()))
        .thenReturn(List.of(item));

    invokeRename(guid, FOLDER, false);

    verify(contentWs, never()).saveItems(anyList(), anyBoolean(), anyBoolean(), eq((IPSGuid) null),
        anyString());
  }

  @Test
  void renameSkippedWhenFolderPathsNull() throws Exception {
    IPSGuid guid = new PSLegacyGuid(108, 1);
    PSCoreItem item = mockCoreItem("page", null, 108);
    when(contentWs.loadItems(anyList(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
            anyBoolean(), anyString()))
        .thenReturn(List.of(item));

    invokeRename(guid, FOLDER, false);

    verify(contentWs, never()).saveItems(anyList(), anyBoolean(), anyBoolean(), eq((IPSGuid) null),
        anyString());
  }

  @Test
  void renameSkippedWhenPathPrefixInvalid() throws Exception {
    IPSGuid guid = new PSLegacyGuid(101, 1);
    // mkd-code-review[paths.hardcoded_sep]: adjudicated intentional (issue #6367,
    // same as #6359) — intentionally-invalid CMS folder-path fixture asserting rejection, never file I/O.
    PSCoreItem item = mockCoreItem("page", List.of("/tmp/foo"), 101);
    when(contentWs.loadItems(anyList(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
            anyBoolean(), anyString()))
        .thenReturn(List.of(item));

    invokeRename(guid, FOLDER, false);

    verify(contentWs, never()).saveItems(anyList(), anyBoolean(), anyBoolean(), eq((IPSGuid) null),
        anyString());
  }

  @Test
  void renameSkippedWhenDestinationNameUnchanged() throws Exception {
    IPSGuid guid = new PSLegacyGuid(102, 1);
    // mkd-code-review[paths.hardcoded_sep]: adjudicated intentional (issue #6367,
    // same as #6359/#5898/#5910/#5927/#6325/#6358) — CMS logical repository path, not an OS path.
    PSCoreItem item = mockCoreItem("page", List.of("//Sites/home"), 102);
    when(contentWs.loadItems(anyList(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
            anyBoolean(), anyString()))
        .thenReturn(List.of(item));
    PSRelationshipProcessor processor = mock(PSRelationshipProcessor.class);
    when(processor.getIdByPath(anyString(), anyString(), anyString())).thenReturn(-1);

    try (MockedStatic<PSRelationshipProcessor> procStatic =
        mockStatic(PSRelationshipProcessor.class)) {
      procStatic.when(PSRelationshipProcessor::getInstance).thenReturn(processor);
      invokeRename(guid, FOLDER, false);
    }

    verify(contentWs, never()).prepareForEdit(guid);
    verify(contentWs, never()).saveItems(anyList(), anyBoolean(), anyBoolean(), eq((IPSGuid) null),
        anyString());
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void renameAppendsSuffixOnConflictAndSaves() throws Exception {
    IPSGuid guid = new PSLegacyGuid(103, 1);
    // mkd-code-review[paths.hardcoded_sep]: adjudicated intentional (issue #6367,
    // same as #6359/#5898/#5910/#5927/#6325/#6358) — CMS logical repository path, not an OS path.
    PSCoreItem loaded = mockCoreItem("page", List.of("//Sites/home"), 103);
    PSCoreItem reloaded = mock(PSCoreItem.class);
    when(contentWs.loadItems(anyList(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
            anyBoolean(), anyString()))
        .thenReturn(List.of(loaded));
    when(contentWs.loadItems(anyList(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean()))
        .thenReturn(List.of(reloaded));
    PSRelationshipProcessor processor = mock(PSRelationshipProcessor.class);
    // First lookup finds a conflicting id, second finds the suffixed path free.
    when(processor.getIdByPath(anyString(), anyString(), anyString())).thenReturn(999, -1);

    try (MockedStatic<PSRelationshipProcessor> procStatic =
        mockStatic(PSRelationshipProcessor.class)) {
      procStatic.when(PSRelationshipProcessor::getInstance).thenReturn(processor);
      invokeRename(guid, FOLDER, false);
    }

    verify(contentWs).prepareForEdit(guid);
    verify(reloaded).setTextField("sys_title", "page-1");
    ArgumentCaptor<List> itemsCaptor = ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
    verify(contentWs)
        .saveItems(itemsCaptor.capture(), eq(false), eq(false), eq((IPSGuid) null), typeCaptor.capture());
    assertEquals(List.of(reloaded), itemsCaptor.getValue());
    assertEquals(RECYCLED, typeCaptor.getValue());
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void renameFromRecyclingAppendsSuffixAndSwapsToFolderType() throws Exception {
    IPSGuid guid = new PSLegacyGuid(104, 1);
    // mkd-code-review[paths.hardcoded_sep]: adjudicated intentional (issue #6367,
    // same as #6359/#5898/#5910/#5927/#6325/#6358) — CMS logical repository path, not an OS path.
    PSCoreItem loaded =
        mockCoreItem("page", List.of("//Folders/$System$/Recycling/Sites/home"), 104);
    PSCoreItem reloaded = mock(PSCoreItem.class);
    when(contentWs.loadItems(anyList(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
            anyBoolean(), anyString()))
        .thenReturn(List.of(loaded));
    when(contentWs.loadItems(anyList(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean()))
        .thenReturn(List.of(reloaded));
    PSRelationshipProcessor processor = mock(PSRelationshipProcessor.class);
    when(processor.getIdByPath(anyString(), anyString(), anyString())).thenReturn(999, -1);

    try (MockedStatic<PSRelationshipProcessor> procStatic =
        mockStatic(PSRelationshipProcessor.class)) {
      procStatic.when(PSRelationshipProcessor::getInstance).thenReturn(processor);
      invokeRename(guid, RECYCLED, false);
    }

    verify(contentWs).prepareForEdit(guid);
    verify(reloaded).setTextField("sys_title", "page-1");
    ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
    verify(contentWs)
        .saveItems(anyList(), eq(false), eq(false), eq((IPSGuid) null), typeCaptor.capture());
    assertEquals(FOLDER, typeCaptor.getValue());
  }

  @Test
  void renameFolderSkipsReloadAndSavesFolder() throws Exception {
    IPSGuid guid = new PSLegacyGuid(105, 1);
    // mkd-code-review[paths.hardcoded_sep]: adjudicated intentional (issue #6367,
    // same as #6359/#5898/#5910/#5927/#6325/#6358) — CMS logical repository path, not an OS path.
    PSCoreItem loaded = mockCoreItem("page", List.of("//Sites/home"), 105);
    when(contentWs.loadItems(anyList(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
            anyBoolean(), anyString()))
        .thenReturn(List.of(loaded));
    PSRelationshipProcessor processor = mock(PSRelationshipProcessor.class);
    when(processor.getIdByPath(anyString(), anyString(), anyString())).thenReturn(999, -1);
    PSFolder folder = mock(PSFolder.class);
    when(contentWs.loadFolder(guid, false)).thenReturn(folder);
    com.percussion.design.objectstore.PSRelationshipConfig recycledConfig =
        mock(com.percussion.design.objectstore.PSRelationshipConfig.class);
    lenient().when(recycledConfig.getCategory()).thenReturn(
        com.percussion.design.objectstore.PSRelationshipConfig.CATEGORY_RECYCLED);

    try (MockedStatic<PSRelationshipProcessor> procStatic =
            mockStatic(PSRelationshipProcessor.class);
        MockedStatic<PSRelationshipCommandHandler> cmdStatic =
            mockStatic(PSRelationshipCommandHandler.class)) {
      procStatic.when(PSRelationshipProcessor::getInstance).thenReturn(processor);
      when(processor.getRelationshipList(
              any(com.percussion.cms.objectstore.PSRelationshipFilter.class)))
          .thenReturn(Collections.emptyList());
      cmdStatic
          .when(() -> PSRelationshipCommandHandler.getRelationshipConfig(RECYCLED))
          .thenReturn(recycledConfig);
      invokeRename(guid, RECYCLED, true);
    }

    verify(contentWs, never()).prepareForEdit(guid);
    verify(loaded).setTextField("sys_title", "page-1");
    verify(contentWs).loadFolder(guid, false);
    verify(contentWs).saveFolder(folder);
    verify(contentWs, never())
        .saveItems(anyList(), anyBoolean(), anyBoolean(), eq((IPSGuid) null), anyString());
  }

  @Test
  void renameMultipleConflictsReplacesSuffix() throws Exception {
    IPSGuid guid = new PSLegacyGuid(107, 1);
    // mkd-code-review[paths.hardcoded_sep]: adjudicated intentional (issue #6367,
    // same as #6359/#5898/#5910/#5927/#6325/#6358) — CMS logical repository path, not an OS path.
    PSCoreItem loaded = mockCoreItem("my-page", List.of("//Sites/home"), 107);
    PSCoreItem reloaded = mock(PSCoreItem.class);
    when(contentWs.loadItems(anyList(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
            anyBoolean(), anyString()))
        .thenReturn(List.of(loaded));
    when(contentWs.loadItems(anyList(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean()))
        .thenReturn(List.of(reloaded));
    PSRelationshipProcessor processor = mock(PSRelationshipProcessor.class);
    // Two conflicts before the path is free: exercises the counter > 1 suffix-replacement branch.
    when(processor.getIdByPath(anyString(), anyString(), anyString()))
        .thenReturn(999, 998, -1);

    try (MockedStatic<PSRelationshipProcessor> procStatic =
        mockStatic(PSRelationshipProcessor.class)) {
      procStatic.when(PSRelationshipProcessor::getInstance).thenReturn(processor);
      invokeRename(guid, FOLDER, false);
    }

    verify(reloaded).setTextField("sys_title", "my-page-2");
  }

  @Test
  void renameFileAssetUsesFullLoadOverload() throws Exception {    IPSGuid guid = new PSLegacyGuid(106, 1);
    // mkd-code-review[paths.hardcoded_sep]: adjudicated intentional (issue #6367,
    // same as #6359/#5898/#5910/#5927/#6325/#6358) — CMS logical repository path, not an OS path.
    PSCoreItem loaded = mockCoreItem("page", List.of("//Sites/home"), 106, "percFileAsset");
    PSCoreItem reloaded = mock(PSCoreItem.class);
    when(contentWs.loadItems(anyList(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
            anyBoolean(), anyString()))
        .thenReturn(List.of(loaded));
    when(contentWs.loadItems(anyList(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean()))
        .thenReturn(List.of(reloaded));
    PSRelationshipProcessor processor = mock(PSRelationshipProcessor.class);
    when(processor.getIdByPath(anyString(), anyString(), anyString())).thenReturn(999, -1);

    try (MockedStatic<PSRelationshipProcessor> procStatic =
        mockStatic(PSRelationshipProcessor.class)) {
      procStatic.when(PSRelationshipProcessor::getInstance).thenReturn(processor);
      invokeRename(guid, FOLDER, false);
    }

    verify(contentWs).loadItems(eq(List.of(guid)), eq(true), eq(true), eq(false), eq(false));
    verify(contentWs, never())
        .loadItems(eq(List.of(guid)), eq(false), eq(false), eq(false), eq(false));
  }

  private PSCoreItem mockCoreItem(String title, List<String> folderPaths, int contentId)
      throws Exception {
    return mockCoreItem(title, folderPaths, contentId, "percPage");
  }

  private PSCoreItem mockCoreItem(
      String title, List<String> folderPaths, int contentId, String definitionName)
      throws Exception {
    PSCoreItem item = mock(PSCoreItem.class);
    PSItemField field = mock(PSItemField.class);
    IPSFieldValue value = mock(IPSFieldValue.class);
    lenient().when(value.getValueAsString()).thenReturn(title);
    lenient().when(field.getValue()).thenReturn(value);
    lenient().when(item.getFieldByName("sys_title")).thenReturn(field);
    lenient().when(item.getFolderPaths()).thenReturn(folderPaths);
    lenient().when(item.getContentId()).thenReturn(contentId);
    PSItemDefinition def = mock(PSItemDefinition.class);
    lenient().when(def.getName()).thenReturn(definitionName);
    lenient().when(item.getItemDefinition()).thenReturn(def);
    return item;
  }

  private void invokeRename(IPSGuid guid, String relType, boolean folder) throws Exception {
    Method m =
        PSRecycleService.class.getDeclaredMethod(
            "renameIfRequired", IPSGuid.class, String.class, boolean.class);
    m.setAccessible(true);
    m.invoke(service, guid, relType, folder);
  }
}
