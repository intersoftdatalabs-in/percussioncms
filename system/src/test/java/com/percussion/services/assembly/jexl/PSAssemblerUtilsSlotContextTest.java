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

package com.percussion.services.assembly.jexl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.assembly.data.PSSlotLayoutStyles;
import com.percussion.services.assembly.data.PSTemplateSlot;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Unit tests for slot layout/styles JEXL helpers on {@link PSAssemblerUtils} (#2629). */
class PSAssemblerUtilsSlotContextTest {

  private final PSAssemblerUtils utils = new PSAssemblerUtils();

  @Test
  void slotLayoutAndStylesHelpers() {
    PSTemplateSlot slot = newSlot();
    Map<String, Object> layout = new LinkedHashMap<>();
    layout.put(PSSlotLayoutStyles.KEY_ORIENTATION, "horizontal");
    slot.setSlotLayout(layout);
    Map<String, Object> styles = new LinkedHashMap<>();
    styles.put(PSSlotLayoutStyles.KEY_ROOTCLASS, "jexl-root");
    slot.setSlotStyles(styles);

    assertEquals("horizontal", utils.slotLayout(slot).get(PSSlotLayoutStyles.KEY_ORIENTATION));
    assertEquals("jexl-root", utils.slotStyles(slot).get(PSSlotLayoutStyles.KEY_ROOTCLASS));
  }

  @Test
  void slotAssemblyContextMatchesBindingNames() {
    PSTemplateSlot slot = newSlot();
    Map<String, Object> styles = new LinkedHashMap<>();
    styles.put(PSSlotLayoutStyles.KEY_ROOTCLASS, "bind-root");
    slot.setSlotStyles(styles);

    Map<String, Object> sysSlot = utils.slotAssemblyContext(slot);

    // Documented bindings: $sys.slot.layout, $sys.slot.styles, $sys.slot.schemaVersion
    assertTrue(sysSlot.containsKey("layout"));
    assertTrue(sysSlot.containsKey("styles"));
    assertTrue(sysSlot.containsKey("schemaVersion"));
    assertTrue(sysSlot.containsKey("name"));

    @SuppressWarnings("unchecked")
    Map<String, Object> styleMap = (Map<String, Object>) sysSlot.get("styles");
    assertEquals("bind-root", styleMap.get(PSSlotLayoutStyles.KEY_ROOTCLASS));
    assertEquals(PSSlotLayoutStyles.SCHEMA_VERSION, sysSlot.get("schemaVersion"));
  }

  @Test
  void nullSlotRejected() {
    assertThrows(IllegalArgumentException.class, () -> utils.slotLayout(null));
    assertThrows(IllegalArgumentException.class, () -> utils.slotStyles(null));
    assertThrows(IllegalArgumentException.class, () -> utils.slotAssemblyContext(null));
  }

  private static PSTemplateSlot newSlot() {
    PSTemplateSlot slot = new PSTemplateSlot();
    slot.setGUID(new PSGuid(PSTypeEnum.SLOT, 9002));
    slot.setName("test.jexl.slot");
    slot.setLabel("test.jexl.slot");
    return slot;
  }
}
