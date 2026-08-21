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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentDesignWs;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Explorer Preview {@code GET .../workflow/checkIn/594} sends a bare content id.
 * {@link PSIdMapper#getGuid(String)} must use {@link PSTypeEnum#LEGACY_CONTENT}
 * instead of untyped {@code makeGuid(String)} (#3688).
 */
@ExtendWith(MockitoExtension.class)
class PSIdMapperNumericContentIdTest {

  private static final long FASTFORWARD_CONTENT_ID = 594L;

  @Mock private IPSGuidManager guidMgr;
  @Mock private IPSContentDesignWs contentDesignWs;
  @Mock private IPSGuid contentGuid;
  @Mock private IPSGuid hyphenatedGuid;
  @Mock private IPSGuid packedGuid;
  @Mock private IPSGuid itemGuid;

  private PSIdMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new PSIdMapper(guidMgr, contentDesignWs);
  }

  @Test
  void parseBareNumericContentIdRecognizesFastForwardSample() {
    assertEquals(FASTFORWARD_CONTENT_ID, PSIdMapper.parseBareNumericContentId("594"));
    assertEquals(FASTFORWARD_CONTENT_ID, PSIdMapper.parseBareNumericContentId(" 594 "));
    assertNull(PSIdMapper.parseBareNumericContentId("0-101-594"));
    assertNull(PSIdMapper.parseBareNumericContentId("host-type-uuid"));
    assertNull(PSIdMapper.parseBareNumericContentId(""));
  }

  @Test
  void parseBareNumericContentIdIgnoresPackedLongWithTypeBits() {
    long packed = ((long) PSTypeEnum.LEGACY_CONTENT.getOrdinal() << 32) | FASTFORWARD_CONTENT_ID;
    assertNull(PSIdMapper.parseBareNumericContentId(Long.toString(packed)));
  }

  @Test
  void getGuidMapsBareNumericToLegacyContent() {
    when(guidMgr.makeGuid(FASTFORWARD_CONTENT_ID, PSTypeEnum.LEGACY_CONTENT))
        .thenReturn(contentGuid);

    assertSame(contentGuid, mapper.getGuid("594"));

    verify(guidMgr).makeGuid(FASTFORWARD_CONTENT_ID, PSTypeEnum.LEGACY_CONTENT);
    verify(guidMgr, never()).makeGuid(anyString());
  }

  @Test
  void getGuidLeavesHyphenatedGuidUntypedMakeGuid() {
    when(guidMgr.makeGuid("0-101-594")).thenReturn(hyphenatedGuid);

    assertSame(hyphenatedGuid, mapper.getGuid("0-101-594"));

    verify(guidMgr).makeGuid("0-101-594");
    verify(guidMgr, never()).makeGuid(anyLong(), eq(PSTypeEnum.LEGACY_CONTENT));
  }

  @Test
  void getGuidLeavesPackedLongWithTypeBitsUntypedMakeGuid() {
    long packed = ((long) PSTypeEnum.LEGACY_CONTENT.getOrdinal() << 32) | FASTFORWARD_CONTENT_ID;
    String packedText = Long.toString(packed);
    when(guidMgr.makeGuid(packedText)).thenReturn(packedGuid);

    assertSame(packedGuid, mapper.getGuid(packedText));

    verify(guidMgr).makeGuid(packedText);
    verify(guidMgr, never()).makeGuid(anyLong(), eq(PSTypeEnum.LEGACY_CONTENT));
  }

  @Test
  void getGuidsMapsBareNumericContentId() {
    when(guidMgr.makeGuid(FASTFORWARD_CONTENT_ID, PSTypeEnum.LEGACY_CONTENT))
        .thenReturn(contentGuid);

    assertEquals(List.of(contentGuid), mapper.getGuids(List.of("594")));

    verify(guidMgr).makeGuid(FASTFORWARD_CONTENT_ID, PSTypeEnum.LEGACY_CONTENT);
  }

  @Test
  void getItemGuidUsesLegacyContentForBareNumeric() {
    when(guidMgr.makeGuid(FASTFORWARD_CONTENT_ID, PSTypeEnum.LEGACY_CONTENT))
        .thenReturn(contentGuid);
    when(contentDesignWs.getItemGuid(contentGuid)).thenReturn(itemGuid);

    assertSame(itemGuid, mapper.getItemGuid("594"));
  }

  @Test
  void getContentIdUsesLegacyContentForBareNumeric() {
    var legacy = new PSLegacyGuid((int) FASTFORWARD_CONTENT_ID, -1);
    when(guidMgr.makeGuid(FASTFORWARD_CONTENT_ID, PSTypeEnum.LEGACY_CONTENT)).thenReturn(legacy);

    assertEquals((int) FASTFORWARD_CONTENT_ID, mapper.getContentId("594"));
  }

  @Test
  void getGuidRejectsBlank() {
    assertThrows(IllegalArgumentException.class, () -> mapper.getGuid(""));
  }
}
