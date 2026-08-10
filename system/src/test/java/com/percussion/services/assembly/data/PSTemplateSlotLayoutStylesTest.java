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

package com.percussion.services.assembly.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PSTemplateSlot} slot_layout / slot_styles persistence and assembly-context
 * visibility (Phase 2 #2629).
 */
class PSTemplateSlotLayoutStylesTest {

  @Test
  void defaultsOnNewSlot() {
    PSTemplateSlot slot = newSlot("test.layout.defaults");

    Map<String, Object> layout = slot.getSlotLayout();
    Map<String, Object> styles = slot.getSlotStyles();

    assertNotNull(layout);
    assertNotNull(styles);
    assertEquals(
        PSSlotLayoutStyles.SCHEMA_VERSION, layout.get(PSSlotLayoutStyles.KEY_SCHEMA_VERSION));
    assertEquals(
        PSSlotLayoutStyles.SCHEMA_VERSION, styles.get(PSSlotLayoutStyles.KEY_SCHEMA_VERSION));
    assertNull(slot.getSlotLayoutJson());
    assertNull(slot.getSlotStylesJson());
  }

  @Test
  void setGetLayoutAndStyles() {
    PSTemplateSlot slot = newSlot("test.layout.setget");

    Map<String, Object> layout = new LinkedHashMap<>();
    layout.put(PSSlotLayoutStyles.KEY_ORIENTATION, "vertical");
    layout.put(PSSlotLayoutStyles.KEY_MAX_ITEMS, "5");
    slot.setSlotLayout(layout);

    Map<String, Object> styles = new LinkedHashMap<>();
    styles.put(PSSlotLayoutStyles.KEY_ROOTCLASS, "my-root");
    slot.setSlotStyles(styles);

    assertEquals("vertical", slot.getSlotLayout().get(PSSlotLayoutStyles.KEY_ORIENTATION));
    assertEquals("5", slot.getSlotLayout().get(PSSlotLayoutStyles.KEY_MAX_ITEMS));
    assertEquals("my-root", slot.getSlotStyles().get(PSSlotLayoutStyles.KEY_ROOTCLASS));
    assertNotNull(slot.getSlotLayoutJson());
    assertNotNull(slot.getSlotStylesJson());
    assertTrue(slot.getSlotLayoutJson().contains("vertical"));
    assertTrue(slot.getSlotStylesJson().contains("my-root"));
  }

  @Test
  void clearLayoutRestoresDefaults() {
    PSTemplateSlot slot = newSlot("test.layout.clear");
    Map<String, Object> layout = new LinkedHashMap<>();
    layout.put(PSSlotLayoutStyles.KEY_COLUMNS, "3");
    slot.setSlotLayout(layout);
    assertNotNull(slot.getSlotLayoutJson());

    slot.setSlotLayout(null);
    assertNull(slot.getSlotLayoutJson());
    assertEquals(
        PSSlotLayoutStyles.SCHEMA_VERSION,
        slot.getSlotLayout().get(PSSlotLayoutStyles.KEY_SCHEMA_VERSION));
    assertFalse(slot.getSlotLayout().containsKey(PSSlotLayoutStyles.KEY_COLUMNS));
  }

  @Test
  void xmlRoundTripPreservesLayoutAndStyles() throws Exception {
    PSTemplateSlot slot = newSlot("test.layout.xml");
    Map<String, Object> layout = new LinkedHashMap<>();
    layout.put(PSSlotLayoutStyles.KEY_ORIENTATION, "horizontal");
    layout.put(PSSlotLayoutStyles.KEY_WRAPPER_CLASS_POLICY, "root");
    slot.setSlotLayout(layout);

    Map<String, Object> styles = new LinkedHashMap<>();
    styles.put(PSSlotLayoutStyles.KEY_ROOTCLASS, "xml-root");
    styles.put(PSSlotLayoutStyles.KEY_ITEMCLASS, "xml-item");
    slot.setSlotStyles(styles);

    String xml = slot.toXML();
    assertNotNull(xml);
    // Design XML stores layout/styles as JSON text elements (omitted when default)
    assertTrue(xml.contains("slot-layout"), "expected slot-layout in XML: " + xml);
    assertTrue(xml.contains("slot-styles"), "expected slot-styles in XML: " + xml);
    assertTrue(xml.contains("horizontal"), "expected layout payload in XML: " + xml);
    assertTrue(xml.contains("xml-root"), "expected styles payload in XML: " + xml);

    PSTemplateSlot copy = new PSTemplateSlot();
    copy.fromXML(xml);
    assertEquals(slot.getName(), copy.getName());
    assertEquals(
        "horizontal", copy.getSlotLayout().get(PSSlotLayoutStyles.KEY_ORIENTATION));
    assertEquals("xml-root", copy.getSlotStyles().get(PSSlotLayoutStyles.KEY_ROOTCLASS));
    assertEquals("xml-item", copy.getSlotStyles().get(PSSlotLayoutStyles.KEY_ITEMCLASS));
  }

  @Test
  void xmlOmitsDefaultLayoutAndStyles() throws Exception {
    PSTemplateSlot slot = newSlot("test.layout.defaults.xml");
    String xml = slot.toXML();
    assertFalse(xml.contains("slot-layout"), "defaults should omit slot-layout: " + xml);
    assertFalse(xml.contains("slot-styles"), "defaults should omit slot-styles: " + xml);
  }

  @Test
  void assemblyContextExposesLayoutStylesAndBindingKeys() {
    PSTemplateSlot slot = newSlot("test.layout.ctx");
    Map<String, Object> layout = new LinkedHashMap<>();
    layout.put(PSSlotLayoutStyles.KEY_EMPTY_STATE, "placeholder");
    slot.setSlotLayout(layout);
    Map<String, Object> styles = new LinkedHashMap<>();
    styles.put(PSSlotLayoutStyles.KEY_ROOTCLASS, "ctx-root");
    slot.setSlotStyles(styles);

    Map<String, Object> ctx = PSSlotLayoutStyles.toAssemblyContext(slot);

    assertTrue(ctx.containsKey(PSSlotLayoutStyles.CTX_LAYOUT));
    assertTrue(ctx.containsKey(PSSlotLayoutStyles.CTX_STYLES));
    assertTrue(ctx.containsKey(PSSlotLayoutStyles.CTX_SCHEMA_VERSION));
    assertTrue(ctx.containsKey(PSSlotLayoutStyles.CTX_NAME));

    @SuppressWarnings("unchecked")
    Map<String, Object> ctxLayout = (Map<String, Object>) ctx.get(PSSlotLayoutStyles.CTX_LAYOUT);
    @SuppressWarnings("unchecked")
    Map<String, Object> ctxStyles = (Map<String, Object>) ctx.get(PSSlotLayoutStyles.CTX_STYLES);

    assertEquals("placeholder", ctxLayout.get(PSSlotLayoutStyles.KEY_EMPTY_STATE));
    assertEquals("ctx-root", ctxStyles.get(PSSlotLayoutStyles.KEY_ROOTCLASS));
    assertEquals(PSSlotLayoutStyles.SCHEMA_VERSION, ctx.get(PSSlotLayoutStyles.CTX_SCHEMA_VERSION));
    assertEquals("test.layout.ctx", ctx.get(PSSlotLayoutStyles.CTX_NAME));

    // Binding path documentation: $sys.slot.layout / $sys.slot.styles
    assertEquals(ctxLayout, ((Map<?, ?>) ctx).get("layout"));
    assertEquals(ctxStyles, ((Map<?, ?>) ctx).get("styles"));
  }

  @Test
  void interfaceContractOnIpsTemplateSlot() {
    IPSTemplateSlot slot = newSlot("test.layout.iface");
    Map<String, Object> styles = new LinkedHashMap<>();
    styles.put(PSSlotLayoutStyles.KEY_ROOTCLASS, "iface");
    slot.setSlotStyles(styles);
    assertEquals("iface", slot.getSlotStyles().get(PSSlotLayoutStyles.KEY_ROOTCLASS));
    assertNotNull(slot.getSlotLayout());
  }

  @Test
  void setSlotLayoutJsonValidatesAndRejectsCorrupt() {
    PSTemplateSlot slot = newSlot("test.layout.rawjson");
    slot.setSlotLayoutJson("{\"orientation\":\"horizontal\"}");
    assertEquals("horizontal", slot.getSlotLayout().get(PSSlotLayoutStyles.KEY_ORIENTATION));
    slot.setSlotLayoutJson(null);
    assertNull(slot.getSlotLayoutJson());
    assertThrows(IllegalArgumentException.class, () -> slot.setSlotLayoutJson("{broken"));
    assertThrows(IllegalArgumentException.class, () -> slot.setSlotStylesJson("[1]"));
  }

  private static PSTemplateSlot newSlot(String name) {
    PSTemplateSlot slot = new PSTemplateSlot();
    slot.setGUID(new PSGuid(PSTypeEnum.SLOT, 9001));
    slot.setName(name);
    slot.setLabel(name);
    slot.setDescription("unit test slot");
    return slot;
  }
}
