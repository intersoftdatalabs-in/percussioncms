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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSInvalidContentTypeException;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.fastforward.managednav.IPSManagedNavService;
import com.percussion.services.content.data.PSItemSummary;
import com.percussion.services.content.data.PSItemSummary.ObjectTypeEnum;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.share.data.IPSItemSummary.Category;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentWs;
import java.util.Collections;
import org.junit.jupiter.api.Test;

/**
 * The Recycling root has no folder-content parent path, so {@code findFolderPaths} returns an
 * empty array for it. Indexing {@code [0]} threw {@code IndexOutOfBoundsException}: the child
 * was skipped with a WARN in listings and {@code find(recyclingId)} escalated to an ERROR
 * {@code Failed to load} on every /Recycling tree navigation (#6549).
 */
class PSItemSummaryServiceRecyclingResilienceTest {

  private static final String RECYCLING_ID = "16777215-101-8";

  @Test
  void findSummarizesRecyclingFolderWithNoParentPaths() throws Exception {
    var ctx = recyclingContext();
    when(ctx.contentWs.findFolderPaths(ctx.guid, "FolderContent")).thenReturn(new String[0]);

    var result = ctx.sut.find(RECYCLING_ID);

    assertNotNull(result);
    assertEquals(Category.FOLDER, result.getCategory());
  }

  @Test
  void findFolderChildrenIncludesPathlessRecyclingChild() throws Exception {
    var ctx = recyclingContext();
    when(ctx.contentWs.findFolderPaths(ctx.guid, "FolderContent")).thenReturn(new String[0]);
    when(ctx.contentWs.findFolderChildren(any(IPSGuid.class), anyBoolean()))
        .thenReturn(Collections.singletonList(ctx.recycling));

    var children = ctx.sut.findFolderChildren(RECYCLING_ID);

    assertEquals(1, children.size());
    assertEquals(Category.FOLDER, children.get(0).getCategory());
  }

  @Test
  void findReturnsNullWhenSingleChildCannotBeSummarized() throws Exception {
    var contentWs = mock(IPSContentWs.class);
    var itemDefManager = mock(PSItemDefManager.class);
    var idMapper = mock(IPSIdMapper.class);
    var navService = mock(IPSManagedNavService.class);
    var sut = new PSItemSummaryService(contentWs, itemDefManager, idMapper, navService);

    var guid = mock(PSLegacyGuid.class);
    when(idMapper.getGuid(RECYCLING_ID)).thenReturn(guid);

    var unreadable = mock(PSItemSummary.class);
    when(unreadable.getGUID()).thenReturn(guid);
    when(unreadable.getContentTypeId()).thenReturn(315);
    when(unreadable.getContentTypeName()).thenReturn("Folder");
    when(unreadable.getName()).thenReturn("Untyped");
    when(contentWs.findItems(anyList(), anyBoolean()))
        .thenReturn(Collections.singletonList(unreadable));
    when(itemDefManager.contentTypeIdToName(315L))
        .thenThrow(new PSInvalidContentTypeException("315"));
    when(navService.getNavonContentTypeIds()).thenReturn(Collections.emptyList());
    when(navService.getNavTreeContentTypeIds()).thenReturn(Collections.emptyList());

    assertNull(sut.find(RECYCLING_ID));
  }

  /** Mocks a known-type Recycling folder summary resolvable under the H2 cell id. */
  private static RecyclingContext recyclingContext() throws Exception {
    var contentWs = mock(IPSContentWs.class);
    var itemDefManager = mock(PSItemDefManager.class);
    var idMapper = mock(IPSIdMapper.class);
    var navService = mock(IPSManagedNavService.class);
    var sut = new PSItemSummaryService(contentWs, itemDefManager, idMapper, navService);

    var guid = mock(PSLegacyGuid.class);
    when(guid.getContentId()).thenReturn(8);
    when(idMapper.getGuid(RECYCLING_ID)).thenReturn(guid);
    when(idMapper.getString(any(IPSGuid.class))).thenReturn(RECYCLING_ID);

    var recycling = mock(PSItemSummary.class);
    when(recycling.getGUID()).thenReturn(guid);
    when(recycling.getName()).thenReturn("Recycling");
    when(recycling.getObjectType()).thenReturn(ObjectTypeEnum.FOLDER);
    when(recycling.getContentTypeId()).thenReturn(101);
    when(recycling.getContentTypeName()).thenReturn("Folder");
    when(recycling.isRevisionLock()).thenReturn(false);
    when(contentWs.findItems(anyList(), anyBoolean()))
        .thenReturn(Collections.singletonList(recycling));

    when(itemDefManager.contentTypeIdToName(101L)).thenReturn("Folder");
    when(itemDefManager.contentTypeNameToId("Folder")).thenReturn(101L);
    when(itemDefManager.contentTypeIdToLabel(101L)).thenReturn("Folder");
    when(navService.getNavonContentTypeIds()).thenReturn(Collections.emptyList());
    when(navService.getNavTreeContentTypeIds()).thenReturn(Collections.emptyList());
    when(navService.findNavigationIdFromFolder(any(IPSGuid.class), anyString())).thenReturn(null);

    var ctx = new RecyclingContext();
    ctx.sut = sut;
    ctx.contentWs = contentWs;
    ctx.guid = guid;
    ctx.recycling = recycling;
    return ctx;
  }

  private static class RecyclingContext {
    PSItemSummaryService sut;
    IPSContentWs contentWs;
    PSLegacyGuid guid;
    PSItemSummary recycling;
  }
}
