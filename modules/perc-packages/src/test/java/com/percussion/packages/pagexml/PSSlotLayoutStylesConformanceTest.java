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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.packages.pagexml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for slot {@code slot_layout} / {@code slot_styles} conformance of one slot (issue
 * #7306 / parent #2626).
 */
class PSSlotLayoutStylesConformanceTest {

  @Test
  void cleanSlotWithKnownKeysAndScalarStylesIsConformant() {
    Map<String, Object> layout = new LinkedHashMap<>();
    layout.put("orientation", "horizontal");
    layout.put("columns", 3);
    layout.put("maxItems", 10);
    layout.put("schemaVersion", 1);
    Map<String, Object> styles = new LinkedHashMap<>();
    styles.put("rootclass", "perc-region");
    styles.put("itemclass", "perc-item");
    styles.put("summaryclass", "custom preserved key");
    styles.put("schemaVersion", 1);

    PSSlotLayoutStylesConformance.Result result =
        PSSlotLayoutStylesConformance.check("main", layout, styles);

    assertTrue(result.isConformant());
    assertEquals("main", result.getSlotName());
    // document order preserved — the helper never sorts
    assertEquals(
        Arrays.asList("orientation", "columns", "maxItems", "schemaVersion"),
        result.getLayoutKeysInOrder());
    assertEquals(
        Arrays.asList("rootclass", "itemclass", "summaryclass", "schemaVersion"),
        result.getStylesKeysInOrder());
    assertTrue(result.getUnknownLayoutKeys().isEmpty());
    assertTrue(result.getNonScalarStyleKeys().isEmpty());
    assertFalse(result.isLayoutEmpty());
    assertFalse(result.isStylesEmpty());
    assertTrue(result.summarize().contains("conformant"));
  }

  @Test
  void regionMapperOutputShapeIsConformant() {
    // Shape produced by PSPageXmlParser.applyClassHints + PSPageXmlCompiler for a
    // class="perc-vertical" region container.
    Map<String, Object> layout = new LinkedHashMap<>();
    layout.put("orientation", "vertical");
    layout.put("vspan", "2");
    Map<String, Object> styles = new LinkedHashMap<>();
    styles.put("rootclass", "perc-vertical");

    PSSlotLayoutStylesConformance.Result result =
        PSSlotLayoutStylesConformance.check("sidebar", layout, styles);

    assertTrue(result.isConformant());
    assertTrue(result.getUnknownLayoutKeys().isEmpty());
    assertTrue(result.getNonScalarStyleKeys().isEmpty());
  }

  @Test
  void unknownLayoutKeysReportedInEncounterOrder() {
    Map<String, Object> layout = new LinkedHashMap<>();
    layout.put("orientaton", "horizontal");
    layout.put("orientation", "horizontal");
    layout.put("columns ", 2);
    layout.put("max_items", 5);

    PSSlotLayoutStylesConformance.Result result =
        PSSlotLayoutStylesConformance.check("typo", layout, Collections.emptyMap());

    assertFalse(result.isConformant());
    // encounter order preserved — the helper never sorts
    assertEquals(
        Arrays.asList("orientaton", "columns ", "max_items"), result.getUnknownLayoutKeys());
    assertTrue(result.getNonScalarStyleKeys().isEmpty());
    assertTrue(result.summarize().contains("NOT conformant"));
    assertTrue(result.summarize().contains("orientaton"));
  }

  @Test
  void nonScalarStyleValuesReportedInEncounterOrder() {
    Map<String, Object> styles = new LinkedHashMap<>();
    styles.put("rootclass", "perc-region");
    styles.put("tokens", Arrays.asList("a", "b"));
    styles.put("itemclass", null);
    styles.put("nested", Collections.singletonMap("k", "v"));
    styles.put("breakpoints", new String[] {"sm", "lg"});

    PSSlotLayoutStylesConformance.Result result =
        PSSlotLayoutStylesConformance.check("bad-values", Collections.emptyMap(), styles);

    assertFalse(result.isConformant());
    assertEquals(
        Arrays.asList("tokens", "itemclass", "nested", "breakpoints"),
        result.getNonScalarStyleKeys());
    assertTrue(result.getUnknownLayoutKeys().isEmpty());
    assertTrue(result.summarize().contains("NOT conformant"));
    assertTrue(result.summarize().contains("tokens"));
  }

  @Test
  void booleanAndCharacterStyleValuesAreScalar() {
    Map<String, Object> styles = new LinkedHashMap<>();
    styles.put("visible", Boolean.TRUE);
    styles.put("marker", 'x');

    PSSlotLayoutStylesConformance.Result result =
        PSSlotLayoutStylesConformance.check("scalars", Collections.emptyMap(), styles);

    assertTrue(result.isConformant());
    assertTrue(result.getNonScalarStyleKeys().isEmpty());
  }

  @Test
  void emptyMapsAreInformationalOnly() {
    PSSlotLayoutStylesConformance.Result result =
        PSSlotLayoutStylesConformance.check(
            "lost-layout", Collections.emptyMap(), Collections.emptyMap());

    assertTrue(result.isConformant());
    assertTrue(result.isLayoutEmpty());
    assertTrue(result.isStylesEmpty());
    assertTrue(result.summarize().contains("conformant"));
    assertTrue(result.summarize().contains("layout empty"));
    assertTrue(result.summarize().contains("styles empty"));
  }

  @Test
  void nullMapsAreVacuouslyConformant() {
    PSSlotLayoutStylesConformance.Result result =
        PSSlotLayoutStylesConformance.check("nulls", null, null);

    assertTrue(result.isConformant());
    assertTrue(result.isLayoutEmpty());
    assertTrue(result.isStylesEmpty());
    assertTrue(result.getLayoutKeysInOrder().isEmpty());
    assertTrue(result.getStylesKeysInOrder().isEmpty());
    assertTrue(result.getUnknownLayoutKeys().isEmpty());
    assertTrue(result.getNonScalarStyleKeys().isEmpty());
  }

  @Test
  void schemaOnlyMapsMatchDefaultsShape() {
    // Shape of PSSlotLayoutStyles.defaultLayout()/defaultStyles(): version only, no overrides.
    Map<String, Object> layout = new LinkedHashMap<>();
    layout.put("schemaVersion", 1);
    Map<String, Object> styles = new LinkedHashMap<>();
    styles.put("schemaVersion", 1);

    PSSlotLayoutStylesConformance.Result result =
        PSSlotLayoutStylesConformance.check("defaults", layout, styles);

    assertTrue(result.isConformant());
    assertFalse(result.isLayoutEmpty());
    assertFalse(result.isStylesEmpty());
  }

  @Test
  void resultListsAreImmutable() {
    Map<String, Object> layout = new LinkedHashMap<>();
    layout.put("bogus", 1);

    PSSlotLayoutStylesConformance.Result result =
        PSSlotLayoutStylesConformance.check("frozen", layout, Collections.emptyMap());

    assertEquals(Collections.singletonList("bogus"), result.getUnknownLayoutKeys());
    assertThrows(
        UnsupportedOperationException.class, () -> result.getUnknownLayoutKeys().add("mutated"));
    assertThrows(
        UnsupportedOperationException.class,
        () -> result.getLayoutKeysInOrder().add("mutated"));
  }
}
