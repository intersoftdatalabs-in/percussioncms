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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSInvalidContentTypeException;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.fastforward.managednav.IPSManagedNavService;
import com.percussion.services.content.data.PSItemSummary;
import com.percussion.services.content.data.PSItemSummary.ObjectTypeEnum;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentWs;
import java.util.Collections;
import org.junit.jupiter.api.Test;

/** Missing rffNavTree type 315 must not fail folder listing (#3410). */
class PSItemSummaryServiceNavTypeResilienceTest {

  @Test
  void isKnownContentTypeFalseWhenItemDefMissing() throws Exception {
    var itemDefManager = mock(PSItemDefManager.class);
    when(itemDefManager.contentTypeIdToName(315L))
        .thenThrow(new PSInvalidContentTypeException("315"));
    var sut = new PSItemSummaryService(mock(IPSContentWs.class), itemDefManager, mock(IPSIdMapper.class), mock(IPSManagedNavService.class));
    assertFalse(sut.isKnownContentType(315));
  }

  @Test
  void isKnownContentTypeTrueWhenHandlerRegistered() throws Exception {
    var itemDefManager = mock(PSItemDefManager.class);
    when(itemDefManager.contentTypeIdToName(101L)).thenReturn("Folder");
    var sut = new PSItemSummaryService(mock(IPSContentWs.class), itemDefManager, mock(IPSIdMapper.class), mock(IPSManagedNavService.class));
    assertTrue(sut.isKnownContentType(101));
  }

  @Test
  void getNavFolderTypeSkipsGetNavonPropertiesWhenNavTypeUnregistered() throws Exception {
    var contentWs = mock(IPSContentWs.class);
    var itemDefManager = mock(PSItemDefManager.class);
    var idMapper = mock(IPSIdMapper.class);
    var navService = mock(IPSManagedNavService.class);
    var sut = new PSItemSummaryService(contentWs, itemDefManager, idMapper, navService);

    var folder = mock(PSItemSummary.class);
    when(folder.getObjectType()).thenReturn(ObjectTypeEnum.FOLDER);
    when(folder.getName()).thenReturn("CorporateInvestments");
    var folderGuid = mock(IPSGuid.class);
    when(folder.getGUID()).thenReturn(folderGuid);
    var navGuid = mock(IPSGuid.class);
    when(navService.findNavigationIdFromFolder(any(IPSGuid.class), anyString())).thenReturn(navGuid);

    var navSum = mock(PSItemSummary.class);
    when(navSum.getContentTypeId()).thenReturn(315);
    when(contentWs.findItems(anyList(), anyBoolean())).thenReturn(Collections.singletonList(navSum));
    when(itemDefManager.contentTypeIdToName(315L))
        .thenThrow(new PSInvalidContentTypeException("315"));

    assertNull(sut.getNavFolderType(folder, "FolderContent"));
    verify(navService, never()).getNavonProperties(any(), anyList());
  }

  @Test
  void getNavFolderTypeReturnsNullWhenFindItemsByIdsWouldHaveFailed() {
    var contentWs = mock(IPSContentWs.class);
    var navService = mock(IPSManagedNavService.class);
    var sut = new PSItemSummaryService(contentWs, null, mock(IPSIdMapper.class), navService);

    var folder = mock(PSItemSummary.class);
    when(folder.getObjectType()).thenReturn(ObjectTypeEnum.FOLDER);
    when(folder.getName()).thenReturn("CorporateInvestments");
    var guid = mock(IPSGuid.class);
    when(folder.getGUID()).thenReturn(guid);
    when(navService.findNavigationIdFromFolder(any(IPSGuid.class), anyString())).thenReturn(guid);
    when(navService.getNavonProperties(any(), anyList()))
        .thenThrow(new RuntimeException("Failed to find items by IDs"));

    assertNull(sut.getNavFolderType(folder, "FolderContent"));
    verify(navService, never()).getNavonProperties(any(), anyList());
  }

  @Test
  void isKnownContentTypeFalseWhenManagerNull() {
    var sut =
        new PSItemSummaryService(
            mock(IPSContentWs.class), null, mock(IPSIdMapper.class), mock(IPSManagedNavService.class));
    assertFalse(sut.isKnownContentType(315));
  }
}
