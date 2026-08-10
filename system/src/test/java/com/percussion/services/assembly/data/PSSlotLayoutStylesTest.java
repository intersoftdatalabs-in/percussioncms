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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Unit tests for versioned slot_layout / slot_styles schema (ADR-003 / #2629). */
class PSSlotLayoutStylesTest {

  @Test
  void defaultsIncludeSchemaVersionOnly() {
    Map<String, Object> layout = PSSlotLayoutStyles.defaultLayout();
    Map<String, Object> styles = PSSlotLayoutStyles.defaultStyles();

    assertEquals(PSSlotLayoutStyles.SCHEMA_VERSION, layout.get(PSSlotLayoutStyles.KEY_SCHEMA_VERSION));
    assertEquals(PSSlotLayoutStyles.SCHEMA_VERSION, styles.get(PSSlotLayoutStyles.KEY_SCHEMA_VERSION));
    assertEquals(1, layout.size());
    assertEquals(1, styles.size());
  }

  @Test
  void parseNullOrBlankYieldsDefaults() {
    Map<String, Object> layout = PSSlotLayoutStyles.parseLayout(null);
    Map<String, Object> styles = PSSlotLayoutStyles.parseStyles("  ");

    assertEquals(PSSlotLayoutStyles.SCHEMA_VERSION, layout.get(PSSlotLayoutStyles.KEY_SCHEMA_VERSION));
    assertEquals(PSSlotLayoutStyles.SCHEMA_VERSION, styles.get(PSSlotLayoutStyles.KEY_SCHEMA_VERSION));
  }

  @Test
  void parseInvalidJsonYieldsDefaults() {
    Map<String, Object> layout = PSSlotLayoutStyles.parseLayout("{not-json");
    assertEquals(PSSlotLayoutStyles.SCHEMA_VERSION, layout.get(PSSlotLayoutStyles.KEY_SCHEMA_VERSION));
    assertEquals(1, layout.size());
  }

  @Test
  void encodeRoundTripPreservesKnownKeys() {
    Map<String, Object> layout = new LinkedHashMap<>();
    layout.put(PSSlotLayoutStyles.KEY_ORIENTATION, "horizontal");
    layout.put(PSSlotLayoutStyles.KEY_COLUMNS, "2");
    layout.put(PSSlotLayoutStyles.KEY_MAX_ITEMS, "10");
    layout.put(PSSlotLayoutStyles.KEY_EMPTY_STATE, "hide");

    String json = PSSlotLayoutStyles.encodeLayout(layout);
    assertNotNull(json);
    assertTrue(json.contains("horizontal"));

    Map<String, Object> parsed = PSSlotLayoutStyles.parseLayout(json);
    assertEquals("horizontal", parsed.get(PSSlotLayoutStyles.KEY_ORIENTATION));
    assertEquals("2", parsed.get(PSSlotLayoutStyles.KEY_COLUMNS));
    assertEquals("10", parsed.get(PSSlotLayoutStyles.KEY_MAX_ITEMS));
    assertEquals("hide", parsed.get(PSSlotLayoutStyles.KEY_EMPTY_STATE));
    assertEquals(
        PSSlotLayoutStyles.SCHEMA_VERSION, parsed.get(PSSlotLayoutStyles.KEY_SCHEMA_VERSION));
  }

  @Test
  void encodeStylesRoundTripRootclass() {
    Map<String, Object> styles = new LinkedHashMap<>();
    styles.put(PSSlotLayoutStyles.KEY_ROOTCLASS, "perc-widget");
    styles.put(PSSlotLayoutStyles.KEY_ITEMCLASS, "perc-item");

    String json = PSSlotLayoutStyles.encodeStyles(styles);
    Map<String, Object> parsed = PSSlotLayoutStyles.parseStyles(json);
    assertEquals("perc-widget", parsed.get(PSSlotLayoutStyles.KEY_ROOTCLASS));
    assertEquals("perc-item", parsed.get(PSSlotLayoutStyles.KEY_ITEMCLASS));
  }

  @Test
  void encodeDefaultsOnlyStoresNull() {
    assertNull(PSSlotLayoutStyles.encodeLayout(PSSlotLayoutStyles.defaultLayout()));
    assertNull(PSSlotLayoutStyles.encodeStyles(null));
    assertNull(PSSlotLayoutStyles.encodeStyles(Map.of()));
  }

  @Test
  void schemaVersionOfHandlesNumberAndString() {
    assertEquals(PSSlotLayoutStyles.SCHEMA_VERSION, PSSlotLayoutStyles.schemaVersionOf(null));
    assertEquals(2, PSSlotLayoutStyles.schemaVersionOf(Map.of(PSSlotLayoutStyles.KEY_SCHEMA_VERSION, 2)));
    assertEquals(
        3, PSSlotLayoutStyles.schemaVersionOf(Map.of(PSSlotLayoutStyles.KEY_SCHEMA_VERSION, "3")));
  }

  @Test
  void validateStoredJsonAcceptsBlankAndObject() {
    assertDoesNotThrow(() -> PSSlotLayoutStyles.validateStoredJson(null));
    assertDoesNotThrow(() -> PSSlotLayoutStyles.validateStoredJson("  "));
    assertDoesNotThrow(() -> PSSlotLayoutStyles.validateStoredJson("{\"rootclass\":\"x\"}"));
  }

  @Test
  void validateStoredJsonRejectsNonObject() {
    assertThrows(
        IllegalArgumentException.class, () -> PSSlotLayoutStyles.validateStoredJson("{not-json"));
    assertThrows(
        IllegalArgumentException.class, () -> PSSlotLayoutStyles.validateStoredJson("[1,2]"));
    assertThrows(
        IllegalArgumentException.class, () -> PSSlotLayoutStyles.validateStoredJson("\"string\""));
  }

  @Test
  void encodeSelfReferentialMapThrowsIllegalState() {
    Map<String, Object> layout = new LinkedHashMap<>();
    layout.put(PSSlotLayoutStyles.KEY_ORIENTATION, "horizontal");
    Map<String, Object> cycle = new LinkedHashMap<>();
    cycle.put("self", cycle);
    layout.put("cycle", cycle);
    // Jackson rejects direct self-references; encode must not silently return null.
    assertThrows(IllegalStateException.class, () -> PSSlotLayoutStyles.encodeLayout(layout));
  }

  @Test
  void mergeInstanceOverrideWinsOverDefinition() {
    Map<String, Object> definition = new LinkedHashMap<>();
    definition.put(PSSlotLayoutStyles.KEY_ORIENTATION, "vertical");
    definition.put(PSSlotLayoutStyles.KEY_COLUMNS, "2");
    definition.put(PSSlotLayoutStyles.KEY_MAX_ITEMS, "5");

    Map<String, Object> instance = new LinkedHashMap<>();
    instance.put(PSSlotLayoutStyles.KEY_ORIENTATION, "horizontal");
    instance.put(PSSlotLayoutStyles.KEY_MAX_ITEMS, "10");

    Map<String, Object> effective = PSSlotLayoutStyles.merge(definition, instance, true);
    assertEquals("horizontal", effective.get(PSSlotLayoutStyles.KEY_ORIENTATION));
    assertEquals("2", effective.get(PSSlotLayoutStyles.KEY_COLUMNS));
    assertEquals("10", effective.get(PSSlotLayoutStyles.KEY_MAX_ITEMS));
    assertEquals(
        PSSlotLayoutStyles.SCHEMA_VERSION, effective.get(PSSlotLayoutStyles.KEY_SCHEMA_VERSION));
  }

  @Test
  void mergeClearOverrideRestoresDefinitionDefault() {
    Map<String, Object> definition = new LinkedHashMap<>();
    definition.put(PSSlotLayoutStyles.KEY_ROOTCLASS, "def-root");
    definition.put(PSSlotLayoutStyles.KEY_ITEMCLASS, "def-item");

    Map<String, Object> instance = new LinkedHashMap<>();
    instance.put(PSSlotLayoutStyles.KEY_ROOTCLASS, "inst-root");

    Map<String, Object> withOverride = PSSlotLayoutStyles.merge(definition, instance, false);
    assertEquals("inst-root", withOverride.get(PSSlotLayoutStyles.KEY_ROOTCLASS));
    assertEquals("def-item", withOverride.get(PSSlotLayoutStyles.KEY_ITEMCLASS));

    PSSlotLayoutStyles.clearOverride(instance, PSSlotLayoutStyles.KEY_ROOTCLASS);
    Map<String, Object> afterClear = PSSlotLayoutStyles.merge(definition, instance, false);
    assertEquals("def-root", afterClear.get(PSSlotLayoutStyles.KEY_ROOTCLASS));
    assertEquals("def-item", afterClear.get(PSSlotLayoutStyles.KEY_ITEMCLASS));
  }

  @Test
  void mergeNullOrEmptyOverridesReturnsDefinition() {
    Map<String, Object> definition = new LinkedHashMap<>();
    definition.put(PSSlotLayoutStyles.KEY_ORIENTATION, "vertical");
    Map<String, Object> fromNull = PSSlotLayoutStyles.merge(definition, null, true);
    Map<String, Object> fromEmpty = PSSlotLayoutStyles.merge(definition, Map.of(), true);
    assertEquals("vertical", fromNull.get(PSSlotLayoutStyles.KEY_ORIENTATION));
    assertEquals("vertical", fromEmpty.get(PSSlotLayoutStyles.KEY_ORIENTATION));
  }

  @Test
  void instanceOverrideRoundTripEncodeParse() {
    Map<String, Object> overrides = new LinkedHashMap<>();
    overrides.put(PSSlotLayoutStyles.KEY_ROOTCLASS, "page-root");
    overrides.put(PSSlotLayoutStyles.KEY_ORIENTATION, "horizontal");

    String layoutJson = PSSlotLayoutStyles.encodeOverrides(overrides, true);
    String stylesJson = PSSlotLayoutStyles.encodeOverrides(overrides, false);
    assertNotNull(layoutJson);
    assertNotNull(stylesJson);

    Map<String, Object> parsedLayout = PSSlotLayoutStyles.parseOverrides(layoutJson);
    Map<String, Object> parsedStyles = PSSlotLayoutStyles.parseOverrides(stylesJson);
    assertEquals("horizontal", parsedLayout.get(PSSlotLayoutStyles.KEY_ORIENTATION));
    assertEquals("page-root", parsedStyles.get(PSSlotLayoutStyles.KEY_ROOTCLASS));

    Map<String, Object> defLayout = new LinkedHashMap<>();
    defLayout.put(PSSlotLayoutStyles.KEY_ORIENTATION, "vertical");
    defLayout.put(PSSlotLayoutStyles.KEY_COLUMNS, "3");
    Map<String, Object> effective = PSSlotLayoutStyles.merge(defLayout, parsedLayout, true);
    assertEquals("horizontal", effective.get(PSSlotLayoutStyles.KEY_ORIENTATION));
    assertEquals("3", effective.get(PSSlotLayoutStyles.KEY_COLUMNS));

    assertNull(PSSlotLayoutStyles.encodeOverrides(null, true));
    assertNull(PSSlotLayoutStyles.encodeOverrides(Map.of(), false));
    assertTrue(PSSlotLayoutStyles.parseOverrides(null).isEmpty());
    assertTrue(PSSlotLayoutStyles.parseOverrides("  ").isEmpty());
  }

  @Test
  void toAssemblyContextAppliesInstanceOverrides() {
    PSTemplateSlot slot = new PSTemplateSlot();
    slot.setName("testSlot");
    Map<String, Object> layout = new LinkedHashMap<>();
    layout.put(PSSlotLayoutStyles.KEY_ORIENTATION, "vertical");
    slot.setSlotLayout(layout);
    Map<String, Object> styles = new LinkedHashMap<>();
    styles.put(PSSlotLayoutStyles.KEY_ROOTCLASS, "def-root");
    slot.setSlotStyles(styles);

    Map<String, Object> layoutOv = Map.of(PSSlotLayoutStyles.KEY_ORIENTATION, "horizontal");
    Map<String, Object> stylesOv = Map.of(PSSlotLayoutStyles.KEY_ROOTCLASS, "inst-root");
    Map<String, Object> ctx = PSSlotLayoutStyles.toAssemblyContext(slot, layoutOv, stylesOv);

    @SuppressWarnings("unchecked")
    Map<String, Object> ctxLayout =
        (Map<String, Object>) ctx.get(PSSlotLayoutStyles.CTX_LAYOUT);
    @SuppressWarnings("unchecked")
    Map<String, Object> ctxStyles =
        (Map<String, Object>) ctx.get(PSSlotLayoutStyles.CTX_STYLES);
    assertEquals("horizontal", ctxLayout.get(PSSlotLayoutStyles.KEY_ORIENTATION));
    assertEquals("inst-root", ctxStyles.get(PSSlotLayoutStyles.KEY_ROOTCLASS));
    assertEquals("testSlot", ctx.get(PSSlotLayoutStyles.CTX_NAME));
  }
}
