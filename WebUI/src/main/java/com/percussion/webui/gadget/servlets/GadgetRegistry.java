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

import com.percussion.security.error.PSExceptionUtils;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.json.JsonFactory;

/**
 * Loads dashboard gadget name → group map for Percussion vs Deprecated grouping (v8.1.7 PR #722 /
 * #885).
 *
 * <p><strong>Single-load (ADR-004 / #2788 / #3025 / #2852 / residual #5810 / parent #2632;
 * dual-load fallback removed #6440):</strong> loads only the modern {@code gadget-catalog.json}
 * on the classpath. The legacy {@code GadgetRegistry.xml} fallback was retired in #5810 and its
 * selection remnants (dual-load overload, legacy source, legacy counters) were removed here:
 * when the modern catalog is absent, empty, or unreadable the load returns an empty map and logs
 * ERROR (no silent legacy load, nothing invented). Do <strong>not</strong> reintroduce a
 * legacy fallback.
 *
 * <p>Each load records {@link #getLastLoadSource()} and logs INFO selection metrics ({@code
 * modern=} / {@code none=} / {@code entries=}) for Phase 5 exit visibility (parity with {@code
 * PSWidgetDao} modern-only metrics).
 *
 * <p><strong>M2 evidence harness (#3131):</strong> cumulative load counters ({@link
 * #getModernLoadCount()}, {@link #getNoneLoadCount()}), {@link #getSelectionMetricsSnapshot()},
 * and {@link #formatSelectionMetricsSummary()} are CI- and support-visible. Reset with {@link
 * #resetSelectionMetrics()} in tests / probes only.
 */
public final class GadgetRegistry {

  private static final Logger log = LogManager.getLogger(GadgetRegistry.class);

  /**
   * Classpath location of the modern aggregate catalog (parity with {@code
   * modules/perc-packages/.../catalogs/gadgets/gadget-catalog.json}).
   */
  public static final String CATALOG_RESOURCE =
      "com/percussion/webui/gadget/servlets/gadget-catalog.json";

  /**
   * Which resource satisfied the last {@link #loadGadgetTypeMap()} call. The retired legacy
   * {@code GadgetRegistry.xml} source ({@code LEGACY_REGISTRY_XML}) was removed with the
   * dual-load fallback (#6440) — it can never be produced.
   */
  public enum Source {
    /** Modern {@code gadget-catalog.json}. */
    MODERN_CATALOG,
    /** Neither source produced entries. */
    NONE
  }

  private static final AtomicReference<Source> LAST_SOURCE = new AtomicReference<>(Source.NONE);

  /** Entry count from the most recent successful single-load (0 when {@link Source#NONE}). */
  private static final AtomicReference<Integer> LAST_ENTRY_COUNT = new AtomicReference<>(0);

  /** Cumulative single-loads that resolved via modern {@code gadget-catalog.json}. */
  private static final AtomicLong MODERN_LOAD_COUNT = new AtomicLong();

  /** Cumulative loads that produced no entries from the modern catalog. */
  private static final AtomicLong NONE_LOAD_COUNT = new AtomicLong();

  private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();

  private GadgetRegistry() {}

  /**
   * Source used by the most recent load attempt (for diagnostics and unit tests).
   *
   * @return never null
   */
  public static Source getLastLoadSource() {
    return LAST_SOURCE.get();
  }

  /**
   * Number of name→group entries produced by the most recent load attempt.
   *
   * @return non-negative count (0 when last source was {@link Source#NONE})
   */
  public static int getLastLoadEntryCount() {
    return LAST_ENTRY_COUNT.get();
  }

  /**
   * Cumulative modern-catalog loads since class init or last {@link #resetSelectionMetrics()}.
   *
   * @return non-negative count
   */
  public static long getModernLoadCount() {
    return MODERN_LOAD_COUNT.get();
  }

  /**
   * Cumulative empty loads (modern catalog missing) since class init or last {@link
   * #resetSelectionMetrics()}.
   *
   * @return non-negative count
   */
  public static long getNoneLoadCount() {
    return NONE_LOAD_COUNT.get();
  }

