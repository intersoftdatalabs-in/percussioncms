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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.Test;

/**
 * Regression for v8.1.7 PR #722 / #885: Percussion + Deprecated groups. Issue #715 (Redirect
 * Management removed). Issue #2788 / #3025 dual-load: prefer {@code gadget-catalog.json}, fall
 * back to {@code GadgetRegistry.xml}; last-load source + entry count + INFO metrics.
 */
public class GadgetRegistryTest {

  private static final String EMPTY_CATALOG_RESOURCE =
      "com/percussion/webui/gadget/servlets/empty-gadget-catalog.json";
  private static final String INVALID_CATALOG_RESOURCE =
      "com/percussion/webui/gadget/servlets/invalid-gadget-catalog.json";

  @Test
  public void dualLoadPrefersModernCatalogWhenPresent() {
    Map<String, String> map = GadgetRegistry.loadGadgetTypeMap();
    assertFalse("gadget type map must load from modern catalog or legacy registry", map.isEmpty());
    assertEquals(
        "product ships modern catalog; dual-load must prefer it",
        GadgetRegistry.Source.MODERN_CATALOG,
        GadgetRegistry.getLastLoadSource());
    assertTrue(
        "modern catalog entry count must be positive", GadgetRegistry.getLastLoadEntryCount() > 0);
    assertEquals(map.size(), GadgetRegistry.getLastLoadEntryCount());

    assertEquals("Deprecated", map.get("Activity"));
    assertEquals("Deprecated", map.get("Siteimprove"));
    assertEquals("Deprecated", map.get("Membership"));
    assertEquals("Deprecated", map.get("Widget Configuration"));

    // #885 undeprecated these back to Percussion
    assertEquals("Percussion", map.get("Google Setup"));
    assertEquals("Percussion", map.get("Traffic"));
    assertEquals("Percussion", map.get("What's Working"));

    assertEquals("Percussion", map.get("Welcome"));
    assertEquals("Percussion", map.get("Bulk Upload"));
  }

  @Test
  public void fallsBackToRegistryXmlWhenModernCatalogAbsent() {
    Map<String, String> map =
        GadgetRegistry.loadGadgetTypeMap(
            "com/percussion/webui/gadget/servlets/does-not-exist-catalog.json",
            GadgetRegistry.REGISTRY_RESOURCE);
    assertFalse("legacy GadgetRegistry.xml must still be on the classpath", map.isEmpty());
    assertEquals(GadgetRegistry.Source.LEGACY_REGISTRY_XML, GadgetRegistry.getLastLoadSource());
    assertEquals(map.size(), GadgetRegistry.getLastLoadEntryCount());

    assertEquals("Deprecated", map.get("Activity"));
    assertEquals("Percussion", map.get("Welcome"));
    assertEquals("Percussion", map.get("Google Setup"));
    assertFalse(map.containsKey("Redirect Management"));
  }

  @Test
  public void fallsBackToRegistryXmlWhenModernCatalogEmpty() {
    Map<String, String> map =
        GadgetRegistry.loadGadgetTypeMap(EMPTY_CATALOG_RESOURCE, GadgetRegistry.REGISTRY_RESOURCE);
    assertFalse("empty modern catalog must fall back to legacy registry", map.isEmpty());
    assertEquals(GadgetRegistry.Source.LEGACY_REGISTRY_XML, GadgetRegistry.getLastLoadSource());
    assertTrue(GadgetRegistry.getLastLoadEntryCount() > 0);
    assertEquals("Percussion", map.get("Welcome"));
  }

  @Test
  public void fallsBackToRegistryXmlWhenModernCatalogUnreadable() {
    Map<String, String> map =
        GadgetRegistry.loadGadgetTypeMap(
            INVALID_CATALOG_RESOURCE, GadgetRegistry.REGISTRY_RESOURCE);
    assertFalse("unreadable modern catalog must fall back to legacy registry", map.isEmpty());
    assertEquals(GadgetRegistry.Source.LEGACY_REGISTRY_XML, GadgetRegistry.getLastLoadSource());
    assertEquals(map.size(), GadgetRegistry.getLastLoadEntryCount());
  }

  @Test
  public void emptyWhenBothSourcesMissing() {
    Map<String, String> map =
        GadgetRegistry.loadGadgetTypeMap(
            "com/percussion/webui/gadget/servlets/missing-a.json",
            "com/percussion/webui/gadget/servlets/missing-b.xml");
    assertTrue(map.isEmpty());
    assertEquals(GadgetRegistry.Source.NONE, GadgetRegistry.getLastLoadSource());
    assertEquals(0, GadgetRegistry.getLastLoadEntryCount());
  }

  @Test
  public void blankCatalogResourceSkipsModernAndUsesLegacy() {
    Map<String, String> map =
        GadgetRegistry.loadGadgetTypeMap("   ", GadgetRegistry.REGISTRY_RESOURCE);
    assertFalse(map.isEmpty());
    assertEquals(GadgetRegistry.Source.LEGACY_REGISTRY_XML, GadgetRegistry.getLastLoadSource());
  }

