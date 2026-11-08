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
package com.percussion.webui.gadget.servlets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import org.junit.Before;
import org.junit.Test;

/**
 * CI-assertable modern-only load selection <strong>metrics evidence harness</strong> for {@link
 * GadgetRegistry} (#3131 / parent #2630; #5810 retired the legacy fallback).
 *
 * <p>Drives modern and none paths and asserts cumulative counters, snapshot map, and ops summary
 * string. Metric keys and log format are unchanged from the dual-load era for QA telemetry
 * compatibility; the {@code legacyRegistryXml} counter always reads {@code 0}.
 */
public class GadgetRegistrySelectionMetricsHarnessTest {

  private static final String EMPTY_CATALOG_RESOURCE =
      "com/percussion/webui/gadget/servlets/empty-gadget-catalog.json";

  @Before
  public void resetMetrics() {
    GadgetRegistry.resetSelectionMetrics();
  }

  @Test
  public void modernOnly_incrementsModernCounterWhenCatalogPresent() {
    Map<String, String> map = GadgetRegistry.loadGadgetTypeMap();
    assertFalse(map.isEmpty());
    assertEquals(GadgetRegistry.Source.MODERN_CATALOG, GadgetRegistry.getLastLoadSource());
    assertEquals(1L, GadgetRegistry.getModernLoadCount());
    assertEquals(0L, GadgetRegistry.getLegacyLoadCount());
    assertEquals(0L, GadgetRegistry.getNoneLoadCount());
    assertEquals(1L, GadgetRegistry.getTotalLoadCount());
    assertEquals(map.size(), GadgetRegistry.getLastLoadEntryCount());
  }

  @Test
  public void modernOnly_countsNoneWhenModernAbsent_despiteLegacyRegistryPresent() {
    // #5810: legacy registry present but retired — records NONE, never LEGACY_REGISTRY_XML.
    Map<String, String> map =
        GadgetRegistry.loadGadgetTypeMap(
            "com/percussion/webui/gadget/servlets/does-not-exist-catalog.json",
            GadgetRegistry.REGISTRY_RESOURCE);
    assertTrue(map.isEmpty());
    assertEquals(GadgetRegistry.Source.NONE, GadgetRegistry.getLastLoadSource());
    assertEquals(0L, GadgetRegistry.getModernLoadCount());
    assertEquals(0L, GadgetRegistry.getLegacyLoadCount());
    assertEquals(1L, GadgetRegistry.getNoneLoadCount());
  }

  @Test
  public void modernOnly_incrementsNoneCounterWhenBothMissing() {
    Map<String, String> map =
        GadgetRegistry.loadGadgetTypeMap(
            "com/percussion/webui/gadget/servlets/missing-a.json",
            "com/percussion/webui/gadget/servlets/missing-b.xml");
    assertTrue(map.isEmpty());
    assertEquals(GadgetRegistry.Source.NONE, GadgetRegistry.getLastLoadSource());
    assertEquals(0L, GadgetRegistry.getModernLoadCount());
    assertEquals(0L, GadgetRegistry.getLegacyLoadCount());
    assertEquals(1L, GadgetRegistry.getNoneLoadCount());
  }

  @Test
  public void successiveLoads_accumulateModernAndNoneWithZeroLegacy() {
    GadgetRegistry.loadGadgetTypeMap(); // modern
    GadgetRegistry.loadGadgetTypeMap(
        "com/percussion/webui/gadget/servlets/does-not-exist-catalog.json",
        GadgetRegistry.REGISTRY_RESOURCE); // none (legacy retired)
    GadgetRegistry.loadGadgetTypeMap(EMPTY_CATALOG_RESOURCE, GadgetRegistry.REGISTRY_RESOURCE);
    // empty modern is none (no legacy fallback)
    GadgetRegistry.loadGadgetTypeMap(
        "com/percussion/webui/gadget/servlets/missing-a.json",
        "com/percussion/webui/gadget/servlets/missing-b.xml"); // none

    assertEquals(1L, GadgetRegistry.getModernLoadCount());
    assertEquals(0L, GadgetRegistry.getLegacyLoadCount());
    assertEquals(3L, GadgetRegistry.getNoneLoadCount());
    assertEquals(4L, GadgetRegistry.getTotalLoadCount());
    assertEquals(GadgetRegistry.Source.NONE, GadgetRegistry.getLastLoadSource());
  }

  @Test
  public void snapshotAndSummary_exposeCountersAndLastSource() {
    GadgetRegistry.loadGadgetTypeMap();
    GadgetRegistry.loadGadgetTypeMap(
        "com/percussion/webui/gadget/servlets/does-not-exist-catalog.json",
        GadgetRegistry.REGISTRY_RESOURCE);

    Map<String, Long> snap = GadgetRegistry.getSelectionMetricsSnapshot();
    assertEquals(Long.valueOf(1L), snap.get("modern"));
    assertEquals(Long.valueOf(0L), snap.get("legacyRegistryXml"));
    assertEquals(Long.valueOf(1L), snap.get("none"));
    assertEquals(Long.valueOf(2L), snap.get("total"));

    String summary = GadgetRegistry.formatSelectionMetricsSummary();
    assertTrue(summary, summary.contains("modern=1"));
    assertTrue(summary, summary.contains("legacyRegistryXml=0"));
    assertTrue(summary, summary.contains("none=1"));
    assertTrue(summary, summary.contains("total=2"));
    assertTrue(summary, summary.contains("lastSource=NONE"));
  }

  @Test
  public void resetSelectionMetrics_clearsCountersAndLastSource() {
    GadgetRegistry.loadGadgetTypeMap();
    assertEquals(1L, GadgetRegistry.getModernLoadCount());

    GadgetRegistry.resetSelectionMetrics();

    assertEquals(0L, GadgetRegistry.getModernLoadCount());
    assertEquals(0L, GadgetRegistry.getLegacyLoadCount());
    assertEquals(0L, GadgetRegistry.getNoneLoadCount());
    assertEquals(0L, GadgetRegistry.getTotalLoadCount());
    assertEquals(GadgetRegistry.Source.NONE, GadgetRegistry.getLastLoadSource());
    assertEquals(0, GadgetRegistry.getLastLoadEntryCount());
  }

  @Test
  public void modernLoadIgnoresLegacyRegistryResource_countsAsModernOnly() {
    // Product classpath still carries both resources; modern-only load reports modern.
    Map<String, String> map =
        GadgetRegistry.loadGadgetTypeMap(
            GadgetRegistry.CATALOG_RESOURCE, GadgetRegistry.REGISTRY_RESOURCE);
    assertFalse(map.isEmpty());
    assertEquals(GadgetRegistry.Source.MODERN_CATALOG, GadgetRegistry.getLastLoadSource());
    assertEquals(1L, GadgetRegistry.getModernLoadCount());
    assertEquals(0L, GadgetRegistry.getLegacyLoadCount());
  }
}
