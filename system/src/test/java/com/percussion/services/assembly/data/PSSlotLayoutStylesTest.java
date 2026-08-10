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
}
