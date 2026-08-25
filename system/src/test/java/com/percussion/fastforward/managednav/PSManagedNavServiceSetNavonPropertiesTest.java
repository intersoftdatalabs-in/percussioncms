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
package com.percussion.fastforward.managednav;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSCoreItem;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.content.IPSContentWs;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Architecture section rename must save navon displaytitle without
 * {@code releaseFromEdit}/check-in on sample workflows (#3797 / #3676).
 */
class PSManagedNavServiceSetNavonPropertiesTest {

  @Mock private IPSContentWs contentWs;
  @Mock private IPSContentDesignWs contentDsWs;
  @Mock private IPSAssemblyService asmService;
  @Mock private IPSGuidManager guidMgr;
  @Mock private IPSCmsObjectMgr cmsMgr;
  @Mock private PSCoreItem coreItem;

  private PSManagedNavService service;
  private IPSGuid navonId;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    service = spy(new PSManagedNavService(contentWs, contentDsWs, asmService, guidMgr, cmsMgr));
    navonId = new PSLegacyGuid(9001, 1);
  }

  @Test
  void setNavonPropertiesSavesItemsWhenAlreadyCheckedOut() throws Exception {
    doReturn(true).when(service).isNavonAlreadyCheckedOut(navonId);
    when(contentWs.loadItems(anyList(), eq(false), eq(false), eq(false), eq(false)))
        .thenReturn(List.of(coreItem));
    Map<String, String> map = new HashMap<>();
    map.put("displaytitle", "Renamed");

    service.setNavonProperties(navonId, map);

    verify(coreItem).setTextField("displaytitle", "Renamed");
    verify(contentWs).saveItems(anyList(), eq(false), eq(false));
    verify(contentWs, never()).prepareForEdit(anyList());
    verify(contentWs, never()).releaseFromEdit(anyList(), anyBoolean());
    verify(contentWs, never()).checkinItems(any(), any());
  }

  @Test
  void setNavonPropertiesSkipsPrepareOnSampleWorkflowNpeThenSaves() throws Exception {
    doReturn(false).when(service).isNavonAlreadyCheckedOut(navonId);
    when(contentWs.prepareForEdit(anyList())).thenThrow(new NullPointerException());
    when(contentWs.loadItems(anyList(), eq(false), eq(false), eq(false), eq(false)))
        .thenReturn(List.of(coreItem));
    Map<String, String> map = new HashMap<>();
    map.put("displaytitle", "Renamed");

    service.setNavonProperties(navonId, map);

    verify(contentWs).prepareForEdit(anyList());
    verify(coreItem).setTextField("displaytitle", "Renamed");
    verify(contentWs).saveItems(anyList(), eq(false), eq(false));
    verify(contentWs, never()).releaseFromEdit(anyList(), anyBoolean());
  }
}
