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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.fastforward.managednav.IPSManagedNavService;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.share.service.IPSDataService.DataServiceLoadException;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.content.IPSContentWs;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link PSItemSummaryService#find(String)} is on the Explorer Preview checkIn
 * path. A bare numeric content id must map as {@link PSTypeEnum#LEGACY_CONTENT}
 * (#3688).
 */
@ExtendWith(MockitoExtension.class)
class PSItemSummaryServiceNumericIdTest {

  @Mock private IPSGuidManager guidMgr;
  @Mock private IPSContentDesignWs contentDesignWs;
  @Mock private IPSContentWs contentWs;
  @Mock private IPSGuid contentGuid;

  @Test
  void findAcceptsBareNumericContentIdWithoutUntypedMakeGuid() throws DataServiceLoadException {
    when(guidMgr.makeGuid(594L, PSTypeEnum.LEGACY_CONTENT)).thenReturn(contentGuid);
    when(contentWs.findItems(anyList(), anyBoolean())).thenReturn(Collections.emptyList());

    var mapper = new PSIdMapper(guidMgr, contentDesignWs);
    var sut =
        new PSItemSummaryService(
            contentWs, mock(PSItemDefManager.class), mapper, mock(IPSManagedNavService.class));

    assertNull(sut.find("594"));

    verify(guidMgr).makeGuid(594L, PSTypeEnum.LEGACY_CONTENT);
    verify(guidMgr, never()).makeGuid(anyString());
  }

  @Test
  void findRetriesLegacyContentWhenGetGuidThrowsUndeterminedType() throws DataServiceLoadException {
    IPSIdMapper mapper = mock(IPSIdMapper.class);
    when(mapper.getGuid("594"))
        .thenThrow(
            new IllegalArgumentException("Type is undetermined, expecting \"type\" argument"));
    when(mapper.getGuidFromContentId(594L)).thenReturn(contentGuid);
    when(contentWs.findItems(anyList(), anyBoolean())).thenReturn(Collections.emptyList());

    var sut =
        new PSItemSummaryService(
            contentWs, mock(PSItemDefManager.class), mapper, mock(IPSManagedNavService.class));

    assertNull(sut.find("594"));

    verify(mapper).getGuidFromContentId(594L);
  }
}
