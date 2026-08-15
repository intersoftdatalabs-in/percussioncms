/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSCoreItem;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.dao.IPSRelationshipCataloger;
import com.percussion.share.service.IPSDataItemSummaryService;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.system.IPSSystemWs;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * After NavTree {@code saveItems}, Hibernate {@code loadBodies} can NPE
 * ({@code StatementPreparerImpl.connection()} null). Post-save {@code find()}
 * must not run — it marks the site-create transaction rollback-only (#3393).
 */
class PSContentItemDaoSaveAfterNavSeedTest {

  @Mock private IPSContentDesignWs contentDesignWs;
  @Mock private IPSContentWs contentWs;
  @Mock private IPSIdMapper idMapper;
  @Mock private IPSDataItemSummaryService itemSummaryService;
  @Mock private IPSFolderHelper folderHelper;
  @Mock private IPSCmsObjectMgr cmsObjectMgr;
  @Mock private IPSRelationshipCataloger relationshipHelper;
  @Mock private IPSSystemWs systemWs;
  @Mock private PSCoreItem coreItem;

  private PSContentItemDao dao;
  private IPSGuid savedGuid;
  private IPSGuid folderGuid;

  @BeforeEach
  void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
    dao =
        new PSContentItemDao(
            contentDesignWs,
            contentWs,
            idMapper,
            itemSummaryService,
            folderHelper,
            cmsObjectMgr,
            relationshipHelper,
            systemWs);
    savedGuid = new PSLegacyGuid(9001, 1);
    folderGuid = new PSLegacyGuid(501, 1);
    when(contentWs.createItems("percPageTemplate", 1)).thenReturn(List.of(coreItem));
    when(coreItem.getFieldByName(anyString())).thenReturn(null);
    when(contentWs.getIdByPath("//Sites/Bare/.system/Templates")).thenReturn(folderGuid);
    when(contentWs.saveItems(anyList(), eq(false), eq(false), eq(folderGuid)))
        .thenReturn(List.of(savedGuid));
    when(idMapper.getString(savedGuid)).thenReturn("guid-9001");
  }

  @Test
  void saveReturnsInMemoryItemWithoutPostSaveFind() throws Exception {
    PSContentItem item = new PSContentItem();
    item.setType("percPageTemplate");
    item.setFolderPaths(List.of("//Sites/Bare/.system/Templates"));
    item.getFields().put("sys_title", "BareTemplate");

    PSContentItem saved = dao.save(item);

    assertSame(item, saved);
    assertEquals("guid-9001", saved.getId());
    verify(contentWs).saveItems(anyList(), eq(false), eq(false), eq(folderGuid));
    verify(folderHelper).addItem("//Sites/Bare/.system/Templates", "guid-9001");
    verify(itemSummaryService, never()).find(anyString());
    verify(contentDesignWs, never()).findNodesByIds(anyList(), anyBoolean());
  }
}
