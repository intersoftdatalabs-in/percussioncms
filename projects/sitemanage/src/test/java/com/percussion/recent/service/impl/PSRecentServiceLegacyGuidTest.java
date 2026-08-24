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
package com.percussion.recent.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.utils.guid.IPSGuid;
import org.junit.jupiter.api.Test;

/** Assembly TEMPLATE guids must not be treated as percTemplate recent rows (#3728). */
class PSRecentServiceLegacyGuidTest {

  @Test
  void assemblyTemplateIsNotLegacyContent() {
    IPSIdMapper mapper = mock(IPSIdMapper.class);
    IPSGuid guid = mock(IPSGuid.class);
    when(mapper.getGuid("0-4-1050")).thenReturn(guid);
    when(guid.getType()).thenReturn(PSTypeEnum.TEMPLATE.getOrdinal());
    assertFalse(PSRecentService.isLegacyContentItemGuid("0-4-1050", mapper));
  }

  @Test
  void percTemplateContentIsLegacyContent() {
    IPSIdMapper mapper = mock(IPSIdMapper.class);
    IPSGuid guid = mock(IPSGuid.class);
    when(mapper.getGuid("1-101-705")).thenReturn(guid);
    when(guid.getType()).thenReturn(PSTypeEnum.LEGACY_CONTENT.getOrdinal());
    assertTrue(PSRecentService.isLegacyContentItemGuid("1-101-705", mapper));
  }

  @Test
  void blankOrUnparseableIsNotLegacyContent() {
    IPSIdMapper mapper = mock(IPSIdMapper.class);
    when(mapper.getGuid("bad")).thenThrow(new IllegalArgumentException("bad"));
    assertFalse(PSRecentService.isLegacyContentItemGuid(null, mapper));
    assertFalse(PSRecentService.isLegacyContentItemGuid(" ", mapper));
    assertFalse(PSRecentService.isLegacyContentItemGuid("bad", mapper));
    assertFalse(PSRecentService.isLegacyContentItemGuid("1-101-705", null));
  }
}
