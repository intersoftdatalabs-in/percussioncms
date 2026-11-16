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
 * Regression for v8.1.7 PR #722 / #885: Percussion + Deprecated groups. Issue #2788 / #3025
 * single-load: {@code gadget-catalog.json} is the sole runtime source (dual-load fallback
 * removed #6440) — modern absent/empty/unreadable yields an empty map with source NONE.
 */
public class GadgetRegistryTest {

  private static final String EMPTY_CATALOG_RESOURCE =
      "com/percussion/webui/gadget/servlets/empty-gadget-catalog.json";
  private static final String INVALID_CATALOG_RESOURCE =
      "com/percussion/webui/gadget/servlets/invalid-gadget-catalog.json";

  @Test
  public void modernOnlyLoadsFromCatalogWhenPresent() {
    Map<String, String> map = GadgetRegistry.loadGadgetTypeMap();
    assertFalse("gadget type map must load from the modern catalog", map.isEmpty());
    assertEquals(
        "product ships modern catalog; modern-only load must report it",
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
  public void singleLoadSourceEnumHasNoLegacyValue() {
    // Registry evidence (#6440): the dual-load fallback is removed, so no legacy source can
    // ever be produced — the enum carries exactly the modern and none states.
    assertEquals(2, GadgetRegistry.Source.values().length);
  }

  @Test
  public void emptyWhenModernCatalogAbsent() {
    // Single-load (#6440): only the modern catalog is consulted — absent yields empty + NONE.
    Map<String, String> map =
        GadgetRegistry.loadGadgetTypeMap(
            "com/percussion/webui/gadget/servlets/does-not-exist-catalog.json");
    assertTrue("absent modern catalog must yield an empty map", map.isEmpty());
    assertEquals(GadgetRegistry.Source.NONE, GadgetRegistry.getLastLoadSource());
    assertEquals(0, GadgetRegistry.getLastLoadEntryCount());
  }

  @Test
  public void emptyWhenModernCatalogEmpty() {
    Map<String, String> map = GadgetRegistry.loadGadgetTypeMap(EMPTY_CATALOG_RESOURCE);
    assertTrue("empty modern catalog must yield an empty map", map.isEmpty());
    assertEquals(GadgetRegistry.Source.NONE, GadgetRegistry.getLastLoadSource());
    assertEquals(0, GadgetRegistry.getLastLoadEntryCount());
  }

  @Test
  public void emptyWhenModernCatalogUnreadable() {
    Map<String, String> map = GadgetRegistry.loadGadgetTypeMap(INVALID_CATALOG_RESOURCE);
    assertTrue("unreadable modern catalog must yield an empty map", map.isEmpty());
    assertEquals(GadgetRegistry.Source.NONE, GadgetRegistry.getLastLoadSource());
    assertEquals(0, GadgetRegistry.getLastLoadEntryCount());
  }

  @Test
  public void emptyWhenModernCatalogMissing() {
    Map<String, String> map =
        GadgetRegistry.loadGadgetTypeMap("com/percussion/webui/gadget/servlets/missing-a.json");
    assertTrue(map.isEmpty());
    assertEquals(GadgetRegistry.Source.NONE, GadgetRegistry.getLastLoadSource());
    assertEquals(0, GadgetRegistry.getLastLoadEntryCount());
  }

  @Test
  public void blankCatalogResourceYieldsEmpty() {
    Map<String, String> map = GadgetRegistry.loadGadgetTypeMap("   ");
    assertTrue(map.isEmpty());
    assertEquals(GadgetRegistry.Source.NONE, GadgetRegistry.getLastLoadSource());
  }

  @Test
  public void nullCatalogResourceYieldsEmpty() {
    Map<String, String> map = GadgetRegistry.loadGadgetTypeMap(null);
    assertTrue(map.isEmpty());
    assertEquals(GadgetRegistry.Source.NONE, GadgetRegistry.getLastLoadSource());
  }

  @Test
  public void lastLoadSourceUpdatesAcrossSuccessiveLoads() {
    GadgetRegistry.loadGadgetTypeMap("com/percussion/webui/gadget/servlets/missing-a.json");
    assertEquals(GadgetRegistry.Source.NONE, GadgetRegistry.getLastLoadSource());
    assertEquals(0, GadgetRegistry.getLastLoadEntryCount());

    GadgetRegistry.loadGadgetTypeMap(
        "com/percussion/webui/gadget/servlets/does-not-exist-catalog.json");
    assertEquals(GadgetRegistry.Source.NONE, GadgetRegistry.getLastLoadSource());
    assertEquals(0, GadgetRegistry.getLastLoadEntryCount());

    GadgetRegistry.loadGadgetTypeMap();
    assertEquals(GadgetRegistry.Source.MODERN_CATALOG, GadgetRegistry.getLastLoadSource());
    assertTrue(GadgetRegistry.getLastLoadEntryCount() > 0);
  }

  @Test
  public void modernLoadFromDefaultCatalog() {
    // Single-load (#6440): the default load reads the modern catalog only.
    Map<String, String> map = GadgetRegistry.loadGadgetTypeMap();
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
    // Retained pure-parser coverage: parseRegistryXml stays available for upgrade/tooling use
    // (LEGACY_GADGET_XML residual slice owns it) even though the single-load production path
    // never calls it (#5810 / #6440).
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

  /**
   * Issue #6348: the shipped catalog carries a TMX {@code nameKey} per gadget id so the served
   * registry and the React {@code gadgetsCatalog.ts} resolve the same localized name. The
   * English {@code name}/{@code group} wire shape is unchanged (existing type-map tests above
   * still pin it).
   */
  @Test
  public void nameKeyMapCoversEveryShippedGadget() {
    Map<String, String> keys = GadgetRegistry.loadGadgetNameKeyMap();
    Map<String, String> types = GadgetRegistry.loadGadgetTypeMap();
    assertEquals(
        "every catalog gadget must carry a TMX name key", types.size(), keys.size());
    // Full pin: a typo'd key in the served copy (the copy GadgetRegistry actually reads)
    // must fail here, not just disagree with the compiled catalog at runtime.
    Map<String, String> expected =
        Map.ofEntries(
            Map.entry("cm1_welcome_gadget", "perc.ui.gadgets.welcome@WELCOME"),
            Map.entry(
                "perc_workflow_status_gadget", "perc.ui.gadgets.workflowStatus@PAGES BY STATUS"),
            Map.entry("perc_activity_gadget", "perc.ui.dashboard.modern@Activity"),
            Map.entry(
                "PercProcessorMonitorGadget", "perc.ui.gadgets.processmonitor@Process Monitor"),
            Map.entry("perc_effectiveness_gadget", "perc.ui.dashboard.modern@What's Working"),
            Map.entry("PercAssetStatusGadget", "perc.ui.asset.status.gadget@ASSETS BY STATUS"),
            Map.entry("perc_bulk_file_upload_gadget", "perc.ui.gadget.bulkUpload@Bulk Upload"),
            Map.entry("perc_reports_gadget", "perc.ui.dashboard.modern@Reports"),
            Map.entry("perc_traffic_gadget", "perc.ui.traffic.gadget@TRAFFIC"),
            Map.entry("PercBlogsGadget", "perc.ui.dashboard.modern@Blogs"),
            Map.entry("perc_comments_gadget", "perc.ui.gadgets.comments@COMMENTS"),
            Map.entry("PercFormTrackerGadget", "perc.ui.dashboard.modern@Form Tracker"),
            Map.entry("perc_cookie_consent_gadget", "perc.ui.gadgets.cookieConsent@COOKIE CONSENT"),
            Map.entry("perc_seo_gadget", "perc.ui.gadgets.seo@SEO AUDIT"),
            Map.entry("perc_google_setup_gadget", "perc.ui.dashboard.modern@Google Setup"),
            Map.entry("perc_membership_gadget", "perc.ui.gadgets.membership@MEMBERSHIP"),
            Map.entry("perc_site_improve_gadget", "perc.ui.site.improve.gadget@SITEIMPROVE"),
            Map.entry("perc_iframe_gadget", "perc.ui.dashboard.modern@External Content"),
            Map.entry(
                "PercGlobalVariablesGadget", "perc.ui.global.variables.gadget@Global Variables"),
            Map.entry(
                "PercSiteFrameworkGadget", "perc.ui.gadgets.sitewideFramework@Sitewide Framework"),
            Map.entry(
                "PercWidgetConfigGadget", "perc.ui.dashboard.modern@Dashboard Configuration"));
    assertEquals(expected, keys);
  }

  @Test
  public void getGadgetNameKeyResolvesKnownIds() {
    assertEquals(
        "perc.ui.gadgets.welcome@WELCOME", GadgetRegistry.getGadgetNameKey("cm1_welcome_gadget"));
    assertEquals(
        "perc.ui.gadgets.cookieConsent@COOKIE CONSENT",
        GadgetRegistry.getGadgetNameKey("perc_cookie_consent_gadget"));
  }

  @Test
  public void getGadgetNameKeyNullForUnknownOrBlank() {
    assertEquals(null, GadgetRegistry.getGadgetNameKey("Not A Real Gadget"));
    assertEquals(null, GadgetRegistry.getGadgetNameKey(null));
    assertEquals(null, GadgetRegistry.getGadgetNameKey("   "));
  }

  @Test
  public void nameKeyMapEmptyWhenCatalogMissing() {
    Map<String, String> keys =
        GadgetRegistry.loadGadgetNameKeyMap(
            "com/percussion/webui/gadget/servlets/does-not-exist-catalog.json");
    assertTrue(keys.isEmpty());
  }

  @Test
  public void parseCatalogNameKeyMapOmitsUnkeyedEntries() throws Exception {
    String json =
        "{\"gadgets\":["
            + "{\"id\":\"known\",\"name\":\"Known\",\"nameKey\":\"some.key@Known\",\"group\":\"Percussion\"},"
            + "{\"id\":\"legacy\",\"name\":\"Legacy\",\"group\":\"Deprecated\"}]}";
    Map<String, String> keys =
        GadgetRegistry.parseCatalogNameKeyMap(
            new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    assertEquals(1, keys.size());
    assertEquals("some.key@Known", keys.get("known"));
  }
}