  @Test
  public void nullCatalogResourceSkipsModernAndUsesLegacy() {
    Map<String, String> map =
        GadgetRegistry.loadGadgetTypeMap(null, GadgetRegistry.REGISTRY_RESOURCE);
    assertFalse(map.isEmpty());
    assertEquals(GadgetRegistry.Source.LEGACY_REGISTRY_XML, GadgetRegistry.getLastLoadSource());
  }

  @Test
  public void lastLoadSourceUpdatesAcrossSuccessiveLoads() {
    GadgetRegistry.loadGadgetTypeMap(
        "com/percussion/webui/gadget/servlets/missing-a.json",
        "com/percussion/webui/gadget/servlets/missing-b.xml");
    assertEquals(GadgetRegistry.Source.NONE, GadgetRegistry.getLastLoadSource());
    assertEquals(0, GadgetRegistry.getLastLoadEntryCount());

    GadgetRegistry.loadGadgetTypeMap(
        "com/percussion/webui/gadget/servlets/does-not-exist-catalog.json",
        GadgetRegistry.REGISTRY_RESOURCE);
    assertEquals(GadgetRegistry.Source.LEGACY_REGISTRY_XML, GadgetRegistry.getLastLoadSource());
    assertTrue(GadgetRegistry.getLastLoadEntryCount() > 0);

    GadgetRegistry.loadGadgetTypeMap();
    assertEquals(GadgetRegistry.Source.MODERN_CATALOG, GadgetRegistry.getLastLoadSource());
    assertTrue(GadgetRegistry.getLastLoadEntryCount() > 0);
  }

  @Test
  public void modernPreferredEvenWhenLegacyAlsoPresent() {
    // Product classpath has both; dual-load must still report modern, not legacy.
    Map<String, String> map =
        GadgetRegistry.loadGadgetTypeMap(
            GadgetRegistry.CATALOG_RESOURCE, GadgetRegistry.REGISTRY_RESOURCE);
    assertFalse(map.isEmpty());
    assertEquals(GadgetRegistry.Source.MODERN_CATALOG, GadgetRegistry.getLastLoadSource());
    assertEquals(map.size(), GadgetRegistry.getLastLoadEntryCount());
  }

  @Test
  public void parseCatalogJsonBuildsNameToGroupMap() throws Exception {
    String json =
        """
        {
          "schemaVersion": "1.0",
          "gadgets": [
            { "id": "g1", "name": "Welcome", "group": "Percussion" },
            { "id": "g2", "name": "Activity", "group": "Deprecated" },
            { "id": "g3", "name": "", "group": "Percussion" },
            { "id": "g4", "name": "NoGroup" }
          ]
        }
        """;
    Map<String, String> map =
        GadgetRegistry.parseCatalogJson(
            new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    assertEquals(2, map.size());
    assertEquals("Percussion", map.get("Welcome"));
    assertEquals("Deprecated", map.get("Activity"));
    assertFalse(map.containsKey("NoGroup"));
  }

  @Test
  public void parseRegistryXmlBuildsNameToGroupMap() throws Exception {
    String xml =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <gadgets>
          <group name="Percussion">
            <gadget name="Welcome" baseuri="/cm/gadgets/repository/cm1_welcome_gadget" file="x.xml"/>
          </group>
          <group name="Deprecated">
            <gadget name="Activity" baseuri="/cm/gadgets/repository/perc_activity_gadget" file="y.xml"/>
          </group>
        </gadgets>
        """;
    Map<String, String> map =
        GadgetRegistry.parseRegistryXml(
            new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    assertEquals("Percussion", map.get("Welcome"));
    assertEquals("Deprecated", map.get("Activity"));
  }

  /**
   * Issue #715: Redirect Management gadget is discontinued and must not appear in the registry
   * (neither Percussion nor Deprecated). Unknown names resolve to Custom.
   */
  @Test
  public void redirectManagementGadgetIsRemoved() {
    Map<String, String> map = GadgetRegistry.loadGadgetTypeMap();
    assertFalse(
        "Redirect Management must not be registered after issue #715",
        map.containsKey("Redirect Management"));
    assertEquals("Custom", GadgetRegistry.getGadgetType("Redirect Management"));
  }

  @Test
  public void unknownGadgetIsCustom() {
    assertEquals("Custom", GadgetRegistry.getGadgetType("Not A Real Gadget"));
  }

  @Test
  public void deprecatedTypeLookup() {
    assertTrue("Deprecated".equals(GadgetRegistry.getGadgetType("Activity")));
  }

  @Test
  public void modernCatalogResourceIsOnClasspath() {
    assertTrue(
        "shipped gadget-catalog.json must be on the WebUI classpath",
        GadgetRegistry.class.getClassLoader().getResource(GadgetRegistry.CATALOG_RESOURCE) != null);
  }
}