  /**
   * Total load attempts counted ({@link #getModernLoadCount()} + {@link #getNoneLoadCount()}).
   *
   * @return non-negative count
   */
  public static long getTotalLoadCount() {
    return MODERN_LOAD_COUNT.get() + NONE_LOAD_COUNT.get();
  }

  /**
   * Snapshot of cumulative load counters for CI assertions and support probes.
   *
   * <p>Keys: {@code modern}, {@code none}, {@code total}. Never null. The retired {@code
   * legacyRegistryXml} key was removed with the dual-load fallback (#6440).
   *
   * @return unmodifiable map of counter name → value
   */
  public static Map<String, Long> getSelectionMetricsSnapshot() {
    long modern = MODERN_LOAD_COUNT.get();
    long none = NONE_LOAD_COUNT.get();
    Map<String, Long> snap = new LinkedHashMap<>(6);
    snap.put("modern", modern);
    snap.put("none", none);
    snap.put("total", modern + none);
    return Collections.unmodifiableMap(snap);
  }

  /**
   * Single-line ops / log summary of modern-only single-load selection metrics (parity with INFO
   * load line and {@code PSWidgetDao} modern-only summary).
   *
   * @return never blank
   */
  public static String formatSelectionMetricsSummary() {
    Map<String, Long> snap = getSelectionMetricsSnapshot();
    return String.format(
        "Gadget registry single-load selection metrics: modern=%d, none=%d, total=%d, lastSource=%s, lastEntries=%d",
        snap.get("modern"),
        snap.get("none"),
        snap.get("total"),
        LAST_SOURCE.get(),
        LAST_ENTRY_COUNT.get());
  }

  /**
   * Resets cumulative load counters and last-load source/entry count. Intended for unit tests
   * and support probes — not a normal runtime control plane.
   */
  public static void resetSelectionMetrics() {
    MODERN_LOAD_COUNT.set(0L);
    NONE_LOAD_COUNT.set(0L);
    LAST_SOURCE.set(Source.NONE);
    LAST_ENTRY_COUNT.set(0);
  }

  /**
   * Loads gadget display-name → group-name map from the modern catalog only (#5810; dual-load
   * fallback removed #6440).
   *
   * @return unmodifiable map; empty if the modern catalog is missing or unreadable
   */
  public static Map<String, String> loadGadgetTypeMap() {
    return loadGadgetTypeMap(CATALOG_RESOURCE);
  }

  /**
   * Modern-only single load with an explicit classpath resource. Package-visible for unit tests
   * that inject missing-catalog paths.
   *
   * @param catalogResource classpath resource for modern JSON (may be null → empty result)
   * @return unmodifiable map; empty if modern fails
   */
  static Map<String, String> loadGadgetTypeMap(String catalogResource) {
    if (catalogResource != null && !catalogResource.isBlank()) {
      Map<String, String> fromCatalog = tryLoadCatalog(catalogResource);
      if (!fromCatalog.isEmpty()) {
        recordSelection(Source.MODERN_CATALOG, fromCatalog.size(), catalogResource);
        return fromCatalog;
      }
    }

    recordSelection(Source.NONE, 0, catalogResource);
    return Collections.emptyMap();
  }

  /**
   * Publish last-load source/entry count and INFO selection metrics (modern / none).
   * Package-visible pattern mirrors {@code PSWidgetDao} modern-only INFO counts for Phase 5 M2.
   */
  private static void recordSelection(Source source, int entries, String catalogResource) {
    LAST_SOURCE.set(source);
    LAST_ENTRY_COUNT.set(entries);
    int modern = source == Source.MODERN_CATALOG ? 1 : 0;
    int none = source == Source.NONE ? 1 : 0;
    if (modern > 0) {
      MODERN_LOAD_COUNT.incrementAndGet();
    } else {
      NONE_LOAD_COUNT.incrementAndGet();
    }
    log.info(
        "Gadget registry single-load selection: modern={}, none={}, entries={}, source={}, cumulativeModern={}, cumulativeNone={}",
        modern,
        none,
        entries,
        source,
        MODERN_LOAD_COUNT.get(),
        NONE_LOAD_COUNT.get());
    if (source == Source.NONE) {
      log.error(
          "No gadget type map available: modern catalog [{}] missing or empty (no legacy fallback; convert customer gadget XML via the gadget registry compiler upgrade path)",
          catalogResource);
    } else if (log.isDebugEnabled()) {
      log.debug(
          "Gadget modern-only load resolved via {} (catalogResource={}, entries={})",
          source,
          catalogResource,
          entries);
    }
  }

