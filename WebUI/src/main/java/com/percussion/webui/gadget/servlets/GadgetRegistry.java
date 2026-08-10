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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
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
 * <p><strong>Dual-load policy (ADR-004 / #2788 / #2630 residual):</strong> prefer modern {@code
 * gadget-catalog.json} on the classpath; fall back to legacy {@code GadgetRegistry.xml} when the
 * modern catalog is absent or unreadable. Selection is intentional dual-run shim so product can
 * retire XML as the sole runtime source without breaking installs that still ship only the
 * registry.
 */
public final class GadgetRegistry {

  private static final Logger log = LogManager.getLogger(GadgetRegistry.class);

  /**
   * Classpath location of the modern aggregate catalog (parity with {@code
   * modules/perc-packages/.../catalogs/gadgets/gadget-catalog.json}).
   */
  public static final String CATALOG_RESOURCE =
      "com/percussion/webui/gadget/servlets/gadget-catalog.json";

  /** Classpath location of the legacy registry resource (fallback). */
  public static final String REGISTRY_RESOURCE =
      "com/percussion/webui/gadget/servlets/GadgetRegistry.xml";

  /** Which resource satisfied the last {@link #loadGadgetTypeMap()} (or overload) call. */
  public enum Source {
    /** Modern {@code gadget-catalog.json}. */
    MODERN_CATALOG,
    /** Legacy {@code GadgetRegistry.xml}. */
    LEGACY_REGISTRY_XML,
    /** Neither source produced entries. */
    NONE
  }

  private static final AtomicReference<Source> LAST_SOURCE = new AtomicReference<>(Source.NONE);

  private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();

  private GadgetRegistry() {}

  /**
   * Source used by the most recent dual-load attempt (for diagnostics and unit tests).
   *
   * @return never null
   */
  public static Source getLastLoadSource() {
    return LAST_SOURCE.get();
  }

  /**
   * Loads gadget display-name → group-name map using dual-load selection (modern catalog preferred,
   * legacy registry XML fallback).
   *
   * @return unmodifiable map; empty if both resources are missing or unreadable
   */
  public static Map<String, String> loadGadgetTypeMap() {
    return loadGadgetTypeMap(CATALOG_RESOURCE, REGISTRY_RESOURCE);
  }

  /**
   * Dual-load with explicit classpath resource names. Package-visible for unit tests that inject
   * missing-catalog / missing-xml paths.
   *
   * @param catalogResource classpath resource for modern JSON (may be null → skip modern)
   * @param registryResource classpath resource for legacy XML (may be null → skip legacy)
   * @return unmodifiable map; empty if both fail
   */
  static Map<String, String> loadGadgetTypeMap(String catalogResource, String registryResource) {
    if (catalogResource != null && !catalogResource.isBlank()) {
      Map<String, String> fromCatalog = tryLoadCatalog(catalogResource);
      if (!fromCatalog.isEmpty()) {
        LAST_SOURCE.set(Source.MODERN_CATALOG);
        log.debug(
            "Loaded gadget type map from modern catalog {} ({} entries)",
            catalogResource,
            fromCatalog.size());
        return fromCatalog;
      }
    }

    if (registryResource != null && !registryResource.isBlank()) {
      Map<String, String> fromXml = tryLoadRegistryXml(registryResource);
      if (!fromXml.isEmpty()) {
        LAST_SOURCE.set(Source.LEGACY_REGISTRY_XML);
        log.debug(
            "Loaded gadget type map from legacy registry {} ({} entries)",
            registryResource,
            fromXml.size());
        return fromXml;
      }
    }

    LAST_SOURCE.set(Source.NONE);
    log.error(
        "No gadget type map available: modern catalog [{}] and legacy registry [{}] both missing or empty",
        catalogResource,
        registryResource);
    return Collections.emptyMap();
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

  private static Map<String, String> tryLoadRegistryXml(String resource) {
    try (InputStream in = openClasspath(resource)) {
      if (in == null) {
        log.debug("Legacy gadget registry not on classpath: {}", resource);
        return Collections.emptyMap();
      }
      return parseRegistryXml(in);
    } catch (Exception e) {
      log.warn(
          "Failed to load legacy gadget registry {}: {}",
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
    try (JsonParser p = JSON_FACTORY.createParser(in)) {
      while (p.nextToken() != null) {
        if (p.currentToken() == JsonToken.PROPERTY_NAME && "gadgets".equals(p.currentName())) {
          if (p.nextToken() != JsonToken.START_ARRAY) {
            break;
          }
          while (p.nextToken() == JsonToken.START_OBJECT) {
            String name = null;
            String group = null;
            while (p.nextToken() != JsonToken.END_OBJECT) {
              String field = p.currentName();
              p.nextToken();
              if ("name".equals(field)) {
                name = p.getValueAsString();
              } else if ("group".equals(field)) {
                group = p.getValueAsString();
              } else {
                p.skipChildren();
              }
            }
            if (name != null && !name.isBlank() && group != null && !group.isBlank()) {
              gadTypeMap.put(name, group);
            }
          }
          break;
        }
      }
    }
    return Collections.unmodifiableMap(gadTypeMap);
  }

  /**
   * Parse legacy {@code GadgetRegistry.xml} into name → group map. Package-visible for unit tests.
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
