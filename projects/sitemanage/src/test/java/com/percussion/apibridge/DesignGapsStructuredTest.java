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

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.DesignGap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** REST-GAPS-01: designGaps are structured {code,message} on CT / Template / Slot. */
@Tag("UnitTest")
class DesignGapsStructuredTest {

  @Test
  void contentTypeDesignGaps_includeCodesAndMessages() {
    List<DesignGap> gaps = ContentTypeAdaptor.contentTypeDesignGaps(true);
    assertFalse(gaps.isEmpty());
    Set<String> codes = gaps.stream().map(DesignGap::getCode).collect(Collectors.toSet());
    assertTrue(codes.contains("CT_FIELD_RULE_EXPR"));
    assertTrue(codes.contains("CT_ITEM_EXITS"));
    assertTrue(codes.contains("CT_CREATE_DELETE"));
    assertTrue(
        gaps.stream()
            .anyMatch(
                g ->
                    "CT_CREATE_DELETE".equals(g.getCode())
                        && g.getMessage().contains("POST /services/contenttypes")
                        && g.getMessage().contains("PUT /contenttypes/{idOrName}/name")
                        && !g.getMessage().startsWith("Create / delete not supported")),
        () -> gaps.toString());
    assertFalse(codes.contains("CT_CONTROL_RESOLUTION"));
    for (DesignGap g : gaps) {
      assertNotNull(g.getCode());
      assertFalse(g.getCode().isBlank());
      assertNotNull(g.getMessage());
      assertFalse(g.getMessage().isBlank());
    }
  }

  @Test
  void contentTypeDesignGaps_addsControlResolutionWhenUnresolved() {
    List<DesignGap> gaps = ContentTypeAdaptor.contentTypeDesignGaps(false);
    assertTrue(
        gaps.stream().anyMatch(g -> "CT_CONTROL_RESOLUTION".equals(g.getCode())),
        () -> gaps.toString());
  }

  @Test
  void templateDesignGaps_areStructured() {
    assertEquals(2, TemplateAdaptor.TEMPLATE_DESIGN_GAPS.size());
    DesignGap first = TemplateAdaptor.TEMPLATE_DESIGN_GAPS.get(0);
    assertEquals("TPL_LOCK", first.getCode());
    assertTrue(first.getMessage().contains("Lock"));
  }

  @Test
  void slotDesignGaps_areStructured() {
    assertEquals(2, SlotsAdaptor.SLOT_DESIGN_GAPS.size());
    DesignGap first = SlotsAdaptor.SLOT_DESIGN_GAPS.get(0);
    assertEquals("SLOT_CREATE_DELETE", first.getCode());
    assertTrue(first.getMessage().contains("Create"));
    assertEquals("SLOT_ASSOC_GUIDS_ONLY", SlotsAdaptor.SLOT_DESIGN_GAPS.get(1).getCode());
  }
}