  /**
   * Maps a gadget display name to its logical group.
   *
   * @param gadgetName display name from catalog/registry (e.g. "Activity")
   * @return group name, or {@code "Custom"} if unknown
   */
  public static String getGadgetType(String gadgetName) {
    if (gadgetName == null || gadgetName.isBlank()) {
      return "Custom";
    }
    String type = loadGadgetTypeMap().get(gadgetName);
    return type != null ? type : "Custom";
  }

  /**
   * Loads gadget id → TMX display-name key from the modern catalog (issue #6348).
   *
   * <p>The {@code name}/{@code group} wire shape is unchanged: {@link #loadGadgetTypeMap()} and
   * {@link #getGadgetType(String)} behave exactly as before. This map lets server-side callers
   * hand the SPA the same TMX key the React {@code gadgetsCatalog.ts} resolves per locale,
   * instead of the English {@code name} fallback.
   *
   * @return unmodifiable map of gadget id → TMX key (entries without a key are omitted);
   *     empty if the modern catalog is missing or unreadable
   */
  public static Map<String, String> loadGadgetNameKeyMap() {
    return loadGadgetNameKeyMap(CATALOG_RESOURCE);
  }

  /**
   * Gadget id → TMX display-name key load with an explicit classpath resource. Package-visible
   * for unit tests that inject missing-catalog paths.
   *
   * @param catalogResource classpath resource for modern JSON (may be null → empty result)
   * @return unmodifiable map (empty if modern fails)
   */
  static Map<String, String> loadGadgetNameKeyMap(String catalogResource) {
    if (catalogResource == null || catalogResource.isBlank()) {
      return Collections.emptyMap();
    }
    try (InputStream in = openClasspath(catalogResource)) {
      if (in == null) {
        log.debug("Modern gadget catalog not on classpath: {}", catalogResource);
        return Collections.emptyMap();
      }
      return parseCatalogNameKeyMap(in);
    } catch (Exception e) {
      log.warn(
          "Failed to load gadget name keys from {}: {}",
          catalogResource,
          PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      return Collections.emptyMap();
    }
  }

  /**
   * TMX display-name key for a gadget id, or null when the catalog has no key for it.
   *
   * @param gadgetId gadget id from the catalog (e.g. "PercBlogsGadget")
   * @return TMX key, or null if unknown
   */
  public static String getGadgetNameKey(String gadgetId) {
    if (gadgetId == null || gadgetId.isBlank()) {
      return null;
    }
    return loadGadgetNameKeyMap().get(gadgetId);
  }

  private static Map<String, String> tryLoadCatalog(String resource) {
    try (InputStream in = openClasspath(resource)) {
      if (in == null) {
        log.debug("Modern gadget catalog not on classpath: {}", resource);
        return Collections.emptyMap();
      }
      return parseCatalogJson(in);
    } catch (Exception e) {
      log.warn(
          "Failed to load modern gadget catalog {}: {}",
          resource,
          PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      return Collections.emptyMap();
    }
  }

  private static InputStream openClasspath(String resource) {
    return GadgetRegistry.class.getClassLoader().getResourceAsStream(resource);
  }

  /**
   * Parse modern {@code gadget-catalog.json} into name → group map. Package-visible for unit tests.
   *
   * @param in non-null JSON stream
   * @return unmodifiable map (empty if no gadgets)
   * @throws Exception on I/O or JSON errors
   */
  static Map<String, String> parseCatalogJson(InputStream in) throws Exception {
    Map<String, String> gadTypeMap = new LinkedHashMap<>();
    for (Map<String, String> gadget : parseGadgetObjects(in)) {
      String name = gadget.get("name");
      String group = gadget.get("group");
      if (name != null && !name.isBlank() && group != null && !group.isBlank()) {
        gadTypeMap.put(name, group);
      }
    }
    return Collections.unmodifiableMap(gadTypeMap);
  }

  /**
   * Parse modern {@code gadget-catalog.json} into id → TMX display-name key map. Package-visible
   * for unit tests. Entries without a {@code nameKey} are omitted; the {@code name}/{@code
   * group} shape parsed by {@link #parseCatalogJson} is untouched.
   *
   * @param in non-null JSON stream
   * @return unmodifiable map (empty if no keyed gadgets)
   * @throws Exception on I/O or JSON errors
   */
  static Map<String, String> parseCatalogNameKeyMap(InputStream in) throws Exception {
    Map<String, String> nameKeyMap = new LinkedHashMap<>();
    for (Map<String, String> gadget : parseGadgetObjects(in)) {
      String id = gadget.get("id");
      String nameKey = gadget.get("nameKey");
      if (id != null && !id.isBlank() && nameKey != null && !nameKey.isBlank()) {
        nameKeyMap.put(id, nameKey);
      }
    }
    return Collections.unmodifiableMap(nameKeyMap);
  }

  /**
   * Reads the {@code gadgets} array of a modern {@code gadget-catalog.json} document into one
   * string-field map per entry. Non-scalar values are skipped; unknown fields are kept so
   * callers project only what they need.
   *
   * @param in non-null JSON stream
   * @return gadget entries in document order (never null)
   * @throws Exception on I/O or JSON errors
   */
  private static List<Map<String, String>> parseGadgetObjects(InputStream in) throws Exception {
    List<Map<String, String>> gadgets = new ArrayList<>();
    try (JsonParser p = JSON_FACTORY.createParser(in)) {
      while (p.nextToken() != null) {
        if (p.currentToken() == JsonToken.PROPERTY_NAME && "gadgets".equals(p.currentName())) {
          if (p.nextToken() != JsonToken.START_ARRAY) {
            break;
          }
          while (p.nextToken() == JsonToken.START_OBJECT) {
            gadgets.add(readStringFields(p));
          }
          break;
        }
      }
    }
    return gadgets;
  }

  /**
   * Reads one {@code START_OBJECT} (parser positioned on it) into a string-field map.
   *
   * @param p non-null parser positioned at {@code START_OBJECT}
   * @return scalar field name → text value (never null)
   * @throws Exception on I/O or JSON errors
   */
  private static Map<String, String> readStringFields(JsonParser p) throws Exception {
    Map<String, String> fields = new LinkedHashMap<>();
    while (p.nextToken() != JsonToken.END_OBJECT) {
      String field = p.currentName();
      if (p.nextToken().isScalarValue()) {
        fields.put(field, p.getValueAsString());
      } else {
        p.skipChildren();
      }
    }
    return fields;
  }

  /**
   * Parse legacy {@code GadgetRegistry.xml} into name → group map. Package-visible for unit tests.
   * Retained as a pure XML→map parser for upgrade/tooling use (e.g. {@code
   * PSGadgetRegistryCompiler} parity, owned by the LEGACY_GADGET_XML residual slice); the
   * single-load production path never calls it (#5810 retired the fallback, #6440 removed the
   * dual-load selection).
   *
   * @param in non-null XML stream
   * @return unmodifiable map (empty if no gadgets)
   * @throws Exception on I/O or XML errors
   */
  static Map<String, String> parseRegistryXml(InputStream in) throws Exception {
    Map<String, String> gadTypeMap = new LinkedHashMap<>();
    Document doc = PSXmlDocumentBuilder.createXmlDocument(in, false);
    NodeList groupElems = doc.getElementsByTagName("group");
    for (int i = 0; i < groupElems.getLength(); i++) {
      Element groupElem = (Element) groupElems.item(i);
      String groupName = groupElem.getAttribute("name");
      NodeList gadgetElems = groupElem.getElementsByTagName("gadget");
      for (int j = 0; j < gadgetElems.getLength(); j++) {
        Element gadgetElem = (Element) gadgetElems.item(j);
        String gdgName = gadgetElem.getAttribute("name");
        if (gdgName != null && !gdgName.isBlank()) {
          gadTypeMap.put(gdgName, groupName);
        }
      }
    }
    return Collections.unmodifiableMap(gadTypeMap);
  }
}
